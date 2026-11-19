package cn.academy.terminal.tutorial.network;

import cn.academy.ability.network.AbilityNetwork;
import cn.academy.terminal.tutorial.TutorialData;
import cn.academy.terminal.tutorial.client.ClientTutorialData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C : pousse les tutoriels ouverts au client qui les a.
 *
 * <p>Meme canal que les donnees d'aptitudes et celles du terminal : c'est le canal des
 * donnees de joueur du mod, et une liste de noms n'a pas besoin du sien.
 */
public class SyncTutorialDataPacket {

    private final CompoundTag data;

    public SyncTutorialDataPacket(TutorialData data) {
        this.data = data.serializeNBT();
    }

    private SyncTutorialDataPacket(CompoundTag tag) {
        this.data = tag;
    }

    public static void encode(SyncTutorialDataPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.data);
    }

    public static SyncTutorialDataPacket decode(FriendlyByteBuf buf) {
        return new SyncTutorialDataPacket(buf.readNbt());
    }

    public static void handle(SyncTutorialDataPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> ClientTutorialData.update(msg.data));
        ctx.setPacketHandled(true);
    }
}
