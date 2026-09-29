package cn.academy.client.hud;

/**
 * La geometrie et les couleurs du temoin de points de controle.
 *
 * <p>Portage des nombres de {@code CPBar} de l'original. Sa barre est une image de
 * {@code 964x147} dessinee a <b>0,2</b>, dans laquelle trois morceaux se posent : le fond sur
 * toute l'image, la <b>surcharge</b> dans une bande posee vers le haut, et le <b>remplissage de
 * CP</b> dans un rectangle de {@code 883x84} a partir de {@code (47, 30)}.
 *
 * <p>Deux details de l'original sont repris ici parce qu'ils se voient :
 *
 * <ul>
 *   <li>le remplissage ne part <b>jamais de zero</b> : il vit entre 16 % et 96 % de sa largeur,
 *       donc la barre montre toujours un morceau, meme a vide ;</li>
 *   <li>son extremite gauche est <b>coupee en biais</b> — 103 pixels de plus en bas qu'en haut,
 *       soit 44 degres — et l'original la dessinait a la main, faute de pouvoir le faire avec un
 *       rectangle. C'est {@link #fillLeftAt(float, int)} qui la decrit, ligne par ligne.</li>
 * </ul>
 *
 * <p>Le remplissage est aussi <b>colore selon le niveau</b> : rouge a vide, orange vers un tiers,
 * blanc a plein. La surcharge a sa propre echelle, presque transparente au debut.
 *
 * <p>Aucun type Minecraft ici : ce sont des nombres, relisibles en JUnit.
 */
public final class CpBarVisuals {

    /** L'echelle de dessin de l'original : ses images font 964x147, dessinees a 0,2. */
    public static final float SCALE = 0.2f;

    /** La taille des images de la barre. */
    public static final int TEX_W = 964;
    public static final int TEX_H = 147;

    /** Le remplissage de CP, dans l'image. */
    public static final int FILL_X = 47;
    public static final int FILL_Y = 30;
    public static final int FILL_W = 883;
    public static final int FILL_H = 84;

    /** La coupe de l'extremite : 103 pixels de large pour 44 degres. */
    public static final double CUT = 103.0 * Math.sin(Math.toRadians(44.0));

    /** Le remplissage vit entre 16 % et 96 % de sa largeur, meme a vide ou a plein. */
    public static final float FILL_MIN = 0.16f;
    public static final float FILL_SPAN = 0.8f;

    /**
     * La vitesse de l'animation, celle de l'original : deux unites de progression par seconde.
     *
     * <p>Chez lui, {@code CP_BALANCE_SPEED} et {@code O_BALANCE_SPEED} valent tous deux 2,0.
     * C'est elle qui fait GLISSER la surcharge au lieu de la faire sauter — dans un sens comme
     * dans l'autre, donc aussi quand la surcharge reflue apres une surcharge pleine. C'est ce
     * reflux, couleur comprise, que le joueur appelle « l'overload qui va a l'envers ».
     */
    public static final float BALANCE_SPEED = 2.0f;

    /** La surcharge se dessine dans cette bande, posee vers le haut de l'image. */
    public static final int OVER_X = 0;
    public static final int OVER_Y = 21;
    public static final int OVER_W = 943;
    public static final int OVER_H = 104;

    /** L'etat surcharge : le bandeau strie se dessine a partir de 30, sur 914 de large. */
    public static final int STRIPE_X = 30;
    public static final int STRIPE_W = 914;

    /** Les trois arrets de la couleur du remplissage, ceux de l'original. */
    private static final float[] CP_STOP = {0.0f, 0.35f, 1.0f};
    private static final int[] CP_COLOR = {0xFFF06767, 0xFFFFAE44, 0xFFFFFFFF};

    /** Les trois arrets de la surcharge : presque transparente, doree, puis rouge. */
    private static final float[] OVER_STOP = {0.0f, 0.55f, 1.0f};
    private static final int[] OVER_COLOR = {0x0ADFDFDF, 0x23F0D49D, 0x50F56464};

    private CpBarVisuals() {
    }

    /** La largeur du remplissage, en pixels d'image, pour un niveau de 0 a 1. */
    public static double fillWidth(float progress) {
        return FILL_W * (FILL_MIN + clamp01(progress) * FILL_SPAN);
    }

    /** Le bord gauche du remplissage <b>en haut</b> : c'est de la que part la coupe. */
    public static double fillLeft(float progress) {
        return FILL_X + FILL_W - fillWidth(progress);
    }

    /**
     * Le bord gauche du remplissage a une ligne donnee.
     *
     * <p>Le bas est decale vers la droite : la coupe va du coin haut-gauche au coin bas-droit
     * d'un rectangle de {@link #CUT} de large sur toute la hauteur. Une ligne sur deux de cette
     * bande n'appartient donc pas au remplissage, et c'est ce que le dessin doit respecter.
     */
    public static double fillLeftAt(float progress, int row) {
        return fillLeft(progress) + CUT * row / (double) FILL_H;
    }

    /** La largeur de la surcharge, en pixels d'image : elle pousse vers la gauche. */
    public static double overloadWidth(float overload) {
        return OVER_W * clamp01(overload);
    }

    /** Le bord gauche de la surcharge. */
    public static double overloadLeft(float overload) {
        return OVER_X + OVER_W - overloadWidth(overload);
    }

    /** La couleur du remplissage a ce niveau : rouge a vide, orange, puis blanc a plein. */
    public static int fillColor(float progress) {
        return ramp(CP_STOP, CP_COLOR, clamp01(progress));
    }

    /** La couleur de la surcharge : presque transparente, doree, puis rouge. */
    public static int overloadColor(float overload) {
        return ramp(OVER_STOP, OVER_COLOR, clamp01(overload));
    }

    /**
     * La valeur affichee, avancee vers la valeur reelle d'au plus {@code step}.
     *
     * <p>Portage exact du {@code balance} de l'original : un pas lineaire, jamais de
     * depassement, et le meme pas dans les deux sens. C'est ce qui donne a la barre un mouvement
     * regulier plutot qu'une suite de sauts.
     */
    public static float balance(float from, float to, float step) {
        float delta = to - from;
        if (Math.abs(delta) <= step) return to;
        return from + Math.signum(delta) * step;
    }

    /** Le pas d'un intervalle de temps, en secondes : la vitesse de l'original. */
    public static float balanceStep(float seconds) {
        return Math.max(0.0f, seconds) * BALANCE_SPEED;
    }

    /**
     * L'opacite d'une couleur, de 0 a 1.
     *
     * <p>Les couleurs de la surcharge portent leur propre transparence — {@code 0x0A}, {@code 0x23}
     * puis {@code 0x50} sur 255 chez l'original, soit a peine visible au debut. Encore faut-il
     * la lire : un dessin qui impose son opacite a la place efface justement ce que ces nombres
     * disent.
     */
    public static float alphaOf(int rgb) {
        return ((rgb >>> 24) & 0xFF) / 255.0f;
    }

    /** La couleur d'un canal alpha compris, entre deux arrets. */
    private static int ramp(float[] stops, int[] colors, float value) {
        int index = 0;
        while (index < stops.length - 2 && value > stops[index + 1]) {
            index++;
        }
        float span = stops[index + 1] - stops[index];
        float ratio = span <= 0 ? 0.0f : (value - stops[index]) / span;
        return mix(colors[index], colors[index + 1], ratio);
    }

    private static int mix(int from, int to, float ratio) {
        return channel(from, to, ratio, 24) | channel(from, to, ratio, 16)
                | channel(from, to, ratio, 8) | channel(from, to, ratio, 0);
    }

    private static int channel(int from, int to, float ratio, int shift) {
        int a = (from >> shift) & 0xFF;
        int b = (to >> shift) & 0xFF;
        return Math.round(a + (b - a) * ratio) << shift;
    }

    private static float clamp01(float value) {
        return value < 0.0f ? 0.0f : Math.min(value, 1.0f);
    }
}
