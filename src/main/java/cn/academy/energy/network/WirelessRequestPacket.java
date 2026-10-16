package cn.academy.energy.network;

import java.util.Locale;
import java.util.function.Supplier;

import cn.academy.energy.NodeFinder;
import cn.academy.energy.WirelessMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Ce que le joueur demande depuis l'onglet sans fil d'une machine : la liste, un raccordement, un
 * detachement.
 *
 * <p>C'est le {@code find_nodes} / {@code user_connect} / {@code unlink} de l'original, dans un seul
 * paquet — les trois ne different que par ce qu'ils visent, et une action dit quoi.
 *
 * <h2>La machine, c'est le conteneur ouvert</h2>
 *
 * <p>Le paquet ne transporte <b>aucune</b> position de machine : le serveur prend celle du conteneur
 * que le joueur a devant lui, par {@link WirelessMachine}. Un client ne peut donc pas raccorder la
 * machine de quelqu'un d'autre, ni une machine devant laquelle il n'est pas — et il n'y a rien a
 * verifier, puisque c'est deja la seule que le joueur puisse voir.
 *
 * <p>Le mot de passe voyage en clair, comme chez l'original : il est lu par le serveur et compare a
 * celui du noeud. Ce n'est pas un secret a proteger contre le joueur qui le tape.
 */
public class WirelessRequestPacket {

    /** Ce qu'un onglet sans fil demande. */
    public enum Action { FIND, CONNECT, DISCONNECT }

    private final Action action;
    private final BlockPos node;
    private final String password;

    public WirelessRequestPacket(Action action, BlockPos node, String password) {
        this.action = action;
        this.node = node == null ? BlockPos.ZERO : node;
        this.password = password == null ? "" : password;
    }

    /** Demande la liste : ce que l'ecran fait des qu'il s'ouvre. */
    public static WirelessRequestPacket find() {
        return new WirelessRequestPacket(Action.FIND, BlockPos.ZERO, "");
    }

    public static WirelessRequestPacket connect(BlockPos node, String password) {
        return new WirelessRequestPacket(Action.CONNECT, node, password);
    }

    public static WirelessRequestPacket disconnect() {
        return new WirelessRequestPacket(Action.DISCONNECT, BlockPos.ZERO, "");
    }

    public static void encode(WirelessRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeBlockPos(msg.node);
        // Un mot de passe de noeud tient dans seize caracteres, la borne du champ de l'ecran.
        buf.writeUtf(msg.password, 16);
    }

    public static WirelessRequestPacket decode(FriendlyByteBuf buf) {
        return new WirelessRequestPacket(buf.readEnum(Action.class), buf.readBlockPos(),
                buf.readUtf(16));
    }

    public static void handle(WirelessRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            if (!(sender.containerMenu instanceof WirelessMachine machine)) return;
            if (!(sender.level() instanceof ServerLevel level)) return;

            BlockPos machinePos = machine.machinePos();
            switch (msg.action) {
                case CONNECT -> {
                    NodeFinder.LinkOutcome outcome = NodeFinder.link(level, machinePos, msg.node,
                            msg.password);
                    sender.displayClientMessage(
                            Component.translatable("ac.gui.wireless.link."
                                    + outcome.name().toLowerCase(Locale.ROOT)), true);
                }
                case DISCONNECT -> {
                    NodeFinder.unlink(level, machinePos);
                    sender.displayClientMessage(
                            Component.translatable("ac.gui.wireless.unlinked"), true);
                }
                case FIND -> {
                    // Rien a faire : la liste part juste apres, et c'est elle qui repond.
                }
            }

            // Dans tous les cas, l'ecran recoit l'etat d'apres : c'est ce qui le rafraichit sans
            // qu'il ait a redemander quoi que ce soit, et sans qu'il puisse afficher autre chose que
            // ce que le serveur vient de faire.
            WirelessNodesPacket.send(sender, machinePos);
        });
        ctx.get().setPacketHandled(true);
    }
}
