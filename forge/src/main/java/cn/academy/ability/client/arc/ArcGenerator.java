package cn.academy.ability.client.arc;

import cn.academy.ability.client.arc.ArcMesh.Quad;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * La generation des eclairs, portage de {@code ArcFactory}.
 *
 * <p>Le principe, tel que l'original l'ecrivait : on part d'un segment droit de la portee du
 * motif ; a chaque passe, chaque segment est coupe en deux et son milieu est deplace au
 * hasard dans le plan perpendiculaire (le plan YZ, l'arc allant le long de X) ; le
 * deplacement maximal est divise par deux a chaque passe, donc le trait se casse de plus en
 * plus fin ; et de temps en temps, une branche part du milieu dans la direction du premier
 * morceau, raccourcie et deviee.
 *
 * <p>Puis chaque ligne ainsi obtenue est transformee en rubans, exactement comme
 * {@code handleSegment} le faisait : pour chaque segment, deux coins au depart et deux a
 * l'arrivee, la direction du ruban etant perpendiculaire au segment et au plan du motif —
 * et le bord de depart reprenant la direction du segment PRECEDENT, donc les quads se
 * partagent leurs bords au lieu de se chevaucher. Un petit quart de tour au hasard, comme
 * le {@code randomRotate(15)} de l'original, evite que tout soit parfaitement aligne.
 *
 * <p>Deux differences avec l'original, et elles sont voulues :
 * <ul>
 *   <li>le hasard est <b>amorce</b>. L'original tirait ses motifs a chaque lancement, donc
 *       rien de tout cela n'etait reproductible ni testable ;</li>
 *   <li>le quart de tour se fait <b>autour du segment</b>, et non autour d'un axe tire au
 *       hasard : le ruban reste ainsi perpendiculaire au trait qu'il dessine, alors que
 *       l'original pouvait le faire basculer hors de son axe. Meme intention, et une
 *       surface qui ne se retourne jamais.</li>
 * </ul>
 */
public final class ArcGenerator {

    /** L'angle maximal dont un ruban est tourne autour de son segment, en degres. */
    private static final double RIBBON_TWIST = 15;

    /** Le plan du motif : tout ce qui n'est pas l'axe de l'arc. */
    private static final double[] PATTERN_NORMAL = { 0, 0, 1 };

    private ArcGenerator() {}

    /** Un motif d'eclair, avec la graine donnee. */
    public static ArcMesh generate(ArcPattern pattern, Random rng) {
        double width = pattern.width();
        double length = pattern.length();

        // La ligne de depart : un seul segment, de l'origine jusqu'a la portee du motif.
        List<List<Segment>> current = new ArrayList<>();
        current.add(List.of(new Segment(new Point(0, 0, 0, width),
                new Point(length, 0, 0, width), 1.0)));

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

        List<Quad> quads = new ArrayList<>();
        for (List<Segment> line : current) {
            ribbons(line, rng, quads);
        }
        return new ArcMesh(quads, length);
    }

    /**
     * Les rubans d'une ligne, enchaines comme dans l'original.
     *
     * <p>C'est ici que se joue l'aspect de l'eclair : chaque quad utilise la direction du
     * ruban du segment precedent pour son bord de depart, donc deux quads voisins decrivent
     * exactement le meme bord. Sans ce chainage, ils se croisent, et deux surfaces
     * translucides superposees se melangent deux fois.
     */
    private static void ribbons(List<Segment> line, Random rng, List<Quad> out) {
        double[] last = null;
        for (Segment segment : line) {
            double[] direction = ribbonDirection(segment, rng);
            double[] start = last == null ? direction : last;

            // Les quatre coins, dans l'ordre de la texture : le bord de depart d'un cote
            // puis de l'autre, et le bord d'arrivee dans le meme sens. C'est l'ordre exact
            // des addVert de l'original.
            Point a = shift(segment.start(), start, segment.start().width());
            Point b = shift(segment.start(), start, -segment.start().width());
            Point c = shift(segment.end(), direction, -segment.end().width());
            Point d = shift(segment.end(), direction, segment.end().width());

            out.add(new Quad(segment.start().x(), segment.alpha(),
                    a.x(), a.y(), a.z(),
                    b.x(), b.y(), b.z(),
                    c.x(), c.y(), c.z(),
                    d.x(), d.y(), d.z()));

            last = direction;
        }
    }

    /**
     * La direction du ruban d'un segment : perpendiculaire au segment, puis tournee au hasard.
     *
     * <p>L'original prenait {@code crossProduct(dir, normal)} avec la normale de son motif, puis
     * passait le resultat a {@code randomRotate(15)} : trois rotations tirees au hasard, sur les
     * trois axes. Le ruban n'est donc pas dans un plan fixe, il est de travers, chacun a sa
     * facon.
     *
     * <p>Ce n'est pas un detail de finition, et le joueur l'a vu. Mon portage tournait le ruban
     * autour du segment, ce qui le laissait dans le plan du motif : de profil les deux se
     * ressemblent, mais le tireur, lui, regarde son eclair par la tranche, et une tranche ne se
     * voit pas. Son attaque paraissait donc plus fine que celle de l'original, alors que vue de
     * l'exterieur elle faisait exactement la meme taille.
     */
    private static double[] ribbonDirection(Segment segment, Random rng) {
        double[] direction = normalize(subtract(segment.start(), segment.end()));
        double[] ribbon = cross(direction, PATTERN_NORMAL);
        if (length(ribbon) < 1e-6) {
            // Segment parallele au plan du motif : il n'y a pas de perpendiculaire unique,
            // on prend la verticale du motif plutot que de rendre un vecteur nul.
            ribbon = new double[] { 0, 1, 0 };
        }

        return deviate(ribbon[0], ribbon[1], ribbon[2], RIBBON_TWIST, rng);
    }

    /**
     * Coupe un segment en deux, en deplacant son milieu, et range les deux morceaux.
     *
     * <p>Rend le premier morceau, dont la direction sert a celle d'une eventuelle branche.
     */
    private static Segment half(Segment segment, double offset, Random rng, List<Segment> out) {
        Point start = segment.start();
        Point end = segment.end();
        double midX = (start.x() + end.x()) / 2;
        double midY = (start.y() + end.y()) / 2;
        double midZ = (start.z() + end.z()) / 2;
        double midWidth = (start.width() + end.width()) / 2;

        // Le milieu part dans une direction quelconque du plan perpendiculaire a l'arc.
        double angle = rng.nextDouble() * Math.PI * 2;
        double distance = rng.nextDouble() * offset;
        midY += distance * Math.sin(angle);
        midZ += distance * Math.cos(angle);

        Segment first = new Segment(start, new Point(midX, midY, midZ, midWidth), segment.alpha());
        Segment second = new Segment(new Point(midX, midY, midZ, midWidth), end, segment.alpha());
        out.add(first);
        out.add(second);
        return first;
    }

    /** La branche qui part du milieu d'un segment, dans la direction de sa premiere moitie. */
    private static List<Segment> branch(Segment first, Segment whole, ArcPattern pattern, Random rng) {
        Point start = first.start();
        Point end = first.end();
        double dx = (end.x() - start.x()) * pattern.lengthShrink();
        double dy = (end.y() - start.y()) * pattern.lengthShrink();
        double dz = (end.z() - start.z()) * pattern.lengthShrink();
        double[] turned = deviate(dx, dy, dz, 10, rng);

        double width = end.width() * pattern.widthShrink();
        return List.of(new Segment(end,
                new Point(end.x() + turned[0], end.y() + turned[1], end.z() + turned[2], width),
                whole.alpha() * pattern.alphaShrink()));
    }

    /**
     * Un vecteur devie au hasard : c'est le {@code randomRotate} de l'original, aux memes
     * nombres. Un angle est tire dans la plage demandee, puis le vecteur tourne sur les trois
     * axes, chacun d'un angle tire au hasard dans cette portee et de signe tire au hasard.
     *
     * <p>L'original faisait bien ces trois rotations. J'en avais garde deux, en jugeant la
     * troisieme invisible : c'est faux, et cela s'est paye ailleurs — voir
     * {@link #ribbonDirection}, qui a justement besoin des trois pour sortir les rubans du plan
     * du motif.
     */
    static double[] deviate(double x, double y, double z, double degrees, Random rng) {
        double reach = (rng.nextDouble() * 2 - 1) * Math.toRadians(degrees);
        double[] turned = rotateX(new double[] { x, y, z }, (rng.nextDouble() * 2 - 1) * reach);
        turned = rotateY(turned, (rng.nextDouble() * 2 - 1) * reach);
        return rotateZ(turned, (rng.nextDouble() * 2 - 1) * reach);
    }

    /** Une rotation autour de l'axe X du motif — le tangage de l'original. */
    private static double[] rotateX(double[] v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new double[] { v[0], v[1] * cos - v[2] * sin, v[1] * sin + v[2] * cos };
    }

    /** Une rotation autour de l'axe Y du motif — le lacet de l'original. */
    private static double[] rotateY(double[] v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new double[] { v[0] * cos + v[2] * sin, v[1], -v[0] * sin + v[2] * cos };
    }

    /** Une rotation autour de l'axe Z du motif — l'axe de son {@code rotateAroundZ}. */
    private static double[] rotateZ(double[] v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new double[] { v[0] * cos - v[1] * sin, v[0] * sin + v[1] * cos, v[2] };
    }

    // ------------------------------------------------------------------
    // Le petit calcul de vecteurs, pour ne dependre de rien
    // ------------------------------------------------------------------

    private static Point shift(Point point, double[] direction, double amount) {
        return new Point(point.x() + direction[0] * amount,
                point.y() + direction[1] * amount,
                point.z() + direction[2] * amount,
                point.width());
    }

    private static double[] subtract(Point from, Point to) {
        return new double[] { to.x() - from.x(), to.y() - from.y(), to.z() - from.z() };
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] { a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0] };
    }

    private static double length(double[] vector) {
        return Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2]);
    }

    private static double[] normalize(double[] vector) {
        double size = length(vector);
        if (size < 1e-9) return new double[] { 1, 0, 0 };
        return new double[] { vector[0] / size, vector[1] / size, vector[2] / size };
    }

    /** Un point du motif, avec la largeur du trait a cet endroit. */
    private record Point(double x, double y, double z, double width) {}

    /** Un bout de la ligne de l'eclair, avant d'etre transforme en ruban. */
    private record Segment(Point start, Point end, double alpha) {}
}
