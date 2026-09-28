package cn.academy.ability.client.arc;

import cn.academy.ability.client.arc.ArcMesh.Segment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les eclairs generes, relus sans jeu.
 *
 * <p>C'est un generateur : ce qui compte n'est pas a quoi ressemble un eclair pris un par
 * un, mais que les regles tiennent — il part de son origine, il ne sort pas de sa portee,
 * il se casse davantage a chaque passe, et les cinq motifs sont bien ceux de l'original.
 * Tout cela se mesure.
 */
class ArcGeneratorTest {

    @Test
    @DisplayName("le meme motif redonne toujours le meme eclair")
    void leMemeMotifRedonneLeMemeEclair() {
        ArcMesh first = ArcGenerator.generate(ArcPattern.WEAK, new Random(7));
        ArcMesh second = ArcGenerator.generate(ArcPattern.WEAK, new Random(7));

        assertEquals(first.segments().size(), second.segments().size());
        for (int i = 0; i < first.segments().size(); i++) {
            assertEquals(first.segments().get(i), second.segments().get(i),
                    "l'original tirait ses eclairs a chaque lancement ; ici, une graine les fige");
        }
    }

    @Test
    @DisplayName("deux graines font deux eclairs differents")
    void deuxGrainesFontDeuxEclairs() {
        assertNotEquals(ArcGenerator.generate(ArcPattern.WEAK, new Random(1)).segments(),
                ArcGenerator.generate(ArcPattern.WEAK, new Random(2)).segments(),
                "sinon les vingt variantes d'un motif seraient vingt fois la meme forme");
    }

    @Test
    @DisplayName("l'eclair part de son origine et tient dans sa portee")
    void lEclairPartDeSonOrigine() {
        ArcMesh mesh = ArcGenerator.generate(ArcPattern.CHARGING, new Random(3));

        Segment first = mesh.segments().get(0);
        assertEquals(0.0, first.x0(), 1e-9, "l'arc est pose entre deux points par le rendu");
        assertEquals(0.0, first.y0(), 1e-9);
        assertEquals(0.0, first.z0(), 1e-9);

        for (Segment segment : mesh.segments()) {
            assertTrue(segment.x0() >= 0 && segment.x1() >= 0,
                    "aucun bout ne recule derriere l'origine");
            assertTrue(segment.x0() <= mesh.extent() && segment.x1() <= mesh.extent(),
                    "ni ne depasse la portee du motif");
            assertTrue(segment.alpha() > 0 && segment.alpha() <= 1,
                    "l'opacite part de 1 et ne fait que decroitre en s'enfoncant dans les branches");
            assertTrue(segment.width0() <= ArcPattern.CHARGING.width() + 1e-9,
                    "aucun ruban n'est plus large que le trait du motif");
        }
    }

    @Test
    @DisplayName("le deplacement s'attenue a chaque passe : le trait se casse de plus en plus fin")
    void leDeplacementSAttenue() {
        // Le deplacement maximal est divise par deux a chaque passe, donc la somme des
        // ecarts lateraux reste bornee : deux fois le deplacement initial, au pire.
        ArcMesh mesh = ArcGenerator.generate(ArcPattern.STRONG, new Random(4));
        double bound = 2 * ArcPattern.STRONG.maxOffset();

        for (Segment segment : mesh.segments()) {
            assertTrue(Math.abs(segment.y0()) < bound && Math.abs(segment.z0()) < bound,
                    "l'eclair s'ecarte du plan de depart, mais pas plus que ses passes ne l'y autorisent");
        }
    }

    @Test
    @DisplayName("plus de passes, un trait plus casse")
    void plusDePassesUnTraitPlusCasse() {
        ArcPattern one = new ArcPattern("essai", 0.1, 0.7, 0.9, 1.1, 0.15, 0.7, 1, 20, 5L);
        ArcPattern six = new ArcPattern("essai", 0.1, 0.7, 0.9, 1.1, 0.15, 0.7, 6, 20, 5L);

        assertTrue(ArcGenerator.generate(six, new Random(5)).segments().size()
                        > ArcGenerator.generate(one, new Random(5)).segments().size(),
                "c'est la passe qui casse le trait, et rien d'autre");
    }

    @Test
    @DisplayName("un arc coupe s'arrete a la longueur demandee")
    void unArcCoupeSArreteALaLongueur() {
        ArcMesh full = ArcGenerator.generate(ArcPattern.WEAK, new Random(6));
        ArcMesh half = full.clippedTo(10);

        assertFalse(half.isEmpty());
        assertTrue(half.segments().size() < full.segments().size(), "il en reste moins");
        for (Segment segment : half.segments()) {
            assertTrue(segment.x0() <= 10, "aucun bout garde ne commence au-dela");
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
        assertNotEquals(ArcPatterns.variant(ArcPattern.WEAK, 0).segments(),
                ArcPatterns.variant(ArcPattern.WEAK, 1).segments(),
                "le scintillement n'existe que si les variantes different");

        // Une variante hors bornes revient dans les bornes plutot que de lever : c'est le
        // tirage au sort qui decide, et il ne doit jamais faire tomber le rendu.
        assertEquals(ArcPatterns.variant(ArcPattern.WEAK, 0).segments(),
                ArcPatterns.variant(ArcPattern.WEAK, ArcPattern.VARIANTS).segments());
        assertEquals(20, ArcPatterns.variants());
    }
}
