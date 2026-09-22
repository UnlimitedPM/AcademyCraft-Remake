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
        ModCreativeTabs.register(modEventBus);
        ModMenus.register(modEventBus);
        // Types de biome modifier du mod (honore les options de generation de la config).
        cn.academy.worldgen.ConfigurableFeatureBiomeModifier.SERIALIZERS.register(modEventBus);
        modEventBus.addListener(cn.academy.ability.AbilityInit::init);
        modEventBus.addListener(cn.academy.terminal.TerminalInit::init);

        // Sans cette ligne, le fichier config/academy-common.toml n'est jamais cree.
        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, Config.SPEC);

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
        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.CAT_ENGINE.get(), CatEngineRenderer::new);
            // La bille de silicium : la premiere entite du mod, et donc son premier rendu
            // d'entite. Sans cette ligne, l'objet se lancerait sans qu'on voie rien.
            event.registerEntityRenderer(ModEntities.SILBARN.get(), cn.academy.client.SilbarnRenderer::new);
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
                // Sans cet appel, l'unite d'energie gardait toujours sa texture pleine :
                // la propriete d'item "academy:energy" n'etait jamais enregistree.
                ModItemProperties.addCustomItemProperties();
            });
        }
    }
}