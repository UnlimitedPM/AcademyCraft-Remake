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
        // Le rapport d'un douzieme ramene le corps de l'original a l'echelle du panneau : un
        // corps 18 dans un panneau a moitie donne 18/12 * 0,5 = 0,75. C'est une echelle de
        // composition : l'ecran la ramene ensuite a un multiple entier de la police du jeu,
        // parce qu'une taille fractionnaire est reechantillonnee, donc floue.
        assertEquals(0.75f, CustomizeUiLayout.fontScale(CustomizeUiLayout.HEADER_FONT), 0.001f);
        assertEquals(0.75f, CustomizeUiLayout.fontScale(CustomizeUiLayout.ROW_FONT), 0.001f);
        assertEquals(3.1667f, CustomizeUiLayout.plainFontScale(38.0f), 0.001f);
        assertEquals(4.5f, CustomizeUiLayout.plainFontScale(54.0f), 0.001f);
    }

    @Test
    void leLecteurMediaEstCeluiDeSonXml() {
        // Sa barre : le fond gris fait 1,3 pixel de haut et la progression blanche 2,1, donc elle
        // deborde a peine de chaque cote. Tout est en dixiemes, d'ou les treize et vingt et un.
        assertEquals(60, Math.round(CustomizeUiLayout.MEDIA_BAR_W
                * CustomizeUiLayout.MEDIA_BAR_PROGRESS), "la barre est remplie a moitie");
        assertEquals(13, CustomizeUiLayout.MEDIA_BAR_GREY_TENTHS, "le gris fait 1,3 pixel");
        assertEquals(21, CustomizeUiLayout.MEDIA_BAR_WHITE_TENTHS, "le blanc en fait 2,1");
        assertTrue(CustomizeUiLayout.MEDIA_BAR_WHITE_TENTHS
                        > CustomizeUiLayout.MEDIA_BAR_GREY_TENTHS,
                "le blanc doit deborder du gris");
        assertTrue(CustomizeUiLayout.MEDIA_BAR_WHITE_TENTHS
                        - CustomizeUiLayout.MEDIA_BAR_GREY_TENTHS <= 8,
                "mais a peine : quatre dixiemes de chaque cote au plus");

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
