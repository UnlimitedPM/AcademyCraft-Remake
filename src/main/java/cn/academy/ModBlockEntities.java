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

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}