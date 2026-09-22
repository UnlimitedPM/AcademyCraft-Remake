package cn.academy.ability.preset.network;

import cn.academy.ability.preset.PresetData;
import cn.academy.ability.preset.PresetTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * C2S : ce que le client demande aux préréglages.
 *
 * <p>Deux gestes seulement, et ils viennent tous les deux des touches : changer de
 * préréglage, et poser une compétence sur une touche. Le serveur reste seul maître de la
 * donnée : il range ce qu'on lui demande, puis renvoie l'état complet — le client ne
 * modifie jamais son propre exemplaire.
 */
public class PresetActionPacket {

    public enum Action {
        SWITCH,
        ASSIGN
    }

    private final Action action;
    private final int presetId;
    private final int key;

    /** Le nom de la compétence à poser, ou {@code null} pour libérer la touche. */
    @Nullable
    private final String skillName;

    private PresetActionPacket(Action action, int presetId, int key, @Nullable String skillName) {
        this.action = action;
        this.presetId = presetId;
        this.key = key;
        this.skillName = skillName;
    }

    public static PresetActionPacket switchTo(int presetId) {
        return new PresetActionPacket(Action.SWITCH, presetId, 0, null);
    }

    public static PresetActionPacket assign(int presetId, int key, @Nullable String skillName) {
        return new PresetActionPacket(Action.ASSIGN, presetId, key, skillName);
    }

    public static void encode(PresetActionPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeVarInt(msg.presetId);
        buf.writeVarInt(msg.key);
        buf.writeBoolean(msg.skillName != null);
        if (msg.skillName != null) buf.writeUtf(msg.skillName);
    }

    public static PresetActionPacket decode(FriendlyByteBuf buf) {
        Action action = buf.readEnum(Action.class);
        int presetId = buf.readVarInt();
        int key = buf.readVarInt();
        String skill = buf.readBoolean() ? buf.readUtf() : null;
        return new PresetActionPacket(action, presetId, key, skill);
    }

    public static void handle(PresetActionPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ServerPlayer player = ctx.getSender();
        ctx.enqueueWork(() -> {
            if (player == null) return;
            PresetData data = PresetTracker.of(player);
            if (data == null) return;

            if (msg.action == Action.SWITCH) {
                data.switchTo(msg.presetId);
                // Le client ne se contente pas de suivre : il pourrait se tromper. On lui
                // renvoie donc l'etat, et c'est lui qui fait foi.
                PresetTracker.sync(player);
            } else {
                if (msg.presetId < 0 || msg.presetId >= PresetData.MAX_PRESETS) return;
                data.getPreset(msg.presetId).assign(msg.key, msg.skillName);
                PresetTracker.sync(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
