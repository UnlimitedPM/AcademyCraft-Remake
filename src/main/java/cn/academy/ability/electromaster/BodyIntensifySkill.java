package cn.academy.ability.electromaster;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** Active skill, port of original BodyIntensify: short self-buff (speed/strength/regen). */
public class BodyIntensifySkill extends Skill {

    /**
     * Ce qu'il coute en CP, repris de l'original.
     *
     * <p>Chez lui le renfort se tenait : 20 a 15 CP par tick pendant les quarante ticks
     * qu'il fallait pour en tirer quelque chose, soit 800 a 600 points en tout. Le port
     * l'applique d'un coup, alors il facture d'un coup ce que l'original prelevait
     * pendant ces quarante ticks — meme depense, une seule ligne au lieu de quarante.
     */
    private static final float CP_COST_MIN_EXP = 40f * 20f;
    private static final float CP_COST_MAX_EXP = 40f * 15f;
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
    public float getCpCost(AbilityData data) {
        return lerp(CP_COST_MIN_EXP, CP_COST_MAX_EXP, data.getSkillExp(this));
    }

    /** Le cout au depart, pour qui n'a pas d'experience a donner. */
    @Override
    public float getCpCost() {
        return CP_COST_MIN_EXP;
    }

    /** Surcout repris de l'original : de 200 a 120 selon l'experience. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(200f, 120f, data.getSkillExp(this));
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        cn.academy.sound.AcademySounds.playFor(player,
                cn.academy.ModSounds.EM_INTENSIFY_ACTIVATE, 0.5f);

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION_TICKS, 1));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, DURATION_TICKS, 0));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DURATION_TICKS, 0));
    }
}
