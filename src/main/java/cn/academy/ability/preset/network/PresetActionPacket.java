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
        ctx.enqueueWork(() -> apply(player, msg.action, msg.presetId, msg.key, msg.skillName));
        ctx.setPacketHandled(true);
    }

    /**
     * Ce que le serveur fait d'une demande, sans passer par le reseau.
     *
     * <p>Extrait du gestionnaire pour que le meme chemin s'exerce en GameTest : le client
     * ne fait que demander, et c'est ici que la donnee change — puis redescend, complete.
     */
    public static void apply(@Nullable ServerPlayer player, Action action, int presetId, int key,
                             @Nullable String skillName) {
        if (player == null) return;
        PresetData data = PresetTracker.of(player);
        if (data == null) return;

        if (action == Action.SWITCH) {
            data.switchTo(presetId);
        } else {
            if (presetId < 0 || presetId >= PresetData.MAX_PRESETS) return;
            data.getPreset(presetId).assign(key, skillName);
        }

        // Et ce qui n'est plus dans la barre ne reste pas allume. Un maintien ne survit pas au
        // prereglage qui l'a ouverte : le joueur l'a demande — « si je le retire de ma barre des
        // competences, il continue toujours de fonctionner, alors que ca devrais faire en sorte de
        // le desactiver par defaut si il n'est pas present dans ma barre ». Voir AbilityEvents.
        cn.academy.ability.AbilityEvents.endHoldsOutsideCurrentPreset(player);

        // Le client ne se contente pas de suivre : il pourrait se tromper. On lui renvoie
        // donc l'etat, et c'est lui qui fait foi.
        PresetTracker.sync(player);
    }
}
