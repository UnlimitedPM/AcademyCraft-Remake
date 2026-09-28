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
    private final double[] from;
    private final double[] to;
    private final int lifeTicks;
    private final boolean clipToDistance;

    public ArcEffectPacket(String pattern, Vec3 from, Vec3 to, int lifeTicks, boolean clipToDistance) {
        this(pattern,
                new double[] { from.x, from.y, from.z },
                new double[] { to.x, to.y, to.z },
                lifeTicks, clipToDistance);
    }

    private ArcEffectPacket(String pattern, double[] from, double[] to, int lifeTicks,
                            boolean clipToDistance) {
        this.pattern = pattern;
        this.from = from;
        this.to = to;
        this.lifeTicks = lifeTicks;
        this.clipToDistance = clipToDistance;
    }

    public static void encode(ArcEffectPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.pattern);
        // Des doubles, et non des floats : un eclair a l'autre bout d'un monde de mille
        // blocs n'a pas besoin de la meme precision qu'a cote, mais il en a besoin d'un peu.
        for (double value : msg.from) buf.writeDouble(value);
        for (double value : msg.to) buf.writeDouble(value);
        buf.writeVarInt(msg.lifeTicks);
        buf.writeBoolean(msg.clipToDistance);
    }

    public static ArcEffectPacket decode(FriendlyByteBuf buf) {
        String pattern = buf.readUtf();
        double[] from = { buf.readDouble(), buf.readDouble(), buf.readDouble() };
        double[] to = { buf.readDouble(), buf.readDouble(), buf.readDouble() };
        return new ArcEffectPacket(pattern, from, to, buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(ArcEffectPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> cn.academy.ability.client.arc.ArcRenderer.spawn(
                msg.pattern,
                new Vec3(msg.from[0], msg.from[1], msg.from[2]),
                new Vec3(msg.to[0], msg.to[1], msg.to[2]),
                msg.lifeTicks, msg.clipToDistance));
        ctx.setPacketHandled(true);
    }
}
