package cn.academy.ability.electromaster;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Competence tenue, portage de MagMovement : le joueur s'accroche a ce qu'il vise de
 * metallique et se fait tirer dessus.
 *
 * <p>C'est le deplacement de l'electromaster : viser un rail, un bloc de fer ou un
 * wagonnet a vingt-cinq blocs, tenir la touche, et partir. La traction monte
 * progressivement — chaque tick rapproche la vitesse de la direction voulue de
 * {@code ACCEL} — donc le depart est mou et l'arrivee rapide, comme dans l'original.
 *
 * <p>La cible est <b>fixee a l'appui</b> : un bloc reste un point, une entite continue
 * d'etre suivie, et si elle meurt le maintien s'arrete. L'experience se paie a la
 * distance parcourue, avec un minimum pour un trajet trop court.
 */
public class MagMovementSkill extends Skill {

    /** Rapprochement de la vitesse par tick, comme {@code ACCEL}. */
    public static final double ACCEL = 0.08;

    /** Vitesse visee, en blocs par tick, comme {@code velocity}. */
    private static final double VELOCITY = 1.0;

    /** Portee de la visee, comme l'original. */
    private static final double RANGE = 25.0;

    public MagMovementSkill() {
        super("mag_movement", 2);
    }

    /** Portee de la visee, comme l'original : vingt-cinq blocs, quel que soit le niveau. */
    public static double getMaxDistance() {
        return RANGE;
    }

    /**
     * Experience gagnee pour un trajet : 0,0011 par bloc, avec un minimum de 0,005.
     *
     * L'original mettait ce plancher pour qu'un trajet de quelques blocs ne rapporte pas
     * rien du tout.
     */
    public static float getExpIncr(double distance) {
        return Math.max(0.005f, 0.0011f * (float) distance);
    }

    /** Cout par tick : 15 a 8, comme l'original. */
    public float cpPerTick(AbilityData data) {
        return lerp(15f, 8f, data.getSkillExp(this));
    }

    /** Surcout d'ouverture : de 60 a 30, comme l'original. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(60f, 30f, data.getSkillExp(this));
    }

    /** La traction ne coute rien a l'ouverture : c'est l'entretien qui paie. */
    @Override
    public float getCpCost() {
        return 0f;
    }

    @Override
    public boolean isHeld() {
        return true;
    }

    /**
     * Tenir ne demande plus rien a viser.
     *
     * <p>ECART ASSUME, demande du joueur : son laser se voit quoi qu'il regarde, meme dans le
     * vide. L'original, lui, terminait la competence des le {@code MSG_MADEALIVE} quand son
     * rayon ne trouvait ni bloc ni entite metallique, donc sans rien facturer. Ici la visee est
     * relue a chaque tick, et c'est la <b>depense</b> qui exige du metal : voir
     * {@link #onHoldTick}. L'experience se paie au trajet, elle aussi seulement quand on tire.
     */
    @Override
    public boolean canStart(Player player, AbilityData data) {
        return true;
    }

    @Override
    public void onStart(Player player, AbilityData data) {
        data.setHeldOverload(this, data.getOverload());
        data.setHoldOrigin(this, player.position());
    }

    /**
     * Un tick de traction : la cible est relue, et rien n'est paye quand il n'y en a pas.
     *
     * <p>Le port fixait sa cible a l'appui, comme l'original. Le joueur veut autre chose :
     * viser du metal paie — CP, surcout, experience du trajet — et viser autre chose ne paie
     * rien du tout, sans interrompre le maintien ni eteindre le laser. La position du tick
     * precedent se garde dans le point de maintien, faute d'un autre endroit ou la mettre :
     * c'est ce qui permet de ne payer l'experience qu'au trajet reellement parcouru.
     */
    @Override
    public boolean onHoldTick(Player player, AbilityData data, int heldTicks) {
        Vec3 previous = data.getHoldPoint(this);
        data.setHoldPoint(this, player.position());

        Anchor target = findTarget(player, data);
        if (target == null) return true;

        if (!data.consumeControlPoint(cpPerTick(data))) return false;

        Vec3 anchor = target.entity() != null
                ? target.entity().position().add(0, target.entity().getEyeHeight(), 0)
                : target.point();
        pull(player, anchor);

        if (previous != null) {
            data.addSkillExp(this, getExpIncr(previous.distanceTo(player.position())));
        }
        return true;
    }

    /**
     * La fin du maintien : rien a payer, tout s'est paye au tick.
     *
     * <p>L'original donnait son experience d'un coup, a la fin, sur la distance du trajet. Le
     * port la donne a chaque tick ou la traction tire vraiment, ce qui revient au meme quand on
     * vise du metal — et a rien du tout quand on ne vise rien.
     */
    @Override
    public void onHoldEnd(Player player, AbilityData data, int heldTicks) {
        player.fallDistance = 0.0f;
    }

    /**
     * La cible du maintien : l'entite suivie si elle vit encore, sinon le point vise.
     *
     * Renvoie {@code null} quand il n'y a plus rien a quoi s'accrocher : une entite
     * morte ou disparue termine le maintien, comme le {@code !target.alive()} de
     * l'original.
     */
    private static Vec3 resolveAnchor(Player player, AbilityData data, int targetId, Vec3 point) {
        if (targetId != 0) {
            Entity target = player.level().getEntity(targetId);
            // Une entite morte ou disparue termine le maintien, comme le
            // `!target.alive()` de l'original.
            if (target == null || !target.isAlive()) return null;
            return target.position().add(0, target.getEyeHeight(), 0);
        }
        return point;
    }

    /** Traction : la vitesse se rapproche de la direction voulue, d'un pas a la fois. */
    private static void pull(Player player, Vec3 anchor) {
        Vec3 want = wantedVelocity(player.position(), anchor);
        if (want == null) return;

        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(
                approach(motion.x, want.x),
                approach(motion.y, want.y),
                approach(motion.z, want.z));
        // Le client doit accepter cette vitesse : sans cela il la corrigerait au tick
        // suivant, et la traction ne se verrait pas.
        player.hurtMarked = true;
        player.fallDistance = 0.0f;
    }

    /**
     * Vitesse visee : la direction de l'ancre, a la norme de {@code VELOCITY}.
     *
     * C'est le {@code delta / (distance / velocity)} de l'original. Renvoie
     * {@code null} quand le joueur est deja sur l'ancre, ou la direction n'existe pas.
     */
    @Nullable
    public static Vec3 wantedVelocity(Vec3 position, Vec3 anchor) {
        Vec3 delta = anchor.subtract(position);
        double length = delta.length();
        if (length < 1.0E-4) return null;
        return delta.scale(VELOCITY / length);
    }

    /**
     * Rapproche une composante de la valeur voulue, d'au plus {@code ACCEL}.
     *
     * Portage de {@code tryAdjust} : c'est ce qui rend le depart mou et l'arrivee
     * rapide, au lieu d'une vitesse posee d'un coup.
     */
    public static double approach(double from, double to) {
        double delta = to - from;
        if (Math.abs(delta) < ACCEL) return to;
        return delta > 0 ? from + ACCEL : from - ACCEL;
    }

    /** Ce que la visee a trouve : une entite, ou un point sur un bloc. */
    private record Anchor(Vec3 point, Entity entity) {}

    /**
     * Cherche ce que le joueur vise.
     *
     * Reprend la regle de {@code toTarget} : un bloc doit etre metallique, et les blocs
     * faiblement metalliques demandent soixante pour cent d'experience ; une entite doit
     * figurer dans la liste des entites metalliques. Le premier des deux trouve est
     * retenu, la visee passant par les entites avant les blocs.
     */
    private Anchor findTarget(Player player, AbilityData data) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(RANGE));

        AABB box = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player, eye, end, box, e -> !e.isSpectator() && e.isPickable() && e != player, RANGE * RANGE);
        if (entityHit != null && MetalTargets.isMetallic(entityHit.getEntity())) {
            return new Anchor(null, entityHit.getEntity());
        }

        BlockHitResult blockHit = player.level().clip(
                new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            BlockState state = player.level().getBlockState(blockHit.getBlockPos());
            if (MetalTargets.canHook(state.getBlock(), data.getSkillExp(this))) {
                return new Anchor(blockHit.getLocation(), null);
            }
        }
        return null;
    }
}
