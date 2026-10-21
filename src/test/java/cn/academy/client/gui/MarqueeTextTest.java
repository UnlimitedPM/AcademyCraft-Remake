package cn.academy.client.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le texte qui ne tient pas dans sa boite : il retrecit, sinon il defile.
 *
 * <p>Le dessin demande un jeu ; ce qui peut se tromper, c'est la decision et l'arithmetique du
 * decalage — un texte retreci qui ne tient toujours pas, un defilement qui repart avant d'avoir montre
 * sa fin, une ligne qui sort de sa boite. C'est donc {@link MarqueeText#plan} et
 * {@link MarqueeText#offset(long, int)} qui sont figes ici, sans jeu.
 */
class MarqueeTextTest {

    private static final long P = MarqueeText.PAUSE_MS;
    private static final long S = MarqueeText.MS_PER_PIXEL;

    // --- Faut-il retrecir, ou defiler ? ---------------------------------

    @Test
    @DisplayName("un texte qui tient garde sa taille et sa place")
    void ilTient() {
        String text = "Portee";
        int cell = 8;
        int width = AcademyFonts.width(null, text, cell);

        MarqueeText.Plan plan = MarqueeText.plan(null, text, "LEFT", "CENTER", 10, 20, width + 4, 8,
                cell, false);

        assertEquals(cell, plan.cell(), "aucune raison de le retrecir");
        assertEquals(10, plan.x(), "aligne a gauche dans sa boite");
        assertFalse(plan.scroll());
    }

    @Test
    @DisplayName("un petit debordement se corrige en retrecissant, et le texte tient")
    void petitDebordementRetreci() {
        String text = "Mot de passe";
        int cell = 8;
        int full = AcademyFonts.width(null, text, cell);
        int box = full - 4;      // quatre unites de trop, comme le libelle du noeud sans fil

        MarqueeText.Plan plan = MarqueeText.plan(null, text, "LEFT", "CENTER", 0, 0, box, 8, cell,
                false);

        assertFalse(plan.scroll(), "quatre unites ne justifient pas un defilement");
        assertTrue(plan.cell() < cell, "il doit avoir retreci");
        assertTrue(AcademyFonts.width(null, text, plan.cell()) <= box,
                "et il doit tenir dans la boite");
    }

    @Test
    @DisplayName("un texte beaucoup trop long defile au lieu de rapetisser a l'invisible")
    void grandDebordementDefile() {
        String text = "Le reseau sans fil de la maison";
        int cell = 8;
        int box = 60;

        MarqueeText.Plan plan = MarqueeText.plan(null, text, "LEFT", "CENTER", 6, 10, box, 8, cell,
                false);

        assertTrue(plan.scroll(), "il faut le faire defiler");
        assertEquals(cell, plan.cell(), "a sa taille normale");
        assertEquals(6, plan.x(), "et depuis le bord gauche de sa boite");
    }

    @Test
    @DisplayName("la plus petite taille vaut quatre cinquiemes de la demandee")
    void laBorneDuRetrecissement() {
        String text = "MMMMMMMMMMMM";   // tres large : rien ne peut le faire tenir
        assertEquals(0, MarqueeText.fittedCell(null, text, 10, 20),
                "mieux vaut defiler que d'ecrire en six unites");
    }

    @Test
    @DisplayName("un champ en cours de saisie ne bouge pas")
    void champEnSaisieFigé() {
        String text = "un nom de noeud tres long";
        int cell = 8;

        MarqueeText.Plan plan = MarqueeText.plan(null, text, "LEFT", "CENTER", 0, 0, 40, 8, cell,
                true);

        assertEquals(cell, plan.cell(), "la taille ne change pas sous les doigts");
        assertEquals(0, plan.x(), "et le debut ne glisse pas");
        assertFalse(plan.scroll(), "aucun defilement pendant la frappe");
    }

    // --- Le defilement --------------------------------------------------

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
        assertEquals(40, MarqueeText.offset(P + 40 * S, 40));
        assertTrue(MarqueeText.offset(P + 40 * S, 80) < 80,
                "a la meme date, le plus long n'a pas fini");
        assertEquals(80, MarqueeText.offset(P + 80 * S, 80));
    }
}
