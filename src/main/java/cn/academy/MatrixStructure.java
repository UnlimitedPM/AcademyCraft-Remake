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

    /**
     * Le carre de la distance entre le <b>centre</b> de la machine et un point, en blocs.
     *
     * <p>La portee se compte depuis le centre, et non depuis le bloc d'ancrage : celui-ci est un COIN
     * du cube, donc deux des quatre cotes de la machine s'etendent d'un bloc vers le point vise et les
     * deux autres s'en eloignent. Mesuree depuis le coin, la meme portee donne un noeud atteint
     * quarante pour cent plus loin selon le cote — ce que le joueur a vu tout de suite : « comme le
     * matrix fait du 2x2x2, il y a 2 cotes ou les nodes se relient a onze blocs de distance, et les 2
     * autres cotes ou il se connecte a seulement neuf ».
     *
     * <p>Les coordonnees sont donnees en doubles et non en {@code BlockPos} pour que le calcul reste
     * celui du rendu : c'est la meme origine, au demi-bloc pres.
     */
    public static double centreDistSqr(double anchorX, double anchorY, double anchorZ, Direction facing,
                                       double x, double y, double z) {
        double dx = x - (anchorX + centreX(facing));
        double dy = y - (anchorY + MACHINE_HALF_HEIGHT);
        double dz = z - (anchorZ + centreZ(facing));
        return dx * dx + dy * dy + dz * dz;
    }
}
