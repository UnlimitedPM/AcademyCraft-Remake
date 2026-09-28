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
 * <p>Un eclair est une suite de <b>quads</b>, et non une suite de segments. Ce n'est pas un
 * detail : l'original calculait, pour chaque segment, deux coins au depart et deux a
 * l'arrivee, et le bord de depart reprenait la direction du segment PRECEDENT — les quads
 * se partagent donc leurs bords exactement, sans recouvrement. Calculer chaque segment pour
 * soi, comme je l'avais fait, les fait se croiser : deux surfaces translucides superposees
 * se melangent deux fois, et l'eclair se retrouve parseme de morceaux plus clairs que
 * d'autres.
 *
 * <p>Les quatre coins sont deja dans l'ordre de la texture — {@code (0,0)}, {@code (0,1)},
 * {@code (1,1)}, {@code (1,0)} — donc le rendu n'a aucune decision a prendre : il deplace
 * des points, et c'est tout. Toute la forme se relit ainsi en test : deux quads voisins
 * partagent leurs bords, et l'opacite ne remonte jamais.
 */
public record ArcMesh(List<Quad> quads, double extent) {

    /**
     * Un ruban du motif : quatre coins dans l'ordre de la texture, et son opacite.
     *
     * <p>{@code startX} est l'abscisse de son bord de depart, le long de l'arc : c'est elle
     * qui dit si le quad tombe dans la longueur demandee quand l'arc est coupe.
     */
    public record Quad(double startX, double alpha,
                       double ax, double ay, double az,
                       double bx, double by, double bz,
                       double cx, double cy, double cz,
                       double dx, double dy, double dz) {}

    public ArcMesh {
        quads = List.copyOf(quads);
    }

    /**
     * L'arc, coupe a cette longueur.
     *
     * <p>L'original ne redimensionnait pas ses motifs : il en existait de vingt blocs, et
     * un arc de dix blocs ne dessinait que les quads qui tombent dans ces dix blocs. C'est
     * pour cela que {@link ArcPattern} porte une longueur, et que ce n'est pas une echelle.
     */
    public ArcMesh clippedTo(double length) {
        List<Quad> kept = new ArrayList<>(quads.size());
        for (Quad quad : quads) {
            if (quad.startX() <= length) kept.add(quad);
        }
        return new ArcMesh(kept, extent);
    }

    /** Jusqu'ou ce motif dessine quelque chose, en blocs. */
    public double reach() {
        double reach = 0;
        for (Quad quad : quads) {
            reach = Math.max(reach, Math.max(Math.max(quad.ax(), quad.bx()),
                    Math.max(quad.cx(), quad.dx())));
        }
        return reach;
    }

    public boolean isEmpty() {
        return quads.isEmpty();
    }
}
