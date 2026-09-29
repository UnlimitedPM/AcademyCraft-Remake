package cn.academy.ability.electromaster;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.core.BlockPos;
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

    /**
     * Surcout d'un bloc SUPPLEMENTAIRE de la lignee : 10, et 5 quand la competence est a fond.
     *
     * <p>C'est la seule depense que le joueur a voulue au-dela de celles de l'original : le
     * premier bloc se paie comme dans le vrai mod, chaque nouveau bloc se paie dix de surcout
     * — cinq une fois la competence remplie — pour le dixieme de pourcent qu'il rapporte.
     */
    public static float overloadPerNewBlock(float exp) {
        return lerp(10f, 5f, exp);
    }

    /** Experience d'un bloc supplementaire de la lignee : un dixieme de pourcent. */
    public static final float EXP_PER_NEW_BLOCK = 0.001f;

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
     * <p>ECART ASSUME, demande du joueur : son laser se voit meme dans le vide, et le maintien
     * s'ouvre tout de suite — l'original, lui, refusait de naitre sans cible. Sans ancre, rien
     * ne se paie non plus : c'est le prix de ce confort.
     *
     * <p>Une fois accroche, en revanche, tout est au vrai mod : le maintien se paie a chaque
     * tick, que le regard suive le bloc ou non — voir {@link #onHoldTick}.
     */
    @Override
    public boolean canStart(Player player, AbilityData data) {
        return true;
    }

    @Override
    public void onStart(Player player, AbilityData data) {
        // Rien n'est epingle tant qu'on n'est pas accroche : sans ancre, le surcout doit pouvoir
        // redescendre — l'original n'avait meme pas de contexte dans ce cas.
        data.setHeldOverload(this, 0f);
        data.setHoldOrigin(this, player.position());
        // La lignee repart de zero : c'est ce qui fait qu'il n'y a qu'un seul premier bloc par
        // activation, comme le joueur l'a demande. Le bloc suivant redevient le premier.
        data.clearHoldLineage(this);
        // Aucune ancre au depart, et c'est voulu : le maintien s'ouvre sans rien viser.
        data.setHoldTarget(this, 0);
        data.setHoldPoint(this, null);
    }

    /**
     * Un tick de traction : la lignee s'agrandit, et le maintien se paie tant qu'il tient.
     *
     * <p>Le partage des roles, tel que le joueur l'a demande :
     *
     * <ul>
     * <li><b>le premier bloc de la lignee</b> — le premier metal accroche depuis l'activation —
     *     se paie exactement comme dans l'original : {@link #cpPerTick} a chaque tick, et le
     *     surcout de {@link #getOverloadCost} pose a l'ouverture, qui ne redescend plus tant que
     *     la prise tient. Sa recompense est celle de l'original, versee a la fin : le trajet
     *     entier, avec son plancher de 0,5 pour cent — voir {@link #onHoldEnd} ;</li>
     * <li><b>chaque nouveau bloc</b> ajoute {@link #overloadPerNewBlock} de surcout et
     *     {@link #EXP_PER_NEW_BLOCK} d'experience, une seule fois : la lignee se souvient des
     *     blocs deja pris ;</li>
     * <li><b>sans metal devant</b>, l'ancre gardee continue de tirer et de se payer. C'est le
     *     vrai mod, et c'est ce que le joueur a vu manquer : une fois accroche, detourner les
     *     yeux ne doit rien arreter ;</li>
     * <li><b>sans ancre du tout</b>, le maintien tient, le laser se voit, et il ne se paie rien.
     *     </li>
     * </ul>
     */
    @Override
    public boolean onHoldTick(Player player, AbilityData data, int heldTicks) {
        Anchor aimed = findTarget(player, data);
        if (aimed != null) {
            if (aimed.entity() != null) {
                data.setHoldTarget(this, aimed.entity().getId());
                data.setHoldPoint(this, null);
            } else {
                data.setHoldTarget(this, 0);
                data.setHoldPoint(this, aimed.point());
                enterLineage(data, aimed.block());
            }
            // L'overloadKeep de l'original : pose juste apres l'accrochage, il gele le surcout
            // tant que la prise tient.
            data.setHeldOverload(this, data.getOverload());
        }

        Vec3 anchor = resolveAnchor(player, data, data.getHoldTargetId(this), data.getHoldPoint(this));
        if (anchor == null) {
            // Rien a quoi s'accrocher : rien n'est paye, et le surcout repart en recuperation.
            data.setHeldOverload(this, 0f);
            return true;
        }

        // La traction passe AVANT le paiement. L'original payait puis terminait — mais son
        // client tirait de son cote, donc le dernier tick tirait quand meme. Ici tout passe par
        // le serveur : payer d'abord laissait le tick d'epuisement sans traction, ce que le
        // joueur a lu comme une competence qui ne fait rien sur un bloc de fer.
        pull(player, anchor);

        // Les vraies donnees de l'original : tant qu'on est accroche, le maintien se paie a
        // chaque tick, meme quand le regard a quitte le bloc.
        return data.consumeControlPoint(cpPerTick(data));
    }

    /**
     * Compte un bloc dans la lignee du maintien.
     *
     * Le premier bloc ne coute rien de plus — c'est celui du vrai mod, recompense a la fin.
     * Les suivants paient leur surcout et versent leur dixieme de pourcent, une seule fois
     * chacun : reviser un bloc deja pris ne repaie rien.
     */
    private void enterLineage(AbilityData data, BlockPos pos) {
        if (pos == null) return;
        if (!data.addHoldLineageBlock(this, pos.asLong())) return;
        if (data.getHoldLineageSize(this) <= 1) return;
        data.perform(0f, overloadPerNewBlock(data.getSkillExp(this)));
        data.addSkillExp(this, EXP_PER_NEW_BLOCK);
    }

    /**
     * La fin du maintien : la recompense du premier bloc, celle de l'original.
     *
     * <p>C'est le {@code s_onEnd} de {@code MagMovement.scala} : le trajet entier, mesure du
     * point de depart a l'arrivee, paye une fois, avec son plancher de 0,5 pour cent
     * ({@code 0.005}). Le joueur l'a voulu pour le premier bloc <b>uniquement</b> : les blocs
     * suivants de la lignee se sont deja payes en route, un dixieme de pourcent chacun.
     *
     * <p>Rien n'est verse quand le maintien n'a rien accroche : viser le ciel ne rapporte pas
     * d'experience, comme il ne coute rien.
     *
     * <p>L'etat du maintien vit encore : {@code AbilityEvents.endHeld} appelle cet effet AVANT
     * de l'oublier, comme l'original le faisait depuis son contexte mourant.
     */
    @Override
    public void onHoldEnd(Player player, AbilityData data, int heldTicks) {
        player.fallDistance = 0.0f;
        if (data.getHoldLineageSize(this) <= 0 && data.getHoldTargetId(this) == 0) return;
        Vec3 origin = data.getHoldOrigin(this);
        if (origin == null) return;
        data.addSkillExp(this, getExpIncr(origin.distanceTo(player.position())));
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

    /**
     * Ce que la visee a trouve : une entite, ou un point sur un bloc.
     *
     * {@code block} n'est renseigne que pour un bloc : c'est la position qui entre dans la
     * lignee du maintien. Une entite n'y entre pas — le joueur a parle de blocs.
     */
    private record Anchor(Vec3 point, Entity entity, BlockPos block) {}

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
            return new Anchor(null, entityHit.getEntity(), null);
        }

        BlockHitResult blockHit = player.level().clip(
                new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            BlockState state = player.level().getBlockState(blockHit.getBlockPos());
            if (MetalTargets.canHook(state.getBlock(), data.getSkillExp(this))) {
                return new Anchor(blockHit.getLocation(), null, blockHit.getBlockPos());
            }
        }
        return null;
    }
}
