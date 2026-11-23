package cn.academy.fabric.client;

import cn.academy.CatEngineRenderer;
import cn.academy.ImagFusorScreen;
import cn.academy.MatrixScreen;
import cn.academy.MetalFormerScreen;
import cn.academy.NodeScreen;
import cn.academy.PhaseGeneratorScreen;
import cn.academy.SolarGenScreen;
import cn.academy.WindgenBaseScreen;
import cn.academy.WindgenMainScreen;
import cn.academy.fabric.FabricContent;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/**
 * Initialisation client Fabric.
 *
 * <p>Les rendus de block entity et les ecrans se declarent ici, pas dans le module commun : les
 * classes vivent dans common/, leur enregistrement est propre au chargeur.
 *
 * <p>Les programmes d'interface (anneau de competence, icones grisees) ne sont pas encore charges
 * sous Fabric : le moteur d'ecran sait dessiner sans eux et s'abstient de ces deux effets.
 */
public final class AcademyCraftFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(FabricContent.CAT_ENGINE_TYPE, CatEngineRenderer::new);

        MenuScreens.register(FabricContent.SOLAR_GEN_MENU, SolarGenScreen::new);
        MenuScreens.register(FabricContent.METAL_FORMER_MENU, MetalFormerScreen::new);
        MenuScreens.register(FabricContent.WINDGEN_BASE_MENU, WindgenBaseScreen::new);
        MenuScreens.register(FabricContent.WINDGEN_MAIN_MENU, WindgenMainScreen::new);
        MenuScreens.register(FabricContent.MATRIX_MENU, MatrixScreen::new);
        MenuScreens.register(FabricContent.NODE_MENU, NodeScreen::new);
        MenuScreens.register(FabricContent.IMAG_FUSOR_MENU, ImagFusorScreen::new);
        MenuScreens.register(FabricContent.PHASE_GEN_MENU, PhaseGeneratorScreen::new);
    }
}
