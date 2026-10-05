package cn.academy.ability.client.vm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le coup de camera du choc au sol.
 *
 * <p>Seule la courbe se relit sans Minecraft — c'est elle qui donne son rythme au geste : monter
 * en quatre ticks, tenir jusqu'a vingt, retomber en cinq. Le reste touche la visee du joueur, et
 * ne se voit qu'en jeu.
 */
class GroundshockCameraTest {

    @Test
    @DisplayName("la montee se fait en quatre ticks")
    void laMonteeSeFaitEnQuatreTicks() {
        assertEquals(0.0f, GroundshockCamera.pull(0), 1e-6, "rien au premier tick");
        assertEquals(0.25f, GroundshockCamera.pull(1), 1e-6, "un quart au suivant");
        assertEquals(0.75f, GroundshockCamera.pull(3), 1e-6, "et trois quarts au dernier");
    }

    @Test
    @DisplayName("elle tient son plein jusqu'a vingt ticks")
    void elleTientJusquaVingtTicks() {
        for (int tick = GroundshockCamera.PULL_RAMP; tick <= GroundshockCamera.PULL_HOLD; tick++) {
            assertEquals(1.0f, GroundshockCamera.pull(tick), 1e-6, "pleine au tick " + tick);
        }
    }

    @Test
    @DisplayName("puis elle retombe en cinq ticks, et se tait")
    void puisElleRetombe() {
        assertEquals(0.4f, GroundshockCamera.pull(23), 1e-6, "deux cinquièmes en moins a 23");
        assertEquals(0.0f, GroundshockCamera.pull(GroundshockCamera.PULL_FADE), 1e-6,
                "et plus rien a la fin de la retombee");
        assertEquals(0.0f, GroundshockCamera.pull(60), 1e-6, "ni jamais apres");
    }

    @Test
    @DisplayName("le piquage du coup dure quatre ticks, comme l'original")
    void lePiquageDureQuatreTicks() {
        assertEquals(4, GroundshockCamera.SLASH_TICKS, "quatre ticks");
        assertTrue(GroundshockCamera.SLASH_DEGREES > 0f, "et il pique vers le bas");
        assertTrue(GroundshockCamera.PULL_DEGREES > 0f, "pendant que la charge leve le regard");
    }
}
