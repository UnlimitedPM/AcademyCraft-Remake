package cn.academy.client.gui.cgui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Les elements d'histogramme, verifies sans lancer un jeu.
 *
 * <p>Les quatre fabriques du moteur reprennent celles de {@code TechUI}, couleurs et formats
 * compris. Elles se testent parce qu'elles ne touchent a rien : elles rangent des lectures, et tout
 * ce qu'elles affirment — la couleur d'une barre, le texte d'une valeur — se lit ici.
 */
@DisplayName("les elements d'histogramme")
class CGuiHistTest {

    private static final double EPS = 1e-9;

    @Test
    @DisplayName("l'energie se lit sur son maximum, et s'ecrit en IF")
    void lEnergie() {
        CGuiHist hist = CGuiHist.energy(() -> 3000.0, 6000.0);

        assertEquals("ac.gui.common.hist.energy", hist.id());
        assertArrayEquals(new int[] { 0x25, 0xC4, 0xFF, 255 }, hist.color());
        assertEquals(0.5, hist.value().getAsDouble(), EPS, "la barre est a moitie");
        assertEquals("3000 IF", hist.desc().get());
    }

    @Test
    @DisplayName("et une couleur a QUATRE cases : rouge, vert, bleu, opacite")
    void quatreCases() {
        // C'est le contrat du composant de barre, qui lit l'opacite a la quatrieme case. Une couleur
        // a trois cases le faisait planter en plein dessin : Index 3 out of bounds for length 3.
        for (CGuiHist hist : new CGuiHist[] { CGuiHist.energy(() -> 1.0, 2.0),
                CGuiHist.buffer(() -> 1.0, 2.0), CGuiHist.liquid(() -> 1.0, 2.0),
                CGuiHist.capacity(() -> 1.0, 2), CGuiHist.custom("IF", 0xFFB983FB, () -> 1.0, () -> "") }) {
            assertEquals(4, hist.color().length, hist.id() + " : quatre cases, pas trois");
            assertEquals(255, hist.color()[3], hist.id() + " : opaque, comme l'original");
        }
    }

    @Test
    @DisplayName("le second tampon est plus clair, la phase liquide plus violette")
    void lesAutresCouleurs() {
        assertArrayEquals(new int[] { 0x25, 0xF7, 0xFF, 255 }, CGuiHist.buffer(() -> 1.0, 2.0).color());
        assertArrayEquals(new int[] { 0x76, 0x80, 0xDE, 255 }, CGuiHist.liquid(() -> 1.0, 2.0).color());
        assertArrayEquals(new int[] { 0xFF, 0x6C, 0x00, 255 }, CGuiHist.capacity(() -> 1.0, 2).color());
    }

    @Test
    @DisplayName("une cuve s'ecrit en millibuckets, une capacite sur son maximum")
    void lesTextes() {
        assertEquals("1234 mB", CGuiHist.liquid(() -> 1234.0, 8000.0).desc().get());
        assertEquals("12/40", CGuiHist.capacity(() -> 12.0, 40).desc().get());
    }

    @Test
    @DisplayName("un element ecrit a la main garde son nom tel quel")
    void unElementEcritALaMain() {
        CGuiHist hist = CGuiHist.custom("IF", 0xFFB983FB, () -> 0.25, () -> "2000 mB");

        // « IF » n'est pas une cle de langue : le generateur de phase de l'original l'ecrivait en
        // dur, faute de frappe de son auteur, et une cle inconnue ressort telle quelle a l'ecran.
        assertEquals("IF", hist.id());
        assertArrayEquals(new int[] { 0xB9, 0x83, 0xFB, 255 }, hist.color(),
                "0xffb983fb est en ARGB : le B9 est la composante ROUGE");
        assertEquals(0.25, hist.value().getAsDouble(), EPS);
        assertEquals("2000 mB", hist.desc().get());
    }

    @Test
    @DisplayName("la boite fait 210 unites dessinees a quatre dixiemes, et avance de 84 pixels")
    void laBoiteEtLesBarres() {
        assertEquals(210, CGuiHist.BOX, EPS);
        assertEquals(0.4, CGuiHist.BOX_SCALE, EPS);
        assertEquals(84, CGuiHist.ADVANCE, EPS, "84 pixels dans le panneau, et non 210");
        assertEquals(CGuiHist.BOX * CGuiHist.BOX_SCALE, CGuiHist.ADVANCE, EPS,
                "c'est la regle qu'applique element() : sa hauteur une fois mise a l'echelle");
        assertEquals(30, CGuiHist.BOX_LIFT, EPS,
                "et le blank(-30) de l'original, qui la remonte sous le curseur");
        assertEquals(16, CGuiHist.BAR_WIDTH, EPS);
        assertEquals(120, CGuiHist.BAR_HEIGHT, EPS);
        assertEquals(56, CGuiHist.BAR_X, EPS, "la premiere barre");
        assertEquals(40, CGuiHist.BAR_STEP, EPS, "et les suivantes tous les quarante");
        assertEquals(78, CGuiHist.BAR_Y, EPS);
    }
}
