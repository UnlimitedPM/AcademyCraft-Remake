package cn.academy.ability.client;

/**
 * Les nombres du bouclier de lumiere, portage de {@code RenderMdShield}.
 *
 * <p>Classe sans aucun type Minecraft, expres : ce sont les courbes de l'original —
 * apparition en quinze ticks, fondu en six, rotation qui accelere jusqu'a trente ticks
 * — et les figer par un test est la seule facon de verifier qu'elles ne bougent pas
 * sans lancer un jeu.
 */
public final class ShieldVisuals {

    /** Taille du bouclier en blocs, comme {@code SIZE} de l'original. */
    public static final float SIZE = 1.8f;

    /** Distance devant le joueur ou il flotte, en blocs. */
    public static final double DISTANCE = 1.0;

    /** Hauteur au-dessus des pieds, en blocs : le {@code (0, 1.1, 0)} de l'original. */
    public static final double HEIGHT = 1.1;

    /** Ticks de l'apparition : le bouclier part petit et grossit jusqu'a sa taille. */
    private static final float GROW_TICKS = 15f;

    /** Ticks du fondu d'entree. */
    private static final float FADE_TICKS = 6f;

    /** Ticks au bout desquels la rotation atteint sa vitesse maximale. */
    private static final float SPIN_TICKS = 30f;

    private ShieldVisuals() {}

    /** Taille du bouclier apres {@code ticks} ticks de maintien. */
    public static float scale(int ticks) {
        return SIZE * lerp(0.2f, 1f, ticks / GROW_TICKS);
    }

    /** Opacite du bouclier apres {@code ticks} ticks de maintien. */
    public static float alpha(int ticks) {
        return Math.min(ticks / FADE_TICKS, 1f);
    }

    /**
     * Vitesse de rotation du disque, en degres par milliseconde.
     *
     * L'original lisait le temps du jeu plutot que les ticks : sa rotation ne depend
     * donc pas de la cadence des ticks, seulement du temps qui passe.
     */
    public static float spinSpeed(int ticks) {
        return lerp(0.8f, 2f, ticks / SPIN_TICKS);
    }

    /** Interpolation bornee, la meme que celle des competences. */
    private static float lerp(float from, float to, float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        return from + (to - from) * clamped;
    }
}
