package cn.academy.ability.client.vm;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Le corps de plasma du canon, portage de {@code PlasmaBodyEffect}.
 *
 * <p>Ce que le canon noue au-dessus de la tete n'est pas une boule mais un <b>essaim</b> : l'original
 * tirait une dizaine de boules, leur donnait a chacune un balancement a deux axes, et son nuanceur
 * les relisait en un seul volume — une vingtaine de pas de marche de rayon par pixel, une densite
 * en {@code 1 / distance} au carre, et une couleur qui va du rose des bords au bleu du coeur.
 *
 * <p>Sans nuanceur, le port ne peut pas refaire ce volume : il en dessine la <b>structure</b>, c'est
 * a dire les boules elles-memes, chacune avec son balancement et sa couleur. C'est une approximation,
 * et elle a un merite : ce sont bien des boules de plasma qu'on voit, et non une tache floue.
 *
 * <h2>Les deux familles, et leurs nombres</h2>
 *
 * <p>L'original en tirait deux sortes, et ce sont elles qui font le volume :
 *
 * <ul>
 *   <li><b>quatre grosses</b> — de 1 a 1,5 de rayon, posees dans un cube de plus ou moins 1,5 bloc,
 *       qui balancent de plus ou moins 1,4 a 2 blocs. Ce sont elles qui font le coeur ;</li>
 *   <li><b>quatre ou cinq petites</b> — de 0,1 a 0,3 seulement, mais lancees de plus ou moins 3,5 a
 *       5 blocs : elles tournent <b>loin</b> autour du corps, et ce sont elles qui font la peripherie.
 *       L'original leur donnait une amplitude deux fois et demie plus grande, ce qui compense
 *       exactement leur taille minuscule.</li>
 * </ul>
 *
 * <p>Le balancement se lit sur deux <b>phases</b> separees, une par axe : {@code x} et {@code z}
 * suivent la meme phase en sinus et cosinus, donc la boule tourne en rond a l'horizontale, tandis que
 * {@code y} suit la sienne et la fait monter et descendre. Deux vitesses differentes a chaque fois :
 * aucune boule ne repasse par ou elle est deja passee.
 *
 * <h2>Sa vie, et sa couleur</h2>
 *
 * <p>Son opacite monte de trois dixiemes par seconde — elle est donc encore en train de monter quand
 * le tir part, c'est voulu — et redescend d'un coup quand elle meurt, en une seconde. Ces deux
 * nombres sont des <b>secondes</b>, comme chez l'original, dont l'horloge comptait en secondes.
 *
 * <p>La couleur d'une boule se lit sur sa <b>taille</b> : les grosses sont au coeur et prennent le
 * bleu, les petites tournent a la peripherie et prennent le rose. C'est la meme direction que le
 * nuanceur de l'original, qui bleuissait la ou la densite montait.
 *
 * <p>Tout est <b>pur</b> — ni Minecraft ni horloge — donc verifiable en JUnit.
 */
public final class PlasmaBodyVisuals {

    /** Les deux familles, telles que l'original les tirait. */
    public static final int BIG_COUNT = 4;
    public static final double BIG_SIZE_MIN = 1.0;
    public static final double BIG_SIZE_MAX = 1.5;
    public static final double BIG_OFFSET = 1.5;

    /** {@code rangei(4, 6)} : quatre ou cinq, la borne haute exclue comme toujours. */
    public static final int SMALL_COUNT_MIN = 4;
    public static final int SMALL_COUNT_MAX = 5;
    public static final double SMALL_SIZE_MIN = 0.1;
    public static final double SMALL_SIZE_MAX = 0.3;
    public static final double SMALL_OFFSET = 3.0;
    public static final double SMALL_AMPLITUDE = 2.5;

    /** Le balancement : amplitude 1,4 a 2 (x la taille de la famille), vitesse 0,5 a 0,7. */
    public static final double AMPLITUDE_MIN = 1.4;
    public static final double AMPLITUDE_MAX = 2.0;
    public static final double SPEED_MIN = 0.5;
    public static final double SPEED_MAX = 0.7;

    /** La montee de l'opacite, en trois dixiemes par seconde, et sa chute, en une seconde. */
    public static final double RISE_PER_SECOND = 0.3;
    public static final double FADE_PER_SECOND = 1.0;

    /**
     * Le rayon dessine d'une boule, en multiple de sa taille.
     *
     * <p>Le nuanceur de l'original n'avait pas de bord : sa densite decroissait en {@code 1/d}
     * au carre, donc une boule se voyait jusqu'a deux a trois fois son rayon. C'est ce que ce
     * facteur reprend — sans lui, les boules du port seraient des pastilles.
     */
    public static final double DRAW_SCALE = 2.2;

    /**
     * L'opacite d'une boule, en part de celle du corps.
     *
     * <p>Le dessin <b>ajoute</b> sa lumiere : c'est une approximation du volume de l'original, et
     * elle demande un facteur. A pleine opacite, une dizaine de boules superposees saturent en blanc
     * pur — le coeur bleu y perdrait sa couleur. A un quart, l'ecart entre le centre et la
     * peripherie se lit encore, et les boules qui se chevauchent se fondent au lieu de s'empiler.
     */
    public static final float DRAW_ALPHA = 0.25f;

    /** Les deux bouts de la couleur du nuanceur : le rose des bords, le bleu du coeur. */
    public static final float EDGE_RED = 0.98f;
    public static final float EDGE_GREEN = 0.51f;
    public static final float EDGE_BLUE = 0.92f;
    public static final float CORE_RED = 0.43f;
    public static final float CORE_GREEN = 0.74f;
    public static final float CORE_BLUE = 1.0f;

    /** Un balancement tire : son amplitude, sa vitesse, et son retard de phase. */
    public record Trig(double amplitude, double speed, double phase) {

        /** La phase a un instant donne : {@code vitesse * temps - retard}. */
        public double at(double seconds) {
            return speed * seconds - phase;
        }
    }

    /** Une boule : sa taille, son centre dans le corps, et ses deux balancements. */
    public record Ball(double size, double cx, double cy, double cz, Trig horizontal, Trig vertical) {
    }

    private PlasmaBodyVisuals() {
    }

    /**
     * L'essaim de l'original, tire une fois pour toutes a la naissance du corps.
     *
     * <p>L'ordre des tirages est celui de l'original — les quatre grosses d'abord, et pour chacune
     * sa taille, les trois composantes de son centre, puis les trois nombres de chacun de ses deux
     * balancements. Un simple tirage, mais il decide de la forme : le meme se reproduit.
     */
    public static List<Ball> roll(RandomSource random) {
        List<Ball> balls = new ArrayList<>(BIG_COUNT + SMALL_COUNT_MAX);

        for (int i = 0; i < BIG_COUNT; i++) {
            balls.add(new Ball(
                    range(random, BIG_SIZE_MIN, BIG_SIZE_MAX),
                    range(random, -BIG_OFFSET, BIG_OFFSET),
                    range(random, -BIG_OFFSET, BIG_OFFSET),
                    range(random, -BIG_OFFSET, BIG_OFFSET),
                    trig(random, 1.0),
                    trig(random, 1.0)));
        }

        int small = SMALL_COUNT_MIN + random.nextInt(SMALL_COUNT_MAX - SMALL_COUNT_MIN + 1);
        for (int i = 0; i < small; i++) {
            balls.add(new Ball(
                    range(random, SMALL_SIZE_MIN, SMALL_SIZE_MAX),
                    range(random, -SMALL_OFFSET, SMALL_OFFSET),
                    range(random, -SMALL_OFFSET, SMALL_OFFSET),
                    range(random, -SMALL_OFFSET, SMALL_OFFSET),
                    trig(random, SMALL_AMPLITUDE),
                    trig(random, SMALL_AMPLITUDE)));
        }

        return balls;
    }

    /**
     * Ou se trouve une boule a cet instant, par rapport au centre du corps.
     *
     * <p>L'horizontale tourne : {@code x} est un sinus de sa phase, {@code z} le cosinus de la
     * <b>meme</b> phase, donc la boule decrit un cercle. La verticale suit l'autre phase, a sa
     * propre vitesse.
     */
    public static Vec3 offset(Ball ball, double seconds) {
        double horizontal = ball.horizontal().at(seconds);
        double vertical = ball.vertical().at(seconds);
        return new Vec3(
                ball.cx() + ball.horizontal().amplitude() * Math.sin(horizontal),
                ball.cy() + ball.vertical().amplitude() * Math.sin(vertical),
                ball.cz() + ball.horizontal().amplitude() * Math.cos(horizontal));
    }

    /** L'opacite du corps a cet age-la, en secondes : elle monte, et se plafonne a un. */
    public static float alpha(double aliveSeconds) {
        return (float) Math.min(1.0, Math.max(0.0, aliveSeconds) * RISE_PER_SECOND);
    }

    /**
     * L'opacite du corps mourant : celle qu'il avait en mourant, moins une seconde de chute.
     *
     * <p>L'original posait une valeur et la rapprochait de zero d'autant par seconde ; c'est la
     * meme chose, ecrite d'un coup.
     */
    public static float fading(float alphaAtDeath, double dyingSeconds) {
        return (float) Math.max(0.0, alphaAtDeath - Math.max(0.0, dyingSeconds) * FADE_PER_SECOND);
    }

    /** La quantite de bleu d'une boule, de zero pour la plus petite a un pour la plus grosse. */
    public static float depth(double size) {
        double t = (size - SMALL_SIZE_MIN) / (BIG_SIZE_MAX - SMALL_SIZE_MIN);
        return (float) Math.min(1.0, Math.max(0.0, t));
    }

    /** Les trois canaux de sa couleur : rose a la peripherie, bleu au coeur. */
    public static float[] color(double size) {
        float t = depth(size);
        return new float[] {
                EDGE_RED + (CORE_RED - EDGE_RED) * t,
                EDGE_GREEN + (CORE_GREEN - EDGE_GREEN) * t,
                EDGE_BLUE + (CORE_BLUE - EDGE_BLUE) * t,
        };
    }

    /** Un tirage uniforme, du plus petit au plus grand : le {@code rangef} de l'original. */
    private static double range(RandomSource random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    /** Un balancement, dont l'amplitude est multipliee par la taille de sa famille. */
    private static Trig trig(RandomSource random, double scale) {
        return new Trig(
                range(random, AMPLITUDE_MIN, AMPLITUDE_MAX) * scale,
                range(random, SPEED_MIN, SPEED_MAX),
                range(random, 0.0, Math.PI * 2));
    }
}
