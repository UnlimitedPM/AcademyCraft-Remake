package cn.academy.client.gui.cgui;

import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * Un element d'histogramme : c'est l'{@code HistElement} de l'original.
 *
 * <p>Un nom, une couleur, une valeur de zero a un, et le texte qui la resume. La valeur se relit a
 * chaque image — c'est ainsi que l'original suivait une machine qui produit — donc rien n'est fige
 * ici : les quatre fabriques ci-dessous ne font que ranger des lectures en attente.
 *
 * <h2>Les quatre du mod</h2>
 *
 * <p>Elles viennent de {@code TechUI}, couleurs comprises, et chacune dit ce qu'elle mesure :
 *
 * <ul>
 *   <li>{@link #energy} — le tampon d'une machine, en IF ;</li>
 *   <li>{@link #buffer} — le meme, d'une autre couleur, pour les pages qui montrent les deux ;</li>
 *   <li>{@link #liquid} — le contenu d'une cuve, en millibuckets ;</li>
 *   <li>{@link #capacity} — une quantite sur son maximum, sans unite.</li>
 * </ul>
 *
 * <p>Le nom est une <b>cle</b> de langue, et l'ecran la traduit au moment de l'afficher. Une cle qui
 * n'existe pas ressort telle quelle : c'est ainsi que l'element « IF » du generateur de phase, ecrit
 * en dur par l'original, s'affiche encore aujourd'hui sous ce nom-la.
 *
 * <p>La couleur est rangee comme le moteur range les siennes : <b>rouge, vert, bleu, alpha</b>, quatre
 * cases. C'est ce que le composant de barre lit pour se dessiner, et lui en donner trois le fait
 * planter — l'alpha se lit a la quatrieme place.
 */
public record CGuiHist(String id, int[] color, DoubleSupplier value, Supplier<String> desc) {

    /** La boite de l'histogramme : le fond de l'original, et sa taille a l'ecran. */
    static final double BOX = 210;
    static final double BOX_SCALE = 0.4;
    static final String TEXTURE = "academy:textures/guis/histogram.png";

    /** Ce qu'une barre occupe dans cette boite : seize de large, cent vingt de haut. */
    static final double BAR_WIDTH = 16;
    static final double BAR_HEIGHT = 120;

    /** Et ou elle se pose : a partir de 56, tous les quarante, a 78 du haut. */
    static final double BAR_X = 56;
    static final double BAR_STEP = 40;
    static final double BAR_Y = 78;

    /** La hauteur qu'un histogramme prend dans le panneau d'infos, une fois mis a l'echelle. */
    static final double ADVANCE = BOX * BOX_SCALE;

    /** La couleur du tampon : {@code 0xff25c4ff} chez l'original. */
    private static final int ENERGY_COLOR = 0x25C4FF;

    /** Celle du second tampon : {@code 0xff25f7ff}. */
    private static final int BUFFER_COLOR = 0x25F7FF;

    /** Celle de la phase liquide : {@code 0xff7680de}. */
    private static final int LIQUID_COLOR = 0x7680DE;

    /** Et celle d'une capacite : {@code 0xffff6c00}. */
    private static final int CAPACITY_COLOR = 0xFF6C00;

    /** Le tampon d'une machine, lu en direct, sur sa taille. */
    public static CGuiHist energy(DoubleSupplier energy, double max) {
        return new CGuiHist("ac.gui.common.hist.energy", rgb(ENERGY_COLOR),
                () -> energy.getAsDouble() / max,
                () -> String.format("%.0f IF", energy.getAsDouble()));
    }

    /** Le meme, en plus clair : les pages qui montrent deux tampons les distinguent ainsi. */
    public static CGuiHist buffer(DoubleSupplier energy, double max) {
        return new CGuiHist("ac.gui.common.hist.buffer", rgb(BUFFER_COLOR),
                () -> energy.getAsDouble() / max,
                () -> String.format("%.0f IF", energy.getAsDouble()));
    }

    /** Le contenu d'une cuve, en millibuckets. */
    public static CGuiHist liquid(DoubleSupplier amount, double max) {
        return new CGuiHist("ac.gui.common.hist.liquid", rgb(LIQUID_COLOR),
                () -> amount.getAsDouble() / max,
                () -> String.format("%.0f mB", amount.getAsDouble()));
    }

    /** Une quantite sur son maximum, sans unite : « 12/40 ». */
    public static CGuiHist capacity(DoubleSupplier amount, int max) {
        return new CGuiHist("ac.gui.common.hist.capacity", rgb(CAPACITY_COLOR),
                () -> amount.getAsDouble() / max,
                () -> (int) amount.getAsDouble() + "/" + max);
    }

    /**
     * Un element ecrit a la main, comme l'original en laissait la possibilite : son nom tel quel,
     * sa couleur, sa valeur et son texte. Le generateur de phase s'en sert pour sa phase liquide.
     *
     * @param color la couleur <b>en ARGB</b>, telle que l'original l'ecrit — {@code 0xffb983fb}
     */
    public static CGuiHist custom(String id, int color, DoubleSupplier value,
                                  Supplier<String> desc) {
        return new CGuiHist(id, rgb(color), value, desc);
    }

    /**
     * Les quatre morceaux d'une couleur, ranges comme le moteur les range : rouge, vert, bleu, et
     * l'opacite — pleine, puisque les couleurs de l'original le sont.
     *
     * <p>Ils acceptent l'ARGB comme le RGB : les composantes se lisent a partir du bit 16, donc un
     * octet d'opacite devant ne gene pas. C'est ce qui permet de recopier les couleurs de l'original
     * telles quelles, {@code 0xff...} compris.
     */
    private static int[] rgb(int colour) {
        return new int[] { red(colour), green(colour), blue(colour), 255 };
    }

    private static int red(int colour) {
        return (colour >> 16) & 0xFF;
    }

    private static int green(int colour) {
        return (colour >> 8) & 0xFF;
    }

    private static int blue(int colour) {
        return colour & 0xFF;
    }
}
