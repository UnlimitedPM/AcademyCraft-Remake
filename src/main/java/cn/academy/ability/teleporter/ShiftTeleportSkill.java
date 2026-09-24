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

    /** Le prix du depose : 260 a 320 CP, comme l'original, et il monte avec l'experience. */
    private static final float CP_COST_MIN_EXP = 260f;
    private static final float CP_COST_MAX_EXP = 320f;

    public ShiftTeleportSkill() {
        super("shift_tp", 4);
    }

    /**
     * Portee reprise de l'original : de 25 a 35 blocs selon l'experience.
     *
     * L'original blessait en plus les creatures traversees (15 a 35 degats) ; le port
     * ne fait que deplacer le joueur, donc cette part n'a pas ete portee. Le cout en
     * CP reste celui du port.
     */
    public double maxRange(AbilityData data) {
        return lerp(25f, 35f, data.getSkillExp(this));
    }

    /** Recharge reprise de l'original : de 100 a 60 ticks, soit 5 a 3 secondes. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(100f, 60f, data.getSkillExp(this));
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
    public float getCpCost(AbilityData data) {
        return lerp(CP_COST_MIN_EXP, CP_COST_MAX_EXP, data.getSkillExp(this));
    }

    /** Le cout au depart, pour qui n'a pas d'experience a donner. */
    @Override
    public float getCpCost() {
        return CP_COST_MIN_EXP;
    }

    /** Surcout repris de l'original : de 40 a 30 selon l'experience. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(40f, 30f, data.getSkillExp(this));
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(maxRange(data)));
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
        // L'original le jouait au dernier moment, et seulement si sa ligne avait trouve
        // quelqu'un : le port, qui ne fait pas ce coup au passage, le joue toujours.
        cn.academy.sound.AcademySounds.playFor(player, cn.academy.ModSounds.TP_TP_SHIFT, 0.5f);
        TeleporterCategory.DIM_FOLDING_THEOREM.onTeleported(data);
    }
}
