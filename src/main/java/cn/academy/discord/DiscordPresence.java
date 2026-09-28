package cn.academy.discord;

import java.util.List;

/**
 * La fiche montree sur Discord : deux lignes de texte, deux images, et un lien.
 *
 * <p>C'est exactement ce que le protocole envoie, et rien de plus : ce qui DECIDE de son
 * contenu vit dans {@link PresenceLines}, pour que la decision se relise en JUnit.
 *
 * <p>Un champ nul veut dire « rien a montrer ». Discord accepte une fiche partielle, et une
 * image dont la cle n'existe pas chez l'application ne montre simplement rien : on peut donc
 * livrer le mod avant d'avoir televerse les images.
 */
public record DiscordPresence(
        String details,
        String state,
        long startMillis,
        String largeImage,
        String largeText,
        String smallImage,
        String smallText,
        List<Button> buttons) {

    /** Un lien sous la fiche. Discord n'en accepte que deux, et seulement en https. */
    public record Button(String label, String url) {}

    public DiscordPresence {
        buttons = List.copyOf(buttons);
    }
}
