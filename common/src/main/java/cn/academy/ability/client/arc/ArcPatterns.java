package cn.academy.ability.client.arc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Les vingt eclairs de chaque motif, tires une fois pour toutes.
 *
 * <p>L'original faisait exactement cela : vingt arcs par motif, generes au chargement du
 * client, puis un tirage au hasard toutes les quelques dixiemes de seconde pour choisir
 * lequel dessiner. C'est ce tirage qui fait scintiller l'eclair — un arc fige a l'air d'un
 * fil de fer.
 *
 * <p>Les tirer « une fois pour toutes » n'est pas qu'une economie : genere a chaque
 * scintillement, l'eclair serait un brouillard qui change entierement de forme dix fois par
 * seconde, au lieu d'un eclair qui vacille entre vingt formes parentes.
 */
public final class ArcPatterns {

    private static final Map<ArcPattern, List<ArcMesh>> VARIANTS = new HashMap<>();

    private ArcPatterns() {}

    /** La variante numero {@code index} d'un motif : l'appelant la choisit au hasard. */
    public static ArcMesh variant(ArcPattern pattern, int index) {
        List<ArcMesh> variants = VARIANTS.computeIfAbsent(pattern, ArcPatterns::generate);
        return variants.get(Math.floorMod(index, variants.size()));
    }

    /** Combien de variantes un motif a, pour les tirer au sort. */
    public static int variants() {
        return ArcPattern.VARIANTS;
    }

    private static List<ArcMesh> generate(ArcPattern pattern) {
        List<ArcMesh> variants = new ArrayList<>(ArcPattern.VARIANTS);
        for (int i = 0; i < ArcPattern.VARIANTS; i++) {
            // Une graine par variante : les vingt eclairs sont bien vingt eclairs
            // differents, mais les memes a chaque lancement du jeu.
            variants.add(ArcGenerator.generate(pattern, new Random(pattern.seed() * 1_000L + i)));
        }
        return variants;
    }
}
