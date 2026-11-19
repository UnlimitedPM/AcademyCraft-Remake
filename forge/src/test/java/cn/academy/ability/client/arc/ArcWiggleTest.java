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
        ArcWiggle wiggle = new ArcWiggle(ArcPattern.WEAK, 0);
        assertTrue(wiggle.visible(), "il est visible a sa naissance");

        wiggle.advance(always(0.0));
        assertFalse(wiggle.visible(), "0 est sous les deux seuils : il disparait");

        wiggle.advance(always(0.0));
        assertTrue(wiggle.visible(), "et il revient au tick suivant");
    }

    @Test
    @DisplayName("avec un hasard toujours defavorable, rien ne bouge")
    void rienNeBouge() {
        ArcWiggle wiggle = new ArcWiggle(ArcPattern.WEAK, 3);

        for (int i = 0; i < 100; i++) {
            wiggle.advance(always(0.99));
        }

        assertTrue(wiggle.visible(), "0,99 passe au-dessus des trois seuils");
        assertEquals(3, wiggle.variant(), "et la variante ne change jamais");
    }

    @Test
    @DisplayName("les trois nombres sont ceux de l'original, et par tick")
    void lesTroisNombresSontCeuxDeLOriginal() {
        // L'original les reglait dans chaque competence : genese d'arc .7/.1/.4, charge en
        // cours .8/.2/.8, manipulation magnetique 1/.1/.6, et les motifs de l'eclair etaient
        // laisses aux valeurs par defaut d'EntityArc (.5/.2/.2). Ce sont des probabilites PAR
        // TICK : lues comme des probabilites par seconde, elles figeraient l'eclair.
        assertEquals(0.7, ArcPattern.WEAK.texWiggle(), 1e-9);
        assertEquals(0.1, ArcPattern.WEAK.showWiggle(), 1e-9);
        assertEquals(0.4, ArcPattern.WEAK.hideWiggle(), 1e-9);

        assertEquals(0.8, ArcPattern.CHARGING.texWiggle(), 1e-9);
        assertEquals(0.2, ArcPattern.CHARGING.showWiggle(), 1e-9);
        assertEquals(0.8, ArcPattern.CHARGING.hideWiggle(), 1e-9);

        assertEquals(1.0, ArcPattern.THIN_CONTINUOUS.texWiggle(), 1e-9);
        assertEquals(0.1, ArcPattern.THIN_CONTINUOUS.showWiggle(), 1e-9);
        assertEquals(0.6, ArcPattern.THIN_CONTINUOUS.hideWiggle(), 1e-9);

        assertEquals(0.5, ArcPattern.STRONG.texWiggle(), 1e-9);
        assertEquals(0.2, ArcPattern.STRONG.showWiggle(), 1e-9);
        assertEquals(0.2, ArcPattern.STRONG.hideWiggle(), 1e-9);

        // Ce que ces nombres decident, une fois poses : la part du temps ou l'arc est visible,
        // qui vaut hide / (show + hide), et la vitesse du clignotement, qui vaut 1 / show
        // ticks allume et 1 / hide ticks eteint. L'arc faible et celui de la charge restent
        // donc tous deux allumes huit ticks sur dix — mais la charge clignote deux fois plus
        // vite, et c'est ce qui donne a chacune son allure.
        double faible = ArcPattern.WEAK.hideWiggle()
                / (ArcPattern.WEAK.showWiggle() + ArcPattern.WEAK.hideWiggle());
        double charge = ArcPattern.CHARGING.hideWiggle()
                / (ArcPattern.CHARGING.showWiggle() + ArcPattern.CHARGING.hideWiggle());
        assertEquals(0.8, faible, 1e-9, "la genese d'arc est visible huit ticks sur dix");
        assertEquals(0.8, charge, 1e-9, "la charge en cours aussi");
        assertEquals(10.0, 1 / ArcPattern.WEAK.showWiggle(), 1e-9, "mais reste allumee dix ticks");
        assertEquals(5.0, 1 / ArcPattern.CHARGING.showWiggle(), 1e-9, "quand la charge n'en tient que cinq");
    }

    @Test
    @DisplayName("une variante hors bornes rentre dans les bornes")
    void uneVarianteHorsBornesRentre() {
        assertEquals(0, new ArcWiggle(ArcPattern.WEAK, ArcPatterns.variants()).variant());
        assertEquals(ArcPatterns.variants() - 1, new ArcWiggle(ArcPattern.WEAK, -1).variant());
    }
}
