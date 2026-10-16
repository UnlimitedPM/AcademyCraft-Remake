package cn.academy.energy.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import cn.academy.ability.network.AbilityNetwork;
import cn.academy.client.gui.cgui.CGuiWireless;
import cn.academy.energy.NodeFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

/**
 * L'etat d'un onglet sans fil, envoye a son joueur : le noeud lie, et les noeuds joignables.
 *
 * <p>C'est le {@code Future} de l'original, en plus simple : il repondait a chaque demande par une
 * liste de tuiles, le port envoie une liste de lignes. Chaque ligne porte ce que l'ecran doit
 * montrer — un nom, et deux drapeaux : le noeud demande-t-il un mot de passe, et lui reste-t-il une
 * place. Un noeud plein est affiche comme les autres, mais son raccordement sera refuse.
 *
 * <p>Le noeud lie n'est pas dans la liste : l'ecran le montre a part, dans son cadre « Connected »,
 * exactement comme l'original.
 */
public class WirelessNodesPacket {

    /** Une ligne de la liste : de quoi la dessiner, et de quoi s'y raccorder. */
    public record Entry(BlockPos pos, String name, boolean encrypted, boolean room) {}

    private final String linked;
    private final List<Entry> nodes;

    public WirelessNodesPacket(String linked, List<Entry> nodes) {
        this.linked = linked == null ? "" : linked;
        this.nodes = List.copyOf(nodes);
    }

    /** Le nom du noeud lie, ou une chaine vide. */
    public String linked() {
        return linked;
    }

    /** Les noeuds joignables, du plus proche au plus loin. */
    public List<Entry> nodes() {
        return nodes;
    }

    /** Envoie l'etat de l'onglet d'une machine : le noeud qui l'alimente, et les noeuds joignables. */
    public static void sendNodes(ServerPlayer player, BlockPos machinePos) {
        if (!(player.level() instanceof ServerLevel level)) return;

        List<Entry> entries = new ArrayList<>();
        for (NodeFinder.Candidate candidate : NodeFinder.candidates(level, machinePos)) {
            entries.add(entry(candidate));
        }

        send(player, NodeFinder.nodeName(level, machinePos), entries);
    }

    /**
     * Et l'etat de l'onglet d'un noeud : le reseau qu'il a rejoint, et les reseaux joignables.
     *
     * <p>Le nom d'un reseau est son SSID, et son cadre dit la meme chose que celui d'une machine :
     * « Connected » et le nom, ou rien du tout.
     */
    public static void sendMatrices(ServerPlayer player, BlockPos nodePos) {
        if (!(player.level() instanceof ServerLevel level)) return;

        List<Entry> entries = new ArrayList<>();
        for (NodeFinder.Candidate candidate : NodeFinder.matrices(level, nodePos)) {
            entries.add(entry(candidate));
        }

        send(player, NodeFinder.matrixName(level, nodePos), entries);
    }

    private static Entry entry(NodeFinder.Candidate candidate) {
        return new Entry(candidate.pos(), candidate.name(), candidate.encrypted(),
                candidate.room());
    }

    private static void send(ServerPlayer player, String linked, List<Entry> entries) {
        AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new WirelessNodesPacket(linked, entries));
    }

    public static void encode(WirelessNodesPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.linked, 16);
        buf.writeVarInt(msg.nodes.size());
        for (Entry entry : msg.nodes) {
            buf.writeBlockPos(entry.pos());
            buf.writeUtf(entry.name(), 16);
            buf.writeBoolean(entry.encrypted());
            buf.writeBoolean(entry.room());
        }
    }

    public static WirelessNodesPacket decode(FriendlyByteBuf buf) {
        String linked = buf.readUtf(16);
        int size = buf.readVarInt();
        List<Entry> nodes = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            nodes.add(new Entry(buf.readBlockPos(), buf.readUtf(16), buf.readBoolean(),
                    buf.readBoolean()));
        }
        return new WirelessNodesPacket(linked, nodes);
    }

    public static void handle(WirelessNodesPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            List<CGuiWireless.Row> rows = new ArrayList<>(msg.nodes.size());
            for (Entry entry : msg.nodes) {
                rows.add(new CGuiWireless.Row(entry.pos(), entry.name(), entry.encrypted(),
                        entry.room()));
            }
            CGuiWireless.deliver(msg.linked, rows);
        });
        ctx.get().setPacketHandled(true);
    }
}
