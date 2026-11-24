package cn.academy.fabric;

import cn.academy.AcademyCraftCommon;
import cn.academy.CatEngineBlock;
import cn.academy.CatEngineBlockEntity;
import cn.academy.EnergyUnitItem;
import cn.academy.MatrixBlock;
import cn.academy.MatrixMenu;
import cn.academy.ImagFusorBlock;
import cn.academy.ImagFusorBlockEntity;
import cn.academy.ImagFusorMenu;
import cn.academy.PhaseGeneratorBlock;
import cn.academy.PhaseGeneratorBlockEntity;
import cn.academy.PhaseGeneratorMenu;
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
import cn.academy.SilbarnItem;
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

    /** Entite de la bille de silicium. */
    public static net.minecraft.world.entity.EntityType<cn.academy.entity.EntitySilbarn> SILBARN_TYPE;

    /** Entite du grappin magnetique. */
    public static net.minecraft.world.entity.EntityType<cn.academy.entity.EntityMagHook> MAG_HOOK_TYPE;

    /** La bille de plasma du meltdowner, la piece du railgun, le bloc arrache. */
    public static net.minecraft.world.entity.EntityType<cn.academy.entity.EntityMdBall> MD_BALL_TYPE;
    public static net.minecraft.world.entity.EntityType<cn.academy.entity.EntityCoinThrowing> COIN_TYPE;
    public static net.minecraft.world.entity.EntityType<cn.academy.entity.EntityMagManipBlock>
            MAG_MANIP_BLOCK_TYPE;

    /** Matrix et noeuds sans fil. */
    public static BlockEntityType<MatrixBlockEntity> MATRIX_TYPE;
    public static MenuType<MatrixMenu> MATRIX_MENU;
    public static BlockEntityType<NodeBlockEntity> NODE_TYPE;
    public static MenuType<NodeMenu> NODE_MENU;

    /** Machines a fluide. */
    public static BlockEntityType<ImagFusorBlockEntity> IMAG_FUSOR_TYPE;
    public static MenuType<ImagFusorMenu> IMAG_FUSOR_MENU;
    public static BlockEntityType<PhaseGeneratorBlockEntity> PHASE_GEN_TYPE;
    public static MenuType<PhaseGeneratorMenu> PHASE_GEN_MENU;

    public static BlockEntityType<cn.academy.AbilityInterfererBlockEntity> ABILITY_INTERFERER_TYPE;
    public static MenuType<cn.academy.AbilityInterfererMenu> ABILITY_INTERFERER_MENU;

    /** Les deux developeurs, et le portable qui ouvre le meme ecran. */
    public static BlockEntityType<cn.academy.DeveloperBlockEntity> DEVELOPER_NORMAL_TYPE;
    public static BlockEntityType<cn.academy.DeveloperBlockEntity> DEVELOPER_ADVANCED_TYPE;
    public static MenuType<cn.academy.DeveloperMenu> DEVELOPER_MENU;

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
        // La piece n'est pas decorative : c'est elle qu'on jette pour armer le railgun.
        register("coin", new cn.academy.CoinItem());
        register("silbarn", new SilbarnItem());
        plainItem("logo");
        // Les morceaux du lecteur media : chacun donne son morceau au joueur, et refuse si
        // l'application n'est pas installee.
        register("media_only_my_railgun", new cn.academy.MediaItem("only_my_railgun"));
        register("media_level5_judgelight", new cn.academy.MediaItem("level5_judgelight"));
        register("media_sisters_noise", new cn.academy.MediaItem("sisters_noise"));
        // Et les quatre applications du terminal, qui ne s'installent que par ces objets.
        register("app_skill_tree", new cn.academy.AppInstallerItem("skill_tree"));
        register("app_media_player", new cn.academy.AppInstallerItem("media_player"));
        register("app_freq_transmitter", new cn.academy.AppInstallerItem("freq_transmitter"));
        register("app_settings", new cn.academy.AppInstallerItem("settings"));

        // Les trois objets qui ont un comportement commun : le grappin, l'installeur de terminal et
        // le tutoriel. Ils ne dependent que du cote serveur ou d'un ecran deja commun.
        register("mag_hook", new cn.academy.MagHookItem());
        register("terminal_installer",
                new cn.academy.TerminalInstallerItem(new Item.Properties().stacksTo(1)));
        register("tutorial", new cn.academy.TutorialItem());

        // Les quatre facteurs d'induction : ils disent quelle categorie le joueur veut apprendre.
        register("factor_electromaster", new cn.academy.FactorItem("electromaster"));
        register("factor_meltdowner", new cn.academy.FactorItem("meltdowner"));
        register("factor_teleporter", new cn.academy.FactorItem("teleporter"));
        register("factor_vecmanip", new cn.academy.FactorItem("vecmanip"));

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

        // --- Machines a phase liquide ---
        // L'unite vide se remplit du liquide pose ; l'unite de phase le repose (voir FabricFluids).
        register("matter_unit", new cn.academy.EmptyUnitItem(new Item.Properties().stacksTo(16)));
        // Le liquide lui-meme : le bloc, les deux variantes du fluide et l'unite qui le pose. Voir
        // FabricFluids — c'est ce qui manquait pour qu'on puisse le poser par terre.
        FabricFluids.register();
        TAB_ITEMS.add(FabricFluids.UNIT);
        // Et l'objet imag_phase, qui est le bloc de liquide lui-meme : chez Forge c'est un BlockItem
        // et il posait le liquide ; ici c'etait un objet decoratif qui ne posait rien.
        register("imag_phase", new BlockItem(FabricFluids.PHASE_LIQUID_BLOCK, new Item.Properties()));

        IMAG_FUSOR_MENU = Registry.register(BuiltInRegistries.MENU, id("imag_fusor"),
                new ExtendedScreenHandlerType<>((syncId, inventory, buf) ->
                        new ImagFusorMenu(FabricContent.IMAG_FUSOR_MENU, syncId, inventory, buf)));
        Block imagFusor = new ImagFusorBlock(
                BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0f).sound(SoundType.METAL)
                        .lightLevel(state -> state.getValue(ImagFusorBlock.LIT) ? 12 : 0),
                () -> IMAG_FUSOR_TYPE,
                (pos, state) -> new ImagFusorBlockEntity(IMAG_FUSOR_TYPE, IMAG_FUSOR_MENU, pos, state));
        Registry.register(BuiltInRegistries.BLOCK, id("imag_fusor"), imagFusor);
        IMAG_FUSOR_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("imag_fusor"),
                BlockEntityType.Builder.of(
                        (pos, state) -> new ImagFusorBlockEntity(IMAG_FUSOR_TYPE, IMAG_FUSOR_MENU, pos, state),
                        imagFusor).build(null));
        register("imag_fusor", new BlockItem(imagFusor, new Item.Properties()));

        PHASE_GEN_MENU = Registry.register(BuiltInRegistries.MENU, id("phase_gen"),
                new ExtendedScreenHandlerType<>((syncId, inventory, buf) ->
                        new PhaseGeneratorMenu(FabricContent.PHASE_GEN_MENU, syncId, inventory, buf)));
        Block phaseGen = new PhaseGeneratorBlock(
                BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).sound(SoundType.METAL)
                        .lightLevel(state -> state.getValue(PhaseGeneratorBlock.LEVEL) > 0 ? 12 : 0),
                () -> PHASE_GEN_TYPE,
                (pos, state) -> new PhaseGeneratorBlockEntity(PHASE_GEN_TYPE, PHASE_GEN_MENU, pos, state));
        Registry.register(BuiltInRegistries.BLOCK, id("phase_gen"), phaseGen);
        PHASE_GEN_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("phase_gen"),
                BlockEntityType.Builder.of(
                        (pos, state) -> new PhaseGeneratorBlockEntity(PHASE_GEN_TYPE, PHASE_GEN_MENU, pos, state),
                        phaseGen).build(null));
        register("phase_gen", new BlockItem(phaseGen, new Item.Properties()));

        // --- Brouilleur d'aptitudes : il ne brouille que sur le serveur, mais son ecran est commun ---
        ABILITY_INTERFERER_MENU = Registry.register(BuiltInRegistries.MENU, id("ability_interferer"),
                new ExtendedScreenHandlerType<>((syncId, inventory, buf) ->
                        new cn.academy.AbilityInterfererMenu(
                                FabricContent.ABILITY_INTERFERER_MENU, syncId, inventory, buf)));
        Block interferer = new cn.academy.AbilityInterfererBlock(
                BlockBehaviour.Properties.of().strength(4.0f).requiresCorrectToolForDrops()
                        .sound(SoundType.METAL),
                () -> ABILITY_INTERFERER_TYPE,
                (pos, state) -> new cn.academy.AbilityInterfererBlockEntity(
                        ABILITY_INTERFERER_TYPE, ABILITY_INTERFERER_MENU, pos, state));
        Registry.register(BuiltInRegistries.BLOCK, id("ability_interferer"), interferer);
        ABILITY_INTERFERER_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                id("ability_interferer"),
                BlockEntityType.Builder.of(
                        (pos, state) -> new cn.academy.AbilityInterfererBlockEntity(
                                ABILITY_INTERFERER_TYPE, ABILITY_INTERFERER_MENU, pos, state),
                        interferer).build(null));
        register("ability_interferer", new BlockItem(interferer, new Item.Properties()));

        // --- Les deux developeurs : le meme bloc, deux qualites. Seul l'ancrage porte le block
        // entity, donc le type se declare par qualite comme chez Forge. ---
        DEVELOPER_MENU = Registry.register(BuiltInRegistries.MENU, id("developer"),
                new ExtendedScreenHandlerType<>((syncId, inventory, buf) ->
                        new cn.academy.DeveloperMenu(
                                FabricContent.DEVELOPER_MENU, syncId, inventory, buf)));
        Block devNormal = new cn.academy.DeveloperBlock(
                BlockBehaviour.Properties.of().strength(4.0f).noOcclusion(),
                () -> cn.academy.ability.develop.DeveloperType.NORMAL,
                () -> DEVELOPER_NORMAL_TYPE,
                () -> DEVELOPER_MENU);
        Registry.register(BuiltInRegistries.BLOCK, id("dev_normal"), devNormal);
        DEVELOPER_NORMAL_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                id("developer_normal"),
                BlockEntityType.Builder.of(
                        (pos, state) -> new cn.academy.DeveloperBlockEntity(DEVELOPER_NORMAL_TYPE,
                                DEVELOPER_MENU, pos, state,
                                cn.academy.ability.develop.DeveloperType.NORMAL),
                        devNormal).build(null));
        register("dev_normal", new BlockItem(devNormal, new Item.Properties()));

        Block devAdvanced = new cn.academy.DeveloperBlock(
                BlockBehaviour.Properties.of().strength(4.5f).noOcclusion(),
                () -> cn.academy.ability.develop.DeveloperType.ADVANCED,
                () -> DEVELOPER_ADVANCED_TYPE,
                () -> DEVELOPER_MENU);
        Registry.register(BuiltInRegistries.BLOCK, id("dev_advanced"), devAdvanced);
        DEVELOPER_ADVANCED_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                id("dev_advanced"),
                BlockEntityType.Builder.of(
                        (pos, state) -> new cn.academy.DeveloperBlockEntity(DEVELOPER_ADVANCED_TYPE,
                                DEVELOPER_MENU, pos, state,
                                cn.academy.ability.develop.DeveloperType.ADVANCED),
                        devAdvanced).build(null));
        register("dev_advanced", new BlockItem(devAdvanced, new Item.Properties()));

        // Le developeur portable : meme ecran, dans la poche. Sans lui, le succes qui l'annonce
        // nommait un objet absent et toute la chaine de succes tombait avec lui.
        register("developer_portable", new cn.academy.DeveloperPortableItem());

        // --- Entites ---
        SILBARN_TYPE = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("silbarn"),
                net.minecraft.world.entity.EntityType.Builder
                        .<cn.academy.entity.EntitySilbarn>of(cn.academy.entity.EntitySilbarn::new,
                                net.minecraft.world.entity.MobCategory.MISC)
                        .sized(cn.academy.entity.SilbarnVisuals.HIT_SIZE,
                                cn.academy.entity.SilbarnVisuals.HIT_SIZE)
                        .clientTrackingRange(4)
                        .updateInterval(10)
                        .build("silbarn"));

        // Le grappin : l'objet le lance par le type de contenu, donc le type doit exister ici aussi,
        // sans quoi un clic droit le trouvait nul. Cadence d'un tick, comme chez Forge : a deux blocs
        // par tick le crochet bondirait sinon d'un bond a chaque image.
        MAG_HOOK_TYPE = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("mag_hook"),
                net.minecraft.world.entity.EntityType.Builder
                        .<cn.academy.entity.EntityMagHook>of(cn.academy.entity.EntityMagHook::new,
                                net.minecraft.world.entity.MobCategory.MISC)
                        .sized(cn.academy.entity.MagHookVisuals.FLY_SIZE,
                                cn.academy.entity.MagHookVisuals.FLY_SIZE)
                        .clientTrackingRange(4)
                        .updateInterval(1)
                        .build("mag_hook"));

        // La bille de plasma du meltdowner, la piece du railgun et le bloc arrache par la
        // manipulation magnetique : trois entites que les competences creent par leur type de
        // contenu. Sans elles, la competence trouvait un type nul et rien ne partait.
        MD_BALL_TYPE = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("md_ball"),
                net.minecraft.world.entity.EntityType.Builder
                        .<cn.academy.entity.EntityMdBall>of(cn.academy.entity.EntityMdBall::new,
                                net.minecraft.world.entity.MobCategory.MISC)
                        .sized(0.25f, 0.25f)
                        .clientTrackingRange(10)
                        .updateInterval(1)
                        .build("md_ball"));

        COIN_TYPE = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("coin"),
                net.minecraft.world.entity.EntityType.Builder
                        .<cn.academy.entity.EntityCoinThrowing>of(
                                cn.academy.entity.EntityCoinThrowing::new,
                                net.minecraft.world.entity.MobCategory.MISC)
                        .sized(0.25f, 0.25f)
                        .clientTrackingRange(8)
                        .updateInterval(1)
                        .build("coin"));

        MAG_MANIP_BLOCK_TYPE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
                id("mag_manip_block"),
                net.minecraft.world.entity.EntityType.Builder
                        .<cn.academy.entity.EntityMagManipBlock>of(
                                cn.academy.entity.EntityMagManipBlock::new,
                                net.minecraft.world.entity.MobCategory.MISC)
                        .sized(1f, 1f)
                        .clientTrackingRange(8)
                        .updateInterval(2)
                        .build("mag_manip_block"));

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
