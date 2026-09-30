package cn.academy.ability.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C : le renfort du corps a pris, et voici qui l'a lance.
 *
 * <p>C'est le portage du {@code MSG_EFFECT_END} de l'original, celui qui faisait naitre son
 * entite d'arcs chez tous ceux qui voyaient le joueur. Le message ne porte qu'un numero
 * d'entite, et c'est voulu : les sept hauteurs, les trois a quatre arcs et leurs delais sont
 * une affaire d'<b>image</b>, qui n'a rien a faire chez le serveur — voir
 * {@code cn.academy.ability.client.BodyIntensifyEffect}.
 *
 * <p>Comme les autres paquets de ce genre, seul {@code handle} nomme une classe de client et il
 * ne s'execute que la : un serveur dedie ne connait ni les arcs ni leur rendu.
 */
public class BodyIntensifyPacket {

    private final int ownerId;

    public BodyIntensifyPacket(int ownerId) {
        this.ownerId = ownerId;
    }

    public static void encode(BodyIntensifyPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.ownerId);
    }

    public static BodyIntensifyPacket decode(FriendlyByteBuf buf) {
        return new BodyIntensifyPacket(buf.readVarInt());
    }

    public static void handle(BodyIntensifyPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> cn.academy.ability.client.BodyIntensifyEffect.start(msg.ownerId));
        ctx.setPacketHandled(true);
    }

    /** Le porteur du renfort. Lisible par le test, qui relit l'aller-retour du paquet. */
    int ownerId() {
        return ownerId;
    }
}
