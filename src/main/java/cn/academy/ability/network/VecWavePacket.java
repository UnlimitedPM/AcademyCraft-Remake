package cn.academy.ability.network;

import cn.academy.ability.client.vm.VecWaves;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * S2C : une onde de choc de vecmanip vient de s'ouvrir, et voici ou.
 *
 * <p>Trois competences en posent une, et aucune ne se voit sans ce message : le <b>choc dirige</b>
 * a l'endroit de son explosion, la <b>deviation</b> sur chaque objet qu'elle fige, et le
 * <b>renvoi</b> sur tout ce qu'il retourne. Chez l'original, chacune vivait dans le contexte client
 * de son lanceur — d'ou trois chemins differents pour un meme effet, et un effet que le lanceur
 * <b>seul</b> voyait.
 *
 * <p>Le port les reunit : un seul message, qui dit ou l'onde s'ouvre, de quel cote, de combien
 * d'anneaux et de quelle taille — et qui part a tous ceux qui <b>voient</b> le lanceur. Un combat
 * a deux se lit mieux quand les deux voient les ondes.
 *
 * <p>Le client, lui, tire ses anneaux au sort : le serveur ne dit que le nombre et la taille, comme
 * il le fait pour les rayons du meltdowner. Voir {@code VecWaves}.
 */
public class VecWavePacket {

    private final Vec3 position;
    private final float yaw;
    private final float pitch;
    private final int rings;
    private final double size;

    public VecWavePacket(Vec3 position, float yaw, float pitch, int rings, double size) {
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
        this.rings = rings;
        this.size = size;
    }

    /** Annonce l'onde a ceux qui voient le lanceur, et a lui. */
    public static void send(Player player, Vec3 position, float yaw, float pitch, int rings,
                            double size) {
        AbilityNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new VecWavePacket(position, yaw, pitch, rings, size));
    }

    public static void encode(VecWavePacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.position.x);
        buf.writeDouble(msg.position.y);
        buf.writeDouble(msg.position.z);
        buf.writeFloat(msg.yaw);
        buf.writeFloat(msg.pitch);
        buf.writeVarInt(msg.rings);
        buf.writeDouble(msg.size);
    }

    public static VecWavePacket decode(FriendlyByteBuf buf) {
        return new VecWavePacket(
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readDouble());
    }

    public static void handle(VecWavePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> VecWaves.play(msg.position, msg.yaw, msg.pitch, msg.rings, msg.size));
        ctx.setPacketHandled(true);
    }

    /** Ou l'onde s'ouvre. Lisible par le test, qui relit l'aller-retour du paquet. */
    Vec3 position() {
        return position;
    }

    /** Et de quel cote : le lacet du lanceur, puis son tangage. */
    float yaw() {
        return yaw;
    }

    float pitch() {
        return pitch;
    }

    /** Le nombre d'anneaux, et leur taille. */
    int rings() {
        return rings;
    }

    double size() {
        return size;
    }
}
