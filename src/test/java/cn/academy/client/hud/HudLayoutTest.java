package cn.academy.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le placement des elements du HUD.
 *
 * <p>Ces nombres ne sont pas arbitraires : ce sont ceux de l'original, releves dans le jeu
 * (les quatre couples par defaut) et dans son code (les bornes, et le fait que la capture
 * d'une touche se fait sur la partie droite de la ligne). Le test les fige pour qu'une
 * retouche de l'ancrage ne deplace pas discretement le HUD de quelqu'un.
 */
class HudLayoutTest {

    @Test
    void lesQuatreDefautsSontCeuxDeLoriginal() {
        HudLayout layout = new HudLayout();

        // Les couples exacts, dans l'ordre du panneau « Elements » de l'original.
        assertEquals(-12.0, layout.getX(HudElement.CP_BAR));
        assertEquals(12.0, layout.getY(HudElement.CP_BAR));

        assertEquals(0.0, layout.getX(HudElement.KEY_HINT));
        assertEquals(30.0, layout.getY(HudElement.KEY_HINT));

        assertEquals(0.0, layout.getX(HudElement.NOTIFICATION));
        assertEquals(15.0, layout.getY(HudElement.NOTIFICATION));

        assertEquals(-6.0, layout.getX(HudElement.MEDIA));
        assertEquals(-6.0, layout.getY(HudElement.MEDIA));
    }

    @Test
    void lesQuatreElementsSontCeuxDeLoriginal() {
        assertEquals(4, HudElement.values().length, "l'original a quatre elements");

        assertNotNull(HudElement.byName("cpbar"));
        assertNotNull(HudElement.byName("keyhint"));
        assertNotNull(HudElement.byName("notification"));
        assertNotNull(HudElement.byName("media"));
        assertNull(HudElement.byName("inconnu"), "un nom inconnu ne designe rien");
        assertNull(HudElement.byName(null));

        // Les noms sont ceux ecrits dans la config : ils ne doivent pas bouger.
        assertEquals("cpbar", HudElement.CP_BAR.getName());
        assertEquals("keyhint", HudElement.KEY_HINT.getName());
        assertEquals("notification", HudElement.NOTIFICATION.getName());
        assertEquals("media", HudElement.MEDIA.getName());
    }

    @Test
    void uneValeurHorsBornesEstRefusee() {
        HudLayout layout = new HudLayout();

        assertFalse(layout.set(HudElement.CP_BAR, 513.0, 0.0), "au-dela de 512, on refuse");
        assertFalse(layout.set(HudElement.CP_BAR, 0.0, -513.0), "et en dessous de -512 aussi");
        assertEquals(-12.0, layout.getX(HudElement.CP_BAR), "le refus ne doit rien changer");

        assertTrue(layout.set(HudElement.CP_BAR, 512.0, -512.0), "les bornes elles-memes passent");
        assertEquals(512.0, layout.getX(HudElement.CP_BAR));

        assertTrue(HudLayout.isValid(0.0));
        assertFalse(HudLayout.isValid(Double.NaN));
    }

    @Test
    void lePlacementReproduitLesQuatreCoins() {
        // Un ecran de 427x240, comme un petit ecran a l'echelle 3 : ce qui compte est la
        // maniere dont chaque ancrage se resout, pas la taille elle-meme.
        int w = 427;
        int h = 240;

        // Barre de CP : accrochee a droite et en haut, 12 pixels de marge.
        int cpW = 193;
        int cpH = 29;
        assertEquals(w - cpW - 12, new HudLayout().placeX(HudElement.CP_BAR, w, cpW),
                "le bord droit de la barre de CP est a 12 pixels du bord droit de l'ecran");
        assertEquals(12, new HudLayout().placeY(HudElement.CP_BAR, h, cpH),
                "et son haut est a 12 pixels du haut de l'ecran");

        // Notifications : accrochees a gauche et en haut.
        assertEquals(0, new HudLayout().placeX(HudElement.NOTIFICATION, w, 100));
        assertEquals(15, new HudLayout().placeY(HudElement.NOTIFICATION, h, 20),
                "15 pixels sous le haut de l'ecran");

        // Lecteur media : accroche a droite et en bas, 6 pixels de marge.
        assertEquals(w - 100 - 6, new HudLayout().placeX(HudElement.MEDIA, w, 100));
        assertEquals(h - 20 - 6, new HudLayout().placeY(HudElement.MEDIA, h, 20),
                "6 pixels au-dessus du bas de l'ecran");

        // Rappel des touches : accroche a droite, 30 pixels au-dessus du milieu.
        int keyH = 96;
        assertEquals(w - 60, new HudLayout().placeX(HudElement.KEY_HINT, w, 60));
        assertEquals(h / 2 - keyH / 2 - 30, new HudLayout().placeY(HudElement.KEY_HINT, h, keyH),
                "au-dessus du milieu de l'ecran");
    }

    @Test
    void unDeplacementSeRelit() {
        HudLayout layout = new HudLayout();
        assertTrue(layout.set(HudElement.NOTIFICATION, -40.0, 100.0));
        assertEquals(-40.0, layout.getX(HudElement.NOTIFICATION));
        assertEquals(100.0, layout.getY(HudElement.NOTIFICATION));

        // Et la remise a zero rend les defauts, sans toucher aux autres.
        layout.set(HudElement.MEDIA, 10.0, 10.0);
        layout.reset(HudElement.NOTIFICATION);
        assertEquals(0.0, layout.getX(HudElement.NOTIFICATION));
        assertEquals(15.0, layout.getY(HudElement.NOTIFICATION));
        assertEquals(10.0, layout.getX(HudElement.MEDIA), "les autres elements ne bougent pas");
    }
}
