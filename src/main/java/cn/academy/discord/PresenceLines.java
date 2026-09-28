package cn.academy.discord;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Ce qu'on montre sur Discord, decide sans Minecraft.
 *
 * <p>Trois situations, et c'est tout : le menu principal, une partie solo, un serveur. Le
 * texte vient des cles de langue du client (la traduction est injectee ici), donc un joueur
 * qui joue en francais voit du francais sur son profil — c'est le client qui sait dans quelle
 * langue il joue, pas Discord.
 *
 * <p>L'adresse du serveur ne s'affiche que si le joueur l'a demandee : un profil Discord se
 * lit par ses amis, et l'adresse d'un serveur prive n'a rien a y faire. C'est pour cela que
 * le reglage existe, et qu'il est eteint par defaut.
 */
public final class PresenceLines {

    /** Ou le joueur est, vu de Discord. */
    public enum Place {
        /** Aucun monde ouvert : menu principal, ou chargement. */
        MENU,
        /** Une partie ouverte en solo. */
        SOLO,
        /** Un serveur, celui de quelqu'un d'autre. */
        MULTIPLAYER
    }

    /** Ce que la situation apporte. Le rendu client le remplit, le test aussi. */
    public record Situation(Place place, @Nullable String serverAddress,
                            @Nullable String dimensionId, long startMillis) {}

    /** Ce que la config autorise. */
    public record Options(boolean showAddress, boolean showDimension,
                          @Nullable String buttonLabel, @Nullable String buttonUrl, String version) {}

    /**
     * La traduction, injectee.
     *
     * <p>Le client donne les cles de langue du joueur ; le test, un faux qui rend la cle et
     * ses arguments. La decision se relit donc sans lancer le jeu.
     */
    public interface Lines {
        String text(String key, Object... args);
    }

    /** L'image de fond de toutes les fiches : le logo du mod. */
    public static final String LARGE_IMAGE = "academy";

    private PresenceLines() {}

    /** La fiche a poser, ou a remplacer quand la situation change. */
    public static DiscordPresence presence(Situation situation, Options options, Lines lines) {
        String details;
        String small;
        switch (situation.place()) {
            case MENU -> {
                details = lines.text("academy.discord.menu");
                small = "menu";
            }
            case SOLO -> {
                details = lines.text("academy.discord.solo");
                small = "solo";
            }
            default -> {
                details = address(situation, options, lines);
                small = "multi";
            }
        }

        // La dimension n'a de sens que dans un monde, et le joueur peut la cacher.
        String state = options.showDimension() && situation.place() != Place.MENU
                ? dimension(situation, lines)
                : null;

        return new DiscordPresence(details, state, situation.startMillis(),
                LARGE_IMAGE, lines.text("academy.discord.large", options.version()),
                small, lines.text("academy.discord.image." + small),
                buttons(options));
    }

    /** Le nom du serveur, ou une phrase generique quand le joueur prefere ne pas le dire. */
    private static String address(Situation situation, Options options, Lines lines) {
        String address = situation.serverAddress();
        if (!options.showAddress() || address == null || address.isBlank()) {
            return lines.text("academy.discord.multiplayer");
        }
        return lines.text("academy.discord.server", address);
    }

    /**
     * Le nom lisible d'une dimension.
     *
     * <p>Les trois dimensions du jeu ont leur cle de langue ; celles d'un autre mod gardent
     * leur identifiant, ce qui reste juste — et mieux qu'un « Inconnu ».
     */
    private static String dimension(Situation situation, Lines lines) {
        String id = situation.dimensionId();
        if (id == null || id.isBlank()) return null;

        String name = switch (id) {
            case "minecraft:overworld" -> lines.text("academy.discord.dimension.overworld");
            case "minecraft:the_nether" -> lines.text("academy.discord.dimension.nether");
            case "minecraft:the_end" -> lines.text("academy.discord.dimension.end");
            default -> id;
        };
        return lines.text("academy.discord.state", name);
    }

    /**
     * Le lien sous la fiche, s'il y en a un.
     *
     * <p>Discord n'accepte que des adresses en https, et refuse la fiche entiere quand une
     * autre arrive : une adresse invalide est donc ignoree, plutot que d'effacer le reste.
     */
    private static List<DiscordPresence.Button> buttons(Options options) {
        String label = options.buttonLabel();
        String url = options.buttonUrl();
        if (label == null || label.isBlank()) return List.of();
        if (url == null || !url.startsWith("https://")) return List.of();
        return List.of(new DiscordPresence.Button(label, url));
    }
}
