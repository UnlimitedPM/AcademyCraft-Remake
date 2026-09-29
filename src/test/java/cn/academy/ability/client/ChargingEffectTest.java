package cn.academy.ability.client;

import cn.academy.ability.client.arc.ArcPattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void lEssaimDeLaMachineTientDansLeBloc() {
        // Les gabarits de l'original sont tailles pour un corps : 1,5 a 2 blocs. Autour d'une
        // machine d'un bloc, le joueur les a trouves trop grands et trop sortants, meme une fois
        // raccourcis : ce sont donc les arcs les plus courts ET le dessin le plus fin.
        assertEquals(4, ChargingEffect.MACHINE_SWARM.count());
        assertEquals(0.2, ChargingEffect.MACHINE_SWARM.minLength(), 1e-6);
        assertEquals(0.4, ChargingEffect.MACHINE_SWARM.maxLength(), 1e-6);
        assertEquals(ArcPattern.SURROUND_MICRO, ChargingEffect.MACHINE_SWARM.pattern(),
                "le dessin le plus fin du port, la moitie de l'entourage fin de l'original");
        assertEquals(0.1, ArcPattern.SURROUND_MICRO.width(), 1e-6,
                "soit la moitie de son entourage fin, qui fait 0,2");
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