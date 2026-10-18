package cn.academy;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
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

                /**
                 * La descente dans le liquide, celle de la 1.12.2.
                 *
                 * <p>PIEGE DE VERSION : par defaut Forge laisse faire la physique de l'eau, et
                 * celle de la 1.20.1 retire la gravite DIVISEE PAR SEIZE a chaque tick
                 * (`LivingEntity.getFluidFallingAdjustedMovement` : `motion.y - gravity / 16`).
                 * La 1.12.2 la retirait pleine moins son frein de 0,8, donc on descendait quatre
                 * fois plus vite. Le joueur l'a senti tout de suite : « on coule très lentement
                 * dedans alors que normalement non ».
                 *
                 * <p>C'est donc la branche de l'eau de `LivingEntity.travel`, recopiee avec
                 * `gravity / 4` : a la gravite d'un joueur (0,08) cela fait 0,02 par tick, le
                 * chiffre exact de `Entity.updateMotion` en 1.12.2. Rendre `true` dit a Forge que
                 * le deplacement est fait, ce qui evite sa version.
                 */
                @Override
                public boolean move(FluidState state, LivingEntity entity, Vec3 input, double gravity) {
                    double departY = entity.getY();

                    entity.moveRelative(0.02f, input);
                    entity.move(MoverType.SELF, entity.getDeltaMovement());
                    Vec3 motion = entity.getDeltaMovement();
                    if (entity.horizontalCollision && entity.onClimbable()) {
                        motion = new Vec3(motion.x, 0.2d, motion.z);
                    }

                    // Le frein de l'eau, puis la gravite : `motionY * 0.8 - 0.02` en 1.12.2, la ou
                    // la 1.20.1 fait `motionY * 0.8 - gravite / 16`.
                    motion = new Vec3(motion.x * 0.8d, motion.y * 0.8d, motion.z * 0.8d);
                    if (!entity.isNoGravity()) {
                        motion = new Vec3(motion.x, motion.y - gravity / 4.0d, motion.z);
                    }

                    // La force de la 1.12.2 quand on nage contre une paroi et qu'il y a de la place
                    // au-dessus de la tete : on remonte le long du mur au lieu d'y rester colle.
                    if (entity.horizontalCollision
                            && entity.isFree(motion.x, motion.y + 0.6d - entity.getY() + departY, motion.z)) {
                        motion = new Vec3(motion.x, 0.3d, motion.z);
                    }
                    entity.setDeltaMovement(motion);
                    return true;
                }

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

                        // Le bleu de l'eau, celui de `EntityRenderer.updateFogColor` en 1.12.2
                        // pour un bloc de material eau, remonte d'un cran pour que le voile se
                        // lise comme bleu et non comme du noir.
                        private static final Vector3f FOG = new Vector3f(0.03f, 0.05f, 0.28f);
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