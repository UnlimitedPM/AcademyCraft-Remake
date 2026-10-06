package cn.academy.ability.client.vm;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le corps de plasma du canon.
 *
 * <p>Ce qui se relit sans Minecraft : les deux familles de boules et leurs bornes, le cercle que
 * chacune decrit, les trois temps de son opacite et les deux bouts de sa couleur. Une boule qui
 * tourne carre, qui nait a la mauvaise taille ou qui prend le rose au coeur ne se verraient qu'a
 * l'ecran — et c'est justement ce qu'aucune des quatre portes ne regarde.
 */
class PlasmaBodyVisualsTest {

    @Test
    void lEssaimPorteLesDeuxFamillesDeLOriginal() {
        List<PlasmaBodyVisuals.Ball> balls =
                PlasmaBodyVisuals.roll(RandomSource.create(20261006L));

        // Quatre grosses, puis quatre ou cinq petites : la borne haute de l'original est exclue.
        assertTrue(balls.size() >= 8 && balls.size() <= 9,
                "quatre grosses et quatre ou cinq petites, trouve " + balls.size());

        for (int i = 0; i < PlasmaBodyVisuals.BIG_COUNT; i++) {
            var ball = balls.get(i);
            assertTrue(ball.size() >= PlasmaBodyVisuals.BIG_SIZE_MIN
                            && ball.size() <= PlasmaBodyVisuals.BIG_SIZE_MAX,
                    "une grosse mesure de 1 a 1,5 : " + ball.size());
            assertTrue(within(ball.cx()) && within(ball.cy()) && within(ball.cz()),
                    "et son centre tient dans le cube du corps");
        }
        for (int i = PlasmaBodyVisuals.BIG_COUNT; i < balls.size(); i++) {
            var ball = balls.get(i);
            assertTrue(ball.size() >= PlasmaBodyVisuals.SMALL_SIZE_MIN
                            && ball.size() <= PlasmaBodyVisuals.SMALL_SIZE_MAX,
                    "une petite mesure de 0,1 a 0,3 : " + ball.size());
            assertTrue(Math.abs(ball.cx()) <= PlasmaBodyVisuals.SMALL_OFFSET
                            && Math.abs(ball.cy()) <= PlasmaBodyVisuals.SMALL_OFFSET
                            && Math.abs(ball.cz()) <= PlasmaBodyVisuals.SMALL_OFFSET,
                    "et son centre tient dans le grand cube");
            // Le balancement des petites vaut deux fois et demie le leur : c'est ce qui les fait
            // tourner LOIN du corps malgre leur taille minuscule.
            assertTrue(ball.horizontal().amplitude() >= PlasmaBodyVisuals.AMPLITUDE_MIN * 2.5
                            && ball.horizontal().amplitude() <= PlasmaBodyVisuals.AMPLITUDE_MAX * 2.5,
                    "elles balancent de 3,5 a 5 blocs : " + ball.horizontal().amplitude());
        }
    }

    @Test
    void chaqueBouleDecritUnCercleEtNeDepassePasSonAutreAmplitude() {
        for (var ball : PlasmaBodyVisuals.roll(RandomSource.create(7L))) {
            double horizontal = ball.horizontal().amplitude();
            double vertical = ball.vertical().amplitude();

            for (double seconds = 0; seconds < 6; seconds += 0.13) {
                Vec3 at = PlasmaBodyVisuals.offset(ball, seconds);
                double radius = Math.hypot(at.x - ball.cx(), at.z - ball.cz());
                assertEquals(horizontal, radius, 1e-9,
                        "x et z tournent sur un meme cercle : c'est la meme phase");
                assertTrue(Math.abs(at.y - ball.cy()) <= vertical + 1e-9,
                        "et la hauteur tient dans sa propre amplitude");
            }
        }
    }

    @Test
    void lOpaciteMonteEnTroisDixiemesParSecondeEtRetombeEnUne() {
        // En secondes, comme l'original : rien a la naissance, puis trois dixiemes par seconde.
        assertEquals(0f, PlasmaBodyVisuals.alpha(0), 1e-6f, "transparent a la naissance");
        assertEquals(0.3f, PlasmaBodyVisuals.alpha(1), 1e-6f, "un dixieme de plus par dixieme");
        assertEquals(0.9f, PlasmaBodyVisuals.alpha(3), 1e-6f, "et encore a trois secondes");
        assertEquals(1f, PlasmaBodyVisuals.alpha(4), 1e-6f, "pleine a quatre");
        assertEquals(1f, PlasmaBodyVisuals.alpha(90), 1e-6f, "et elle ne monte plus");

        // La chute, elle, prend une seconde, d'ou qu'elle parte.
        assertEquals(1f, PlasmaBodyVisuals.fading(1f, 0), 1e-6f, "elle part de sa valeur");
        assertEquals(0.5f, PlasmaBodyVisuals.fading(1f, 0.5), 1e-6f, "a moitie en une demi-seconde");
        assertEquals(0f, PlasmaBodyVisuals.fading(1f, 1), 1e-6f, "et il n'en reste rien");
        assertEquals(0f, PlasmaBodyVisuals.fading(0.4f, 3), 1e-6f, "sans jamais passer sous zero");
    }

    @Test
    void laPeripheriePrendLeRoseEtLeCoeurLeBleu() {
        // Les deux bouts du nuanceur, tels quels : rose (0,98 / 0,51 / 0,92) et bleu (0,43 / 0,74 / 1).
        float[] pink = PlasmaBodyVisuals.color(PlasmaBodyVisuals.SMALL_SIZE_MIN);
        assertEquals(0.98f, pink[0], 1e-6f, "la plus petite boule est rose, en rouge");
        assertEquals(0.51f, pink[1], 1e-6f, "en vert");
        assertEquals(0.92f, pink[2], 1e-6f, "en bleu");

        float[] blue = PlasmaBodyVisuals.color(PlasmaBodyVisuals.BIG_SIZE_MAX);
        assertEquals(0.43f, blue[0], 1e-6f, "la plus grosse est bleue, en rouge");
        assertEquals(0.74f, blue[1], 1e-6f, "en vert");
        assertEquals(1.0f, blue[2], 1e-6f, "en bleu");

        // Et entre les deux, elle bleuit a mesure qu'elle grossit.
        assertTrue(PlasmaBodyVisuals.depth(0.75) > PlasmaBodyVisuals.depth(0.2),
                "plus une boule est grosse, plus elle prend le bleu");
    }

    /** Vrai si un centre tient dans le cube de plus ou moins 1,5 bloc des grosses. */
    private static boolean within(double value) {
        return Math.abs(value) <= PlasmaBodyVisuals.BIG_OFFSET;
    }
}
