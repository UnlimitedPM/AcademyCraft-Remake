package cn.academy.client.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le lecteur d'OBJ, relu sans Minecraft.
 *
 * <p>Il lit les fichiers du mod pour en redessiner des morceaux qui bougent : les pales de
 * l'eolienne, les plaques du matrix. Un quad mal decoupe ou un indice mal resolu se verrait
 * comme un trou dans le modele, en jeu seulement — d'ou ces quelques verifications.
 */
class ObjMeshTest {

    /** Un carre, un triangle qui se sert des memes sommets, et une normale. */
    private static final String MODELE = """
            # un petit modele de test
            v 0 0 0
            v 1 0 0
            v 1 0 1
            v 0 0 1
            vt 0 0
            vt 1 0
            vt 1 1
            vt 0 1
            vn 0 1 0
            mtllib rien.mtl
            g plat
            usemtl initialShadingGroup
            f 1/1/1 2/2/1 3/3/1 4/4/1
            g penche
            f -4/-4/-1 -3/-3/-1 -2/-2/-1
            """;

    @Test
    @DisplayName("un quad devient deux triangles")
    void unQuadDevientDeuxTriangles() {
        ObjMesh mesh = ObjMesh.parse(MODELE);
        List<ObjMesh.Face> faces = mesh.group("plat");

        assertEquals(2, faces.size(), "un quad, deux triangles");
        for (ObjMesh.Face face : faces) {
            assertEquals(3, face.vertices().size(), "et chaque triangle a trois sommets");
        }
        // Le premier triangle garde l'ordre du fichier.
        assertEquals(0f, faces.get(0).vertices().get(0).x(), 0.001f);
        assertEquals(1f, faces.get(0).vertices().get(1).x(), 0.001f);
        assertEquals(1f, faces.get(0).vertices().get(2).x(), 0.001f);
    }

    @Test
    @DisplayName("les indices negatifs comptent depuis la fin")
    void lesIndicesNegatifsComptentDepuisLaFin() {
        ObjMesh mesh = ObjMesh.parse(MODELE);
        List<ObjMesh.Face> faces = mesh.group("penche");

        assertEquals(1, faces.size(), "un triangle reste un triangle");
        // -4 depuis quatre sommets, c'est le premier.
        assertEquals(0f, faces.get(0).vertices().get(0).x(), 0.001f);
        assertEquals(0f, faces.get(0).vertices().get(0).z(), 0.001f);
        assertEquals(1f, faces.get(0).vertices().get(2).x(), 0.001f);
        assertEquals(1f, faces.get(0).vertices().get(2).z(), 0.001f);
    }

    @Test
    @DisplayName("les coordonnees de texture suivent leur sommet, ou valent zero")
    void lesCoordonneesDeTextureSuiventLeurSommet() {
        ObjMesh mesh = ObjMesh.parse(MODELE);
        ObjMesh.Vertex premier = mesh.group("plat").get(0).vertices().get(0);
        assertEquals(0f, premier.u(), 0.001f);
        assertEquals(0f, premier.v(), 0.001f);
        assertEquals(1f, mesh.group("plat").get(0).vertices().get(2).u(), 0.001f);

        // Le triangle sans vt ni vn : u et v a zero, et une normale deduite de la face.
        ObjMesh.Vertex nu = mesh.group("penche").get(0).vertices().get(0);
        assertEquals(0f, nu.u(), 0.001f);
        assertEquals(1f, Math.abs(nu.ny()), 0.001f, "une normale plate, vers le haut");
    }

    @Test
    @DisplayName("les groupes se lisent separement, et le modele peut etre vide")
    void lesGroupesSeLisentSeparement() {
        ObjMesh mesh = ObjMesh.parse(MODELE);
        assertEquals(3, mesh.all().size(), "deux triangles et un");
        assertTrue(mesh.group("inconnu").isEmpty(), "un groupe qui n'existe pas ne rend rien");
        assertFalse(mesh.isEmpty());

        assertTrue(ObjMesh.empty().isEmpty());
        assertTrue(ObjMesh.empty().all().isEmpty());
    }

    @Test
    @DisplayName("un fichier abime ne fait pas tomber le rendu")
    void unFichierAbimeResteLisible() {
        ObjMesh mesh = ObjMesh.parse("""
                v 0 0 0
                v 1 0 0
                f 1 2 xx
                f 9 10 11
                g
                f 1
                """);

        // La face sans trois sommets valides, et celle qui vise des sommets absents, sont
        // simplement laissees de cote : mieux vaut un modele incomplet qu'un plantage.
        assertEquals(0, mesh.all().size());
    }
}
