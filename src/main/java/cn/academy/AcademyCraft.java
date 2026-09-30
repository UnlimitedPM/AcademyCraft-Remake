package cn.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AcademyCraft.MOD_ID)
public class AcademyCraft {
    public static final String MOD_ID = "academy";

    public AcademyCraft(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModFluids.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModSounds.register(modEventBus);
        ModBlockEntities.register(modEventBus); // <-- LA LIGNE MAGIQUE QUI MANQUAIT
        // Les declencheurs de succes vivent dans la table de CriteriaTriggers, pas dans un
        // registre : ils s'enregistrent donc au demarrage, avant que le premier monde ne
        // lise ses fichiers de succes.
        modEventBus.addListener(cn.academy.advancements.AcademyAdvancements::setup);
        ModCreativeTabs.register(modEventBus);
        ModMenus.register(modEventBus);
        // Types de biome modifier du mod (honore les options de generation de la config).
        cn.academy.worldgen.ConfigurableFeatureBiomeModifier.SERIALIZERS.register(modEventBus);
        modEventBus.addListener(cn.academy.ability.AbilityInit::init);
        modEventBus.addListener(cn.academy.terminal.TerminalInit::init);

        // Sans cette ligne, le fichier config/academy-common.toml n'est jamais cree.
        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, Config.SPEC);
        // La place des elements du HUD est une affaire de client : elle vit donc dans une
        // config de type CLIENT, comme l'original la rangeait chez lui. Sur un serveur
        // dedie, Forge ignore simplement cette config.
        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
                cn.academy.client.hud.HudConfig.SPEC);

        MinecraftForge.EVENT_BUS.register(this);
    }

    // Garde tes ??v??nements de gameplay ici (Forge Bus)
    @SubscribeEvent
    public void onBucketFill(FillBucketEvent event) { /* ... */ }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) { /* ... */ }

    // D??PLACE LE RENDU ICI (Mod Bus + Client Only)
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        /**
         * Retient la config du HUD une fois chargee.
         *
         * <p>Sans cette prise, l'ecran de reglage pourrait lire la place des elements
         * mais pas la reecrire : Forge ne rend la config en main que par cet evenement.
         */
        @SubscribeEvent
        public static void onConfigLoading(net.minecraftforge.fml.event.config.ModConfigEvent.Loading event) {
            if (event.getConfig().getSpec() == cn.academy.client.hud.HudConfig.SPEC) {
                cn.academy.client.hud.HudConfig.capture(event.getConfig());
            }
            if (event.getConfig().getSpec() == Config.SPEC) {
                Config.capture(event.getConfig());
            }
        }

        @SubscribeEvent
        public static void onConfigReloading(net.minecraftforge.fml.event.config.ModConfigEvent.Reloading event) {
            if (event.getConfig().getSpec() == cn.academy.client.hud.HudConfig.SPEC) {
                cn.academy.client.hud.HudConfig.capture(event.getConfig());
            }
            if (event.getConfig().getSpec() == Config.SPEC) {
                Config.capture(event.getConfig());
            }
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.CAT_ENGINE.get(), CatEngineRenderer::new);
            // L'imag phase liquide : un bloc de fluide n'a pas de modele, ses nappes se
            // dessinent donc par-dessus, bloc de fluide par bloc de fluide.
            event.registerBlockEntityRenderer(ModBlockEntities.IMAG_PHASE.get(),
                    cn.academy.client.render.ImagPhaseLiquidRenderer::new);
            // L'eolienne : sa nacelle et ses pales, qui tournent.
            event.registerBlockEntityRenderer(ModBlockEntities.WINDGEN_MAIN.get(),
                    cn.academy.client.render.WindgenMainRenderer::new);
            // Le matrix : son socle, et ses plaques, qui ne se montrent que sur une machine
            // complete.
            event.registerBlockEntityRenderer(ModBlockEntities.MATRIX.get(),
                    cn.academy.client.render.MatrixRenderer::new);
            // La bille de silicium : la premiere entite du mod, et donc son premier rendu
            // d'entite. Sans cette ligne, l'objet se lancerait sans qu'on voie rien.
            event.registerEntityRenderer(ModEntities.SILBARN.get(), cn.academy.client.SilbarnRenderer::new);
            // Le bloc de la manipulation magnetique n'a rien a dessiner ici : c'est le rendu du
            // monde qui s'en charge, voir MagManipRenderer. Mais il lui faut QUAND MEME un rendu
            // d'entite, meme vide, et ce n'est pas une precaution de style : le jeu lit le rendu
            // de chaque entite visible pour savoir s'il faut la dessiner, et quand il n'en trouve
            // pas, il plante. Vecu le 30/09 : attraper un bloc fermait le jeu sur
            // « Cannot invoke EntityRenderer.shouldRender because "entityrenderer" is null ».
            event.registerEntityRenderer(ModEntities.MAG_MANIP_BLOCK.get(),
                    net.minecraft.client.renderer.entity.NoopRenderer::new);
            // La bille de plasma du meltdowner : deux images tournees vers le joueur, un halo
            // et un coeur qui clignotent — voir MdBallRenderer.
            event.registerEntityRenderer(ModEntities.MD_BALL.get(),
                    cn.academy.ability.client.md.MdBallRenderer::new);
        }

        @SubscribeEvent
        public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.SOLAR_GEN.get(), SolarGenScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.MATRIX.get(), MatrixScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.METAL_FORMER.get(), MetalFormerScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.IMAG_FUSOR.get(), ImagFusorScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.WINDGEN_BASE.get(), WindgenBaseScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.WINDGEN_MAIN.get(), WindgenMainScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.DEVELOPER.get(), DeveloperScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.PHASE_GENERATOR.get(), PhaseGeneratorScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.ABILITY_INTERFERER.get(), AbilityInterfererScreen::new);
                // Les ecrans des applications du terminal vivent a part des menus :
                // une application ne s'ouvre pas par un conteneur mais par la touche
                // du terminal. Sans cette ligne, l'arbre de competences s'installe
                // mais reste une icone morte.
                cn.academy.terminal.client.TerminalScreens.init();
                // Les boucles sonores des machines : le block entity est une classe
                // partagee, il ne peut donc pas nommer la sienne. C'est ici qu'on la lui
                // installe — et sans cette ligne, les machines tournent en silence.
                cn.academy.sound.client.MachineLoopSounds.install();
                // Sans cet appel, l'unite d'energie gardait toujours sa texture pleine :
                // la propriete d'item "academy:energy" n'etait jamais enregistree.
                ModItemProperties.addCustomItemProperties();
            });
        }
    }
}