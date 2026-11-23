package cn.academy.fabric;

import cn.academy.AcademyCraftCommon;
import cn.academy.CatEngineBlock;
import cn.academy.CatEngineBlockEntity;
import cn.academy.EnergyUnitItem;
import cn.academy.MatrixBlock;
import cn.academy.MatrixMenu;
import cn.academy.MetalFormerBlock;
import cn.academy.MetalFormerBlockEntity;
import cn.academy.MetalFormerMenu;
import cn.academy.SolarGenBlock;
import cn.academy.SolarGenBlockEntity;
import cn.academy.SolarGenMenu;
import cn.academy.TooltipItem;
import cn.academy.WindgenBaseBlock;
import cn.academy.WindgenBaseBlockEntity;
import cn.academy.WindgenBaseMenu;
import cn.academy.WindgenMainBlock;
import cn.academy.WindgenMainBlockEntity;
import cn.academy.WindgenMainMenu;
import cn.academy.NodeMenu;
import cn.academy.energy.MatrixBlockEntity;
import cn.academy.energy.NodeBlock;
import cn.academy.energy.NodeBlockEntity;
import cn.academy.energy.NodeType;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.List;

/**
 * Premier palier du portage Fabric : les objets et blocs qui n'utilisent que des APIs vanilla.
 *
 * <p>Le reste du mod (machines, capacites, reseau, aptitudes) reference des APIs Forge qui n'ont
 * pas d'equivalent direct sous Fabric. Ils viendront au fur et a mesure que les abstractions de
 * common/ seront ecrites. Voir FABRIC_PORT.md.
 */
public final class FabricContent {
    private static final List<Item> TAB_ITEMS = new ArrayList<>();

    private FabricContent() {
    }

    /** Type de block entity du moteur a chat ; expose pour le rendu client. */
    public static BlockEntityType<CatEngineBlockEntity> CAT_ENGINE_TYPE;

    /** Type de block entity du generateur solaire, partage avec Forge/NeoForge. */
    public static BlockEntityType<SolarGenBlockEntity> SOLAR_GEN_TYPE;

    /** Menu du generateur solaire (donnees d'ouverture : position du bloc). */
    public static MenuType<SolarGenMenu> SOLAR_GEN_MENU;

    /** Type de block entity du formeur de metal. */
    public static BlockEntityType<MetalFormerBlockEntity> METAL_FORMER_TYPE;

    /** Menu du formeur de metal. */
    public static MenuType<MetalFormerMenu> METAL_FORMER_MENU;

    /** Eolienne : base et rotor ont chacun leur type et leur menu. */
    public static BlockEntityType<WindgenBaseBlockEntity> WINDGEN_BASE_TYPE;
    public static BlockEntityType<WindgenMainBlockEntity> WINDGEN_MAIN_TYPE;
    public static MenuType<WindgenBaseMenu> WINDGEN_BASE_MENU;
    public static MenuType<WindgenMainMenu> WINDGEN_MAIN_MENU;

    /** Matrix et noeuds sans fil. */
    public static BlockEntityType<MatrixBlockEntity> MATRIX_TYPE;
    public static MenuType<MatrixMenu> MATRIX_MENU;
    public static BlockEntityType<NodeBlockEntity> NODE_TYPE;
    public static MenuType<NodeMenu> NODE_MENU;

    public static void register() {
        FabricSounds.register();

        // --- Materiaux et composants (Item vanilla) ---
        plainItem("crystal_low");
        plainItem("crystal_normal");
        plainItem("crystal_pure");
        plainItem("reso_crystal");
        plainItem("constraint_ingot");
        plainItem("constraint_plate");
        plainItem("imag_silicon_ingot");
        plainItem("wafer");
        plainItem("imag_silicon_piece");
        plainItem("brain_component");
        plainItem("info_component");
        plainItem("resonance_component");
        plainItem("energy_convert_component");
        plainItem("reinforced_iron_plate");
        plainItem("calc_chip");
        plainItem("data_chip");
        plainItem("mat_core_0");
        plainItem("mat_core_1");
        plainItem("mat_core_2");
        plainItem("needle");

        // --- Premier objet energetique porte depuis common/ ---
        register("energy_unit", new EnergyUnitItem());
        register("magnetic_coil", new TooltipItem("item.academy.magnetic_coil.desc"));

        // --- Blocs vanilla (minerais et cadre de machine) ---
        plainBlock("constraint_metal", BlockBehaviour.Properties.copy(Blocks.IRON_ORE).sound(SoundType.STONE));
        plainBlock("crystal_ore", BlockBehaviour.Properties.of()
                .strength(3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
        plainBlock("imagsil_ore", BlockBehaviour.Properties.copy(Blocks.IRON_ORE).sound(SoundType.STONE));
        plainBlock("reso_ore", BlockBehaviour.Properties.of()
                .strength(3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
        plainBlock("machine_frame", BlockBehaviour.Properties.of().strength(3.0f).sound(SoundType.METAL));

        // --- Moteur a chat : premier bloc a block entity partage avec Forge/NeoForge ---
        Block catEngine = new CatEngineBlock(BlockBehaviour.Properties.of().noOcclusion(),
                () -> CAT_ENGINE_TYPE);
        Registry.register(BuiltInRegistries.BLOCK, id("cat_engine"), catEngine);
        CAT_ENGINE_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("cat_engine"),
                BlockEntityType.Builder.of(
                        (pos, state) -> new CatEngineBlockEntity(CAT_ENGINE_TYPE, pos, state),
                        catEngine).build(null));
        register("cat_engine", new BlockItem(catEngine, new Item.Properties()));

        // --- Generateur solaire : premiere machine complete portee depuis common/ ---
        SOLAR_GEN_MENU = Registry.register(BuiltInRegistries.MENU, id("solar_gen"),
                new ExtendedScreenHandlerType<>((syncId, inventory, buf) ->
                        new SolarGenMenu(FabricContent.SOLAR_GEN_MENU, syncId, inventory, buf)));
        Block solarGen = new SolarGenBlock(
                BlockBehaviour.Properties.of().strength(3.5f).sound(SoundType.METAL),
                () -> SOLAR_GEN_TYPE,
                (pos, state) -> new SolarGenBlockEntity(SOLAR_GEN_TYPE, SOLAR_GEN_MENU, pos, state));
        Registry.register(BuiltInRegistries.BLOCK, id("solar_gen"), solarGen);
        SOLAR_GEN_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("solar_gen"),
                BlockEntityType.Builder.of(
                        (pos, state) -> new SolarGenBlockEntity(SOLAR_GEN_TYPE, SOLAR_GEN_MENU, pos, state),
                        solarGen).build(null));
        register("solar_gen", new BlockItem(solarGen, new Item.Properties()));

        // --- Formeur de metal : premier consommateur du reseau energetique ---
        METAL_FORMER_MENU = Registry.register(BuiltInRegistries.MENU, id("metal_former"),
                new ExtendedScreenHandlerType<>((syncId, inventory, buf) ->
                        new MetalFormerMenu(FabricContent.METAL_FORMER_MENU, syncId, inventory, buf)));
        Block metalFormer = new MetalFormerBlock(
                BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3.5f).sound(SoundType.METAL),
                () -> METAL_FORMER_TYPE,
                (pos, state) -> new MetalFormerBlockEntity(METAL_FORMER_TYPE, METAL_FORMER_MENU, pos, state));
        Registry.register(BuiltInRegistries.BLOCK, id("metal_former"), metalFormer);
        METAL_FORMER_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("metal_former"),
                BlockEntityType.Builder.of(
                        (pos, state) -> new MetalFormerBlockEntity(METAL_FORMER_TYPE, METAL_FORMER_MENU, pos, state),
                        metalFormer).build(null));
        register("metal_former", new BlockItem(metalFormer, new Item.Properties()));

        // --- Eolienne : base productrice, piliers, rotor ---
        register("windgen_fan", new Item(new Item.Properties().stacksTo(1)));
        plainBlock("windgen_pillar", BlockBehaviour.Properties.of()
                .strength(3.5f).sound(SoundType.METAL).noOcclusion());

        WINDGEN_BASE_MENU = Registry.register(BuiltInRegistries.MENU, id("windgen_base"),
                new ExtendedScreenHandlerType<>((syncId, inventory, buf) ->
                        new WindgenBaseMenu(FabricContent.WINDGEN_BASE_MENU, syncId, inventory, buf)));
        Block windgenBase = new WindgenBaseBlock(
                BlockBehaviour.Properties.of().strength(3.5f).sound(SoundType.METAL),
                () -> WINDGEN_BASE_TYPE,
                (pos, state) -> new WindgenBaseBlockEntity(WINDGEN_BASE_TYPE, WINDGEN_BASE_MENU, pos, state));
        Registry.register(BuiltInRegistries.BLOCK, id("windgen_base"), windgenBase);
        WINDGEN_BASE_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("windgen_base"),
                BlockEntityType.Builder.of(
                        (pos, state) -> new WindgenBaseBlockEntity(WINDGEN_BASE_TYPE, WINDGEN_BASE_MENU, pos, state),
                        windgenBase).build(null));
        register("windgen_base", new BlockItem(windgenBase, new Item.Properties()));

        WINDGEN_MAIN_MENU = Registry.register(BuiltInRegistries.MENU, id("windgen_main"),
                new ExtendedScreenHandlerType<>((syncId, inventory, buf) ->
                        new WindgenMainMenu(FabricContent.WINDGEN_MAIN_MENU, syncId, inventory, buf)));
        Block windgenMain = new WindgenMainBlock(
                BlockBehaviour.Properties.of().strength(3.5f).sound(SoundType.METAL).noOcclusion(),
                () -> WINDGEN_MAIN_TYPE,
                (pos, state) -> new WindgenMainBlockEntity(WINDGEN_MAIN_TYPE, WINDGEN_MAIN_MENU, pos, state));
        Registry.register(BuiltInRegistries.BLOCK, id("windgen_main"), windgenMain);
        WINDGEN_MAIN_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("windgen_main"),
                BlockEntityType.Builder.of(
                        (pos, state) -> new WindgenMainBlockEntity(WINDGEN_MAIN_TYPE, WINDGEN_MAIN_MENU, pos, state),
                        windgenMain).build(null));
        register("windgen_main", new BlockItem(windgenMain, new Item.Properties()));

        // --- Matrix et noeuds sans fil ---
        MATRIX_MENU = Registry.register(BuiltInRegistries.MENU, id("matrix"),
                new ExtendedScreenHandlerType<>((syncId, inventory, buf) ->
                        new MatrixMenu(FabricContent.MATRIX_MENU, syncId, inventory, buf)));
        Block matrix = new MatrixBlock(
                BlockBehaviour.Properties.of().strength(4.0f).sound(SoundType.METAL).noOcclusion(),
                (pos, state) -> new MatrixBlockEntity(MATRIX_TYPE, MATRIX_MENU, pos, state));
        Registry.register(BuiltInRegistries.BLOCK, id("matrix"), matrix);
        MATRIX_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("matrix"),
                BlockEntityType.Builder.of(
                        (pos, state) -> new MatrixBlockEntity(MATRIX_TYPE, MATRIX_MENU, pos, state),
                        matrix).build(null));
        register("matrix", new BlockItem(matrix, new Item.Properties()));

        NODE_MENU = Registry.register(BuiltInRegistries.MENU, id("node"),
                new ExtendedScreenHandlerType<>((syncId, inventory, buf) ->
                        new NodeMenu(FabricContent.NODE_MENU, syncId, inventory, buf)));
        Block nodeBasic = nodeBlock(NodeType.BASIC, "node_basic");
        Block nodeStandard = nodeBlock(NodeType.STANDARD, "node_standard");
        Block nodeAdvanced = nodeBlock(NodeType.ADVANCED, "node_advanced");
        NODE_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("wireless_node"),
                BlockEntityType.Builder.of(
                        (pos, state) -> new NodeBlockEntity(NODE_TYPE, NODE_MENU, pos, state),
                        nodeBasic, nodeStandard, nodeAdvanced).build(null));

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("academy_tab"),
                FabricItemGroup.builder()
                        .title(Component.translatable("itemGroup.academy_tab"))
                        .icon(() -> new ItemStack(BuiltInRegistries.ITEM.get(id("crystal_normal"))))
                        .displayItems((parameters, output) -> TAB_ITEMS.forEach(output::accept))
                        .build());
    }

    private static void plainItem(String name) {
        register(name, new Item(new Item.Properties()));
    }

    private static void register(String name, Item item) {
        Registry.register(BuiltInRegistries.ITEM, id(name), item);
        TAB_ITEMS.add(item);
    }

    /** Un noeud sans fil d'une qualite donnee : meme bloc, memes proprietes, type different. */
    private static Block nodeBlock(NodeType type, String name) {
        Block block = new NodeBlock(type, BlockBehaviour.Properties.of()
                .strength(3.0f).requiresCorrectToolForDrops().sound(SoundType.METAL),
                (pos, state) -> new NodeBlockEntity(NODE_TYPE, NODE_MENU, pos, state));
        Registry.register(BuiltInRegistries.BLOCK, id(name), block);
        register(name, new BlockItem(block, new Item.Properties()));
        return block;
    }

    private static void plainBlock(String name, BlockBehaviour.Properties properties) {        Block block = new Block(properties);
        Registry.register(BuiltInRegistries.BLOCK, id(name), block);
        BlockItem item = new BlockItem(block, new Item.Properties());
        Registry.register(BuiltInRegistries.ITEM, id(name), item);
        TAB_ITEMS.add(item);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(AcademyCraftCommon.MOD_ID, path);
    }
}
