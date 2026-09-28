package cn.academy.ability.client.arc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le scintillement d'un eclair.
 *
 * <p>Trois nombres par tick, et un hasard injecte : on peut donc faire scintiller un eclair
 * « toujours » ou « jamais », et lire ce qui se passe. Sans cela, il faudrait regarder
 * l'ecran et esperer.
 */
class ArcWiggleTest {

    /** Un hasard qui repond toujours la meme chose. */
    private static Random always(double value) {
        return new Random(0) {
            @Override
            public double nextDouble() {
                return value;
            }

            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
    }

    @Test
    @DisplayName("avec un hasard toujours favorable, l'eclair clignote a chaque tick")
    void lEclairClignote() {
        ArcWiggle wiggle = new ArcWiggle(0);
        assertTrue(wiggle.visible(), "il est visible a sa naissance");

        wiggle.advance(always(0.0));
        assertFalse(wiggle.visible(), "0 est sous les deux seuils : il disparait");

        wiggle.advance(always(0.0));
        assertTrue(wiggle.visible(), "et il revient au tick suivant");
    }

    @Test
    @DisplayName("avec un hasard toujours defavorable, rien ne bouge")
    void rienNeBouge() {
        ArcWiggle wiggle = new ArcWiggle(3);

        for (int i = 0; i < 100; i++) {
            wiggle.advance(always(0.99));
        }

        assertTrue(wiggle.visible(), "0,99 passe au-dessus des trois seuils");
        assertEquals(3, wiggle.variant(), "et la variante ne change jamais");
    }

    @Test
    @DisplayName("les trois nombres sont ceux de l'original, et par tick")
    void lesTroisNombresSontCeuxDeLOriginal() {
        // EntityArc : showWiggle .2, hideWiggle .2, texWiggle .5. Ce sont des probabilites
        // PAR TICK : lues comme des probabilites par seconde, elles figeraient l'eclair.
        assertEquals(0.5, ArcWiggle.TEX_WIGGLE, 1e-9);
        assertEquals(0.2, ArcWiggle.SHOW_WIGGLE, 1e-9);
        assertEquals(0.2, ArcWiggle.HIDE_WIGGLE, 1e-9);
    }

    @Test
    @DisplayName("une variante hors bornes rentre dans les bornes")
    void uneVarianteHorsBornesRentre() {
        assertEquals(0, new ArcWiggle(ArcPatterns.variants()).variant());
        assertEquals(ArcPatterns.variants() - 1, new ArcWiggle(-1).variant());
    }
}
