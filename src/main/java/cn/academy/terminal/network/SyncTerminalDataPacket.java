package cn.academy.terminal.network;

import cn.academy.ability.network.AbilityNetwork;
import cn.academy.terminal.TerminalData;
import cn.academy.terminal.client.ClientTerminalData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C : pousse l'etat du terminal au client qui le possede.
 *
 * Meme canal que les donnees d'aptitudes ({@link AbilityNetwork#CHANNEL}) : c'est
 * le canal des donnees de joueur du mod, et ouvrir un second canal pour un
 * drapeau et une liste de noms ne gagnerait rien.
 */
public class SyncTerminalDataPacket {

    private final CompoundTag data;

    /** Vrai quand le terminal vient d'etre installe : le client joue alors son animation. */
    private final boolean installedNow;

    public SyncTerminalDataPacket(TerminalData data) {
        this(data, false);
    }

    public SyncTerminalDataPacket(TerminalData data, boolean installedNow) {
        this.data = data.serializeNBT();
        this.installedNow = installedNow;
    }

    private SyncTerminalDataPacket(CompoundTag tag, boolean installedNow) {
        this.data = tag;
        this.installedNow = installedNow;
    }

    public static void encode(SyncTerminalDataPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.data);
        buf.writeBoolean(msg.installedNow);
    }

    public static SyncTerminalDataPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncTerminalDataPacket(tag, buf.readBoolean());
    }

    public static void handle(SyncTerminalDataPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> ClientTerminalData.update(msg.data, msg.installedNow));
        ctx.setPacketHandled(true);
    }
}
