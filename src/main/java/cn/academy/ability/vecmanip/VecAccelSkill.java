package cn.academy.ability.vecmanip;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Active skill, port of original VecAccel: propels the player forward along their look vector. */
public class VecAccelSkill extends Skill {

    private static final float CP_COST = 15f;
    private static final double SPEED = 2.2;

    public VecAccelSkill() {
        super("vec_accel");
    }

    @Override
    public float getCpCost() {
        return CP_COST;
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        // Look slightly upward like the original (pitch - 10) so the arc carries the player forward.
        double pitch = Math.toRadians(player.getXRot() - 10);
        double yaw = Math.toRadians(player.getYRot());
        double x = -Math.sin(yaw) * Math.cos(pitch);
        double y = -Math.sin(pitch);
        double z = Math.cos(yaw) * Math.cos(pitch);

        // Original replaces the velocity outright rather than stacking onto existing motion.
        player.setDeltaMovement(new Vec3(x, y, z).normalize().scale(SPEED));
        player.fallDistance = 0;
        player.hurtMarked = true;
    }
}
