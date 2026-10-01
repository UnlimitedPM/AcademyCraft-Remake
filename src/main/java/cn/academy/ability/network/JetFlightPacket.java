package cn.academy.ability.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C : le vol du reacteur commence, et voici son porteur.
 *
 * <p>C'est le portage du message {@code MSG_TRIGGER} d'{@code JEContext}, envoye au lanceur seul
 * comme l'original : son bouclier de diamant et sa trainee de plasma se jouaient chez lui, pas
 * chez ceux qui le regardaient. C'est aussi ce que le contexte client faisait de ce message — il
 * posait son entite de bouclier et semait ses particules.
 *
 * <p>Le deplacement, lui, ne vient pas d'ici : le serveur teleporte son porteur a chaque tick de
 * vol, et le client suit. Le message ne fait donc que <b>dire quand</b> le vol a commence, ce que
 * le client ne peut pas deviner autrement — son maintien a lui s'arrete au relachement, alors que
 * celui du serveur continue pendant quinze ticks encore.
 *
 * <p>Comme les autres paquets de ce genre, seul {@code handle} nomme une classe de client et il
 * ne s'execute que la.
 */
public class JetFlightPacket {

    private final int playerId;

    public JetFlightPacket(int playerId) {
        this.playerId = playerId;
    }

    public static void encode(JetFlightPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.playerId);
    }

    public static JetFlightPacket decode(FriendlyByteBuf buf) {
        return new JetFlightPacket(buf.readVarInt());
    }

    public static void handle(JetFlightPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> cn.academy.ability.client.JetEngineEffect.startFlight(msg.playerId));
        ctx.setPacketHandled(true);
    }

    /** Le porteur du vol. Lisible par le test, qui relit l'aller-retour du paquet. */
    int playerId() {
        return playerId;
    }
}
