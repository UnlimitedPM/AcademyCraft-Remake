package cn.academy.discord;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * La conversation avec Discord, sans Minecraft.
 *
 * <p>Elle ne fait que trois choses : ouvrir le tuyau quand c'est possible, lire ce que Discord
 * repond, et poser la fiche voulue quand le rythme le permet. Tout ce qui vient du jeu passe
 * par {@link #want}, pose depuis le fil du client : ce fil-la ne doit JAMAIS attendre apres
 * un tuyau, sinon le jeu s'arrete le temps d'une reponse.
 *
 * <p>Rien n'est obligatoire ici, et rien ne doit faire tomber le jeu : Discord absent, Discord
 * ferme en cours de route, identifiant d'application errone — tout se solde par une nouvelle
 * tentative plus tard, ou par un arret tranquille.
 *
 * <p>Le temps est passe en parametre et le tuyau vient d'une fabrique : la conversation
 * entiere se relit donc en JUnit, avec un faux tuyau et une horloge qu'on avance a la main.
 * C'est la seule facon de verifier ce qui ne se voit qu'en jeu, et seulement quand Discord
 * est ouvert.
 */
public final class DiscordClient {

    /** Ou en est la conversation : de quoi tenir un journal utile, et savoir s'il faut insister. */
    public enum State {
        /** Rien n'est ouvert : Discord n'est pas lance, ou la derniere tentative a echoue. */
        IDLE,
        /** Le tuyau est ouvert et la poignee de main est partie : on attend la reponse. */
        CONNECTING,
        /** Discord a repondu : la fiche peut partir. */
        READY,
        /** L'identifiant d'application ne vaut rien : insister ne servirait a rien. */
        BAD_APPLICATION_ID
    }

    /** Ouvre un tuyau neuf : appele a chaque tentative de connexion. */
    public interface TransportFactory {
        DiscordTransport open() throws Exception;
    }

    /** Delai entre deux tentatives quand Discord n'est pas la. */
    private static final long RETRY_MILLIS = 10_000L;

    /** Delai maximal pour la reponse a la poignee de main. */
    private static final long HANDSHAKE_TIMEOUT_MILLIS = 5_000L;

    private final String applicationId;
    private final long pid;
    private final TransportFactory transports;
    private final PresenceGate gate = new PresenceGate();

    /** Ce qui est arrive de Discord, et ce qu'on en a deja lu. */
    private final byte[] incoming = new byte[8192];
    private final byte[] chunk = new byte[1024];
    private int incomingSize;

    private DiscordTransport transport;

    /** La fiche voulue, posee par le fil du jeu : d'ou le volatile. */
    private volatile DiscordPresence wanted;

    private volatile State state = State.IDLE;

    private long attemptedAt;

    /** Vrai des la premiere tentative : sans ce drapeau, le tout premier tour serait compte
     *  comme « trop tot » (le delai se compte depuis l'heure zero), et rien ne s'ouvrirait
     *  jamais tant que l'horloge du systeme n'aurait pas passe le delai. */
    private boolean attempted;

    private long handshakeAt;

    public DiscordClient(String applicationId, long pid, TransportFactory transports) {
        this.applicationId = applicationId;
        this.pid = pid;
        this.transports = transports;
    }

    /**
     * La fiche a poser.
     *
     * <p>Appele depuis le fil du jeu, aussi souvent qu'on veut : seule la fiche qui differe de
     * la derniere envoyee partira, et jamais plus d'une fois toutes les quinze secondes.
     */
    public void want(@Nullable DiscordPresence presence) {
        this.wanted = presence;
    }

    /** Ou en est la conversation. */
    public State state() {
        return state;
    }

    /**
     * Un tour de conversation : retenter la connexion, lire les reponses, poser la fiche due.
     *
     * <p>A appeler une fois par seconde, depuis son propre fil.
     */
    public void tick(long nowMillis) {
        // Inutile d'insister sur un identifiant qui ne vaut rien : c'est le reglage qu'il
        // faut corriger, et le journal le dit une fois.
        if (state == State.BAD_APPLICATION_ID) return;

        if (transport == null) {
            if (attempted && nowMillis - attemptedAt < RETRY_MILLIS) return;
            attempted = true;
            attemptedAt = nowMillis;
            open(nowMillis);
            return;
        }

        if (!pump()) {
            // Discord a ferme, ou le tuyau s'est casse : on refermera proprement, et la
            // prochaine tentative repartira du debut.
            closeTransport();
            state = State.IDLE;
            return;
        }

        if (state == State.CONNECTING && nowMillis - handshakeAt > HANDSHAKE_TIMEOUT_MILLIS) {
            // Discord n'a pas repondu a la poignee de main : ce n'est pas la peine de
            // garder ce tuyau ouvert, il ne dira rien.
            closeTransport();
            state = State.IDLE;
            return;
        }

        send(nowMillis);
    }

    /**
     * Efface la fiche et ferme le tuyau.
     *
     * <p>A appeler a l'arret du jeu : sans cela, Discord garderait « en jeu » jusqu'a ce qu'il
     * s'apercoive tout seul que le jeu n'est plus la.
     */
    public void close() {
        if (transport != null) {
            try {
                transport.write(DiscordProtocol.setActivity(pid, null, UUID.randomUUID().toString()));
            } catch (Exception ignored) {
                // Discord est peut-etre deja parti : il n'y a rien a faire, et rien a dire.
            }
            closeTransport();
        }
        state = State.IDLE;
    }

    private void open(long nowMillis) {
        if (!DiscordProtocol.looksLikeApplicationId(applicationId)) {
            state = State.BAD_APPLICATION_ID;
            return;
        }

        try {
            transport = transports.open();
            transport.write(DiscordProtocol.handshake(applicationId));
            // Une conversation neuve a sa propre fenetre : la fiche doit repartir tout de suite.
            gate.reset();
            incomingSize = 0;
            handshakeAt = nowMillis;
            state = State.CONNECTING;
        } catch (Exception e) {
            closeTransport();
            state = State.IDLE;
        }
    }

    /** Lit ce qui est arrive, et traite les trames entieres. Rend faux si le tuyau est mort. */
    private boolean pump() {
        try {
            // Borne : une rafale ne doit pas faire tourner cette boucle sans fin, et une
            // trame plus grosse que le tampon se reprend au tour suivant.
            for (int burst = 0; burst < 16; burst++) {
                int read = transport.read(chunk);
                if (read < 0) return false;
                if (read == 0) break;
                if (!append(read)) break;
            }
        } catch (Exception e) {
            return false;
        }

        DiscordProtocol.Message message;
        while ((message = DiscordProtocol.read(incoming, incomingSize)) != null) {
            consume(message);
        }
        return true;
    }

    /** Ajoute un morceau au tampon, ou rend faux s'il n'y a plus de place. */
    private boolean append(int length) {
        if (incomingSize + length > incoming.length) return false;
        System.arraycopy(chunk, 0, incoming, incomingSize, length);
        incomingSize += length;
        return true;
    }

    private void consume(DiscordProtocol.Message message) {
        // La trame est traitee : on la retire du tampon avant tout, pour que la suite de la
        // lecture reparte sur la trame suivante meme si le traitement se plaint.
        System.arraycopy(incoming, message.length(), incoming, 0, incomingSize - message.length());
        incomingSize -= message.length();

        if (DiscordProtocol.isReady(message)) {
            state = State.READY;
            return;
        }

        if (DiscordProtocol.errorCode(message) == DiscordProtocol.ERR_INVALID_APPLICATION) {
            closeTransport();
            state = State.BAD_APPLICATION_ID;
        }
    }

    private void send(long nowMillis) {
        if (state != State.READY) return;

        DiscordPresence due = gate.due(wanted, nowMillis);
        if (due == null) return;

        try {
            transport.write(DiscordProtocol.setActivity(pid, due, UUID.randomUUID().toString()));
        } catch (Exception e) {
            closeTransport();
            state = State.IDLE;
        }
    }

    /** Ferme le tuyau sans toucher a l'etat : c'est l'appelant qui sait ou on en est. */
    private void closeTransport() {
        if (transport == null) return;
        transport.close();
        transport = null;
        incomingSize = 0;
    }
}
