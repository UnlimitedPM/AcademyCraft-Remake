package cn.academy.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Le protocole de Discord, ecrit a la main.
 *
 * <p>Le client Discord ouvre un tube nomme (Windows) ou une socket locale (Linux, macOS) et
 * parle en trames : quatre octets d'opcode, quatre octets de longueur, puis du JSON en UTF-8.
 * Les deux entiers sont en petit-boutiste. Il n'y a rien de plus : une poignee de main, des
 * ordres, et des reponses.
 *
 * <p>Aucune classe de Minecraft ni de systeme ici : c'est la partie ou une erreur ne se
 * verrait qu'en jeu, sans le moindre message, donc c'est celle qui se relit en JUnit.
 *
 * <p>Pourquoi a la main plutot qu'avec une bibliotheque : le mod n'a aucune dependance
 * (hors JUnit, pour les tests), et Java parle deja aux deux systemes — par JNA, que Minecraft
 * livre, pour le tube de Windows, et par la socket locale du JDK pour le reste. Ajouter un
 * jar au mod pour deux cents lignes de protocole ne se justifiait pas.
 */
public final class DiscordProtocol {

    /** La poignee de main : la version du protocole, puis l'identifiant de l'application. */
    public static final int OP_HANDSHAKE = 0;

    /** Une trame ordinaire : nos ordres, et les reponses de Discord. */
    public static final int OP_FRAME = 1;

    /** Discord ferme la conversation. */
    public static final int OP_CLOSE = 2;

    /** Discord demande si on est encore la. */
    public static final int OP_PING = 3;

    /** Et la reponse a cette question. */
    public static final int OP_PONG = 4;

    /** Discord refuse plus de 128 caracteres sur chacune des deux lignes. */
    public static final int MAX_TEXT = 128;

    /**
     * Le code d'erreur de l'application inconnue.
     *
     * <p>Insister ne sert a rien : c'est le reglage qu'il faut corriger, et Discord le dit.
     */
    public static final int ERR_INVALID_APPLICATION = 4000;

    private DiscordProtocol() {}

    /**
     * Un identifiant d'application Discord est un « snowflake » : dix-sept a vingt chiffres.
     *
     * <p>Le verifier evite la seule erreur qu'un joueur peut vraiment faire en recopiant son
     * identifiant depuis le portail des developpeurs — coller un nom, ou un lien.
     */
    public static boolean looksLikeApplicationId(String value) {
        return value != null && value.matches("\\d{17,20}");
    }

    /** La poignee de main, prete a ecrire. */
    public static byte[] handshake(String applicationId) {
        JsonObject json = new JsonObject();
        json.addProperty("v", 1);
        json.addProperty("client_id", applicationId);
        return frame(OP_HANDSHAKE, json.toString());
    }

    /**
     * L'ordre qui pose la fiche — ou qui l'efface, quand {@code presence} est nulle.
     *
     * <p>Discord attend aussi le numero du processus qui joue, faute de quoi la fiche se
     * disputerait avec celle d'une autre application.
     */
    public static byte[] setActivity(long pid, DiscordPresence presence, String nonce) {
        JsonObject args = new JsonObject();
        args.addProperty("pid", pid);
        args.add("activity", presence == null ? null : activity(presence));

        JsonObject json = new JsonObject();
        json.addProperty("cmd", "SET_ACTIVITY");
        json.add("args", args);
        json.addProperty("nonce", nonce);
        return frame(OP_FRAME, json.toString());
    }

    private static JsonObject activity(DiscordPresence presence) {
        JsonObject activity = new JsonObject();
        putText(activity, "details", presence.details());
        putText(activity, "state", presence.state());

        if (presence.startMillis() > 0L) {
            JsonObject timestamps = new JsonObject();
            timestamps.addProperty("start", presence.startMillis());
            activity.add("timestamps", timestamps);
        }

        JsonObject assets = new JsonObject();
        putText(assets, "large_image", presence.largeImage());
        putText(assets, "large_text", presence.largeText());
        putText(assets, "small_image", presence.smallImage());
        putText(assets, "small_text", presence.smallText());
        // Par size() : Minecraft livre Gson 2.10, dont JsonObject n'a PAS isEmpty().
        if (assets.size() > 0) activity.add("assets", assets);

        if (!presence.buttons().isEmpty()) {
            JsonArray buttons = new JsonArray();
            for (DiscordPresence.Button button : presence.buttons()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("label", trim(button.label()));
                entry.addProperty("url", button.url());
                buttons.add(entry);
            }
            activity.add("buttons", buttons);
        }

        activity.addProperty("instance", false);
        return activity;
    }

    /** Une ligne de la fiche, coupee a la longueur que Discord accepte. */
    private static void putText(JsonObject json, String key, String value) {
        if (value == null || value.isBlank()) return;
        json.addProperty(key, trim(value));
    }

    private static String trim(String value) {
        return value.length() <= MAX_TEXT ? value : value.substring(0, MAX_TEXT);
    }

    /** Une trame complete : l'entete, puis le JSON en UTF-8. */
    public static byte[] frame(int opcode, String json) {
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(8 + payload.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(opcode);
        buffer.putInt(payload.length);
        buffer.put(payload);
        return buffer.array();
    }

    /** L'opcode et la longueur d'un entete : les deux entiers sont en petit-boutiste. */
    public static Header header(byte[] data, int offset) {
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, 8).order(ByteOrder.LITTLE_ENDIAN);
        return new Header(buffer.getInt(), buffer.getInt());
    }

    /** L'entete d'une trame : son opcode, et la longueur de ce qui suit. */
    public record Header(int opcode, int length) {}

    /**
     * Une trame recue : son opcode, son JSON, et sa longueur TOTALE en octets.
     *
     * <p>La longueur en octets est celle qui permet de retirer la trame du tampon : le texte
     * decode ne dit pas combien d'octets il occupait.
     */
    public record Message(int opcode, String json, int length) {}

    /**
     * La premiere trame entiere du tampon, ou {@code null} s'il n'y en a pas encore une.
     *
     * <p>Un tube ne rend pas forcement une trame d'un coup : l'entete peut arriver seul, et
     * le JSON en deux morceaux. C'est pour cela que la lecture passe par ici, plutot que de
     * decouper a l'aveugle.
     */
    public static Message read(byte[] data, int size) {
        if (size < 8) return null;

        Header header = header(data, 0);
        if (header.length() < 0 || header.length() > size - 8) return null;

        String json = new String(data, 8, header.length(), StandardCharsets.UTF_8);
        return new Message(header.opcode(), json, 8 + header.length());
    }

    /**
     * Le code d'erreur d'une reponse, ou zero quand tout va bien.
     *
     * <p>Discord repond a chaque ordre : soit une erreur, soit l'evenement demande. On ne lit
     * que le code, parce que c'est la seule chose qui nous apprenne quelque chose — et une
     * reponse incomprehensible ne doit pas faire tomber le jeu.
     */
    public static int errorCode(Message message) {
        JsonObject json = object(message);
        if (json == null) return 0;
        if (json.has("evt") && !json.get("evt").isJsonNull()) return 0;

        JsonElement data = json.get("data");
        if (data == null || !data.isJsonObject()) return 0;
        JsonElement code = data.getAsJsonObject().get("code");
        return code == null || !code.isJsonPrimitive() ? 0 : code.getAsInt();
    }

    /** Vrai quand Discord annonce que la conversation est ouverte. */
    public static boolean isReady(Message message) {
        JsonObject json = object(message);
        if (json == null) return false;
        JsonElement event = json.get("evt");
        return event != null && event.isJsonPrimitive() && "READY".equals(event.getAsString());
    }

    private static JsonObject object(Message message) {
        if (message == null || message.json() == null || message.json().isBlank()) return null;
        try {
            JsonElement parsed = JsonParser.parseString(message.json());
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
