package cn.academy.client.discord;

import cn.academy.discord.DiscordClient;
import cn.academy.discord.DiscordPipes;
import cn.academy.discord.DiscordPresence;
import cn.academy.discord.DiscordProtocol;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

/**
 * Le fil qui parle a Discord.
 *
 * <p>Pourquoi un fil a part : ouvrir un tube, ecrire dedans et lire ce qui revient peut
 * attendre. Le fil du client, lui, ne le peut pas — c'est celui qui dessine l'image. La
 * conversation entiere vit donc ici, une seconde a la fois, et le jeu ne fait que deposer
 * la fiche voulue par {@link #want}.
 *
 * <p>Ce fil ne doit jamais tomber : une exception qui remonterait ferait taire la presence
 * pour de bon, sans que le jeu n'en dise un mot. Chaque tour est donc protege, et l'etat
 * n'est touche que par ce fil-la.
 *
 * <p>Rien n'est obligatoire : Discord ferme, Discord lance apres le jeu, identifiant errone,
 * config modifiee en pleine partie — tout se reprend tout seul, ou s'arrete proprement.
 */
@OnlyIn(Dist.CLIENT)
public final class DiscordService {

    private static final Logger LOGGER = LogManager.getLogger("academy/discord");

    /** Le tour du fil. Discord ne veut pas plus d'une fiche par quinze secondes. */
    private static final long PERIOD_MILLIS = 1_000L;

    /** Le temps laisse au fil pour effacer la fiche quand le jeu s'arrete. */
    private static final long STOP_TIMEOUT_MILLIS = 2_000L;

    private static final String CONFIG_HINT =
            " Presence Discord : collez l'identifiant de votre application dans [discord] applicationId,"
                    + " dans config/academy-discord.toml (dix-sept a vingt chiffres), ou creez-en un sur"
                    + " https://discord.com/developers/applications";

    /** La fiche voulue, posee par le fil du jeu : d'ou le volatile. */
    private static volatile DiscordPresence wanted;

    /** Vrai tant que le fil tourne. */
    private static volatile boolean running;

    private static Thread worker;

    /** Le client, touche par le fil de service et par lui seul. */
    private static DiscordClient client;
    private static String clientApplicationId = "";

    /** Le dernier etat annonce, et ce qui a deja ete dit : on ne repete pas au journal. */
    private static DiscordClient.State lastState = DiscordClient.State.IDLE;
    private static boolean warnedMissingId;
    private static boolean announcedWaiting;

    private DiscordService() {}

    /**
     * Ouvre la conversation, si les reglages le permettent.
     *
     * <p>Appele une fois, au demarrage du client. Si les reglages ne permettent rien pour
     * l'instant, le fil s'ouvre quand meme : c'est lui qui relit la config, et un identifiant
     * colle en jeu doit prendre effet sans redemarrer.
     */
    public static void start() {
        if (running) return;
        running = true;

        worker = new Thread(DiscordService::loop, "academy-discord");
        worker.setDaemon(true);
        worker.start();

        // A l'arret du jeu, Discord doit voir la fiche disparaitre : sans cela, le profil
        // resterait fige sur « en jeu » jusqu'a ce que Discord s'en apercoive tout seul.
        Runtime.getRuntime().addShutdownHook(new Thread(DiscordService::stop, "academy-discord-stop"));
    }

    /**
     * La fiche a montrer, ou {@code null} pour n'en montrer aucune.
     *
     * <p>Posee depuis le fil du jeu, aussi souvent qu'on veut : le fil de service s'en sert
     * au tour suivant, et ne l'envoie que si elle a change.
     */
    public static void want(@Nullable DiscordPresence presence) {
        wanted = presence;
    }

    /** Le tour du fil de service. */
    private static void loop() {
        while (running) {
            try {
                Thread.sleep(PERIOD_MILLIS);
            } catch (InterruptedException e) {
                // Le jeu s'arrete en pleine attente : il n'y a plus rien a faire.
                Thread.currentThread().interrupt();
                break;
            }

            try {
                followSettings();
                if (client != null) {
                    client.want(wanted);
                    client.tick(System.currentTimeMillis());
                }
                report();
            } catch (Exception e) {
                // Un tour perdu n'est rien : le suivant reprendra tout. Ce qui compte est
                // que ce fil survive, sinon la presence se tairait pour de bon.
                LOGGER.warn("Presence Discord : un tour a echoue, le suivant reprendra.", e);
            }
        }

        // Sortie de boucle : on efface la fiche et on referme le tuyau. C'est ce fil qui
        // possede le client, donc c'est lui qui le ferme — jamais le fil d'arret.
        DiscordClient finished = client;
        client = null;
        if (finished != null) finished.close();
    }

    /**
     * Relit la config, et ouvre ou referme le tuyau en consequence.
     *
     * <p>C'est ce qui applique un identifiant corrige en jeu : des qu'il change, l'ancienne
     * conversation est refermee et une neuve commence.
     */
    private static void followSettings() {
        DiscordConfig.Settings settings = DiscordConfig.read();
        if (!settings.enabled()) {
            closeClient();
            return;
        }

        String applicationId = settings.applicationId();
        if (!DiscordProtocol.looksLikeApplicationId(applicationId)) {
            if (!warnedMissingId) {
                warnedMissingId = true;
                LOGGER.info(CONFIG_HINT);
            }
            closeClient();
            return;
        }

        warnedMissingId = false;
        if (client != null && applicationId.equals(clientApplicationId)) return;

        closeClient();
        clientApplicationId = applicationId;
        client = new DiscordClient(applicationId, ProcessHandle.current().pid(), DiscordPipes::open);
    }

    /** Efface la fiche, et referme le tuyau. */
    private static void closeClient() {
        if (client == null) return;
        client.close();
        client = null;
        clientApplicationId = "";
        lastState = DiscordClient.State.IDLE;
        announcedWaiting = false;
    }

    /** Un mot au journal quand l'etat change : sans cela, un joueur ne saurait pas pourquoi rien n'apparait. */
    private static void report() {
        DiscordClient.State state = client == null ? DiscordClient.State.IDLE : client.state();
        if (state == lastState) return;
        lastState = state;

        switch (state) {
            case CONNECTING -> LOGGER.info("Presence Discord : conversation ouverte avec Discord.");
            case READY -> {
                announcedWaiting = false;
                LOGGER.info("Presence Discord : connecte.");
            }
            case BAD_APPLICATION_ID -> LOGGER.warn(
                    "Presence Discord : Discord a refuse l'identifiant d'application." + CONFIG_HINT);
            case IDLE -> {
                // Rien n'a encore repondu : le client Discord est-il lance ? On le dit une
                // fois, et on se tait ensuite — le fil retente tout seul.
                if (client != null && !announcedWaiting) {
                    announcedWaiting = true;
                    LOGGER.info("Presence Discord : en attente du client Discord, qui n'a pas repondu.");
                }
            }
        }
    }

    /**
     * Arrete le fil, et attend qu'il ait efface la fiche.
     *
     * <p>Appele a l'arret du jeu. Il ne touche pas au tuyau lui-meme : le fil de service en
     * est le seul maitre, et deux fils qui ecrivent en meme temps dans le meme tube
     * melangeraient leurs octets.
     */
    private static void stop() {
        running = false;
        Thread thread = worker;
        if (thread == null) return;
        try {
            thread.join(STOP_TIMEOUT_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
