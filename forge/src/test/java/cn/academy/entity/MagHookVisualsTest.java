package cn.academy.entity;

import cn.academy.client.render.ObjMesh;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Les nombres du crochet magnetique : son vol, son accroche, ses angles et ses deux modeles.
 *
 * <p>Ce sont ceux d'{@code EntityMagHook} et de {@code RendererMagHook}, et aucun ne se verifie
 * sans lancer un jeu. Deux d'entre eux portent pourtant plus que de la fidelite :
 *
 * <ul>
 *   <li>le point d'accroche, parce que c'est lui qui fait paraitre le crochet pose CONTRE la paroi
 *       au lieu d'y etre enfonce ;</li>
 *   <li>et les angles des six faces, qui couchent le crochet a plat sous un plafond — une faute s'y
 *       verrait tout de suite, et ne se lirait nulle part ailleurs.</li>
 * </ul>
 */
class MagHookVisualsTest {

    private static final double EPSILON = 1e-9;

    @Test
    @DisplayName("il se plante un centieme devant la face, comme l'original")
    void ilSePlanteDevantLaFace() {
        BlockPos block = new BlockPos(3, -7, 11);

        // 0,51 depuis le CENTRE du bloc : le demi-bloc y mene, et le centieme qui reste est DEVANT
        // la face — c'est le `setPosition(hookX + 0.5 + dir * 0.51, ...)` de l'original.
        assertVec(MagHookVisuals.snapTo(block, Direction.NORTH), 3.5, -6.5, 10.99);
        assertVec(MagHookVisuals.snapTo(block, Direction.SOUTH), 3.5, -6.5, 12.01);
        assertVec(MagHookVisuals.snapTo(block, Direction.WEST), 2.99, -6.5, 11.5);
        assertVec(MagHookVisuals.snapTo(block, Direction.EAST), 4.01, -6.5, 11.5);
        assertVec(MagHookVisuals.snapTo(block, Direction.DOWN), 3.5, -7.01, 11.5);
        assertVec(MagHookVisuals.snapTo(block, Direction.UP), 3.5, -5.99, 11.5);
    }

    @Test
    @DisplayName("chaque face lui donne son angle, comme le switch de l'original")
    void chaqueFaceDonneSonAngle() {
        // Les faces du sol et du plafond le couchent, par le tangage.
        assertEquals(-90f, MagHookVisuals.pitchFor(Direction.DOWN), 1e-4f);
        assertEquals(90f, MagHookVisuals.pitchFor(Direction.UP), 1e-4f);
        assertEquals(0f, MagHookVisuals.yawFor(Direction.DOWN), 1e-4f);
        assertEquals(0f, MagHookVisuals.yawFor(Direction.UP), 1e-4f);

        // Les quatre faces verticales le tournent, et le laissent droit.
        assertEquals(0f, MagHookVisuals.yawFor(Direction.NORTH), 1e-4f);
        assertEquals(180f, MagHookVisuals.yawFor(Direction.SOUTH), 1e-4f);
        assertEquals(-90f, MagHookVisuals.yawFor(Direction.WEST), 1e-4f);
        assertEquals(90f, MagHookVisuals.yawFor(Direction.EAST), 1e-4f);
        for (Direction side : List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST,
                Direction.EAST)) {
            assertEquals(0f, MagHookVisuals.pitchFor(side), 1e-4f, side + " se tient droit");
        }
    }

    @Test
    @DisplayName("il vole a deux blocs par tick et fait quatre degats, comme l'original")
    void ilVoleCommeLOriginal() {
        assertEquals(2.0, MagHookVisuals.SPEED, EPSILON);
        assertEquals(0.05, MagHookVisuals.GRAVITY, EPSILON);
        assertEquals(4f, MagHookVisuals.HIT_DAMAGE, 1e-4f);
        assertEquals(0.0054f, MagHookVisuals.MODEL_SCALE, 1e-6f);
    }

    @Test
    @DisplayName("se planter l'ouvre en grand : un demi-bloc en vol, un bloc plante")
    void sePlanterLOuvre() {
        assertEquals(0.5f, MagHookVisuals.FLY_SIZE, 1e-4f);
        // Un bloc entier contre une paroi, et c'est voulu : c'est la boite que la traction doit
        // rencontrer (MagMovementSkill ne vise que les entites `isPickable`), et une pince qu'il
        // faudrait toucher au centimetre pres ne serait pas jouable. L'original changeait de taille
        // de la meme facon, par un second setSize.
        assertEquals(1f, MagHookVisuals.HIT_SIZE, 1e-4f);
    }

    @Test
    @DisplayName("les deux modeles portent bien les neuf groupes du rendu")
    void lesDeuxModelesOntLeursGroupes() throws Exception {
        // Le rendu dessine le crochet groupe par groupe, avec des noms ecrits a la main : un nom
        // qui change dans le fichier laisserait un morceau invisible, sans erreur ni avertissement.
        for (String file : new String[] { "/assets/academy/models/maghook.obj",
                                          "/assets/academy/models/maghook_open.obj" }) {
            ObjMesh mesh = lire(file);
            int connu = 0;
            for (String group : MagHookVisuals.GROUPS) {
                List<ObjMesh.Face> faces = mesh.group(group);
                assertFalse(faces.isEmpty(), file + " : le groupe " + group + " est vide");
                connu += faces.size();
            }
            assertEquals(mesh.all().size(), connu,
                    file + " : un groupe du fichier n'est pas dans MagHookVisuals.GROUPS");
        }
    }

    @Test
    @DisplayName("le crochet a quatre crampons, et pas trois")
    void leCrochetAQuatreCrampons() throws Exception {
        // L'original n'en avait que trois : son auteur avait duplique la paire +/-X et le crampon
        // +Z, mais pas le jumeau de ce dernier. Le port a ajoute ce quatrieme crampon — les deux
        // groupes `_mirror` — et c'est le joueur qui l'a demande : « il pourrait tres clairement en
        // posseder 4 [...] je prefererais en voir 4 ».
        for (String file : new String[] { "/assets/academy/models/maghook.obj",
                                          "/assets/academy/models/maghook_open.obj" }) {
            ObjMesh mesh = lire(file);
            for (String[] paire : new String[][] { { "Object004", "Object004_mirror" },
                                                   { "Object005", "Object005_mirror" } }) {
                double[] source = zExtent(mesh, paire[0]);
                double[] jumeau = zExtent(mesh, paire[1]);
                assertEquals(-source[1], jumeau[0], EPSILON,
                        file + " : " + paire[1] + " commence au miroir de la fin de " + paire[0]);
                assertEquals(-source[0], jumeau[1], EPSILON,
                        file + " : et finit au miroir de son debut");
            }
            // Et le modele entier est symetrique en Z autour de son origine : quatre crampons
            // autour de l'axe, plus trois. C'est ce que le joueur voulait voir.
            ObjMesh tout = mesh;
            double mn = Double.MAX_VALUE, mx = -Double.MAX_VALUE;
            for (ObjMesh.Face face : tout.all()) {
                for (ObjMesh.Vertex v : face.vertices()) {
                    mn = Math.min(mn, v.z());
                    mx = Math.max(mx, v.z());
                }
            }
            assertEquals(mx, -mn, 1e-4, file + " : le crochet est symetrique en Z");
        }
    }

    /** L'etendue en Z d'un groupe du modele : { min, max }. */
    private static double[] zExtent(ObjMesh mesh, String group) {
        double mn = Double.MAX_VALUE, mx = -Double.MAX_VALUE;
        for (ObjMesh.Face face : mesh.group(group)) {
            assertFalse(face.vertices().isEmpty(), group + " a des faces");
            for (ObjMesh.Vertex v : face.vertices()) {
                mn = Math.min(mn, v.z());
                mx = Math.max(mx, v.z());
            }
        }
        return new double[] { mn, mx };
    }

    private static ObjMesh lire(String chemin) throws Exception {
        try (var in = MagHookVisualsTest.class.getResourceAsStream(chemin)) {
            assertNotNull(in, chemin + " doit etre livre");
            return ObjMesh.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static void assertVec(Vec3 at, double x, double y, double z) {
        assertEquals(x, at.x, EPSILON, "x de " + at);
        assertEquals(y, at.y, EPSILON, "y de " + at);
        assertEquals(z, at.z, EPSILON, "z de " + at);
    }
}
