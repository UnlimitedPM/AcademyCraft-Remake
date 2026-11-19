package cn.academy.ability.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C : le missile electronique tient, et voici son porteur.
 *
 * <p>C'est le portage du message {@code MSG_EFFECT_UPDATE} d'{@code EMContext}, envoye a chaque
 * tick du maintien — et a son lanceur <b>seul</b>, comme l'original : l'anneau de plasma du
 * missile se joue chez celui qui le tient, pas chez ceux qui le regardent. C'est la difference
 * avec la marque de radiation, qui se joue chez tout le monde.
 *
 * <p>Le message ne porte qu'un numero d'entite : la position, l'anneau et la derive sont
 * l'affaire du client, qui les relit sur le porteur. Voir
 * {@code cn.academy.ability.client.md.MdMissileSpray}.
 *
 * <p>Comme les autres paquets de ce genre, seul {@code handle} nomme une classe de client et il
 * ne s'execute que la : un serveur dedie ne connait ni les etincelles ni leur rendu.
 */
public class MdMissilePacket {

    private final int entityId;

    public MdMissilePacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(MdMissilePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
    }

    public static MdMissilePacket decode(FriendlyByteBuf buf) {
        return new MdMissilePacket(buf.readVarInt());
    }

    public static void handle(MdMissilePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> cn.academy.ability.client.md.MdMissileSpray.apply(msg.entityId));
        ctx.setPacketHandled(true);
    }

    /** Le porteur de l'anneau. Lisible par le test, qui relit l'aller-retour du paquet. */
    int entityId() {
        return entityId;
    }
}
