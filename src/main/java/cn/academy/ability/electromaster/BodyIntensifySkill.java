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

    /** Recharge reprise de l'original : de 900 a 600 ticks, soit 45 a 30 secondes. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(900f, 600f, data.getSkillExp(this));
    }

    @Override
    public float getCpCost() {
        return CP_COST;
    }

    /** Surcout repris de l'original : de 200 a 120 selon l'experience. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(200f, 120f, data.getSkillExp(this));
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION_TICKS, 1));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, DURATION_TICKS, 0));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DURATION_TICKS, 0));
    }
}
