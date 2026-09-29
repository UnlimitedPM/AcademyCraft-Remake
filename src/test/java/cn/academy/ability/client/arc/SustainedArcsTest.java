package cn.academy.ability.client.arc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le rythme des eclairs tenus : un seul a la fois, et le premier tout de suite.
 *
 * <p>Le joueur a vu trois eclairs superposes quand le port en posait un par tick, chacun vivant
 * trois ticks. C'est ce rythme-la qui est fixe ici, et il vaut pour la charge comme pour la
 * traction magnetique.
 */
class SustainedArcsTest {

    @Test
    void unSeulArcALaFois() {
        // Le compteur du maintien part de un : le premier arc part donc tout de suite, puis plus
        // rien tant qu'il vit — dix ticks plus tard, et pas avant.
        assertTrue(SustainedArcs.due(0), "le premier arc part tout de suite");
        assertTrue(SustainedArcs.due(1), "et au premier tick du maintien");
        for (int t = 2; t < SustainedArcs.LIFE_TICKS; t++) {
            assertFalse(SustainedArcs.due(t), "un arc vit dix ticks");
        }
        assertTrue(SustainedArcs.due(10));
        assertTrue(SustainedArcs.due(20));
    }
}