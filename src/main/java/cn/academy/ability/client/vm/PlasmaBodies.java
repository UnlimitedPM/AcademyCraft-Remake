package cn.academy.ability.client.vm;

import cn.academy.ability.Skill;
import cn.academy.ability.client.ClientCharge;
import cn.academy.ability.vecmanip.PlasmaCannonSkill;
import cn.academy.ability.vecmanip.VecmanipCategory;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Le corps de plasma du canon, chez le client : ce qui vit, et ou il est.
 *
 * <p>L'original en faisait une <b>entite cliente</b> que le contexte de la competence posait chez
 * son porteur, et il la deplacait lui-meme a chaque tick. Le port n'a pas d'entites d'effet : l'etat
 * vit ici, et {@code PlasmaBodyRenderer} le dessine — le meme partage que les tornades.
 *
 * <h2>Ce que le client sait, et ce qu'il ne sait pas</h2>
 *
 * <p>La boule nait a l'<b>appui</b> : le client ouvre sa charge, donc il sait tout — ou est le
 * porteur, et qu'il faut poser le corps quinze blocs au-dessus de sa tete. Il ne sait pas, en
 * revanche, ou le <b>tir</b> va : c'est le serveur qui vise, cent blocs devant le regard, sur un
 * bloc ou sur un corps. Un paquet le lui dit — {@code PlasmaShotPacket} — et il porte les
 * <b>deux</b> bouts, la position de la boule et sa destination : le client reprend donc le vol
 * exactement la ou le serveur en est, et les deux ne peuvent pas diverger.
 *
 * <p>A partir de la, le client mene la boule <b>tout seul</b>, d'un bloc par tick, avec le meme
 * calcul que le serveur. C'est ce que faisait l'original, qui rejouait son deplacement cote client
 * et se contentait d'une correction de position tous les cinq ticks. Le port n'a pas besoin de la
 * correction : il part de la position vraie, et il ne conduit qu'un dessin.
 *
 * <h2>Quand elle meurt</h2>
 *
 * <p>Le client s'arrete aux memes bornes que le serveur — un mur en travers, la destination
 * atteinte, ou les quatre cents ticks du vol — et il s'efface alors en une seconde. Quand le tir
 * n'a pas lieu, le corps ne reste pas plante la : la fermeture de la charge lui laisse le temps
 * d'un message, puis l'oublie.
 *
 * <p>Rien de tout cela n'a de sens hors du client : la classe ne se charge que la.
 */
@OnlyIn(Dist.CLIENT)
public final class PlasmaBodies {

    /** Les quatre temps du corps : rien, la charge, le vol, et l'effacement. */
    public enum Phase { NONE, CHARGE, FLY, DYING }

    /**
     * Ce qu'on laisse a un tir annonce pour arriver.
     *
     * <p>Un relachement ferme la charge chez le client un ou deux ticks avant que le serveur n'ait
     * pu repondre. Sans cette attente, le corps partirait en fumee juste avant le message qui le
     * fait voler.
     */
    public static final int SHOT_GRACE_TICKS = 20;

    /** Le tirage des boules : un essaim se tire une fois, a sa naissance. */
    private static final RandomSource RANDOM = RandomSource.create();

    private static Phase phase = Phase.NONE;
    private static List<PlasmaBodyVisuals.Ball> balls = List.of();
    private static Vec3 previous = Vec3.ZERO;
    private static Vec3 position = Vec3.ZERO;
    private static Vec3 destination = Vec3.ZERO;
    private static int ageTicks;
    private static int flightTicks;
    private static int dyingTicks;
    private static int graceTicks;
    private static float alphaAtDeath;
    private static double scale = 1.0;

    private PlasmaBodies() {
    }

    /** L'etat du corps : rien, la charge, le vol, ou l'effacement. */
    public static Phase phase() {
        return phase;
    }

    /** L'essaim en vie, ses balancements compris. */
    public static List<PlasmaBodyVisuals.Ball> balls() {
        return balls;
    }

    /**
     * L'echelle du corps : un a ciel ouvert, moins sous un plafond.
     *
     * <p>C'est celle de la colonne du canon (voir {@code TornadoVisuals.cannonScale}), et elle est
     * figee a la naissance : le corps garde la taille qu'il a prise en se nouant, meme quand la
     * boule part ensuite au grand jour. Les rayons et les ecarts la suivent tous les deux — les
     * ecarts dans l'essaim, les rayons au moment du dessin. Le joueur l'a demande : « il faudrait
     * aussi un peu reduire la taille des boules pour que ce soit dans le meme ordre que la grande
     * tornade ».
     */
    public static double scale() {
        return scale;
    }

    /** Ou est le corps, vue par une image : l'entre-deux d'un vol, pour qu'il ne saute pas. */
    public static Vec3 centre(float partialTick) {
        return previous.add(position.subtract(previous).scale(partialTick));
    }

    /** Son age en secondes, vue par une image : c'est lui qui mene les balancements. */
    public static double ageSeconds(float partialTick) {
        return (ageTicks + partialTick) / 20.0;
    }

    /** Son opacite du moment. */
    public static float alpha() {
        return phase == Phase.DYING
                ? PlasmaBodyVisuals.fading(alphaAtDeath, dyingTicks / 20.0)
                : PlasmaBodyVisuals.alpha(ageTicks / 20.0);
    }

    /**
     * La charge du canon est ouverte : c'est elle qui fait naitre le corps.
     *
     * <p>C'est SA charge qui est lue, et non « la charge en cours » : le joueur peut charger le
     * canon et tenir autre chose — une veille, les ailes — en meme temps. Voir ClientCharge.
     */
    private static boolean charging() {
        return ClientCharge.isOpen(VecmanipCategory.PLASMA_CANNON.getName());
    }

    /** Un tick du client : c'est ici que le corps nait, avance, et s'en va. */
    public static void tick() {
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return;

        if (phase == Phase.NONE) {
            // Le corps nait a l'appui, et c'est la charge du canon qui le dit : le client ouvre la
            // sienne lui-meme, donc il n'a rien a demander a personne.
            if (charging()) {
                begin(player);
            }
            return;
        }

        ageTicks++;

        if (phase == Phase.CHARGE) {
            // Le tir doit s'annoncer. Passe le delai, c'est qu'il n'a pas eu lieu.
            if (graceTicks > 0 && --graceTicks == 0) clear();
            return;
        }

        if (phase == Phase.FLY) {
            previous = position;
            Vec3 next = PlasmaCannonSkill.travel(previous, destination);
            // Un mur en travers arrete la boule, comme chez le serveur.
            boolean blocked = !next.equals(previous) && hitsBlock(player, previous, next);
            position = next;
            flightTicks++;

            if (blocked || PlasmaCannonSkill.arrived(next, destination)
                    || flightTicks >= PlasmaCannonSkill.FLIGHT_TICKS) {
                die();
            }
            return;
        }

        // L'effacement : une seconde, puis plus rien.
        dyingTicks++;
        if (alpha() <= 0f) clear();
    }

    /**
     * Le tir est parti : le serveur dit d'ou et jusqu'ou.
     *
     * <p>Le corps <b>reprend la position du serveur</b> au lieu de la sienne : les deux ont vecu le
     * meme vol, mais c'est le serveur qui decide, et une image d'ecart entre eux se verrait sur la
     * boule qui explose.
     */
    public static void shot(Vec3 position, Vec3 destination) {
        if (phase == Phase.NONE) {
            balls = PlasmaBodyVisuals.scaled(PlasmaBodyVisuals.roll(RANDOM), scale);
            ageTicks = 1;
        }

        phase = Phase.FLY;
        PlasmaBodies.previous = position;
        PlasmaBodies.position = position;
        PlasmaBodies.destination = destination;
        flightTicks = 0;
        graceTicks = 0;
    }

    /** La charge est refermee : le corps attend son tir, ou s'en va. */
    public static void end(Skill skill) {
        if (skill == VecmanipCategory.PLASMA_CANNON && phase == Phase.CHARGE) {
            graceTicks = SHOT_GRACE_TICKS;
        }
    }

    /** Tout oublier : le monde change, et plus rien ne tourne dans celui d'avant. */
    public static void clear() {
        phase = Phase.NONE;
        balls = List.of();
        previous = Vec3.ZERO;
        position = Vec3.ZERO;
        destination = Vec3.ZERO;
        ageTicks = 0;
        flightTicks = 0;
        dyingTicks = 0;
        graceTicks = 0;
        alphaAtDeath = 0f;
        scale = 1.0;
    }

    /**
     * La naissance : l'essaim se tire, et se pose quinze blocs au-dessus — ou sous le plafond.
     *
     * <p>La place qu'on y trouve decide de l'echelle de tout le corps : c'est le meme nombre que
     * celui de la colonne (voir {@code TornadoVisuals.cannonScale}), donc les deux ne peuvent pas
     * se contredire.
     */
    private static void begin(Player player) {
        scale = TornadoVisuals.cannonScale(PlasmaCannonSkill.groundGap(player));
        balls = PlasmaBodyVisuals.scaled(PlasmaBodyVisuals.roll(RANDOM), scale);
        previous = PlasmaCannonSkill.spawnPoint(player);
        position = previous;
        destination = previous;
        phase = Phase.CHARGE;
        ageTicks = 0;
        flightTicks = 0;
        dyingTicks = 0;
        graceTicks = 0;
    }

    /** Le corps meurt : son opacite se fige, et l'effacement commence. */
    private static void die() {
        alphaAtDeath = alpha();
        dyingTicks = 0;
        phase = Phase.DYING;
    }

    /** Un bloc sur le trajet, cote client : le meme arret que le serveur, au dessin pres. */
    private static boolean hitsBlock(Player player, Vec3 from, Vec3 to) {
        HitResult hit = player.level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() != HitResult.Type.MISS;
    }
}
