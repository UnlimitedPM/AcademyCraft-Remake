package cn.academy.ability.electromaster;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ce que fait un bloc attrape, {@code MagManipVisuals} : ou il se tient, comment il y va, et
 * comment il tourne.
 *
 * <p>Trois calculs courts, mais ce sont eux qui decident de tout : le point de portage est ce
 * que le joueur voit devant lui, la vitesse de portage doit s'annuler sur ce point au lieu de
 * l'osciller, et la vitesse de lancer doit partir vers ce que le regard touche.
 */
class MagManipTest {

    private static final double EPS = 1.0e-9;

    @Test
    void leBlocSeTientDeuxBlocsDevantLesYeux() {
        Vec3 eye = new Vec3(10, 65, 10);
        Vec3 look = new Vec3(0, 0, 1);

        Vec3 target = MagManipVisuals.carryTarget(eye, look);
        assertEquals(10.0, target.x, EPS);
        assertEquals(64.9, target.y, 1.0e-6, "un dixieme sous la tete, comme l'original");
        assertEquals(12.0, target.z, 1.0e-6, "et deux blocs devant");

        // Le regard incline deplace le point avec lui : on tient le bloc ou l'on regarde.
        Vec3 down = MagManipVisuals.carryTarget(eye, new Vec3(0, -1, 0));
        assertEquals(62.9, down.y, 1.0e-6, "deux blocs sous les yeux, plus le dixieme");
    }

    @Test
    void laVitesseDePortageSAnnuleSurLePoint() {
        Vec3 target = new Vec3(0, 0, 0);

        // Loin : la vitesse pleine, 0,2 par tick, dans la direction du point.
        Vec3 far = MagManipVisuals.carryVelocity(new Vec3(0, 0, 10), target);
        assertEquals(0.2, far.length(), EPS);
        assertEquals(-0.2, far.z, 1.0e-6, "elle va vers le point");

        // A un bloc : ralentie au quart — c'est le distSq / 4 de l'original.
        Vec3 near = MagManipVisuals.carryVelocity(new Vec3(0, 0, 1), target);
        assertEquals(0.2 * 1.0 / 4.0, near.length(), EPS, "a un bloc, un quart de la vitesse");

        // Et sur le point : nulle, donc le bloc s'y arrete au lieu de le depasser d'un cote
        // puis de l'autre.
        assertEquals(Vec3.ZERO, MagManipVisuals.carryVelocity(target, target));
    }

    @Test
    void leLancerPartVersCeQueLeRegardTouche() {
        // Un bloc au-dessus de la tete, un point vise devant : il part vers le bas et
        // devant, a la vitesse demandee et pas a une autre.
        Vec3 velocity = MagManipVisuals.throwVelocity(new Vec3(0, 10, 0), new Vec3(0, 0, 10), 1.0);
        assertEquals(1.0, velocity.length(), EPS);
        assertTrue(velocity.y < 0, "il descend vers le point : " + velocity);
        assertTrue(velocity.z > 0, "et il avance : " + velocity);

        // La vitesse suit l'experience : de 0,5 a 1.
        assertEquals(0.5, MagManipVisuals.throwVelocity(new Vec3(0, 10, 0),
                new Vec3(0, 0, 10), 0.5).length(), EPS);
        assertEquals(1.0, MagManipVisuals.throwVelocity(new Vec3(0, 10, 0),
                new Vec3(0, 0, 10), 1.0).length(), EPS);

        // Un point confondu avec le bloc n'a pas de direction : pas de division par zero.
        assertEquals(Vec3.ZERO, MagManipVisuals.throwVelocity(new Vec3(1, 2, 3),
                new Vec3(1, 2, 3), 1.0));
    }

    @Test
    void chaqueBlocTourneARythmeDifferent() {
        // L'original tirait deux vitesses au hasard, entre 1 et 3 degres par tick. Le port les
        // deduit de l'identifiant : elles restent entieres, jamais nulles, et deux blocs
        // voisins ne tournent pas ensemble.
        for (int id = 0; id < 9; id++) {
            double yaw = MagManipVisuals.spinYaw(10, id) - MagManipVisuals.spinYaw(9, id);
            double pitch = MagManipVisuals.spinPitch(10, id) - MagManipVisuals.spinPitch(9, id);

            assertTrue(yaw >= 1.0 && yaw <= 3.0, "lacet du bloc " + id + " : " + yaw);
            assertTrue(pitch >= 0.75 && pitch <= 2.25, "tangage du bloc " + id + " : " + pitch);
        }

        // Et les deux vitesses ne tombent jamais ensemble : sinon le bloc tournerait autour
        // d'un axe fixe, ce qui se voit tout de suite.
        boolean same = false;
        for (int id = 0; id < 9; id++) {
            same |= MagManipVisuals.spinYaw(1, id) == MagManipVisuals.spinPitch(1, id);
        }
        assertFalse(same, "un lacet et un tangage egaux donneraient un axe fixe");

        // A l'instant zero, tous les blocs sont droits.
        assertEquals(0.0, MagManipVisuals.spinYaw(0, 7), EPS);
    }

    @Test
    void leGresillementGardeQuatreArcsVivants() {
        // L'original avait quatre arcs d'entourage vivants a la fois, et chacun vivait trente
        // ticks : il en reensemencait donc un de temps en temps. Ici un arc ne vit que trois
        // ticks, donc les semis se repartissent sur ces trois ticks : deux, un, un.
        assertEquals(2, MagManipVisuals.arcsToSow(0));
        assertEquals(1, MagManipVisuals.arcsToSow(1));
        assertEquals(1, MagManipVisuals.arcsToSow(2));
        assertEquals(2, MagManipVisuals.arcsToSow(3));

        // Et le compte retombe toujours sur quatre arcs vivants par periode.
        int sown = 0;
        for (long tick = 0; tick < MagManipVisuals.ARC_LIFE; tick++) {
            sown += MagManipVisuals.arcsToSow(tick);
        }
        assertEquals(MagManipVisuals.ARCS_ALIVE, sown, "quatre arcs par vie d'arc");

        // Aucun tick ne seme rien : ca s'eteindrait un instant, et cela se verrait.
        for (long tick = 0; tick < 100; tick++) {
            assertTrue(MagManipVisuals.arcsToSow(tick) >= 1, "rien seme au tick " + tick);
        }
    }

    @Test
    void leCubeDuGresillementEstCeluiDuBloc() {
        // Un bloc, fois le sizeMultiplyer de 1,3 de l'original.
        assertEquals(1.3, MagManipVisuals.SURROUND_CUBE, EPS);
    }
}
