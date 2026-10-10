package cn.academy.ability.electromaster;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Active skill, portage de ThunderBolt : un eclat qui frappe droit devant, puis se
 * propage en cercle autour de son point d'impact.
 *
 * <p>Deux degats differents, comme dans l'original : la cible touchee encaisse la
 * version forte, ce qui se trouve dans les huit blocs autour encaisse la version
 * faible. L'engourdissement, lui, n'apparait qu'a partir d'un cinquieme d'experience :
 * un debutant fait mal, il ne paralyse pas.
 */
public class ThunderBoltSkill extends Skill {

    /** Portee de la visee, comme {@code ThunderBolt.RANGE}. */
    private static final double RANGE = 20.0;

    /** Rayon de la propagation, comme {@code ThunderBolt.AOE_RANGE}. */
    private static final double AOE_RANGE = 8.0;

    /** Le cout en CP, repris de l'original : de 280 a 420 selon l'experience. */
    private static final float CP_COST_MIN_EXP = 280f;
    private static final float CP_COST_MAX_EXP = 420f;

    /** Chance d'engourdir, et seuil d'experience, comme l'original. */
    private static final float SLOW_CHANCE = 0.8f;
    private static final float SLOW_EXP = 0.2f;

    /** Trois gros arcs pour l'eclair lui-meme, comme l'original, et vingt ticks de vie. */
    private static final int MAIN_ARCS = 3;
    private static final int MAIN_ARC_TICKS = 20;

    /** Duree d'un arc de propagation : l'original tirait entre 15 et 25 ticks. */
    private static final int AOE_ARC_MIN_TICKS = 15;
    private static final int AOE_ARC_SPAN = 11;

    public ThunderBoltSkill() {
        super("thunder_bolt", 4);
    }

    /** Degats sur la cible touchee : de 10 a 25. */
    public float damage(AbilityData data) {
        return lerp(10f, 25f, data.getSkillExp(this));
    }

    /** Degats sur ce qui entoure l'impact : de 6 a 15. */
    public float aoeDamage(AbilityData data) {
        return lerp(6f, 15f, data.getSkillExp(this));
    }

    /** Recharge reprise de l'original : de 120 a 50 ticks, soit 6 a 2,5 secondes. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(120f, 50f, data.getSkillExp(this));
    }

    /** Surcout repris de l'original : de 50 a 27. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(50f, 27f, data.getSkillExp(this));
    }

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
     * Experience d'un tir qui ne touche rien : 0,003, comme l'original.
     *
     * Le tir qui touche rapporte davantage (0,005) ; la difference est versee depuis
     * l'effet lui-meme, qui est le seul a savoir s'il a touche quelque chose.
     */
    @Override
    public float getExpGain(AbilityData data) {
        return 0.003f;
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        // L'eclair s'annonce a l'appui, plus fort que l'arc faible du premier degre :
        // l'original jouait 0,6 contre 0,5.
        cn.academy.sound.AcademySounds.playFor(player, cn.academy.ModSounds.EM_ARC_STRONG, 0.6f);

        // L'eclair se dessine, et chez tous ceux qui voient le tireur : c'est le message
        // d'effet de l'original, qui faisait naitre ses trois arcs chez chaque client. Ils
        // partent des YEUX et vont jusqu'au bout de la portee, meme si un mur est plus proche —
        // l'original envoyait sa portee sans regarder le bloc rencontre. Le moteur tire un
        // dessin different pour chacun, donc les trois se voient.
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        for (int i = 0; i < MAIN_ARCS; i++) {
            sendArc(player, cn.academy.ability.client.arc.ArcPattern.STRONG.name(), eye,
                    eye.add(look.scale(RANGE)), MAIN_ARC_TICKS);
        }

        Entity target = TargetingUtil.findEntityInSight(player, RANGE);
        Vec3 impact = target != null
                ? target.position().add(0, target.getEyeHeight(), 0)
                : TargetingUtil.findImpactPoint(player, RANGE);

        boolean effective = false;

        if (target instanceof LivingEntity living) {
            effective = true;
            living.hurt(skillDamage(player), scaled(damage(data)));
            // La foudre charge parfois le creeper qu'elle touche (voir CreeperCharge) : c'est
            // l'EMDamageHelper de l'original, ou les deux competences a arc passaient.
            CreeperCharge.tryCharge(living, player.getRandom().nextFloat());
            // Et elle change le villageois en sorciere, comme la foudre du jeu : meme endroit chez
            // vanilla, mais une regle certaine (voir WitchConversion).
            WitchConversion.tryConvert(living);
            if (data.getSkillExp(this) > SLOW_EXP && isSlowed(player)) {
                // 40 ticks d'engourdissement, comme l'original (2 secondes).
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
            }
        }

        for (LivingEntity around : entitiesAround(player, impact, target)) {
            effective = true;
            around.hurt(skillDamage(player), scaled(aoeDamage(data)));
            // La propagation charge elle aussi ce qu'elle embrase : l'original passait ses
            // voisines par le meme aide que sa cible. Et les villageoises prises dans l'arc
            // deviennent des sorcieres, comme sous la foudre du jeu.
            CreeperCharge.tryCharge(around, player.getRandom().nextFloat());
            WitchConversion.tryConvert(around);
            // Un arc par voisine : l'original reliait le point d'impact a chacune, d'une duree
            // tiree au hasard entre 15 et 25 ticks.
            sendArc(player, cn.academy.ability.client.arc.ArcPattern.AOE.name(), impact,
                    around.position().add(0, around.getEyeHeight(), 0),
                    AOE_ARC_MIN_TICKS + player.getRandom().nextInt(AOE_ARC_SPAN));
            // L'original appliquait cet engourdissement a la cible principale dans sa
            // boucle de propagation, donc jamais a l'entite concernee. Le port suit
            // l'intention : chaque entite prise dans l'arc est engourdie.
            if (data.getSkillExp(this) > SLOW_EXP && isSlowed(player)) {
                around.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3));
            }
        }

        // Un tir qui touche rapporte 0,005 : les 0,002 manquants s'ajoutent ici.
        if (effective) {
            data.addSkillExp(this, 0.002f);
        }
    }

    /** Les entites vivantes autour du point d'impact, sans la cible deja frappee. */
    private static List<LivingEntity> entitiesAround(Player player, Vec3 impact, Entity target) {
        AABB area = new AABB(impact, impact).inflate(AOE_RANGE);
        return player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e != target);
    }

    /**
     * Envoie un arc a tous ceux qui voient le tireur.
     *
     * <p>Le motif voyage par son <b>nom</b> : le serveur nomme un motif pur, et n'a donc rien a
     * connaitre du rendu. Sans ce message, l'eclair ne se dessinerait que chez celui qui appuie
     * sur la touche, et les autres joueurs prendraient les degats sans rien voir venir.
     */
    private static void sendArc(Player player, String pattern, Vec3 from, Vec3 to, int ticks) {
        cn.academy.ability.network.AbilityNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF
                        .with(() -> player),
                new cn.academy.ability.network.ArcEffectPacket(pattern, from, to, ticks, false,
                        player.getId()));
    }

    /** Les 80 % de chance de l'original : un tirage par entite, pas un par competence. */
    private static boolean isSlowed(Player player) {
        return player.getRandom().nextFloat() < SLOW_CHANCE;
    }
}
