package cn.academy.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les nombres de la bille de silicium : sa rotation, ses eclats, et sa taille.
 *
 * <p>Ce sont ceux de {@code RenderSibarn} et de la poussiere d'{@code EntitySilbarn}, et aucun
 * ne se verifie sans lancer un jeu. Le plus important n'est pourtant pas de l'original : c'est
 * la <b>boite de collision</b>, que le joueur a demandee plus large que la vraie, parce que viser
 * une bille en vol au centimetre pres n'etait pas jouable.
 */
class SilbarnVisualsTest {

    private static final double EPSILON = 1e-6;

    @Test
    @DisplayName("la boite est plus large que celle de l'original, et c'est voulu")
    void laBoiteEstPlusLarge() {
        // L'original : setSize(.4f, .4f). Le port l'a doublee — c'est un ecart assume, et ce
        // test est la pour que personne ne la « corrige » vers quarante centimetres.
        assertTrue(SilbarnVisuals.HIT_SIZE > 0.4f,
                "la boite du port doit rester plus large que celle du vrai mod");
        assertEquals(0.8f, SilbarnVisuals.HIT_SIZE, 1e-4f);
    }

    @Test
    @DisplayName("elle tourne de trente degres par seconde, et jamais par a-coups")
    void elleTourneLentement() {
        // L'original : 0,03 degre par milliseconde, soit 30 par seconde, soit 1,5 par tick.
        assertEquals(1.5f, SilbarnVisuals.SPIN_PER_TICK, 1e-4f);
        assertEquals(0f, SilbarnVisuals.spinDegrees(0), 1e-4f);
        assertEquals(1.5f, SilbarnVisuals.spinDegrees(1), 1e-4f);
        assertEquals(30f, SilbarnVisuals.spinDegrees(20), 1e-4f);
        // Le tick partiel compte : sans lui, la rotation avancerait par sauts de vingt images
        // par seconde, ce qui se voit tout de suite sur une barre de cinquante centimetres.
        assertEquals(15.75f, SilbarnVisuals.spinDegrees(10.5), 1e-4f);
    }

    @Test
    @DisplayName("elle est dessinee a l'echelle de l'original")
    void lEchelleEstCelleDeLOriginal() {
        assertEquals(0.05f, SilbarnVisuals.MODEL_SCALE, 1e-4f);
    }

    @Test
    @DisplayName("les eclats sont dix-huit a vingt-sept, petits, et lents")
    void lesEclatsSontCeuxDeLOriginal() {
        assertEquals(18, SilbarnVisuals.FRAG_MIN);
        assertEquals(27, SilbarnVisuals.FRAG_MAX);
        assertEquals(0.08, SilbarnVisuals.FRAG_SPEED_MIN, EPSILON);
        assertEquals(0.18, SilbarnVisuals.FRAG_SPEED_MAX, EPSILON);
        assertEquals(0.2, SilbarnVisuals.FRAG_RISE, EPSILON);
        assertEquals(0.1f, SilbarnVisuals.FRAG_SIZE, 1e-4f);
        assertEquals(0.03, SilbarnVisuals.FRAG_GRAVITY, EPSILON);
        assertEquals(25f, SilbarnVisuals.FRAG_SPIN_PER_TICK, 1e-4f);

        // Six fois moins lourds que la bille : le nuage flotte avant de retomber.
        assertTrue(SilbarnVisuals.FRAG_GRAVITY < 0.12,
                "un eclat tombe plus lentement que la bille dont il vient");
    }

    @Test
    @DisplayName("les eclats partent dans toutes les directions, mais jamais vers le sol")
    void lesEclatsJaillissent() {
        var random = new java.util.Random(1234);
        double lowest = 1;

        for (int i = 0; i < 500; i++) {
            double[] direction = SilbarnVisuals.fragDirection(random);

            assertEquals(1.0, Math.sqrt(direction[0] * direction[0]
                    + Math.pow(direction[1] - SilbarnVisuals.FRAG_RISE, 2)
                    + direction[2] * direction[2]), 1e-9,
                    "tiree sur la sphere : le redressement est le seul ecart a la norme");
            lowest = Math.min(lowest, direction[1]);
        }

        // Le redressement de l'original : meme l'eclat le plus bas garde une chance de monter.
        // C'est une borne, pas une valeur atteinte — cinq cents tirages n'iront pas chercher le
        // pole sud de la sphere au hasard.
        assertTrue(lowest >= -1 + SilbarnVisuals.FRAG_RISE,
                "aucun eclat n'est redresse plus bas que le sol : " + lowest);
        assertTrue(lowest > -1, "aucun eclat ne part droit vers le sol");
    }
}
