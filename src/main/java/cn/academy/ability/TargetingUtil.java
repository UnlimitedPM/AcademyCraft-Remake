package cn.academy.ability;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shared hitscan targeting helper for skills that fire in the direction the player looks. */
public final class TargetingUtil {

    private TargetingUtil() {}

    public static Entity findEntityInSight(Player player, double range) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(range));
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                e -> !e.isSpectator() && e.isPickable() && e != player, range * range);
        return hit != null ? hit.getEntity() : null;
    }

    /**
     * Point d'impact du regard : le bloc devant, ou la portee maximale s'il n'y en a pas.
     *
     * Portage de {@code Raytrace.traceLiving} pour sa position : les competences qui
     * frappent en cercle autour de leur point d'impact — l'eclair du thunder bolt, le
     * claquement du thunder clap — ont besoin de ce point, et pas seulement de l'entite
     * touchee.
     */
    public static Vec3 findImpactPoint(Player player, double range) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult hit = player.level().clip(
                new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }
}
