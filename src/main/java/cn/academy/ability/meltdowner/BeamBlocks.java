package cn.academy.ability.meltdowner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Le disque de colonnes qu'un faisceau creuse : le balayage de l'original.
 *
 * <p>Portage de la boucle de {@code RangedRayDamage.perform} : l'original ne creusait pas une ligne,
 * il balayait un <b>disque</b> perpendiculaire au tir, du rayon du faisceau, et creusait une colonne
 * par point du disque — chaque colonne etant ensuite parcourue le long du tir. C'est ce qui donne un
 * <b>trou</b> et non un trait de crayon.
 *
 * <p>Deux nombres, ceux de l'original :
 *
 * <ul>
 *   <li>le pas du balayage, un demi-bloc ({@code STEP}) ;</li>
 *   <li>et une fluctuation du rayon <b>a chaque point</b> ({@code ranged(0.9, 1.1)}) : le bord du
 *       disque est donc irregulier, et c'est ce qui fait que le trou n'a pas l'air decoupe au
 *       compas.</li>
 * </ul>
 *
 * <p>Classe PURE — aucun type de Minecraft — donc verifiable en JUnit, et c'est tout son interet :
 * c'est elle qui decide de la forme du trou.
 */
public final class BeamBlocks {

    /** Le pas du balayage : le {@code STEP} de l'original, un demi-bloc. */
    public static final double STEP = 0.5;

    /** La fluctuation du rayon, point par point : le {@code ranged(0.9, 1.1)} de l'original. */
    public static final double WOBBLE_MIN = 0.9;
    public static final double WOBBLE_MAX = 1.1;

    private BeamBlocks() {
    }

    /**
     * Les points du disque, en decalages — le premier sur la <b>largeur</b> du tir, le second sur sa
     * <b>hauteur</b>.
     *
     * <p>Le tirage est un parametre pour que le test puisse le figer : voir {@link #offsets(double)}.
     */
    public static List<double[]> offsets(double radius, Random random) {
        List<double[]> offsets = new ArrayList<>();
        for (double width = -radius; width <= radius; width += STEP) {
            for (double height = -radius; height <= radius; height += STEP) {
                double wobble = radius * (WOBBLE_MIN
                        + random.nextDouble() * (WOBBLE_MAX - WOBBLE_MIN));
                if (width * width + height * height <= wobble * wobble) {
                    offsets.add(new double[] {width, height});
                }
            }
        }
        return offsets;
    }

    /** Le meme disque, avec le hasard du monde. */
    public static List<double[]> offsets(double radius) {
        return offsets(radius, new Random());
    }
}
