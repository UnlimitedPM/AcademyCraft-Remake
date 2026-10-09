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
 * <b>quand</b> ils sortent se relit ici. C'est aussi la partie que le joueur reglera a l'oeil, et
 * elle porte deux regles : la zone frappee est un <b>carre</b> (une boite, pas un disque), et
 * chaque cellule de la grille en recoit un — c'est ce qui tient les 360o. C'est toute la raison
 * d'etre de l'effet, qui est de <b>montrer</b> la portee de l'attaque.
 */
class GroundArcsTest {

    @Test
    @DisplayName("chaque cellule du carre recoit son eclair : les 360o sont tenus")
    void chaqueCelluleRecoitSonEclair() {
        List<GroundArcs.Shot> shots = GroundArcs.rolls(new Random(7));

        assertEquals(GroundArcs.COUNT, shots.size(), "le compte est celui de la grille");
        int[][] perCell = new int[GroundArcs.GRID][GroundArcs.GRID];
        for (GroundArcs.Shot shot : shots) {
            assertTrue(Math.abs(shot.offsetX()) <= 1.0 && Math.abs(shot.offsetZ()) <= 1.0,
                    "aucun eclair ne tombe hors de la zone frappee, qui est un CARRE");
            perCell[cell(shot.offsetX())][cell(shot.offsetZ())] += 1;

            assertTrue(shot.length() >= SurroundArcs.BOLD.minLength()
                            && shot.length() <= SurroundArcs.BOLD.maxLength(),
                    "sa hauteur est celle du gresillement de la charge de ce meme orage");
            assertTrue(Math.abs(shot.tilt()) <= GroundArcs.TILT_MAX,
                    "et il ne penche que d'un tiers de bloc");
        }

        for (int x = 0; x < GroundArcs.GRID; x++) {
            for (int z = 0; z < GroundArcs.GRID; z++) {
                assertEquals(1, perCell[x][z],
                        "une cellule, un eclair : ni trou dans une direction, ni paquet dans une autre");
            }
        }
    }

    @Test
    @DisplayName("la vague part du centre et va vers les quatre bords")
    void laVaguePartDuCentre() {
        for (GroundArcs.Shot shot : GroundArcs.rolls(new Random(3))) {
            assertEquals((int) Math.round(GroundArcs.square(shot.offsetX(), shot.offsetZ())
                            * GroundArcs.SPREAD_TICKS), shot.delay(),
                    "le retard est la distance au centre POUR UN CARRE, ramenee aux ticks de la vague");
        }

        // Et les deux bouts du retard sont bel et bien atteints : le centre sort le premier, le
        // bord du carre au dernier. C'est ce qui fait la propagation plutot qu'un semis qui
        // s'allume d'un coup — et c'est le CARRE qui s'ouvre, les quatre bords ensemble.
        int soonest = Integer.MAX_VALUE;
        int latest = -1;
        for (int seed = 0; seed < 100; seed++) {
            for (GroundArcs.Shot shot : GroundArcs.rolls(new Random(seed))) {
                soonest = Math.min(soonest, shot.delay());
                latest = Math.max(latest, shot.delay());
            }
        }
        assertEquals(0, soonest, "le centre sort au premier tick");
        assertEquals(GroundArcs.SPREAD_TICKS, latest, "et le bord du carre au dernier");
    }

    /** La cellule d'une fraction du rayon, entre moins un et un. La meme que celle de l'effet. */
    private static int cell(double offset) {
        return Math.min(GroundArcs.GRID - 1, (int) ((offset + 1) / 2 * GroundArcs.GRID));
    }
}
