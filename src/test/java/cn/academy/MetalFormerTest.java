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

    /** La tolerance des comparaisons de nombres a virgule. */
    private static final double EPS = 1e-9;

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

        // Les noms SONT ceux de l'original (`TileMetalFormer.Mode.toString()`, donc en majuscules),
        // a une exception demandee par le joueur : « je prefere Etching plutot que juste Etch comme
        // dans le vrai », puis « je veux bien que tu l'ecrive en majuscule ». Ils etaient Rolling /
        // Cutting / Etching / Refining, c'est-a-dire une invention du port.
        assertEquals("PLATE", lang.get("academy.metal_former.mode.plate").getAsString());
        assertEquals("INCISE", lang.get("academy.metal_former.mode.incise").getAsString());
        assertEquals("ETCHING", lang.get("academy.metal_former.mode.etch").getAsString());
        assertEquals("REFINE", lang.get("academy.metal_former.mode.refine").getAsString());
    }

    /**
     * Les minerais du mod, dans l'etiquette que lit le raffinage.
     *
     * <p>Le mode REFINE se resout dans les recettes de fonte du jeu, mais seulement pour ce qui est
     * <b>un minerai</b> : c'est {@code forge:ores} qui le dit, et une etiquette qui perd un minerai le
     * rend invisible au raffinage — et aux autres mods, qui lisent la meme.
     */
    @Test
    @DisplayName("les minerais du mod sont etiquetes comme des minerais")
    void lesMineraisDuModSontEtiquetes() {
        java.util.List<String> attendus = java.util.List.of("academy:imagsil_ore",
                "academy:constraint_metal", "academy:reso_ore", "academy:crystal_ore");

        for (String path : new String[] { "data/forge/tags/items/ores.json",
                "data/forge/tags/blocks/ores.json" }) {
            java.util.List<String> valeurs = new java.util.ArrayList<>();
            json(path).getAsJsonArray("values").forEach(valeur -> valeurs.add(valeur.getAsString()));
            assertEquals(attendus, valeurs, path);
        }
    }

    /**
     * La barre de travail se pose par paliers, comme celle de l'original.
     *
     * <p>L'original montait bien son compteur de zero a soixante, mais ne le synchronisait que tous
     * les dix ticks : le client ne voyait donc que six valeurs, et le joueur se souvient de « gros
     * blocs qui apparaissent d'un coup ». Le port synchronise a chaque tick, donc la barre grimpait
     * d'un pixel a la fois — et comme le dessin arrondit sa largeur et sa texture separement, son bord
     * bougeait d'un pixel a gauche ou a droite. C'est ce reglage-la que ce test fige.
     */
    @Test
    @DisplayName("la barre de travail se pose par paliers, comme l'original")
    void laBarreSePoseParPaliers() {
        assertEquals(0.0, MetalFormerBlockEntity.displayProgress(0.0), EPS, "rien fait, rien montre");
        assertEquals(0.0, MetalFormerBlockEntity.displayProgress(0.16), EPS, "premier morceau pas atteint");
        assertEquals(1.0 / 6, MetalFormerBlockEntity.displayProgress(0.17), EPS, "un morceau sur six");
        assertEquals(1.0 / 6, MetalFormerBlockEntity.displayProgress(0.33), EPS, "toujours le premier");
        assertEquals(2.0 / 6, MetalFormerBlockEntity.displayProgress(0.34), EPS, "le deuxieme");
        assertEquals(1.0, MetalFormerBlockEntity.displayProgress(1.0), EPS, "le dernier morceau");
        assertEquals(1.0, MetalFormerBlockEntity.displayProgress(4.0), EPS, "et jamais au-dela");

        // La barre ne montre jamais plus que le travail fait : elle peut etre en retard, pas en avance.
        for (double avancement = 0.0; avancement <= 1.0; avancement += 0.01) {
            assertTrue(MetalFormerBlockEntity.displayProgress(avancement) <= avancement + EPS,
                    "la barre ne depasse pas le travail fait, a " + avancement);
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
        return json("assets/academy/lang/en_us.json");
    }

    private static JsonObject json(String path) {
        try (InputStream in = MetalFormerTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, path + " est introuvable");
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (Exception failure) {
            throw new AssertionError(path + " n'a pas pu etre lu", failure);
        }
    }
}
