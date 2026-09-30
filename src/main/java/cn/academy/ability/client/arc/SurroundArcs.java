package cn.academy.ability.client.arc;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Les arcs d'entourage : un gresillement d'eclairs courts autour d'une chose.
 *
 * <p>Portage d'{@code EntitySurroundArc}. L'original ne reliait pas deux points, ici : il semait
 * quelques arcs dans une boite, autour du joueur qui charge une machine ou qui s'electrise,
 * autour du bloc que la manipulation magnetique tient en l'air. Chacun vivait trois ticks, et
 * l'essaim entier se reensemencait quand le dernier s'eteignait — d'ou le gresillement continu.
 *
 * <p>Trois gabarits, ceux de l'original, du plus fin au plus gras. Son {@code ArcType} portait
 * le nombre d'arcs vivants a la fois, et ses gabarits etaient fabriques avec une longueur tiree
 * entre deux bornes : ces trois nombres sont ici, dans {@link Gabarit}.
 *
 * <p>C'est la piece partagee de l'electromaster : la charge, le claquement d'orage et
 * l'intensification du corps s'en servent tous les trois, comme la manipulation magnetique.
 */
public final class SurroundArcs {

    /** Un gabarit : son motif, le nombre d'arcs vivants, et la longueur tiree de chacun. */
    public record Gabarit(ArcPattern pattern, int count, double minLength, double maxLength) {}

    // LES TROIS PORTEES CI-DESSOUS SONT CELLES DE L'ORIGINAL.
    //
    // Elles ont ete rallongees un moment, a 2 - 6, 4 - 8 et 6 - 12, puis remises a l'original
    // a la demande du joueur : un essaim de cinq blocs autour d'un bloc d'un metre, cela ne
    // ressemblait plus a l'original. Et c'est la borne qui fait la taille, parce que l'essaim
    // passe lengthFixed = faux : l'arc se dessine alors jusqu'au bout vise, donc de sa longueur
    // tiree, et non de celle de son motif. Voir ClientArcs.spawn.

    /** Fin : quatre arcs, longs de 1,5 a 2 blocs. C'est le corps qui s'electrise. */
    public static final Gabarit THIN = new Gabarit(ArcPattern.SURROUND_THIN, 4, 1.5, 2.0);

    /** Moyen : six arcs, longs de 3 a 4 blocs. */
    public static final Gabarit NORMAL = new Gabarit(ArcPattern.SURROUND_NORMAL, 6, 3.0, 4.0);

    /** Gras : cinq arcs, longs de 3,5 a 4,5 blocs. Le claquement d'orage. */
    public static final Gabarit BOLD = new Gabarit(ArcPattern.SURROUND_BOLD, 5, 3.5, 4.5);

    /**
     * L'orage du claquement : quatre arcs courts et minces, serres contre le corps.
     *
     * <p>ECART ASSUME, demande du joueur : le gras ci-dessus, qui est celui de l'original,
     * lui a paru beaucoup trop grand et beaucoup trop gros autour de lui. Les bornes de
     * longueur n'ont d'effet que sous la longueur du motif, qui est donc la vraie taille de
     * l'arc — 1,2 bloc ici.
     */
    public static final Gabarit CLAP = new Gabarit(ArcPattern.SURROUND_FINE, 4, 0.35, 0.6);

    /** La vie d'un arc d'entourage, en ticks : trois, comme {@code EntityIntensifyEffect}. */
    public static final int LIFE_TICKS = 3;

    private SurroundArcs() {
    }

    /**
     * Le cote de la boite ou semer, pour qu'un arc ne sorte d'un cube que de la moitie de lui-meme.
     *
     * <p>Un arc d'entourage nait a un point de la boite et file dans une direction tiree : pose
     * au bord, il en sort donc de toute sa longueur. L'original ne s'en souciait pas — ses arcs
     * etaient tailles pour un corps, ou depasser se voit peu.
     *
     * <p>Le point de depart recule donc d'une longueur d'arc, et pas de deux : un arc peut
     * encore depasser de la moitie de sa taille, ce qui se voit — le joueur a demande "un peu"
     * de debordement — sans que l'eclair aille se planter dans le bloc d'a cote.
     *
     * <p>Quand la boite est plus petite que cela — les gros gabarits dans un bloc — il ne reste
     * rien a retrecir, et la fonction rend zero : l'appelant garde alors son point au centre, et
     * laisse les arcs depasser, comme l'original.
     */
    public static double inset(double cubeSize, Gabarit gabarit) {
        return Math.max(0.0, cubeSize - gabarit.maxLength());
    }

    /**
     * Seme un essaim autour d'un point.
     *
     * <p>A appeler tous les {@link #LIFE_TICKS} ticks pour que le gresillement ne s'arrete pas :
     * chaque appel pose des arcs qui vivent trois ticks, et le suivant prend la releve.
     *
     * @param sizeXZ l'etendue de la boite en largeur, centree sur {@code centre}
     * @param minY   la hauteur du bas de la boite, par rapport a {@code centre}
     * @param maxY   la hauteur de son haut
     */
    public static void spawn(Gabarit gabarit, Vec3 centre, double sizeXZ, double minY, double maxY,
                             int ownerId, RandomSource random) {
        spawnAt(gabarit, spread(centre, sizeXZ, minY, maxY, gabarit.count(), random), ownerId,
                random);
    }

    /**
     * Seme un essaim a des points DONNES plutot que dans une boite.
     *
     * <p>L'intensification du corps s'en sert : ses arcs ne sont pas tires dans un volume mais
     * poses sur un ANNEAU, a une hauteur fixe — le {@code phi} de 0,5 a 0,6 de l'original est un
     * rayon, pas une demi-largeur. Voir {@code BodyIntensifyEffect}.
     *
     * <p>La longueur de chaque arc est tiree dans les bornes du gabarit, et la direction sur la
     * sphere : c'est ce qui donne le gresillement, l'original ayant ses arcs tout faits.
     */
    public static void spawnAt(Gabarit gabarit, List<Vec3> points, int ownerId,
                               RandomSource random) {
        for (Vec3 point : points) {
            double length = gabarit.minLength()
                    + random.nextDouble() * (gabarit.maxLength() - gabarit.minLength());
            ArcRenderer.spawn(gabarit.pattern().name(), point,
                    point.add(randomDirection(random).scale(length)),
                    LIFE_TICKS, false, ownerId);
        }
    }

    /**
     * Les points d'un essaim, tires dans une boite.
     *
     * <p>L'original tirait ses points d'un {@code CubePointFactory} : une boite aux dimensions de
     * la chose entouree, multipliees par 1,3. La hauteur va du bas au haut de la chose, et la
     * largeur se repartit de part et d'autre de son centre — ce sont les trois nombres que
     * l'appelant donne ici.
     */
    public static List<Vec3> spread(Vec3 centre, double sizeXZ, double minY, double maxY,
                                    int count, RandomSource random) {
        List<Vec3> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            points.add(new Vec3(
                    centre.x + (random.nextDouble() - 0.5) * sizeXZ,
                    centre.y + minY + random.nextDouble() * (maxY - minY),
                    centre.z + (random.nextDouble() - 0.5) * sizeXZ));
        }
        return points;
    }

    /**
     * Une direction tiree sur la sphere, comme le faisait {@code doGenerate}.
     *
     * <p>L'original calculait ces angles puis ne s'en servait pas : son arc venait tout fait de
     * son gabarit, pose a l'endroit tire. Le port n'a pas de gabarits tout faits — ses motifs se
     * generent — donc c'est la direction qui varie d'un arc a l'autre, et le gresillement vient
     * de la.
     */
    private static Vec3 randomDirection(RandomSource random) {
        double yaw = random.nextDouble() * Math.PI * 2;
        double pitch = random.nextDouble() * Math.PI;
        double y = Math.sin(pitch);
        double ring = Math.sqrt(1 - y * y);
        return new Vec3(ring * Math.sin(yaw), y, ring * Math.cos(yaw));
    }
}
