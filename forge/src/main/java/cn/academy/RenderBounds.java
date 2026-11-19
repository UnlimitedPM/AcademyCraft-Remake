package cn.academy;

import net.minecraft.core.Direction;

/**
 * La boite dans laquelle un rendu de block entity a le droit de se dessiner.
 *
 * <p>Minecraft elimine un rendu de block entity des que la boite de <b>son bloc</b> sort du champ
 * de la camera. C'est vrai pour une caisse, faux pour tout ce que le mod dessine : les pales de
 * l'eolienne balaient sept blocs de rayon, le Matrix est un cube de deux blocs dont le rendu part
 * d'un <b>coin</b>, et le developpeur s'etend sur trois blocs de profondeur et trois de haut. Le
 * joueur l'a vu tout de suite (22/10) : « les helices de l'eolienne partent tres vite si je regarde
 * un peu trop vers le bas, et le Matrix, si je regarde un peu trop haut en etant dessus, disparait
 * visuellement du jeu ».
 *
 * <p>L'original ne se posait pas la question autrement : ses block entities surchargeaient
 * {@code getRenderBoundingBox} — {@code BlockMulti.getRenderBB} pour les multi-blocs, et
 * {@code INFINITE_EXTENT_AABB} pour la tete de l'eolienne. Le port ne l'avait jamais fait, d'ou des
 * boites d'un bloc.
 *
 * <p>PUR : des nombres, relisibles en JUnit. Chaque methode rend {@code {minX, minY, minZ, maxX,
 * maxY, maxZ}} <b>relatif au bloc d'ancrage</b>, que l'appelant decale de sa position.
 */
public final class RenderBounds {

    /**
     * La marge ajoutee a chaque boite, en blocs.
     *
     * <p>Un rendu ne s'arrete pas pile au bord de sa geometrie : les plaques du Matrix flottent,
     * les pales de l'eolienne depassent leur rayon theorique d'un demi-bloc, et une boite trop
     * juste fait reapparaitre le defaut a la premiere image ou la camera bouge. Un quart de bloc
     * suffit, et ne coute rien : c'est une boite, pas un tri.
     */
    public static final double MARGIN = 0.25d;

    private RenderBounds() {}

    /**
     * La tete de l'eolienne : ses pales balaient {@code BLADE_RADIUS} blocs de rayon.
     *
     * <p>L'original rendait cette boite <b>infinie</b> ({@code INFINITE_EXTENT_AABB}), ce qui
     * revient a ne jamais eliminer le rendu. Une boite de la taille des pales suffit et garde le
     * benefice de l'elimination quand l'eolienne est vraiment loin derriere soi.
     *
     * @param radius le rayon balaye, en blocs — {@code WindgenMainBlockEntity.BLADE_RADIUS}
     */
    public static double[] windgenRotor(int radius) {
        double r = radius + MARGIN;
        return new double[]{-r, -r, -r, 1.0d + r, 1.0d + r, 1.0d + r};
    }

    /**
     * Le Matrix : un cube de deux blocs dont l'ancrage est un <b>coin</b>.
     *
     * <p>Le multi-bloc s'etend d'un bloc vers {@code left} et d'un bloc vers {@code back}, et de
     * deux blocs en hauteur. Le rendu, lui, part du coin de l'ancrage — c'est la meme origine que
     * {@link MatrixStructure#centreX} et {@link MatrixStructure#centreZ}.
     */
    public static double[] matrix(Direction facing) {
        double dx = MatrixStructure.left(facing).getStepX() + MatrixStructure.back(facing).getStepX();
        double dz = MatrixStructure.left(facing).getStepZ() + MatrixStructure.back(facing).getStepZ();
        return box(dx, 2.0d, dz);
    }

    /**
     * Le developpeur : trois blocs de profondeur, trois de haut, un de large.
     *
     * <p>Les huit parties de {@code DeveloperBlock.DevPart} vont de {@code dist} 0 a 2 vers
     * l'arriere et de {@code h} 0 a 2 vers le haut ; l'ancrage est en bas devant.
     */
    public static double[] developer(Direction facing) {
        Direction back = facing.getOpposite();
        return box(back.getStepX() * 2.0d, 3.0d, back.getStepZ() * 2.0d);
    }

    /**
     * Une boite qui part du bloc d'ancrage et s'etend de {@code dx}, {@code dy}, {@code dz} blocs.
     *
     * <p>Les trois decalages peuvent etre negatifs : le multi-bloc s'etend alors dans le sens
     * negatif de l'axe, et la boite est retournee pour rester valide.
     */
    private static double[] box(double dx, double dy, double dz) {
        double minX = Math.min(0.0d, dx) - MARGIN;
        double minY = -MARGIN;
        double minZ = Math.min(0.0d, dz) - MARGIN;
        double maxX = Math.max(1.0d, 1.0d + dx) + MARGIN;
        double maxY = Math.max(1.0d, 1.0d + dy) + MARGIN;
        double maxZ = Math.max(1.0d, 1.0d + dz) + MARGIN;
        return new double[]{minX, minY, minZ, maxX, maxY, maxZ};
    }
}
