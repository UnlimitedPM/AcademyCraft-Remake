package cn.academy;

import cn.academy.client.gui.cgui.CGuiDocument;
import cn.academy.client.gui.cgui.CWidget;
import cn.academy.crafting.MetalFormerMode;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le formeur de metal : sa page, ses emplacements, et les quatre modes avec leur icone.
 *
 * <p>Ce sont les trois choses qui se verifient sans lancer le jeu, et les trois qui se cassent en
 * silence : un widget renomme dans le document laisserait l'ecran muet, un emplacement deplace
 * poserait son objet a cote de son trou, et un mode sans icone ni nom afficherait un carre vide ou
 * une cle brute. Le port a deja paye la derniere : quatre noms d'aptitudes manquaient a la langue du
 * developpeur, et le joueur les a vus en cle brute avant tout test.
 */
class MetalFormerTest {

    /** La page de l'original, celle que l'ecran pose sous l'identifiant « inv ». */
    private static final String DOCUMENT = "rework/page_metalformer";

    @Test
    @DisplayName("la page du formeur porte tout ce que l'ecran demande")
    void laPagePorteCeQueLEcranDemande() {
        CWidget page = CGuiDocument.read(DOCUMENT);

        CWidget progress = find(page, "progress");
        assertNotNull(progress, "la barre de travail");
        assertNotNull(progress.bar, "la barre de travail doit porter sa barre");

        CWidget icon = find(page, "icon_mode");
        assertNotNull(icon, "l'icone du mode");

        assertNotNull(find(page, "btn_left"), "la fleche de gauche");
        assertNotNull(find(page, "btn_right"), "la fleche de droite");
    }

    @Test
    @DisplayName("les emplacements sont ceux de l'original")
    void lesEmplacementsSontCeuxDeLOriginal() {
        // ContainerMetalFormer, 1.12.2 : `new SlotMFItem(tile, 0, 13, 49)`, le second a 143, et la
        // batterie a 42, 80. Comme le cadre du document se pose sur le conteneur, ce sont ces
        // nombres-la qui mettent chaque emplacement sur son trou.
        assertEquals(13, MetalFormerMenu.IN_SLOT_X);
        assertEquals(143, MetalFormerMenu.OUT_SLOT_X);
        assertEquals(49, MetalFormerMenu.SLOT_Y);
        assertEquals(42, MetalFormerMenu.BATTERY_SLOT_X);
        assertEquals(80, MetalFormerMenu.BATTERY_SLOT_Y);

        // Et les deux boutons de mode, ceux que l'ecran envoie par le chemin vanilla.
        assertEquals(0, MetalFormerMenu.BUTTON_MODE_PREVIOUS);
        assertEquals(1, MetalFormerMenu.BUTTON_MODE_NEXT);
    }

    @Test
    @DisplayName("chaque mode a son icone et son nom")
    void chaqueModeASonIconeEtSonNom() {
        JsonObject lang = langue();

        for (MetalFormerMode mode : MetalFormerMode.values()) {
            String icon = "assets/academy/textures/guis/icons/icon_former_" + mode.getId() + ".png";
            assertNotNull(MetalFormerTest.class.getClassLoader().getResourceAsStream(icon),
                    icon + " n'existe pas : le mode s'afficherait en carre vide");

            String key = "academy.metal_former.mode." + mode.getId();
            assertTrue(lang.has(key), "la langue n'a pas " + key);
            assertTrue(!lang.get(key).getAsString().isBlank(), key + " est vide");
        }
    }

    /** Le document range ses widgets les uns dans les autres : on les cherche donc a la main. */
    private static CWidget find(CWidget parent, String name) {
        CWidget direct = parent.child(name);
        if (direct != null) return direct;
        for (CWidget child : parent.children()) {
            CWidget found = find(child, name);
            if (found != null) return found;
        }
        return null;
    }

    private static JsonObject langue() {
        String path = "assets/academy/lang/en_us.json";
        try (InputStream in = MetalFormerTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, path + " est introuvable");
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (Exception failure) {
            throw new AssertionError("la langue n'a pas pu etre lue", failure);
        }
    }
}
