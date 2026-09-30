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
    private static final int[][] COLORS = {
            { 115, 200, 227 },
            { 161, 181, 188 },
            { 87, 231, 248 },
            { 97, 204, 94 },
            { 235, 109, 84 },
    };

    private MineDetectVisuals() {}

    /** La portee reellement balayee. */
    public static double capRange(double range) {
        return Math.min(range, RANGE_CAP);
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
        return advanced ? Math.min(COLORS.length - 2, harvestTier + 1) : 0;
    }

    /** La couleur d'un palier, en trois canaux de 0 a 255. */
    public static int[] colorFor(int tier) {
        int index = Math.max(0, Math.min(COLORS.length - 1, tier));
        return COLORS[index];
    }
}
