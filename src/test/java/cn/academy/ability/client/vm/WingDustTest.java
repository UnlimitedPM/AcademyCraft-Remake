package cn.academy.ability.client.vm;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La poussiere des ailes de tempete.
 *
 * <p>Ce qui se relit sans Minecraft : ou les grains naissent, et de quel cote ils partent. C'est
 * tout le mouvement — un grain qui ne tourne pas autour du porteur ne fait pas un essaim, il fait
 * une pluie.
 */
class WingDustTest {

    @Test
    void unGrainNaitSurLaCoquille() {
        for (double phi = -3.0; phi < 3.0; phi += 0.5) {
            double radius = WingDust.birth(5.5, 1.2, phi).length();
            assertEquals(5.5, radius, 1e-9, "le rayon demande est celui de la naissance");
        }
    }

    @Test
    void unGrainPartEnTangentielle() {
        // A l'horizontale : la vitesse est perpendiculaire au rayon, donc le grain tourne autour.
        for (double theta = 0; theta < 6.0; theta += 0.5) {
            Vec3 at = WingDust.birth(5.0, theta, Math.PI / 2);
            Vec3 speed = WingDust.velocity(theta, 0);

            assertEquals(0.0, at.dot(speed), 1e-9, "rien ne va vers le porteur ni ne s'en va");
            assertEquals(WingDust.TANGENT, speed.horizontalDistance(), 1e-9,
                    "et il tourne a sept dixiemes de bloc par tick");
        }
    }

    @Test
    void leGrainNeTombePresquePas() {
        assertTrue(WingDust.GRAVITY < 0.1, "deux centiemes de pesanteur, contre un pour la poussiere de bloc");
        assertTrue(WingDust.TANGENT > WingDust.GRAVITY * 10,
                "et il tourne bien plus vite qu'il ne tombe");
    }

    @Test
    void unGrainApparaitPuisSEfface() {
        WingDust.Grain grain = new WingDust.Grain(new double[] { 0, 0, 0 }, new double[] { 0, 0, 0 }, 20);

        assertEquals(0f, grain.alpha(), 1e-6f, "il nait transparent");
        for (int i = 0; i < WingDust.FADE_IN_TICKS; i++) {
            grain.advance();
        }
        assertEquals(WingDust.ALPHA, grain.alpha(), 1e-6f, "il est plein au bout de l'apparition");
        assertFalse(grain.dead(), "et il vit encore");

        for (int i = 0; i < 25; i++) {
            grain.advance();
        }
        assertTrue(grain.alpha() < WingDust.ALPHA, "puis il s'efface");
        assertTrue(grain.dead(), "et s'en va pour de bon a la fin de l'effacement");
    }
}
