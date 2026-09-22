package cn.academy.misc.media;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Les morceaux livres avec le mod.
 *
 * <p>L'original lisait sa liste dans sa configuration — un juge de morceaux par defaut, plus
 * tout ce que le joueur deposait dans un dossier — et decodait chaque fichier pour en
 * connaitre la duree, la pochette et le titre. Le port garde les <b>trois</b> morceaux du
 * mod, dans l'ordre de l'original, et rien d'autre : un dossier de musique se gere tres bien
 * hors du jeu, et le port n'a pas de decodeur OGG.
 *
 * <p>L'ordre compte : la donnee d'un joueur range ses morceaux par identifiant, mais
 * l'affichage les suit dans l'ordre de cette liste, comme l'original.
 */
public final class MediaManager {

    /** Les trois morceaux livres, dans l'ordre de l'original. */
    public static final List<Media> INTERNAL = List.of(
            new Media("only_my_railgun"),
            new Media("level5_judgelight"),
            new Media("sisters_noise"));

    private MediaManager() {}

    public static List<Media> internalMedias() {
        return INTERNAL;
    }

    /** Le morceau de cet identifiant, ou {@code null} s'il n'existe pas. */
    @Nullable
    public static Media get(String id) {
        if (id == null) return null;
        for (Media media : INTERNAL) {
            if (media.id().equals(id)) return media;
        }
        return null;
    }

    /** L'objet qui donne ce morceau, ou {@code null} s'il n'en existe aucun. */
    @Nullable
    public static Media ofItemName(String itemName) {
        if (itemName == null) return null;
        for (Media media : INTERNAL) {
            if (media.itemName().equals(itemName)) return media;
        }
        return null;
    }
}
