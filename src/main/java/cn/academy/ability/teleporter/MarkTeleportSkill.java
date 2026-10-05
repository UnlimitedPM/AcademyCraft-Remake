package cn.academy.ability.teleporter;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

/**
 * Competence chargee, portage de MarkTeleport : le joueur vise, la portee grandit tant
 * qu'il tient la touche, et il part ou il regarde.
 *
 * <p>La portee ne grandit pas indefiniment : elle avance de deux blocs par tick, mais
 * elle est bornee par l'experience (vingt-cinq a soixante blocs) et surtout par ce qui
 * reste dans la reserve — la teleportation se paie au bloc parcouru, donc viser plus loin
 * que ce qu'on peut payer ne changerait rien. Le cout est prelevé <b>sans verification</b>,
 * comme le {@code consumeWithForce} de l'original : la limite ci-dessus fait qu'il ne
 * depasse jamais la reserve.
 *
 * <p>Le reperage de la destination est celui de l'original : une entite visee donne sa
 * position a hauteur d'yeux, un bloc donne la face regardee avec un decalage par face, et
 * un regard dans le vide donne la portee maximale. La verification de la tete, elle, fait
 * redescendre d'un bloc et quart quand le joueur ne passerait pas.
 */
public class MarkTeleportSkill extends Skill {

    /** En dessous, l'original ne teleportait pas : il n'y a pas de saut a faire. */
    public static final double MINIMUM_DISTANCE = 3.0;

    /** Blocs gagnes par tick de charge. */
    private static final double BLOCKS_PER_TICK = 2.0;

    public MarkTeleportSkill() {
        super("mark_teleport", 2);
    }

    /** Cout par bloc : 12 a 4, comme l'original. */
    public float cpPerBlock(AbilityData data) {
        return lerp(12f, 4f, data.getSkillExp(this));
    }

    /**
     * Portee atteignable : deux blocs par tick, bornee par l'experience et par la
     * reserve.
     *
     * Portage de {@code getMaxDist} : le {@code min} sur la reserve n'est pas une
     * decoration, c'est lui qui garantit que le cout ne depasse jamais ce qu'on a.
     */
    public static double maxDistance(float exp, float controlPoint, int chargeTicks, float cpPerBlock) {
        double byExperience = lerp(25f, 60f, exp);
        double byReserve = cpPerBlock <= 0f ? byExperience : controlPoint / cpPerBlock;
        return Math.min((chargeTicks + 1) * BLOCKS_PER_TICK, Math.min(byExperience, byReserve));
    }

    /** Surcout : de 40 a 20, comme l'original. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(40f, 20f, data.getSkillExp(this));
    }

    /**
     * Recharge : de 30 a 0 tick.
     *
     * Au maximum d'experience, se teleporter ne coute plus d'attente du tout : c'est ce
     * qui fait de cette competence un deplacement et non un tour de passe-passe.
     */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(30f, 0f, data.getSkillExp(this));
    }

    /** Tout vient de la distance parcourue : voir {@link #earnsExpOnEffect()}. */
    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    @Override
    public boolean isChargeable() {
        return true;
    }

    @Override
    public void onStart(Player player, AbilityData data) {
        data.setHeldOverload(this, data.getOverload());
    }

    @Override
    public void onActivateCharged(Player player, AbilityData data, int chargeTicks) {
        Vec3 destination = destination(player, data, chargeTicks);
        double distance = destination.distanceTo(player.position());
        if (distance < MINIMUM_DISTANCE) return;

        // Le cout se paie sans verification, comme le consumeWithForce de l'original :
        // la portee a deja ete bornee par la reserve, donc il ne peut pas la depasser.
        data.performForced(cpCost(data, distance), getOverloadCost(data));

        if (player.isPassenger()) {
            player.stopRiding();
        }
        player.teleportTo(destination.x, destination.y, destination.z);
        player.fallDistance = 0.0f;
        cn.academy.sound.AcademySounds.playFor(player, cn.academy.ModSounds.TP_TP, 0.5f);

        // 0,00018 par bloc, comme l'original : c'est la distance qui compte, pas le geste.
        data.addSkillExp(this, 0.00018f * (float) distance);
        TeleporterCategory.DIM_FOLDING_THEOREM.onTeleported(data);
    }

    /** A payer pour ce saut : le surcout fixe et le prix des blocs parcourus. */
    public float cpCost(AbilityData data, double distance) {
        return (float) (distance * cpPerBlock(data));
    }

    /**
     * Ou le joueur atterrirait apres {@code chargeTicks} ticks de visee.
     *
     * Portage de {@code getDest} : l'entite la plus proche l'emporte sur le bloc, le bloc
     * donne sa face regardee — avec le decalage qui fait atterrir sur la surface et non
     * dedans — et un regard dans le vide donne la portee maximale.
     */
    public Vec3 destination(Player player, AbilityData data, int chargeTicks) {
        float exp = data.getSkillExp(this);
        double reach = maxDistance(exp, data.getControlPoint(), chargeTicks, cpPerBlock(data));

        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(reach));

        BlockHitResult blockHit = player.level().clip(
                new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        EntityHitResult entityHit = findEntity(player, eye, end, reach);

        double blockDistance = blockHit.getType() == HitResult.Type.MISS
                ? Double.MAX_VALUE : blockHit.getLocation().distanceToSqr(eye);
        double entityDistance = entityHit == null
                ? Double.MAX_VALUE : entityHit.getLocation().distanceToSqr(eye);

        if (entityDistance < blockDistance && entityHit != null) {
            Entity target = entityHit.getEntity();
            return target.position().add(0, target.getEyeHeight(), 0);
        }
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            // Aux pieds, et non a hauteur de tete : c'est ce point que la marque montre, et c'est
            // celui-la que le saut doit donner. Voir LandingSite.onFeet.
            return LandingSite.onFeet(blockHit.getDirection(), blockHit.getLocation(),
                    blockHit.getBlockPos(), pos -> !player.level().isEmptyBlock(pos),
                    player.getEyeHeight());
        }
        return end;
    }

    @Nullable
    private static EntityHitResult findEntity(Player player, Vec3 eye, Vec3 end, double reach) {
        AABB box = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
        return ProjectileUtil.getEntityHitResult(player, eye, end, box,
                e -> !e.isSpectator() && e.isPickable() && e != player, reach * reach);
    }
}
