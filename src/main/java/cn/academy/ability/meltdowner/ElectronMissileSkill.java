package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Electron Missile : la derniere competence du meltdowner, portage de {@code ElectronMissile}.
 *
 * <p>Competence <b>tenue</b>, et celle du port qui se joue le plus longtemps : tant que la touche
 * reste enfoncee, le lanceur accumule des billes — une toutes les dix ticks, cinq au plus — et
 * toutes les huit ticks l'une d'elles part sur l'ennemi le plus proche dans les cinq a treize
 * blocs. C'est une arme d'attrition : le surcout d'ouverture est epingle (il ne redescend pas
 * pendant le maintien), l'entretien se paie en CP par tick, et chaque tir se paie en plus.
 *
 * <p>Les billes de l'original etaient des entites visibles, dessinees par un shader ; le port
 * n'a pas d'effets de ce genre, il les <b>compte</b> donc dans l'etat du maintien (voir
 * {@code AbilityData.Hold}). Le joueur ne les voit pas tourner autour de lui, mais il les sent
 * partir : le tir est refusé quand il n'en reste aucune.
 *
 * <p>Une coquille de l'original est corrigee : sa recharge etait calculee par
 * {@code clampi(700, 400, exp)}, qui borne une valeur entre deux bornes donnees a l'envers et
 * retombait donc presque toujours sur 400. Le port suit l'intention — de 700 a 400 ticks selon
 * l'experience, comme toutes les autres courbes du meme genre.
 */
public class ElectronMissileSkill extends Skill {

    /** Le nombre de billes qu'un maintien peut porter, comme l'original. */
    public static final int MAX_BALLS = 5;
    /** Une bille toutes les dix ticks. */
    public static final int SPAWN_PERIOD = 10;
    /** Et un tir toutes les huit, des qu'il y a de quoi. */
    public static final int ATTACK_PERIOD = 8;
    /** Le surcout d'ouverture, epingle pendant tout le maintien. */
    public static final float OPEN_OVERLOAD = 200f;

    public ElectronMissileSkill() {
        super("electron_missile", 5);
    }

    @Override
    public boolean isHeld() {
        return true;
    }

    /** Le tir se paie et verse son experience depuis l'effet, qui seul sait s'il a touche. */
    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    /** Le branchement ne coute pas de CP : c'est l'entretien qui paie. */
    @Override
    public float getCpCost() {
        return 0f;
    }

    @Override
    public float getOverloadCost(AbilityData data) {
        return OPEN_OVERLOAD;
    }

    /** Entretien : de 12 a 5 CP par tick, comme l'original. */
    public float upkeep(AbilityData data) {
        return lerp(12f, 5f, data.getSkillExp(this));
    }

    /** Et le tir lui-meme : de 60 a 25 CP, plus 9 a 4 de surcout. */
    public float shotCost(AbilityData data) {
        return lerp(60f, 25f, data.getSkillExp(this));
    }

    public float shotOverload(AbilityData data) {
        return lerp(9f, 4f, data.getSkillExp(this));
    }

    /** Degats d'une bille : de 10 a 18. */
    public float damage(AbilityData data) {
        return lerp(10f, 18f, data.getSkillExp(this));
    }

    /** Portee de la chasse : de 5 a 13 blocs. */
    public float range(AbilityData data) {
        return lerp(5f, 13f, data.getSkillExp(this));
    }

    /** Duree maximale du maintien : de 80 a 200 ticks, comme l'original. */
    @Override
    public int getMaxHoldTicks(AbilityData data) {
        return (int) lerp(80f, 200f, data.getSkillExp(this));
    }

    /** Recharge a la fin : de 700 a 400 ticks. Voir la coquille corrigee dans la classe. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(700f, 400f, data.getSkillExp(this));
    }

    @Override
    public void onStart(Player player, AbilityData data) {
        // Le surcout d'ouverture est epingle : sans cela, tenir le missile rembourserait son
        // propre prix au bout de quelques secondes, et l'arme serait gratuite.
        data.setHeldOverload(this, data.getOverload());
        data.setHoldBalls(this, 0);
    }

    /**
     * Un tick de maintien : l'entretien, puis la bille a poser, puis le tir.
     *
     * <p>L'ordre est celui de l'original : il payait son entretien d'abord et s'arretait la si
     * la reserve ne suivait plus, posait sa bille tous les dix ticks, et tirait tous les huit.
     */
    @Override
    public boolean onHoldTick(Player player, AbilityData data, int ticks) {
        if (!data.consumeControlPoint(upkeep(data))) return false;

        int balls = data.getHoldBalls(this);
        if (ticks % SPAWN_PERIOD == 0 && balls < MAX_BALLS) {
            data.setHoldBalls(this, balls + 1);
            balls++;
        }

        if (ticks != 0 && ticks % ATTACK_PERIOD == 0 && balls > 0) {
            LivingEntity target = closest(player, range(data));
            if (target != null && data.perform(shotCost(data), shotOverload(data))) {
                target.invulnerableTime = 0;
                target.hurt(player.damageSources().indirectMagic(player, player),
                        scaled(damage(data)));
                // Comme les autres tirs du meltdowner, celui-ci marque sa cible : c'est par la
                // que l'original le faisait passer (MDDamageHelper.attack).
                RadiationMarks.mark(target, data);
                data.setHoldBalls(this, balls - 1);
                data.addSkillExp(this, 0.001f);
            }
        }
        return true;
    }

    /**
     * L'ennemi le plus proche de la portee, ou {@code null}.
     *
     * <p>L'original parcourait les vivants autour de lui et gardait le plus proche : le port fait
     * la meme chose, sans tri, donc sans dependre de l'ordre de la liste.
     */
    private static LivingEntity closest(Player player, float range) {
        Vec3 center = player.position();
        AABB area = new AABB(center, center).inflate(range);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : player.level().getEntities(player, area,
                e -> e instanceof LivingEntity && e.isAlive())) {
            double distance = entity.distanceToSqr(player);
            if (distance > (double) range * range) continue;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = (LivingEntity) entity;
            }
        }
        return best;
    }
}
