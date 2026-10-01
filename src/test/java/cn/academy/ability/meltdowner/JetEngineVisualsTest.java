package cn.academy.ability.meltdowner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les courbes de la marque du reacteur, et les nombres de son bouclier.
 *
 * <p>Ce sont ceux de {@code RippleMarkRender} et de {@code RenderDiamondShield}, et rien de tout
 * cela ne se verifie sans lancer un jeu : un cycle de 3,6 seconde, trois ondes qui se relaient,
 * un fondu qui monte puis qui descend. C'est le genre de nombre qu'un portage perd en silence —
 * la marque reste jolie, mais elle clignote au lieu de respirer, et personne ne sait plus
 * pourquoi. Les figer ici est la seule facon de s'en apercevoir.
 */
class JetEngineVisualsTest {

    private static final double EPSILON = 1e-6;

    @Test
    @DisplayName("le cycle d'une onde vaut 3,6 seconde, et les trois ondes se relaient par tiers")
    void lesTroisOndesSeRelayent() {
        assertEquals(3.6, JetEngineVisuals.CYCLE, EPSILON);
        assertEquals(3, JetEngineVisuals.OFFSETS.length);

        for (int i = 1; i < JetEngineVisuals.OFFSETS.length; i++) {
            assertEquals(JetEngineVisuals.CYCLE / 3,
                    JetEngineVisuals.OFFSETS[i - 1] - JetEngineVisuals.OFFSETS[i], EPSILON,
                    "les ondes se suivent a un tiers de cycle : l'onde " + i + " part quand la "
                            + "precedente a fait son tiers de chemin");
        }
    }

    @Test
    @DisplayName("l'age d'une onde est toujours ramene dans son cycle, meme negatif")
    void lAgeEstToujoursDansLeCycle() {
        // Un age negatif arrive : les ondes sont decalees, et la troisieme part avant la
        // premiere. Le reste du langage ramene deja les negatifs, mais une onde posee sur un
        // age negatif ne doit pas disparaitre pour autant.
        for (double age = -10; age < 20; age += 0.37) {
            for (int i = 0; i < JetEngineVisuals.OFFSETS.length; i++) {
                double phase = JetEngineVisuals.phase(age, i);
                assertTrue(phase >= 0 && phase < JetEngineVisuals.CYCLE,
                        "age " + age + ", onde " + i + " : phase hors cycle (" + phase + ")");
            }
        }

        // Et l'age zero pose la premiere onde au debut de son cycle, les deux autres plus loin.
        assertEquals(0.0, JetEngineVisuals.phase(0, 0), EPSILON);
        assertEquals(1.2, JetEngineVisuals.phase(0, 1), EPSILON);
        assertEquals(2.4, JetEngineVisuals.phase(0, 2), EPSILON);
    }

    @Test
    @DisplayName("l'onde part large et se resserre, de 1,9 a 1,4")
    void lOndeSeResserre() {
        assertEquals(1.9f, JetEngineVisuals.size(0), 1e-4f);
        assertEquals(1.4f, JetEngineVisuals.size(JetEngineVisuals.CYCLE), 1e-4f);
        assertTrue(JetEngineVisuals.size(1) < JetEngineVisuals.size(0),
                "elle se resserre en vieillissant, pas l'inverse");
    }

    @Test
    @DisplayName("elle monte de trente centimetres par seconde")
    void lOndeMonte() {
        assertEquals(0.0, JetEngineVisuals.height(0), EPSILON);
        assertEquals(0.3, JetEngineVisuals.height(1), EPSILON);
        assertEquals(1.08, JetEngineVisuals.height(JetEngineVisuals.CYCLE), 1e-9);
    }

    @Test
    @DisplayName("elle apparait en 1,6 seconde, se tient, puis s'efface en 1,6 seconde")
    void lOndeRespire() {
        assertEquals(0f, JetEngineVisuals.alpha(0), 1e-4f);
        assertEquals(1f, JetEngineVisuals.alpha(JetEngineVisuals.FADE_IN), 1e-4f);
        assertEquals(0.5f, JetEngineVisuals.alpha(JetEngineVisuals.FADE_IN / 2), 1e-4f);

        // Le palier : la fin du fondu d'entree jusqu'au debut du fondu de sortie.
        assertEquals(1f, JetEngineVisuals.alpha(2.0), 1e-4f);

        // Le fondu de sortie part de 2,0 seconde et rend zero au bout du cycle.
        assertEquals(0.5f, JetEngineVisuals.alpha(2.8), 1e-4f);
        assertEquals(0f, JetEngineVisuals.alpha(JetEngineVisuals.CYCLE), 1e-4f);

        // Et les deux fondus occupent tout le cycle : il ne reste qu'un dixieme de palier.
        assertEquals(JetEngineVisuals.CYCLE,
                JetEngineVisuals.FADE_IN + JetEngineVisuals.FADE_OUT + 0.4, 1e-9);
    }

    @Test
    @DisplayName("la marque ne s'affiche que pour le reacteur, et seulement tenu")
    void laMarqueEstBienLaSienne() {
        assertTrue(JetEngineVisuals.showsMark("jet_engine", true));
        assertFalse(JetEngineVisuals.showsMark("jet_engine", false),
                "relache : le vol a commence, la marque a fini son office");
        assertFalse(JetEngineVisuals.showsMark("light_shield", true),
                "une autre competence tenue n'a pas a montrer la marque du reacteur");
        assertFalse(JetEngineVisuals.showsMark(null, true));
    }

    @Test
    @DisplayName("le bouclier est un bloc devant les yeux, 1,1 au-dessus des pieds, fois 1,5")
    void leBouclierEstPlace() {
        assertEquals(1.8f, JetEngineVisuals.SHIELD_SIZE, 1e-4f);
        assertEquals(1.5f, JetEngineVisuals.SHIELD_SCALE, 1e-4f);
        assertEquals(1.0, JetEngineVisuals.SHIELD_FORWARD, EPSILON);
        assertEquals(1.1, JetEngineVisuals.SHIELD_HEIGHT, EPSILON);
    }

    @Test
    @DisplayName("la trainee seme dix etincelles par tick, serrees et presque immobiles")
    void laTraineeSeme() {
        assertEquals(10, JetEngineVisuals.TRAIL_PER_TICK);
        assertEquals(0.3, JetEngineVisuals.TRAIL_SPREAD, EPSILON);
        assertEquals(0.02, JetEngineVisuals.TRAIL_DRIFT, EPSILON);
        assertTrue(JetEngineVisuals.TRAIL_DRIFT < JetEngineVisuals.TRAIL_SPREAD,
                "la derive est bien plus petite que la ou l'etincelle nait : elle s'attarde "
                        + "autour du porteur au lieu de filer");
    }

    @Test
    @DisplayName("la couleur de la marque est le vert de l'original")
    void laCouleurEstVerte() {
        // 51 / 255 : l'original la posait en octets, et un portage en flottants se trompe
        // volontiers de facteur.
        assertEquals(0.2f, JetEngineVisuals.RED, 1e-4f);
        assertEquals(1f, JetEngineVisuals.GREEN, 1e-4f);
        assertEquals(0.2f, JetEngineVisuals.BLUE, 1e-4f);
        assertEquals(JetEngineVisuals.RED, JetEngineVisuals.BLUE, EPSILON);
    }
}
