package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Passive skill, port of original LightShield: absorbs half of incoming damage while there is enough CP. */
public class LightShieldSkill extends Skill {

    private static final float ABSORB_RATIO = 0.5f;
    private static final float CP_COST_PER_HIT = 8f;

    public LightShieldSkill() {
        super("light_shield", 2);
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public float onDamaged(Player player, AbilityData data, LivingHurtEvent event) {
        if (!data.consumeControlPoint(CP_COST_PER_HIT)) return event.getAmount();
        return event.getAmount() * (1 - ABSORB_RATIO);
    }
}
