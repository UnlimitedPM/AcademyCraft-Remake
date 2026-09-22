package cn.academy.ability.electromaster;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** Active skill, port of original BodyIntensify: short self-buff (speed/strength/regen). */
public class BodyIntensifySkill extends Skill {

    private static final float CP_COST = 25f;
    private static final int DURATION_TICKS = 200; // 10s

    public BodyIntensifySkill() {
        super("body_intensify", 3);
    }

    /** 0,01 a l'application du renfort, comme dans l'original. */
    @Override
    public float getExpGain(AbilityData data) {
        return 0.01f;
    }

    @Override
    public float getCpCost() {
        return CP_COST;
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION_TICKS, 1));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, DURATION_TICKS, 0));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DURATION_TICKS, 0));
    }
}
