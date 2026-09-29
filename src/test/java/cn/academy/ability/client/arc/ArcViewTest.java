package cn.academy.ability.client.arc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Le recollement de l'eclair du tireur sur sa camera.
 *
 * <p>Ce qui doit tenir : le trait ne change ni de direction ni de longueur — la camera est sur
 * l'axe de l'arc, donc le glissement se fait le long de cet axe, et le motif n'a pas a etre
 * regenere. Et en vue interne, ou la camera est deja au depart, rien ne doit bouger du tout.
 */
class ArcViewTest {

    private static final double[] FROM = { 10, 64, -20 };
    private static final double[] TO = { 10, 64, -5 };

    @Test
    @DisplayName("le depart devient la camera, et le trait glisse entier")
    void leDepartDevientLaCamera() {
        double[] camera = { 10, 64, -24 };
        double[][] fixed = ArcView.fix(FROM, TO, camera);

        assertEquals(camera[0], fixed[0][0], 1e-9);
        assertEquals(camera[1], fixed[0][1], 1e-9);
        assertEquals(camera[2], fixed[0][2], 1e-9);
        // L'arrivee recule du meme vecteur : le trait est le meme, prolonge jusqu'a l'oeil.
        assertEquals(TO[2] - 4, fixed[1][2], 1e-9);
    }

    @Test
    @DisplayName("ni la direction ni la longueur ne changent")
    void niDirectionNiLongueur() {
        double[][] fixed = ArcView.fix(FROM, TO, new double[] { 10, 64, -24 });

        for (int i = 0; i < 3; i++) {
            assertEquals(TO[i] - FROM[i], fixed[1][i] - fixed[0][i], 1e-9,
                    "le vecteur de l'arc est inchange");
        }
        assertEquals(size(TO, FROM), size(fixed[1], fixed[0]), 1e-9, "et sa longueur aussi");
    }

    @Test
    @DisplayName("en vue interne, la camera est au depart et rien ne bouge")
    void enVueInterneRienNeBouge() {
        double[][] fixed = ArcView.fix(FROM, TO, FROM);

        assertEquals(FROM[0], fixed[0][0], 1e-9);
        for (int i = 0; i < 3; i++) {
            assertEquals(TO[i], fixed[1][i], 1e-9, "l'eclair reste exactement ou il est");
        }
    }

    private static double size(double[] a, double[] b) {
        double dx = a[0] - b[0], dy = a[1] - b[1], dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
