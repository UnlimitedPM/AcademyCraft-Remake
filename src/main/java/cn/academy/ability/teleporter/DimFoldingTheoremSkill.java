package cn.academy.ability.teleporter;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Passive skill, port of original DimFoldingTheorem: negates fall damage while learned. */
public class DimFoldingTheoremSkill extends Skill {

    public DimFoldingTheoremSkill() {
        super("dim_folding_theorem");
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public float onDamaged(Player player, AbilityData data, LivingHurtEvent event) {
        if (event.getSource().is(DamageTypes.FALL)) {
            return 0f;
        }
        return event.getAmount();
    }
}
