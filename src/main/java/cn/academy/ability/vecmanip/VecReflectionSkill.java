package cn.academy.ability.vecmanip;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Passive skill, port of original VecReflection: redirects part of incoming damage back
 * at the attacker and absorbs part of it, at the cost of Control Points per hit.
 */
public class VecReflectionSkill extends Skill {

    private static final float REFLECT_RATIO = 0.5f;
    private static final float ABSORB_RATIO = 0.3f;
    private static final float CP_COST_PER_HIT = 5f;

    public VecReflectionSkill() {
        super("vec_reflection", 4);
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public float onDamaged(Player player, AbilityData data, LivingHurtEvent event) {
        DamageSource source = event.getSource();
        Entity attackerEntity = source.getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) return event.getAmount();
        if (!data.consumeControlPoint(CP_COST_PER_HIT)) return event.getAmount();

        float amount = event.getAmount();
        float reflected = amount * REFLECT_RATIO;
        float absorbed = amount * ABSORB_RATIO;
        attacker.hurt(player.damageSources().magic(), reflected);
        return amount - absorbed;
    }
}
