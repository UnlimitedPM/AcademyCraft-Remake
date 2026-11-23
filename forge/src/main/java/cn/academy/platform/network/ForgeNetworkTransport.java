package cn.academy.platform.network;

import cn.academy.AcademyCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Transport Forge/NeoForge : un seul canal, et un identifiant par message.
 *
 * <p>Les paquets eux-memes sont communs : ce transport ne fait que les enregistrer aupres du
 * {@code SimpleChannel} et les envoyer.
 */
public final class ForgeNetworkTransport implements Network.Transport {

    private static final String PROTOCOL = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AcademyCraft.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static int nextId;

    @Override
    public void registerServerbound(String id, Class<?> type, Network.Codec<Object> codec,
                                    Network.ServerHandler<Object> handler) {
        CHANNEL.registerMessage(nextId++, (Class<Object>) type,
                codec::encode, codec::decode,
                (message, ctx) -> {
                    ctx.get().enqueueWork(() -> handler.handle(message, ctx.get().getSender()));
                    ctx.get().setPacketHandled(true);
                });
    }

    @Override
    public void registerClientbound(String id, Class<?> type, Network.Codec<Object> codec,
                                    Network.ClientHandler<Object> handler) {
        CHANNEL.registerMessage(nextId++, (Class<Object>) type,
                codec::encode, codec::decode,
                (message, ctx) -> {
                    ctx.get().enqueueWork(() -> handler.handle(message));
                    ctx.get().setPacketHandled(true);
                });
    }

    @Override
    public void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    @Override
    public void sendToAll(Object message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }
}
