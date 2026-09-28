package cn.academy.ability.client.arc;

import cn.academy.ability.client.arc.ArcMesh.Segment;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * La generation des eclairs, portage de {@code ArcFactory.generate}.
 *
 * <p>Le principe, tel que l'original l'ecrivait : on part d'un segment droit de la portee du
 * motif ; a chaque passe, chaque segment est coupe en deux et son milieu est deplace au
 * hasard dans le plan perpendiculaire (le plan YZ, l'arc allant le long de X) ; le
 * deplacement maximal est divise par deux a chaque passe, donc le trait se casse de plus en
 * plus fin ; et de temps en temps, une branche part du milieu dans la direction du premier
 * morceau, raccourcie et deviee.
 *
 * <p>Deux differences avec l'original, et elles sont voulues :
 * <ul>
 *   <li>le hasard est <b>amorce</b>. L'original tirait ses motifs a chaque lancement (et
 *       meme a chaque image pour les arcs non figes), donc rien de tout cela n'etait
 *       reproductible ni testable. Ici, une graine donne toujours le meme eclair ;</li>
 *   <li>l'orientation des rubans n'est pas tiree au hasard ici : c'est le rendu qui fait
 *       toujours face a la camera. L'original retournait chaque ruban a chaque image, ce
 *       qui participait a son scintillement — mais un ruban qu'on regarde de profil
 *       disparait, et c'est justement ce qu'on ne veut pas.</li>
 * </ul>
 */
public final class ArcGenerator {

    /** L'angle maximal dont une branche est deviee, en degres. */
    private static final double BRANCH_DEVIATION = 10;

    private ArcGenerator() {}

    /** Un motif d'eclair, avec la graine donnee. */
    public static ArcMesh generate(ArcPattern pattern, Random rng) {
        double width = pattern.width();
        double length = pattern.length();

        // La ligne de depart : un seul segment, de l'origine jusqu'a la portee du motif.
        List<List<Segment>> current = new ArrayList<>();
        current.add(List.of(new Segment(0, 0, 0, length, 0, 0, width, width, 1.0)));

        double offset = pattern.maxOffset();
        for (int pass = 0; pass < pattern.passes(); pass++) {
            List<List<Segment>> next = new ArrayList<>();
            for (List<Segment> line : current) {
                List<Segment> built = new ArrayList<>(line.size() * 2);
                List<List<Segment>> branches = new ArrayList<>();
                for (Segment segment : line) {
                    Segment first = half(segment, offset, rng, built);
                    if (rng.nextDouble() < pattern.branchFactor()) {
                        // Une branche part du milieu : elle est rangee avec les autres
                        // lignes, et sera coupee a la passe suivante comme le reste. Elle
                        // passe apres la ligne dont elle est nee, pour que la premiere
                        // ligne du motif reste celle qui part de l'origine.
                        branches.add(branch(first, segment, pattern, rng));
                    }
                }
                next.add(built);
                next.addAll(branches);
            }
            current = next;
            offset /= 2;
        }

        List<Segment> all = new ArrayList<>();
        for (List<Segment> line : current) {
            all.addAll(line);
        }
        return new ArcMesh(all, length);
    }

    /**
     * Coupe un segment en deux, en deplacant son milieu, et range les deux morceaux.
     *
     * <p>Rend le premier morceau, dont la direction sert a celle d'une eventuelle branche.
     */
    private static Segment half(Segment segment, double offset, Random rng, List<Segment> out) {
        double midX = (segment.x0() + segment.x1()) / 2;
        double midY = (segment.y0() + segment.y1()) / 2;
        double midZ = (segment.z0() + segment.z1()) / 2;
        double midWidth = (segment.width0() + segment.width1()) / 2;

        // Le milieu part dans une direction quelconque du plan perpendiculaire a l'arc.
        double angle = rng.nextDouble() * Math.PI * 2;
        double distance = rng.nextDouble() * offset;
        midY += distance * Math.sin(angle);
        midZ += distance * Math.cos(angle);

        Segment first = new Segment(segment.x0(), segment.y0(), segment.z0(),
                midX, midY, midZ, segment.width0(), midWidth, segment.alpha());
        Segment second = new Segment(midX, midY, midZ,
                segment.x1(), segment.y1(), segment.z1(), midWidth, segment.width1(), segment.alpha());
        out.add(first);
        out.add(second);
        return first;
    }

    /** La branche qui part du milieu d'un segment, dans la direction de sa premiere moitie. */
    private static List<Segment> branch(Segment first, Segment whole, ArcPattern pattern, Random rng) {
        double dx = (first.x1() - first.x0()) * pattern.lengthShrink();
        double dy = (first.y1() - first.y0()) * pattern.lengthShrink();
        double dz = (first.z1() - first.z0()) * pattern.lengthShrink();
        double[] dir = deviate(dx, dy, dz, BRANCH_DEVIATION, rng);

        double width = first.width1() * pattern.widthShrink();
        return List.of(new Segment(first.x1(), first.y1(), first.z1(),
                first.x1() + dir[0], first.y1() + dir[1], first.z1() + dir[2],
                width, width, whole.alpha() * pattern.alphaShrink()));
    }

    /**
     * Un vecteur devie au hasard, de moins de {@code degrees} sur chaque axe.
     *
     * <p>L'original faisait tourner la direction sur les trois axes, chacun d'un angle tire
     * entre -a et +a. Deux axes suffisent a donner la meme chose a l'oeil, et la troisieme
     * rotation n'etait jamais visible : un ruban d'eclair n'a pas d'orientation propre.
     */
    static double[] deviate(double x, double y, double z, double degrees, Random rng) {
        double limit = Math.toRadians(degrees);
        double yaw = (rng.nextDouble() * 2 - 1) * limit;
        double pitch = (rng.nextDouble() * 2 - 1) * limit;

        double turnedX = Math.cos(yaw) * x + Math.sin(yaw) * z;
        double turnedZ = -Math.sin(yaw) * x + Math.cos(yaw) * z;
        double turnedY = Math.cos(pitch) * y - Math.sin(pitch) * turnedZ;
        double finalZ = Math.sin(pitch) * y + Math.cos(pitch) * turnedZ;
        return new double[] { turnedX, turnedY, finalZ };
    }
}
