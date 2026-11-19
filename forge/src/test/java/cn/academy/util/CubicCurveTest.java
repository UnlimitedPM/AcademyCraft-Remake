package cn.academy.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * La courbe cubique des ondes de vecmanip.
 *
 * <p>Elle vient de l'original, et deux proprietes comptent : elle <b>passe par ses points</b> (les
 * deux courbes de l'onde sont lues a des instants fixes, et ces valeurs-la sont celles de
 * l'original), et elle <b>prolonge en ligne droite</b> au-dela de son dernier point.
 */
class CubicCurveTest {

    private static CubicCurve courbe() {
        return new CubicCurve().add(0, 0.4).add(0.2, 0.8).add(2.5, 1.5);
    }

    @Test
    @DisplayName("la courbe passe par chacun de ses points")
    void laCourbePasseParSesPoints() {
        CubicCurve curve = courbe();

        assertEquals(3, curve.size(), "trois points, comme la courbe d'echelle de l'original");
        assertEquals(0.4, curve.valueAt(0), 1e-9, "au premier point");
        assertEquals(0.8, curve.valueAt(0.2), 1e-9, "et au deuxieme");
        assertEquals(1.5, curve.valueAt(2.5), 1e-9, "et au dernier");
    }

    @Test
    @DisplayName("la courbe grandit entre ses points")
    void laCourbeGranditEntreSesPoints() {
        CubicCurve curve = courbe();
        double previous = curve.valueAt(0);

        for (double x = 0.02; x <= 2.5; x += 0.02) {
            double value = curve.valueAt(x);
            assertEquals(true, value >= previous - 1e-9,
                    "une courbe qui ne redescend pas : " + value + " apres " + previous);
            previous = value;
        }
    }

    @Test
    @DisplayName("hors de ses points, elle prolonge en ligne droite")
    void elleProlongeEnLigneDroite() {
        CubicCurve curve = courbe();
        // La pente du dernier segment : (1,5 - 0,8) / (2,5 - 0,2).
        double slope = (1.5 - 0.8) / (2.5 - 0.2);

        assertEquals(1.5 + slope, curve.valueAt(3.5), 1e-9,
                "un point au-dela du dernier suit la pente du dernier segment");
        assertEquals(1.5 + slope * 10, curve.valueAt(12.5), 1e-9, "aussi loin qu'on veuille");
    }

    @Test
    @DisplayName("une courbe sans point ne rend rien")
    void uneCourbeVideRendZero() {
        assertEquals(0, new CubicCurve().valueAt(1), 1e-12, "aucun point, aucune valeur");
    }

    @Test
    @DisplayName("la courbe d'opacite de l'original a bien ses cinq points")
    void laCourbeDOpaciteEstCelleDeLOriginal() {
        CubicCurve alpha = new CubicCurve().add(0, 0).add(0.2, 1).add(0.5, 1).add(0.8, 1).add(1, 0);

        assertEquals(0, alpha.valueAt(0), 1e-9, "elle part de rien");
        assertEquals(1, alpha.valueAt(0.2), 1e-9, "monte en un cinquieme");
        assertEquals(1, alpha.valueAt(0.5), 1e-9, "se tient");
        assertEquals(1, alpha.valueAt(0.8), 1e-9, "encore");
        assertEquals(0, alpha.valueAt(1), 1e-9, "et retombe au bout");
    }
}
