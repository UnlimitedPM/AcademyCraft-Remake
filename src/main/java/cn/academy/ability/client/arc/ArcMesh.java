package cn.academy.ability.client.arc;

import java.util.ArrayList;
import java.util.List;

/**
 * Un eclair genere, dans son propre repere.
 *
 * <p>Les coordonnees sont locales : l'arc part de l'origine et s'en va le long de l'axe X,
 * jusqu'a sa portee (vingt blocs, comme les motifs de l'original). C'est le rendu qui pose
 * ce repere entre les deux points vises — l'axe X devient la direction de l'arc.
 *
 * <p>Ce decoupage est ce qui rend la generation verifiable : un eclair est une liste de
 * bouts de ruban avec leur largeur et leur opacite, et c'est tout. Aucun type de Minecraft
 * ici, donc les tests peuvent le lire entierement, mesurer sa portee et le couper.
 */
public record ArcMesh(List<Segment> segments, double extent) {

    /**
     * Un bout d'arc : de son debut a sa fin, avec la largeur du ruban a chaque bout.
     *
     * <p>Les largeurs different parce qu'une branche s'amincit en s'eloignant, et l'opacite
     * aussi : c'est elle qui fait disparaitre les branches les plus profondes, et donc
     * apparaitre la forme de l'eclair plutot qu'un buisson.
     */
    public record Segment(double x0, double y0, double z0, double x1, double y1, double z1,
                          double width0, double width1, double alpha) {}

    public ArcMesh {
        segments = List.copyOf(segments);
    }

    /**
     * L'arc, coupe a cette longueur.
     *
     * <p>L'original ne redimensionnait pas ses motifs : il en existait de vingt blocs, et
     * un arc de dix blocs ne dessinait que les bouts qui tombent dans ces dix blocs. C'est
     * pour cela que {@link ArcPattern} porte une longueur, et que ce n'est pas une echelle.
     */
    public ArcMesh clippedTo(double length) {
        List<Segment> kept = new ArrayList<>(segments.size());
        for (Segment segment : segments) {
            if (segment.x0() > length) continue;
            kept.add(segment);
        }
        return new ArcMesh(kept, extent);
    }

    /** Jusqu'ou ce motif dessine quelque chose, en blocs. */
    public double reach() {
        double reach = 0;
        for (Segment segment : segments) {
            reach = Math.max(reach, Math.max(segment.x0(), segment.x1()));
        }
        return reach;
    }

    public boolean isEmpty() {
        return segments.isEmpty();
    }
}
