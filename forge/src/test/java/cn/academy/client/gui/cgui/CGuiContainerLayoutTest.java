package cn.academy.client.gui.cgui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Les nombres du cadre des machines, verifies sans lancer un jeu.
 *
 * <p>C'est tout l'interet d'avoir sorti ces chiffres de l'ecran : un {@code AbstractContainerScreen}
 * ne se construit pas sans {@code Minecraft}, donc un cadre qui garderait ses nombres ne se testerait
 * pas. Ici il n'y a que des widgets, et les deux choses qui comptent se verifient : <b>le document
 * tombe sur le coin des slots</b>, et chaque morceau tombe ou l'original le posait.
 */
@DisplayName("le cadre d'un ecran de machine")
class CGuiContainerLayoutTest {

    private static final double EPS = 1e-6;

    /** Le coin des slots, en pixels : ce que l'ecran passe a {@code place}. */
    private static final double LEFT = 100;
    private static final double TOP = 40;

    @Test
    @DisplayName("le document tombe sur le coin des slots, au demi-pixel pres")
    void leDocumentTombeSurLesSlots() {
        CWidget root = CGuiContainerLayout.root(new CWidget("root"));
        CGuiContainerLayout.place(root, LEFT, TOP);

        // 207 fait 172 plus 35 : la racine, accrochee au centre puis reculee de dix-huit unites,
        // tombe donc un demi-pixel a gauche du coin des slots, et un demi-pixel au-dessus. C'est le
        // calcul de l'original, et c'est lui qui fait que la page et les slots ne se contredisent
        // pas : sans ce demi-pixel, chaque slot se dessinerait a cote de son fond.
        assertEquals(LEFT - 0.5, root.absX, EPS);
        assertEquals(TOP - 0.5, root.absY, EPS);
        assertEquals(172, root.absW, EPS);
        assertEquals(187, root.absH, EPS);
    }

    @Test
    @DisplayName("la page est deux unites a gauche de la racine, et les onglets vingt encore a gauche")
    void laPageEtLesOnglets() {
        CWidget root = CGuiContainerLayout.root(new CWidget("root"));
        CWidget tab = CGuiContainerLayout.tab(CGuiDocument.read("rework/pageselect").copy(), 0, "inv");
        CWidget page = CGuiContainerLayout.page(CGuiDocument.read("rework/page_inv").copy());
        // L'ordre de l'original : le bouton d'abord, donc la page se dessine par-dessus.
        root.add(tab);
        root.add(page);

        CGuiContainerLayout.place(root, LEFT, TOP);

        // La page fait 176 dans une racine de 172 : centree, elle deborde donc d'une unite de chaque
        // cote, et c'est ce debordement qui se retrouve dans le fichier de l'original.
        assertEquals(root.absX - 2, page.absX, EPS);
        assertEquals(root.absY, page.absY, EPS);
        assertEquals(176, page.absW, EPS);
        assertEquals(187, page.absH, EPS);

        assertEquals(root.absX - 20, tab.absX, EPS);
        assertEquals(root.absY, tab.absY, EPS);
        assertEquals(24 * 0.7, tab.absW, EPS, "le bouton se dessine a sept dixiemes");
        assertNotNull(tab.texture);
        assertEquals("academy:textures/guis/icons/icon_inv.png", tab.texture.texture);
    }

    @Test
    @DisplayName("l'onglet survole et la page montree sont blancs, les autres gris")
    void lAllumageDesOnglets() {
        // Le FrameEvent de l'original, ses deux regles separees. L'onglet de la page montree est
        // blanc et plein, quoi qu'il arrive ; un autre onglet est gris — et sous la souris il perd
        // seulement son opacite, pas sa lumiere.
        int[] montre = CGuiContainerLayout.tabColor(true, false);
        assertEquals(255, montre[0]);
        assertEquals(255, montre[3]);
        assertEquals(montre[0], montre[1]);
        assertEquals(montre[1], montre[2]);

        int[] auRepos = CGuiContainerLayout.tabColor(false, false);
        assertEquals(204, auRepos[0], "gris : les quatre cinquiemes de la lumiere");
        assertEquals(204, auRepos[3], "et quatre cinquiemes de l'opacite aussi");

        int[] survole = CGuiContainerLayout.tabColor(false, true);
        assertEquals(204, survole[0], "survole, il reste gris...");
        assertEquals(255, survole[3], "...mais il s'opacifie");
    }

    @Test
    @DisplayName("les onglets descendent de vingt-deux pixels chacun")
    void lesOngletsDescendent() {
        CWidget root = CGuiContainerLayout.root(new CWidget("root"));
        CWidget premier = CGuiContainerLayout.tab(CGuiDocument.read("rework/pageselect").copy(), 0,
                "inv");
        CWidget troisieme = CGuiContainerLayout.tab(CGuiDocument.read("rework/pageselect").copy(), 2,
                "windbase");
        root.add(premier);
        root.add(troisieme);

        CGuiContainerLayout.place(root, LEFT, TOP);

        assertEquals(44, troisieme.absY - premier.absY, EPS);
        assertEquals("academy:textures/guis/icons/icon_windbase.png",
                troisieme.texture.texture, "et chacun porte l'icone de sa page");
    }

    @Test
    @DisplayName("le panneau d'infos se pose sept unites a droite de la racine")
    void lePanneauDInfos() {
        CWidget root = CGuiContainerLayout.root(new CWidget("root"));
        CWidget info = CGuiContainerLayout.info(new CWidget("info"), 18);
        root.add(info);

        CGuiContainerLayout.place(root, LEFT, TOP);

        assertEquals(root.absX + 179, info.absX, EPS);
        assertEquals(root.absY + 5, info.absY, EPS);
        assertEquals(50, info.absH, EPS, "il a une hauteur plancher");
        assertNotNull(info.texture, "sa boite porte son fond");
        assertEquals(128, info.texture.alpha, "un noir a moitie");
        assertEquals(0, info.texture.red, "et vraiment noir : le halo, lui, ne fait que le bord");
        assertNotNull(info.frame, "que le cadre souple arrondit");
        assertEquals(4, info.frame.margin, EPS, "en debordant de quatre unites");
    }

    @Test
    @DisplayName("le cadre souple pose deux bandes claires, l'une au-dessus de la boite")
    void leCadreSouple() {
        CWidget.Frame frame = new CWidget.Frame();

        // Les nombres de TechUI.BlendQuad : la bande du haut se pose 8,6 unites AU-DESSUS du bord,
        // la bande du bas 2 unites au-dessus du sien, et toutes deux debordent de 3,2 unites.
        assertEquals(-8.6, frame.topY, EPS);
        assertEquals(12, frame.topHeight, EPS);
        assertEquals(2, frame.bottomInset, EPS);
        assertEquals(8, frame.bottomHeight, EPS);
        assertEquals(3.2, frame.lineMargin, EPS);
        assertEquals("academy:textures/guis/blend_quad.png", frame.blend);
        assertEquals("academy:textures/guis/line.png", frame.line);
    }

    @Test
    @DisplayName("le souffle des fonds de page va de 172 a 216, sans sortir de la")
    void leSouffle() {
        int alpha = 0;
        for (int i = 0; i < 1000; i++) {
            alpha = CGuiContainerLayout.breatheAlpha(i * 0.01);
            assertEquals(Math.max(172, Math.min(216, alpha)), alpha);
        }

        assertEquals(194, CGuiContainerLayout.breatheAlpha(0.0));
        assertEquals(216, CGuiContainerLayout.breatheAlpha(0.8 * Math.PI / 2), "le plus clair");
        assertEquals(172, CGuiContainerLayout.breatheAlpha(0.8 * 3 * Math.PI / 2), "le plus sombre");
    }
}
