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
    void lEssaimDeLaMachineTientDansLeBloc() {
        // Les gabarits de l'original sont tailles pour un corps : 1,5 a 2 blocs. Autour d'une
        // machine d'un bloc, le joueur les a trouves trop grands et trop sortants, meme une fois
        // raccourcis : ce sont donc les arcs les plus courts ET le dessin le plus fin.
        assertEquals(8, ChargingEffect.MACHINE_SWARM.count(),
                "le joueur n'en voyait pas assez a quatre");
        assertEquals(0.2, ChargingEffect.MACHINE_SWARM.minLength(), 1e-6);
        assertEquals(0.4, ChargingEffect.MACHINE_SWARM.maxLength(), 1e-6);
        assertEquals(ArcPattern.SURROUND_MICRO, ChargingEffect.MACHINE_SWARM.pattern(),
                "le dessin le plus fin du port, la moitie de l'entourage fin de l'original");
        assertEquals(0.1, ArcPattern.SURROUND_MICRO.width(), 1e-6,
                "soit la moitie de son entourage fin, qui fait 0,2");
        // Le moteur genere le motif a sa longueur puis le recadre : un motif court avec les
        // depassements d'un grand motif ferait un gribouillis de la taille des depassements.
        // Les deux vont donc ensemble, dans la proportion de l'entourage fin de l'original.
        assertEquals(0.4, ArcPattern.SURROUND_MICRO.length(), 1e-6);
        assertEquals(0.8 * 0.4 / 1.75, ArcPattern.SURROUND_MICRO.maxOffset(), 0.01,
                "le depassement suit la longueur du motif");
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