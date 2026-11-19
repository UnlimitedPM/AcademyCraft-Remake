package cn.academy.terminal.tutorial;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La lecture du markdown des tutoriels (voir {@link TutorialMarkdown}).
 *
 * <p>C'est la partie qui decide si une capture s'affiche : le joueur s'est plaint deux fois
 * de ne pas les voir. Un contenu livre peut donc etre relu sans demarrer le jeu.
 */
class TutorialMarkdownTest {

    private static final Function<String, String> KEYS = id -> switch (id) {
        case "ability_activation" -> "V";
        case "open_data_terminal" -> "Left Alt";
        default -> null;
    };

    @Test
    void lesImagesSontReconnuesSurLeurPropreLigne() {
        List<TutorialMarkdown.Block> blocks = TutorialMarkdown.parse("""
                Before the picture.

                ![Skill Tree](academy:textures/tutorial/skill_tree_ui.png)

                After.
                """, KEYS);

        assertEquals(5, blocks.size());
        assertEquals("Before the picture.", blocks.get(0).text());
        assertTrue(blocks.get(1).text().isEmpty());
        assertTrue(blocks.get(2).isImage());
        assertEquals("academy:textures/tutorial/skill_tree_ui.png", blocks.get(2).image());
        assertNull(blocks.get(2).text());
        assertEquals("After.", blocks.get(4).text());
    }

    @Test
    void lesTouchesPrennentLeurNomEtLesInconnuesDesPointsDInterrogation() {
        List<TutorialMarkdown.Block> blocks = TutorialMarkdown.parse(
                "Use ![key id=\"ability_activation\"] then ![key id=\"nulle_part\"].", KEYS);

        assertEquals(1, blocks.size());
        assertEquals("Use V then ???.", blocks.get(0).text());
    }

    @Test
    void lesMarquesDeGrasNeSontPasDessinees() {
        List<TutorialMarkdown.Block> blocks =
                TutorialMarkdown.parse("A __strong__ and **bold** word.", KEYS);

        assertEquals("A strong and bold word.", blocks.get(0).text());
    }

    @Test
    void lesVidesDeSuiteNeFontQuUnParagraphe() {
        List<TutorialMarkdown.Block> blocks =
                TutorialMarkdown.parse("One.\n\n\n\nTwo.\n\n", KEYS);

        assertEquals(3, blocks.size());
        assertEquals("One.", blocks.get(0).text());
        assertTrue(blocks.get(1).text().isEmpty());
        assertEquals("Two.", blocks.get(2).text());
    }

    @Test
    void unContenuVideNeRendRien() {
        assertTrue(TutorialMarkdown.parse("", KEYS).isEmpty());
        assertTrue(TutorialMarkdown.parse(null, KEYS).isEmpty());
    }

    @Test
    void lesVraisTutorielsLivresSeLisentTousSansErreur() {
        String basis = """
                The Ability Developer is powered by Imag Energy.

                ![Skill Tree](academy:textures/tutorial/skill_tree_ui.png)

                Activate the ability using __Ability Activation Key__ ![key id="ability_activation"].
                """;
        List<TutorialMarkdown.Block> blocks = TutorialMarkdown.parse(basis, KEYS);

        assertTrue(blocks.stream().anyMatch(TutorialMarkdown.Block::isImage),
                "la capture doit ressortir comme une image");
    }
}
