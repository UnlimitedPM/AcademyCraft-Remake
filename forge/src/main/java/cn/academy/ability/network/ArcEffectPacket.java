package cn.academy.ability.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C : un eclair est parti, et voici ses deux bouts.
 *
 * <p>C'est le portage du {@code MSG_EFFECT} de l'original, celui qui faisait naitre son
 * entite d'arc chez tous les clients qui voyaient le tireur. Sans lui, l'eclair ne se
 * dessinerait que chez celui qui appuie sur la touche, et les autres joueurs recevraient
 * les degats sans rien voir venir — ce qui est aussi faux en jeu qu'a l'ecran.
 *
 * <p>Le motif voyage par son <b>nom</b> et non par son rang : un nom se relit, resiste a un
 * ajout de motif dans la table, et un nom inconnu retombe sur l'arc faible plutot que de
 * faire tomber le rendu.
 *
 * <p>Comme les autres paquets de ce genre, seul {@code handle} nomme une classe de client
 * et il ne s'execute que la : un serveur dedie ne connait ni les arcs ni leur rendu.
 */
public class ArcEffectPacket {

    private final String pattern;
    private final Vec3 from;
    private final Vec3 to;
    private final int lifeTicks;
    private final boolean lengthFixed;
    private final int ownerId;
    private final boolean beam;

    public ArcEffectPacket(String pattern, Vec3 from, Vec3 to, int lifeTicks, boolean lengthFixed,
                           int ownerId) {
        this(pattern, from, to, lifeTicks, lengthFixed, ownerId, false);
    }

    private ArcEffectPacket(String pattern, Vec3 from, Vec3 to, int lifeTicks, boolean lengthFixed,
                            int ownerId, boolean beam) {
        this.pattern = pattern;
        this.from = from;
        this.to = to;
        this.lifeTicks = lifeTicks;
        this.lengthFixed = lengthFixed;
        this.ownerId = ownerId;
        this.beam = beam;
    }

    /**
     * Un faisceau plutot qu'un eclair : le railgun ne pose pas un ruban mais un cylindre.
     *
     * <p>Un drapeau plutot qu'un motif de plus : le serveur nomme ce qu'il connait — des motifs
     * purs — et n'a rien a savoir du cylindre, qui n'existe que chez le client.
     */
    public static ArcEffectPacket beam(Vec3 from, Vec3 to, int lifeTicks, int ownerId) {
        return new ArcEffectPacket("", from, to, lifeTicks, false, ownerId, true);
    }

    public static void encode(ArcEffectPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.pattern);
        writePoint(buf, msg.from);
        writePoint(buf, msg.to);
        buf.writeVarInt(msg.lifeTicks);
        buf.writeBoolean(msg.lengthFixed);
        buf.writeVarInt(msg.ownerId);
        buf.writeBoolean(msg.beam);
    }

    public static ArcEffectPacket decode(FriendlyByteBuf buf) {
        String pattern = buf.readUtf();
        Vec3 from = readPoint(buf);
        Vec3 to = readPoint(buf);
        int lifeTicks = buf.readVarInt();
        boolean lengthFixed = buf.readBoolean();
        int ownerId = buf.readVarInt();
        return new ArcEffectPacket(pattern, from, to, lifeTicks, lengthFixed, ownerId,
                buf.readBoolean());
    }

    public static void handle(ArcEffectPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (msg.beam) {
                cn.academy.ability.client.arc.ArcRenderer.spawnBeam(
                        msg.from, msg.to, msg.lifeTicks, msg.ownerId);
            } else {
                cn.academy.ability.client.arc.ArcRenderer.spawn(
                        msg.pattern, msg.from, msg.to, msg.lifeTicks, msg.lengthFixed, msg.ownerId);
            }
        });
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

    /** Le motif demande. Lisible par le test, qui relit l'aller-retour du paquet. */
    String pattern() {
        return pattern;
    }

    /** Le point de depart de l'eclair. */
    Vec3 from() {
        return from;
    }

    /** Le point d'arrivee de l'eclair. */
    Vec3 to() {
        return to;
    }

    /** La duree de vie de l'eclair, en ticks. */
    int lifeTicks() {
        return lifeTicks;
    }

    /** Vrai si l'eclair garde la portee entiere de son motif, comme {@code EntityArc.lengthFixed}. */
    boolean lengthFixed() {
        return lengthFixed;
    }

    /** Le tireur : c'est sa camera qui sert de depart quand il se regarde tirer. */
    int ownerId() {
        return ownerId;
    }
}
