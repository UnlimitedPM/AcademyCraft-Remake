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
 * l'AGE et de l'ELAN du lancer, donc les deux cotes le calculent a l'identique. Tout le reste — le
 * lancer, l'objet, le tir — demande un serveur, donc un GameTest.
 *
 * <p>La plupart des tests d'ici se placent au LANCER A L'ARRET, ou {@code launchVel} vaut
 * {@link CoinToss#INIT_VEL} et ou les bornes sont celles des constantes ; les deux derniers
 * verifient ce que l'elan du joueur et la hauteur de sa main changent.
 */
class CoinTossTest {

    @Test
    @DisplayName("la piece part a zero, passe par la moitie au sommet, et finit a un")
    void laCourbeDeLoriginal() {
        double vel = CoinToss.INIT_VEL;
        // L'original : la montee compte pour la premiere moitie du vol, la descente pour la seconde.
        assertEquals(0.0, CoinToss.progress(0, vel), 1e-9, "au lancer");
        // Sur la montee, la progression est la vitesse perdue : 5 ticks font 5 x 0,06 sur 0,92, et
        // la montee entiere ne compte que pour la moitie du vol.
        assertEquals(0.16304, CoinToss.progress(5, vel), 1e-5, "cinq ticks de montee");
        assertTrue(CoinToss.progress(CoinToss.APEX_TICK, vel) < 0.5,
                "elle n'atteint la moitie qu'au sommet, pas avant");

        // Et sur la descente, c'est la hauteur perdue depuis le sommet qui compte.
        assertEquals(1.0, CoinToss.progress(CoinToss.LAND_TICK, vel), 1e-9, "de retour au sol");
    }

    @Test
    @DisplayName("la progression ne redescend jamais, et la borne est respectee")
    void laProgressionMonteToujours() {
        double previous = -1;
        for (int ticks = 0; ticks <= CoinToss.LAND_TICK; ticks++) {
            double progress = CoinToss.progress(ticks, CoinToss.INIT_VEL);
            assertTrue(progress >= previous, "le vol recule au tick " + ticks);
            assertTrue(progress >= 0 && progress <= 1, "hors des bornes au tick " + ticks);
            previous = progress;
        }
    }

    @Test
    @DisplayName("la hauteur monte puis retombe, et le sommet est celui qu'on croit")
    void laHauteurDuVol() {
        double vel = CoinToss.INIT_VEL;
        assertEquals(0.0, CoinToss.height(0, vel), 1e-9, "elle part de la hauteur du lancer");

        double previous = 0;
        for (int ticks = 1; ticks <= CoinToss.APEX_TICK; ticks++) {
            assertTrue(CoinToss.height(ticks, vel) > previous, "elle monte au tick " + ticks);
            previous = CoinToss.height(ticks, vel);
        }
        assertEquals(previous, CoinToss.apexHeight(vel), 1e-9,
                "et le sommet est le dernier tick montant");
        assertEquals(0.0, CoinToss.velocity(CoinToss.APEX_TICK + 1, vel), 0.05,
                "ou la vitesse s'annule");

        // Sept blocs de haut : elle passe au-dessus de la tete du joueur, comme l'original.
        assertTrue(CoinToss.apexHeight(vel) > 6.0 && CoinToss.apexHeight(vel) < 7.0,
                "un sommet a sept blocs : " + CoinToss.apexHeight(vel));
        assertTrue(CoinToss.height(CoinToss.LAND_TICK, vel) <= 0, "et elle revient a sa hauteur");
    }

    @Test
    @DisplayName("le tir n'est permis que sur la retombee, et pendant assez longtemps")
    void laFenetreDeTir() {
        double vel = CoinToss.INIT_VEL;
        // Une piece retombee en un peu plus d'une seconde et demie : 0,92 de vitesse pour 0,06 de
        // gravite font un sommet vers le quinzieme tick, et un retour vers le trentieme.
        assertEquals(15, CoinToss.APEX_TICK, "le sommet, au quinzieme tick");
        assertEquals(30, CoinToss.LAND_TICK, "et le retour, a une seconde et demie");
        assertEquals(25, CoinToss.READY_TICK, "le tir s'ouvre au vingt-cinquieme");

        assertTrue(CoinToss.READY_TICK > CoinToss.APEX_TICK,
                "le tir s'ouvre sur la retombee, jamais dans la montee");
        assertTrue(CoinToss.LAND_TICK - CoinToss.READY_TICK >= 5,
                "et la fenetre dure au moins cinq ticks");
        assertFalse(CoinToss.isReady(CoinToss.APEX_TICK, vel), "rien au sommet");
        assertTrue(CoinToss.isReady(CoinToss.READY_TICK, vel), "et le tir est ouvert a partir de la");

        // Ce que la borne veut dire, tick par tick : rien jusqu'au vingt-quatrieme.
        for (int ticks = 0; ticks < CoinToss.READY_TICK; ticks++) {
            assertFalse(CoinToss.isReady(ticks, vel), "le tir ne s'ouvre pas au tick " + ticks);
        }
        assertEquals(0.7, CoinToss.READY, 1e-9, "sept dixiemes, comme l'original");
    }

    @Test
    @DisplayName("elle s'arrete avec la main qui l'a lancee, pas a son point de depart")
    void laRetombeeSeLitSurLaMain() {
        double vel = CoinToss.INIT_VEL;

        // Un joueur qui n'a pas bouge : sa main est la ou elle etait, donc la piece finit son vol.
        assertFalse(CoinToss.hasLanded(20, vel, 0.0), "encore en l'air au vingtieme tick");
        assertTrue(CoinToss.hasLanded(CoinToss.LAND_TICK, vel, 0.0),
                "et posee a sa hauteur de depart");

        // Un joueur qui est MONTE de trois blocs : sa main est venue a sa rencontre.
        assertTrue(premierAtterrissage(vel, 3.0) < CoinToss.LAND_TICK,
                "la main qui monte la rattrape plus tot que le " + CoinToss.LAND_TICK + "e tick");

        // Un joueur qui est DESCENDU, au contraire, la laisse tomber plus bas, et donc plus longtemps.
        assertTrue(premierAtterrissage(vel, -3.0) > CoinToss.LAND_TICK,
                "et la main qui descend la laisse descendre avec elle");
    }

    @Test
    @DisplayName("un lancer en plein saut monte plus haut, et retombe plus tard")
    void lelanDuJoueurCompte() {
        // L'elan d'un saut, ajoute au lancer : c'est le motionY = player.motionY de l'original.
        double saut = CoinToss.INIT_VEL + 0.42;

        // La gravite ne change pas, donc la hauteur, si : presque trois fois plus, et un vol qui dure
        // moitie plus longtemps. C'est ce que le joueur a remarque en sautant.
        assertTrue(CoinToss.apexHeight(saut) > CoinToss.apexHeight(CoinToss.INIT_VEL) * 2,
                "plus de deux fois plus haut : " + CoinToss.apexHeight(saut));
        assertTrue(CoinToss.landTick(saut) > CoinToss.LAND_TICK, "et elle retombe plus tard");
        assertTrue(premierPret(saut) > CoinToss.READY_TICK,
                "donc le tir s'ouvre plus tard, en ticks");
        assertTrue(CoinToss.progress(0, saut) < 0.0,
                "et la montee commence sous zero, avec sa vitesse en plus");
    }

    /** Le premier tick ou la piece a rejoint une main decalee de {@code drop} blocs. */
    private static int premierAtterrissage(double launchVel, double drop) {
        for (int ticks = 1; ticks <= CoinToss.MAX_LIFE; ticks++) {
            if (CoinToss.hasLanded(ticks, launchVel, drop)) return ticks;
        }
        return CoinToss.MAX_LIFE + 1;
    }

    /** Et le premier tick ou le railgun peut partir, pour cet elan-la. */
    private static int premierPret(double launchVel) {
        for (int ticks = 1; ticks <= CoinToss.MAX_LIFE; ticks++) {
            if (CoinToss.isReady(ticks, launchVel)) return ticks;
        }
        return CoinToss.MAX_LIFE + 1;
    }
}
