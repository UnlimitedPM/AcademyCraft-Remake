package cn.academy;

import net.minecraft.core.Direction;

/**
 * La geometrie du multi-bloc du Matrix.
 *
 * <p>Le Matrix occupe un cube de deux blocs de cote. Sa partie d'ancrage, {@code B_F_R},
 * en est un <b>coin</b> : les sept autres parties se posent vers {@code left} et vers
 * {@code back}, jusqu'a un bloc de distance chacune — c'est ce que fait
 * {@link MatrixBlock#setPlacedBy}. Le multi-bloc s'etend donc d'un bloc de chaque cote du
 * coin de l'ancrage.
 *
 * <p>Le modele OBJ, lui, est centre sur son origine. Pour le poser au bon endroit, il faut
 * donc amener cette origine au <b>centre</b> de la machine : le milieu des quatre blocs du
 * bas, c'est-a-dire le centre du bloc d'ancrage, plus un demi-bloc du cote ou le multi-bloc
 * s'etend dans chacun des deux axes. Chaque axe recoit alors zero ou un bloc plein — jamais
 * un demi-bloc.
 */
public final class MatrixStructure {

    private MatrixStructure() {}

    /** Le premier axe du multi-bloc, vu depuis sa partie d'ancrage. */
    public static Direction left(Direction facing) {
        return facing.getCounterClockWise();
    }

    /** Le second axe du multi-bloc, vu depuis sa partie d'ancrage. */
    public static Direction back(Direction facing) {
        return facing.getOpposite();
    }

    /**
     * Le decalage du centre de la machine, en blocs, depuis le coin du bloc d'ancrage —
     * l'origine du rendu d'un bloc.
     *
     * <p>Un demi-bloc pour aller du coin au centre du bloc d'ancrage, plus un demi-bloc
     * dans chaque axe ou le multi-bloc s'etend.
     */
    public static double centreX(Direction facing) {
        return 0.5d + 0.5d * (left(facing).getStepX() + back(facing).getStepX());
    }

    /** Le decalage du centre de la machine, en blocs, le long de l'axe z. */
    public static double centreZ(Direction facing) {
        return 0.5d + 0.5d * (left(facing).getStepZ() + back(facing).getStepZ());
    }

    /**
     * La hauteur du centre du cube, en blocs, depuis la base de l'ancrage.
     *
     * <p>Un bloc plein, et non un demi : le multi-bloc occupe DEUX blocs de haut, donc son centre est
     * un bloc au-dessus du bas de l'ancrage. Le rendu, lui, pose son modele au milieu des quatre blocs
     * du bas (un demi-bloc) — c'est une autre question, et c'est pour cela que les deux nombres sont
     * separes.
     */
    public static final double MACHINE_HALF_HEIGHT = 1.0d;

    /** Le demi-cote du cube, en blocs : deux blocs de large, donc un de chaque cote du centre. */
    public static final double MACHINE_HALF_SIZE = 1.0d;

    /**
     * Le carre de la distance entre le Matrix et un bloc, en blocs.
     *
     * <p>La portee se compte depuis la <b>surface</b> du cube, et jusqu'au <b>centre</b> du bloc
     * vise — la distance d'un objet a un autre, pas d'un coin a un coin.
     *
     * <p>Trois erreurs se cachaient dans les deux coins de cette phrase, et le joueur les a toutes
     * vues : « comme le matrix fait du 2x2x2, il y a 2 cotes ou les nodes se relient a onze blocs de
     * distance, et les 2 autres cotes ou il se connecte a seulement neuf », puis, le centre corrige,
     * « il fait toujours un rayon de 11x11x10x10 en fonction de comment je l'ai pose ». Mesuree depuis
     * le bloc d'ancrage, qui est un COIN du cube, la portee s'arretait deux blocs trop tot d'un cote
     * et jamais de l'autre ; mesuree depuis le centre mais jusqu'au coin du bloc du noeud, il restait
     * un demi-bloc, donc un bloc entier d'ecart entre les deux cotes opposes.
     *
     * <p>Les coordonnees sont donnees en doubles et non en {@code BlockPos} pour que le calcul reste
     * celui du rendu : c'est la meme origine, au demi-bloc pres.
     */
    public static double machineDistSqr(double anchorX, double anchorY, double anchorZ, Direction facing,
                                        double blockX, double blockY, double blockZ) {
        double dx = outside(blockX + 0.5d, anchorX + centreX(facing));
        double dy = outside(blockY + 0.5d, anchorY + MACHINE_HALF_HEIGHT);
        double dz = outside(blockZ + 0.5d, anchorZ + centreZ(facing));
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * L'ecart entre un point et le cube, sur un axe : zero tant que le point est dans le cube.
     *
     * <p>C'est ce qui fait que la distance est la meme des quatre cotes — le cube s'etendant d'un
     * bloc de chaque cote de son centre, et non d'un bloc d'un cote et de rien de l'autre.
     */
    private static double outside(double point, double centre) {
        return Math.max(0.0d, Math.abs(point - centre) - MACHINE_HALF_SIZE);
    }
}
