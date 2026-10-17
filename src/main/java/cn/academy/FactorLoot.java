package cn.academy;

import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Les facteurs d'induction dans les coffres de structure, comme dans l'original.
 *
 * <p>Portage de {@code ACItemAdditionalRegistry}, dont la liste {@code DEFAULT_APPEARANCE} nommait
 * cinq tables : la mine abandonnee, la pyramide du desert, le temple de la jungle, la bibliotheque
 * du fort et le donjon a generateur. Ce sont ces cinq-la, et seulement elles. Le joueur a demande de
 * ne pas y ajouter les structures venues plus tard — celles de la 1.20 sont plus faciles a trouver
 * et le facteur y perdrait sa valeur.
 *
 * <p>Le tirage reprend celui de l'original : quatre tirages par coffre, un facteur pour neuf fois du
 * vide, soit un facteur dans un coffre sur trois environ. Les quatre categories du port ont chacune
 * le meme poids, et le vide pese ce qu'il faut pour que la chance totale reste celle de l'original.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID)
public final class FactorLoot {

    /** Les cinq tables de l'original, et rien d'autre. */
    private static final Set<ResourceLocation> TABLES = Set.of(
            ResourceLocation.fromNamespaceAndPath("minecraft", "chests/abandoned_mineshaft"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "chests/desert_pyramid"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "chests/jungle_temple"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "chests/stronghold_library"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "chests/simple_dungeon"));

    /** Quatre tirages par coffre, comme le {@code RandomValueRange(4, 4)} de l'original. */
    private static final float ROLLS = 4f;

    /** Poids d'un facteur : quatre facteurs a 3 contre 108 de vide, soit 10 % par tirage. */
    private static final int FACTOR_WEIGHT = 3;
    private static final int EMPTY_WEIGHT = 108;

    private FactorLoot() {}

    @SubscribeEvent
    public static void surLeChargementDUneTable(LootTableLoadEvent event) {
        if (!TABLES.contains(event.getName())) return;

        LootPool pool = LootPool.lootPool()
                .name("academy:induction_factor")
                .setRolls(ConstantValue.exactly(ROLLS))
                .add(LootItem.lootTableItem(ModItems.FACTOR_ELECTRO.get()).setWeight(FACTOR_WEIGHT))
                .add(LootItem.lootTableItem(ModItems.FACTOR_MELT.get()).setWeight(FACTOR_WEIGHT))
                .add(LootItem.lootTableItem(ModItems.FACTOR_TELE.get()).setWeight(FACTOR_WEIGHT))
                .add(LootItem.lootTableItem(ModItems.FACTOR_VEC.get()).setWeight(FACTOR_WEIGHT))
                .add(EmptyLootItem.emptyItem().setWeight(EMPTY_WEIGHT))
                .build();

        event.getTable().addPool(pool);
    }
}
