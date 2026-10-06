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
 * <p>Et les <b>couleurs</b> sont separees, ce qui ne s'invente pas : les boules prennent toutes le
 * <b>rose</b> des bords du nuanceur, et le <b>bleu</b> est un coeur pose a part, au centre. Chez
 * l'original, la ou le nuanceur bleuissait, c'est le centre du <b>volume</b> — la ou les boules
 * s'entassent et ou la densite monte — et aucune boule prise a part n'est bleue. Voir
 * {@link #CORE_RADIUS}, qui raconte comment le joueur l'a fait voir.
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
     * L'echelle du rayon d'une boule.
     *
     * <p>Le rayon visible d'une boule n'est <b>pas</b> proportionnel a sa taille : le nuanceur de
     * l'original empilait des densites en {@code taille / distance carre}, donc une boule se voit
     * jusqu'a la distance ou cette densite passe sous le seuil de l'oeil — c'est a dire jusqu'a
     * <b>racine de sa taille</b>. Un facteur direct rendrait les petites boules invisibles (deux
     * dixiemes de bloc) et les grosses enormes ; la racine les tient toutes les deux dans le champ,
     * ce que l'original faisait : ses petites boules se voyaient comme des points denses.
     */
    public static final double RADIUS_SCALE = 2.0;

    /**
     * L'opacite d'une boule, en part de celle du corps.
     *
     * <p>Le melange est <b>normal</b> — celui de l'original, qui composait son volume en alpha —
     * donc les boules s'empilent au lieu de s'additionner. C'est ce qui garde leurs couleurs :
     * en melange ajoute, un rose et un bleu superposes font du BLANC, et le joueur n'a vu que ca.
     *
     * <p>La valeur est haute parce qu'une boule seule ne fait PAS le corps : il faut trois ou
     * quatre recouvrements pour que le coeur devienne opaque, comme chez l'original, dont le
     * marcheur de rayon empilait vingt pas et atteignait le plein des les premieres couches.
     */
    public static final float DRAW_ALPHA = 0.6f;

    /**
     * La couverture d'une boule a cette opacite de corps.
     *
     * <p>C'est le facteur du nuanceur, mot pour mot : il terminait par
     * {@code alpha * (0.5 + 0.5 * alpha)}, donc le corps n'etait <b>jamais</b> terne, meme a mi-charge.
     * Le port multipliait par la seule opacite, et le joueur a vu le resultat — « tellement
     * transparent que c'est a peine si j'arrive a le voir ».
     *
     * <p>C'est cette courbe qui disparait a la naissance, et non le rayon : voir {@link #growth},
     * qui fait naitre les boules de rien. Les deux ensemble font la matiere qui se noue.
     */
    public static float coverage(float alpha) {
        return DRAW_ALPHA * (0.5f + 0.5f * Math.min(1f, Math.max(0f, alpha)));
    }

    /**
     * La croissance d'une boule, en part de son rayon.
     *
     * <p>L'opacite du nuanceur multipliait sa densite : {@code alpha * taille / distance carre}.
     * Une boule se voit donc jusqu'a la distance ou cette densite passe sous le seuil de l'oeil,
     * c'est a dire jusqu'a {@code racine de alpha} — elle <b>grossit en naitre</b>, et elle
     * <b>retrecit en mourir</b>. Sans cela, les boules du port apparaissent et disparaissent a
     * taille pleine, ce qui se lit comme un allumage et non comme de la matiere qui se noue.
     */
    public static double growth(float alpha) {
        return Math.sqrt(Math.min(1.0, Math.max(0.0, alpha)));
    }

    /** Le rayon dessine d'une boule a cet instant : racine de sa taille, et croissance comprise. */
    public static double visibleRadius(double size, float alpha) {
        return RADIUS_SCALE * Math.sqrt(Math.max(0.0, size)) * growth(alpha);
    }

    /**
     * Les deux bouts de la couleur du nuanceur : le rose des bords, le bleu du coeur.
     *
     * <p>Et c'est la <b>densite</b> qui choisissait entre les deux : {@code 1 - densite/2}, donc
     * rose la ou la matiere est clairsemee, bleu la ou elle s'entasse. L'original en faisait un
     * degrade continu.
     */
    public static final float EDGE_RED = 0.98f;
    public static final float EDGE_GREEN = 0.51f;
    public static final float EDGE_BLUE = 0.92f;
    public static final float CORE_RED = 0.43f;
    public static final float CORE_GREEN = 0.74f;
    public static final float CORE_BLUE = 1.0f;

    /**
     * Le rayon du coeur bleu, une fois le corps noue.
     *
     * <p>C'est une PIECE AJOUTEE, et elle vient du joueur : devant une capture du vrai mod, il a
     * dit que <b>aucune boule n'y est bleue</b>. Il a raison — la ou le nuanceur bleuissait, c'est
     * le centre du VOLUME, la ou les boules s'entassent et ou la densite monte. Aucune boule prise
     * a part n'est bleue, et le port les peignait une a une : il en sortait quatre grosses boules
     * bleues, qui n'existent pas.
     *
     * <p>Le port dessine donc la meme chose qu'elles, mais <b>a part</b> : toutes les boules au rose
     * des bords, et un coeur bleu par-dessus, au centre et a cette taille-la — un peu moins de la
     * moitie de ce que l'essaim occupe.
     */
    public static final double CORE_RADIUS = 2.2;

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

    /** La couleur d'une boule : le ROSE des bords du nuanceur, la seule qui se voie. */
    public static float[] ballColor() {
        return new float[] { EDGE_RED, EDGE_GREEN, EDGE_BLUE };
    }

    /** Et celle du coeur : le bleu, la ou la densite monte. */
    public static float[] coreColor() {
        return new float[] { CORE_RED, CORE_GREEN, CORE_BLUE };
    }

    /** Le rayon du coeur a cet instant : une part de sa taille, et la croissance du corps. */
    public static double coreRadius(float alpha) {
        return CORE_RADIUS * growth(alpha);
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
