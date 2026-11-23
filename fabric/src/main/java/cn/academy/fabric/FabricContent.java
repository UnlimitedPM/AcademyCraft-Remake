package cn.academy.fabric;

import cn.academy.AcademyCraftCommon;
import cn.academy.CatEngineBlock;
import cn.academy.CatEngineBlockEntity;
import cn.academy.EnergyUnitItem;
import cn.academy.SolarGenBlock;
import cn.academy.SolarGenBlockEntity;
import cn.academy.SolarGenMenu;
import cn.academy.TooltipItem;
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

    private static void plainBlock(String name, BlockBehaviour.Properties properties) {
        Block block = new Block(properties);
        Registry.register(BuiltInRegistries.BLOCK, id(name), block);
        BlockItem item = new BlockItem(block, new Item.Properties());
        Registry.register(BuiltInRegistries.ITEM, id(name), item);
        TAB_ITEMS.add(item);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(AcademyCraftCommon.MOD_ID, path);
    }
}
