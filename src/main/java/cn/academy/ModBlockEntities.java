package cn.academy;

import cn.academy.energy.MatrixBlockEntity;
import cn.academy.energy.NodeBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AcademyCraft.MOD_ID);

    public static final RegistryObject<BlockEntityType<CatEngineBlockEntity>> CAT_ENGINE =
            BLOCK_ENTITIES.register("cat_engine", () ->
                    BlockEntityType.Builder.of(CatEngineBlockEntity::new, ModBlocks.CAT_ENGINE.get()).build(null));

    public static final RegistryObject<BlockEntityType<SolarGenBlockEntity>> SOLAR_GEN =
            BLOCK_ENTITIES.register("solar_gen", () ->
                    BlockEntityType.Builder.of(SolarGenBlockEntity::new, ModBlocks.SOLAR_GEN.get()).build(null));

    /**
     * Un seul type pour les trois qualites de noeud : la qualite se deduit du
     * bloc porteur, ce qui evite trois types et trois classes identiques.
     */
    public static final RegistryObject<BlockEntityType<NodeBlockEntity>> WIRELESS_NODE =
            BLOCK_ENTITIES.register("wireless_node", () ->
                    BlockEntityType.Builder.of(NodeBlockEntity::new,
                            ModBlocks.NODE_BASIC.get(),
                            ModBlocks.NODE_STANDARD.get(),
                            ModBlocks.NODE_ADVANCED.get()).build(null));

    /**
     * Le Matrix n'a de block entity que sur son bloc d'ancrage : le type est
     * declare sur le bloc entier, mais {@code MatrixBlock.newBlockEntity}
     * renvoie {@code null} pour les sept autres parties du multi-bloc.
     */
    public static final RegistryObject<BlockEntityType<MatrixBlockEntity>> MATRIX =
            BLOCK_ENTITIES.register("matrix", () ->
                    BlockEntityType.Builder.of(MatrixBlockEntity::new, ModBlocks.MATRIX.get()).build(null));

    public static final RegistryObject<BlockEntityType<MetalFormerBlockEntity>> METAL_FORMER =
            BLOCK_ENTITIES.register("metal_former", () ->
                    BlockEntityType.Builder.of(MetalFormerBlockEntity::new, ModBlocks.METAL_FORMER.get()).build(null));

    public static final RegistryObject<BlockEntityType<ImagFusorBlockEntity>> IMAG_FUSOR =
            BLOCK_ENTITIES.register("imag_fusor", () ->
                    BlockEntityType.Builder.of(ImagFusorBlockEntity::new, ModBlocks.IMAG_FUSOR.get()).build(null));

    /**
     * L'eolienne a deux block entities : la base qui produit, et le rotor qui
     * garde l'helice et surveille la zone balayee par les pales. Les blocs qui
     * completent les deux structures (moitie haute de la base, parties avant et
     * arriere du rotor) n'en ont pas : ce sont des leurres d'affichage.
     */
    public static final RegistryObject<BlockEntityType<WindgenBaseBlockEntity>> WINDGEN_BASE =
            BLOCK_ENTITIES.register("windgen_base", () ->
                    BlockEntityType.Builder.of(WindgenBaseBlockEntity::new, ModBlocks.WINDGEN_BASE.get()).build(null));

    public static final RegistryObject<BlockEntityType<WindgenMainBlockEntity>> WINDGEN_MAIN =
            BLOCK_ENTITIES.register("windgen_main", () ->
                    BlockEntityType.Builder.of(WindgenMainBlockEntity::new, ModBlocks.WINDGEN_MAIN.get()).build(null));

    /**
     * Les deux developeurs ont chacun leur type de block entity, parce que la
     * qualite y est passee a la construction : contrairement aux noeuds, elle ne se
     * deduit pas du bloc porteur sans le consulter.
     */
    public static final RegistryObject<BlockEntityType<DeveloperBlockEntity>> DEVELOPER_NORMAL =
            BLOCK_ENTITIES.register("developer_normal", () ->
                    BlockEntityType.Builder.of(
                            (pos, state) -> new DeveloperBlockEntity(pos, state,
                                    cn.academy.ability.develop.DeveloperType.NORMAL),
                            ModBlocks.DEV_NORMAL.get()).build(null));

    public static final RegistryObject<BlockEntityType<DeveloperBlockEntity>> DEVELOPER_ADVANCED =
            BLOCK_ENTITIES.register("developer_advanced", () ->
                    BlockEntityType.Builder.of(
                            (pos, state) -> new DeveloperBlockEntity(pos, state,
                                    cn.academy.ability.develop.DeveloperType.ADVANCED),
                            ModBlocks.DEV_ADVANCED.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}