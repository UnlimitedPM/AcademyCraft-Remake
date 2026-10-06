package cn.academy.ability.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L'onde du renfort : ses sept hauteurs, leurs delais, et ce qu'elle seme.
 *
 * <p>Ce sont les nombres d'{@code EntityIntensifyEffect} de l'original, et rien d'autre. Les
 * figer ici evite qu'un reglage ne les fasse deriver sans qu'on s'en apercoive — c'est deja ce
 * qui est arrive au compte des arcs, que le port avait porte a trois ou quatre.
 */
class BodyIntensifyEffectTest {

    @Test
    void leNoirDuVoileEstCeluiDeLoriginal() {
        // Il a ete retire une fois, puis redemande : ce test est ce qui l'empeche de disparaitre
        // une troisieme fois. C'est l'original qui tranche — son CurrentChargingHUD posait un noir
        // a dix pour cent SOUS son image bleue, multiplie par l'opacite du voile.
        assertEquals(0.1f, BodyIntensifyEffect.DIM, 1e-6, "dix pour cent, comme chez lui");

        assertEquals(0, BodyIntensifyEffect.dimColor(0f), "rien du tout quand le voile est eteint");
        assertEquals(0x19000000, BodyIntensifyEffect.dimColor(1f),
                "25/255 a pleine opacite, soit un dixieme de noir");

        // Et il suit le voile : c'est la meme courbe d'entree et de sortie.
        assertTrue(BodyIntensifyEffect.dimColor(0.5f) < BodyIntensifyEffect.dimColor(1f),
                "plus le voile monte, plus l'ecran s'assombrit");
        assertTrue(BodyIntensifyEffect.dimColor(0.5f) > BodyIntensifyEffect.dimColor(0f),
                "et il n'y a rien tant qu'il n'a pas commence");

        // Bornee : une opacite hors de 0..1 ne fait ni plus noir, ni une couleur claire.
        assertEquals(BodyIntensifyEffect.dimColor(1f), BodyIntensifyEffect.dimColor(9f));
        assertEquals(0, BodyIntensifyEffect.dimColor(-1f));
    }

    @Test
    void lOndeDescendDesSeptHauteursDeLoriginal() {
        assertEquals(7, BodyIntensifyEffect.HEIGHTS.length);
        assertEquals(7, BodyIntensifyEffect.DELAYS.length);

        assertEquals(2.0, BodyIntensifyEffect.HEIGHTS[0], 1e-6,
                "le haut de l'onde passe au-dessus de la tete");
        assertEquals(-0.1, BodyIntensifyEffect.HEIGHTS[6], 1e-6,
                "et le bas passe sous les pieds, comme chez l'original");

        for (int i = 1; i < BodyIntensifyEffect.HEIGHTS.length; i++) {
            assertTrue(BodyIntensifyEffect.HEIGHTS[i] < BodyIntensifyEffect.HEIGHTS[i - 1],
                    "les hauteurs descendent");
            assertTrue(BodyIntensifyEffect.DELAYS[i] > BodyIntensifyEffect.DELAYS[i - 1],
                    "et leurs ticks montent : l'onde descend au lieu de s'allumer d'un coup");
        }

        // L'entite de l'original finissait son onde en quinze ticks, soit trois quarts de
        // seconde : le dernier anneau se pose au huitieme, et les arcs qui restent s'eteignent.
        assertEquals(15, BodyIntensifyEffect.LIFE_TICKS);
    }

    @Test
    void chaqueAnneauSeContenteDeDeuxArcs() {
        // L'original en semait trois, et son RandUtils.rangei(3, 4) exclut le 4 : toujours trois.
        // Mais ses arcs NAISSENT invisibles et ne se montrent qu'un tick sur trois, alors que
        // ceux du port naissent visibles et se montrent deux ticks sur trois : trois par anneau
        // se liraient donc comme six ici. Deux rendent le grain de l'original — et cela vaut
        // aussi pour les anneaux des pieds, ceux qui plongeaient dans le sol.
        assertEquals(2, BodyIntensifyEffect.ARCS_PER_HEIGHT);
        assertEquals(0.5, BodyIntensifyEffect.RING_MIN, 1e-6,
                "l'anneau part juste au-dela du rayon du corps, qui fait 0,3");
        assertEquals(0.6, BodyIntensifyEffect.RING_MAX, 1e-6);
    }
}
