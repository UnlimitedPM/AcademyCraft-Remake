package cn.academy.fabric;

import cn.academy.AcademyCraftCommon;
import cn.academy.platform.network.Network;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Transport Fabric : un seul canal, l'identifiant du message en tete.
 *
 * <p>Fabric 1.20.1 n'a pas de registre de charges utiles typees : le canal porte des octets, et
 * c'est ce transport qui precede chaque message de son identifiant avant de le confier au codec
 * commun. Les gestionnaires sont donc les memes que sous Forge.
 */
public final class FabricNetworkTransport implements Network.Transport {

    private static final ResourceLocation CHANNEL =
            new ResourceLocation(AcademyCraftCommon.MOD_ID, "main");

    private final Map<String, Network.ServerHandler<Object>> serverHandlers = new LinkedHashMap<>();
    private final Map<String, Network.ClientHandler<Object>> clientHandlers = new LinkedHashMap<>();

    private static final FabricNetworkTransport INSTANCE = new FabricNetworkTransport();

    /** Cote serveur : recoit les messages des joueurs. */
    static void installServer() {
        Network.install(INSTANCE);
        INSTANCE.receiveServerbound();
    }

    /** Cote client : recoit les messages du serveur et sait lui en envoyer. */
    @Environment(EnvType.CLIENT)
    public static void installClient() {
        Network.install(INSTANCE);
        INSTANCE.receiveClientbound();
    }

    private FabricNetworkTransport() {
    }

    private void receiveServerbound() {
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL,
                (server, player, handler, buf, responseSender) -> {
                    String id = buf.readUtf(128);
                    Object message = Network.decode(id, buf);
                    Network.ServerHandler<Object> deliver = serverHandlers.get(id);
                    if (deliver == null || message == null) return;
                    server.execute(() -> deliver.handle(message, player));
                });
    }

    @Environment(EnvType.CLIENT)
    private void receiveClientbound() {
        ClientPlayNetworking.registerGlobalReceiver(CHANNEL,
                (client, handler, buf, responseSender) -> {
                    String id = buf.readUtf(128);
                    Object message = Network.decode(id, buf);
                    Network.ClientHandler<Object> deliver = clientHandlers.get(id);
                    if (deliver == null || message == null) return;
                    Minecraft.getInstance().execute(() -> deliver.handle(message));
                });
    }

    @Override
    public void registerServerbound(String id, Class<?> type, Network.Codec<Object> codec,
                                    Network.ServerHandler<Object> handler) {
        serverHandlers.put(id, handler);
    }

    @Override
    public void registerClientbound(String id, Class<?> type, Network.Codec<Object> codec,
                                    Network.ClientHandler<Object> handler) {
        clientHandlers.put(id, handler);
    }

    @Override
    public void sendToServer(Object message) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(Network.idOf(message));
        Network.encode(message, buf);
        ClientPlayNetworking.send(CHANNEL, buf);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, Object message) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(Network.idOf(message));
        Network.encode(message, buf);
        ServerPlayNetworking.send(player, CHANNEL, buf);
    }

    @Override
    public void sendToAll(Object message) {
        throw new UnsupportedOperationException(
                "sendToAll n'a pas encore d'equivalent Fabric dans ce transport");
    }
}
