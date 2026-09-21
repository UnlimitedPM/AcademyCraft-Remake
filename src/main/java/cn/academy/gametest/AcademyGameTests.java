package cn.academy.gametest;

import cn.academy.AcademyCraft;
import cn.academy.ModBlocks;
import cn.academy.ModItems;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Tests headless executes par {@code gradlew runGameTestServer} :
 * aucun rendu graphique, aucun client lourd, la partie se termine seule.
 *
 * Objectif : detecter tot les erreurs de donnees (recipes non chargees,
 * worldgen absent, objets non enregistres) que l'on ne voit pas en jeu.
 */
@GameTestHolder(AcademyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AcademyGameTests {

    // ------------------------------------------------------------------
    // Registres
    // ------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void blocksAndItemsAreRegistered(GameTestHelper helper) {
        helper.assertTrue(ModBlocks.CONSTRAINT_METAL_ORE.get() != null, "constraint_metal non enregistre");
        helper.assertTrue(ModBlocks.PHASE_GENERATOR.get() != null, "phase_generator non enregistre");
        helper.assertTrue(ModBlocks.SOLAR_GEN.get() != null, "solar_gen non enregistre");
        helper.assertTrue(ModBlocks.DEV_ADVANCED.get() != null, "developer_advanced non enregistre");
        helper.assertTrue(ModItems.CRYSTAL_LOW.get() != null, "crystal_low non enregistre");
        helper.assertTrue(ModItems.MATTER_UNIT.get() != null, "matter_unit non enregistre");
        helper.assertTrue(ModItems.FACTOR_ELECTRO.get() != null, "factor_electromaster non enregistre");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // Recipes : "Loaded N recipes" dans le log compte des TYPES de recipe,
    // pas des recipes. On verifie donc les identifiants un par un.
    // ------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void craftingRecipesAreLoaded(GameTestHelper helper) {
        for (String id : new String[]{"academy:constraint_plate", "academy:machine_frame", "academy:solar_gen",
                "academy:node_basic", "academy:energy_unit"}) {
            assertRecipe(helper, id, RecipeType.CRAFTING);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void smeltingRecipesAreLoaded(GameTestHelper helper) {
        assertRecipe(helper, "academy:crystal_low", RecipeType.SMELTING);
        assertRecipe(helper, "academy:constraint_ingot", RecipeType.SMELTING);
        helper.succeed();
    }

    /** Toutes les recipes du mod doivent avoir ete chargees, pas juste quelques-unes. */
    @GameTest(template = "empty")
    public static void everyAcademyCraftingRecipeIsLoaded(GameTestHelper helper) {
        long crafting = helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .filter(r -> r.getType() == RecipeType.CRAFTING)
                .filter(r -> r.getId().getNamespace().equals(AcademyCraft.MOD_ID))
                .count();
        assertTrue(helper, crafting >= 40,
                "seulement " + crafting + " recipes de crafting 'academy' chargees (>= 40 attendues)");
        helper.succeed();
    }

    private static void assertRecipe(GameTestHelper helper, String id, RecipeType<?> type) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        boolean found = helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .anyMatch(r -> r.getId().equals(rl) && r.getType() == type);
        assertTrue(helper, found, "recipe manquante : " + id);
    }

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        helper.assertTrue(condition, message);
    }

    // ------------------------------------------------------------------
    // Worldgen : les minerais doivent exister dans les registres de datapack.
    // ------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void oreWorldgenIsDeclared(GameTestHelper helper) {
        var access = helper.getLevel().getServer().registryAccess();
        var configured = access.registryOrThrow(Registries.CONFIGURED_FEATURE);
        var placed = access.registryOrThrow(Registries.PLACED_FEATURE);

        for (String ore : new String[]{"constraint_metal", "crystal_ore", "imagsil_ore", "reso_ore"}) {
            ResourceLocation rl = new ResourceLocation(AcademyCraft.MOD_ID, ore);
            assertTrue(helper, configured.containsKey(rl), "configured_feature manquante : " + rl);
            assertTrue(helper, placed.containsKey(rl), "placed_feature manquante : " + rl);
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // Loot tables : chaque bloc doit lacher quelque chose.
    // ------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void oreBlocksHaveLootTables(GameTestHelper helper) {
        var lootData = helper.getLevel().getServer().getLootData();
        for (String ore : new String[]{"constraint_metal", "crystal_ore", "imagsil_ore", "reso_ore"}) {
            ResourceLocation rl = new ResourceLocation(AcademyCraft.MOD_ID, "blocks/" + ore);
            assertTrue(helper, lootData.getElementOptional(LootDataType.TABLE, rl).isPresent(),
                    "loot table manquante : " + rl);
        }
        helper.succeed();
    }

    /** Garde-fou : le registre de blocs du mod ne doit jamais etre vide. */
    @GameTest(template = "empty")
    public static void registryCountsAreSane(GameTestHelper helper) {
        Registry<Block> blocks = helper.getLevel().getServer().registryAccess().registryOrThrow(Registries.BLOCK);
        long acBlocks = blocks.keySet().stream()
                .filter(id -> id.getNamespace().equals(AcademyCraft.MOD_ID)).count();
        assertTrue(helper, acBlocks >= 21, "seulement " + acBlocks + " blocs 'academy' enregistres (>= 21 attendus)");
        helper.succeed();
    }
}
