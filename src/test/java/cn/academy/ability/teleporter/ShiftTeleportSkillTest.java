package cn.academy.ability.teleporter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La geometrie du depose au loin : ou le bloc se pose, et ce que la ligne traverse.
 *
 * <p>Ce sont les morceaux <b>purs</b> du pouvoir — les deux facons de lire un rayon, et le
 * croisement d'un segment et d'une boite —, donc ceux qui se verifient sans lancer le jeu. La pose
 * elle-meme, les degats et le fait que le joueur ne bouge pas demandent un monde, et se verifient
 * en jeu.
 */
class ShiftTeleportSkillTest {

    @Test
    @DisplayName("la case visee suit la face touchee, et le point touche la suit aussi")
    void laCaseSuitLaFace() {
        // Une face touchee, et la case qui la suit : c'est la que le bloc se poserait. Le point
        // touche se deplace du meme cran, parce que c'est lui que la pose lit pour orienter un bloc
        // qui a une direction — un escalier, un coffre.
        BlockPos block = new BlockPos(10, 64, 10);
        Vec3 hit = new Vec3(10.5, 64.2, 10.3);

        assertTarget(Direction.UP, block, hit, new BlockPos(10, 65, 10), new Vec3(10.5, 65.2, 10.3));
        assertTarget(Direction.DOWN, block, hit, new BlockPos(10, 63, 10), new Vec3(10.5, 63.2, 10.3));
        assertTarget(Direction.NORTH, block, hit, new BlockPos(10, 64, 9), new Vec3(10.5, 64.2, 9.3));
        assertTarget(Direction.SOUTH, block, hit, new BlockPos(10, 64, 11), new Vec3(10.5, 64.2, 11.3));
        assertTarget(Direction.WEST, block, hit, new BlockPos(9, 64, 10), new Vec3(9.5, 64.2, 10.3));
        assertTarget(Direction.EAST, block, hit, new BlockPos(11, 64, 10), new Vec3(11.5, 64.2, 10.3));
    }

    private static void assertTarget(Direction face, BlockPos block, Vec3 hit, BlockPos cell,
                                     Vec3 point) {
        ShiftTeleportSkill.Target target =
                ShiftTeleportSkill.fromHit(new BlockHitResult(hit, face, block, false));

        assertEquals(cell, target.cell(), "la case de la face " + face);
        assertEquals(point, target.point(), "le point de la face " + face);
        assertEquals(face, target.face(), "la face touchee");
        assertEquals(block, target.block(), "le bloc touche, dont la face est justement la");
    }

    @Test
    @DisplayName("un regard qui ne butte sur rien vise la case ou il passe")
    void laCaseDuVide() {
        // Le rayon part vers le bas du monde : un bloc qui se pose dans le vide s'y accroche par en
        // dessous, et la case est celle qui contient le bout du regard — celle du bloc, pas du point.
        ShiftTeleportSkill.Target target =
                ShiftTeleportSkill.fromMiss(new Vec3(10.7, 70.2, -3.4));

        assertEquals(new BlockPos(10, 70, -4), target.cell(), "la case qui contient le bout");
        assertEquals(Direction.DOWN, target.face(), "un bloc du vide s'accroche par en dessous");
        assertEquals(new Vec3(10.7, 70.2, -3.4), target.point(), "et il tombe au bout du regard");
    }

    @Test
    @DisplayName("la ligne prend ce qu'elle croise, et rien d'a cote")
    void laLigneCroiseLesBoites() {
        Vec3 from = new Vec3(0, 64, 0);
        Vec3 to = new Vec3(20, 64, 0);

        assertTrue(ShiftTeleportSkill.crosses(new AABB(-1, 63, -1, 1, 65, 1), from, to),
                "une creature a l'entree de la ligne y est");
        assertTrue(ShiftTeleportSkill.crosses(new AABB(9.8, 63.5, -0.2, 10.2, 65, 0.2), from, to),
                "et une au milieu aussi");

        assertFalse(ShiftTeleportSkill.crosses(new AABB(9, 63.5, 1.5, 10, 65, 2.5), from, to),
                "a cote, non");
        assertFalse(ShiftTeleportSkill.crosses(new AABB(-3, 63, -1, -2, 65, 1), from, to),
                "derriere le geste, non");
        assertFalse(ShiftTeleportSkill.crosses(new AABB(21, 63, -1, 22, 65, 1), from, to),
                "et au-dela de la case visee, non plus");

        // Le geste part des PIEDS : une creature au ras du sol est sur la ligne, meme si elle est
        // petite — c'est le corps qui frappe, pas le regard.
        Vec3 low = new Vec3(0, 64, 0);
        assertTrue(ShiftTeleportSkill.crosses(new AABB(9.8, 64, -0.2, 10.2, 64.4, 0.2), low,
                        new Vec3(20, 64.5, 0)),
                "une creature basse, sur une ligne qui monte a peine");
    }
}
