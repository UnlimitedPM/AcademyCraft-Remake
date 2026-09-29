package cn.academy.ability.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le rythme de l'electricite de la charge : un seul arc a la fois, et l'essaim sur la machine.
 *
 * <p>Ce sont les deux choses que le joueur a corrigees du premier essai : trois eclairs poses
 * l'un sur l'autre au lieu d'un, et un entourage autour de lui au lieu de la machine.
 */
class ChargingEffectTest {

    @Test
    void unSeulArcALaFois() {
        // Le compteur du maintien part de un : le premier arc part donc tout de suite, puis plus
        // rien tant qu'il vit — dix ticks plus tard, et pas avant.
        assertTrue(ChargingEffect.arcDue(0), "le premier arc part tout de suite");
        assertTrue(ChargingEffect.arcDue(1), "et au premier tick du maintien");
        for (int t = 2; t < 10; t++) {
            assertFalse(ChargingEffect.arcDue(t), "un arc vit dix ticks");
        }
        assertTrue(ChargingEffect.arcDue(10));
        assertTrue(ChargingEffect.arcDue(20));
    }

    @Test
    void lEssaimSeResemeQuandSesArcsSEteignent() {
        // Trois ticks, comme la vie d'un arc d'entourage : l'essaim ne se chevauche jamais.
        assertTrue(ChargingEffect.swarmDue(0));
        assertTrue(ChargingEffect.swarmDue(1));
        assertFalse(ChargingEffect.swarmDue(2));
        assertTrue(ChargingEffect.swarmDue(3));
        assertTrue(ChargingEffect.swarmDue(6));
    }
}