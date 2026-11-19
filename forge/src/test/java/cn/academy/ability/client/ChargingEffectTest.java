package cn.academy.ability.client;

import cn.academy.ability.client.arc.ArcPattern;
import cn.academy.ability.client.arc.SurroundArcs;
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
    void lEssaimDeLaMachineEstCeluiDeLoriginal() {
        // La machine recoit l'entourage MOYEN de l'original — six arcs de 3 a 4 blocs, donc 0,9
        // a 1,2 une fois la mise a l'echelle de son dessin retrouvee. Le port les avait
        // raccourcis a 0,2 : il croyait corriger une taille, il rattrapait une echelle.
        assertEquals(ArcPattern.SURROUND_NORMAL, SurroundArcs.NORMAL.pattern());
        assertEquals(6, SurroundArcs.NORMAL.count());
        assertEquals(0.9, SurroundArcs.NORMAL.minLength(), 1e-6);
        assertEquals(1.2, SurroundArcs.NORMAL.maxLength(), 1e-6);

        // Et la longueur du motif est la vraie taille de l'arc : le moteur le genere a cette
        // longueur, puis le recadre a la portee demandee — il ne le met pas a l'echelle.
        assertEquals(1.05, ArcPattern.SURROUND_NORMAL.length(), 1e-6);
        assertEquals(0.09, ArcPattern.SURROUND_NORMAL.width(), 1e-6,
                "l'original fait 0,3 de large, dessine a 0,3");
        assertEquals(0.24, ArcPattern.SURROUND_NORMAL.maxOffset(), 1e-6,
                "et 0,8 de depassement, dessine a 0,3");
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