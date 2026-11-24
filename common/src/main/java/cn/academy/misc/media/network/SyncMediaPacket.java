package cn.academy.misc.media.network;

import cn.academy.misc.media.MediaAcquireData;
import cn.academy.misc.media.client.ClientMediaData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * S2C : pousse les morceaux du joueur a son client.
 *
 * <p>Le client en a besoin pour afficher la liste de son lecteur. Un seul paquet pour tout, comme
 * les autres donnees de joueur du port — trois noms au plus, un differentiel n'apporterait rien.
 */
public class SyncMediaPacket {

    private final CompoundTag data;

    public SyncMediaPacket(MediaAcquireData media) {
        this.data = media.serializeNBT();
    }

    private SyncMediaPacket(CompoundTag tag) {
        this.data = tag;
    }

    public static void encode(SyncMediaPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.data);
    }

    public static SyncMediaPacket decode(FriendlyByteBuf buf) {
        return new SyncMediaPacket(buf.readNbt());
    }

    public static void handle(SyncMediaPacket msg) {
        ClientMediaData.update(msg.data);
    }
}
