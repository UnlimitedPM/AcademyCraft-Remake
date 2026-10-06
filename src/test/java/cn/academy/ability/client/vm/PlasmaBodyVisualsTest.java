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
    void uneBouleGrossitEnNaitreEtRetrecitEnMourir() {
        // La densite du nuanceur valait alpha * taille / distance carre : une boule se voit donc
        // jusqu'a racine de alpha. Rien a la naissance, pleine taille une fois le corps noue — et
        // c'est ce qui manquait : les boules apparaissaient a taille pleine, ce qui se lisait comme
        // un allumage et non comme de la matiere qui se noue.
        assertEquals(0.0, PlasmaBodyVisuals.growth(0f), 1e-9, "rien tant qu'il n'y a rien");
        assertEquals(0.5, PlasmaBodyVisuals.growth(0.25f), 1e-9, "un quart d'opacite, moitie de rayon");
        assertEquals(1.0, PlasmaBodyVisuals.growth(1f), 1e-9, "et pleine une fois nouee");
        assertEquals(1.0, PlasmaBodyVisuals.growth(4f), 1e-9, "jamais plus que sa taille");
        assertEquals(0.0, PlasmaBodyVisuals.growth(-2f), 1e-9, "et jamais negative");
    }

    @Test
    void unePetiteBouleSeVoitSansEtreInvisibleNiEnorme() {
        // Le rayon suit la RACINE de la taille, pas la taille : un facteur direct rendrait les
        // petites boules invisibles (deux dixiemes de bloc) et les grosses enormes. L'original,
        // lui, les voyait toutes les deux — ses petites comme des points denses.
        double petite = PlasmaBodyVisuals.visibleRadius(PlasmaBodyVisuals.SMALL_SIZE_MIN, 1f);
        double grosse = PlasmaBodyVisuals.visibleRadius(PlasmaBodyVisuals.BIG_SIZE_MAX, 1f);

        assertTrue(petite > 0.5, "une petite boule fait plus d'un demi-bloc : " + petite);
        assertTrue(grosse < 3.0, "et une grosse en fait moins de trois : " + grosse);
        assertTrue(grosse / petite < 5, "les deux tiennent dans le meme champ : " + (grosse / petite));

        // Et a la naissance il n'y a rien : le rayon suit la croissance du corps.
        assertEquals(0.0, PlasmaBodyVisuals.visibleRadius(1.0, 0f), 1e-9, "rien a la naissance");
        assertEquals(grosse, PlasmaBodyVisuals.visibleRadius(PlasmaBodyVisuals.BIG_SIZE_MAX, 1f),
                1e-9, "et sa taille pleine une fois le corps noue");
    }

    @Test
    void laCouvertureNeLaissePasLeCorpsTerne() {
        // Le facteur du nuanceur, mot pour mot : alpha * (0,5 + 0,5 alpha). Le port multipliait par
        // la seule opacite, et le joueur a vu le resultat — « tellement transparent que c'est a
        // peine si j'arrive a le voir ».
        assertEquals(PlasmaBodyVisuals.DRAW_ALPHA * 0.5f, PlasmaBodyVisuals.coverage(0f), 1e-6f,
                "a la naissance la moitie du facteur — mais le rayon, lui, est nul");
        assertEquals(PlasmaBodyVisuals.DRAW_ALPHA * 0.75f, PlasmaBodyVisuals.coverage(0.5f), 1e-6f,
                "a mi-charge, deja les trois quarts");
        assertEquals(PlasmaBodyVisuals.DRAW_ALPHA, PlasmaBodyVisuals.coverage(1f), 1e-6f,
                "et le plein une fois le corps noue");

        assertEquals(PlasmaBodyVisuals.DRAW_ALPHA, PlasmaBodyVisuals.coverage(4f), 1e-6f, "bornee");
        assertEquals(PlasmaBodyVisuals.DRAW_ALPHA * 0.5f, PlasmaBodyVisuals.coverage(-1f), 1e-6f,
                "et jamais sous la moitie");
    }

    @Test
    void lesBoulesSontToutesRoseEtLeBleuEstAuCentre() {
        // Le joueur l'a dit devant une capture du vrai mod : AUCUNE boule n'y est bleue. La ou le
        // nuanceur bleuissait, c'est le centre du VOLUME — la ou les boules s'entassent — et le port
        // les peignait une a une : il en sortait quatre grosses boules bleues qui n'existent pas.
        float[] ball = PlasmaBodyVisuals.ballColor();
        assertEquals(0.98f, ball[0], 1e-6f, "une boule prend le rose des bords, en rouge");
        assertEquals(0.51f, ball[1], 1e-6f, "en vert");
        assertEquals(0.92f, ball[2], 1e-6f, "en bleu");

        float[] core = PlasmaBodyVisuals.coreColor();
        assertEquals(0.43f, core[0], 1e-6f, "et le coeur prend le bleu, en rouge");
        assertEquals(0.74f, core[1], 1e-6f, "en vert");
        assertEquals(1.0f, core[2], 1e-6f, "en bleu");

        // Le coeur grandit avec le corps, comme les boules.
        assertEquals(PlasmaBodyVisuals.CORE_RADIUS, PlasmaBodyVisuals.coreRadius(1f), 1e-9,
                "sa taille pleine une fois le corps noue");
        assertEquals(0.0, PlasmaBodyVisuals.coreRadius(0f), 1e-9, "et rien a la naissance");
        assertTrue(PlasmaBodyVisuals.coreRadius(0.8f) > PlasmaBodyVisuals.coreRadius(0.2f),
                "il grandit a mesure que la matiere se noue");
    }

    /** Vrai si un centre tient dans le cube de plus ou moins 1,5 bloc des grosses. */
    private static boolean within(double value) {
        return Math.abs(value) <= PlasmaBodyVisuals.BIG_OFFSET;
    }
}
