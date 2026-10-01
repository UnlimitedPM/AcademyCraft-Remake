package cn.academy.ability.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C : le vol du reacteur commence, et voici sa trajectoire.
 *
 * <p>C'est le portage du message {@code MSG_TRIGGER} d'{@code JEContext}, envoye au lanceur seul
 * comme l'original : son bouclier de diamant et sa trainee de plasma se jouaient chez lui, pas
 * chez ceux qui le regardaient. C'est aussi ce que le contexte client faisait de ce message — il
 * posait son entite de bouclier et semait ses particules.
 *
 * <p>Il porte le <b>point de depart et la cible</b>, et pas seulement l'ordre de partir. C'est
 * tout l'interet du message : le vol est <b>pose</b>, pas parcouru — l'original forcait la
 * position de son client a chaque tick ({@code setPosition(lerp(start, target, ticks / 8))}), et
 * il faut que le client connaisse exactement la meme trajectoire que le serveur, sinon les deux
 * se corrigent l'un l'autre a chaque tick et le vol redevient la saccade qu'il etait.
 *
 * <p>Le deplacement lui-meme ne vient pas d'ici : le serveur pose sa position et sa vitesse, et
 * le client fait de meme de son cote — voir {@code JetEngineEffect}.
 *
 * <p>Comme les autres paquets de ce genre, seul {@code handle} nomme une classe de client et il
 * ne s'execute que la.
 */
public class JetFlightPacket {

    private final int playerId;
    private final Vec3 start;
    private final Vec3 target;

    public JetFlightPacket(int playerId, Vec3 start, Vec3 target) {
        this.playerId = playerId;
        this.start = start;
        this.target = target;
    }

    public static void encode(JetFlightPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.playerId);
        writeVec(buf, msg.start);
        writeVec(buf, msg.target);
    }

    public static JetFlightPacket decode(FriendlyByteBuf buf) {
        return new JetFlightPacket(buf.readVarInt(), readVec(buf), readVec(buf));
    }

    private static void writeVec(FriendlyByteBuf buf, Vec3 vec) {
        buf.writeDouble(vec.x);
        buf.writeDouble(vec.y);
        buf.writeDouble(vec.z);
    }

    private static Vec3 readVec(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(JetFlightPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> cn.academy.ability.client.JetEngineEffect
                .startFlight(msg.playerId, msg.start, msg.target));
        ctx.setPacketHandled(true);
    }

    /** Le porteur du vol. Lisible par le test, qui relit l'aller-retour du paquet. */
    int playerId() {
        return playerId;
    }

    /** D'ou le vol part. */
    Vec3 start() {
        return start;
    }

    /** Et ou il visait — la fin de la premiere phase, pas l'arret du vol. */
    Vec3 target() {
        return target;
    }
}
