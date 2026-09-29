package cn.academy.client.hud;

/**
 * Le voile d'ecran de l'original : ses couleurs et sa vitesse, sans le dessin.
 *
 * <p>Portage de {@code BackgroundMask}. Plein cadre, l'original teintait tout de la couleur de
 * la categorie du joueur des que son aptitude etait allumee, et <b>de rouge</b> pendant une
 * surcharge. Ce voile se pose derriere le HUD : c'est lui qui donnait au temoin de CP la densite
 * qui manquait ici, et c'est pour cela que sa plaque et sa bande de surcharge avaient ete
 * densifiees a la main le 30/09.
 *
 * <p>Sa couleur <b>glisse</b> vers la visee au lieu de sauter, d'une unite par seconde (le
 * {@code CHANGE_PER_SEC} de l'original) : l'allumage et l'extinction se voient comme un fondu,
 * et le passage au rouge en surcharge comme une teinte qui gagne.
 */
public final class MaskVisuals {

    /** Le rouge de la surcharge, tel quel de l'original : (208, 20, 20, 170). */
    public static final int OVERLOAD_COLOR = 0xAAD01414;

    /** La vitesse de l'original, son {@code CHANGE_PER_SEC} : une unite sur une, par seconde. */
    public static final float CHANGE_PER_SEC = 1.0f;

    private MaskVisuals() {
    }

    /**
     * La couleur visee, sachant ce qui se passe.
     *
     * <p>L'original gardait les trois canaux d'une couleur eteinte et ne baissait que son
     * opacite : le voile s'efface donc en fondu, sans passer par le noir. C'est a cela que sert
     * {@code current} — la couleur affichee en ce moment.
     */
    public static int target(int current, boolean overloaded, boolean activated, int categoryColor) {
        if (overloaded) return OVERLOAD_COLOR;
        if (activated) return categoryColor;
        return current & 0x00FFFFFF;
    }

    /** Le pas d'une image, exprime en unites de canal (0 a 255). */
    public static float step(float seconds) {
        return Math.max(0f, seconds) * CHANGE_PER_SEC * 255.0f;
    }

    /**
     * Avance chacun des quatre canaux vers la couleur visee, d'au plus un pas.
     *
     * <p>C'est le {@code balanceTo} de l'original, canal par canal : un pas lineaire, jamais de
     * depassement, et le meme dans les deux sens. {@link CpBarVisuals#balance} porte ce pas.
     */
    public static int smooth(int from, int to, float step) {
        return channel(CpBarVisuals.balance(alpha(from), alpha(to), step), 24)
                | channel(CpBarVisuals.balance(red(from), red(to), step), 16)
                | channel(CpBarVisuals.balance(green(from), green(to), step), 8)
                | channel(CpBarVisuals.balance(blue(from), blue(to), step), 0);
    }

    private static int channel(float value, int shift) {
        float clamped = Math.max(0f, Math.min(255f, value));
        return Math.round(clamped) << shift;
    }

    private static float alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    private static float red(int argb) {
        return (argb >> 16) & 0xFF;
    }

    private static float green(int argb) {
        return (argb >> 8) & 0xFF;
    }

    private static float blue(int argb) {
        return argb & 0xFF;
    }
}
