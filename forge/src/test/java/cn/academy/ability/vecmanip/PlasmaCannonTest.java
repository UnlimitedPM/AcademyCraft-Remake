package cn.academy.ability.vecmanip;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le vol de la boule du canon a plasma, {@code PlasmaCannonSkill}.
 *
 * <p>La boule avance d'un bloc par tick en ligne droite vers le point vise, et s'arrete
 * quand il ne lui reste plus rien a faire : deux calculs qui tiennent dans une ligne chacun,
 * mais qui decident de tout — une boule qui depasse sa cible se met a osciller autour d'elle
 * sans jamais exploser.
 */
class PlasmaCannonTest {

    private static final double EPS = 1.0e-9;

    @Test
    void laBouleAvanceDUnBlocParTick() {
        // Droit devant : un bloc par tick, sans jamais depasser.
        Vec3 ahead = Vec3.ZERO;
        for (int tick = 1; tick <= 8; tick++) {
            ahead = PlasmaCannonSkill.travel(ahead, new Vec3(0, 0, 10));
            assertEquals(tick, ahead.z, EPS, "au tick " + tick);
        }
        assertFalse(PlasmaCannonSkill.arrived(ahead, new Vec3(0, 0, 10)),
                "a huit blocs, la boule vole encore");

        // Et elle arrive au neuvieme : il ne lui reste qu'un bloc a faire, moins qu'un pas.
        ahead = PlasmaCannonSkill.travel(ahead, new Vec3(0, 0, 10));
        assertEquals(9.0, ahead.z, EPS);
        assertTrue(PlasmaCannonSkill.arrived(ahead, new Vec3(0, 0, 10)),
                "a un bloc de la cible, elle explode");

        // En diagonale, le pas est unitaire : c'est la direction qui est normalisee.
        Vec3 diagonal = PlasmaCannonSkill.travel(Vec3.ZERO, new Vec3(3, 4, 0));
        assertEquals(1.0, diagonal.length(), EPS);
        assertEquals(0.6, diagonal.x, 1.0e-6);
        assertEquals(0.8, diagonal.y, 1.0e-6);

        // Et la boule tombe de quinze blocs au-dessus de la tete du joueur : vers le bas
        // aussi, elle avance d'un bloc.
        Vec3 falling = PlasmaCannonSkill.travel(new Vec3(0, 15, 0), Vec3.ZERO);
        assertEquals(14.0, falling.y, EPS);
    }

    @Test
    void laBouleNeDepasseJamaisSaCible() {
        // Le `if (rawDelta.length() < 1) return` de l'original : une cible plus proche qu'un
        // bloc laisse la boule ou elle est, au lieu de la faire passer d'un cote puis de
        // l'autre. La position rendue est la meme instance, donc rien n'a bouge du tout.
        Vec3 ball = new Vec3(4, 4, 4);
        assertSame(ball, PlasmaCannonSkill.travel(ball, new Vec3(4.5, 4, 4)),
                "a moins d'un bloc, la boule ne bouge pas");
        assertSame(ball, PlasmaCannonSkill.travel(ball, ball), "et sur place non plus");

        // L'arrivee, elle, se juge a un bloc et demi : c'est ce qui arrete la boule avant
        // qu'elle ne touche le point vise, et c'est l'original.
        assertTrue(PlasmaCannonSkill.arrived(Vec3.ZERO, new Vec3(0, 0, 1.4)));
        assertFalse(PlasmaCannonSkill.arrived(Vec3.ZERO, new Vec3(0, 0, 1.5)));
        assertFalse(PlasmaCannonSkill.arrived(Vec3.ZERO, new Vec3(0, 0, 2)));
    }
}
