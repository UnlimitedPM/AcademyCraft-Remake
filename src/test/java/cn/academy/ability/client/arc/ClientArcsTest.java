package cn.academy.ability.client.arc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La vie des eclairs du client : ils naissent, scintillent, et meurent.
 *
 * <p>C'est ce que l'original confiait a ses entites ({@code Life(10)} et l'age qui avance) :
 * ici c'est une liste et un numero de tick, donc cela se relit sans monde et sans jeu.
 */
class ClientArcsTest {

    /** Un hasard qui repond toujours la meme chose. */
    private static Random always(double value) {
        return new Random(0) {
            @Override
            public double nextDouble() {
                return value;
            }

            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
    }

    private static final double[] ORIGIN = { 0, 0, 0 };

    @BeforeEach
    void viderLesEclairs() {
        ClientArcs.clear();
    }

    @Test
    @DisplayName("un eclair vit exactement sa duree de vie")
    void unEclairVitSaDureeDeVie() {
        ClientArcs.spawn(ArcPattern.WEAK, ORIGIN, new double[] { 10, 0, 0 }, 10, true, 100,
                always(0.99));

        assertEquals(1, ClientArcs.live().size());
        assertTrue(ClientArcs.live().get(0).endTick() == 110,
                "dix ticks a partir du tick 100 : c'est le Life(10) de l'original");

        ClientArcs.tick(109, always(0.99));
        assertEquals(1, ClientArcs.live().size(), "il vit encore au dernier tick");

        ClientArcs.tick(110, always(0.99));
        assertEquals(0, ClientArcs.live().size(), "et il disparait au suivant");
    }

    @Test
    @DisplayName("il scintille pendant sa vie, et meurt visible ou non")
    void ilScintillePendantSaVie() {
        ClientArcs.spawn(ArcPattern.STRONG, ORIGIN, new double[] { 0, 8, 0 }, 4, false, 0,
                always(0.99));
        assertTrue(ClientArcs.live().get(0).visible(), "visible a sa naissance");

        ClientArcs.tick(1, always(0.0));
        assertFalse(ClientArcs.live().get(0).visible(), "le tick l'eteint");

        ClientArcs.tick(2, always(0.99));
        assertFalse(ClientArcs.live().get(0).visible(),
                "et il reste eteint tant que le hasard ne le rallume pas");

        ClientArcs.tick(4, always(0.99));
        assertEquals(0, ClientArcs.live().size());
    }

    @Test
    @DisplayName("la forme suit la longueur demandee, ou la portee du motif")
    void laFormeSuitLaLongueurDemandee() {
        int full = ArcPatterns.variant(ArcPattern.WEAK, 0).quads().size();

        // Un arc non fige (la genese d'arc) s'arrete au point vise : cinq blocs, donc
        // moins de rubans que le motif entier de vingt blocs.
        ClientArcs.spawn(ArcPattern.WEAK, ORIGIN, new double[] { 5, 0, 0 }, 10, true, 0, always(0.99));
        int clipped = ClientArcs.live().get(0).mesh().quads().size();
        assertTrue(clipped < full, "un arc de cinq blocs est plus court qu'un arc de vingt");
        assertTrue(clipped > 0, "mais il en reste quelque chose");

        // Un arc fige (l'eclair) garde toute sa portee, meme si la cible est plus proche.
        ClientArcs.clear();
        ClientArcs.spawn(ArcPattern.WEAK, ORIGIN, new double[] { 5, 0, 0 }, 10, false, 0, always(0.99));
        assertEquals(full, ClientArcs.live().get(0).mesh().quads().size());
    }

    @Test
    @DisplayName("plusieurs eclairs vivent ensemble, et le depart les emporte tous")
    void plusieursEclairsViventEnsemble() {
        ClientArcs.spawn(ArcPattern.WEAK, ORIGIN, new double[] { 5, 0, 0 }, 10, true, 0, always(0.99));
        ClientArcs.spawn(ArcPattern.AOE, ORIGIN, new double[] { 0, 0, 5 }, 4, true, 0, always(0.99));
        assertEquals(2, ClientArcs.live().size(), "l'eclair frappe trois cibles : trois arcs");

        ClientArcs.tick(4, always(0.99));
        assertEquals(1, ClientArcs.live().size(), "le plus court s'en va le premier");

        ClientArcs.clear();
        assertEquals(0, ClientArcs.live().size(), "et quitter le monde n'en laisse aucun");
    }
}
