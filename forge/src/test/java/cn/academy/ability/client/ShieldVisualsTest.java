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

    @Test
    void lEssaimNaitTroisTicksSurDix() {
        // L'original tirait nextFloat() < 0,3 : trois ticks sur dix portent une etincelle.
        assertEquals(0.3f, ShieldVisuals.SPARK_CHANCE, 0.0001f);
        assertTrue(ShieldVisuals.sparksThisTick(0f));
        assertTrue(ShieldVisuals.sparksThisTick(0.2999f));
        assertFalse(ShieldVisuals.sparksThisTick(0.3f), "la borne est exclue");
        assertFalse(ShieldVisuals.sparksThisTick(0.9f));
    }

    @Test
    void lEssaimNaitSurLeDisque() {
        // Un bloc devant les yeux — le lookingPos(player, 1) de l'original — dans un cube de
        // 0,5 bloc de cote : c'est le disque lui-meme, qui fait 1,8 bloc.
        assertEquals(1.0, ShieldVisuals.SPARK_FORWARD, 0.0001);
        assertEquals(0.5, ShieldVisuals.SPARK_JITTER, 0.0001);

        java.util.Random random = new java.util.Random(1);
        net.minecraft.world.phys.Vec3 eyes = new net.minecraft.world.phys.Vec3(10, 65, 10);
        net.minecraft.world.phys.Vec3 look = new net.minecraft.world.phys.Vec3(0, 0, 1);
        net.minecraft.world.phys.Vec3 centre = eyes.add(look);

        for (int i = 0; i < 200; i++) {
            net.minecraft.world.phys.Vec3 born = ShieldVisuals.sparkBorn(eyes, look, random);
            assertTrue(Math.abs(born.x - centre.x) <= ShieldVisuals.SPARK_JITTER + 1e-9
                            && Math.abs(born.y - centre.y) <= ShieldVisuals.SPARK_JITTER + 1e-9
                            && Math.abs(born.z - centre.z) <= ShieldVisuals.SPARK_JITTER + 1e-9,
                    "etincelle hors du cube : " + born);
            assertTrue(born.distanceTo(centre) > 0.0, "et les trois axes sont tires separement");
        }
    }

    @Test
    void lEssaimGresille() {
        // Sa derive est la seule du meltdowner a pouvoir descendre : un centimetre en bas,
        // cinq en haut, et deux centimetres au plus de cote.
        java.util.Random random = new java.util.Random(2);
        for (int i = 0; i < 200; i++) {
            net.minecraft.world.phys.Vec3 drift = ShieldVisuals.sparkDrift(random);
            assertTrue(drift.y >= ShieldVisuals.SPARK_RISE_MIN - 1e-9
                            && drift.y <= ShieldVisuals.SPARK_RISE_MAX + 1e-9,
                    "hauteur hors bornes : " + drift.y);
            assertTrue(Math.abs(drift.x) <= ShieldVisuals.SPARK_DRIFT_XZ + 1e-9
                            && Math.abs(drift.z) <= ShieldVisuals.SPARK_DRIFT_XZ + 1e-9,
                    "derive trop large : " + drift);
        }
    }
}
