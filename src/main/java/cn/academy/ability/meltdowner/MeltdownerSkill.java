package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Active skill, port of original Meltdowner: sustained plasma beam, single-target hitscan damage. */
public class MeltdownerSkill extends Skill {

    private static final float CP_COST = 20f;
    private static final double RANGE = 20;
    private static final float DAMAGE = 12f;

    public MeltdownerSkill() {
        super("meltdowner", 3);
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
    }
}
