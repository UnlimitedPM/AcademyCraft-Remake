package cn.academy.platform.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Reseau du mod, neutre de chargeur.
 *
 * <p>Les paquets vivent dans {@code common/} et ne parlent que de {@link FriendlyByteBuf} ; chaque
 * chargeur fournit un transport qui les envoie et les recoit. Forge utilise un {@code SimpleChannel},
 * Fabric ses charges utiles personnalisees.
 *
 * <p>Chaque message est declare par un identifiant stable (le meme dans tous les chargeurs), une
 * fonction d'ecriture, une fonction de lecture, et un gestionnaire cote serveur ou cote client. Un
 * message peut etre declare une seule fois par direction.
 */
public final class Network {

    /** Ecriture et lecture d'un message. */
    public interface Codec<T> {
        void encode(T message, FriendlyByteBuf buffer);

        T decode(FriendlyByteBuf buffer);
    }

    /** Gestionnaire cote serveur : le joueur qui a envoye le message est fourni. */
    public interface ServerHandler<T> {
        void handle(T message, ServerPlayer sender);
    }

    /** Gestionnaire cote client. Il tourne sur le fil du client. */
    public interface ClientHandler<T> {
        void handle(T message);
    }

    /** Ce qu'un chargeur doit implementer pour transporter les messages. */
    public interface Transport {
        void registerServerbound(String id, Class<?> type, Codec<Object> codec,
                                 ServerHandler<Object> handler);

        void registerClientbound(String id, Class<?> type, Codec<Object> codec,
                                 ClientHandler<Object> handler);

        void sendToServer(Object message);

        void sendToPlayer(ServerPlayer player, Object message);

        void sendToAll(Object message);

        /**
         * Aux joueurs qui voient cette entite, plus elle-meme s'il s'agit d'un joueur.
         *
         * <p>C'est le {@code TRACKING_ENTITY_AND_SELF} de Forge. Un effet qui nait autour d'une
         * victime doit se voir la ou elle est, pas dans tout le serveur.
         */
        void sendToTracking(Object message, net.minecraft.world.entity.Entity entity);
    }

    /** Une declaration, pour retrouver l'identifiant et le codec d'un message a l'envoi. */
    private record Entry(String id, Codec<Object> codec) {
    }

    private static final Map<Class<?>, Entry> BY_TYPE = new LinkedHashMap<>();

    private static Transport transport = new Transport() {
        @Override
        public void registerServerbound(String id, Class<?> type, Codec<Object> codec,
                                        ServerHandler<Object> handler) {
        }

        @Override
        public void registerClientbound(String id, Class<?> type, Codec<Object> codec,
                                        ClientHandler<Object> handler) {
        }

        @Override
        public void sendToServer(Object message) {
        }

        @Override
        public void sendToPlayer(ServerPlayer player, Object message) {
        }

        @Override
        public void sendToAll(Object message) {
        }

        @Override
        public void sendToTracking(Object message, net.minecraft.world.entity.Entity entity) {
        }
    };

    private Network() {
    }

    public static void install(Transport value) {
        transport = value;
    }

    /** Declare un message client vers serveur. */
    public static <T> void serverbound(String id, Class<T> type, Codec<T> codec,
                                       ServerHandler<T> handler) {
        BY_TYPE.put(type, new Entry(id, cast(codec)));
        transport.registerServerbound(id, type, cast(codec),
                (message, sender) -> handler.handle(type.cast(message), sender));
    }

    /** Declare un message serveur vers client. */
    public static <T> void clientbound(String id, Class<T> type, Codec<T> codec,
                                       ClientHandler<T> handler) {
        BY_TYPE.put(type, new Entry(id, cast(codec)));
        transport.registerClientbound(id, type, cast(codec),
                message -> handler.handle(type.cast(message)));
    }

    public static void sendToServer(Object message) {
        transport.sendToServer(message);
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        transport.sendToPlayer(player, message);
    }

    public static void sendToAll(Object message) {
        transport.sendToAll(message);
    }

    /** Aux joueurs qui voient l'entite (et a elle, si c'est un joueur). */
    public static void sendToTracking(Object message, net.minecraft.world.entity.Entity entity) {
        if (entity == null) return;
        transport.sendToTracking(message, entity);
    }

    /** Le meme, dans l'ordre de Forge : l'entite d'abord, le message ensuite. */
    public static void sendToTracking(net.minecraft.world.entity.Entity entity, Object message) {
        sendToTracking(message, entity);
    }

    // ------------------------------------------------------------------
    // Ce dont le transport a besoin pour (de)serialiser
    // ------------------------------------------------------------------

    /** L'identifiant d'un message deja declare, ou {@code null}. */
    public static String idOf(Object message) {
        Entry entry = BY_TYPE.get(message.getClass());
        return entry == null ? null : entry.id();
    }

    /** Ecrit le message avec son codec declare. */
    public static void encode(Object message, FriendlyByteBuf buffer) {
        Entry entry = BY_TYPE.get(message.getClass());
        if (entry == null) throw new IllegalArgumentException(
                "Message non declare : " + message.getClass().getName());
        entry.codec().encode(message, buffer);
    }

    /** Lit un message a partir de son identifiant deja lu par le transport. */
    public static Object decode(String id, FriendlyByteBuf buffer) {
        for (Entry entry : BY_TYPE.values()) {
            if (entry.id().equals(id)) return entry.codec().decode(buffer);
        }
        throw new IllegalArgumentException("Message inconnu : " + id);
    }

    /** Les types declares dans une direction, avec leur identifiant et codec. */
    public static Map<String, Codec<Object>> codecs() {
        Map<String, Codec<Object>> map = new LinkedHashMap<>();
        BY_TYPE.values().forEach(entry -> map.put(entry.id(), entry.codec()));
        return map;
    }

    @SuppressWarnings("unchecked")
    private static Codec<Object> cast(Codec<?> codec) {
        return (Codec<Object>) codec;
    }

    /** Utilitaire : codec construit a partir de deux methodes statiques. */
    public static <T> Codec<T> codec(BiConsumer<T, FriendlyByteBuf> encoder,
                                     Function<FriendlyByteBuf, T> decoder) {
        return new Codec<>() {
            @Override
            public void encode(T message, FriendlyByteBuf buffer) {
                encoder.accept(message, buffer);
            }

            @Override
            public T decode(FriendlyByteBuf buffer) {
                return decoder.apply(buffer);
            }
        };
    }
}
