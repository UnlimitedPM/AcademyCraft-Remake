package cn.academy;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Les facteurs d'induction et les morceaux de musique dans les coffres de structure.
 *
 * <p>Portage de {@code ACItemAdditionalRegistry}, qui posait deux pools sur cinq tables : un
 * facteur (quatre tirages, un facteur pour neuf fois du vide) et un morceau (un tirage, un morceau
 * pour neuf fois du vide). Les deux pools sont ici, et les listes de tables vivent dans
 * {@link StructureLootTables}, ou un test les relit.
 *
 * <p>Ce qui a change le 22/10, a la demande du joueur : les facteurs descendent aussi dans la cite
 * de l'End, les bastions et la forteresse du Nether, et les morceaux dans <b>toutes</b> les
 * structures du jeu — un morceau est un collectionnable, pas une recompense rare.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID)
public final class FactorLoot {

    /** Quatre tirages par coffre pour un facteur, comme le {@code RandomValueRange(4, 4)}. */
    private static final float FACTOR_ROLLS = 4f;

    /** Poids d'un facteur : quatre facteurs a 3 contre 108 de vide, soit 10 % par tirage. */
    private static final int FACTOR_WEIGHT = 3;
    private static final int FACTOR_EMPTY_WEIGHT = 108;

    /** Un seul tirage pour un morceau, comme l'original. */
    private static final float MEDIA_ROLLS = 1f;

    /**
     * Poids d'un morceau : trois morceaux a 10 contre 70 de vide, soit 30 % par coffre.
     *
     * <p>L'original avait un seul objet de musique, a 10 contre 90 de vide. Le port en a trois (la
     * 1.20.1 n'a plus de metadonnees pour ranger le morceau choisi), donc le meme poids par morceau
     * donnerait trois fois trop de musique : le vide est ramene a 70 pour que la chance totale
     * reste celle de l'original.
     */
    private static final int MEDIA_WEIGHT = 10;
    private static final int MEDIA_EMPTY_WEIGHT = 70;

    private FactorLoot() {}

    @SubscribeEvent
    public static void surLeChargementDUneTable(LootTableLoadEvent event) {
        if (StructureLootTables.hasFactor(event.getName())) {
            event.getTable().addPool(factorPool());
        }
        if (StructureLootTables.hasMedia(event.getName())) {
            event.getTable().addPool(mediaPool());
        }
    }

    /** Le pool des facteurs : les quatre categories, et du vide. */
    private static LootPool factorPool() {
        return LootPool.lootPool()
                .name("academy:induction_factor")
                .setRolls(ConstantValue.exactly(FACTOR_ROLLS))
                .add(LootItem.lootTableItem(ModItems.FACTOR_ELECTRO.get()).setWeight(FACTOR_WEIGHT))
                .add(LootItem.lootTableItem(ModItems.FACTOR_MELT.get()).setWeight(FACTOR_WEIGHT))
                .add(LootItem.lootTableItem(ModItems.FACTOR_TELE.get()).setWeight(FACTOR_WEIGHT))
                .add(LootItem.lootTableItem(ModItems.FACTOR_VEC.get()).setWeight(FACTOR_WEIGHT))
                .add(EmptyLootItem.emptyItem().setWeight(FACTOR_EMPTY_WEIGHT))
                .build();
    }

    /** Le pool des morceaux : les trois du mod, et du vide. */
    private static LootPool mediaPool() {
        return LootPool.lootPool()
                .name("academy:media")
                .setRolls(ConstantValue.exactly(MEDIA_ROLLS))
                .add(LootItem.lootTableItem(ModItems.MEDIA_RAILGUN.get()).setWeight(MEDIA_WEIGHT))
                .add(LootItem.lootTableItem(ModItems.MEDIA_JUDGELIGHT.get()).setWeight(MEDIA_WEIGHT))
                .add(LootItem.lootTableItem(ModItems.MEDIA_SISTERS_NOISE.get()).setWeight(MEDIA_WEIGHT))
                .add(EmptyLootItem.emptyItem().setWeight(MEDIA_EMPTY_WEIGHT))
                .build();
    }
}
