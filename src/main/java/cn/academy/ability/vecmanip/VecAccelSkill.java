package cn.academy.ability.vecmanip;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Active skill, port of original VecAccel: propels the player forward along their look vector. */
public class VecAccelSkill extends Skill {

    private static final float CP_COST = 15f;
    private static final double IMPULSE = 1.6;

    public VecAccelSkill() {
        super("vec_accel");
    }

    @Override
    public float getCpCost() {
        return CP_COST;
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(player.getDeltaMovement().add(
                look.x * IMPULSE, Math.max(look.y, 0.2) * IMPULSE * 0.5, look.z * IMPULSE));
        player.hurtMarked = true;
    }
}
