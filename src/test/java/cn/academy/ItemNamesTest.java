package cn.academy;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les noms que le jeu ecrit sous les objets.
 *
 * <p>Le jeu affiche l'identifiant du registre — {@code academy:phase_gen} — quand on demande
 * les details techniques (F3+H), et le nom affiche vient de la langue. Les deux doivent dire
 * la meme chose que l'original : c'est son identifiant qu'il fallait retrouver, parce que
 * c'est lui que les recettes, les succes et les tutoriels nomment.
 */
class ItemNamesTest {

    private static final String LANG = "/assets/academy/lang/en_us.json";

    @Test
    @DisplayName("les identifiants de l'original sont ceux de la langue")
    void lesIdentifiantsDeLOriginalSontCeuxDeLaLangue() {
        String lang = lang();

        assertTrue(lang.contains("\"block.academy.phase_gen\""), "le generateur, en bloc");
        assertTrue(lang.contains("\"item.academy.phase_gen\""), "le generateur, en objet");
        assertTrue(lang.contains("\"block.academy.dev_advanced\""), "le developpeur avance");

        // Les noms d'avant ne servent plus a rien, et les laisser cacherait le changement.
        assertFalse(lang.contains("\"block.academy.phase_generator\""), "phase_generator est parti");
        assertFalse(lang.contains("\"item.academy.phase_generator\""), "et l'objet aussi");
        assertFalse(lang.contains("\"block.academy.developer_advanced\""),
                "developer_advanced est parti");
    }

    @Test
    @DisplayName("la bobine magnetique dit ce qu'elle fait, en deux lignes")
    void laBobineMagnetiqueDitCeQuElleFait() {
        String lang = lang();

        assertTrue(lang.contains("\"item.academy.magnetic_coil.desc\""), "elle a une description");
        // L'original la portait sur une seule entree et la coupait sur `<br>` : le port
        // separe par un retour a la ligne, donc la traduction porte bien les deux lignes.
        assertTrue(lang.contains("Overcharges the developer\\nIn order to rewrite personal reality"),
                "la description tient en deux lignes, comme l'original");
    }

    @Test
    @DisplayName("le chargeur de debogage a ete retire")
    void leChargeurDeDebogageNAPasSurvecu() {
        assertFalse(lang().contains("debug_charger"), "sa cle de langue est partie");

        // Son modele aussi. Le chemin est celui des sources, pas du build : ce qui reste
        // dans build/ apres une suppression ne dit rien de ce que le mod livre.
        Path model = Path.of("src/main/resources/assets/academy/models/item/debug_charger.json");
        Assumptions.assumeTrue(Files.isDirectory(Path.of("src/main/resources")),
                "le test se lit depuis la racine du projet");
        assertFalse(Files.exists(model), "son modele est parti");
    }

    /** Le contenu du fichier de langue anglaise. */
    private static String lang() {
        try (InputStream in = ItemNamesTest.class.getResourceAsStream(LANG)) {
            assertNotNull(in, "le fichier de langue doit etre dans les ressources");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new AssertionError("lecture impossible : " + LANG, e);
        }
    }
}
