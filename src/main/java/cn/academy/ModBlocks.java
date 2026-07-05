package cn.academy;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;
import java.util.function.Supplier;

import static cn.academy.ImagFusorBlock.FACING;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AcademyCraft.MOD_ID);

    // --- MINERAIS ---
    public static final RegistryObject<Block> CONSTRAINT_METAL_ORE = registerBlock("constraint_metal",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_ORE).sound(SoundType.STONE)));
    public static final RegistryObject<Block> CRYSTAL_ORE = registerBlock("crystal_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DIAMOND_ORE).sound(SoundType.STONE)));
    public static final RegistryObject<Block> IMAGSIL_ORE = registerBlock("imagsil_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_ORE).sound(SoundType.STONE)));
    public static final RegistryObject<Block> RESO_ORE = registerBlock("reso_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.EMERALD_ORE).sound(SoundType.STONE)));

    // --- MACHINES ET STRUCTURES ---
    public static final RegistryObject<Block> MACHINE_FRAME = registerBlock("machine_frame",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.0f).sound(SoundType.METAL)));

    public static final RegistryObject<Block> IMAG_FUSOR = BLOCKS.register("imag_fusor",
            () -> new ImagFusorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(5.0f)
                    .sound(SoundType.METAL)
                    // Sécurité pour la propriété LIT
                    .lightLevel(state -> state.hasProperty(BlockStateProperties.LIT) &&
                            state.getValue(BlockStateProperties.LIT) ? 12 : 0)));

    public static final RegistryObject<Block> METAL_FORMER = BLOCKS.register("metal_former",
            () -> new MetalFormerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(3.5f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> PHASE_GENERATOR = BLOCKS.register("phase_generator",
            () -> new PhaseGeneratorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(4.0f)
                    .sound(SoundType.METAL)
                    // Sécurité : on vérifie si la propriété existe avant de demander la valeur
                    .lightLevel(state -> state.hasProperty(PhaseGeneratorBlock.LEVEL) ?
                            (state.getValue(PhaseGeneratorBlock.LEVEL) > 0 ? 12 : 0) : 0)));

    public static final RegistryObject<SolarGenBlock> SOLAR_GEN = registerBlock("solar_gen",
            () -> new SolarGenBlock(BlockBehaviour.Properties.of().strength(3.5f).sound(SoundType.METAL)));

    public static final RegistryObject<Block> CAT_ENGINE = BLOCKS.register("cat_engine",
            () -> new CatEngineBlock(BlockBehaviour.Properties.of().noOcclusion()));

    // --- RÉSEAU SANS FIL ---
    public static final RegistryObject<Block> MATRIX = BLOCKS.register("matrix",
            () -> new MatrixBlock(BlockBehaviour.Properties.of().strength(4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<Block> NODE_BASIC = registerBlock("node_basic",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.0f).sound(SoundType.METAL)));
    public static final RegistryObject<Block> NODE_STANDARD = registerBlock("node_standard",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.0f).sound(SoundType.METAL)));
    public static final RegistryObject<Block> NODE_ADVANCED = registerBlock("node_advanced",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.0f).sound(SoundType.METAL)));

    // --- ÉOLIENNE ---
    public static final RegistryObject<WindgenBaseBlock> WINDGEN_BASE = BLOCKS.register("windgen_base",
            () -> new WindgenBaseBlock(BlockBehaviour.Properties.of().strength(3.5f).sound(SoundType.METAL)));
    public static final RegistryObject<Block> WINDGEN_PILLAR = registerBlock("windgen_pillar",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.5f).sound(SoundType.METAL).noOcclusion()) {
                @Override
                public net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState state, net.minecraft.world.level.BlockGetter world, net.minecraft.core.BlockPos pos) {
                    return net.minecraft.world.phys.shapes.Shapes.empty();
                }
            });
    public static final RegistryObject<Block> WINDGEN_MAIN = BLOCKS.register("windgen_main",
            () -> new WindgenMainBlock(BlockBehaviour.Properties.of().strength(3.5f).sound(SoundType.METAL).noOcclusion()));

    // --- DÉVELOPPEMENT D'APPLICATIONS ---
    public static final RegistryObject<Block> DEV_NORMAL = BLOCKS.register("dev_normal",
            () -> new DeveloperBlock(BlockBehaviour.Properties.of().strength(4.0f).noOcclusion()));
    public static final RegistryObject<Block> DEV_ADVANCED = BLOCKS.register("developer_advanced",
            () -> new DeveloperBlock(BlockBehaviour.Properties.of()
                    .strength(4.5f) // Un peu plus résistant que le normal
                    .noOcclusion()
            ));
    // Dans ModBlocks.java, remplacez la section ABILITY_INTERFERER par celle-ci :
    public static final RegistryObject<Block> ABILITY_INTERFERER = registerBlock("ability_interferer",
            () -> new Block(BlockBehaviour.Properties.of().strength(4.0f).sound(SoundType.METAL)) {
                public static final BooleanProperty ON = BooleanProperty.create("on");

                @Override
                protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
                    builder.add(ON);
                }
            });

    public static final RegistryObject<LiquidBlock> PHASE_LIQUID_BLOCK = BLOCKS.register("phase_liquid",
            () -> new LiquidBlock(ModFluids.SOURCE_PHASE_LIQUID, BlockBehaviour.Properties.copy(Blocks.WATER).noLootTable()));

    // --- MÉTHODES UTILITAIRES ---
    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}