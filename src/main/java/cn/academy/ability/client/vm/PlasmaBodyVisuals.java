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
 * <p>L'original en tirait deux sortes, et ce sont elles qui font le volume — le joueur en a change
 * les nombres, et la forme se lit mieux ainsi :
 *
 * <ul>
 *   <li><b>trois grosses</b> — de 1,3 a 1,9 de rayon, posees dans un cube de plus ou moins 1,5 bloc,
 *       et <b>presque immobiles</b> : elles ne balancent plus que de 0,17 a 0,24 bloc, la ou
 *       l'original les promenait de 1,4 a 2. Ce sont elles qui font le coeur, et c'est leur immobilité
 *       qui fait tenir la masse ;</li>
 *   <li><b>trois petites</b> — de 0,1 a 0,3 seulement, mais lancees de plus ou moins 3,5 a 5 blocs :
 *       elles tournent <b>loin</b> autour du corps, et ce sont elles qui font la peripherie. Le
 *       balancement deux fois et demie plus grand de l'original compense exactement leur taille
 *       minuscule, et c'est ce qui les fait lire comme trois boules qui passent pendant que le centre
 *       tient.</li>
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
 * <p>Et ses <b>couleurs</b> ne sont pas celles qu'on croit. Le nuanceur melangeait du rose au bleu
 * <b>sur la densite</b>, donc le bleu entoure chaque <b>centre de boule</b> — l'endroit ou la
 * densite est la plus forte — et non la boule entiere : chacune est un <b>halo rose avec un coeur
 * bleu</b>, la meme recette que la bille du meltdowner du meme mod. Et la ou plusieurs boules
 * s'entassent, leurs densites s'additionnent, donc le <b>centre du corps</b> entier bleuit sur une
 * tache continue — d'ou une seconde piece, celle-la.
 *
 * <p>Trois fois le port s'est trompe la-dessus, et trois fois c'est le joueur qui l'a vu : des
 * boules bleues (le bleu n'est pas une couleur de boule), un coeur rose (chaque boule en a un
 * bleu), et enfin des boules trop petites pour se fondre, qui se comptaient une a une au lieu de
 * faire une masse. Voir {@link #VISIBILITY} et {@link #CORE_DENSITY}, qui portent les deux seuils.
 *
 * <p>Tout est <b>pur</b> — ni Minecraft ni horloge — donc verifiable en JUnit.
 */
public final class PlasmaBodyVisuals {

    /**
     * Les deux familles, telles que l'original les tirait — et ce que le joueur en a change.
     *
     * <p>L'original tirait <b>quatre</b> grosses et <b>quatre ou cinq</b> petites. Le joueur voit le
     * vrai mod autrement, et il l'a dit deux fois : « on voyait trois grosses boules au centre qui
     * ne bougent pas vraiment et seulement trois petites boules autour qui se deplacent », puis
     * « visuellement on devrait avoir moins de boules ». Le port suit donc sa description : trois
     * et trois, six boules au lieu de huit ou neuf.
     *
     * <p>Et les grosses sont plus grosses qu'a l'original (1,3 a 1,9 au lieu de 1 a 1,5) : elles
     * paraissaient trop petites une fois leur balancement calme.
     */
    public static final int BIG_COUNT = 3;
    public static final double BIG_SIZE_MIN = 1.3;
    public static final double BIG_SIZE_MAX = 1.9;
    public static final double BIG_OFFSET = 1.5;

    /** {@code rangei(4, 6)} chez l'original : quatre ou cinq. Ici trois, toujours. */
    public static final int SMALL_COUNT_MIN = 3;
    public static final int SMALL_COUNT_MAX = 3;
    public static final double SMALL_SIZE_MIN = 0.1;
    public static final double SMALL_SIZE_MAX = 0.3;
    public static final double SMALL_OFFSET = 3.0;
    public static final double SMALL_AMPLITUDE = 2.5;

    /** Le balancement : amplitude 1,4 a 2 (x la taille de la famille), vitesse 0,5 a 0,7. */
    public static final double AMPLITUDE_MIN = 1.4;
    public static final double AMPLITUDE_MAX = 2.0;
    public static final double SPEED_MIN = 0.5;
    public static final double SPEED_MAX = 0.7;

    /**
     * Ce qui reste du balancement des <b>grosses</b> boules : presque rien.
     *
     * <p>Elles balançaient de plus ou moins 1,4 a 2 blocs chez l'original, et le joueur n'en
     * voulait pas : « les grosses boules qui sont censees rester au centre bougent trop [...]
     * elles ne doivent pas bouger ». Il leur reste un souffle — 0,17 a 0,24 bloc, soit huit fois
     * moins —, assez pour que la masse respire, trop peu pour qu'on la voie se deplacer. C'est ce
     * qui les separe enfin des petites, qui tournent toujours a 3,5 ou 5 blocs.
     */
    public static final double BIG_MOTION = 0.12;

    /** La montee de l'opacite, en trois dixiemes par seconde, et sa chute, en une seconde. */
    public static final double RISE_PER_SECOND = 0.3;
    public static final double FADE_PER_SECOND = 1.0;

    /**
     * Le seuil de densite ou une boule s'efface — et c'est lui qui fixe son rayon.
     *
     * <p>Le nuanceur empilait des densites en {@code alpha * taille / distance carre}, sans aucun
     * bord : une boule se voyait jusqu'a la distance ou cette densite passait sous ce qu'un oeil
     * distingue du fond. D'ou un rayon en <b>racine de la taille</b> et non proportionnel a elle —
     * un facteur direct rendrait les petites boules invisibles (deux dixiemes de bloc) et les
     * grosses enormes.
     *
     * <p>Et ce rayon est <b>long</b> : a pleine opacite, une grosse boule se voit a plus de trois
     * blocs. C'est ce qui fait qu'elles se <b>fondent</b> en une masse au lieu de se compter une a
     * une ; le port avait un rayon de moitie, laissait des trous entre elles, et le joueur y a lu
     * « quinze petites boules qui se deplacent » au lieu d'un corps.
     *
     * <p>Le seuil a encore ete <b>baisse</b> (0,15 puis 0,12) : le joueur a trouve « le nuage rose
     * [...] un peu plus gros » que ce que le port dessinait, et ce seuil est le seul levier qui
     * elargit les halos de toutes les boules d'un coup — racine de 0,15 sur 0,12 les porte un
     * huitieme plus loin.
     */
    public static final double VISIBILITY = 0.12;

    /**
     * Et le seuil ou elle <b>bleuit</b>.
     *
     * <p>Le nuanceur melangeait du rose au bleu sur la densite, donc la couleur bleue entourait
     * chaque <b>centre de boule</b> — l'endroit ou la densite est la plus forte — et non la boule
     * entiere. Chacune est donc un halo rose <b>avec un coeur bleu</b>, comme la bille du meltdowner
     * du meme mod (une lueur, un coeur). Le joueur l'a dit en une phrase : « leur centre est rose,
     * donc pas comme le vrai ».
     */
    public static final double CORE_DENSITY = 1.2;

    /**
     * La part <b>visible</b> d'un carre.
     *
     * <p>L'image s'eteint avant son bord — son profil est un {@code (1 - r) au carre}, qui passe
     * sous un dixieme d'opacite a 68 % du carre. Le carre a dessiner est donc plus grand que le
     * rayon qu'on veut voir, et c'est ce facteur qui les relie. Voir {@code scripts/make-plasma-ball.mjs}.
     */
    public static final double SPRITE_REACH = 0.68;

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

    /** Le rayon <b>vu</b> d'une boule : la ou la densite du nuanceur tombe sous le seuil. */
    public static double haloRadius(double size, float alpha) {
        return densityRadius(size, alpha, VISIBILITY);
    }

    /** Et le rayon de son <b>coeur bleu</b> : le meme calcul, au seuil du bleu. */
    public static double coreRadius(double size, float alpha) {
        return densityRadius(size, alpha, CORE_DENSITY);
    }

    /** Le rayon du coeur bleu du <b>corps entier</b>, ou les densites de toutes les boules s'ajoutent. */
    public static double bodyCoreRadius(float alpha) {
        return BODY_CORE_RADIUS * growth(alpha);
    }

    /**
     * La moitie du carre a dessiner pour obtenir ce rayon vu.
     *
     * <p>L'image s'eteint avant son bord : sans ce rattrapage, tout ce que le port dessine est
     * d'un bon tiers plus petit que ce qu'il croit dessiner. Voir {@link #SPRITE_REACH}.
     */
    public static double quadRadius(double visibleRadius) {
        return visibleRadius / SPRITE_REACH;
    }

    /** La ou {@code alpha * taille / distance carre} tombe sous un seuil donne, en blocs. */
    private static double densityRadius(double size, float alpha, double threshold) {
        return Math.sqrt(Math.max(0.0, alpha) * Math.max(0.0, size) / threshold);
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
     * Le rayon du coeur bleu du corps entier, une fois noue.
     *
     * <p>A cote des coeurs de chaque boule, il y a celui du <b>corps</b> : au centre, les densites
     * de toutes les boules s'additionnent, et le nuanceur y passait au bleu sur une tache continue
     * — c'est la tache bleue que le joueur voit au milieu de la masse. Un coeur par boule ne la
     * refait pas : il laisse des points bleus separes la ou le vrai mod n'en fait qu'un. D'ou cette
     * piece, qui n'est que la somme.
     */
    public static final double BODY_CORE_RADIUS = 1.8;

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
                    trig(random, BIG_MOTION),
                    trig(random, BIG_MOTION)));
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
