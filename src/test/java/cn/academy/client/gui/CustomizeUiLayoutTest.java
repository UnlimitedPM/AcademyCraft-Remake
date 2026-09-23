package cn.academy.client.gui;

import cn.academy.client.hud.HudElement;
import cn.academy.client.hud.HudLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La geometrie de l'ecran de reglage du HUD.
 *
 * <p>Ces nombres ne sont pas un gout personnel : ils viennent du {@code ui_edit.xml} de
 * l'original, qui est ce que son ecran lisait pour se dessiner. Le panneau y est pose en
 * (94, 104) et mesure 144x203 a l'echelle 0.5 ; une ligne y fait 128x24 et son texte part a
 * 10 pixels du bord ; le cadre des deux champs se met a 5 pixels a droite de la ligne et
 * mesure 90x16. Le test les fige, et verifie au passage que le panneau ne recouvre pas
 * l'apercu des notifications — la raison pour laquelle l'original le pose si bas.
 */
class CustomizeUiLayoutTest {

    @Test
    void lePanneauEstCeluiDeLoriginal() {
        assertEquals(94, CustomizeUiLayout.PANEL_X);
        assertEquals(104, CustomizeUiLayout.PANEL_Y);
        assertEquals(72, CustomizeUiLayout.panelWidth(), "144 dessine a moitie");
        assertEquals(102, CustomizeUiLayout.panelHeight(), "203 dessine a moitie");
    }

    @Test
    void lePanneauNeRecouvrePasLesNotifications() {
        int hauteur = HudElement.NOTIFICATION.getPreviewHeight();
        int bas = new HudLayout().placeY(HudElement.NOTIFICATION, 240, hauteur) + hauteur;

        assertTrue(CustomizeUiLayout.PANEL_Y >= bas,
                "le panneau commence sous l'apercu des notifications, en haut a gauche");
    }

    @Test
    void lesLignesSontCellesDuXml() {
        assertEquals(98, CustomizeUiLayout.rowLeft(), "8 pixels dans le panneau, a moitie");
        assertEquals(64, CustomizeUiLayout.rowWidth(), "128 a moitie");
        assertEquals(12, CustomizeUiLayout.rowHeight(), "24 a moitie");
        assertEquals(122, CustomizeUiLayout.rowTop(0), "36 pixels dans le panneau, a moitie");
        assertEquals(158, CustomizeUiLayout.rowTop(3), "la quatrieme ligne, 24 pixels plus bas");
        assertEquals(103, CustomizeUiLayout.rowTextLeft(), "10 pixels du bord, a moitie");
    }

    @Test
    void lEnTeteEstCentreDansLePanneau() {
        assertEquals(101, CustomizeUiLayout.headerTextLeft());
        assertEquals(115, CustomizeUiLayout.headerTextCenterY());
    }

    @Test
    void lesChampsSePosentALaDroiteDeLaLigneChoisie() {
        // Le cadre de l'original n'est pas a une place fixe : son code le repositionne a
        // chaque ligne choisie, juste a droite de cette ligne.
        assertEquals(167, CustomizeUiLayout.editLeft());
        assertEquals(120, CustomizeUiLayout.editTop(0), "centre sur la premiere ligne");
        assertEquals(132, CustomizeUiLayout.editTop(1), "et il suit la ligne suivante");
        assertEquals(177, CustomizeUiLayout.fieldLeft(true), "le champ X est a 10 dans le cadre");
        assertEquals(220, CustomizeUiLayout.fieldLeft(false), "et le champ Y a 53");
        assertEquals(123, CustomizeUiLayout.fieldTop(0), "les deux a 3 pixels du haut");
        assertEquals(34, CustomizeUiLayout.FIELD_W, "un champ fait 34 de large");
    }

    @Test
    void laPoliceEstCelleDeLoriginalRameneeALaNotre() {
        // La police embarquee est rasterisee a 9 pixels, et le rapport d'un dixieme fait tomber
        // les textes de l'ecran a environ un pour un avec ce raster. Un corps 18 dans un panneau
        // a moitie donne donc 0,9 ; le titre d'une notification, ecrit en corps 38 et dessine au
        // quart, doit se lire en multipliant les deux.
        assertEquals(0.9f, CustomizeUiLayout.fontScale(CustomizeUiLayout.HEADER_FONT), 0.001f);
        assertEquals(0.9f, CustomizeUiLayout.fontScale(CustomizeUiLayout.ROW_FONT), 0.001f);
        assertEquals(3.8f, CustomizeUiLayout.plainFontScale(38.0f), 0.001f);
        assertEquals(5.4f, CustomizeUiLayout.plainFontScale(54.0f), 0.001f);
        // Le titre du lecteur media : dessine a l'echelle un, donc pile sur le raster.
        assertEquals(1.0f, CustomizeUiLayout.plainFontScale(CustomizeUiLayout.MEDIA_TITLE_FONT),
                0.001f);
    }

    @Test
    void leLecteurMediaEstCeluiDeSonXml() {
        // Son media_player_aux.xml : une barre de 120 a partir de 14, remplie a moitie, posee
        // a 27 ; son fond descend un cheveu plus bas (27,2) et fait 1,1 de haut.
        assertEquals(60, Math.round(CustomizeUiLayout.MEDIA_BAR_W
                * CustomizeUiLayout.MEDIA_BAR_PROGRESS), "la barre est remplie a moitie");
        assertEquals(1, CustomizeUiLayout.MEDIA_BAR_BACK_H, "le fond fait un pixel");
        // Le fond doit etre AU CENTRE de la progression : du blanc au-dessus et en dessous.
        assertTrue(CustomizeUiLayout.MEDIA_BAR_FILL_TOP < CustomizeUiLayout.MEDIA_BAR_Y,
                "il reste du blanc au-dessus du fond");
        assertTrue(CustomizeUiLayout.MEDIA_BAR_Y + CustomizeUiLayout.MEDIA_BAR_BACK_H
                        < CustomizeUiLayout.MEDIA_BAR_FILL_TOP + CustomizeUiLayout.MEDIA_BAR_FILL_H,
                "et du blanc en dessous");

        // Ses deux textes sont cales par le BAS de leur boite de 10 (l'une posee a 17, l'autre
        // a 27) : le titre tombe donc a 27 et la duree a 37, dix pixels plus bas, et non a la
        // meme hauteur comme on pourrait le croire.
        assertEquals(10, CustomizeUiLayout.MEDIA_TIME_BOTTOM - CustomizeUiLayout.MEDIA_TITLE_BOTTOM);
        assertEquals(CustomizeUiLayout.MEDIA_BAR_Y, CustomizeUiLayout.MEDIA_TITLE_BOTTOM,
                "le titre s'arrete juste au-dessus de la barre");
    }

    @Test
    void leTexteEstCentreSurSaLigne() {
        assertEquals(118, CustomizeUiLayout.textTop(122, 8), "8 pixels centres sur 122");
        assertEquals(115, CustomizeUiLayout.textTop(119, 8), "et cela suit la ligne");
    }
}
