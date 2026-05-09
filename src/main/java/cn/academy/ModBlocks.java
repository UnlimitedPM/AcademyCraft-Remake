package cn.academy;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.function.Supplier;

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
    public static final RegistryObject<Block> IMAG_FUSOR = registerBlock("imag_fusor",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.5f).sound(SoundType.METAL)));
    public static final RegistryObject<Block> METAL_FORMER = registerBlock("metal_former",
            () -> new HorizontalDirectionalBlock(BlockBehaviour.Properties.of()
                    .strength(3.5f).sound(SoundType.METAL)) {
                @Override
                protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
                    builder.add(FACING);
                }
                @Nullable
                @Override
                public BlockState getStateForPlacement(BlockPlaceContext context) {
                    return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
                }
            });
    public static final RegistryObject<Block> PHASE_GEN = registerBlock("phase_gen",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.5f).sound(SoundType.METAL)));
    public static final RegistryObject<Block> SOLAR_GEN = registerBlock("solar_gen",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.5f).sound(SoundType.METAL)));
    public static final RegistryObject<Block> CAT_ENGINE = registerBlock("cat_engine",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.5f).sound(SoundType.METAL)));

    // --- RÉSEAU SANS FIL ---
    public static final RegistryObject<Block> MATRIX = registerBlock("matrix",
            () -> new Block(BlockBehaviour.Properties.of().strength(4.0f).sound(SoundType.METAL)));
    public static final RegistryObject<Block> NODE_BASIC = registerBlock("node_basic",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.0f).sound(SoundType.METAL)));
    public static final RegistryObject<Block> NODE_STANDARD = registerBlock("node_standard",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.0f).sound(SoundType.METAL)));
    public static final RegistryObject<Block> NODE_ADVANCED = registerBlock("node_advanced",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.0f).sound(SoundType.METAL)));

    // --- ÉOLIENNE ---
    public static final RegistryObject<Block> WINDGEN_BASE = registerBlock("windgen_base",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.5f).sound(SoundType.METAL)));
    public static final RegistryObject<Block> WINDGEN_PILLAR = registerBlock("windgen_pillar",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.5f).sound(SoundType.METAL)));
    public static final RegistryObject<Block> WINDGEN_MAIN = registerBlock("windgen_main",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.5f).sound(SoundType.METAL)));

    // --- DÉVELOPPEMENT D'APPLICATIONS ---
    public static final RegistryObject<Block> DEV_NORMAL = registerBlock("dev_normal",
            () -> new Block(BlockBehaviour.Properties.of().strength(4.0f).sound(SoundType.METAL)));
    public static final RegistryObject<Block> DEV_ADVANCED = registerBlock("dev_advanced",
            () -> new Block(BlockBehaviour.Properties.of().strength(4.0f).sound(SoundType.METAL)));
    public static final RegistryObject<Block> ABILITY_INTERFERER = registerBlock("ability_interferer",
            () -> new Block(BlockBehaviour.Properties.of().strength(4.0f).sound(SoundType.METAL)) {
                public static final BooleanProperty ON = BooleanProperty.create("on");
                // Logique pour changer l'état 'on' plus tard...
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