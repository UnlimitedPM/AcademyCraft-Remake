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

    private static final float CP_COST = 35f;
    private static final double RANGE = 30;
    private static final double KNOCKBACK = 2.5;

    public RailgunSkill() {
        super("railgun", 4);
    }

    /**
     * Degats repris de l'original : de 60 a 110 selon l'experience.
     *
     * C'est le chiffre de la 1.12.2, et il est bien plus eleve que celui du port (20)
     * : le railgun est cense tuer net, et c'est une competence de niveau 4. Le
     * reglage {@code general.damageScale} permet de l'adoucir sans rien recompiler.
     * Le cout en CP reste celui du port, pour la meme raison que partout ailleurs.
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

    @Override
    public float getCpCost() {
        return CP_COST;
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
        Entity target = TargetingUtil.findEntityInSight(player, RANGE);
        if (!(target instanceof LivingEntity living)) return;

        living.hurt(player.damageSources().indirectMagic(player, player), scaled(damage(data)));
        Vec3 push = living.position().subtract(player.position()).normalize().scale(KNOCKBACK);
        living.setDeltaMovement(living.getDeltaMovement().add(push.x, 0.2, push.z));
        living.hurtMarked = true;
    }
}
