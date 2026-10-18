package cn.academy;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Vector3f;

import java.util.function.Consumer;

public class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, AcademyCraft.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, AcademyCraft.MOD_ID);

    public static final RegistryObject<FluidType> PHASE_LIQUID_TYPE = FLUID_TYPES.register("phase_liquid",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("block.academy.phase_liquid")
                    .sound(SoundActions.BUCKET_FILL, net.minecraft.sounds.SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY)
                    .viscosity(6000)
                    .density(2000)
                    .canSwim(false)) { // Désactive la nage horizontale

                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        // L'original : `academy:blocks/black` pour les deux. Le fluide ne se
                        // dessine pas lui-meme — c'est son rendu (ImagPhaseLiquidRenderer)
                        // qui lui donne son aspect. Sa texture d'objet, elle, reste l'image
                        // de l'imag phase.
                        private static final ResourceLocation STILL = ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "block/black");
                        private static final ResourceLocation FLOW = ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "block/black");

                        // Le voile de l'eau de vanilla, quand la tete est dans le liquide.
                        // L'original l'avait sans rien demander : son bloc etait en
                        // `Material.WATER`, et Minecraft appliquait le filtre de l'eau.
                        private static final ResourceLocation OVERLAY =
                                ResourceLocation.withDefaultNamespace("textures/misc/underwater.png");

                        // Le bleu sombre de l'eau, celui de `EntityRenderer.updateFogColor`
                        // en 1.12.2 pour un bloc de material eau.
                        private static final Vector3f FOG = new Vector3f(0.02f, 0.02f, 0.2f);

                        @Override
                        public ResourceLocation getStillTexture() { return STILL; }
                        @Override
                        public ResourceLocation getFlowingTexture() { return FLOW; }
                        @Override
                        public ResourceLocation getRenderOverlayTexture(Minecraft mc) { return OVERLAY; }

                        @Override
                        public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level,
                                                       int renderDistance, float darkenWorldAmount,
                                                       Vector3f fluidFogColor) {
                            return FOG;
                        }
                    });
                }
            });

    public static final RegistryObject<FlowingFluid> SOURCE_PHASE_LIQUID = FLUIDS.register("phase_liquid",
            () -> new ForgeFlowingFluid.Source(ModFluids.PHASE_LIQUID_PROPERTIES));
    public static final RegistryObject<FlowingFluid> FLOWING_PHASE_LIQUID = FLUIDS.register("phase_liquid_flowing",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.PHASE_LIQUID_PROPERTIES));

    public static final ForgeFlowingFluid.Properties PHASE_LIQUID_PROPERTIES = new ForgeFlowingFluid.Properties(
            PHASE_LIQUID_TYPE, SOURCE_PHASE_LIQUID, FLOWING_PHASE_LIQUID)
            .slopeFindDistance(2)
            .levelDecreasePerBlock(3)
            .tickRate(30)
            .block(ModBlocks.PHASE_LIQUID_BLOCK);

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }
}