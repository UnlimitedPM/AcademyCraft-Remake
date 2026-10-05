package cn.academy.ability.client.vm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les bouffees de fumee du choc au sol.
 *
 * <p>Ce qui se relit sans Minecraft : sa vie, qui se lit sur un temps etire par un modificateur, et
 * l'atlas de quatre images. Une bouffee qui dure le mauvais temps, ou qui prend la mauvaise image,
 * ne se verrait qu'a l'ecran.
 */
class SmokesTest {

    @Test
    void uneBouffeeMonteSeTientEtSEfface() {
        // Un modificateur de 0,5 : sa montee dure donc 0,3 * 0,5 = 0,15 seconde, soit trois ticks,
        // sa tenue jusqu'a 0,75 seconde, et son effacement jusqu'a une seconde.
        Smokes.Puff puff = new Smokes.Puff(new double[] { 0, 0, 0 }, new double[] { 0, 0, 0 }, 0, 0.5);

        assertEquals(0f, puff.alpha(0f), 1e-6f, "elle nait transparente");
        for (int i = 0; i < 3; i++) {
            puff.advance();
        }
        assertEquals(1f, puff.alpha(0f), 1e-6f, "elle est pleine au bout de sa montee");
        for (int i = 0; i < 12; i++) {
            puff.advance();
        }
        assertEquals(1f, puff.alpha(0f), 1e-6f, "et elle se tient");
        for (int i = 0; i < 5; i++) {
            puff.advance();
        }
        assertEquals(0f, puff.alpha(0f), 1e-6f, "puis elle s'efface jusqu'a disparaitre");
        assertFalse(puff.dead(), "mais la bouffee vit encore");

        for (int i = 0; i < 60; i++) {
            puff.advance();
        }
        assertTrue(puff.dead(), "et s'en va a quatre secondes");
    }

    @Test
    void unePlusGrandeBouffeeDurePlusLongtemps() {
        // C'est tout l'interet du modificateur : les deux bouffees disparaissent au meme moment,
        // mais celle qui a le plus grand modificateur s'efface plus tard.
        Smokes.Puff petite = new Smokes.Puff(new double[] { 0, 0, 0 }, new double[] { 0, 0, 0 }, 0, 0.5);
        Smokes.Puff grande = new Smokes.Puff(new double[] { 0, 0, 0 }, new double[] { 0, 0, 0 }, 0, 0.7);
        for (int i = 0; i < 20; i++) {
            petite.advance();
            grande.advance();
        }

        assertEquals(0f, petite.alpha(0f), 1e-6f, "la petite a fini de s'effacer");
        assertTrue(grande.alpha(0f) > 0f, "et la grande se voit encore");
    }

    @Test
    void lesQuatreImagesPrennentLesQuatreCoinsDeLAtlas() {
        assertEquals(0f, Smokes.frameU(0), 1e-6f);
        assertEquals(0f, Smokes.frameV(0), 1e-6f);
        assertEquals(0.5f, Smokes.frameU(1), 1e-6f);
        assertEquals(0f, Smokes.frameV(1), 1e-6f);
        assertEquals(0f, Smokes.frameU(2), 1e-6f);
        assertEquals(0.5f, Smokes.frameV(2), 1e-6f);
        assertEquals(0.5f, Smokes.frameU(3), 1e-6f);
        assertEquals(0.5f, Smokes.frameV(3), 1e-6f);
    }
}
