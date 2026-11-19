package cn.academy.ability.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C : une cible vient d'etre marquee par la radiation, et voici pour combien de temps.
 *
 * <p>C'est le portage du message {@code sync} de {@code MDDamageHelper} : l'original posait sa
 * marque dans les donnees de l'entite, chez le serveur, puis <b>l'annoncait</b> a tous ceux qui
 * voyaient la cible. Sans cela, la fumee du plasma ne se verrait que chez celui qui a tire — et
 * c'est le seul interet de ce paquet, car la marque, elle, ne se lit que chez le serveur.
 *
 * <p>Le message ne porte qu'un numero d'entite et une duree : la position, la taille et la
 * couleur sont l'affaire du client, qui les relit sur la cible. Voir
 * {@code cn.academy.ability.client.md.RadiationMarksEffect}.
 *
 * <p>Comme les autres paquets de ce genre, seul {@code handle} nomme une classe de client et il
 * ne s'execute que la : un serveur dedie ne connait ni les etincelles ni leur rendu.
 */
public class RadiationMarkPacket {

    private final int entityId;
    private final int ticks;

    public RadiationMarkPacket(int entityId, int ticks) {
        this.entityId = entityId;
        this.ticks = ticks;
    }

    public static void encode(RadiationMarkPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.ticks);
    }

    public static RadiationMarkPacket decode(FriendlyByteBuf buf) {
        return new RadiationMarkPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(RadiationMarkPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> cn.academy.ability.client.md.RadiationMarksEffect.apply(
                msg.entityId, msg.ticks));
        ctx.setPacketHandled(true);
    }

    /** La cible marquee. Lisible par le test, qui relit l'aller-retour du paquet. */
    int entityId() {
        return entityId;
    }

    /** La duree de la marque, en ticks. */
    int ticks() {
        return ticks;
    }
}
