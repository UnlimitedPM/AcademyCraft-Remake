package cn.academy.ability.electromaster;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Active skill, port of original Railgun: long-range high-damage snipe with strong knockback. */
public class RailgunSkill extends Skill {

    private static final float CP_COST_MIN_EXP = 200f;
    private static final float CP_COST_MAX_EXP = 450f;
    private static final double RANGE = 30;
    private static final double KNOCKBACK = 2.5;

    /** La longueur du rail, comme le rayon de l'original : quarante-cinq blocs. */
    private static final double BEAM_LENGTH = 45.0;

    /** Quinze arcs, comme ses {@code ARC_SIZE}, vivant le temps d'un tir. */
    private static final int BEAM_ARCS = 15;
    private static final int BEAM_ARC_TICKS = 15;

    /** L'ecart lateral des arcs autour de l'axe, comme le sien (0,1 a 0,25). */
    private static final double BEAM_WOBBLE = 0.25;

    public RailgunSkill() {
        super("railgun", 4);
    }

    /**
     * Degats repris de l'original : de 60 a 110 selon l'experience.
     *
     * C'est le chiffre de la 1.12.2, et il est bien plus eleve que celui du port (20)
     * : le railgun est cense tuer net, et c'est une competence de niveau 4. Le
     * reglage {@code general.damageScale} permet de l'adoucir sans rien recompiler.
     * Le cout en CP suit l'original : 200 a 450, et il monte avec l'experience.
     */
    public float damage(AbilityData data) {
        return lerp(60f, 110f, data.getSkillExp(this));
    }

    /** Recharge reprise de l'original : de 300 a 160 ticks, soit 15 a 8 secondes. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(300f, 160f, data.getSkillExp(this));
    }

    /**
     * Experience de l'original : 0,005 pour un tir, 0,01 s'il touche. Le paquet
     * d'activation ne sait pas si le tir a porte, donc c'est le montant du tir qui est
     * verse ; la part du coup au but reviendra avec les evenements de degats.
     */
    @Override
    public float getExpGain(AbilityData data) {
        return 0.005f;
    }

    /** Le cout en CP, repris de l'original : de 200 a 450 selon l'experience. */
    @Override
    public float getCpCost(AbilityData data) {
        return lerp(CP_COST_MIN_EXP, CP_COST_MAX_EXP, data.getSkillExp(this));
    }

    /** Le cout au depart, pour qui n'a pas d'experience a donner. */
    @Override
    public float getCpCost() {
        return CP_COST_MIN_EXP;
    }

    /**
     * Surcout repris de l'original : de 180 a 120 selon l'experience.
     *
     * C'est le tir qui remplit le plus la reserve : sur une reserve de 350 points,
     * deux tirs d'affilee suffisent a mettre le joueur en surcharge.
     */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(180f, 120f, data.getSkillExp(this));
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        // Le seul son de l'original qui se pose dans le monde plutot qu'au joueur : un tir de
        // railgun s'entend de loin. Il part a CHAQUE tir — le port ne le jouait qu'en touchant,
        // ce qui rendait muet le tir qui manque.
        cn.academy.sound.AcademySounds.playAt(player.level(), player.position(),
                cn.academy.ModSounds.EM_RAILGUN, 0.5f, 1.0f);

        shootBeam(player);

        Entity target = TargetingUtil.findEntityInSight(player, RANGE);
        if (!(target instanceof LivingEntity living)) return;

        living.hurt(player.damageSources().indirectMagic(player, player), scaled(damage(data)));
        Vec3 push = living.position().subtract(player.position()).normalize().scale(KNOCKBACK);
        living.setDeltaMovement(living.getDeltaMovement().add(push.x, 0.2, push.z));
        living.hurtMarked = true;
    }

    /**
     * Le rail : quinze arcs semes le long du tir, jusqu'a quarante-cinq blocs.
     *
     * <p>C'est ce que dessinait {@code EntityRailgunFX} : un rayon lumineux, et des arcs
     * gresillant le long de son axe, un tous les un a deux blocs, avec un petit ecart lateral
     * autour de la ligne. Le port n'a pas encore de rendu de rayon — il lui faudrait un trait
     * qu'il n'a pas — mais les arcs, eux, se font avec le moteur d'eclairs qu'il a deja, et
     * c'est eux qui donnent au tir son cote electrique.
     */
    private static void shootBeam(Player player) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        net.minecraft.util.RandomSource random = player.getRandom();

        for (int i = 0; i < BEAM_ARCS; i++) {
            double start = 1.0 + i * (BEAM_LENGTH - 1.0) / BEAM_ARCS;
            double end = Math.min(BEAM_LENGTH, start + 1.5 + random.nextDouble() * 1.5);
            // Les arcs ne sont pas SUR l'axe : ils gresillent autour, comme les siens. Un rail
            // parfaitement droit ne ressemblerait a rien.
            Vec3 from = wobble(eye.add(look.scale(start)), random);
            Vec3 to = wobble(eye.add(look.scale(end)), random);
            sendArc(player, cn.academy.ability.client.arc.ArcPattern.RAILGUN.name(), from, to,
                    BEAM_ARC_TICKS);
        }
    }

    /** Un point du rail, ecarte d'un rien de l'axe : la demi-borne laterale de l'original. */
    private static Vec3 wobble(Vec3 point, net.minecraft.util.RandomSource random) {
        return point.add(wobble(random), wobble(random), wobble(random));
    }

    /** Une composante de cet ecart, entre moins et plus {@code BEAM_WOBBLE}. */
    private static double wobble(net.minecraft.util.RandomSource random) {
        return (random.nextDouble() - 0.5) * BEAM_WOBBLE * 2.0;
    }

    /**
     * Envoie un arc a tous ceux qui voient le tireur.
     *
     * <p>Le motif voyage par son nom, donc le serveur n'a rien a connaitre du rendu — c'est le
     * message d'effet de l'original, celui qui fait qu'on voit le tir venir.
     */
    private static void sendArc(Player player, String pattern, Vec3 from, Vec3 to, int ticks) {
        cn.academy.ability.network.AbilityNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF
                        .with(() -> player),
                new cn.academy.ability.network.ArcEffectPacket(pattern, from, to, ticks, false,
                        player.getId()));
    }
}
