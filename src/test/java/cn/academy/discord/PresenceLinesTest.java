package cn.academy.discord;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ce qu'on montre sur Discord, relu sans Minecraft.
 *
 * <p>La traduction est injectee : le faux ci-dessous rend la cle et son argument, ce qui fait
 * lire la decision elle-meme plutot qu'une phrase anglaise.
 */
class PresenceLinesTest {

    /** La fausse traduction : « cle » ou « cle(argument) ». */
    private static final PresenceLines.Lines LANG = (key, args) ->
            args.length == 0 ? key : key + "(" + args[0] + ")";

    /** Le reglage par defaut : l'adresse du serveur cachee, la dimension montree. */
    private static final PresenceLines.Options SIMPLE =
            new PresenceLines.Options(false, true, null, null, "0.0.1");

    @Test
    @DisplayName("au menu principal, il n'y a ni dimension ni serveur a montrer")
    void auMenuPrincipal() {
        DiscordPresence presence = PresenceLines.presence(
                new PresenceLines.Situation(PresenceLines.Place.MENU, null, null, 42L), SIMPLE, LANG);

        assertEquals("academy.discord.menu", presence.details());
        assertNull(presence.state(), "hors d'un monde, aucune dimension");
        assertEquals("menu", presence.smallImage());
        assertEquals(PresenceLines.LARGE_IMAGE, presence.largeImage());
        assertEquals("academy.discord.large(0.0.1)", presence.largeText());
        assertEquals(42L, presence.startMillis(), "l'horloge de la session part du lancement");
    }

    @Test
    @DisplayName("en solo, la dimension se lit en clair")
    void enSoloLaDimensionSeLit() {
        DiscordPresence presence = PresenceLines.presence(
                new PresenceLines.Situation(PresenceLines.Place.SOLO, null, "minecraft:the_nether", 0L),
                SIMPLE, LANG);

        assertEquals("academy.discord.solo", presence.details());
        assertEquals("academy.discord.state(academy.discord.dimension.nether)", presence.state());
        assertEquals("solo", presence.smallImage());
    }

    @Test
    @DisplayName("une dimension d'un autre mod garde son identifiant")
    void uneDimensionInconnueGardeSonNom() {
        DiscordPresence presence = PresenceLines.presence(
                new PresenceLines.Situation(PresenceLines.Place.SOLO, null,
                        "twilightforest:twilight_forest", 0L),
                SIMPLE, LANG);

        assertEquals("academy.discord.state(twilightforest:twilight_forest)", presence.state());
    }

    @Test
    @DisplayName("l'adresse du serveur ne s'affiche que si le joueur le demande")
    void lAdresseSeCacheParDefaut() {
        PresenceLines.Situation situation = new PresenceLines.Situation(
                PresenceLines.Place.MULTIPLAYER, "play.example.net", "minecraft:overworld", 0L);

        assertEquals("academy.discord.multiplayer", PresenceLines.presence(situation, SIMPLE, LANG).details());
        assertEquals("multi", PresenceLines.presence(situation, SIMPLE, LANG).smallImage());

        PresenceLines.Options montre = new PresenceLines.Options(true, true, null, null, "0.0.1");
        assertEquals("academy.discord.server(play.example.net)",
                PresenceLines.presence(situation, montre, LANG).details());
    }

    @Test
    @DisplayName("la dimension se cache a la demande")
    void laDimensionSeCacheALaDemande() {
        PresenceLines.Options sansDimension = new PresenceLines.Options(false, false, null, null, "0.0.1");

        assertNull(PresenceLines.presence(new PresenceLines.Situation(
                PresenceLines.Place.SOLO, null, "minecraft:overworld", 0L), sansDimension, LANG).state());
    }

    @Test
    @DisplayName("un lien sans https est ignore, comme Discord le refuse")
    void leLienDoitEtreEnHttps() {
        PresenceLines.Situation situation =
                new PresenceLines.Situation(PresenceLines.Place.MENU, null, null, 0L);

        PresenceLines.Options complet =
                new PresenceLines.Options(false, true, "GitHub", "https://github.com/exemple", "0.0.1");
        assertEquals(1, PresenceLines.presence(situation, complet, LANG).buttons().size());
        assertEquals("GitHub", PresenceLines.presence(situation, complet, LANG).buttons().get(0).label());

        PresenceLines.Options sansEtiquette =
                new PresenceLines.Options(false, true, "", "https://github.com/exemple", "0.0.1");
        assertTrue(PresenceLines.presence(situation, sansEtiquette, LANG).buttons().isEmpty());

        PresenceLines.Options pasHttps =
                new PresenceLines.Options(false, true, "GitHub", "http://github.com/exemple", "0.0.1");
        assertTrue(PresenceLines.presence(situation, pasHttps, LANG).buttons().isEmpty(),
                "une adresse refusee vaut mieux qu'une fiche entiere rejetee");
    }
}
