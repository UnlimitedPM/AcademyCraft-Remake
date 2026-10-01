package cn.academy.ability.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C : un rayon du meltdowner est parti, et voici ses deux bouts.
 *
 * <p>C'est le portage du {@code MSG_EFFECT} de ses competences, qui faisait naitre son entite
 * de rayon chez tous les clients qui voyaient le tireur. Sans lui, le rayon ne se dessinerait
 * que chez celui qui appuie sur la touche, et les autres joueurs prendraient les degats sans
 * rien voir venir.
 *
 * <p>Le genre du rayon voyage par son <b>nom</b> et non par son rang, comme le motif des
 * eclairs : un nom se relit, resiste a un ajout de genre dans la table, et un nom inconnu
 * retombe sur le petit rayon plutot que de faire tomber le rendu.
 *
 * <p>Le son fait partie du genre : l'original le jouait dans l'entite elle-meme, a sa position,
 * et c'est donc ici qu'il part — chez le client, une fois, en naissant.
 *
 * <p>Le <b>tireur</b> voyage avec le rayon, comme dans le paquet des eclairs. C'est lui qui dit
 * au client si le rayon doit se recoller a SA main ou rester ou il est ne : l'original ne posait
 * celui de la vue interne que sur son propre tir — « thirdPersonView == 0 && clientPlayer ==
 * entity.getPlayer() ». Sans cet identifiant, le client ne saurait pas distinguer le rayon qu'il
 * vient de tirer de celui d'un voisin. Un rayon qui n'appartient a personne — celui d'une bille,
 * par exemple — annonce {@code -1}, et le client le pose alors comme celui d'un autre.
 *
 * <p>Comme les autres paquets de ce genre, seul {@code handle} nomme une classe de client et il
 * ne s'execute que la : un serveur dedie ne connait ni les rayons ni leur rendu.
 */
public class MdRayPacket {

    private final String kind;
    private final Vec3 from;
    private final Vec3 to;
    private final int ownerId;

    public MdRayPacket(String kind, Vec3 from, Vec3 to, int ownerId) {
        this.kind = kind;
        this.from = from;
        this.to = to;
        this.ownerId = ownerId;
    }

    public static void encode(MdRayPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.kind);
        writePoint(buf, msg.from);
        writePoint(buf, msg.to);
        buf.writeInt(msg.ownerId);
    }

    public static MdRayPacket decode(FriendlyByteBuf buf) {
        String kind = buf.readUtf();
        Vec3 from = readPoint(buf);
        Vec3 to = readPoint(buf);
        return new MdRayPacket(kind, from, to, buf.readInt());
    }

    public static void handle(MdRayPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> cn.academy.ability.client.md.MdRayView.spawn(
                cn.academy.ability.client.md.MdRayKind.byName(msg.kind),
                msg.from, msg.to, msg.ownerId));
        ctx.setPacketHandled(true);
    }

    private static void writePoint(FriendlyByteBuf buf, Vec3 point) {
        buf.writeDouble(point.x);
        buf.writeDouble(point.y);
        buf.writeDouble(point.z);
    }

    private static Vec3 readPoint(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    /** Le genre demande. Lisible par le test, qui relit l'aller-retour du paquet. */
    String kind() {
        return kind;
    }

    /** Le point de depart du rayon. */
    Vec3 from() {
        return from;
    }

    /** Et son point d'arrivee. */
    Vec3 to() {
        return to;
    }

    /** L'identifiant du tireur, ou {@code -1} quand le rayon n'appartient a personne. */
    int ownerId() {
        return ownerId;
    }
}
