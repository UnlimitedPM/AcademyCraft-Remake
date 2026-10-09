package cn.academy.ability.client;

import cn.academy.ability.client.arc.SurroundArcs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les tirages des eclairs de sol du claquement d'orage.
 *
 * <p>C'est la seule partie de cet effet qu'une porte puisse regarder : le sol se cherche dans le
 * monde et les eclairs se dessinent a l'ecran, mais <b>ou</b> ils tombent, de quelle hauteur, et
 * <b>quand</b> ils sortent se relit ici. C'est aussi la partie que le joueur reglera a l'oeil : la
 * vague doit couvrir tout le rayon, jamais se coller au point d'impact, et partir du centre vers le
 * bord — c'est toute la raison d'etre de l'effet, qui est de <b>montrer</b> la portee de l'attaque.
 */
class GroundArcsTest {

    @Test
    @DisplayName("les eclairs tombent dans tout le rayon, jamais dessus")
    void ilsTombentDansToutLeRayon() {
        double radius = 25.0;
        List<GroundArcs.Shot> shots = GroundArcs.rolls(radius, new Random(7));

        assertEquals(GroundArcs.COUNT, shots.size(), "le compte est celui de l'effet");
        for (GroundArcs.Shot shot : shots) {
            assertTrue(shot.fraction() >= GroundArcs.REACH_MIN,
                    "aucun eclair ne sort sur le point d'impact meme");
            assertTrue(shot.fraction() <= GroundArcs.REACH_MAX,
                    "ni au-dela du rayon : c'est la portee qui se montre");
            assertTrue(shot.length() >= SurroundArcs.BOLD.minLength()
                            && shot.length() <= SurroundArcs.BOLD.maxLength(),
                    "sa hauteur est celle du gresillement de la charge de ce meme orage");
            assertTrue(Math.abs(shot.tilt()) <= GroundArcs.TILT_MAX,
                    "et il ne penche que d'un tiers de bloc");
            assertTrue(shot.heading() >= 0 && shot.heading() < Math.PI * 2, "l'azimut fait le tour");
        }
    }

    @Test
    @DisplayName("la vague part du centre et va vers le bord")
    void laVaguePartDuCentre() {
        for (GroundArcs.Shot shot : GroundArcs.rolls(25.0, new Random(3))) {
            assertEquals((int) Math.round(shot.fraction() * GroundArcs.SPREAD_TICKS), shot.delay(),
                    "le retard est la distance, ramenee aux ticks de la vague");
        }

        // Et les deux bouts du retard sont bel et bien atteints : le plus pres sort au premier
        // tick, le plus loin au dernier. C'est ce qui fait la propagation plutot qu'un semis qui
        // s'allume d'un coup.
        int soonest = Integer.MAX_VALUE;
        int latest = -1;
        for (int seed = 0; seed < 100; seed++) {
            for (GroundArcs.Shot shot : GroundArcs.rolls(25.0, new Random(seed))) {
                soonest = Math.min(soonest, shot.delay());
                latest = Math.max(latest, shot.delay());
            }
        }
        assertEquals(1, soonest, "le plus pres sort au premier tick");
        assertEquals(GroundArcs.SPREAD_TICKS, latest, "et le bord du rayon au dernier");
    }

    @Test
    @DisplayName("un rayon nul ne tire rien de travers")
    void unRayonNulNeCasseRien() {
        // Le retard se calcule sur la PART du rayon, jamais sur le rayon : un rayon nul ne peut
        // donc pas donner un retard infini ni un tirage NaN. Le rayon lui-meme est borne par
        // l'experience de la competence, voir ThunderClapSkill.range.
        for (GroundArcs.Shot shot : GroundArcs.rolls(0, new Random(1))) {
            assertTrue(Double.isFinite(shot.fraction()));
            assertTrue(shot.delay() >= 0 && shot.delay() <= GroundArcs.SPREAD_TICKS);
        }
    }
}
