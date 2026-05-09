package cn.academy;

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
                        // Syntaxe compatible 1.20.1 pour éviter les erreurs de version
                        private static final ResourceLocation STILL = new ResourceLocation(AcademyCraft.MOD_ID, "block/phase_liquid");
                        private static final ResourceLocation FLOW = new ResourceLocation(AcademyCraft.MOD_ID, "block/phase_liquid");

                        @Override
                        public ResourceLocation getStillTexture() { return STILL; }
                        @Override
                        public ResourceLocation getFlowingTexture() { return FLOW; }
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