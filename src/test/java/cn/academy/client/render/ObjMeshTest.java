package cn.academy.client.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Test
    @DisplayName("les modeles livres ne sont faits que de triangles")
    void lesModelesLivresSontDesTriangles() throws Exception {
        // Le rendu ecrit les faces telles quelles : ses TRIANGLES doivent donc tomber sur un
        // type de rendu qui en dessine (ceux de Minecraft declarent des QUADS, et la carte
        // recollerait alors la fin d'un triangle au debut du suivant). Ce test tient
        // l'invariant de l'autre cote : aucun fichier ne contient de face a quatre sommets,
        // et aucune coordonnee de texture ne sort de l'image.
        for (String nom : List.of("windgen_fan", "windgen_main", "windgen_base", "windgen_pillar", "matrix")) {
            String texte;
            try (var in = getClass().getResourceAsStream("/assets/academy/models/" + nom + ".obj")) {
                assertNotNull(in, nom + " doit etre livre");
                texte = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }

            ObjMesh mesh = ObjMesh.parse(texte);
            assertFalse(mesh.isEmpty(), nom + " doit se lire");
            for (ObjMesh.Face face : mesh.all()) {
                assertEquals(3, face.vertices().size(), nom + " : une face de trois sommets");
                for (ObjMesh.Vertex vertex : face.vertices()) {
                    assertTrue(vertex.u() >= 0f && vertex.u() <= 1f, nom + " : u dans l'image");
                    assertTrue(vertex.v() >= 0f && vertex.v() <= 1f, nom + " : v dans l'image");
                }
            }
        }
    }

    @Test
    @DisplayName("les faces recouvertes passent derriere, et aucune n'est retiree")
    void lesFacesRecouvertesPassentDerriere() {
        // Un modele double : la meme face deux fois, au meme plan. Au meme plan, la
        // profondeur ne departage pas et son infime imprecision change avec l'angle de la
        // camera : c'est le scintillement signale sur le matrix. La premiere passe donc
        // derriere, et rien n'est retire.
        ObjMesh miroir = ObjMesh.parse("""
                v 0 0 0
                v 1 0 0
                v 0 1 0
                g socle
                f 1 2 3
                f 3 2 1
                """);
        assertEquals(1, miroir.behind("socle").size(), "la premiere passe derriere");
        assertEquals(1, miroir.front("socle").size(), "la seconde reste devant");
        assertEquals(2, miroir.group("socle").size(), "et le groupe les garde toutes");

        // Deux triangles qui ne font que se TOUCHER (le pave d'un meme panneau) : aucune des
        // deux ne recouvre l'autre, donc aucune ne passe derriere.
        ObjMesh pave = ObjMesh.parse("""
                v 0 0 0
                v 1 0 0
                v 2 0 0
                v 1 1 0
                f 1 2 4
                f 2 3 4
                """);
        assertTrue(pave.behind("" ).isEmpty(), "un panneau pave ne cache rien");
        assertEquals(2, pave.front("").size());

        // Un vrai recouvrement PARTIEL : le grand panneau passe derriere (il est recouvert),
        // mais il reste dessine — le retirer faisait un trou, et c'est ce qu'on a vu.
        ObjMesh recouvre = ObjMesh.parse("""
                v 0 0 0
                v 4 0 0
                v 0 4 0
                v 1 1 0
                v 2 1 0
                v 1 2 0
                f 1 2 3
                f 4 5 6
                """);
        assertEquals(1, recouvre.behind("").size(), "le grand panneau recule");
        assertEquals(1, recouvre.front("").size(), "le detail reste devant");
        assertEquals(2, recouvre.all().size(), "et les deux sont dessines");
    }

    @Test
    @DisplayName("les modeles livres : ce qui passe derriere, face par face")
    void lesModelesLivresOntUnFondConnu() throws Exception {
        // Les fichiers du mod viennent d'un export qui double chaque paroi. Ce sont ces
        // nombres qui diront si un modele a change : 42 faces derriere dans le matrix, 18
        // dans la base, 8 dans le pilier, 2 dans la nacelle, aucune dans les pales.
        assertEquals(0, derriere("windgen_fan"), "les pales n'ont aucune face recouverte");
        assertEquals(2, derriere("windgen_main"));
        assertEquals(18, derriere("windgen_base"));
        assertEquals(8, derriere("windgen_pillar"));
        assertEquals(42, derriere("matrix"));
    }

    /** Le nombre de faces d'un modele livre qui passent derriere. */
    private static int derriere(String nom) throws Exception {
        return lit(nom).behind().size();
    }

    private static ObjMesh lit(String nom) throws Exception {
        try (var in = ObjMeshTest.class.getResourceAsStream("/assets/academy/models/" + nom + ".obj")) {
            assertNotNull(in, nom + " doit etre livre");
            return ObjMesh.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
