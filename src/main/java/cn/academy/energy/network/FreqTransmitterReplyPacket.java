package cn.academy.energy.network;

import java.util.function.Supplier;

import cn.academy.ability.network.AbilityNetwork;
import cn.academy.terminal.client.FreqTransmitterClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

/**
 * La reponse du serveur a l'Emetteur de frequence : ce qu'etait le bloc, et si l'action a abouti.
 *
 * <p>{@code result} vaut {@code "ok"} ou la cle de l'erreur a dire au joueur ({@code e0} a
 * {@code e4}, celles de l'original).
 */
public class FreqTransmitterReplyPacket {

    private final FreqTransmitterRequestPacket.Action action;
    private final int kind;
    private final BlockPos pos;
    private final String name;
    private final boolean encrypted;
    private final String result;

    public FreqTransmitterReplyPacket(FreqTransmitterRequestPacket.Action action, int kind,
                                      BlockPos pos, String name, boolean encrypted, String result) {
        this.action = action;
        this.kind = kind;
        this.pos = pos;
        this.name = name == null ? "" : name;
        this.encrypted = encrypted;
        this.result = result;
    }

    public FreqTransmitterRequestPacket.Action action() { return action; }

    /** 1 un Matrix, 2 un noeud, 0 rien de cela. */
    public int kind() { return kind; }

    public BlockPos pos() { return pos; }

    public String name() { return name; }

    public boolean encrypted() { return encrypted; }

    public String result() { return result; }

    public static void send(ServerPlayer player, FreqTransmitterRequestPacket.Action action,
                            int kind, BlockPos pos, String name, boolean encrypted, String result) {
        AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new FreqTransmitterReplyPacket(action, kind, pos, name, encrypted, result));
    }

    public static void encode(FreqTransmitterReplyPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeVarInt(msg.kind);
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.name, 64);
        buf.writeBoolean(msg.encrypted);
        buf.writeUtf(msg.result, 8);
    }

    public static FreqTransmitterReplyPacket decode(FriendlyByteBuf buf) {
        return new FreqTransmitterReplyPacket(buf.readEnum(FreqTransmitterRequestPacket.Action.class),
                buf.readVarInt(), buf.readBlockPos(), buf.readUtf(64), buf.readBoolean(),
                buf.readUtf(8));
    }

    public static void handle(FreqTransmitterReplyPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> FreqTransmitterClient.onReply(msg));
        ctx.get().setPacketHandled(true);
    }
}
