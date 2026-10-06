package cn.academy.ability.electromaster;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Competence chargee, portage de ThunderClap : on tient la touche, la foudre s'amasse,
 * et au relachement elle tombe la ou le joueur regarde, en emportant tout ce qui
 * l'entoure.
 *
 * <p>C'est la competence la plus chere d'electromaster, et la seule dont la puissance
 * depend du temps de charge <b>et</b> dont l'entretien se paie par tick. Tenir plus
 * longtemps ne coute rien au-dela du minimum, comme dans l'original : les quarante
 * premiers ticks sont obligatoires et factures, les vingt suivants sont du bonus.
 */
public class ThunderClapSkill extends Skill {

    /** Charge minimale, et duree de l'entretien facture, comme {@code MIN_TICKS}. */
    public static final int MIN_TICKS = 40;

    /** Charge maximale, comme {@code MAX_TICKS} : au-dela, la foudre ne grossit plus. */
    public static final int MAX_TICKS = 60;

    /** Portee de la visee ou tombe la foudre, comme l'original. */
    private static final double RANGE = 40.0;

    /** La duree d'un arc : celle du thunder bolt, pour que les deux se ressemblent. */
    private static final int STRIKE_ARC_TICKS = 20;

    public ThunderClapSkill() {
        super("thunder_clap", 5);
    }

    /**
     * Facteur de charge, portage de {@code lerpf(1.0f, 1.2f, (ticks - 40.0f) / 60.0f)}.
     *
     * <p>Le diviseur de l'original est 60 alors que la charge plafonne a 60 ticks : le
     * facteur vaut donc 1 au minimum et 1,067 au maximum, jamais les 1,2 de la borne.
     * Le port garde la formule telle quelle — c'est la courbe voulue par l'auteur, et
     * sa borne haute lui servait pour une charge plus longue que la duree autorisee.
     */
    public float damageFactor(int chargeTicks) {
        return lerp(1f, 1.2f, (chargeTicks - MIN_TICKS) / 60f);
    }

    /** Degats de la foudre : de 36 a 72 selon l'experience, fois le facteur de charge. */
    public float damage(AbilityData data) {
        return lerp(36f, 72f, data.getSkillExp(this)) * damageFactor(data.getChargeTicks(this));
    }

    /** Rayon frappe autour du point d'impact : de 15 a 30 blocs. */
    public double range(AbilityData data) {
        return lerp(15f, 30f, data.getSkillExp(this));
    }

    /** Surcout d'ouverture : de 390 a 252, comme l'original. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(390f, 252f, data.getSkillExp(this));
    }

    /**
     * Entretien des quarante premiers ticks.
     *
     * L'original payait 18 a 25 CP par tick sur sa reserve ; ramene a 100, cela fait
     * 0,65 a 0,9, soit une trentaine de points pour la charge obligatoire.
     */
    public float chargeCpCost(AbilityData data) {
        return lerp(18f, 25f, data.getSkillExp(this));
    }

    /**
     * Le client rejoue ce chiffre pour ses nombres : voir {@link Skill#getTickUpkeep}.
     *
     * <p>Au-dela du minimum, la charge est du bonus : elle ne se paie plus, des deux cotes.
     */
    @Override
    public float getTickUpkeep(AbilityData data, int ticks) {
        return ticks <= MIN_TICKS ? chargeCpCost(data) : 0f;
    }

    /**
     * Recharge : le temps tenu fois 10 a 6, selon l'experience.
     *
     * Une charge au minimum coute donc 400 ticks (20 s) au depart, 240 (12 s) au
     * maximum : c'est le prix de la competence la plus destructrice de la categorie.
     */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) (data.getChargeTicks(this) * lerp(10f, 6f, data.getSkillExp(this)));
    }

    /** 0,003 par claquement, quelle que soit la charge, comme l'original. */
    @Override
    public float getExpGain(AbilityData data) {
        return 0.003f;
    }

    @Override
    public boolean isChargeable() {
        return true;
    }

    @Override
    public int getMinChargeTicks(AbilityData data) {
        return MIN_TICKS;
    }

    @Override
    public int getMaxChargeTicks(AbilityData data) {
        return MAX_TICKS;
    }

    /**
     * Oui : l'orage tombe tout seul au bout de sa charge.
     *
     * <p>L'original le faisait depuis son tick serveur — {@code ticks >= MAX_TICKS} envoyait la
     * fin du contexte, et la foudre tombait sans qu'on relache la touche. C'est la seule
     * competence du mod dans ce cas, et c'est ce que dit {@link Skill#firesAtMaxCharge()}.
     */
    @Override
    public boolean firesAtMaxCharge() {
        return true;
    }

    @Override
    public void onStart(Player player, AbilityData data) {
        // Le surcout de l'orage se paie a la CHARGE, et pas au claquement : c'est le
        // {@code s_onStart} de l'original, qui consommait son surcout des l'appui. La
        // competence paie donc elle-meme, et le declenchement ne repaie rien — voir
        // {@link #paysOnEffect}.
        data.perform(0f, getOverloadCost(data));
        // Meme epinglage que le meltdowner : la reserve ne redescend pas pendant qu'on charge.
        data.setHeldOverload(this, data.getOverload());
    }

    /**
     * L'orage paie lui-meme, et a sa facon.
     *
     * <p>Le surcout tombe a l'appui (voir {@link #onStart}) et les CP tick par tick pendant la
     * charge (voir {@link #onChargeTick}) : il ne reste donc rien a payer au declenchement.
     */
    @Override
    public boolean paysOnEffect() {
        return true;
    }

    @Override
    public boolean onChargeTick(Player player, AbilityData data, int chargeTicks) {
        // Au-dela du minimum, la charge est du bonus : elle ne se paie plus.
        if (chargeTicks > MIN_TICKS) return true;
        return data.consumeControlPoint(getTickUpkeep(data, chargeTicks));
    }

    @Override
    public void onActivateCharged(Player player, AbilityData data, int chargeTicks) {
        Vec3 impact = TargetingUtil.findImpactPoint(player, RANGE);

        if (player.level() instanceof ServerLevel level) {
            // Foudre purement visuelle : l'original la posait en `effectOnly`, donc elle
            // ne met pas le feu et ne frappe pas d'elle-meme — les degats sont ceux de
            // la competence, calcules ici.
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.setVisualOnly(true);
                bolt.moveTo(impact);
                level.addFreshEntity(bolt);
            }
        }

        strike(player, impact, data);
    }

    /** Frappe tout ce qui se trouve dans le rayon, sauf le lanceur. */
    private void strike(Player player, Vec3 impact, AbilityData data) {
        AABB area = new AABB(impact, impact).inflate(range(data));
        List<LivingEntity> targets =
                player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player);
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().indirectMagic(player, player), scaled(damage(data)));
            // Un arc par victime, pose au POINT D'IMPACT : c'est la foudre qui rebondit sur ce
            // qu'elle prend, comme dans le thunder bolt. L'original n'en avait pas, mais sa
            // foudre de vanilla se dessine a l'interieur du plafond des qu'on est sous terre —
            // le joueur a trouve une grotte ou il ne se passait rien a l'ecran. Aucun arc ne
            // part du joueur : il n'en a jamais ete question.
            sendArc(player, cn.academy.ability.client.arc.ArcPattern.AOE.name(), impact,
                    target.position().add(0, target.getEyeHeight(), 0), STRIKE_ARC_TICKS);
        }
    }

    /**
     * Envoie un arc a tous ceux qui voient le tireur.
     *
     * <p>Le motif voyage par son nom, donc le serveur n'a rien a connaitre du rendu — c'est le
     * message d'effet de l'original, celui qui fait qu'on voit l'attaque venir.
     */
    private static void sendArc(Player player, String pattern, Vec3 from, Vec3 to, int ticks) {
        cn.academy.ability.network.AbilityNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF
                        .with(() -> player),
                new cn.academy.ability.network.ArcEffectPacket(pattern, from, to, ticks, false,
                        player.getId()));
    }
}
