package cn.academy;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "academy");

    public static final RegistryObject<BlockEntityType<CatEngineBlockEntity>> CAT_ENGINE =
            BLOCK_ENTITIES.register("cat_engine", () ->
                    BlockEntityType.Builder.of(CatEngineBlockEntity::new, ModBlocks.CAT_ENGINE.get()).build(null));

    // AJOUTE CETTE MÉTHODE :
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}