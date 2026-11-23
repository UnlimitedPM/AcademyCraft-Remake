package cn.academy.platform.network;

import cn.academy.energy.network.MatrixConfigPacket;
import cn.academy.energy.network.NodeConfigPacket;
import cn.academy.energy.network.WirelessNodesPacket;
import cn.academy.energy.network.WirelessRequestPacket;
import cn.academy.ability.network.SyncAbilityDataPacket;
import cn.academy.ability.network.BloodSplashPacket;
import cn.academy.ability.network.ShiftTeleportPacket;
import cn.academy.ability.network.SmokePuffPacket;
import cn.academy.ability.network.TeleportCritPacket;
import cn.academy.ability.network.VecWavePacket;

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

        Network.clientbound("sync_ability_data", SyncAbilityDataPacket.class,
                Network.codec(SyncAbilityDataPacket::encode, SyncAbilityDataPacket::decode),
                SyncAbilityDataPacket::handle);

        // Les cinq effets d'aptitude qui naissent autour d'une cible ou d'un lanceur, et que tous
        // ceux qui les voient doivent voir.
        Network.clientbound("blood_splash", BloodSplashPacket.class,
                Network.codec(BloodSplashPacket::encode, BloodSplashPacket::decode),
                BloodSplashPacket::handle);

        Network.clientbound("shift_teleport", ShiftTeleportPacket.class,
                Network.codec(ShiftTeleportPacket::encode, ShiftTeleportPacket::decode),
                ShiftTeleportPacket::handle);

        Network.clientbound("teleport_crit", TeleportCritPacket.class,
                Network.codec(TeleportCritPacket::encode, TeleportCritPacket::decode),
                TeleportCritPacket::handle);

        Network.clientbound("vec_wave", VecWavePacket.class,
                Network.codec(VecWavePacket::encode, VecWavePacket::decode),
                VecWavePacket::handle);

        Network.clientbound("smoke_puff", SmokePuffPacket.class,
                Network.codec(SmokePuffPacket::encode, SmokePuffPacket::decode),
                SmokePuffPacket::handle);
    }
}
