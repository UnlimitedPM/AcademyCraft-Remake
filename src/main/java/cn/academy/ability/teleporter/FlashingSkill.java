package cn.academy.ability.teleporter;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Scintillement, portage de {@code Flashing} : s'ouvrir a la teleportation courte, puis
 * sauter d'un bloc et demi dans la direction d'une des quatre touches de deplacement.
 *
 * <h2>Deux temps, comme le reacteur</h2>
 *
 * La competence s'ouvre en tenant sa touche — elle coute alors un surcout de 250 a 180 et
 * une poignee de CP — et vit de trois a sept secondes et demie. Pendant ce temps, chaque
 * direction est un saut : on <b>vise</b> en enfoncant W, A, S ou D, et on <b>part</b> en
 * relachant. L'original affichait un anneau a l'endroit ou l'on allait atterrir ; le port
 * n'a pas ce rendu, donc ces quatre touches sont a la fois la visee et le declencheur.
 *
 * <p>Les touches de deplacement ne sont <b>pas</b> detournees : on marche normalement tout
 * en visant, et sauter ne fait que deplacer d'un coup. C'est ce que faisait l'original, qui
 * ajoutait ses ecouteurs par-dessus ceux du jeu.
 *
 * <h2>Ce que coute un saut</h2>
 *
 * De 12 a 18 blocs selon l'experience, pour 13 a 6 CP chez l'original — soit un demi-point
 * a l'echelle du port. La vraie depense est ailleurs : l'ouverture charge la reserve d'un
 * quart, et la fin du maintien pose une recharge de <b>45 secondes</b> au depart, qui tombe
 * a 20 au maximum. Tenir le scintillement est donc un engagement, pas une habitude.
 *
 * <h2>Ou l'on arrive</h2>
 *
 * Le saut part des <b>yeux</b>, dans la direction de la touche, incline par le regard :
 * regarder en l'air fait monter, regarder au sol fait descendre. Le trajet s'arrete sur le
 * premier corps vivant ou la premiere face de bloc, et l'atterrissage se pose alors sur
 * cette face ({@link LandingSite}) : on ressort devant le mur, sous le plafond, ou dessus,
 * jamais dedans.
 */
public class FlashingSkill extends Skill {

    /** Les quatre directions, dans l'ordre de l'original. */
    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    public static final int FORWARD = 3;
    public static final int BACK = 4;

    /** Distance minimale d'un saut, en blocs, a l'experience minimale. */
    public static final double MIN_DISTANCE = 12.0;

    /** Distance maximale, a l'experience maximale. */
    public static final double MAX_DISTANCE = 18.0;

    /**
     * Duree de la suspension de gravite apres un saut, en ticks.
     *
     * Portage du {@code GravityCancellor} de l'original : deux secondes pendant lesquelles
     * la chute est presque annulee, ce qui rend le saut utilisable en l'air — sans quoi on
     * ne ferait que tomber un peu plus loin.
     */
    public static final int GRAVITY_SUSPENSION = 40;

    public FlashingSkill() {
        super("flashing", 5);
    }

    /** Distance d'un saut : de 12 a 18 blocs selon l'experience. */
    public double distance(AbilityData data) {
        return lerp((float) MIN_DISTANCE, (float) MAX_DISTANCE, data.getSkillExp(this));
    }

    /**
     * Cout d'un saut : 13 a 6 CP chez l'original, divises par 28.
     *
     * C'est le saut le moins cher du port, et de loin : la competence se paie a l'ouverture
     * et a la fermeture, pas a l'usage.
     */
    public float dashCost(AbilityData data) {
        return lerp(13f, 6f, data.getSkillExp(this));
    }

    /** Cout d'ouverture en CP : 80 a 60 chez l'original, divises par 28. */
    @Override
    public float getCpCost(AbilityData data) {
        return lerp(80f, 60f, data.getSkillExp(this));
    }

    /** Surcout d'ouverture : de 250 a 180, comme l'original. Epingle pour tout le maintien. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(250f, 180f, data.getSkillExp(this));
    }

    /** Duree du maintien : de trois secondes a sept secondes et demie, comme l'original. */
    @Override
    public int getMaxHoldTicks(AbilityData data) {
        return (int) lerp(60f, 150f, data.getSkillExp(this));
    }

    /**
     * Recharge : de 45 secondes a 20, comme l'original.
     *
     * C'est de loin la plus longue du port — dix fois celle du railgun — et elle se pose a
     * la <b>fin</b> du maintien, pas au saut : on peut enchainer les sauts tant qu'on tient
     * la touche, mais on paie l'attente apres.
     */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(900f, 400f, data.getSkillExp(this));
    }

    /** Chaque saut se paie et se compte : voir {@link #earnsExpOnEffect()}. */
    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    @Override
    public boolean isHeld() {
        return true;
    }

    /**
     * Le scintillement ecoute les quatre touches de deplacement : c'est le seul du port, et
     * c'est ce que dit ce drapeau au client, qui guette alors les touches du jeu.
     */
    @Override
    public boolean listensToDirections() {
        return true;
    }

    @Override
    public void onStart(Player player, AbilityData data) {
        // L'original epingle le surcout de l'ouverture pour tout le maintien
        // (`overloadKeep`) : sans cela, la reserve redescendrait pendant qu'on saute et
        // l'ouverture finirait par ne plus rien couter.
        data.setHeldOverload(this, data.getOverload());
    }

    /**
     * Un saut, dans la direction demandee.
     *
     * Portage du {@code serverPerform} de l'original : il paie, puis deplace. Le paiement
     * vient en premier et vaut refus : un saut qu'on ne peut pas payer ne deplace personne
     * et ne laisse rien derriere lui.
     */
    @Override
    public void onHoldAction(Player player, AbilityData data, int action) {
        if (!data.consumeControlPoint(dashCost(data))) return;

        Vec3 destination = destination(player, data, action);

        // L'original descendaient son porteur de sa monture : on ne se teleporte pas avec.
        if (player.isPassenger()) player.stopRiding();
        player.teleportTo(destination.x, destination.y, destination.z);
        player.fallDistance = 0.0f;
        cn.academy.sound.AcademySounds.playFor(player, cn.academy.ModSounds.TP_TP_FLASHING, 1.0f);

        // 0,002 par saut, comme l'original.
        data.addSkillExp(this, 0.002f);
        // Et deux secondes de chute presque annulee, le temps de se rattraper.
        data.suspendGravity(GRAVITY_SUSPENSION);
        // Une teleportation de plus pour le theoreme de repli, qui la compte.
        TeleporterCategory.DIM_FOLDING_THEOREM.onTeleported(data);
    }

    /**
     * La direction d'un saut, avant l'inclinaison du regard.
     *
     * Portage des deux rotations de l'original ({@code rotateAroundZ} puis {@code rotateYaw}
     * sur un vecteur unitaire) : le resultat est un vecteur horizontal dont le lacet suit le
     * regard, et qui s'incline du tangage. Fonction pure, donc verifiable : les quatre
     * directions, et leur miroir quand le joueur se retourne.
     */
    public static Vec3 dashDirection(Vec3 look, double pitchDegrees, int direction) {
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-6) {
            // Regard pile a la verticale : il n'y a plus d'horizon a suivre, l'original
            // prenait alors la direction du nord comme les autres competences.
            flat = new Vec3(0, 0, 1);
        }
        flat = flat.normalize();

        Vec3 side = switch (direction) {
            case LEFT -> new Vec3(flat.z, 0, -flat.x);
            case RIGHT -> new Vec3(-flat.z, 0, flat.x);
            case BACK -> flat.scale(-1);
            default -> flat;
        };

        double pitch = Math.toRadians(pitchDegrees);
        return side.scale(Math.cos(pitch)).add(0, -Math.sin(pitch), 0);
    }

    /**
     * Ou le saut fait atterrir.
     *
     * Le trajet part des yeux et va jusqu'a la portee de la competence, dans la direction
     * de la touche. Le premier corps vivant ou la premiere face de bloc l'arrete — les deux
     * sont compares par distance, comme dans l'original — et l'atterrissage se pose alors
     * sur ce qu'il a trouve. Faute de quoi on arrive au bout du saut, dans le vide.
     */
    public Vec3 destination(Player player, AbilityData data, int direction) {
        Vec3 from = player.position();
        Vec3 eyes = player.getEyePosition(1.0f);
        Vec3 end = eyes.add(dashDirection(player.getViewVector(1.0f), player.getXRot(), direction)
                .scale(distance(data)));

        BlockHitResult blockHit = player.level().clip(new ClipContext(from, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Entity target = TargetingUtil.findEntityAlong(player, from, end,
                e -> e instanceof LivingEntity);

        double blockDistance = blockHit.getType() == HitResult.Type.MISS
                ? Double.MAX_VALUE : blockHit.getLocation().distanceToSqr(from);
        double entityDistance = target == null
                ? Double.MAX_VALUE : target.position().distanceToSqr(from);

        if (entityDistance < blockDistance && target != null) {
            return target.position().add(0, target.getEyeHeight(), 0);
        }
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            // Aux pieds, et non a hauteur de tete : le scintillement se pose la ou l'on se tient,
            // comme la teleportation au marqueur. Voir LandingSite.onFeet.
            return LandingSite.onFeet(blockHit.getDirection(), blockHit.getLocation(),
                    blockHit.getBlockPos(), pos -> !player.level().isEmptyBlock(pos),
                    player.getEyeHeight());
        }
        return end;
    }

    /** Le saut tel que le paquet l'appelle : une direction, et rien d'autre. */
    public static boolean isDirection(int direction) {
        return direction >= LEFT && direction <= BACK;
    }
}
