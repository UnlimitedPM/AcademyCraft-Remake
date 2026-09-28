package cn.academy.discord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le protocole de Discord, relu sans Discord.
 *
 * <p>C'est la partie ou une erreur ne dirait rien : une trame mal formee n'afficherait
 * simplement aucune fiche, sans message et sans plantage. Les octets sont donc verifies un
 * par un.
 */
class DiscordProtocolTest {

    private static final String ID = "123456789012345678";

    @Test
    @DisplayName("l'entete est en petit-boutiste : opcode, puis longueur")
    void lEnteteEstEnPetitBoutiste() {
        byte[] frame = DiscordProtocol.frame(DiscordProtocol.OP_FRAME, "{\"a\":1}");

        // Sept octets de JSON. L'opcode et la longueur tiennent chacun sur quatre octets,
        // et c'est le premier qui porte le poids faible.
        assertEquals(1, frame[0]);
        assertEquals(0, frame[1]);
        assertEquals(0, frame[2]);
        assertEquals(0, frame[3]);
        assertEquals(7, frame[4]);
        assertEquals(0, frame[7]);

        DiscordProtocol.Header header = DiscordProtocol.header(frame, 0);
        assertEquals(DiscordProtocol.OP_FRAME, header.opcode());
        assertEquals(7, header.length());
    }

    @Test
    @DisplayName("une trame n'est lue que lorsqu'elle est entiere")
    void uneTrameNestLueQuUneFoisEntiere() {
        byte[] frame = DiscordProtocol.handshake(ID);

        assertNull(DiscordProtocol.read(frame, 4), "l'entete seul ne suffit pas");
        assertNull(DiscordProtocol.read(frame, frame.length - 1), "le JSON doit etre complet");

        DiscordProtocol.Message message = DiscordProtocol.read(frame, frame.length);
        assertNotNull(message);
        assertEquals(DiscordProtocol.OP_HANDSHAKE, message.opcode());
        assertEquals(frame.length, message.length(), "la longueur sert a retirer la trame du tampon");
    }

    @Test
    @DisplayName("la poignee de main porte la version et l'identifiant")
    void laPoigneeDeMainPorteLIdentifiant() {
        JsonObject json = json(DiscordProtocol.handshake(ID));

        assertEquals(1, json.get("v").getAsInt());
        assertEquals(ID, json.get("client_id").getAsString());
    }

    @Test
    @DisplayName("l'ordre SET_ACTIVITY porte le processus, la fiche et un nonce")
    void lOrdrePorteLaFiche() {
        DiscordPresence presence = new DiscordPresence("details", "state", 1_700_000_000_000L,
                "academy", "AcademyCraft Remake 0.0.1", "menu", "Main menu", List.of());

        JsonObject json = json(DiscordProtocol.setActivity(4242L, presence, "nonce-1"));

        assertEquals("SET_ACTIVITY", json.get("cmd").getAsString());
        assertEquals("nonce-1", json.get("nonce").getAsString());

        JsonObject args = json.getAsJsonObject("args");
        assertEquals(4242L, args.get("pid").getAsLong());

        JsonObject activity = args.getAsJsonObject("activity");
        assertEquals("details", activity.get("details").getAsString());
        assertEquals("state", activity.get("state").getAsString());
        assertEquals(1_700_000_000_000L,
                activity.getAsJsonObject("timestamps").get("start").getAsLong());
        assertEquals("academy", activity.getAsJsonObject("assets").get("large_image").getAsString());
        assertEquals("menu", activity.getAsJsonObject("assets").get("small_image").getAsString());
        assertFalse(activity.get("instance").getAsBoolean());
    }

    @Test
    @DisplayName("les champs vides ne partent pas, et une ligne trop longue est coupee")
    void lesChampsVidesNePartentPas() {
        DiscordPresence pauvre = new DiscordPresence(null, null, 0L, null, null, null, null, List.of());
        JsonObject activity = activity(pauvre);

        assertFalse(activity.has("details"), "une ligne nulle ne part pas");
        assertFalse(activity.has("timestamps"), "sans debut de session, pas d'horloge");
        assertFalse(activity.has("assets"), "sans image, pas de bloc d'images");
        assertFalse(activity.has("buttons"), "sans lien, pas de boutons");

        String trop = "x".repeat(DiscordProtocol.MAX_TEXT + 50);
        DiscordPresence longue = new DiscordPresence(trop, null, 0L, null, null, null, null, List.of());
        assertEquals(DiscordProtocol.MAX_TEXT, activity(longue).get("details").getAsString().length(),
                "Discord refuse plus de 128 caracteres : la ligne est coupee avant de partir");
    }

    @Test
    @DisplayName("effacer la fiche, c'est envoyer une activite nulle")
    void effacerEnvoieUneActiviteNulle() {
        JsonObject json = json(DiscordProtocol.setActivity(4242L, null, "nonce"));

        assertTrue(json.getAsJsonObject("args").get("activity").isJsonNull());
    }

    @Test
    @DisplayName("les erreurs de Discord se lisent, et un evenement n'en est pas une")
    void lesErreursSeLisent() {
        DiscordProtocol.Message erreur = message(
                "{\"cmd\":\"SET_ACTIVITY\",\"evt\":null,\"data\":{\"code\":4000,\"message\":\"Invalid Client ID\"},\"nonce\":\"n\"}");
        assertEquals(DiscordProtocol.ERR_INVALID_APPLICATION, DiscordProtocol.errorCode(erreur));
        assertFalse(DiscordProtocol.isReady(erreur));

        DiscordProtocol.Message pret = message("{\"cmd\":\"DISPATCH\",\"evt\":\"READY\",\"data\":{\"v\":1}}");
        assertEquals(0, DiscordProtocol.errorCode(pret), "un evenement n'est pas une erreur");
        assertTrue(DiscordProtocol.isReady(pret));

        assertEquals(0, DiscordProtocol.errorCode(message("pas du json")));
    }

    @Test
    @DisplayName("un identifiant d'application est un snowflake")
    void unIdentifiantEstUnSnowflake() {
        assertTrue(DiscordProtocol.looksLikeApplicationId(ID));
        assertFalse(DiscordProtocol.looksLikeApplicationId(null));
        assertFalse(DiscordProtocol.looksLikeApplicationId(""));
        assertFalse(DiscordProtocol.looksLikeApplicationId("AcademyCraft"));
        assertFalse(DiscordProtocol.looksLikeApplicationId("12345"));
    }

    private static DiscordProtocol.Message seuleTrame(byte[] frame) {
        DiscordProtocol.Message message = DiscordProtocol.read(frame, frame.length);
        assertNotNull(message, "la trame doit se relire");
        return message;
    }

    private static JsonObject json(byte[] frame) {
        return JsonParser.parseString(seuleTrame(frame).json()).getAsJsonObject();
    }

    private static JsonObject activity(DiscordPresence presence) {
        return json(DiscordProtocol.setActivity(1L, presence, "nonce"))
                .getAsJsonObject("args").getAsJsonObject("activity");
    }

    private static DiscordProtocol.Message message(String json) {
        return new DiscordProtocol.Message(DiscordProtocol.OP_FRAME, json, json.length());
    }
}
