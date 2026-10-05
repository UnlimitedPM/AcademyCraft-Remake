package cn.academy.ability.teleporter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Les decalages d'atterrissage des teleportations visees.
 *
 * <p>Ce sont les seuls chiffres du portage qui decident si le joueur se retrouve <b>devant</b>
 * un mur ou dedans. Ils venaient de la fin de {@code getDest} de l'original, ou ils etaient
 * recopies dans chaque competence de teleportation ; ils vivent maintenant a un seul endroit,
 * et ce fichier les fige : six faces, plus le cas de la tete qui ne passe pas.
 *
 * <p>La classe ne connait ni monde ni joueur, donc le cas de la tete se simule avec un
 * predicat : c'est exactement ce que la competence lui passe en jeu.
 */
class LandingSiteTest {

    private static final BlockPos BLOCK = new BlockPos(10, 64, 10);

    /** Un point d'impact au milieu de la face nord du bloc, par exemple. */
    private static Vec3 point(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    @Test
    void chaqueFaceDecaleVersLaSurface() {
        Vec3 impact = point(10.5, 64.5, 10.0);

        // Le dessous : on ressort un bloc plus bas.
        assertEquals(new Vec3(10.5, 63.5, 10.0),
                LandingSite.onBlockFace(Direction.DOWN, impact, BLOCK, pos -> false));
        // Le dessus : on monte d'un bloc et huit dixiemes.
        assertEquals(new Vec3(10.5, 66.3, 10.0),
                LandingSite.onBlockFace(Direction.UP, impact, BLOCK, pos -> false));

        // Les quatre cotes : soixante centimetres en arriere, et la hauteur se lit sur le
        // bloc touche, pas sur le point d'impact — c'est ce qui fait qu'on se tient toujours
        // a la meme hauteur devant un mur, quel que soit l'endroit vise.
        Vec3 north = LandingSite.onBlockFace(Direction.NORTH, impact, BLOCK, pos -> false);
        assertEquals(9.4, north.z, 0.0001);
        assertEquals(65.7, north.y, 0.0001);

        Vec3 south = LandingSite.onBlockFace(Direction.SOUTH, impact, BLOCK, pos -> false);
        assertEquals(10.6, south.z, 0.0001);
        assertEquals(65.7, south.y, 0.0001);

        Vec3 west = LandingSite.onBlockFace(Direction.WEST, impact, BLOCK, pos -> false);
        assertEquals(9.9, west.x, 0.0001);
        assertEquals(65.7, west.y, 0.0001);

        Vec3 east = LandingSite.onBlockFace(Direction.EAST, impact, BLOCK, pos -> false);
        assertEquals(11.1, east.x, 0.0001);
        assertEquals(65.7, east.y, 0.0001);
    }

    @Test
    void lesPiedsSontUneHauteurDYeuxSousLaTete() {
        Vec3 impact = point(10.5, 64.5, 10.0);
        double eyes = 1.62;

        // Sur le dessus du bloc : on se tient a dix-huit centimetres de la surface, pas a un
        // bloc et demi au-dessus d'elle. C'est la difference entre un fantome plante dans le
        // sol et un fantome en l'air, plus loin que le bloc qu'on vise.
        Vec3 top = LandingSite.onFeet(Direction.UP, impact, BLOCK, pos -> false, eyes);
        assertEquals(10.5, top.x, 0.0001);
        assertEquals(64.68, top.y, 0.0001);
        assertEquals(10.0, top.z, 0.0001);

        // Devant un mur, on se tient a huit centimetres du sol du bloc, la tete degagee.
        Vec3 north = LandingSite.onFeet(Direction.NORTH, impact, BLOCK, pos -> false, eyes);
        assertEquals(9.4, north.z, 0.0001);
        assertEquals(64.08, north.y, 0.0001);

        // Et le retrait est exactement la hauteur d'yeux : c'est tout ce que fait cette porte.
        Vec3 head = LandingSite.onBlockFace(Direction.UP, impact, BLOCK, pos -> false);
        assertEquals(head.y - eyes, LandingSite.onFeet(Direction.UP, impact, BLOCK, pos -> false, eyes).y,
                1e-9);
    }

    @Test
    void uneTeteDansLePassageFaitRedescendre() {
        Vec3 impact = point(10.5, 64.5, 10.0);

        Vec3 free = LandingSite.onBlockFace(Direction.NORTH, impact, BLOCK, pos -> false);
        Vec3 blocked = LandingSite.onBlockFace(Direction.NORTH, impact, BLOCK, pos -> true);

        // Un bloc et quart plus bas : on passe sous l'obstacle plutot que dedans.
        assertEquals(free.y - 1.25, blocked.y, 0.0001);
        // Et rien ne bouge horizontalement.
        assertEquals(free.z, blocked.z, 0.0001);
        assertEquals(free.x, blocked.x, 0.0001);
    }

    @Test
    void seulesLesFacesVerticalesSeSoucientDeLaTete() {
        Vec3 impact = point(10.5, 64.5, 10.0);

        // Le dessus et le dessous n'ont pas de passage a hauteur de tete : la verification
        // ne doit meme pas etre posee, et le predicat le dit ici.
        var refused = new java.util.concurrent.atomic.AtomicBoolean(false);

        assertEquals(new Vec3(10.5, 66.3, 10.0),
                LandingSite.onBlockFace(Direction.UP, impact, BLOCK, pos -> {
                    refused.set(true);
                    return true;
                }));
        assertEquals(new Vec3(10.5, 63.5, 10.0),
                LandingSite.onBlockFace(Direction.DOWN, impact, BLOCK, pos -> {
                    refused.set(true);
                    return true;
                }));
        assertFalse(refused.get(), "les faces horizontales ne doivent pas consulter la tete");
    }

    @Test
    void laTeteSeVerifieUnBlocAuDessusDuPointDecale() {
        Vec3 impact = point(10.5, 64.5, 10.0);
        var tested = new java.util.concurrent.atomic.AtomicReference<BlockPos>();

        LandingSite.onBlockFace(Direction.NORTH, impact, BLOCK, pos -> {
            tested.set(pos);
            return false;
        });

        // Le point d'atterrissage est a (10.5, 65.7, 9.4) : la tete occupe donc le bloc
        // (10, 66, 9), comme dans l'original qui ajoutait un bloc a y avant d'arrondir.
        assertEquals(new BlockPos(10, 66, 9), tested.get());
    }
}
