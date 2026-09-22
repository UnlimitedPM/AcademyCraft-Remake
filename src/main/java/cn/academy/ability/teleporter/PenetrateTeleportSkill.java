package cn.academy.ability.teleporter;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Active skill, port of original PenetrateTeleport: blinks forward through walls, ignoring obstacles. */
public class PenetrateTeleportSkill extends Skill {

    private static final float CP_COST = 30f;
    private static final double RANGE = 6;

    public PenetrateTeleportSkill() {
        super("penetrate_teleport", 2);
    }

    @Override
    public float getCpCost() {
        return CP_COST;
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        Vec3 start = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 dest = start.add(look.scale(RANGE));
        player.teleportTo(dest.x, dest.y - 1.6, dest.z);
        player.fallDistance = 0;
    }
}
