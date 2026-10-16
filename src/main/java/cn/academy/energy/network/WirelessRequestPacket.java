package cn.academy.energy.network;

import java.util.Locale;
import java.util.function.Supplier;

import cn.academy.energy.NodeFinder;
import cn.academy.energy.WirelessMachine;
import cn.academy.energy.WirelessNode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Ce que le joueur demande depuis l'onglet sans fil d'une machine ou d'un noeud : la liste, un
 * raccordement, un detachement.
 *
 * <p>C'est le {@code find_nodes} / {@code user_connect} / {@code unlink} de l'original, et ses trois
 * jumeaux cote noeud, dans un seul paquet — les trois ne different que par ce qu'ils visent, et une
 * action dit quoi. Le meme paquet sert les deux moities de l'onglet, parce que la demande est la
 * meme : rejoindre ce qu'on a choisi.
 *
 * <h2>Le materiel vise, c'est le conteneur ouvert</h2>
 *
 * <p>Le paquet ne transporte <b>aucune</b> position : le serveur prend celle du conteneur que le
 * joueur a devant lui, par {@link WirelessMachine} ou {@link WirelessNode}. Un client ne peut donc
 * pas raccorder la machine de quelqu'un d'autre, ni rejoindre un reseau depuis un noeud devant lequel
 * il n'est pas — et il n'y a rien a verifier, puisque c'est deja la seule chose que le joueur puisse
 * voir.
 *
 * <p>Le mot de passe voyage en clair, comme chez l'original : il est lu par le serveur et compare a
 * celui du noeud ou du Matrix. Ce n'est pas un secret a proteger contre le joueur qui le tape.
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
            if (!(sender.level() instanceof ServerLevel level)) return;

            // De quoi parle-t-on ? Du conteneur ouvert, et de rien d'autre : un paquet ne porte
            // aucune position, donc un client ne peut pas agir sur du materiel devant lequel il
            // n'est pas. Une machine cherche un noeud, un noeud cherche un Matrix — c'est la seule
            // difference entre les deux moities de l'onglet.
            if (sender.containerMenu instanceof WirelessMachine machine) {
                machineRequest(level, sender, machine.machinePos(), msg);
            } else if (sender.containerMenu instanceof WirelessNode node) {
                nodeRequest(level, sender, node.nodePos(), msg);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /** Une machine qui cherche son noeud : la liste, le raccordement, le detachement. */
    private static void machineRequest(ServerLevel level, ServerPlayer sender, BlockPos machinePos,
                                       WirelessRequestPacket msg) {
        switch (msg.action) {
            case CONNECT -> {
                say(sender, NodeFinder.link(level, machinePos, msg.node, msg.password));
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

        WirelessNodesPacket.sendNodes(sender, machinePos);
    }

    /** Un noeud qui cherche son reseau : la meme chose, l'autre moitie. */
    private static void nodeRequest(ServerLevel level, ServerPlayer sender, BlockPos nodePos,
                                    WirelessRequestPacket msg) {
        switch (msg.action) {
            case CONNECT -> {
                say(sender, NodeFinder.linkMatrix(level, nodePos, msg.node, msg.password));
            }
            case DISCONNECT -> {
                NodeFinder.unlinkMatrix(level, nodePos);
                sender.displayClientMessage(
                        Component.translatable("ac.gui.wireless.unlinked"), true);
            }
            case FIND -> {
                // Idem : la liste suit.
            }
        }

        WirelessNodesPacket.sendMatrices(sender, nodePos);
    }

    /**
     * Ce qu'on dit au joueur apres lui : une ligne, au-dessus de sa barre rapide.
     *
     * <p>Une reponse possible = un texte, et c'est l'enumeration {@code LinkOutcome} qui les nomme :
     * un cas ajoute sans traduction se verrait comme une cle brute, ce que le joueur a deja vu une
     * fois dans ce port. {@code WirelessTabTest} relit cette liste avec le fichier de langue.
     */
    private static void say(ServerPlayer sender, NodeFinder.LinkOutcome outcome) {
        sender.displayClientMessage(
                Component.translatable("ac.gui.wireless.link."
                        + outcome.name().toLowerCase(Locale.ROOT)), true);
    }
}
