package cn.academy.client.hud;

import cn.academy.client.gui.CustomizeUiLayout;

/**
 * Le deroulement d'une notification : ses temps, ses fondus et le glissement de son icone.
 *
 * <p>Portage de {@code NotifyUI}. L'original n'en montrait qu'<b>une seule</b> a la fois, et
 * celle-ci vivait six secondes : un demi-seconde d'entree, un demi-seconde pendant laquelle son
 * icone glissait de la droite vers sa place, une tenue, puis le fondu de sortie.
 *
 * <p>Une unite melangee de l'original est corrigee ici, comme le veut la regle du port quand le
 * code ne fait pas ce que ses constantes annoncent : ses opacites etaient calculees avec des
 * <b>millisecondes</b> ({@code dt / 300.0}, {@code (dt - 200) / 300}) alors que ses durees sont en
 * <b>secondes</b> ({@code KEEP_TIME = 6}, {@code BLEND_IN_TIME = 0.5}). Le port suit les
 * secondes — c'est ce que les noms disent, et ce qui donne a l'entree la demi-seconde qu'elle
 * annonce au lieu d'un clignotement.
 *
 * <p>PUR : aucun type du jeu, donc {@code NotificationVisualsTest} en est juge.
 */
public final class NotificationVisuals {

    /** La duree de vie complete : six secondes, comme l'original. */
    public static final float KEEP_TIME = 6f;

    /** L'entree du panneau, le glissement de l'icone, puis la sortie. */
    public static final float BLEND_IN_TIME = 0.5f;
    public static final float SCAN_TIME = 0.5f;
    public static final float BLEND_OUT_TIME = 0.3f;

    /** L'icone commence a apparaitre a 0,2 s : les 200 ms de l'original, en secondes. */
    public static final float ICON_FADE_FROM = 0.2f;

    /** Quand la tenue commence, et quand la sortie commence. */
    public static final float HOLD_FROM = BLEND_IN_TIME + SCAN_TIME;
    public static final float FADE_FROM = KEEP_TIME - BLEND_OUT_TIME;

    private NotificationVisuals() {}

    /** La notification est-elle encore la ? */
    public static boolean visible(float age) {
        return age >= 0f && age < KEEP_TIME;
    }

    /**
     * L'opacite commune : celle du panneau, et le facteur que le reste multiplie.
     *
     * <p>Elle entre en {@link #BLEND_IN_TIME}, tient jusqu'a {@link #FADE_FROM}, puis sort en
     * {@link #BLEND_OUT_TIME} — les trois temps de l'original.
     */
    public static float fade(float age) {
        if (age < 0f) return 0f;
        if (age < BLEND_IN_TIME) return age / BLEND_IN_TIME;
        if (age < FADE_FROM) return 1f;
        return clamp01((KEEP_TIME - age) / BLEND_OUT_TIME);
    }

    /** L'opacite de l'icone : elle entre pendant la seconde moitie de l'entree. */
    public static float iconAlpha(float age) {
        float own = clamp01((age - ICON_FADE_FROM) / (BLEND_IN_TIME - ICON_FADE_FROM));
        return fade(age) * own;
    }

    /** Celle des deux lignes : elles n'arrivent qu'avec le glissement, comme chez lui. */
    public static float textAlpha(float age) {
        if (age < BLEND_IN_TIME) return 0f;
        return fade(age) * scanProgress(age);
    }

    /**
     * L'avancement du glissement, ralenti en arrivant.
     *
     * <p>L'original le lissait par un sinus ({@code sin(prog * PI/2)}) : l'icone part vite et se
     * pose doucement.
     */
    public static float scanProgress(float age) {
        return (float) Math.sin(clamp01((age - BLEND_IN_TIME) / SCAN_TIME) * Math.PI / 2);
    }

    /**
     * La position horizontale de l'icone, dans les unites de l'original.
     *
     * <p>Elle part de {@code NOTIFY_ICON_START_X} (la droite du panneau) et se pose a
     * {@code NOTIFY_ICON_X}, en suivant le glissement.
     */
    public static float iconX(float age) {
        float start = CustomizeUiLayout.NOTIFY_ICON_START_X;
        float end = CustomizeUiLayout.NOTIFY_ICON_X;
        return start + (end - start) * scanProgress(age);
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
