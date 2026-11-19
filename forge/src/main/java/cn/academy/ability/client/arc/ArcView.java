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
     * <p>C'est le decalage des ECLAIRS, que l'original appliquait en translatant son dessin
     * entier. Un rayon ne s'en sert pas : voir {@link #fixStart}.
     *
     * <p>Le repere vient de {@link ArcFrame}, ou il est deja verifie : un eclair de longueur
     * nulle n'en a pas, et reste alors ou il est.
     */
    public static double[][] fix(double[] from, double[] to, double[] above, double[] offset) {
        ArcFrame frame = ArcFrame.between(from, to, above);
        if (frame == null) return new double[][] { from, to };

        double[] delta = delta(frame, offset);
        double[] fixedFrom = new double[3];
        double[] fixedTo = new double[3];
        for (int i = 0; i < 3; i++) {
            fixedFrom[i] = from[i] + delta[i];
            fixedTo[i] = to[i] + delta[i];
        }
        return new double[][] { fixedFrom, fixedTo };
    }

    /**
     * Le meme decalage, mais sur le SEUL bout de depart : le rayon part de la main et arrive
     * toujours la ou il visait.
     *
     * <p>C'est ce que faisait le rendu des RAYONS de l'original — {@code RendererRayBaseGlow} et
     * {@code RendererRayBaseSimple} — et son commentaire dit pourquoi : « Don't fix end to get
     * accurate pointing direction ». Un rayon n'est pas un eclair : sa pointe est un point du
     * monde qu'on a vise, et la deplacer avec le depart la detachait de ce qu'elle touche.
     *
     * <p>La ou cela se voit, c'est sur la salve de rayons : son pre-rayon finit sur la bille de
     * silicium, et les vingt-cinq a trente traits qui partent juste apres partent de la bille
     * elle-meme. Avec les deux bouts decales, la pointe du pre-rayon se posait a une trentaine de
     * centimetres du point de depart de la gerbe, et le joueur l'a vu tout de suite.
     *
     * <p>L'echange est celui de l'original : la direction du rayon change d'un rien, puisque son
     * depart a bouge et que sa pointe est restee. C'est le prix de la pointe juste.
     */
    public static double[][] fixStart(double[] from, double[] to, double[] above,
                                      double[] offset) {
        ArcFrame frame = ArcFrame.between(from, to, above);
        if (frame == null) return new double[][] { from, to };

        double[] delta = delta(frame, offset);
        double[] fixedFrom = new double[3];
        for (int i = 0; i < 3; i++) {
            fixedFrom[i] = from[i] + delta[i];
        }
        return new double[][] { fixedFrom, to };
    }

    /** Le decalage dans le monde : le long de l'axe, puis la hauteur, puis le cote. */
    private static double[] delta(ArcFrame frame, double[] offset) {
        double[] delta = new double[3];
        for (int i = 0; i < 3; i++) {
            delta[i] = frame.axis()[i] * offset[0]
                    + frame.up()[i] * offset[1]
                    + frame.side()[i] * offset[2];
        }
        return delta;
    }
}
