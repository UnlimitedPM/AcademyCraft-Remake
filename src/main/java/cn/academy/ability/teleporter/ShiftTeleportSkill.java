package cn.academy.ability.teleporter;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Active skill, port of original ShiftTeleport: blinks toward where the player looks, stopping at any obstacle. */
public class ShiftTeleportSkill extends Skill {

    private static final float CP_COST = 20f;
    private static final double MAX_RANGE = 12;

    public ShiftTeleportSkill() {
        super("shift_tp", 4);
    }

    /**
     * L'original versait 0,002 par entite traversee, plus 0,002. Le port ne compte pas
     * les entites traversees : c'est donc le montant de base qui est verse.
     */
    @Override
    public float getExpGain(AbilityData data) {
        return 0.002f;
    }

    @Override
    public float getCpCost() {
        return CP_COST;
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(MAX_RANGE));
        double eyeHeight = player.getEyeHeight();

        HitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        double destX, destY, destZ;
        if (hit.getType() == HitResult.Type.MISS) {
            // Nothing in range: only then do we use the full distance.
            destX = end.x;
            destY = end.y - eyeHeight;
            destZ = end.z;
        } else {
            Vec3 hitLoc = hit.getLocation();
            Direction face = ((BlockHitResult) hit).getDirection();
            if (face == Direction.UP) {
                // Looking at the ground: land exactly on top of the block that was aimed at.
                destX = hitLoc.x;
                destY = hitLoc.y;
                destZ = hitLoc.z;
            } else if (face == Direction.DOWN) {
                Vec3 backed = hitLoc.subtract(look.scale(0.5));
                destX = backed.x;
                destY = backed.y - eyeHeight;
                destZ = backed.z;
            } else {
                // Wall: stop just short of it, keep current height.
                Vec3 backed = hitLoc.subtract(look.scale(0.5));
                destX = backed.x;
                destY = player.getY();
                destZ = backed.z;
            }
        }

        player.teleportTo(destX, destY, destZ);
        player.fallDistance = 0;
        TeleporterCategory.DIM_FOLDING_THEOREM.onTeleported(data);
    }
}
