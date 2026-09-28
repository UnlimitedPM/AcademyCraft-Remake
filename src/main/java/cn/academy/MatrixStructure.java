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
}
