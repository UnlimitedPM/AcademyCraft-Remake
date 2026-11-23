package cn.academy.fabric;

import cn.academy.AcademyCraftCommon;
import net.fabricmc.api.ModInitializer;

/**
 * Point d'entree Fabric (et Quilt, qui charge les mods Fabric).
 *
 * <p>Le coeur du mod vit dans common/ ; ce module ne contient que le code qui parle a
 * fabric-loader ou a la Fabric API.
 */
public final class AcademyCraftFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        AcademyCraftCommon.init();
        cn.academy.Config.load(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
        FabricPlatformHooks.install();
        FabricNetworkTransport.installServer();
        cn.academy.platform.network.AcademyPackets.register();
        FabricContent.register();
        FabricWorldgen.register();
    }
}
