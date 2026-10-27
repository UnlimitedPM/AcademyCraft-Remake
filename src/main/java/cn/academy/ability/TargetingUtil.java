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

    /**
     * La creature que le regard touche, ou {@code null}.
     *
     * <p>Le rayon s'arrete au premier <b>bloc</b> : sans cela il traversait le sol et les murs, et
     * le coup partait sur une vache sous la terre. Le joueur l'a vu autrement — « il utilise des CP
     * et de l'overload meme quand il ne vise aucune entite » : il ne visait rien, mais la visee, si.
     * C'est le {@code Raytrace.traceLiving} de l'original, qui tracait les deux d'un seul rayon.
     */
    public static Entity findEntityInSight(Player player, double range) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult block = player.level().clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        // Un oeil DANS la matiere — coince dans un mur, ou dans une salle de test taillee dans la
        // pierre — bute sur le bloc qui le contient, a distance nulle : la visee ne verrait alors
        // plus rien du tout. Ce cas-la ne coupe rien.
        boolean embedded = block.getType() != HitResult.Type.MISS
                && block.getLocation().distanceToSqr(eye) < 0.01;
        Vec3 stop = block.getType() == HitResult.Type.MISS || embedded ? end : block.getLocation();
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, stop, box,
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

    /**
     * Le bout du regard, quand rien ne l'arrete : c'est le centre des effets d'une visee
     * qui n'a rien trouve.
     *
     * <p>L'original avait deux replis sur trois lignes, et un seul vivait. Son code lisait
     * {@code if (trace == null) player.getPositionVector + regard * 4} — donc les
     * <b>pieds</b> — puis un dernier {@code else} qui prenait le <b>point</b> du resultat.
     * Or {@code Raytrace.perform} ne rend jamais {@code null} : quand rien n'est touche, il
     * rend un resultat {@code Type.MISS} dont le point est <b>la fin du rayon</b>, c'est-a-dire
     * {@code yeux + regard * portee}. La branche des pieds etait morte, et le port avait
     * recopie la morte : ses ondes s'ouvraient un bloc et demi sous les yeux.
     *
     * <p>Cela ne se voyait pas devant un mur — le centre etait de toute facon celui du bloc
     * touche — mais en vol, dans le vide, l'onde s'ouvrait a hauteur de sol au lieu de
     * s'ouvrir devant le visage. C'est le joueur qui l'a vu : « si je m'envole et que je tape
     * dans le vide, les ondes apparaissent pratiquement au niveau du sol plutot que d'aller
     * devant moi ».
     */
    public static Vec3 fallbackPoint(Player player, double range) {
        return player.getEyePosition(1.0f).add(player.getViewVector(1.0f).scale(range));
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
        EntityHitResult hit = findEntityHit(owner, from, to, extra);
        return hit != null ? hit.getEntity() : null;
    }

    /**
     * La meme chose, mais avec le <b>point d'impact</b>.
     *
     * <p>C'est ce point qui arrete un rayon : celui de la bombe a electrons se dessinait jusqu'au
     * bout de sa portee, derriere la bete touchee, faute de le connaitre — voir
     * {@code EntityMdBall.shoot}. Le point tombe sur la <b>boite</b> de la bete, donc sur sa
     * surface, et non sur son centre.
     */
    @Nullable
    public static EntityHitResult findEntityHit(Player owner, Vec3 from, Vec3 to,
                                                java.util.function.Predicate<Entity> extra) {
        AABB box = new AABB(from, to).inflate(1.0);
        return ProjectileUtil.getEntityHitResult(owner, from, to, box,
                e -> !e.isSpectator() && e.isPickable() && e != owner && extra.test(e),
                from.distanceToSqr(to));
    }
}
