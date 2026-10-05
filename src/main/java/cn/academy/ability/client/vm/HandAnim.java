package cn.academy.ability.client.vm;

import cn.academy.util.CubicCurve;

/**
 * Le geste du poing de vecmanip, portage de {@code AnimPresets} et de {@code CompTransformAnim}.
 *
 * <p>Deux competences le partagent, et c'est le meme : le <b>choc dirige</b> et l'<b>onde
 * dirigee</b>. Le joueur <b>arme</b> son poing pendant la charge — la main recule, monte et se
 * tourne — puis le <b>lance</b> d'un coup sec au relachement, et la main revient d'elle-meme.
 * L'original n'ecrivait ces deux mouvements qu'une fois, dans un fichier a part, et ses deux
 * contextes client s'en servaient tels quels.
 *
 * <h2>Un deplacement, trois angles</h2>
 *
 * <p>Chaque instant du geste est une <b>translation</b> et <b>trois rotations</b>, lues sur six
 * courbes. C'est la {@code CompTransform} de l'original : la main se deplace, puis tourne autour
 * de X, puis de Y, puis de Z. Ses deux gestes ne se servent jamais de la rotation en Z — elle est
 * donc nulle ici, comme chez lui, et ses courbes a elle sont absentes.
 *
 * <p>Le repere est celui de la <b>vue</b> : c'est celui dans lequel Minecraft dessine la main,
 * et l'original y appliquait son deplacement a l'identique. Donc {@code x} va vers la droite,
 * {@code y} vers le haut, et {@code z} <b>vers le joueur</b> — une valeur negative eloigne la main
 * dans l'ecran, ce qui est le geste du coup.
 *
 * <h2>Les deux temps, et leurs secondes</h2>
 *
 * <p>Le temps se lit en <b>secondes</b> chez l'original, pas en ticks : c'est ce qui rend le geste
 * regulier quelle que soit la cadence des images. Deux courbes, deux echelles :
 *
 * <ul>
 *   <li>la <b>charge</b> atteint son plein en un peu plus d'un dixieme de seconde — le temps vaut
 *       {@code dt / 0.15} — puis elle <b>continue de monter</b> lentement : l'original la
 *       plafonnait a {@code 2.0}, et sa derniere courbe prolongee en ligne droite vaut alors 0,8.
 *       La main reste donc armee tant que la touche se tient ;</li>
 *   <li>le <b>coup</b> se parcourt en trois dixiemes de seconde — {@code dt / 0.3} — et revient
 *       exactement a la position ordinaire de la main quand il s'acheve.</li>
 * </ul>
 *
 * <p>Les courbes sont celles de l'original, au point pres : elles passent par chacun de leurs
 * points, donc leurs valeurs a ces instants-la sont les siennes. Tout est <b>pur</b> — ni Minecraft
 * ni le temps — donc verifiable en JUnit, et c'est ce qui compte : un geste se relit sur ses
 * nombres, jamais a l'oeil.
 *
 * <p>Non porte : le {@code pivotPt} de l'original, qui permet de faire tourner la main autour d'un
 * point choisi. Ses deux gestes ne s'en servent pas — il vaut zero — donc le port n'a pas joint le
 * mecanisme a la copie.
 */
public final class HandAnim {

    /** Le plafond du temps de la charge : {@code min(2.0, dt / 0.15)} chez l'original. */
    public static final double PREPARE_HOLD = 2.0;

    /**
     * Ce que vaut un temps plein de charge, en ticks : {@code 0,15 s}, soit trois.
     *
     * <p>C'est cette constante qui fait le pont entre les secondes de l'original et les ticks du
     * port : le temps de la charge se lit deja en ticks, il ne reste qu'a les diviser.
     */
    public static final double PREPARE_TICKS_PER_UNIT = 3.0;

    /** La duree du coup, en ticks : ses trois dixiemes de seconde. */
    public static final int PUNCH_TICKS = 6;

    private final CubicCurve x, y, z, rx, ry, rz;

    private HandAnim(CubicCurve x, CubicCurve y, CubicCurve z,
                     CubicCurve rx, CubicCurve ry, CubicCurve rz) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
    }

    /**
     * Une pose de la main : ou elle se deplace, et de combien elle tourne sur chaque axe.
     *
     * <p>Toutes les rotations sont en <b>degres</b>, comme celles de l'original.
     */
    public record Pose(double x, double y, double z, double rx, double ry, double rz) {
    }

    /** Le geste de la charge. Il ne depend d'aucun joueur, donc il est construit une fois. */
    public static final HandAnim PREPARE = prepare();

    /** Et celui du coup, construit une fois pour la meme raison. */
    public static final HandAnim PUNCH = punch();

    /**
     * Le poing qui s'arme, pendant la charge.
     *
     * <p>La main recule d'un cinquieme de bloc, monte un peu plus d'un tiers, s'eloigne de cinq
     * centiemes, et se couche vers l'arriere de vingt degres — c'est le geste qu'on fait en
     * retenant un coup.
     */
    private static HandAnim prepare() {
        return new HandAnim(
                curve(0, 0, 1, -0.02),
                curve(0, 0, 0.5, 0.2, 1, 0.4),
                curve(0, 0, 1, -0.05),
                curve(0, 0, 1, -20),
                curve(0, 0),
                curve(0, 0));
    }

    /**
     * Le poing qui part, au coup.
     *
     * <p>Il commence <b>haut</b> — huit dixiemes de bloc au-dessus de sa place — et tombe d'un
     * coup sec jusqu'a la sienne en trois dixiemes de seconde, en poussant vers l'ecran d'un
     * demi-bloc au passage, le poignet incline de quarante degres puis redresse, et un dixieme
     * de tour en Y qui s'ouvre au tiers du geste. C'est ce depart en hauteur qui donne au coup
     * son abattage : la main arrive de dessus.
     */
    private static HandAnim punch() {
        return new HandAnim(
                curve(0, -0.04, 0.5, -0.04, 1, 0),
                curve(0, 0.8, 0.5, 0.75, 1, 0),
                curve(0, 0, 0.3, -0.4, 1, 0),
                curve(0, -40, 0.5, -45, 1, 0),
                curve(0, 0, 0.3, 10, 1, 0),
                curve(0, 0));
    }

    /** Le temps de la charge pour un age en ticks, et son plafond. */
    public static double prepareTime(double ticks) {
        return Math.min(PREPARE_HOLD, ticks / PREPARE_TICKS_PER_UNIT);
    }

    /** Le temps du coup pour un age en ticks — un, quand il a fini. */
    public static double punchTime(double ticks) {
        return ticks / PUNCH_TICKS;
    }

    /** La pose du geste a cet instant-la. */
    public Pose pose(double t) {
        return new Pose(x.valueAt(t), y.valueAt(t), z.valueAt(t),
                rx.valueAt(t), ry.valueAt(t), rz.valueAt(t));
    }

    /** Une courbe batie sur des couples (temps, valeur) donnes dans l'ordre. */
    private static CubicCurve curve(double... points) {
        CubicCurve curve = new CubicCurve();
        for (int i = 0; i + 1 < points.length; i += 2) {
            curve.add(points[i], points[i + 1]);
        }
        return curve;
    }
}
