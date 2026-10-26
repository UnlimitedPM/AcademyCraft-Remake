package cn.academy.ability.client;

/**
 * Ce que l'eclat de la detection de minerais dessine, sans type de monde ni de rendu : les
 * couleurs, la transparence, et le plafond de portee.
 *
 * <p>L'original avait tout cela dans son entite de rendu — cinq couleurs figees, une
 * transparence qui s'efface avec la distance, et un plafond de vingt-huit blocs sur une
 * portee qui monte pourtant a trente. Le port les range ici pour que ce soit verifiable : une
 * couleur par palier de pioche se relit sur une capture d'ecran, pas une valeur.
 */
public final class MineDetectVisuals {

    /**
     * Le plafond de la portee balayee.
     *
     * La competence monte jusqu'a trente blocs, mais l'original plafonnait son balayage a
     * vingt-huit : le volume d'un rayon de trente est trop grand pour etre relu a chaque
     * five ticks, et personne ne voit la difference.
     */
    public static final double RANGE_CAP = 28.0;

    /** Periode du balayage, en ticks : l'original relisait tous les cinq. */
    public static final int SCAN_PERIOD = 5;

    /** Le nombre de minerais au-dela duquel le balayage s'arrete, comme l'original. */
    public static final int SCAN_LIMIT = 8400;

    /** La transparence minimale, pour les minerais les plus loin. */
    public static final float BASE_ALPHA = 0.3f;

    /** Et ce qui efface un minerai avec la distance, dans l'original. */
    public static final double FADE = 2.2;

    /**
     * Les cinq couleurs de l'original, du minerai ordinaire au plus dur.
     *
     * La premiere est celle de tous les minerais tant que l'eclat n'est pas complet ; les
     * quatre autres sont celles des paliers de pioche, la couleur montant avec la difficulte.
     * La cinquieme n'etait jamais atteinte chez l'original, dont le palier plafonne a trois —
     * le port garde la table telle quelle, parce que c'est la sienne.
     */
    /**
     * Le BLEU de l'eclat, et c'est la seule couleur qu'il donne.
     *
     * <p>L'original avait cinq teintes, une par palier de pioche, et son eclat complet (demi-
     * experience et categorie de niveau 4) les allumait toutes : le vert et l'orange arrivaient
     * donc sans que le joueur ait rien demande — « certains minerais apparaissent en vert et je
     * ne sais pas pourquoi [...] je ne vois pas l'utilite ». Demande du 26/10 : tout redevient
     * blue, et seul le minerai choisi dans l'ecran de filtre passe au vert (voir {@link #SELECTED}).
     */
    public static final int[] ORE = { 115, 200, 227 };

    /**
     * Le VERT du minerai choisi dans l'ecran de filtre.
     *
     * <p>Demande du joueur le 26/10 : les minerais non choisis restent bleus, celui qu'il a
     * designe passe au vert. C'est une couleur de <b>choix</b>, et non de palier : elle passe
     * donc avant la table, quel que soit l'eclat et quelle que soit la pioche.
     */
    public static final int[] SELECTED = { 90, 230, 120 };

    private MineDetectVisuals() {}

    /** La portee reellement balayee. */
    public static double capRange(double range) {
        return Math.min(range, RANGE_CAP);
    }

    /**
     * La part de la portee ou un minerai a encore une opacite.
     *
     * <p>{@link #alpha} tombe a zero quand {@code 1 - d / range * FADE} atteint
     * {@code -BASE_ALPHA / (1 - BASE_ALPHA)}, c'est-a-dire a
     * {@code (1 + BASE_ALPHA / (1 - BASE_ALPHA)) / FADE} — 0,6494 avec les chiffres de
     * l'original. Au-dela, le minerai n'est <b>jamais</b> dessine : le balayage n'a donc rien a
     * lire la-bas, et c'est ce que {@link #visibleRange} dit a {@code MineDetectOverlay}.
     */
    public static final double VISIBLE_RANGE_FRACTION =
            (1.0 + BASE_ALPHA / (1.0 - BASE_ALPHA)) / FADE;

    /** La distance au-dela de laquelle un minerai ne se dessine plus, pour une portee donnee. */
    public static double visibleRange(double range) {
        return range * VISIBLE_RANGE_FRACTION;
    }

    /**
     * La transparence d'un minerai, selon sa distance.
     *
     * Portage de {@code calcAlpha} : la base est a 0,3, et le minerai gagne jusqu'a 1 en
     * s'approchant — l'effacement valant 2,2 fois la portee, un minerai a mi-portee est deja
     * presque efface. A 0,65 portee, la formule tombe sur zero : c'est la portee REELLE de
     * l'eclat, plus courte que celle de son balayage.
     *
     * <p>ECART CORRIGE : le port bornait le FACTEUR d'effacement a zero avant de le melanger a
     * la base, donc laissait les minerais lointains a 0,3 jusqu'au bout, alors que l'original
     * les effacait tout a fait — son alpha negatif finissait borne a zero par la couleur LWJGL
     * qui le recevait (voir {@code Colors.f2i}, qui ne borne rien lui-meme). C'est la borne
     * FINALE qui est reprise ici, et c'est elle qui rend la portee lisible : on voit ce qu'on
     * eclaire, pas tout ce qui a ete balaye.
     */
    public static float alpha(double distance, double range) {
        if (range <= 0) return BASE_ALPHA;
        double fade = BASE_ALPHA + (1.0 - distance / range * FADE) * (1.0 - BASE_ALPHA);
        return (float) Math.max(0.0, Math.min(1.0, fade));
    }

    /**
     * Le palier de couleur d'un minerai.
     *
     * Sans l'eclat complet, tous les minerais se ressemblent (palier 0, la couleur par
     * defaut). Avec, le palier de pioche s'affiche — decale d'un cran, parce que l'original
     * reservait sa premiere couleur aux minerais ordinaires : un minerai qui se creuse a la
     * pierre prend la deuxieme, un minerai de diamant la quatrieme.
     */
    public static int tierOf(boolean advanced, int harvestTier) {
        return advanced ? Math.min(3, harvestTier + 1) : 0;
    }

    /** La couleur d'un minerai ordinaire : le bleu de l'eclat, quel que soit son palier. */
    public static int[] colorFor(int tier) {
        return ORE;
    }

    /**
     * La couleur d'un minerai, le choix du joueur passant AVANT le palier.
     *
     * <p>Le minerai choisi dans l'ecran de filtre est vert ; tous les autres gardent la couleur de
     * leur palier, donc le bleu de l'eclat. Voir {@link #SELECTED}.
     */
    public static int[] colorFor(int tier, boolean selected) {
        return selected ? SELECTED : colorFor(tier);
    }
}
