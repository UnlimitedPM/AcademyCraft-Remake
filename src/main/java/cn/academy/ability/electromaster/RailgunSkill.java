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
    private static final float DAMAGE = 20f;
    private static final double KNOCKBACK = 2.5;

    public RailgunSkill() {
        super("railgun", 4);
    }

    @Override
    public float getCpCost() {
        return CP_COST;
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        Entity target = TargetingUtil.findEntityInSight(player, RANGE);
        if (!(target instanceof LivingEntity living)) return;

        living.hurt(player.damageSources().indirectMagic(player, player), scaled(DAMAGE));
        Vec3 push = living.position().subtract(player.position()).normalize().scale(KNOCKBACK);
        living.setDeltaMovement(living.getDeltaMovement().add(push.x, 0.2, push.z));
        living.hurtMarked = true;
    }
}
