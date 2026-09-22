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
import javax.annotation.Nullable;

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

    /**
     * Le bloc que le regard touche, ou {@code null} s'il n'y en a aucun.
     *
     * <p>La difference avec {@link #findImpactPoint} est celle entre « ou » et « quoi » :
     * certaines competences ont besoin des <b>coordonnees du bloc</b> — l'onde de choc
     * dirigee prend le centre de son rayon dessus — et non du point de la face frappee.
     */
    @Nullable
    public static BlockHitResult findBlockInSight(Player player, double range) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult hit = player.level().clip(
                new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    /** Le bout du regard, quand rien ne l'arrete : celui de l'original, portee comprise. */
    public static Vec3 fallbackPoint(Player player, double range) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        // L'original partait de la <b>position</b> du joueur, pas de ses yeux — et c'est ce
        // point-la qui devient le centre de ses effets.
        return player.position().add(look.scale(range));
    }

    /**
     * Premiere entite rencontree sur un segment, l'auteur exclu.
     *
     * Portage de {@code Raytrace.perform} pour sa partie « entites » : les competences
     * qui frappent depuis un point du monde — la bille posee en l'air par la bombe a
     * fragmentation — ne partent pas de l'oeil du joueur, et ne peuvent donc pas se
     * contenter de {@link #findEntityInSight}. L'auteur est exclu comme dans l'original,
     * qui retirait le lanceur de son selecteur.
     */
    public static Entity findEntityAlong(Player owner, Vec3 from, Vec3 to) {
        return findEntityAlong(owner, from, to, e -> true);
    }

    /**
     * La meme chose, en ne retenant que ce que le filtre accepte.
     *
     * Le vol du jet engine ne frappe que ce qui vit ({@code exclude(player).and(living)}
     * dans l'original) : sans ce filtre, une barque croisee sur la trajectoire
     * arreterait le coup a la place du passager.
     */
    public static Entity findEntityAlong(Player owner, Vec3 from, Vec3 to,
                                         java.util.function.Predicate<Entity> extra) {
        AABB box = new AABB(from, to).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(owner, from, to, box,
                e -> !e.isSpectator() && e.isPickable() && e != owner && extra.test(e),
                from.distanceToSqr(to));
        return hit != null ? hit.getEntity() : null;
    }
}
