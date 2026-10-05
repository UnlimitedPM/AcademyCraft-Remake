package cn.academy.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * La courbe cubique de l'original, portage de {@code CubicCurve}.
 *
 * <p>Une suite de points, joints par des segments dont les tangentes sont prises <b>au milieu</b>
 * des pentes voisines — la meme recette que les splines de Catmull-Rom —, et qui vaut la pente du
 * segment aux deux bouts. La courbe <b>passe donc par chacun de ses points</b>, ce qui est tout ce
 * qu'on lui demande ici : les courbes des ondes de vecmanip se lisent a des instants fixes, et
 * leurs valeurs a ces instants-la sont celles de l'original.
 *
 * <p>Hors de son domaine, elle <b>prolonge en ligne droite</b> (c'est ce que fait l'original, et
 * ce n'est pas un detail : la courbe d'echelle des ondes est lue jusqu'a 1,62 alors que son
 * dernier point est a 2,5, donc dans son domaine — mais la courbe d'opacite, elle, se lit une fois
 * par anneau, jusqu'a un peu plus d'un). Une courbe vide rend zero.
 *
 * <p>Elle est pure : elle ne connait ni Minecraft ni le temps, donc elle se verifie en JUnit.
 */
public final class CubicCurve {

    /** Un point de la courbe. */
    public record Point(double x, double y) {
    }

    private final List<Point> points = new ArrayList<>();

    /** Ajoute un point, et rend la courbe : les points se rangent dans l'ordre des x. */
    public CubicCurve add(double x, double y) {
        points.add(new Point(x, y));
        points.sort(Comparator.comparingDouble(Point::x));
        return this;
    }

    public int size() {
        return points.size();
    }

    /** La valeur de la courbe en {@code x}, prolongee en ligne droite au-dela de ses bouts. */
    public double valueAt(double x) {
        if (points.isEmpty()) return 0;

        int index = 0;
        while (index < points.size() && points.get(index).x() < x) {
            index++;
        }

        // Au-dela du dernier point : la pente du dernier segment, prolongee.
        if (index == points.size()) {
            Point last = points.get(points.size() - 1);
            double slope = points.size() >= 2
                    ? slope(points.size() - 1, points.size() - 2) : 0;
            return last.y() + (x - last.x()) * slope;
        }
        // Et en deca du premier : celle du premier.
        if (index == 0) {
            Point first = points.get(0);
            return first.y() + tangent(0, 1) * (x - first.x());
        }

        Point from = points.get(index - 1), to = points.get(index);
        double length = to.x() - from.x();
        double t = (x - from.x()) / length, t2 = t * t, t3 = t2 * t;
        double y0 = from.y(), y1 = to.y();
        double m0 = tangent(index - 1, length), m1 = tangent(index, length);

        return t3 * (m0 + m1 + 2 * y0 - 2 * y1)
                + t2 * (-2 * m0 - m1 - 3 * y0 + 3 * y1)
                + t * m0
                + y0;
    }

    /** La tangente d'un point, ramenee a la longueur du segment ou elle sert. */
    private double tangent(int i, double length) {
        double ret;
        if (i == 0) {
            ret = points.size() == 1 ? 0 : slope(i, i + 1);
        } else if (i == points.size() - 1) {
            ret = slope(i, i - 1);
        } else {
            ret = 0.5 * (slope(i + 1, i) + slope(i, i - 1));
        }
        return ret * length;
    }

    private double slope(int from, int to) {
        Point a = points.get(from), b = points.get(to);
        return (b.y() - a.y()) / (b.x() - a.x());
    }
}
