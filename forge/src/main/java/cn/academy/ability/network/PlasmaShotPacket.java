package cn.academy.ability.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * S2C : le tir du canon a plasma est parti, et voici ou il va.
 *
 * <p>Le corps de plasma vit chez son porteur seul — l'original le posait dans le contexte client de
 * celui qui appuyait, et personne d'autre ne le voyait. Ce paquet part donc vers <b>lui seul</b>,
 * comme le {@code sendToClient} de l'original, et il porte les <b>deux</b> bouts du vol : la boule,
 * telle que le serveur la voit, et sa destination.
 *
 * <p>Le client n'a plus qu'a conduire : d'un bloc par tick, en ligne droite, jusqu'a ce que le mur,
 * la destination ou le temps l'arrete — les memes bornes que le serveur, qui les revalide de son
 * cote. C'est le partage de l'original, qui rejouait le deplacement chez le client et n'envoyait sa
 * position que tous les cinq ticks pour corriger la derive ; le port n'a pas besoin de cette
 * correction, puisqu'il part de la position vraie.
 *
 * <p>Comme les autres paquets de ce genre, seul {@code handle} nomme une classe de client.
 */
public class PlasmaShotPacket {

    private final Vec3 position;
    private final Vec3 destination;

    public PlasmaShotPacket(Vec3 position, Vec3 destination) {
        this.position = position;
        this.destination = destination;
    }

    /** Annonce le tir a son lanceur, et a lui seul. */
    public static void send(ServerPlayer player, Vec3 position, Vec3 destination) {
        if (player == null) return;
        AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new PlasmaShotPacket(position, destination));
    }

    public static void encode(PlasmaShotPacket msg, FriendlyByteBuf buf) {
        write(buf, msg.position);
        write(buf, msg.destination);
    }

    public static PlasmaShotPacket decode(FriendlyByteBuf buf) {
        return new PlasmaShotPacket(read(buf), read(buf));
    }

    public static void handle(PlasmaShotPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
                cn.academy.ability.client.vm.PlasmaBodies.shot(msg.position, msg.destination));
        ctx.setPacketHandled(true);
    }

    /** La boule, telle que le serveur la voit. Lisible par le test, qui relit l'aller-retour. */
    Vec3 position() {
        return position;
    }

    /** La ou le tir a ete vise. Lisible par le test, pour le meme usage. */
    Vec3 destination() {
        return destination;
    }

    private static void write(FriendlyByteBuf buf, Vec3 vec) {
        buf.writeDouble(vec.x);
        buf.writeDouble(vec.y);
        buf.writeDouble(vec.z);
    }

    private static Vec3 read(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}
