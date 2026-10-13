package cn.academy.ability.meltdowner;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le disque de colonnes que le faisceau creuse.
 *
 * <p>C'est lui qui fait la <b>forme du trou</b> : l'original ne tracait pas une ligne, il balayait un
 * disque perpendiculaire au tir, et creusait une colonne par point de ce disque. Une erreur ici ne se
 * verrait qu'en jeu, un mur troue de travers — et il faudrait creuser un couloir pour s'en apercevoir.
 *
 * <p>La classe est pure, donc verifiable en JUnit : c'est tout l'interet de l'avoir sortie du skill.
 * Le hasard est fige par une graine, sinon la fluctuation du bord rendrait tout test instable.
 */
class BeamBlocksTest {

    /** Le rayon du railgun, et celui du meltdowner au sommet : les deux bornes reelles. */
    private static final double PETIT = 2.0;
    private static final double GRAND = 3.0;

    @Test
    @DisplayName("tout point du disque est dans le rayon, et le centre y est toujours")
    void leDisqueTientDansSonRayon() {
        for (double radius : new double[] {PETIT, GRAND}) {
            var offsets = BeamBlocks.offsets(radius, new Random(1));

            assertTrue(!offsets.isEmpty(), "un rayon de " + radius + " doit donner des colonnes");

            boolean centre = false;
            for (double[] offset : offsets) {
                double distance = Math.hypot(offset[0], offset[1]);
                // La fluctuation ne fait que RETRECIR le disque point par point ({@code ranged(0.9,
                // 1.1)}) : aucun point ne peut donc sortir du plus grand rayon possible.
                assertTrue(distance <= radius * BeamBlocks.WOBBLE_MAX + 1e-9,
                        "le point (" + offset[0] + ", " + offset[1] + ") sort du rayon " + radius);
                if (offset[0] == 0.0 && offset[1] == 0.0) centre = true;
            }
            assertTrue(centre, "le centre est toujours creuse : le faisceau y passe");
        }
    }

    @Test
    @DisplayName("le disque est PLEIN : tout point du noyau y est, et les pas tiennent sur la grille")
    void leDisqueEstPlein() {
        var offsets = BeamBlocks.offsets(GRAND, new Random(7));
        var presents = new HashSet<String>();
        for (double[] offset : offsets) {
            // Le balayage se fait de demi-bloc en demi-bloc, comme l'original : c'est ce qui fait
            // que les colonnes tombent sur les blocs sans en oublier un seul entre deux.
            assertEquals(0.0, Math.abs(offset[0]) % BeamBlocks.STEP, 1e-9, "la largeur suit le pas");
            assertEquals(0.0, Math.abs(offset[1]) % BeamBlocks.STEP, 1e-9, "la hauteur aussi");
            assertTrue(presents.add(offset[0] + ";" + offset[1]),
                    "le point (" + offset[0] + ", " + offset[1] + ") est visite deux fois");
        }

        // Et l'interieur est PLEIN, pas seulement le bord : la fluctuation la plus faible garde
        // encore ce noyau-la, donc aucun de ses points ne peut manquer. La grille se parcourt par
        // ENTIERS fois le pas, comme le fait le generateur : -2.2 + 0.5 n'est pas -1.7 en binaire,
        // et une accumulation flottante raterait les cles d'un chouia.
        int pas = (int) Math.floor((GRAND * BeamBlocks.WOBBLE_MIN - BeamBlocks.STEP)
                / BeamBlocks.STEP);
        for (int i = -pas; i <= pas; i++) {
            for (int j = -pas; j <= pas; j++) {
                double width = i * BeamBlocks.STEP;
                double height = j * BeamBlocks.STEP;
                assertTrue(presents.contains(width + ";" + height),
                        "le point (" + width + ", " + height + ") manque au disque");
            }
        }
    }

    @Test
    @DisplayName("un faisceau plus large fait plus de colonnes, et la graine decide du bord")
    void leRayonFaitLeNombreDeColonnes() {
        List<double[]> petit = BeamBlocks.offsets(PETIT, new Random(3));
        List<double[]> grand = BeamBlocks.offsets(GRAND, new Random(3));

        // C'est le mecanisme de l'original : l'energie du tir est partagee entre les colonnes, donc
        // un faisceau plus large creuse MOINS profond. Sans colonnes en plus, ce partage serait faux.
        assertTrue(grand.size() > petit.size(),
                "trois de rayon donnent plus de colonnes que deux : " + grand.size() + " contre "
                        + petit.size());

        // La graine fige le bord : deux appels identiques rendent le meme disque, ce qui est ce qui
        // rend ce test possible.
        assertEquals(grand.size(), BeamBlocks.offsets(GRAND, new Random(3)).size(),
                "la meme graine rend le meme nombre de colonnes");
        Set<String> autres = new HashSet<>();
        for (double[] offset : BeamBlocks.offsets(GRAND, new Random(4))) {
            autres.add(offset[0] + ";" + offset[1]);
        }
        Set<String> memes = new HashSet<>();
        for (double[] offset : grand) {
            memes.add(offset[0] + ";" + offset[1]);
        }
        assertTrue(!memes.equals(autres),
                "une autre graine creuse un bord different : c'est ce qui rend le trou irregulier");
    }
}
