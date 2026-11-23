package cn.academy.platform.network;

import cn.academy.energy.network.MatrixConfigPacket;
import cn.academy.energy.network.NodeConfigPacket;
import cn.academy.energy.network.WirelessNodesPacket;
import cn.academy.energy.network.WirelessRequestPacket;

/**
 * Declarations des paquets communs.
 *
 * <p>Appele une fois par chargeur, apres installation de son transport. Les identifiants sont
 * stables : ils voyagent tels quels sous Fabric, et servent d'ordre d'enregistrement sous Forge.
 */
public final class AcademyPackets {

    private AcademyPackets() {
    }

    public static void register() {
        Network.serverbound("wireless_request", WirelessRequestPacket.class,
                Network.codec(WirelessRequestPacket::encode, WirelessRequestPacket::decode),
                WirelessRequestPacket::handle);

        Network.clientbound("wireless_nodes", WirelessNodesPacket.class,
                Network.codec(WirelessNodesPacket::encode, WirelessNodesPacket::decode),
                WirelessNodesPacket::handle);

        Network.serverbound("node_config", NodeConfigPacket.class,
                Network.codec(NodeConfigPacket::encode, NodeConfigPacket::decode),
                NodeConfigPacket::handle);

        Network.serverbound("matrix_config", MatrixConfigPacket.class,
                Network.codec(MatrixConfigPacket::encode, MatrixConfigPacket::decode),
                MatrixConfigPacket::handle);
    }
}
