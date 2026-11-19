package cn.academy.client.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L'arithmetique du texte qui defile.
 *
 * <p>Le dessin lui-meme demande un jeu ; ce qui peut se tromper, c'est le decalage — une pause
 * oubliee, un retour qui saute une image, un texte qui repart avant d'avoir montre sa fin. C'est donc
 * {@link MarqueeText#offset(long, int)} qui est fige ici, sans jeu.
 */
class MarqueeTextTest {

    private static final long P = MarqueeText.PAUSE_MS;
    private static final long S = MarqueeText.MS_PER_PIXEL;

    @Test
    @DisplayName("un texte qui tient dans sa boite ne bouge jamais")
    void rienAScroller() {
        assertEquals(0, MarqueeText.offset(0, 0));
        assertEquals(0, MarqueeText.offset(10_000, -5));
    }

    @Test
    @DisplayName("il reste immobile au debut, le temps de lire")
    void pauseAuDebut() {
        assertEquals(0, MarqueeText.offset(0, 40));
        assertEquals(0, MarqueeText.offset(P - 1, 40), "encore dans la pause");
    }

    @Test
    @DisplayName("il part ensuite, d'un pixel toutes les MS_PER_PIXEL")
    void defilement() {
        assertEquals(0, MarqueeText.offset(P, 40));
        assertEquals(1, MarqueeText.offset(P + S, 40));
        assertEquals(20, MarqueeText.offset(P + 20 * S, 40));
    }

    @Test
    @DisplayName("il s'arrete sur sa fin, et il y reste un moment")
    void arretSurLaFin() {
        long arrived = P + 40 * S;
        assertEquals(40, MarqueeText.offset(arrived, 40));
        assertEquals(40, MarqueeText.offset(arrived + P - 1, 40), "pause de fin");
    }

    @Test
    @DisplayName("puis il revient a son debut")
    void retourAuDebut() {
        long cycle = P + 40 * S + P;
        assertEquals(0, MarqueeText.offset(cycle, 40));
        assertEquals(0, MarqueeText.offset(cycle + P - 1, 40));
    }

    @Test
    @DisplayName("le decalage ne depasse jamais la distance a parcourir")
    void jamaisAuDelaDeLaFin() {
        long cycle = P + 40 * S + P;
        for (long now = 0; now < 3 * cycle; now += 7) {
            int offset = MarqueeText.offset(now, 40);
            assertTrue(offset >= 0 && offset <= 40, "decalage hors bornes a " + now + " : " + offset);
        }
    }

    @Test
    @DisplayName("il ne recule pas pendant qu'il defile")
    void monotonePendantLeDefilement() {
        int previous = -1;
        for (long now = P; now <= P + 40 * S; now += S) {
            int offset = MarqueeText.offset(now, 40);
            assertTrue(offset >= previous, "le texte est revenu en arriere a " + now);
            previous = offset;
        }
        assertEquals(40, previous);
    }

    @Test
    @DisplayName("un texte plus long met plus de temps a passer")
    void laDureeSuitLaLongueur() {
        // Deux textes de meme largeur de boite : celui qui deborde de 80 pixels met deux fois plus
        // de temps a montrer sa fin que celui qui deborde de 40.
        assertEquals(40, MarqueeText.offset(P + 40 * S, 40));
        assertTrue(MarqueeText.offset(P + 40 * S, 80) < 80,
                "a la meme date, le plus long n'a pas fini");
        assertEquals(80, MarqueeText.offset(P + 80 * S, 80));
    }
}
