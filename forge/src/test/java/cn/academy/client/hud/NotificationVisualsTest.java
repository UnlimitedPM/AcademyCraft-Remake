package cn.academy.client.hud;

import cn.academy.client.gui.CustomizeUiLayout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le deroulement d'une notification, relu sans Minecraft.
 *
 * <p>Tout y est fonction de l'age en secondes, ce qui permet de figer chaque temps separement :
 * les six secondes de vie, l'entree du panneau, l'arrivee du texte, le glissement de l'icone et
 * la sortie. C'est le genre de courbe qu'on ne verifie pas a l'oeil.
 */
class NotificationVisualsTest {

    @Test
    @DisplayName("une notification vit six secondes")
    void uneNotificationVitSixSecondes() {
        assertTrue(NotificationVisuals.visible(0f));
        assertTrue(NotificationVisuals.visible(5.9f));
        assertFalse(NotificationVisuals.visible(6f));
        assertFalse(NotificationVisuals.visible(9f));
    }

    @Test
    @DisplayName("le panneau entre en une demi-seconde et sort en trois dixiemes")
    void lePanneauEntreEtSort() {
        assertEquals(0f, NotificationVisuals.fade(0f), 0.001f);
        assertEquals(0.5f, NotificationVisuals.fade(0.25f), 0.001f);
        assertEquals(1f, NotificationVisuals.fade(0.5f), 0.001f);
        // La tenue, puis la sortie : elle commence a 5,7 s et finit au bout des six.
        assertEquals(1f, NotificationVisuals.fade(5f), 0.001f);
        assertEquals(0.5f, NotificationVisuals.fade(5.85f), 0.001f);
        assertEquals(0f, NotificationVisuals.fade(6f), 0.001f);
    }

    @Test
    @DisplayName("le texte n'arrive qu'avec le glissement")
    void leTexteNArriveQuAvecLeGlissement() {
        // Pendant l'entree du panneau, il n'y a pas encore de ligne du tout.
        assertEquals(0f, NotificationVisuals.textAlpha(0.4f), 0.001f);
        // Une demi-seconde plus tard, le glissement est fini : les deux lignes sont pleines.
        assertEquals(1f, NotificationVisuals.textAlpha(1f), 0.001f);
        // Et entre les deux, elles montent doucement — le sinus de l'original.
        assertTrue(NotificationVisuals.textAlpha(0.55f) < 0.35f,
                "au tout debut du glissement, le texte est encore presque invisible");
        assertTrue(NotificationVisuals.textAlpha(0.75f) < 1f);
    }

    @Test
    @DisplayName("l'icone glisse de la droite et ralentit en arrivant")
    void lIconeGlisseDeLaDroiteEtRalentit() {
        assertEquals(CustomizeUiLayout.NOTIFY_ICON_START_X, NotificationVisuals.iconX(0f), 0.001f);
        assertEquals(CustomizeUiLayout.NOTIFY_ICON_X, NotificationVisuals.iconX(1f), 0.001f);
        // A la moitie du TEMPS, elle a fait plus de la moitie du chemin : l'original ralentit la
        // fin (`sin(prog * PI/2)`), comme un objet qui se pose.
        float middle = (CustomizeUiLayout.NOTIFY_ICON_START_X + CustomizeUiLayout.NOTIFY_ICON_X) / 2f;
        assertTrue(NotificationVisuals.iconX(0.75f) < middle,
                "le glissement doit ralentir en arrivant");
        // Et elle ne recule jamais.
        assertTrue(NotificationVisuals.iconX(0.9f) <= NotificationVisuals.iconX(0.7f));
    }

    @Test
    @DisplayName("l'icone entre pendant la seconde moitie de l'entree")
    void lIconeEntrePendantLaPremiereMoitie() {
        // Les chiffres de l'original : elle commence a 0,2 s et finit a 0,5 s — mais son opacite
        // est celle de la notification entiere, qui est encore en train de monter.
        assertEquals(0f, NotificationVisuals.iconAlpha(0.2f), 0.001f);
        assertEquals(0.35f, NotificationVisuals.iconAlpha(0.35f), 0.001f);
        assertEquals(1f, NotificationVisuals.iconAlpha(0.5f), 0.001f);
        assertEquals(1f, NotificationVisuals.iconAlpha(3f), 0.001f);
        // Et elle s'efface avec le reste a la fin.
        assertEquals(0f, NotificationVisuals.iconAlpha(6f), 0.001f);
    }
}
