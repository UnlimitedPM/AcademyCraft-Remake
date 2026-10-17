package cn.academy;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La geometrie du Matrix, relue sans Minecraft.
 *
 * <p>Le Matrix est un cube de deux blocs de cote, et sa partie d'ancrage en est un coin :
 * les sept autres parties se posent vers la gauche et vers l'arriere de l'ancrage, a un
 * bloc de distance. Le centre de la machine se deduit de la, et c'est lui qui dit ou poser
 * le modele — centre, lui, sur son origine.
 */
class MatrixStructureTest {

    @Test
    @DisplayName("les deux axes du multi-bloc sont ceux du placement")
    void lesDeuxAxesDuMultiBloc() {
        // Meme convention que MatrixBlock.setPlacedBy : c'est elle qui place les huit
        // parties, donc c'est elle qui decide ou est le centre.
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            assertEquals(facing.getCounterClockWise(), MatrixStructure.left(facing));
            assertEquals(facing.getOpposite(), MatrixStructure.back(facing));
        }
    }

    @Test
    @DisplayName("chaque axe recoit un bloc plein, jamais un demi-bloc")
    void jamaisUnDemiBloc() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            double x = MatrixStructure.centreX(facing);
            double z = MatrixStructure.centreZ(facing);
            assertTrue(x == 0.0d || x == 1.0d, "decalage en x pour " + facing + " : " + x);
            assertTrue(z == 0.0d || z == 1.0d, "decalage en z pour " + facing + " : " + z);
        }
    }

    @Test
    @DisplayName("le centre tombe sur le milieu des quatre blocs du bas")
    void leCentreTombeSurLeMilieuDesQuatreBlocs() {
        // Le calcul, en clair. Le bloc d'ancrage est en (10, 64, 10) et occupe donc
        // x de 10 a 11, z de 10 a 11. Vers le nord, `left` est l'ouest et `back` le sud :
        // la partie posee a un bloc de la est en x de 9 a 10, et celle posee vers
        // l'arriere en z de 11 a 12. Le multi-bloc va donc de x 9 a 11 et de z 10 a 12 :
        // son centre est en (10, 64, 11), soit (0, 1) depuis le coin du bloc d'ancrage.
        assertEquals(0.0d, MatrixStructure.centreX(Direction.NORTH), 0.0d);
        assertEquals(1.0d, MatrixStructure.centreZ(Direction.NORTH), 0.0d);

        // Vers le sud, c'est l'inverse : `left` est l'est, `back` le nord.
        assertEquals(1.0d, MatrixStructure.centreX(Direction.SOUTH), 0.0d);
        assertEquals(0.0d, MatrixStructure.centreZ(Direction.SOUTH), 0.0d);

        // Vers l'est, les deux axes partent vers l'ouest et le nord : le coin du bloc
        // d'ancrage est deja le centre du multi-bloc.
        assertEquals(0.0d, MatrixStructure.centreX(Direction.EAST), 0.0d);
        assertEquals(0.0d, MatrixStructure.centreZ(Direction.EAST), 0.0d);

        // Vers l'ouest, les deux axes partent vers l'est et le sud : un bloc dans chaque.
        assertEquals(1.0d, MatrixStructure.centreX(Direction.WEST), 0.0d);
        assertEquals(1.0d, MatrixStructure.centreZ(Direction.WEST), 0.0d);
    }

    @Test
    @DisplayName("le centre calcule tombe au milieu des quatre blocs du bas")
    void leCentreTombeAuMilieuDesBlocsDuBas() {
        // Les quatre blocs du bas sont poses en l = 0 ou 1 et b = 0 ou 1 — la table de
        // MatrixBlock les nomme B_F_R(0,0,0), B_F_L(1,0,0), B_B_R(0,0,1), B_B_L(1,0,1).
        // Cette table ne se lit pas ici : MatrixBlock est une classe de Minecraft, et la
        // charger dans un test unitaire demande un bootstrap de registres.
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            double sumX = 0.0d;
            double sumZ = 0.0d;
            for (int l = 0; l <= 1; l++) {
                for (int b = 0; b <= 1; b++) {
                    sumX += 0.5d + MatrixStructure.left(facing).getStepX() * l
                            + MatrixStructure.back(facing).getStepX() * b;
                    sumZ += 0.5d + MatrixStructure.left(facing).getStepZ() * l
                            + MatrixStructure.back(facing).getStepZ() * b;
                }
            }
            assertEquals(MatrixStructure.centreX(facing), sumX / 4.0d, 1.0e-9d,
                    "centre en x pour " + facing);
            assertEquals(MatrixStructure.centreZ(facing), sumZ / 4.0d, 1.0e-9d,
                    "centre en z pour " + facing);
        }
    }

    @Test
    @DisplayName("la portee se compte depuis la surface du cube, jusqu'au centre du bloc vise")
    void laPorteePartDeLaSurfaceDuCube() {
        // Ancrage en (10, 64, 10) tourne vers le nord : ses huit parties occupent x 9..11,
        // y 64..66 et z 10..12. Le cube fait donc deux blocs de large dans les trois axes et son
        // centre est en (10, 65, 11).
        //
        // Le bloc pose contre lui, en (11, 65, 11), a son centre a (11,5 ; 65,5 ; 11,5) : il est a un
        // demi-bloc de la surface du cube, et a un demi-bloc en hauteur et en profondeur il est
        // DEDANS. Sa distance est donc 0,5 — pas 1,5 comme si on comptait depuis le coin.
        assertEquals(0.25d, MatrixStructure.machineDistSqr(10, 64, 10, Direction.NORTH,
                11, 65, 11), 1.0e-9d);

        // Un bloc a l'interieur du cube est a distance nulle : le cube n'est pas un point.
        assertEquals(0.0d, MatrixStructure.machineDistSqr(10, 64, 10, Direction.NORTH,
                10, 65, 11), 1.0e-9d);
    }

    @Test
    @DisplayName("un noeud se raccorde a la meme distance des quatre cotes")
    void memePorteeDesQuatreCotes() {
        // Ce que le joueur a mesure, deux fois : d'abord « il y a 2 cotes ou les nodes se relient a
        // onze blocs de distance, et les 2 autres cotes ou il se connecte a seulement neuf », puis,
        // le centre corrige, « il fait toujours un rayon de 11x11x10x10 en fonction de comment je
        // l'ai pose ».
        //
        // On compte donc les blocs VIDES entre le cube et le noeud : ce nombre doit etre le meme des
        // quatre cotes, pour une portee de noeud donnee. Douze est celle du noeud STANDARD.
        for (Direction side : Direction.Plane.HORIZONTAL) {
            assertEquals(11, blocsVidesAtteints(side, 12.0d), "cote " + side);
        }
    }

    /**
     * Le nombre de blocs vides entre le cube et le noeud le plus eloigne encore atteint.
     *
     * <p>Le cube va de x 9 a 11 et de z 10 a 12 : le premier bloc vide de chaque cote est donc le
     * onze en x croissant, le huit en x decroissant, le douze en z croissant et le neuf en z
     * decroissant.
     */
    private static int blocsVidesAtteints(Direction side, double range) {
        int stepX = side.getStepX();
        int stepZ = side.getStepZ();
        int atteints = -1;
        for (int vides = 0; vides <= 30; vides++) {
            int dx = stepX == 0 ? 0 : (stepX > 0 ? 1 : -2) + stepX * vides;
            int dz = stepZ == 0 ? 0 : (stepZ > 0 ? 1 : -2) + stepZ * vides;
            double distSqr = MatrixStructure.machineDistSqr(10, 64, 10, Direction.NORTH,
                    10 + dx, 65, 11 + dz);
            if (distSqr > range * range) break;
            atteints = vides;
        }
        return atteints;
    }
}
