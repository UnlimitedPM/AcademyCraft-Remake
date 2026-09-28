package cn.academy.client.discord;

import cn.academy.AcademyCraft;
import cn.academy.discord.DiscordPresence;
import cn.academy.discord.PresenceLines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * Ou est le joueur, et depuis quand.
 *
 * <p>Le seul endroit du mod qui regarde le jeu pour le raconter : le menu, une partie solo,
 * un serveur, et la dimension courante. Ce qui en est montre — et ce qui en est cache — se
 * decide dans {@code PresenceLines}, teste hors du jeu ; ici on ne fait que regarder et
 * transmettre.
 *
 * <p>La fiche est relue une fois par seconde, et deposee telle quelle au service : c'est lui
 * qui decide de l'envoyer, et jamais plus d'une fois toutes les quinze secondes.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class DiscordEvents {

    /** L'heure de lancement : l'horloge montree sous la fiche part de la, et ne bouge plus. */
    private static final long STARTED_AT = System.currentTimeMillis();

    /** Deux ecritures par tick, et la situation ne change pas d'un tick a l'autre. */
    private static final int PERIOD_TICKS = 20;

    private static int ticks;

    /** Le nom du mod et sa version, lus une seule fois. */
    private static String version;

    /** La traduction du joueur : ce qu'il lit sur son profil est donc dans sa langue. */
    private static final PresenceLines.Lines LANG =
            (key, args) -> Component.translatable(key, args).getString();

    private DiscordEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++ticks < PERIOD_TICKS) return;
        ticks = 0;

        DiscordConfig.Settings settings = DiscordConfig.read();
        if (!settings.enabled()) {
            // Eteint en jeu : la fiche deja posee doit disparaitre, pas attendre un redemarrage.
            DiscordService.want(null);
            return;
        }

        DiscordService.want(PresenceLines.presence(situation(), options(settings), LANG));
    }

    /**
     * La fiche s'efface avant l'adresse du serveur.
     *
     * <p>Forge previent quand la connexion se termine, et c'est le dernier instant ou l'on
     * sait encore sur quel serveur on etait : sans cela, l'adresse resterait affichee le
     * temps que le menu se charge.
     */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DiscordService.want(null);
    }

    /** Ou le joueur est, en ce moment. */
    private static PresenceLines.Situation situation() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            // Menu principal, ou chargement : aucun monde, donc aucune dimension.
            return new PresenceLines.Situation(PresenceLines.Place.MENU, null, null, STARTED_AT);
        }

        String dimension = minecraft.level.dimension().location().toString();
        if (minecraft.hasSingleplayerServer()) {
            return new PresenceLines.Situation(PresenceLines.Place.SOLO, null, dimension, STARTED_AT);
        }

        ServerData server = minecraft.getCurrentServer();
        return new PresenceLines.Situation(PresenceLines.Place.MULTIPLAYER,
                server == null ? null : server.ip, dimension, STARTED_AT);
    }

    /** Ce que la config autorise a montrer. */
    private static PresenceLines.Options options(DiscordConfig.Settings settings) {
        return new PresenceLines.Options(settings.showServerAddress(), settings.showDimension(),
                settings.buttonLabel(), settings.buttonUrl(), version());
    }

    /**
     * La version du mod, lue au premier tick.
     *
     * <p>Pas dans un champ initialise a la construction de la classe : celle-ci est chargee
     * avant que la liste des mods ne soit remplie, et la version serait vide pour toujours.
     */
    private static String version() {
        if (version == null) {
            version = ModList.get().getModContainerById(AcademyCraft.MOD_ID)
                    .map(container -> container.getModInfo().getVersion().toString())
                    .orElse("");
        }
        return version;
    }
}
