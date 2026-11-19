package cn.academy.ability.preset.network;

import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.preset.PresetCapability;
import cn.academy.ability.preset.client.ClientPresetData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C : pousse les quatre préréglages du joueur à son client.
 *
 * <p>Le client en a besoin pour deux choses : savoir ce que ses touches allument, et
 * afficher les préréglages. Un seul paquet pour tout, comme les autres données de joueur du
 * port — les préréglages sont petits, et un différentiel n'apporterait rien.
 */
public class SyncPresetPacket {

    private final CompoundTag data;

    public SyncPresetPacket(PresetCapability.Holder holder) {
        this.data = holder.serializeNBT();
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

    public static void handle(SyncPresetPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> ClientPresetData.update(msg.data));
        ctx.setPacketHandled(true);
    }
}
