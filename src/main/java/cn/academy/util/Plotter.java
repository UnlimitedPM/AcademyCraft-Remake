package cn.academy.util;

import net.minecraft.core.BlockPos;

/**
 * Le promeneur en ligne de l'original, {@code cn.academy.util.Plotter} : il avance d'un
 * bloc a la fois depuis une origine, en suivant une direction donnee.
 *
 * <p>C'est une <b>marche de Bresenham</b> sans les arrondis habituels : a chaque pas, on
 * regarde si l'ecart entre la ligne ideale et la position courante depasse un demi-bloc,
 * sur la hauteur d'abord puis sur la largeur, et on avance l'axe qui a le plus decroche.
 * Un seul axe bouge par pas, donc la ligne reste connexe — c'est ce qui permet a l'onde
 * de choc de creuser une tranche sans trou.
 *
 * <p>L'axe principal est celui qui varie le plus. L'astuce du code d'origine est de
 * <b>permettre les coordonnees</b> pour toujours avancer sur X, puis de les remettre dans
 * l'ordre a la sortie : trois axes, un seul algorithme. La classe est pure (aucun type de
 * monde), donc verifiable en test unitaire — ce qui compte, parce que c'est elle qui
 * decide ou l'onde de choc tape.
 *
 * <p>Elle ne s'arrete jamais d'elle-meme : c'est l'appelant qui borne le nombre de pas,
 * et l'energie qui borne l'effet.
 */
public class Plotter {

    private enum Axis { X, Y, Z }

    private final Axis axis;
    private final double dyx;
    private final double dzx;
    private final int dirflag;

    private final int x0;
    private final int y0;
    private final int z0;

    private int x;
    private int y;
    private int z;

    /**
     * Une marche partant de {@code (originX, originY, originZ)} dans la direction
     * {@code (dx, dy, dz)}.
     *
     * <p>La direction n'a pas besoin d'etre unitaire : seuls ses rapports comptent. Un
     * vecteur horizontalement nul ne definit aucune direction, et l'original refusait ce
     * cas — le port fait de meme, plutot que de marcher droit devant par accident.
     *
     * <p>Attention en relisant ce constructeur : l'original nommait ses parametres
     * {@code _x0, _y0, _z0} pour que l'echange des axes porte bien sur les <b>champs</b>.
     * Un port qui reprendrait les memes noms que les champs echangerait les parametres et
     * laisserait l'origine intacte — ce qui ne se voit pas quand l'origine vaut zero, et
     * qui fait alors pietiner la marche. D'ou les noms distincts ici.
     */
    public Plotter(int originX, int originY, int originZ, double dx, double dy, double dz) {
        // L'axe principal est celui qui varie le plus, et on echange les coordonnees pour
        // se ramener au cas "on avance sur X".
        double adx = Math.abs(dx);
        double ady = Math.abs(dy);
        double adz = Math.abs(dz);

        int itemp;
        double dtemp;
        Axis axis;
        if (adz > ady && adz > adx) {
            dtemp = dz;
            dz = dx;
            dx = dtemp;
            itemp = originZ;
            originZ = originX;
            originX = itemp;
            axis = Axis.Z;
        } else if (ady > adx) {
            dtemp = dy;
            dy = dx;
            dx = dtemp;
            itemp = originY;
            originY = originX;
            originX = itemp;
            axis = Axis.Y;
        } else if (adx > 0) {
            axis = Axis.X;
        } else {
            throw new IllegalArgumentException("Zero slope vector");
        }

        this.axis = axis;
        this.x0 = originX;
        this.y0 = originY;
        this.z0 = originZ;
        x = originX;
        y = originY;
        z = originZ;
        dyx = dy / dx;
        dzx = dz / dx;
        dirflag = dx > 0 ? 1 : -1;
    }

    /**
     * Le pas suivant.
     *
     * <p>La hauteur est corrigee <b>avant</b> la largeur, comme dans l'original : sur une
     * pente a 45 degres, la marche monte donc d'abord et avance ensuite, ce qui donne un
     * escalier regulier. Inverser les deux tests changerait la forme de la tranche.
     */
    public BlockPos next() {
        int nextX = x + dirflag;
        double valy = y0 + (nextX - x0) * dyx;
        double valz = z0 + (nextX - x0) * dzx;
        if (Math.abs(valy - y) > 0.5) {
            y += (int) Math.signum(dyx) * dirflag;
        } else if (Math.abs(valz - z) > 0.5) {
            z += (int) Math.signum(dzx) * dirflag;
        } else {
            x = nextX;
        }

        return switch (axis) {
            case X -> new BlockPos(x, y, z);
            case Y -> new BlockPos(y, x, z);
            case Z -> new BlockPos(z, y, x);
        };
    }
}
