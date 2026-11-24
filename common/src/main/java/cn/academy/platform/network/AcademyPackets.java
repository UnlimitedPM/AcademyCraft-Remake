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

        Network.clientbound("blood_spray", cn.academy.ability.network.BloodSprayPacket.class,
                Network.codec(cn.academy.ability.network.BloodSprayPacket::encode,
                        cn.academy.ability.network.BloodSprayPacket::decode),
                cn.academy.ability.network.BloodSprayPacket::handle);

        Network.clientbound("plasma_shot", cn.academy.ability.network.PlasmaShotPacket.class,
                Network.codec(cn.academy.ability.network.PlasmaShotPacket::encode,
                        cn.academy.ability.network.PlasmaShotPacket::decode),
                cn.academy.ability.network.PlasmaShotPacket::handle);

        Network.clientbound("radiation_mark", cn.academy.ability.network.RadiationMarkPacket.class,
                Network.codec(cn.academy.ability.network.RadiationMarkPacket::encode,
                        cn.academy.ability.network.RadiationMarkPacket::decode),
                cn.academy.ability.network.RadiationMarkPacket::handle);

        Network.clientbound("arc_effect", cn.academy.ability.network.ArcEffectPacket.class,
                Network.codec(cn.academy.ability.network.ArcEffectPacket::encode,
                        cn.academy.ability.network.ArcEffectPacket::decode),
                cn.academy.ability.network.ArcEffectPacket::handle);

        Network.clientbound("body_intensify", cn.academy.ability.network.BodyIntensifyPacket.class,
                Network.codec(cn.academy.ability.network.BodyIntensifyPacket::encode,
                        cn.academy.ability.network.BodyIntensifyPacket::decode),
                cn.academy.ability.network.BodyIntensifyPacket::handle);

        Network.clientbound("thunder_clap_ground",
                cn.academy.ability.network.ThunderClapGroundPacket.class,
                Network.codec(cn.academy.ability.network.ThunderClapGroundPacket::encode,
                        cn.academy.ability.network.ThunderClapGroundPacket::decode),
                cn.academy.ability.network.ThunderClapGroundPacket::handle);

        Network.clientbound("md_ray", cn.academy.ability.network.MdRayPacket.class,
                Network.codec(cn.academy.ability.network.MdRayPacket::encode,
                        cn.academy.ability.network.MdRayPacket::decode),
                cn.academy.ability.network.MdRayPacket::handle);

        Network.clientbound("md_missile", cn.academy.ability.network.MdMissilePacket.class,
                Network.codec(cn.academy.ability.network.MdMissilePacket::encode,
                        cn.academy.ability.network.MdMissilePacket::decode),
                cn.academy.ability.network.MdMissilePacket::handle);

        Network.clientbound("jet_flight", cn.academy.ability.network.JetFlightPacket.class,
                Network.codec(cn.academy.ability.network.JetFlightPacket::encode,
                        cn.academy.ability.network.JetFlightPacket::decode),
                cn.academy.ability.network.JetFlightPacket::handle);

        Network.clientbound("mine_ray_sight", cn.academy.ability.network.MineRaySightPacket.class,
                Network.codec(cn.academy.ability.network.MineRaySightPacket::encode,
                        cn.academy.ability.network.MineRaySightPacket::decode),
                cn.academy.ability.network.MineRaySightPacket::handle);

        Network.clientbound("mine_detect", cn.academy.ability.network.MineDetectPacket.class,
                Network.codec(cn.academy.ability.network.MineDetectPacket::encode,
                        cn.academy.ability.network.MineDetectPacket::decode),
                cn.academy.ability.network.MineDetectPacket::handle);

        Network.clientbound("mine_detect_stop",
                cn.academy.ability.network.MineDetectStopPacket.class,
                Network.codec(cn.academy.ability.network.MineDetectStopPacket::encode,
                        cn.academy.ability.network.MineDetectStopPacket::decode),
                cn.academy.ability.network.MineDetectStopPacket::handle);
    }
}
