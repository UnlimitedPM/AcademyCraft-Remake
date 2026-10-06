package cn.academy.ability.client.vm;

import cn.academy.util.ImprovedNoise;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * La geometrie des tornades de vecmanip : portage de {@code TornadoEffect} et de son rendu.
 *
 * <p>Une tornade n'est pas un modele : c'est un <b>empilement d'anneaux</b>, et deux bruits de
 * Perlin suffisent a lui donner sa forme. Le premier fait <b>gondoler</b> chaque anneau — c'est
 * ce qui fait tituber la colonne au lieu de la laisser droite —, le second donne son
 * <b>rayon</b>, qui s'evase en montant, et la <b>rotation</b> de son image, qui fait tourner
 * le tourbillon.
 *
 * <p class="note">L'original tirait son rayon et son gondolement a chaque image, avec le temps
 * courant : c'est ce qui donne le mouvement. Le port garde le meme calcul, mais separe ce qui se
 * <b>tire une fois</b> (les anneaux, a la naissance de la tornade) de ce qui se <b>calcule a
 * chaque image</b> (le rayon, la rotation, le gondolement) — c'est ce qui rend la chose
 * verifiable, un tirage au hasard ne se verifiant pas.
 *
 * <h2>Les trois formes du port</h2>
 *
 * <p>L'original n'en avait qu'une, parametree, et s'en servait trois fois : les <b>ailes de
 * tempete</b> en posent quatre petites autour du dos de leur porteur, le <b>canon a plasma</b>
 * en dresse une grande sous sa boule, et c'est tout. Les deux autres effets de vecmanip (les
 * ondes de choc et les ondulations d'ecran) sont ailleurs.
 *
 * <p>Les nombres sont ceux de l'original, y compris ses deux constantes les plus visibles : les
 * ailes sont <b>couchees de soixante-dix degres</b> — elles partent vers l'arriere — et leurs
 * quatre fuseaux sont tournes de quarante-cinq degres autour de l'axe du dos, ce qui les fait
 * diverger en eventail.
 */
public final class TornadoVisuals {

    /** La hauteur de l'empilement est divisee en autant de pas, comme l'original. */
    public static final int DIVIDE = 40;

    /** Et chaque anneau est un ruban de vingt segments, comme lui. */
    public static final int SEGMENTS = 20;

    /** La chance qu'un anneau en cache un second, plus large : un tiers. */
    public static final double DOUBLE_RING = 0.35;

    /** L'inclinaison des ailes : elles partent vers l'arriere et vers le haut. */
    public static final double WINGS_TILT_X = -70;

    /** Et leur recul, dans leur propre repere. */
    public static final double WINGS_PRE_Y = 0.2;
    public static final double WINGS_PRE_Z = -0.5;

    /**
     * La hauteur ou se posent les ailes : 1,6 bloc au-dessus des pieds, comme l'original.
     *
     * <p>C'est la hauteur d'epaules ou il posait son entite ({@code player.posY + 1.6}) — sans
     * elle, les ailes pendent SOUS le joueur, ce que le joueur a vu tout de suite.
     */
    public static final double SHOULDERS = 1.6;

    /** L'ecartement des quatre fuseaux, sur l'axe du dos. */
    public static final double WINGS_FAN = 45;

    /** Un fuseau : deux blocs de haut, seize centimetres de large, et trois fois plus gondole. */
    public static final double WING_HEIGHT = 2;
    public static final double WING_SIZE = 0.16;
    public static final double WING_SCALE = 2.0;

    /** La colonne du canon : douze blocs pour huit, et trois fois moins gondolee. */
    public static final double CANNON_HEIGHT = 12;
    public static final double CANNON_SIZE = 8;
    public static final double CANNON_SCALE = 0.3;

    /**
     * L'ecart garde entre le sommet de la colonne et la boule : trois blocs.
     *
     * <p>C'est celui de l'original : sa boule se nouait a quinze blocs, sa colonne en faisait
     * douze, donc son sommet tombait juste sous la boule — et c'est ce qui se voit, la colonne
     * <b>nourrit</b> la boule. La colonne se raccourcit quand il y a moins de place, mais l'ecart,
     * lui, ne change pas.
     */
    public static final double CANNON_GAP = 3;

    /** L'opacite des ailes : leur montee pendant la charge, puis une disparition de quinze ticks. */
    public static final double WINGS_ALPHA = 0.7;
    public static final int WINGS_FADE_TICKS = 15;

    /** Celle du canon : la colonne monte en vingt ticks, et meurt trente ticks apres le tir. */
    public static final double CANNON_ALPHA = 0.5;
    public static final int CANNON_RISE_TICKS = 20;
    public static final int CANNON_FADE_TICKS = 20;
    public static final int CANNON_DEATH_TICKS = 30;

    /**
     * Ce que l'original multiplie a l'opacite de la tornade au moment de la dessiner.
     *
     * <p>Son rendu faisait {@code glColor4d(1, 1, 1, eff.alpha * 0.7)} : les ailes ouvertes se
     * dessinent donc a 0,49, et non a 0,7.
     */
    public static final double DRAW_ALPHA = 0.7;

    /** Un anneau de la colonne : sa hauteur, sa largeur, et la rotation de son image. */
    public record Ring(double y, double width, double phase, double sizeScale) {
    }

    /**
     * Une tornade : sa forme, ses anneaux, et le decalage de temps qui la fait tourner autrement
     * que sa voisine.
     */
    public record Tornado(double height, double size, double density, double scale,
                          List<Ring> rings, double timeOffset) {

        /** La part de hauteur d'un pas, avant les tirages. */
        public double step() {
            return height / DIVIDE;
        }
    }

    /** Un fuseau dessine : sa tornade, et le placement de son repere. */
    public record Part(Tornado tornado, double tx, double ty, double tz,
                       double rotateY, double rotateZ) {
    }

    /**
     * Tout ce qu'il faut pour poser un effet dans le monde : l'inclinaison de son repere, son
     * recul, et ses fuseaux.
     */
    public record Layout(double tiltX, double preX, double preY, double preZ, List<Part> parts) {
    }

    private TornadoVisuals() {
    }

    /**
     * Une tornade tiree au sort : ses anneaux, et son decalage de temps.
     *
     * <p>Attention a l'<b>ordre des tirages</b> : l'original declaraient ses champs avant son bloc
     * d'initialisation, donc le decalage de temps se tirait <b>avant</b> le premier anneau. Le
     * port suit cet ordre, sans quoi deux clients tireraient des tornades differentes.
     */
    public static Tornado tornado(RandomSource random, double height, double size, double density,
                                  double scale) {
        double timeOffset = random.nextDouble() * 20;
        double step = height / DIVIDE;
        double accum = 0;
        List<Ring> rings = new ArrayList<>();

        while (accum < height) {
            accum += step * (1 + random.nextGaussian() * 0.2);
            if (random.nextDouble() >= density) continue;

            rings.add(new Ring(accum, step * ranged(random, 1.8, 2.2),
                    random.nextDouble() * 360, ranged(random, 0.9, 1.2)));
            if (random.nextDouble() < DOUBLE_RING) {
                rings.add(new Ring(accum, step * ranged(random, 1.8, 2.2),
                        random.nextDouble() * 360, ranged(random, 1.2, 1.7)));
            }
        }
        return new Tornado(height, size, density, scale, List.copyOf(rings), timeOffset);
    }

    /** Les ailes de tempete : quatre fuseaux en eventail, couches vers l'arriere. */
    public static Layout wings(RandomSource random) {
        List<Part> parts = new ArrayList<>(4);
        parts.add(wing(random, -0.1, -0.3, 0.1, WINGS_FAN, WINGS_FAN));
        parts.add(wing(random, 0.1, -0.3, 0.1, -WINGS_FAN, -WINGS_FAN));
        parts.add(wing(random, -0.1, -0.5, -0.1, -WINGS_FAN, WINGS_FAN));
        parts.add(wing(random, 0.1, -0.5, -0.1, WINGS_FAN, -WINGS_FAN));
        return new Layout(WINGS_TILT_X, 0, WINGS_PRE_Y, WINGS_PRE_Z, List.copyOf(parts));
    }

    private static Part wing(RandomSource random, double tx, double ty, double tz,
                             double rotateY, double rotateZ) {
        return new Part(tornado(random, WING_HEIGHT, WING_SIZE, 1, WING_SCALE),
                tx, ty, tz, rotateY, rotateZ);
    }

    /**
     * La colonne du canon : une seule, droite, et large comme une maison.
     *
     * <p>Sa hauteur est celle de l'original tant qu'il y a de la place, mais elle s'arrete sous la
     * boule : {@code gapToBall} est la distance qui separe son pied de la boule, et la colonne
     * grandit de ce qui reste — {@link #CANNON_GAP} etant garde pour que son sommet tombe juste
     * sous elle. Sa LARGEUR suit sa hauteur, sans quoi une colonne courte serait trapue. Dans une
     * grotte, la boule se noue sous le plafond (voir {@code PlasmaCannonSkill.spawnPoint}), et la
     * colonne se raccourcit donc avec elle au lieu de traverser la pierre.
     */
    public static Layout cannon(RandomSource random, double gapToBall) {
        double height = Math.min(CANNON_HEIGHT, Math.max(1.0, gapToBall - CANNON_GAP));
        return new Layout(0, 0, 0, 0,
                List.of(new Part(tornado(random, height, CANNON_SIZE * (height / CANNON_HEIGHT),
                        1, CANNON_SCALE), 0, 0, 0, 0, 0)));
    }

    /**
     * Le temps de la tornade, en secondes : quatre fois le temps reel, moins son decalage.
     *
     * <p>C'est le {@code GameTimer.getTime * 4.0 - timeOffset} de l'original, dont le temps etait
     * deja en secondes.
     */
    public static double time(double seconds, double offset) {
        return seconds * 4.0 - offset;
    }

    /**
     * Le gondolement d'un anneau, dans le plan horizontal : les deux bruits de {@code calcdx}.
     *
     * <p>L'amplitude grandit avec la hauteur — {@code 0.3 + (2 ny)^1.4} —, donc le pied de la
     * colonne est presque droit et son sommet part dans tous les sens.
     */
    public static double[] wobble(double ny, double t) {
        double amplitude = 0.3 + Math.pow(ny * 2, 1.4);
        double scaled = t * 0.1;
        return new double[] {
                ImprovedNoise.noise(ny, scaled) * amplitude,
                ImprovedNoise.noise(ny, scaled, 1) * amplitude };
    }

    /** Le rayon d'un anneau, avant son echelle : le {@code r} de l'original. */
    public static double radius(double ny, double t) {
        return 0.5 + 0.3 * ImprovedNoise.noise(ny, 0.2 * t)
                + 0.5 * Math.pow(1.5 * ny, 2)
                + ImprovedNoise.noise(ny);
    }

    /** Et la rotation de son image : plus haut et plus large a la fois. */
    public static double spin(double ny, double t) {
        return 0.1 * (1 + 0.5 * ny) * t;
    }

    /** L'opacite des ailes pendant leur charge : elles montent avec elle. */
    public static double wingsChargeAlpha(int tick, float chargeTime) {
        return tick / chargeTime * WINGS_ALPHA;
    }

    /** Et celle de leur disparition, en quinze ticks. */
    public static double wingsFadeAlpha(int fadeTick) {
        return WINGS_ALPHA * (1 - (double) fadeTick / WINGS_FADE_TICKS);
    }

    /** L'opacite de la colonne du canon : elle se dresse en vingt ticks. */
    public static double cannonRiseAlpha(int tick) {
        return Math.min(1, (double) tick / CANNON_RISE_TICKS) * CANNON_ALPHA;
    }

    /** Et celle de sa fin : vingt ticks, puis trente ticks avant de disparaitre pour de bon. */
    public static double cannonFadeAlpha(int fadeTick) {
        return CANNON_ALPHA * (1 - (double) fadeTick / CANNON_FADE_TICKS);
    }

    private static double ranged(RandomSource random, double from, double to) {
        return from + random.nextDouble() * (to - from);
    }
}
