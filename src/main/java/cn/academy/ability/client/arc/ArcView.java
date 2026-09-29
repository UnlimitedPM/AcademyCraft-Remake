package cn.academy.ability.client.arc;

/**
 * L'optimisation de vue de l'original : l'eclair est decale jusqu'a la main du tireur.
 *
 * <p>Portage fidele de ce que LambdaLib2 appelait {@code ViewOptimize}, dont le commentaire dit
 * exactement cela : « transforms the origin to the player's hand in thirdPerson or firstPerson ».
 * Ses deux decalages sont repris ici au chiffre pres.
 *
 * <p>Ce qui compte, c'est le repere dans lequel ils s'appliquent. L'original les posait APRES
 * avoir tourne son entite : le decalage est donc mesure dans le repere de l'ARC — le long de
 * l'eclair, dans sa hauteur, puis sur son cote — et non dans celui du monde. C'est ce qui rend
 * l'illusion stable : un bras monte quand on leve les yeux au ciel, ce decalage non. Un depart
 * pose sur le corps, lui, monte avec le tangage, et l'eclair finit par flotter a cote du joueur
 * — c'est la faute que le joueur a vue deux fois.
 *
 * <p>Et c'est ce qui explique ses trois remarques. En vue interne le decalage est petit :
 * l'eclair part juste sous le reticule, et l'oeil qui tire ne voit pas d'ou il sort. En vue
 * externe il vaut 0,8 bloc vers le bas, soit 0,82 bloc au-dessus des pieds : la main qui pend,
 * exactement le point que le joueur a decrit — la ou les jambes, le torse et le bras se
 * rejoignent. Et quand on vise le ciel, ce decalage s'applique dans le repere de l'arc, donc
 * derriere et sous l'oeil : l'attaque ne part plus du personnage du tout.
 *
 * <p>Aucun type de Minecraft ici : les points sont des coordonnees, et tout se relit en test.
 */
public final class ArcView {

    /**
     * Vu de sa propre vue interne : un peu en arriere, plus bas, et du cote de la main qui tient.
     *
     * <p>Les trois nombres de l'original, dans son ordre : le long de l'arc, dans sa hauteur,
     * puis sur son cote.
     */
    public static final double[] FIRST_PERSON = { -0.05, -0.25, 0.2 };

    /** Vu autrement — vue externe, ou l'eclair d'un autre joueur : jusqu'a sa main. */
    public static final double[] THIRD_PERSON = { 0.15, -0.8, 0.23 };

    private ArcView() {}

    /**
     * L'eclair, decale dans son propre repere : le long de l'arc, dans sa hauteur, puis sur son
     * cote. Les deux bouts glissent du meme vecteur — direction et longueur inchangees.
     *
     * <p>Le repere vient de {@link ArcFrame}, ou il est deja verifie : un eclair de longueur
     * nulle n'en a pas, et reste alors ou il est.
     */
    public static double[][] fix(double[] from, double[] to, double[] above, double[] offset) {
        ArcFrame frame = ArcFrame.between(from, to, above);
        if (frame == null) return new double[][] { from, to };

        double[] fixedFrom = new double[3];
        double[] fixedTo = new double[3];
        for (int i = 0; i < 3; i++) {
            double delta = frame.axis()[i] * offset[0]
                    + frame.up()[i] * offset[1]
                    + frame.side()[i] * offset[2];
            fixedFrom[i] = from[i] + delta;
            fixedTo[i] = to[i] + delta;
        }
        return new double[][] { fixedFrom, fixedTo };
    }
}
