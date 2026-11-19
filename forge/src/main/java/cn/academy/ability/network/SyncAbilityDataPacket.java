package cn.academy.ability.network;

import cn.academy.ability.AbilityData;
import cn.academy.ability.client.ClientAbilityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S2C: pushes the authoritative AbilityData to the owning client (HUD/UI cache). */
public class SyncAbilityDataPacket {

    private final CompoundTag data;

    public SyncAbilityDataPacket(AbilityData data) {
        this.data = data.serializeNBT();
    }

    private SyncAbilityDataPacket(CompoundTag tag) {
        this.data = tag;
    }

    public static void encode(SyncAbilityDataPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.data);
    }

    public static SyncAbilityDataPacket decode(FriendlyByteBuf buf) {
        return new SyncAbilityDataPacket(buf.readNbt());
    }

    public static void handle(SyncAbilityDataPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> ClientAbilityData.update(msg.data));
        ctx.setPacketHandled(true);
    }
}
