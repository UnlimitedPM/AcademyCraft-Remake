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
        cn.academy.fabric.FabricNetworkTransport.installClient();
        BlockEntityRenderers.register(FabricContent.CAT_ENGINE_TYPE, CatEngineRenderer::new);
        // Les machines dessinees par un fichier OBJ : Forge les posait par son chargeur de modele,
        // qui n'existe pas ici — voir FabricMachineModels.
        FabricMachineModels.register();
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                FabricContent.SILBARN_TYPE, cn.academy.client.SilbarnRenderer::new);
        // Le grappin : son rendu lit le meme modele OBJ que chez Forge.
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                FabricContent.MAG_HOOK_TYPE, cn.academy.client.MagHookRenderer::new);
        // La bille de plasma du meltdowner, et la piece du railgun.
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                FabricContent.MD_BALL_TYPE, cn.academy.ability.client.md.MdBallRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                FabricContent.COIN_TYPE, cn.academy.client.CoinRenderer::new);
        // Le bloc arrache par la manipulation magnetique : le dessin du monde s'en charge, mais le
        // jeu lit le rendu de chaque entite visible, et plante quand il n'en trouve pas.
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                FabricContent.MAG_MANIP_BLOCK_TYPE,
                net.minecraft.client.renderer.entity.NoopRenderer::new);

        MenuScreens.register(FabricContent.SOLAR_GEN_MENU, SolarGenScreen::new);
        MenuScreens.register(FabricContent.METAL_FORMER_MENU, MetalFormerScreen::new);
        MenuScreens.register(FabricContent.WINDGEN_BASE_MENU, WindgenBaseScreen::new);
        MenuScreens.register(FabricContent.WINDGEN_MAIN_MENU, WindgenMainScreen::new);
        MenuScreens.register(FabricContent.MATRIX_MENU, MatrixScreen::new);
        MenuScreens.register(FabricContent.NODE_MENU, NodeScreen::new);
        MenuScreens.register(FabricContent.IMAG_FUSOR_MENU, ImagFusorScreen::new);
        MenuScreens.register(FabricContent.PHASE_GEN_MENU, PhaseGeneratorScreen::new);
        MenuScreens.register(FabricContent.ABILITY_INTERFERER_MENU,
                cn.academy.AbilityInterfererScreen::new);
        // Le developeur : le meme ecran pour les deux machines et pour le portable.
        MenuScreens.register(FabricContent.DEVELOPER_MENU, cn.academy.DeveloperScreen::new);

        // Les touches d'aptitude sont communes ; Fabric les declare par son propre registre.
        cn.academy.ability.client.AbilityKeyBindings.register(
                net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper::registerKeyBinding);

        // Le rendu de l'imag phase liquide : sans cela le jeu ne sait pas quelle image poser sur
        // ses faces, et Fabric refuse de dessiner le fluide du tout.
        net.minecraft.resources.ResourceLocation black = new net.minecraft.resources.ResourceLocation(
                cn.academy.AcademyCraftCommon.MOD_ID, "block/black");
        net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry.INSTANCE.register(
                cn.academy.fabric.FabricFluids.SOURCE, cn.academy.fabric.FabricFluids.FLOWING,
                new net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler(black, black));
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putFluids(
                net.minecraft.client.renderer.RenderType.translucent(),
                cn.academy.fabric.FabricFluids.SOURCE, cn.academy.fabric.FabricFluids.FLOWING);

        // Les nappes de l'imag phase liquide : Forge les dessine une fois la couche translucide du
        // monde peinte (voir ClientRenderEvents). C'est la seule place ou elles se voient — un bloc
        // de fluide est peint dans cette couche, donc un rendu de block entity passerait dessous.
        // Sans elles le liquide n'est qu'un aplat noir.
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_TRANSLUCENT.register(
                context -> cn.academy.client.render.ImagPhaseLiquidRenderer.draw(
                        new com.mojang.blaze3d.vertex.PoseStack(),
                        context.camera().getPosition(),
                        context.frustum()));

        // Les effets du mod : leurs ticks et leurs rendus. Tous les dessins sont communs ; c'est ici
        // qu'on les declare a Fabric, qui n'a pas une classe d'evenements par effet comme Forge.
        FabricClientRenders.register();

        // La touche du terminal et ses applications : sans ces deux lignes le terminal est
        // inatteignable et ses icones restent mortes.
        cn.academy.terminal.client.TerminalKeyBindings.register(
                net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper::registerKeyBinding);
        cn.academy.terminal.client.TerminalScreens.init();
    }
}
