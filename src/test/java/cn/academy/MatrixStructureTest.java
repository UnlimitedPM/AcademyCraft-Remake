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
}
