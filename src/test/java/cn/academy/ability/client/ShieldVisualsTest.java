package cn.academy.ability.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les nombres du bouclier de lumiere.
 *
 * Portage de {@code RenderMdShield} : un disque qui grossit pendant quinze ticks,
 * apparait en six, et tourne de plus en plus vite pendant trente. Ce sont des courbes
 * comme les autres, elles se figent ici plutot qu'a l'oeil.
 */
class ShieldVisualsTest {

    @Test
    void leBouclierGrossitEnQuinzeTicks() {
        assertEquals(ShieldVisuals.SIZE * 0.2f, ShieldVisuals.scale(0), 0.0001f,
                "il part petit");
        assertEquals(ShieldVisuals.SIZE, ShieldVisuals.scale(15), 0.0001f,
                "et atteint sa taille en quinze ticks");
        assertEquals(ShieldVisuals.SIZE, ShieldVisuals.scale(400), 0.0001f,
                "sans grandir davantage ensuite");
    }

    @Test
    void leBouclierApparaitEnSixTicks() {
        assertEquals(0f, ShieldVisuals.alpha(0), 0.0001f);
        assertEquals(0.5f, ShieldVisuals.alpha(3), 0.0001f, "a moitie au bout de trois");
        assertEquals(1f, ShieldVisuals.alpha(6), 0.0001f);
        assertEquals(1f, ShieldVisuals.alpha(200), 0.0001f);
    }

    @Test
    void laRotationAccelerePuisSeFixe() {
        assertEquals(0.8f, ShieldVisuals.spinSpeed(0), 0.0001f);
        assertEquals(1.4f, ShieldVisuals.spinSpeed(15), 0.0001f);
        assertEquals(2f, ShieldVisuals.spinSpeed(30), 0.0001f, "vitesse maximale a trente ticks");
        assertEquals(2f, ShieldVisuals.spinSpeed(500), 0.0001f);
    }

    @Test
    void leBouclierFlotteDevantLesYeux() {
        // L'original le posait un bloc devant le joueur, 1,1 bloc au-dessus de ses
        // pieds, avec une taille de 1,8 bloc.
        assertEquals(1.0, ShieldVisuals.DISTANCE, 0.0001);
        assertEquals(1.1, ShieldVisuals.HEIGHT, 0.0001);
        assertEquals(1.8f, ShieldVisuals.SIZE, 0.0001f);
    }

    @Test
    void leBouclierNappartientQuaSaCompetence() {
        assertTrue(ShieldVisuals.showsShield("light_shield", true));

        // Les maintiens de l'electromaster, et tous les autres : le port les affichait
        // avec le bouclier de la meltdowner, ce qui n'a aucun sens.
        assertFalse(ShieldVisuals.showsShield("charging", true));
        assertFalse(ShieldVisuals.showsShield("mag_movement", true));
        assertFalse(ShieldVisuals.showsShield("mag_manip", true));
        assertFalse(ShieldVisuals.showsShield("jet_engine", true));
        assertFalse(ShieldVisuals.showsShield(null, true));

        // Et hors maintien, il n'y a rien a dessiner, meme pour la bonne competence :
        // une charge de light_shield n'est pas un bouclier.
        assertFalse(ShieldVisuals.showsShield("light_shield", false));
    }
}
