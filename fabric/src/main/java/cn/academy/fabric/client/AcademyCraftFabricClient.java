package cn.academy.fabric.client;

import cn.academy.CatEngineRenderer;
import cn.academy.fabric.FabricContent;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/**
 * Initialisation client Fabric.
 *
 * <p>Les rendus de block entity se declarent ici, pas dans le module commun : la classe de rendu
 * peut vivre dans common/, mais son enregistrement est propre au chargeur.
 */
public final class AcademyCraftFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(FabricContent.CAT_ENGINE_TYPE, CatEngineRenderer::new);
    }
}
