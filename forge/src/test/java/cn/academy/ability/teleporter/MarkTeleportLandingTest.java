package cn.academy.ability.teleporter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * L'atterrissage de la teleportation au marqueur : le dessus d'un bloc degage, et la place
 * qu'il faut.
 *
 * <p>Demande du joueur, 22/10 : viser le dernier bloc d'un mur (ou un bloc au-dessus de soi) doit
 * le poser <b>dessus</b>, pas devant ; et le saut final d'un bloc ne s'ajoute que s'il reste la
 * place. Les deux chiffres qui decident de tout sont ici, sans monde ni joueur : le predicat dit
 * si un bloc est occupe, exactement comme la competence le lui passe en jeu.
 */
class MarkTeleportLandingTest {

    private static final BlockPos BLOCK = new BlockPos(10, 64, 10);
    private static final double EYES = 1.62;

    /** Le point d'impact au milieu de la face nord du bloc. */
    private static final Vec3 NORTH_FACE = new Vec3(10.5, 64.5, 10.0);

    /** Un predicat d'occupation construit sur une liste de blocs pleins. */
    private static Predicate<BlockPos> solid(BlockPos... blocks) {
        Set<BlockPos> set = Set.of(blocks);
        return set::contains;
    }

    /**
     * Le dernier bloc d'un mur : son dessus est degage, donc on s'y tient — au centre du bloc, et
     * non sur l'arete visee.
     */
    @Test
    void leDernierBlocDUnMurSePrendParLeDessus() {
        Vec3 feet = MarkTeleportLanding.feet(Direction.NORTH, NORTH_FACE, BLOCK,
                solid(BLOCK, new BlockPos(10, 67, 10)), EYES);

        assertEquals(10.5, feet.x, 1e-9, "au centre du bloc en x");
        assertEquals(65.0, feet.y, 1e-9, "sur la surface du dessus");
        assertEquals(10.5, feet.z, 1e-9, "au centre du bloc en z");
    }

    /** Un bloc au-dessus de soi se prend aussi par le dessus, meme en le regardant par le bas. */
    @Test
    void unBlocAuDessusDeSoiSePrendParLeDessus() {
        Vec3 underside = new Vec3(10.5, 64.0, 10.5);

        Vec3 feet = MarkTeleportLanding.feet(Direction.DOWN, underside, BLOCK,
                solid(BLOCK, new BlockPos(10, 67, 10)), EYES);

        assertEquals(65.0, feet.y, 1e-9, "on se tient sur le bloc, pas dessous");
        assertEquals(10.5, feet.x, 1e-9);
        assertEquals(10.5, feet.z, 1e-9);
    }

    /** Un bloc au milieu d'un mur garde son atterrissage devant la face, comme avant. */
    @Test
    void unBlocCouverteGardeLaFace() {
        Vec3 feet = MarkTeleportLanding.feet(Direction.NORTH, NORTH_FACE, BLOCK,
                solid(BLOCK, BLOCK.above()), EYES);

        // Soixante centimetres devant la face, et le saut d'un bloc s'ajoute : la colonne devant
        // le mur est libre de ses trois blocs, comme en jeu (on arrive un bloc au-dessus du sol
        // et l'on retombe).
        assertEquals(65.08, feet.y, 1e-9, "les pieds un bloc au-dessus du niveau du bloc vise");
        assertEquals(9.4, feet.z, 1e-9, "soixante centimetres devant la face");
    }

    /** Un seul bloc d'air au-dessus du dernier bloc ne suffit pas : le joueur n'y tient pas. */
    @Test
    void unSeulBlocLibreNeSuffitPasASeTenirDessus() {
        Vec3 feet = MarkTeleportLanding.feet(Direction.NORTH, NORTH_FACE, BLOCK,
                solid(BLOCK, BLOCK.above().above()), EYES);

        assertEquals(9.4, feet.z, 1e-9, "on retombe devant la face");
        assertEquals(65.08, feet.y, 1e-9,
                "un bloc au-dessus du sol du bloc vise, et non sur son dessus");
    }

    /** Le saut d'un bloc ne s'ajoute qu'avec trois blocs libres : le saut, puis les deux du corps. */
    @Test
    void leSautDUneHauteurDemandeTroisBlocsLibres() {
        Vec3 free = MarkTeleportLanding.feet(Direction.NORTH, NORTH_FACE, BLOCK,
                solid(BLOCK, new BlockPos(10, 68, 10)), EYES);
        assertEquals(66.0, free.y, 1e-9, "trois blocs libres : on arrive un bloc plus haut");

        Vec3 twoOnly = MarkTeleportLanding.feet(Direction.NORTH, NORTH_FACE, BLOCK,
                solid(BLOCK, new BlockPos(10, 67, 10)), EYES);
        assertEquals(65.0, twoOnly.y, 1e-9, "deux blocs libres : on se pose a la surface");
    }

    /** Le saut ne se pose pas non plus par-dessus un plafond, meme quand le dessus est degage. */
    @Test
    void leSautNeTraversePasUnPlafond() {
        // Un plafond juste au-dessus de la tete du saut : le bloc du dessus (66) est libre, celui
        // du saut (67) ne l'est pas.
        Vec3 feet = MarkTeleportLanding.feet(Direction.NORTH, NORTH_FACE, BLOCK,
                solid(BLOCK, BLOCK.above().above().above()), EYES);

        assertEquals(65.0, feet.y, 1e-9, "on reste a la surface du bloc");
    }

    /**
     * Le fantome se pose AU SOL, sans le saut d'un bloc.
     *
     * <p>Demande du joueur, 22/10 : le fantome marque l'endroit ou l'on se tient, pas celui d'ou
     * l'on tombe. Le joueur, lui, arrive bien un bloc au-dessus.
     */
    @Test
    void leFantomeSePoseSansLeSaut() {
        Vec3 surface = MarkTeleportLanding.onSurface(Direction.NORTH, NORTH_FACE, BLOCK,
                solid(BLOCK), EYES);
        Vec3 feet = MarkTeleportLanding.feet(Direction.NORTH, NORTH_FACE, BLOCK,
                solid(BLOCK), EYES);

        assertEquals(65.0, surface.y, 1e-9, "le fantome touche le dessus du bloc");
        assertEquals(66.0, feet.y, 1e-9, "le joueur, lui, arrive un bloc plus haut");
        assertEquals(surface.x, feet.x, 1e-9, "et au meme endroit, en x");
        assertEquals(surface.z, feet.z, 1e-9, "et au meme endroit, en z");

        // Devant un mur, la meme difference d'un bloc : la colonne devant lui est libre.
        Predicate<BlockPos> wall = solid(BLOCK, BLOCK.above());
        assertEquals(64.08,
                MarkTeleportLanding.onSurface(Direction.NORTH, NORTH_FACE, BLOCK, wall, EYES).y,
                1e-9, "le fantome devant le mur, au sol");
        assertEquals(65.08,
                MarkTeleportLanding.feet(Direction.NORTH, NORTH_FACE, BLOCK, wall, EYES).y,
                1e-9, "le joueur, un bloc au-dessus");
    }
}
