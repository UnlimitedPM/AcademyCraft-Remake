package cn.academy.ability.client.md;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La fumee du plasma : ses nombres, et le corps autour duquel elle tourne.
 *
 * <p>C'est tout ce que le passif de radiation <b>montre</b> — sa mecanique, elle, est ailleurs,
 * dans {@code RadiationIntensifyTest} — et ces nombres sont ceux de l'original. Ils se figent
 * ici pour qu'un reglage de dessin ne les fasse pas deriver sans qu'on s'en apercoive.
 */
class RadiationMarksEffectTest {

    @Test
    void uneCibleLargeFumePlusLarge() {
        // L'anneau n'est pas un rayon fixe : il suit la largeur du corps, de 0,6 a 0,7 fois.
        // C'est ce qui fait qu'un zombie et un golem ne fument pas de la meme taille.
        Vec3 centre = new Vec3(10, 64, -3);
        Random random = new Random(7L);

        for (int i = 0; i < 300; i++) {
            Vec3 pos = RadiationMarksEffect.ring(centre, 0.6, 1.95, random);
            double radius = Math.sqrt(sqr(pos.x - centre.x) + sqr(pos.z - centre.z));

            assertTrue(radius >= RadiationMarksEffect.RING_MIN * 0.6 - 1e-9
                            && radius <= RadiationMarksEffect.RING_MAX * 0.6 + 1e-9,
                    "l'etincelle se pose sur l'anneau du corps : " + radius);
            assertTrue(pos.y >= centre.y - 1e-9 && pos.y <= centre.y + 1.95 + 1e-9,
                    "et entre les pieds et le sommet de la cible : " + pos.y);
        }
    }

    @Test
    void lEtincelleDeriveDeDeuxCentimetres() {
        Random random = new Random(11L);
        for (int i = 0; i < 200; i++) {
            Vec3 vel = RadiationMarksEffect.velocity(random);
            assertTrue(Math.abs(vel.x) <= RadiationMarksEffect.SPEED + 1e-9);
            assertTrue(Math.abs(vel.y) <= RadiationMarksEffect.SPEED + 1e-9);
            assertTrue(Math.abs(vel.z) <= RadiationMarksEffect.SPEED + 1e-9);
        }
        // Les deux sens sortent : une derive qui irait toujours du meme cote serait un vent.
        assertTrue(RadiationMarksEffect.velocity(new Random(1L)).x != 0);
    }

    @Test
    void unTickSurTroisNeMontreRien() {
        // Le RandUtils.rangei(0, 3) de l'original : trois valeurs, et pas quatre — c'est un
        // tirage a borne haute exclue, que le port avait deja paye sur le renfort du corps.
        Random random = new Random(3L);
        boolean[] vus = new boolean[RadiationMarksEffect.SPARKS_BOUND];

        for (int i = 0; i < 300; i++) {
            int count = RadiationMarksEffect.sparksPerTick(random);
            assertTrue(count >= 0 && count < RadiationMarksEffect.SPARKS_BOUND);
            vus[count] = true;
        }

        for (int i = 0; i < vus.length; i++) {
            assertTrue(vus[i], "la valeur " + i + " doit sortir");
        }
        assertEquals(3, RadiationMarksEffect.SPARKS_BOUND);
    }

    @Test
    void lEtincelleEstCelleDuPlasma() {
        // C'est la meme particule que celle des rayons : elle vit 45 a 74 ticks, se dessine sur
        // 5 a 7 centimetres et n'a ni gravite ni trainee.
        assertEquals(0.05, MdSparks.SIZE_MIN, 1e-9);
        assertEquals(0.07, MdSparks.SIZE_MAX, 1e-9);
        assertEquals(25, MdSparks.LIFE_MIN_TICKS);
        assertEquals(20, MdSparks.FADE_TICKS, "elle s'efface en vingt ticks apres sa vie");
    }

    private static double sqr(double value) {
        return value * value;
    }
}
