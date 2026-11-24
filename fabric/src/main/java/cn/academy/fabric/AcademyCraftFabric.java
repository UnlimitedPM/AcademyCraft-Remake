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
        FabricAbilityData.install();
        FabricNetworkTransport.installServer();
        cn.academy.platform.network.AcademyPackets.register();
        // Les quatre cursus d'aptitude sont communs : Fabric les enregistre comme Forge.
        cn.academy.ability.AbilityCategories.registerAll();
        FabricContent.register();
        FabricWorldgen.register();

        // Les declencheurs de succes sont communs ; Fabric n'a pas d'evenement de « setup », donc
        // ils se posent ici, une fois, au demarrage.
        cn.academy.advancements.AcademyAdvancements.register();

        // /aim et /aimp : les memes commandes que Forge, posees sur le repartiteur de Fabric.
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        cn.academy.command.Commands.register(dispatcher));
    }
}
