package cn.academy.ability.client.arc;

import cn.academy.ability.client.arc.ArcMesh.Quad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les eclairs generes, relus sans jeu.
 *
 * <p>C'est un generateur : ce qui compte n'est pas a quoi ressemble un eclair pris un par
 * un, mais que les regles tiennent — il part de son origine, il ne sort pas de sa portee, et
 * surtout deux rubans voisins PARTAGENT leur bord. C'est cette derniere regle qui repare
 * l'eclair qui semblait parseme de morceaux plus clairs que d'autres : deux surfaces
 * translucides qui se chevauchent se melangent deux fois.
 */
class ArcGeneratorTest {

    @Test
    @DisplayName("le meme motif redonne toujours le meme eclair")
    void leMemeMotifRedonneLeMemeEclair() {
        ArcMesh first = ArcGenerator.generate(ArcPattern.WEAK, new Random(7));
        ArcMesh second = ArcGenerator.generate(ArcPattern.WEAK, new Random(7));

        assertEquals(first.quads(), second.quads(),
                "l'original tirait ses eclairs a chaque lancement ; ici, une graine les fige");
    }

    @Test
    @DisplayName("deux graines font deux eclairs differents")
    void deuxGrainesFontDeuxEclairs() {
        assertNotEquals(ArcGenerator.generate(ArcPattern.WEAK, new Random(1)).quads(),
                ArcGenerator.generate(ArcPattern.WEAK, new Random(2)).quads(),
                "sinon les vingt variantes d'un motif seraient vingt fois la meme forme");
    }

    @Test
    @DisplayName("les rubans d'une ligne se partagent leurs bords, sans se chevaucher")
    void lesRubansSePartagentLeursBords() {
        ArcMesh mesh = ArcGenerator.generate(ArcPattern.WEAK, new Random(11));
        int chained = 0;

        // Le bord d'arrivee d'un quad est le bord de depart du suivant : le coin « d »
        // devient le coin « a », et « c » devient « b ». C'est le chainage de lastDir dans
        // l'original, et c'est lui qui evite le recouvrement.
        for (int i = 1; i < mesh.quads().size(); i++) {
            Quad before = mesh.quads().get(i - 1);
            Quad after = mesh.quads().get(i);

            // Les lignes se suivent dans la liste : on ne compare que ce qui se touche. Le
            // bord d'arrivee porte l'abscisse du point qui le porte, ses deux coins etant
            // decales lateralement, donc c'est leur milieu qui la donne.
            if (Math.abs(after.startX() - (before.cx() + before.dx()) / 2) > 1e-6) continue;

            assertEquals(before.dx(), after.ax(), 1e-6, "bord partage : d devient a");
            assertEquals(before.dy(), after.ay(), 1e-6);
            assertEquals(before.dz(), after.az(), 1e-6);
            assertEquals(before.cx(), after.bx(), 1e-6, "bord partage : c devient b");
            assertEquals(before.cy(), after.by(), 1e-6);
            assertEquals(before.cz(), after.bz(), 1e-6);
            chained++;
        }
        assertTrue(chained > 20, "les lignes d'un eclair sont longues : " + chained + " bords partages");
    }

    @Test
    @DisplayName("l'eclair part de son origine et tient dans sa portee")
    void lEclairPartDeSonOrigine() {
        ArcMesh mesh = ArcGenerator.generate(ArcPattern.CHARGING, new Random(3));
        Quad first = mesh.quads().get(0);

        assertEquals(0.0, first.startX(), 1e-9, "l'arc est pose entre deux points par le rendu");
        // Les coins sont decales lateralement, donc ce n'est pas leur position qui est sur
        // l'origine mais le milieu de leur bord — le trait, lui, part bien de zero.
        assertEquals(0.0, (first.ax() + first.bx()) / 2, 1e-9);

        double bound = 2 * ArcPattern.CHARGING.maxOffset() + ArcPattern.CHARGING.width();
        for (Quad quad : mesh.quads()) {
            assertTrue(quad.alpha() > 0 && quad.alpha() <= 1,
                    "l'opacite part de 1 et ne fait que decroitre en s'enfoncant dans les branches");
            for (double corner : new double[] { (quad.ax() + quad.bx()) / 2, (quad.cx() + quad.dx()) / 2 }) {
                assertTrue(corner >= 0, "le trait ne recule pas derriere l'origine");
                assertTrue(corner <= mesh.extent(), "ni ne depasse la portee du motif");
            }
            // La largeur du ruban s'ajoute aux ecarts lateraux du motif.
            assertTrue(Math.abs(quad.ay()) < bound && Math.abs(quad.az()) < bound,
                    "l'eclair s'ecarte du plan de depart, mais pas plus que ses passes ne l'y autorisent");
        }
    }

    @Test
    @DisplayName("aucun ruban n'est plat, et tous suivent le sens de la ligne")
    void aucunRubanNestPlat() {
        ArcMesh mesh = ArcGenerator.generate(ArcPattern.STRONG, new Random(4));

        for (Quad quad : mesh.quads()) {
            // L'aire du quad, par ses deux diagonales : elle ne doit jamais etre nulle. Un
            // ruban plat serait un ruban qu'on ne voit pas, et il viendrait d'une largeur
            // perdue ou de deux coins confondus.
            double[] diagonalA = { quad.cx() - quad.ax(), quad.cy() - quad.ay(), quad.cz() - quad.az() };
            double[] diagonalB = { quad.dx() - quad.bx(), quad.dy() - quad.by(), quad.dz() - quad.bz() };
            double[] area = {
                    diagonalA[1] * diagonalB[2] - diagonalA[2] * diagonalB[1],
                    diagonalA[2] * diagonalB[0] - diagonalA[0] * diagonalB[2],
                    diagonalA[0] * diagonalB[1] - diagonalA[1] * diagonalB[0] };
            double size = Math.sqrt(area[0] * area[0] + area[1] * area[1] + area[2] * area[2]);
            assertTrue(size > 1e-4, "aire nulle : le ruban serait invisible");

            // Et il avance : l'abscisse de son bord d'arrivee est au-dela du depart.
            assertTrue((quad.cx() + quad.dx()) / 2 >= (quad.ax() + quad.bx()) / 2 - 1e-9,
                    "les rubans d'une ligne se suivent dans le sens de l'arc");
        }
    }

    @Test
    @DisplayName("plus de passes, un trait plus casse")
    void plusDePassesUnTraitPlusCasse() {
        ArcPattern one = new ArcPattern("essai", 0.1, 0.7, 0.9, 1.1, 0.15, 0.7, 1, 20, 0.5, 0.2, 0.2, 5L);
        ArcPattern six = new ArcPattern("essai", 0.1, 0.7, 0.9, 1.1, 0.15, 0.7, 6, 20, 0.5, 0.2, 0.2, 5L);

        assertTrue(ArcGenerator.generate(six, new Random(5)).quads().size()
                        > ArcGenerator.generate(one, new Random(5)).quads().size(),
                "c'est la passe qui casse le trait, et rien d'autre");
    }

    @Test
    @DisplayName("un arc coupe s'arrete a la longueur demandee")
    void unArcCoupeSArreteALaLongueur() {
        ArcMesh full = ArcGenerator.generate(ArcPattern.WEAK, new Random(6));
        ArcMesh half = full.clippedTo(10);

        assertFalse(half.isEmpty());
        assertTrue(half.quads().size() < full.quads().size(), "il en reste moins");
        for (Quad quad : half.quads()) {
            assertTrue(quad.startX() <= 10, "aucun ruban garde ne commence au-dela");
        }
        assertEquals(full.extent(), half.extent(), "la portee du motif ne change pas");
    }

    @Test
    @DisplayName("les cinq motifs sont ceux de l'original")
    void lesCinqMotifsSontCeuxDeLOriginal() {
        // Ces nombres viennent d'ArcFactory et d'ArcPatterns, un par un : le faible et le
        // continu different par l'epaisseur, le fort par la sienne et par son ecart, et
        // trois motifs sur cinq ont cinq passes.
        assertEquals(0.1, ArcPattern.WEAK.width(), 1e-9);
        assertEquals(0.15, ArcPattern.WEAK.branchFactor(), 1e-9);
        assertEquals(6, ArcPattern.WEAK.passes());

        assertEquals(0.08, ArcPattern.THIN_CONTINUOUS.width(), 1e-9);
        assertEquals(0.2, ArcPattern.THIN_CONTINUOUS.branchFactor(), 1e-9);

        assertEquals(0.1, ArcPattern.CHARGING.width(), 1e-9);
        assertEquals(0.3, ArcPattern.CHARGING.branchFactor(), 1e-9);

        assertEquals(0.3, ArcPattern.STRONG.width(), 1e-9);
        assertEquals(1.4, ArcPattern.STRONG.maxOffset(), 1e-9);

        assertEquals(0.13, ArcPattern.AOE.width(), 1e-9);
        assertEquals(0.28, ArcPattern.AOE.branchFactor(), 1e-9);

        // Tous les motifs font vingt blocs et vingt variantes, comme les generateList de
        // l'original, et l'amincissement comme l'affaiblissement sont les memes partout.
        for (ArcPattern pattern : ArcPattern.all()) {
            assertEquals(20.0, pattern.length(), 1e-9);
            assertEquals(0.7, pattern.lengthShrink(), 1e-9);
            assertEquals(0.9, pattern.alphaShrink(), 1e-9);
            assertEquals(0.7, pattern.widthShrink(), 1e-9);
        }
        assertEquals(20, ArcPattern.VARIANTS);

        assertEquals(ArcPattern.STRONG, ArcPattern.byName("strong"));
        assertEquals(ArcPattern.WEAK, ArcPattern.byName("n'importe quoi"),
                "un nom inconnu ne doit pas laisser l'arc sans motif");
    }

    @Test
    @DisplayName("les vingt variantes d'un motif sont vingt eclairs")
    void lesVingtVariantesSontVingtEclairs() {
        List<Quad> first = ArcPatterns.variant(ArcPattern.WEAK, 0).quads();
        List<Quad> second = ArcPatterns.variant(ArcPattern.WEAK, 1).quads();

        assertNotEquals(first, second, "le scintillement n'existe que si les variantes different");

        // Une variante hors bornes revient dans les bornes plutot que de lever : c'est le
        // tirage au sort qui decide, et il ne doit jamais faire tomber le rendu.
        assertEquals(first, ArcPatterns.variant(ArcPattern.WEAK, ArcPattern.VARIANTS).quads());
        assertEquals(20, ArcPatterns.variants());
    }
}
