package cn.academy.ability.network;

import cn.academy.ability.client.tp.ShiftTrail;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * S2C : le depose au loin vient de partir, et voici son trajet.
 *
 * <p>C'est la trainee de particules de {@code ShiftTeleport} : l'original en semait une le long du
 * geste, de ses pieds jusqu'a la case visee, et c'est la <b>seule</b> chose que le geste donne a
 * voir. Il ne se deplace pas lui-meme — il pose un bloc au loin — donc sans cette trainee, le
 * joueur ne verrait rien du tout du chemin parcouru.
 *
 * <p>Le paquet ne porte que les <b>deux bouts</b> : d'ou l'on part, et ou l'on veut aller. C'est le
 * client qui seme, avec son propre hasard, comme il le fait pour les rayons du meltdowner — voir
 * {@code ShiftTrail}, qui porte les nombres. Le serveur, lui, dit seulement que le geste a eu lieu :
 * un geste refuse par la reserve ne laisse donc aucune trace, ce que la version d'origine ne
 * savait pas faire (elle envoyait sa trainee avant meme de payer).
 *
 * <p>Il part a ceux qui <b>voient</b> le lanceur, et non au seul lanceur : l'original semait ses
 * particules dans le monde, ou tout le monde les voyait. Le port garde ce comportement — un
 * compagnon qui regarde le geste doit en voir la trainee.
 *
 * <p>Comme les autres paquets de ce genre, seul {@code handle} nomme une classe de client, et il ne
 * s'execute que la : un serveur dedie ne connait ni la trainee ni ses etincelles.
 */
public class ShiftTeleportPacket {

    private final Vec3 feet;
    private final BlockPos cell;

    public ShiftTeleportPacket(Vec3 feet, BlockPos cell) {
        this.feet = feet;
        this.cell = cell;
    }

    /** Annonce la trainee a ceux qui voient le lanceur, et a lui. */
    public static void send(Player player, Vec3 feet, BlockPos cell) {
        AbilityNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new ShiftTeleportPacket(feet, cell));
    }

    public static void encode(ShiftTeleportPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.feet.x);
        buf.writeDouble(msg.feet.y);
        buf.writeDouble(msg.feet.z);
        buf.writeBlockPos(msg.cell);
    }

    public static ShiftTeleportPacket decode(FriendlyByteBuf buf) {
        return new ShiftTeleportPacket(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                buf.readBlockPos());
    }

    public static void handle(ShiftTeleportPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> ShiftTrail.play(msg.feet, msg.cell));
        ctx.setPacketHandled(true);
    }

    /** Les pieds du lanceur. Lisible par le test, qui relit l'aller-retour du paquet. */
    Vec3 feet() {
        return feet;
    }

    /** Et la case visee, qui donne le bout de la trainee. */
    BlockPos cell() {
        return cell;
    }
}
