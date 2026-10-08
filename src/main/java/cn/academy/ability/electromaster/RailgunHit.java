package cn.academy.ability.electromaster;

/**
 * La zone du tir tendu du railgun : un <b>cylindre</b>, portage de {@code RangedRayDamage}.
 *
 * <p>L'original ne tirait pas sur une cible : il prenait <b>toutes celles d'un cylindre</b>, et il
 * les touchait de la plus proche a la plus lointaine. Deux nombres le definissent, et ils se lisent
 * dans son constructeur :
 *
 * <ul>
 *   <li>{@code new RangedRayDamage.Reflectible(ctx, 2, energy, ...)} — ce {@code 2} est le
 *       <b>RAYON</b> du cylindre, et non sa portee : c'est lui qui donne le disque de blocs creuses
 *       ({@code s * s + t * t <= range * range}) et le filtre des entites
 *       ({@code proj.length() < range * 1.2}) ;</li>
 *   <li>et {@code maxIncrement = 50} est sa <b>LONGUEUR</b> — la boite des entites s'etend de
 *       cinquante blocs le long du tir, et le traceur de blocs s'arrete la lui aussi.</li>
 * </ul>
 *
 * <p>Le port, lui, ne prenait qu'<b>une</b> cible a trente blocs, et le joueur l'a vu tout de suite :
 * « un monstre a 46 blocs ne se fait pas tuer alors que dans le vrai mod si ». C'etait exactement
 * cela : trente au lieu de cinquante, une cible au lieu d'un cylindre.
 *
 * <p>Ses degats tombent avec la distance, et c'est la distance <b>PERPENDICULAIRE</b> qui compte, non
 * celle le long du tir : {@code lerpf(1, 0.2, dist / maxIncrement)}. Un monstre juste devant le canon
 * encaisse donc le plein tarif, et un autre, cinquante blocs plus loin mais dans l'axe, autant —
 * c'est la largeur du faisceau qui decroit, pas sa portee.
 *
 * <p>Classe PURE, sans un seul type de Minecraft : la geometrie du tir se relit donc en JUnit, et
 * c'est ce qui permet de verifier le tir a quarante-six blocs sans lancer un jeu.
 */
public final class RailgunHit {

    /** Le rayon du cylindre : le {@code 2} de l'original. */
    public static final double RADIUS = 2.0;

    /** Et sa longueur : les cinquante blocs de son {@code maxIncrement}. */
    public static final double LENGTH = 50.0;

    /**
     * De combien l'original elargissait son filtre : {@code range * 1.2}.
     *
     * <p>Une tolerance, et elle est visible en jeu : le coin du cylindre, ou une cible qui longe le
     * bord, ne sont pas perdus pour un centimetre.
     */
    public static final double WIDEN = 1.2;

    /** Les degats au depart, et ce qu'il en reste au bout : {@code lerpf(1, 0.2, ...)}. */
    public static final double FULL = 1.0;
    public static final double AT_END = 0.2;

    private RailgunHit() {
    }

    /**
     * La distance perpendiculaire d'une cible a l'axe du tir, ou {@code -1} si elle est hors du
     * cylindre.
     *
     * <p>C'est le seul test du tir : hors de la portee, ou plus loin du canon que le rayon, la cible
     * est manquee. Tout ce qui reste est la distance qui donne l'attenuation, car c'est elle qui
     * sert ensuite — il n'y a donc rien a recalculer.
     *
     * @param to X, Y et Z de la cible <b>moins le depart du tir</b>, en blocs
     * @param direction la direction du tir, NORMALISEE
     */
    public static double hitDistance(double toX, double toY, double toZ,
                                     double directionX, double directionY, double directionZ) {
        // Le long du tir : negatif, la cible est derriere le canon ; au-dela de la longueur, elle
        // est hors de portee. C'est le `maxIncrement` de l'original.
        double along = toX * directionX + toY * directionY + toZ * directionZ;
        if (along < 0.0 || along > LENGTH) return -1.0;

        // Et la distance au droit de l'axe : c'est elle qui a un plafond — le rayon.
        double offX = toX - directionX * along;
        double offY = toY - directionY * along;
        double offZ = toZ - directionZ * along;
        double perpendicular = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
        return perpendicular > RADIUS * WIDEN ? -1.0 : perpendicular;
    }

    /**
     * Ce qu'il reste des degats a cette distance perpendiculaire, de {@link #FULL} a {@link #AT_END}.
     *
     * <p>La distance est plafonnee a la longueur du tir, comme chez l'original
     * ({@code dist = min(maxIncrement, ...)}) : au-dela, l'attenuation ne descend plus.
     */
    public static double damageFactor(double perpendicular) {
        double beyond = Math.min(perpendicular, LENGTH) / LENGTH;
        return FULL + (AT_END - FULL) * beyond;
    }
}
