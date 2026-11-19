package cn.academy;

import org.junit.jupiter.api.Test;

import net.minecraft.core.Direction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les boites de rendu des block entities.
 *
 * <p>Une boite trop petite ne casse rien a la compilation : elle fait disparaitre un modele des
 * que la camera bouge, et c'est exactement ce que le joueur a vu (22/10). Ces tests figent donc
 * les trois choses qui comptent — la boite contient le bloc d'ancrage, elle contient la geometrie
 * du multi-bloc, et elle est valide (minimum plus petit que maximum).
 */
class RenderBoundsTest {

    @Test
    void laBoiteDesPalesCouvreLeurRayon() {
        double[] b = RenderBounds.windgenRotor(7);
        // Les pales balaient sept blocs de rayon : la boite doit les contenir, marge comprise.
        assertTrue(b[0] <= -7.0d, "vers la gauche");
        assertTrue(b[1] <= -7.0d, "vers le bas");
        assertTrue(b[2] <= -7.0d, "vers l'arriere");
        assertTrue(b[3] >= 8.0d, "vers la droite");
        assertTrue(b[4] >= 8.0d, "vers le haut");
        assertTrue(b[5] >= 8.0d, "vers l'avant");
        assertValid(b);
    }

    @Test
    void laBoiteDuMatrixCouvreSonCubeDeDeuxBlocs() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            double[] b = RenderBounds.matrix(facing);
            // Le cube s'etend d'un bloc de chaque cote du coin d'ancrage, et de deux en hauteur.
            // La LARGEUR compte, pas le bord : selon la direction, le cube part vers les x
            // negatifs ou positifs.
            assertTrue(b[3] - b[0] >= 2.0d, "deux blocs sur x, " + facing);
            assertTrue(b[5] - b[2] >= 2.0d, "deux blocs sur z, " + facing);
            assertTrue(b[4] - b[1] >= 2.0d, "deux blocs en hauteur, " + facing);
            assertValid(b);
        }
    }

    @Test
    void laBoiteDuMatrixNeDependPasDuCoteOuLeCubeSEtend() {
        // Le multi-bloc s'etend vers `left` et `back`, donc la boite n'est pas la meme selon la
        // direction — mais elle doit toujours contenir le bloc d'ancrage ET le cube entier.
        double[] north = RenderBounds.matrix(Direction.NORTH);
        double[] south = RenderBounds.matrix(Direction.SOUTH);
        assertTrue(north[0] != south[0] || north[2] != south[2],
                "deux directions opposees n'ont pas la meme boite");
    }

    @Test
    void laBoiteDuDeveloppeurCouvreSesTroisBlocs() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            double[] b = RenderBounds.developer(facing);
            assertTrue(b[4] - b[1] >= 3.0d, "trois blocs en hauteur, " + facing);
            // Trois blocs de profondeur : un de large, deux vers l'arriere. Selon la direction,
            // ces deux blocs partent vers les x ou vers les z — c'est la SOMME des deux largeurs
            // qui doit valoir trois.
            double span = (b[3] - b[0]) + (b[5] - b[2]);
            assertTrue(span >= 4.0d, "trois blocs de profondeur, " + facing + " : " + span);
            assertValid(b);
        }
    }

    @Test
    void laMargeEstLaMemePartout() {
        // Un quart de bloc : assez pour un flottement, assez peu pour ne rien couter.
        assertEquals(0.25d, RenderBounds.MARGIN, 1e-9);
        double[] b = RenderBounds.matrix(Direction.NORTH);
        assertEquals(-RenderBounds.MARGIN, b[1], 1e-9, "le bas de la boite est la marge");
    }

    private static void assertValid(double[] b) {
        assertTrue(b[0] < b[3], "minX < maxX");
        assertTrue(b[1] < b[4], "minY < maxY");
        assertTrue(b[2] < b[5], "minZ < maxZ");
    }
}
