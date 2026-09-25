package cn.academy.client.hud;

import java.util.Locale;

/**
 * Ce que le lecteur media affiche : une position, et une part de morceau ecoulee.
 *
 * <p>Portage de {@code MediaBackend.getDisplayTime} : deux chiffres partout, donc {@code 04:30}
 * et non {@code 4:30}. Le reste — la barre — se lit en part, entre 0 et 1.
 *
 * <p>Aucun type Minecraft ici : c'est un modele, donc relisible en JUnit.
 */
public final class MediaHudVisuals {

    private MediaHudVisuals() {}

    /**
     * Une position, en {@code mm:ss}.
     *
     * <p>Une duree inconnue (ou negative, ou NaN) s'ecrit {@code 00:00}, comme l'original quand
     * rien ne jouait : un morceau dont on ne sait pas la longueur ne doit pas afficher un
     * nombre invente.
     */
    public static String formatTime(float seconds) {
        if (!(seconds > 0f)) return "00:00";
        int total = (int) seconds;
        return String.format(Locale.ROOT, "%02d:%02d", total / 60, total % 60);
    }

    /**
     * La part du morceau deja ecoulee, entre 0 et 1.
     *
     * <p>Sans duree connue, la barre reste vide : c'est le seul choix qui ne mente pas.
     */
    public static float progress(float elapsedSeconds, float lengthSeconds) {
        if (!(lengthSeconds > 0f) || !(elapsedSeconds > 0f)) return 0f;
        return Math.min(1f, elapsedSeconds / lengthSeconds);
    }
}
