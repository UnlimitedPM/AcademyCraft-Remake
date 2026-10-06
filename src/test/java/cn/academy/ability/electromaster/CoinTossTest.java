package cn.academy.ability.electromaster;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le vol de la piece du railgun : sa courbe, et la fenetre pendant laquelle le tir est permis.
 *
 * <p>C'est la seule partie de la piece qui se relit sans monde : l'entite ne fait que la nourrir
 * avec ses champs, et le railgun ne fait que la lire. Tout le reste — le lancer, l'objet, le tir —
 * demande un serveur, donc un GameTest.
 */
class CoinTossTest {

    /** Le vol simule : la meme integration que l'entite, tick par tick. */
    private record Flight(double maxHt, int apexTick, int landTick, int readyTick) {}

    private static Flight fly() {
        double posY = 0, motionY = CoinToss.INIT_VEL, maxHt = 0, previous = 0;
        int apex = -1, land = -1, ready = -1;
        for (int tick = 1; tick <= CoinToss.MAX_LIFE; tick++) {
            motionY -= CoinToss.GRAVITY;
            posY += motionY;

            // Le sommet est le dernier tick ou l'on monte encore : c'est la que la hauteur cesse
            // d'augmenter, et non le premier tick ou la vitesse devient negative (elle l'est un
            // tick apres le sommet, quand la piece a deja commence a descendre).
            if (apex < 0 && posY < previous) apex = tick - 1;
            previous = posY;
            maxHt = Math.max(maxHt, posY);

            if (ready < 0 && CoinToss.isReady(CoinToss.progress(motionY, maxHt, posY, 0))) ready = tick;
            if (posY <= 0 && motionY < 0) {
                land = tick;
                break;
            }
        }
        return new Flight(maxHt, apex, land, ready);
    }

    @Test
    @DisplayName("la piece part a zero, passe par la moitie au sommet, et finit a un")
    void laCourbeDeLoriginal() {
        // L'original : la montee compte pour la premiere moitie, la descente pour la seconde.
        assertEquals(0.0, CoinToss.progress(CoinToss.INIT_VEL, 0, 0, 0), 1e-9, "au lancer");
        assertEquals(0.25, CoinToss.progress(CoinToss.INIT_VEL / 2, 7, 5, 0), 1e-9,
                "a mi-montee");
        assertEquals(0.5, CoinToss.progress(0, 7, 7, 0), 1e-9, "au sommet, vitesse nulle");
        assertEquals(0.75, CoinToss.progress(-0.46, 7, 3.5, 0), 1e-9, "a mi-descente");
        assertEquals(1.0, CoinToss.progress(-0.92, 7, 0, 0), 1e-9, "et de retour au sol");
    }

    @Test
    @DisplayName("la progression ne redescend jamais, et la borne est respectee")
    void laProgressionMonteToujours() {
        double previous = -1;
        double posY = 0, motionY = CoinToss.INIT_VEL, maxHt = 0;

        for (int tick = 1; tick <= CoinToss.MAX_LIFE; tick++) {
            motionY -= CoinToss.GRAVITY;
            posY += motionY;
            maxHt = Math.max(maxHt, posY);

            double progress = CoinToss.progress(motionY, maxHt, posY, 0);
            assertTrue(progress >= previous, "le vol recule au tick " + tick);
            assertTrue(progress >= 0 && progress <= 1, "hors des bornes au tick " + tick);
            previous = progress;

            if (posY <= 0 && motionY < 0) break;
        }
    }

    @Test
    @DisplayName("le tir n'est permis que sur la retombee, et pendant assez longtemps")
    void laFenetreDeTir() {
        Flight flight = fly();

        // Une piece retombee en un peu plus d'une seconde et demie : 0,92 de vitesse pour 0,06 de
        // gravite font un sommet vers le quinzieme tick, et un retour vers le trentieme.
        assertEquals(15, flight.apexTick(), "le sommet, au quinzieme tick");
        assertEquals(30, flight.landTick(), "et le retour au sol, a une seconde et demie");

        // La piece atteint sept dixiemes APRES le sommet : la fenetre est donc dans la descente, et
        // elle dure une poignee de ticks — de quoi appuyer, pas de quoi attendre.
        assertTrue(flight.readyTick() > flight.apexTick(),
                "le tir s'ouvre sur la retombee, jamais dans la montee");
        assertTrue(flight.landTick() - flight.readyTick() >= 5,
                "et la fenetre dure au moins cinq ticks");

        // Ce que la borne veut dire : juste en dessous, on ne tire pas.
        assertEquals(0.7, CoinToss.READY, 1e-9, "sept dixiemes, comme l'original");
        assertFalse(CoinToss.isReady(0.7), "la borne elle-meme n'ouvre pas le tir");
        assertFalse(CoinToss.isReady(0.69));
        assertTrue(CoinToss.isReady(0.71));
        assertFalse(CoinToss.isReady(0.0), "et jamais pendant la montee");
    }
}
