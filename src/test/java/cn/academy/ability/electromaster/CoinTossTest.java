package cn.academy.ability.electromaster;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le vol de la piece du railgun : sa courbe, ses bornes, et la fenetre pendant laquelle le tir est
 * permis.
 *
 * <p>C'est la seule partie de la piece qui se relit sans monde : le vol entier est une fonction de
 * l'AGE, donc les deux cotes le calculent a l'identique. Tout le reste — le lancer, l'objet, le tir —
 * demande un serveur, donc un GameTest.
 */
class CoinTossTest {

    @Test
    @DisplayName("la piece part a zero, passe par la moitie au sommet, et finit a un")
    void laCourbeDeLoriginal() {
        // L'original : la montee compte pour la premiere moitie du vol, la descente pour la seconde.
        assertEquals(0.0, CoinToss.progress(0), 1e-9, "au lancer");
        // Sur la montee, la progression est la vitesse perdue : 5 ticks font 5 x 0,06 sur 0,92, et
        // la montee entiere ne compte que pour la moitie du vol.
        assertEquals(0.16304, CoinToss.progress(5), 1e-5, "cinq ticks de montee");
        assertTrue(CoinToss.progress(CoinToss.APEX_TICK) < 0.5,
                "elle n'atteint la moitie qu'au sommet, pas avant");

        // Et sur la descente, c'est la hauteur perdue depuis le sommet qui compte.
        assertEquals(1.0, CoinToss.progress(CoinToss.LAND_TICK), 1e-9, "de retour au sol");
    }

    @Test
    @DisplayName("la progression ne redescend jamais, et la borne est respectee")
    void laProgressionMonteToujours() {
        double previous = -1;
        for (int ticks = 0; ticks <= CoinToss.LAND_TICK; ticks++) {
            double progress = CoinToss.progress(ticks);
            assertTrue(progress >= previous, "le vol recule au tick " + ticks);
            assertTrue(progress >= 0 && progress <= 1, "hors des bornes au tick " + ticks);
            previous = progress;
        }
    }

    @Test
    @DisplayName("la hauteur monte puis retombe, et le sommet est celui qu'on croit")
    void laHauteurDuVol() {
        assertEquals(0.0, CoinToss.height(0), 1e-9, "elle part de la hauteur du lancer");

        double previous = 0;
        for (int ticks = 1; ticks <= CoinToss.APEX_TICK; ticks++) {
            assertTrue(CoinToss.height(ticks) > previous, "elle monte au tick " + ticks);
            previous = CoinToss.height(ticks);
        }
        assertEquals(previous, CoinToss.apexHeight(), 1e-9, "et le sommet est le dernier tick montant");
        assertEquals(0.0, CoinToss.velocity(CoinToss.APEX_TICK + 1), 0.05,
                "ou la vitesse s'annule");

        // Sept blocs de haut : elle passe au-dessus de la tete du joueur, comme l'original.
        assertTrue(CoinToss.apexHeight() > 6.0 && CoinToss.apexHeight() < 7.0,
                "un sommet a sept blocs : " + CoinToss.apexHeight());
        assertTrue(CoinToss.height(CoinToss.LAND_TICK) <= 0, "et elle revient a sa hauteur");
    }

    @Test
    @DisplayName("le tir n'est permis que sur la retombee, et pendant assez longtemps")
    void laFenetreDeTir() {
        // Une piece retombee en un peu plus d'une seconde et demie : 0,92 de vitesse pour 0,06 de
        // gravite font un sommet vers le quinzieme tick, et un retour vers le trentieme.
        assertEquals(15, CoinToss.APEX_TICK, "le sommet, au quinzieme tick");
        assertEquals(30, CoinToss.LAND_TICK, "et le retour, a une seconde et demie");
        assertEquals(25, CoinToss.READY_TICK, "le tir s'ouvre au vingt-cinquieme");

        assertTrue(CoinToss.READY_TICK > CoinToss.APEX_TICK,
                "le tir s'ouvre sur la retombee, jamais dans la montee");
        assertTrue(CoinToss.LAND_TICK - CoinToss.READY_TICK >= 5,
                "et la fenetre dure au moins cinq ticks");
        assertFalse(CoinToss.isReady(CoinToss.APEX_TICK), "rien au sommet");
        assertTrue(CoinToss.isReady(CoinToss.READY_TICK), "et le tir est ouvert a partir de la");

        // Ce que la borne veut dire, tick par tick : rien jusqu'au vingt-quatrieme.
        for (int ticks = 0; ticks < CoinToss.READY_TICK; ticks++) {
            assertFalse(CoinToss.isReady(ticks), "le tir ne s'ouvre pas au tick " + ticks);
        }
        assertEquals(0.7, CoinToss.READY, 1e-9, "sept dixiemes, comme l'original");
    }

    @Test
    @DisplayName("le vol finit tout seul, et jamais deux fois")
    void laFinDuVol() {
        for (int ticks = 0; ticks < CoinToss.LAND_TICK; ticks++) {
            assertFalse(CoinToss.finished(ticks), "le vol dure encore au tick " + ticks);
        }
        assertTrue(CoinToss.finished(CoinToss.LAND_TICK), "et il est fini au retour");
        assertTrue(CoinToss.finished(CoinToss.MAX_LIFE + 1),
                "le filet de l'original attrape ce qui traine");
    }
}
