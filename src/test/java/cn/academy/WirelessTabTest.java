package cn.academy;

import cn.academy.client.gui.cgui.CGuiDocument;
import cn.academy.client.gui.cgui.CGuiWireless;
import cn.academy.client.gui.cgui.CWidget;
import cn.academy.energy.NodeFinder;
import cn.academy.energy.WirelessMachine;
import cn.academy.energy.WirelessNode;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L'onglet sans fil : sa page, son champ de mot de passe, ses textes, et son icone d'onglet.
 *
 * <p>Ce sont exactement les choses qui se cassent en silence. Un widget renomme dans le document
 * laisserait une page vide sans qu'aucune compilation ne s'en plaigne ; un champ qui a perdu son
 * masque afficherait le mot de passe du noeud a l'ecran ; une reponse du serveur sans traduction
 * afficherait sa cle brute ; et un conteneur qui a cesse de se declarer {@code WirelessMachine}
 * rendrait l'onglet muet — il demanderait bien sa liste, mais le serveur ne saurait pas de quelle
 * machine on lui parle.
 *
 * <p>Le port a deja paye le dernier genre de panne : quatre noms d'aptitudes manquaient a la langue
 * du developeur, et le joueur les a vus en cle brute avant tout test.
 */
class WirelessTabTest {

    /** La page de l'original, celle que l'onglet pose sous l'identifiant {@code wireless}. */
    private static final String DOCUMENT = "rework/page_wireless";

    private static final String ICONS = "assets/academy/textures/guis/icons/";

    @Test
    @DisplayName("la page sans fil porte tout ce que l'onglet demande")
    void laPagePorteToutCeQueLOngletDemande() {
        CWidget page = CGuiDocument.read(DOCUMENT);

        assertNotNull(find(page, "panel_wireless"), "le panneau de l'onglet");
        assertNotNull(find(page, "zone_elementlist"), "la zone des lignes");

        CWidget template = find(page, "element");
        assertNotNull(template, "le gabarit d'une ligne");
        assertNotNull(find(template, "text_name"), "le nom du noeud");
        assertNotNull(find(template, "icon_key"), "la petite cle des noeuds a mot de passe");
        assertNotNull(find(template, "icon_connect"), "l'icone de connexion");
        assertNotNull(find(template, "input_pass"), "le champ de mot de passe");

        CWidget connected = find(page, "elem_connected");
        assertNotNull(connected, "le cadre du noeud lie");
        assertNotNull(find(connected, "text_name"), "le nom du noeud lie");
        CWidget icon = find(connected, "icon_connect");
        assertNotNull(icon, "le bouton de deconnexion");
        assertNotNull(icon.texture, "il doit porter une icone, pas un carre blanc");

        assertNotNull(find(page, "btn_arrowup"), "la fleche du haut");
        assertNotNull(find(page, "btn_arrowdown"), "la fleche du bas");
        assertNotNull(find(page, "text_connected"), "le titre du cadre du noeud lie");
        assertNotNull(find(page, "text_available"), "le titre de la liste");
    }

    @Test
    @DisplayName("le champ de mot de passe est editable et masque")
    void leChampDeMotDePasseEstEditableEtMasque() {
        CWidget field = find(find(CGuiDocument.read(DOCUMENT), "element"), "input_pass");
        assertNotNull(field, "le champ de mot de passe");
        assertNotNull(field.text, "il doit porter sa zone de texte");

        assertTrue(field.text.allowEdit, "un mot de passe se tape");
        assertTrue(field.text.doesEcho, "et il ne doit pas s'afficher");
        assertEquals('*', field.text.echoChar, "masque par une etoile, comme l'original");
    }

    /**
     * Le document de l'original ecrit ses deux libelles en dur — « Connected » et « Available » — et
     * celui du port est le meme au bit pres. L'ecran les remplace par leur cle de langue : si la
     * traduction disait autre chose, l'onglet ne dirait plus la meme chose que l'original.
     */
    @Test
    @DisplayName("les deux libelles ecrits en dur dans la page gardent leur mot")
    void lesDeuxLibellesEcritsEnDurGardentLeurMot() {
        CWidget page = CGuiDocument.read(DOCUMENT);
        JsonObject lang = langue();

        assertEquals(content(find(page, "text_connected")), lang.get("ac.gui.wireless.connected")
                .getAsString(), "le titre du cadre du noeud lie");
        assertEquals(content(find(page, "text_available")), lang.get("ac.gui.wireless.available")
                .getAsString(), "le titre de la liste");
    }

    @Test
    @DisplayName("tous les textes de l'onglet sont dans la langue")
    void tousLesTextesDeLOngletSontDansLaLangue() {
        JsonObject lang = langue();

        List<String> keys = new ArrayList<>(List.of("ac.gui.wireless.connected",
                "ac.gui.wireless.available", "ac.gui.wireless.not_connected",
                "ac.gui.wireless.unlinked"));

        // Un message par reponse possible du serveur : c'est l'enumeration qui les nomme, donc un cas
        // ajoute sans texte se verrait ici plutot qu'en jeu, en cle brute, sous les yeux du joueur.
        for (NodeFinder.LinkOutcome outcome : NodeFinder.LinkOutcome.values()) {
            keys.add("ac.gui.wireless.link." + outcome.name().toLowerCase(Locale.ROOT));
        }

        for (String key : keys) {
            assertTrue(lang.has(key), "la langue n'a pas " + key);
            assertFalse(lang.get(key).getAsString().isBlank(), key + " est vide");
        }
    }

    @Test
    @DisplayName("l'onglet sans fil a son icone, comme la page d'inventaire")
    void lOngletSansFilASonIcone() {
        // Le moteur nomme l'icone d'un onglet d'apres l'identifiant de sa page
        // (voir CGuiContainerLayout.tab) : c'est ce nom-la qui doit avoir son image.
        assertEquals("wireless", CGuiWireless.PAGE);

        // Et la page elle-meme porte un logo qui dit ce qu'on cherche : un noeud pour une machine,
        // un Matrix pour un noeud. C'est la seule difference entre les deux moities de l'onglet.
        for (String icon : new String[] { "icon_wireless.png", "icon_inv.png", "icon_tonode.png",
                "icon_tomatrix.png", "icon_connected.png", "icon_unconnected.png" }) {
            assertNotNull(WirelessTabTest.class.getClassLoader().getResourceAsStream(ICONS + icon),
                    ICONS + icon + " manque : l'onglet serait un carre vide");
        }
    }

    /**
     * Les six machines dont l'original portait l'onglet, et qui le portent encore — plus le noeud,
     * qui en a l'autre moitie.
     *
     * <p>C'est le seul lien que rien d'autre ne verifie : l'ecran pose sa page, le serveur prend la
     * position du conteneur ouvert, et les deux se rejoignent par cette interface. Un conteneur qui
     * cesse de la declarer laisse l'onglet en place, et l'onglet ne peut plus rien demander — pas
     * d'erreur, pas de message, juste une liste qui ne se remplit jamais.
     */
    @Test
    @DisplayName("les sept porteurs de l'onglet se declarent au serveur")
    void lesSeptPorteursDeLOnglet() {
        List<Class<?>> menus = List.of(SolarGenMenu.class, PhaseGeneratorMenu.class, WindgenBaseMenu.class,
                MetalFormerMenu.class, ImagFusorMenu.class, AbilityInterfererMenu.class);

        for (Class<?> menu : menus) {
            assertTrue(WirelessMachine.class.isAssignableFrom(menu),
                    menu.getSimpleName() + " doit se declarer WirelessMachine");
            assertTrue(Arrays.stream(menu.getMethods())
                            .anyMatch(method -> method.getName().equals("machinePos")),
                    menu.getSimpleName() + " doit dire de quelle machine il parle");
        }

        // Le noeud, lui, cherche un Matrix : c'est l'autre moitie de la meme page, et l'autre
        // interface.
        assertTrue(WirelessNode.class.isAssignableFrom(NodeMenu.class),
                "NodeMenu doit se declarer WirelessNode");
        assertTrue(Arrays.stream(NodeMenu.class.getMethods())
                        .anyMatch(method -> method.getName().equals("nodePos")),
                "NodeMenu doit dire de quel noeud il parle");
    }

    /** Le contenu de la zone de texte d'un widget, ou {@code null} s'il n'en a pas. */
    private static String content(CWidget widget) {
        assertNotNull(widget, "le widget doit exister");
        assertNotNull(widget.text, widget.name() + " doit porter sa zone de texte");
        return widget.text.content;
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
        try (InputStream in = WirelessTabTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, path + " est introuvable");
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (Exception failure) {
            throw new AssertionError(path + " n'a pas pu etre lu", failure);
        }
    }
}
