package cn.academy.terminal.tutorial;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le contenu livre avec le mod, et ce qui l'ouvre.
 *
 * Comme le document des credits, ce test lit les fichiers reellement livres : une
 * traduction oubliee, un fichier vide ou une balise mal recopiee font donc echouer le
 * build, au lieu de se decouvrir a l'ecran chez le joueur.
 */
class TutorialLibraryTest {

    /** L'original declarait treize tutoriels hors le pont d'energie, absent du port. */
    private static final int TUTORIALS = 13;

    @Test
    void lesTutorielsDeLOriginalSontTousLa() {
        assertEquals(TUTORIALS, TutorialLibrary.entries().size());

        Set<String> ids = new HashSet<>();
        for (TutorialLibrary.Entry entry : TutorialLibrary.entries()) {
            assertTrue(ids.add(entry.id()), "identifiant en double : " + entry.id());
        }
        assertNotNull(TutorialLibrary.byId("welcome"), "le premier tutoriel de l'original");
        assertNotNull(TutorialLibrary.byId("wireless_network"), "le dernier");
        assertNull(TutorialLibrary.byId("energy_bridge"),
                "le pont d'energie n'est pas livre : il n'existait qu'avec un mod d'energie");
    }

    /** Les langues livrees avec le mod, celles des fichiers de {@code lang/}. */
    private static final java.util.List<String> LANGUAGES =
            java.util.List.of("en_us", "fr_fr", "de_de", "es_es", "zh_cn");

    @Test
    void chaqueTutorielEstEcritDansLesCinqLangues() {
        // Le joueur a vu le trou : le MisakaCloud parlait anglais en francais, parce que les pages
        // n'existaient qu'en en_us et zh_cn. Une langue sans fichier retombe sur l'anglais SANS RIEN
        // DIRE, donc l'oubli se verifie ici.
        java.util.List<String> missing = new java.util.ArrayList<>();
        for (String language : LANGUAGES) {
            for (TutorialLibrary.Entry entry : TutorialLibrary.entries()) {
                TutorialText text = TutorialLibrary.load(entry.id(), language);
                // Un titre chinois tient en deux caracteres (« 资源 ») : c'est le fait qu'il soit
                // VIDE qui compte, pas sa longueur.
                if (text.isEmpty() || text.title().isBlank() || text.content().length() <= 20) {
                    missing.add(language + "/" + entry.id());
                }
                assertFalse(text.content().contains("![content]"),
                        "la balise ne doit pas rester dans le texte : " + language + "/"
                                + entry.id());
            }
        }
        assertTrue(missing.isEmpty(), "pages absentes ou vides : " + missing);
    }

    @Test
    void uneVersionTraduiteNeRecopiePasLAnglais() {
        // Un titre suffit comme temoin : c'est lui qu'on lit dans la liste du MisakaCloud.
        for (String language : java.util.List.of("fr_fr", "de_de", "es_es")) {
            for (TutorialLibrary.Entry entry : TutorialLibrary.entries()) {
                String english = TutorialLibrary.load(entry.id(), "en_us").title();
                String translated = TutorialLibrary.load(entry.id(), language).title();
                assertFalse(translated.equals(english), language + "/" + entry.id()
                        + " porte encore le titre anglais « " + english + " »");
            }
        }
    }

    @Test
    void uneLangueInconnueRetombeSurLAnglais() {
        // Comme l'original : un tutoriel traduit a moitie ne doit pas disparaitre.
        TutorialText fallback = TutorialLibrary.load("welcome", "xx_yy");

        assertFalse(fallback.isEmpty(), "la langue de repli est l'anglais");
        assertEquals(TutorialLibrary.load("welcome", "en_us").title(), fallback.title());
    }

    @Test
    void unTutorielInconnuNeLevePas() {
        TutorialText text = TutorialLibrary.load("un_tutoriel_qui_n_existe_pas", "en_us");

        assertTrue(text.isEmpty(), "un tutoriel inconnu rend un texte vide, pas une exception");
    }

    @Test
    void lesTutorielsDObjetDemandentDesObjetsQuiExistent() {
        // Les noms sont ceux du port, qui a garde ceux de la 1.12.2 (dev_normal,
        // dev_advanced, phase_gen).
        // Un nom faux ne ferait rien tomber en jeu : le tutoriel resterait simplement
        // ferme pour toujours, sans message. Le fait que ces objets existent vraiment se
        // verifie dans un GameTest, ou les registres sont charges.
        Set<String> named = new HashSet<>();
        for (TutorialLibrary.Entry entry : TutorialLibrary.entries()) {
            named.addAll(entry.requiredItems());
        }

        assertTrue(named.contains("academy:constraint_metal"), "les minerais de l'original");
        assertTrue(named.contains("academy:dev_normal"), "le developpeur normal, nom de 1.12.2");
        assertTrue(named.contains("academy:dev_advanced"), "et le developpeur avance");
        assertTrue(named.contains("academy:terminal_installer"), "l'objet du terminal");
        assertEquals(17, named.size(), "la liste des objets ouvreurs a change");
    }

    @Test
    void lesTutorielsDeFondSontToujoursOuverts() {
        for (String id : new String[] { "welcome", "ability_basis", "misc", "develop_ability",
                "wireless_network" }) {
            TutorialLibrary.Entry entry = TutorialLibrary.byId(id);
            assertNotNull(entry, "tutoriel attendu : " + id);
            assertTrue(entry.alwaysOpen(), id + " ne depend d'aucun objet");
        }

        assertFalse(TutorialLibrary.byId("ores").alwaysOpen(), "les minerais s'ouvrent par objet");
    }
}
