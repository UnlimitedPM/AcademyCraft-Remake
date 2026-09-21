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
        ModBlockEntities.register(modEventBus); // <-- LA LIGNE MAGIQUE QUI MANQUAIT
        ModCreativeTabs.register(modEventBus);
        ModMenus.register(modEventBus);
        // Types de biome modifier du mod (honore les options de generation de la config).
        cn.academy.worldgen.ConfigurableFeatureBiomeModifier.SERIALIZERS.register(modEventBus);
        modEventBus.addListener(cn.academy.ability.AbilityInit::init);

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
        }

        @SubscribeEvent
        public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.SOLAR_GEN.get(), SolarGenScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.MATRIX.get(), MatrixScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.METAL_FORMER.get(), MetalFormerScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.IMAG_FUSOR.get(), ImagFusorScreen::new);
                // Sans cet appel, l'unite d'energie gardait toujours sa texture pleine :
                // la propriete d'item "academy:energy" n'etait jamais enregistree.
                ModItemProperties.addCustomItemProperties();
            });
        }
    }
}