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

    /**
     * La profondeur du bol du fond, en <b>rayons</b> de faisceau.
     *
     * <p>Le bol ne peut pas etre plus LARGE que le faisceau — il a exactement sa largeur — mais il peut
     * etre plus <b>profond</b> qu'il n'est large, et c'est alors une demi-<b>ellipse</b> et non une
     * demi-sphere. C'est ce chiffre-la qui dose l'arrondi, et il a fallu le monter : le premier
     * portage donnait un bol profond de son seul rayon, soit deux a trois blocs sur un tunnel de
     * trente-huit, et le joueur, en jeu : « la forme arrondi au fond du tunnel est bien presente mais
     * clairement pas asser forte ». A trois rayons, le bol fait donc six a neuf blocs de creux.
     */
    public static final double DOME_DEPTH = 3.0;

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

    /**
     * Jusqu'ou va une colonne : <b>le fond du tunnel est arrondi</b>.
     *
     * <p>L'original faisait un mur plat : toutes ses colonnes marchaient la meme longueur, et le
     * tunnel finissait donc sur une paroi bien droite. Le joueur l'a vu deux fois : « fait en sorte que
     * le fond ait une forme un peu arrondie, la maintenant ca fait vraiment mur plat au bout du
     * tunnel », puis, le premier arrondi fait, « clairement pas asser forte ».
     *
     * <p>Le fond suit donc une demi-<b>ellipse</b> : la colonne de l'axe creuse toute la longueur,
     * celle du bord s'arrete {@link #DOME_DEPTH} rayons plus tot, et entre les deux la profondeur suit
     * une racine — c'est ce qui arrondit le creux sans en faire une pointe. Au bord, la pente est
     * <b>verticale</b> : le bol rejoint donc la paroi du tunnel en douceur, sans arete.
     *
     * @param radius le rayon du faisceau, en blocs
     * @param offset la distance de la colonne a l'axe, en blocs — elle peut depasser le rayon, la
     *               fluctuation du bord elargissant le disque point par point
     * @param length la portee du tir : la profondeur de la colonne de l'axe
     */
    public static double reach(double radius, double offset, double length) {
        if (radius <= 0.0) return length;

        double depth = radius * DOME_DEPTH;
        double inside = 1.0 - (offset * offset) / (radius * radius);
        return length - depth + depth * Math.sqrt(Math.max(0.0, inside));
    }
}
