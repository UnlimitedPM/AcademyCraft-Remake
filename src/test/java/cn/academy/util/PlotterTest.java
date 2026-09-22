package cn.academy.util;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le promeneur en ligne du portage, {@code Plotter}.
 *
 * <p>C'est lui qui decide ou l'onde de choc creuse : un pas de travers, et la tranche
 * part de biais ou se coupe. Il est pur — aucune donnee de monde — donc verifiable ici,
 * ce qui est la seule facon de le verifier autrement qu'en regardant le sol s'effondrer.
 *
 * <p>La propriete qui compte le plus est <b>la connexite</b> : deux pas consecutifs sont
 * toujours voisins d'une face, jamais en diagonale. C'est ce qui fait une tranche continue
 * plutot qu'une ligne en pointille.
 */
class PlotterTest {

    private static BlockPos[] walk(Plotter plotter, int steps) {
        BlockPos[] path = new BlockPos[steps];
        for (int i = 0; i < steps; i++) {
            path[i] = plotter.next();
        }
        return path;
    }

    @Test
    void laMarcheSuitLAxeDemande() {
        // Vers +Z, l'axe des blocs du sud quand le lacet vaut zero.
        BlockPos[] south = walk(new Plotter(0, 0, 0, 0, 0, 1), 4);
        assertPath(south, new BlockPos[] {
                new BlockPos(0, 0, 1), new BlockPos(0, 0, 2),
                new BlockPos(0, 0, 3), new BlockPos(0, 0, 4) });

        // Et la meme chose depuis une origine quelconque : c'est ce cas-la qui compte, et
        // c'est celui qui a manque. Le portage avait echange les <b>parametres</b> au lieu
        // des champs, ce qui laissait l'origine intacte — invisible tant que l'origine vaut
        // zero, et fatal des qu'elle ne l'est plus : la marche pietinait sur place.
        BlockPos[] fromElsewhere = walk(new Plotter(10, 5, 10, 0, 0, 1), 3);
        assertPath(fromElsewhere, new BlockPos[] {
                new BlockPos(10, 5, 11), new BlockPos(10, 5, 12), new BlockPos(10, 5, 13) });

        // Vers +X.
        BlockPos[] east = walk(new Plotter(10, 5, 10, 1, 0, 0), 3);
        assertPath(east, new BlockPos[] {
                new BlockPos(11, 5, 10), new BlockPos(12, 5, 10), new BlockPos(13, 5, 10) });

        // Et vers -X : la direction negative avance bien de l'autre cote.
        BlockPos[] west = walk(new Plotter(10, 5, 10, -1, 0, 0), 3);
        assertPath(west, new BlockPos[] {
                new BlockPos(9, 5, 10), new BlockPos(8, 5, 10), new BlockPos(7, 5, 10) });

        // Vers le bas : aucune face de terrain n'est oubliee, l'onde peut descendre.
        BlockPos[] down = walk(new Plotter(0, 0, 0, 0, -1, 0), 3);
        assertPath(down, new BlockPos[] {
                new BlockPos(0, -1, 0), new BlockPos(0, -2, 0), new BlockPos(0, -3, 0) });
    }

    @Test
    void uneDirectionObliqueDonneUnEscalierSansTrou() {
        // A 45 degres : un pas vers le sud, un pas vers l'est, et on recommence. Jamais
        // les deux a la fois — c'est ce qui garde la tranche connexe.
        BlockPos[] diagonal = walk(new Plotter(0, 0, 0, 1, 0, 1), 6);
        assertPath(diagonal, new BlockPos[] {
                new BlockPos(0, 0, 1), new BlockPos(1, 0, 1), new BlockPos(1, 0, 2),
                new BlockPos(2, 0, 2), new BlockPos(2, 0, 3), new BlockPos(3, 0, 3) });

        // Et la meme oblique depuis une origine non nulle, ou les deux axes se suivent dans
        // un ordre qui depend de l'origine.
        BlockPos[] offset = walk(new Plotter(37, 180, 34, 1, 0, 1), 4);
        assertPath(offset, new BlockPos[] {
                new BlockPos(37, 180, 35), new BlockPos(38, 180, 35),
                new BlockPos(38, 180, 36), new BlockPos(39, 180, 36) });

        // Et la meme chose vers le haut : l'onde monte en marches, pas en saut.
        BlockPos[] climb = walk(new Plotter(0, 0, 0, 1, 1, 0), 6);
        assertPath(climb, new BlockPos[] {
                new BlockPos(0, 1, 0), new BlockPos(1, 1, 0), new BlockPos(1, 2, 0),
                new BlockPos(2, 2, 0), new BlockPos(2, 3, 0), new BlockPos(3, 3, 0) });
    }

    @Test
    void deuxPasConsecutifsSontToujoursVoisinsDUneFace() {
        // La propriete qui compte, verifiee sur des directions variees : aucun trou, donc
        // aucune fuite d'air par ou la lumiere passerait.
        for (double[] direction : new double[][] {
                {1, 0, 0}, {0, 0, 1}, {-1, 0, 0}, {0, 0, -1},
                {1, 0, 1}, {1, 0, 3}, {0.3, 0, 1}, {1, -1, 0}, {1, 2, 0}, {2, 0, 1} }) {
            // L'origine n'est pas la meme pour toutes : une origine a zero ne dit rien des
            // echanges de coordonnees (voir le commentaire de la marche vers +Z).
            Plotter plotter = new Plotter(37, 180, 34, direction[0], direction[1], direction[2]);
            BlockPos previous = new BlockPos(37, 180, 34);
            for (int step = 0; step < 20; step++) {
                BlockPos next = plotter.next();
                int distance = Math.abs(next.getX() - previous.getX())
                        + Math.abs(next.getY() - previous.getY())
                        + Math.abs(next.getZ() - previous.getZ());
                assertEquals(1, distance,
                        "pas de " + previous + " a " + next + " pour la direction "
                                + direction[0] + "," + direction[1] + "," + direction[2]);
                assertNotEquals(previous, next, "le marcheur ne peut pas rester sur place");
                previous = next;
            }
        }
    }

    @Test
    void uneDirectionNulleEstRefusee() {
        // L'original levait une exception ici. Le port la garde : c'est au demandeur de
        // choisir un repli, pas au marcheur de partir droit devant par accident.
        assertThrows(IllegalArgumentException.class, () -> new Plotter(0, 0, 0, 0, 0, 0));
    }

    @Test
    void laLongueurDeLaDirectionNaPasDImportance() {
        // L'onde passe un vecteur unitaire, mais l'original documentait deja que seuls les
        // rapports comptent : une direction trois fois plus longue doit marcher pareil.
        BlockPos[] unit = walk(new Plotter(0, 0, 0, 0, 0, 1), 3);
        BlockPos[] long3 = walk(new Plotter(0, 0, 0, 0, 0, 3), 3);
        assertPath(long3, unit);
        assertTrue(unit[2].getZ() == 3, "trois pas vers le sud");
    }

    private static void assertPath(BlockPos[] actual, BlockPos[] expected) {
        assertEquals(expected.length, actual.length, "longueur du chemin");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], "pas numero " + i);
        }
    }
}
