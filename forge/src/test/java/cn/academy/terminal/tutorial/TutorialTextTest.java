package cn.academy.terminal.tutorial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La lecture d'un fichier de tutoriel.
 *
 * Le format vient de l'original : trois balises separent le titre, le resume et le
 * contenu, et une quatrieme est remplacee par le nom du joueur. C'est peu de regles,
 * mais elles decident de tout ce qui s'affiche.
 */
class TutorialTextTest {

    private static final String SAMPLE = """
            ![title]
            The Wind Generator

            ![brief]
            Blows in the wind.

            ![content]
            Place the fan, then the pillar.

            Thanks, ![misakaname].
            """;

    @Test
    void lesTroisMorceauxSeSeparent() {
        TutorialText text = TutorialText.parse(SAMPLE);

        assertEquals("The Wind Generator", text.title());
        assertEquals("Blows in the wind.", text.brief());
        assertTrue(text.content().startsWith("Place the fan"), "le contenu suit la balise");
        assertTrue(text.content().endsWith("Thanks, ![misakaname]."), "et va jusqu'a la fin");
    }

    @Test
    void leNomDuJoueurRemplaceSaBalise() {
        TutorialText text = TutorialText.parse(SAMPLE);

        assertTrue(text.contentFor("Paul").endsWith("Thanks, Paul."),
                "la balise du nom ne doit pas s'afficher telle quelle");
        assertEquals(1, text.content().split("!\\[misakaname\\]", -1).length - 1,
                "une seule balise dans l'echantillon");
    }

    @Test
    void unFichierSansBaliseNeMontrePasSesBalises() {
        TutorialText text = TutorialText.parse("just some text");

        // L'original affichait UNKNOWN a la place : le port rend un texte vide, et
        // c'est l'ecran qui le dira — le message est ainsi traduisible.
        assertTrue(text.isEmpty(), "un texte sans balise n'est pas un tutoriel");
        assertEquals("", text.title());
    }

    @Test
    void unTitreSansContenuResteLisible() {
        TutorialText text = TutorialText.parse("![title]\nOnly a title\n");

        assertEquals("Only a title", text.title());
        assertEquals("", text.content());
        assertEquals("", text.brief());
        assertFalse(text.isEmpty(), "un titre suffit a faire un tutoriel");
    }

    @Test
    void unTitreAbsentNePrendPasLeContenu() {
        TutorialText text = TutorialText.parse("![content]\nTout le texte\n");

        // Une balise absente rend une chaine vide : le contenu ne doit pas se retrouver
        // a la place du titre.
        assertEquals("", text.title());
        assertEquals("Tout le texte", text.content());
    }
}
