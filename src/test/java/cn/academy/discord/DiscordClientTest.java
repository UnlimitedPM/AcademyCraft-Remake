package cn.academy.discord;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La conversation avec Discord, relue avec un faux tuyau.
 *
 * <p>C'est tout l'interet d'avoir separe le tuyau du reste : sans Discord lance, et sans
 * attendre quinze secondes, on verifie ce qui ne se voit qu'en jeu — l'attente de la
 * poignee de main, la fiche qui part apres la reponse, le refus d'un mauvais identifiant,
 * et la reconnexion quand Discord disparait.
 */
class DiscordClientTest {

    private static final String ID = "123456789012345678";

    private static final String READY_JSON = "{\"cmd\":\"DISPATCH\",\"evt\":\"READY\",\"data\":{\"v\":1}}";

    /** La reponse de Discord, telle qu'elle arrive vraiment : une trame entiere. */
    private static final byte[] READY =
            DiscordProtocol.frame(DiscordProtocol.OP_FRAME, READY_JSON);

    private static DiscordPresence fiche(String details) {
        return new DiscordPresence(details, "state", 0L, null, null, null, null, List.of());
    }

    /** Un faux tuyau : il garde ce qu'on lui ecrit, et rend ce que Discord dirait. */
    private static final class FakePipe implements DiscordTransport {

        final List<byte[]> written = new ArrayList<>();
        final Deque<byte[]> incoming = new ArrayDeque<>();
        boolean dead;
        boolean closed;

        @Override
        public void write(byte[] frame) throws IOException {
            if (dead) throw new IOException("tuyau mort");
            written.add(frame);
        }

        @Override
        public int read(byte[] buffer) throws IOException {
            if (dead) throw new IOException("tuyau mort");
            if (incoming.isEmpty()) return 0;

            byte[] next = incoming.poll();
            int take = Math.min(next.length, buffer.length);
            System.arraycopy(next, 0, buffer, 0, take);
            if (take < next.length) incoming.addFirst(Arrays.copyOfRange(next, take, next.length));
            return take;
        }

        @Override
        public void close() {
            closed = true;
        }

        /** Le JSON de la derniere trame ecrite. */
        String last() {
            byte[] frame = written.get(written.size() - 1);
            return new String(frame, 8, frame.length - 8, StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("la poignee de main part d'abord, la fiche seulement apres la reponse")
    void laFichePartApresLaReponse() {
        FakePipe pipe = new FakePipe();
        DiscordClient client = new DiscordClient(ID, 42L, () -> pipe);
        client.want(fiche("menu"));

        client.tick(0L);
        assertEquals(1, pipe.written.size(), "la poignee de main est la premiere trame");
        assertEquals(DiscordProtocol.OP_HANDSHAKE, DiscordProtocol.header(pipe.written.get(0), 0).opcode());
        assertTrue(pipe.last().contains(ID));

        client.tick(1_000L);
        assertEquals(1, pipe.written.size(), "sans reponse, un ordre serait refuse");
        assertEquals(DiscordClient.State.CONNECTING, client.state());

        pipe.incoming.add(READY);
        client.tick(2_000L);
        assertEquals(DiscordClient.State.READY, client.state());
        assertEquals(2, pipe.written.size(), "et la fiche suit la reponse");
        assertTrue(pipe.last().contains("menu"));
    }

    @Test
    @DisplayName("une reponse coupee en deux morceaux ne se perd pas")
    void uneReponseCoupeeNeSePerdPas() {
        FakePipe pipe = new FakePipe();
        DiscordClient client = new DiscordClient(ID, 42L, () -> pipe);
        client.tick(0L);

        // Un tube ne rend pas forcement une trame d'un coup. Ici, trois octets, puis le reste.
        pipe.incoming.add(Arrays.copyOfRange(READY, 0, 3));
        client.tick(100L);
        assertEquals(DiscordClient.State.CONNECTING, client.state(), "l'entete seul ne suffit pas");

        pipe.incoming.add(Arrays.copyOfRange(READY, 3, READY.length));
        client.tick(200L);
        assertEquals(DiscordClient.State.READY, client.state());
    }

    @Test
    @DisplayName("un identifiant d'application qui n'en est pas un arrete tout, sans insister")
    void unIdentifiantInvalideArreteTout() {
        FakePipe pipe = new FakePipe();
        DiscordClient client = new DiscordClient("pas-un-identifiant", 42L, () -> pipe);

        client.tick(0L);
        assertEquals(DiscordClient.State.BAD_APPLICATION_ID, client.state());
        assertEquals(0, pipe.written.size(), "inutile d'ouvrir un tuyau pour rien");

        client.tick(1_000_000L);
        assertEquals(0, pipe.written.size(), "et on n'insiste pas : c'est le reglage qu'il faut corriger");
    }

    @Test
    @DisplayName("un refus venu de Discord arrete aussi, et referme le tuyau")
    void unRefusDeDiscordArreteTout() {
        FakePipe pipe = new FakePipe();
        DiscordClient client = new DiscordClient(ID, 42L, () -> pipe);
        client.tick(0L);

        pipe.incoming.add(DiscordProtocol.frame(DiscordProtocol.OP_FRAME,
                "{\"cmd\":\"SET_ACTIVITY\",\"evt\":null,\"data\":{\"code\":4000},\"nonce\":\"n\"}"));
        client.tick(100L);

        assertEquals(DiscordClient.State.BAD_APPLICATION_ID, client.state());
        assertTrue(pipe.closed);
    }

    @Test
    @DisplayName("Discord ferme : on retente plus tard, sans rien casser")
    void discordFermeSeRetente() {
        AtomicInteger attempts = new AtomicInteger();
        DiscordClient client = new DiscordClient(ID, 42L, () -> {
            attempts.incrementAndGet();
            throw new IOException("Discord n'est pas lance");
        });

        client.tick(0L);
        assertEquals(DiscordClient.State.IDLE, client.state());
        assertEquals(1, attempts.get());

        client.tick(5_000L);
        assertEquals(1, attempts.get(), "pas tout de suite : on ne harcèle pas Discord");

        client.tick(10_000L);
        assertEquals(2, attempts.get(), "mais on retente");
    }

    @Test
    @DisplayName("une deuxieme fiche attend les quinze secondes de Discord")
    void uneDeuxiemeFicheAttend() {
        FakePipe pipe = new FakePipe();
        DiscordClient client = new DiscordClient(ID, 42L, () -> pipe);
        client.want(fiche("menu"));
        client.tick(0L);
        pipe.incoming.add(READY);
        client.tick(100L);
        assertEquals(2, pipe.written.size());

        client.want(fiche("solo"));
        client.tick(1_000L);
        assertEquals(2, pipe.written.size(), "trop tot pour Discord");
        assertTrue(pipe.last().contains("menu"), "la fiche posee reste celle du menu");

        client.tick(100L + PresenceGate.MIN_INTERVAL_MILLIS);
        assertEquals(3, pipe.written.size(), "la fenetre est passee : la fiche part");
        assertTrue(pipe.last().contains("solo"));
    }

    @Test
    @DisplayName("un tuyau qui casse se referme, et la fiche repart apres reconnexion")
    void unTuyauQuiCasseSeReconnecte() {
        FakePipe premier = new FakePipe();
        FakePipe second = new FakePipe();
        AtomicInteger opened = new AtomicInteger();
        DiscordClient client = new DiscordClient(ID, 42L,
                () -> opened.getAndIncrement() == 0 ? premier : second);

        client.want(fiche("menu"));
        client.tick(0L);
        premier.incoming.add(READY);
        client.tick(100L);
        assertEquals(DiscordClient.State.READY, client.state());

        premier.dead = true;
        client.tick(200L);
        assertEquals(DiscordClient.State.IDLE, client.state());
        assertTrue(premier.closed, "le tuyau casse est referme");

        client.tick(10_200L);
        assertEquals(2, opened.get(), "Discord revient : on retente");
        second.incoming.add(READY);
        client.tick(10_300L);

        assertEquals(DiscordClient.State.READY, client.state());
        assertTrue(second.last().contains("menu"),
                "la fiche repart sur la conversation neuve, sans attendre la fenetre");
    }

    @Test
    @DisplayName("a l'arret du jeu, la fiche est effacee")
    void aLArretLaFicheEstEffacee() {
        FakePipe pipe = new FakePipe();
        DiscordClient client = new DiscordClient(ID, 42L, () -> pipe);
        client.tick(0L);
        pipe.incoming.add(READY);
        client.tick(100L);

        client.close();

        assertTrue(pipe.closed);
        assertTrue(pipe.last().contains("\"activity\":null"),
                "sans cela, Discord garderait « en jeu » apres la fermeture : " + pipe.last());
    }
}
