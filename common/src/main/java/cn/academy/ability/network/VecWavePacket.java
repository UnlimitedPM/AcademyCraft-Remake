package cn.academy.ability.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * S2C : une onde de choc vient de s'ouvrir, a cet endroit et dans cette direction.
 *
 * <p>Trois competences de vecmanip en posent une, et aucune ne se voit sans ce message : l'onde
 * naissait dans le contexte client du lanceur chez l'original, donc personne d'autre ne la voyait.
 * Le port la dit a tous ceux qui voient le lanceur.
 *
 * <p>Le client seme ses propres particules a partir de ces nombres — voir {@code VecWaves} — et
 * seul {@code handle} nomme une classe de client.
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
        if (player == null) return;
        cn.academy.platform.network.Network.sendToTracking(
                new VecWavePacket(position, yaw, pitch, rings, size), player);
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

    public static void handle(VecWavePacket msg) {
        cn.academy.ability.client.vm.VecWaves.play(msg.position, msg.yaw, msg.pitch, msg.rings,
                msg.size);
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
