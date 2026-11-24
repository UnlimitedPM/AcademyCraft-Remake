package cn.academy.ability.preset.network;

import cn.academy.ability.preset.PresetData;
import cn.academy.ability.preset.client.ClientPresetData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * S2C : pousse les quatre prereglages du joueur a son client.
 *
 * <p>Le client en a besoin pour deux choses : savoir ce que ses touches allument, et
 * afficher les prereglages. Un seul paquet pour tout, comme les autres donnees de joueur du
 * port — les prereglages sont petits, et un differentiel n'apporterait rien.
 */
public class SyncPresetPacket {

    private final CompoundTag data;

    public SyncPresetPacket(PresetData presets) {
        this.data = presets.serializeNBT();
    }

    private SyncPresetPacket(CompoundTag tag) {
        this.data = tag;
    }

    public static void encode(SyncPresetPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.data);
    }

    public static SyncPresetPacket decode(FriendlyByteBuf buf) {
        return new SyncPresetPacket(buf.readNbt());
    }

    public static void handle(SyncPresetPacket msg) {
        ClientPresetData.update(msg.data);
    }
}
