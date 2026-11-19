package cn.academy.ability.network;

import cn.academy.ability.client.GroundArcs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * S2C : la foudre du claquement d'orage vient de tomber, et voici ou — et jusqu'ou.
 *
 * <p>C'est ce qui fait <b>voir la portee</b> de l'attaque. Ses degats emportent tout un rayon de
 * quinze a trente blocs autour du point d'impact, alors que la foudre de Minecraft, elle, ne
 * dessine qu'un seul eclair : les betes qui tombaient alentour mouraient « pour aucune raison
 * apparente ». Les eclairs de sol, eux, jaillissent dans tout le disque, du centre vers le bord —
 * voir {@link GroundArcs}, qui les seme.
 *
 * <p>Le message ne porte donc que <b>deux nombres</b>, comme ceux du meme genre : le point
 * d'impact, que le serveur seul connait (c'est lui qui a trace le rayon), et le rayon de la
 * competence, qui vient de son experience. Le client, lui, tire ses eclairs au sort — ou, de quelle
 * hauteur, dans quel sens —, exactement comme il tire les anneaux de l'onde de vecmanip ou les
 * rayons de la salve du meltdowner.
 */
public class ThunderClapGroundPacket {

    private final Vec3 impact;
    private final double radius;

    public ThunderClapGroundPacket(Vec3 impact, double radius) {
        this.impact = impact;
        this.radius = radius;
    }

    /** Annonce la chute a ceux qui voient le lanceur, et a lui. */
    public static void send(Player player, Vec3 impact, double radius) {
        AbilityNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new ThunderClapGroundPacket(impact, radius));
    }

    public static void encode(ThunderClapGroundPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.impact.x);
        buf.writeDouble(msg.impact.y);
        buf.writeDouble(msg.impact.z);
        buf.writeDouble(msg.radius);
    }

    public static ThunderClapGroundPacket decode(FriendlyByteBuf buf) {
        return new ThunderClapGroundPacket(
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readDouble());
    }

    public static void handle(ThunderClapGroundPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> GroundArcs.burst(msg.impact, msg.radius));
        ctx.setPacketHandled(true);
    }

    /** Ou la foudre est tombee. Lisible par le test, qui relit l'aller-retour du paquet. */
    Vec3 impact() {
        return impact;
    }

    /** Et le rayon qu'elle emporte : c'est lui qui donne l'etendue du semis. */
    double radius() {
        return radius;
    }
}
