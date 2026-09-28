package cn.academy.ability.client.arc;

/**
 * Le repere dans lequel un eclair est pose : l'axe de l'arc, et les deux directions de
 * l'ecran.
 *
 * <p>Le motif d'un eclair est dessine dans son propre repere : son axe X va de la main a la
 * cible, et ses deux autres directions portent ses ecarts. Ces deux-la doivent etre
 * <b>perpendiculaires a l'axe</b> et <b>perpendiculaires entre elles</b>, sinon les rubans se
 * replient sur eux-memes et l'eclair part de travers.
 *
 * <p>C'est arrive : le rendu passait deux fois la meme direction, l'une etant l'autre
 * normalisee. Rien ne se voyait a la compilation, et a l'ecran l'eclair s'effilochait en
 * haut a gauche au lieu de partir de la main. Le repere est donc sorti du rendu, ou il ne se
 * relisait pas, pour etre verifie : les trois directions sont unitaires et se coupent a angle
 * droit, pour n'importe quelle visee.
 *
 * <p>Les deux directions laterales sont prises dans le plan de l'ECRAN : c'est ce que faisait
 * l'original, dont le motif suivait le regard du tireur. Sans cela, une des deux directions
 * tombe le long de la vue, l'ecart part vers l'oeil, et l'eclair perd la moitie de son
 * zigzag.
 */
public record ArcFrame(double[] axis, double[] side, double[] up) {

    /** Le repere d'un arc, entre ses deux bouts et pour une verticale d'ecran donnee. */
    public static ArcFrame between(double[] from, double[] to, double[] cameraUp) {
        double dx = to[0] - from[0];
        double dy = to[1] - from[1];
        double dz = to[2] - from[2];
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1e-6) return null;

        double[] axis = { dx / length, dy / length, dz / length };

        // La verticale de l'ecran, rendue perpendiculaire a l'axe : elle porte les ecarts du
        // motif dans le sens de la hauteur de l'ecran.
        double along = cameraUp[0] * axis[0] + cameraUp[1] * axis[1] + cameraUp[2] * axis[2];
        double[] up = {
                cameraUp[0] - axis[0] * along,
                cameraUp[1] - axis[1] * along,
                cameraUp[2] - axis[2] * along };
        double upLength = Math.sqrt(up[0] * up[0] + up[1] * up[1] + up[2] * up[2]);
        if (upLength < 1e-4) {
            // L'arc monte ou descend droit : la verticale de l'ecran ne dit plus rien, et
            // une perpendiculaire quelconque vaut mieux qu'un repere nul.
            up = perpendicular(axis);
        } else {
            up = new double[] { up[0] / upLength, up[1] / upLength, up[2] / upLength };
        }

        return new ArcFrame(axis, cross(axis, up), up);
    }

    /** Une perpendiculaire quelconque a l'axe, choisie stable. */
    private static double[] perpendicular(double[] axis) {
        double[] reference = Math.abs(axis[1]) > 0.9 ? new double[] { 1, 0, 0 } : new double[] { 0, 1, 0 };
        double[] side = cross(axis, reference);
        double length = Math.sqrt(side[0] * side[0] + side[1] * side[1] + side[2] * side[2]);
        return new double[] { side[0] / length, side[1] / length, side[2] / length };
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] { a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0] };
    }

    /** Un coin du motif dans le monde : le long de l'axe, puis les deux ecarts. */
    public double[] point(double[] from, double x, double y, double z) {
        return new double[] {
                from[0] + axis[0] * x + side[0] * y + up[0] * z,
                from[1] + axis[1] * x + side[1] * y + up[1] * z,
                from[2] + axis[2] * x + side[2] * y + up[2] * z };
    }
}
