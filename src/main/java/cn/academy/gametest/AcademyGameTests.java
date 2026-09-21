package cn.academy.gametest;

import cn.academy.AcademyCraft;
import cn.academy.ModBlocks;
import cn.academy.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
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
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, ore);
            assertTrue(helper, configured.containsKey(rl), "configured_feature manquante : " + rl);
            assertTrue(helper, placed.containsKey(rl), "placed_feature manquante : " + rl);
        }
        helper.succeed();
    }

    /**
     * La feature du liquide Phase doit etre declaree ET pointer sur notre fluide.
     * C'est le seul vrai contenu de notre cote : la forme de la poche est generee
     * par le {@code minecraft:lake} vanilla.
     */
    @GameTest(template = "empty")
    public static void phaseLiquidLakeUsesOurFluid(GameTestHelper helper) {
        var access = helper.getLevel().getServer().registryAccess();
        var configured = access.registryOrThrow(Registries.CONFIGURED_FEATURE);
        var placed = access.registryOrThrow(Registries.PLACED_FEATURE);

        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "phase_liquid_lake");
        assertTrue(helper, configured.containsKey(rl), "configured_feature manquante : " + rl);
        assertTrue(helper, placed.containsKey(rl), "placed_feature manquante : " + rl);

        var holder = configured.getHolder(ResourceKey.create(Registries.CONFIGURED_FEATURE, rl)).orElse(null);
        assertTrue(helper, holder != null, "configured_feature academy:phase_liquid_lake introuvable");
        assertTrue(helper, holder.value().config() instanceof LakeFeature.Configuration,
                "academy:phase_liquid_lake devrait utiliser minecraft:lake");

        var lake = (LakeFeature.Configuration) holder.value().config();
        var fluidState = lake.fluid().getState(helper.getLevel().getRandom(), helper.absolutePos(BlockPos.ZERO));
        assertTrue(helper, fluidState.is(ModBlocks.PHASE_LIQUID_BLOCK.get()),
                "la feature ne place pas academy:phase_liquid mais " + fluidState);
        helper.succeed();
    }

    /** Le bloc de liquide Phase doit pouvoir etre pose et relu. */
    @GameTest(template = "empty")
    public static void phaseLiquidBlockCanBePlaced(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        var state = ModBlocks.PHASE_LIQUID_BLOCK.get().defaultBlockState();
        boolean set = helper.getLevel().setBlock(helper.absolutePos(rel), state, 3);
        var readBack = helper.getBlockState(rel);
        assertTrue(helper, set, "setBlock a refuse la pose de academy:phase_liquid");
        assertTrue(helper, readBack.is(ModBlocks.PHASE_LIQUID_BLOCK.get()),
                "le bloc phase_liquid n'a pas survecu a la pose, lu : " + readBack);
        helper.succeed();
    }

    /**
     * Test de bout en bout : on place reellement la feature dans un volume de
     * pierre (structure {@code stone_vault}) et on verifie que des blocs de
     * liquide Phase apparaissent.
     *
     * On appelle la <b>configured</b> feature et non la placed feature : les
     * modifiers de placement ({@code height_range}, {@code in_square},
     * {@code rarity_filter}) deplacent l'origine, la feature ecrirait donc
     * ailleurs que dans la zone observee.
     */
    @GameTest(template = "stone_vault")
    public static void phaseLiquidLakeActuallyPlacesFluid(GameTestHelper helper) {
        var access = helper.getLevel().getServer().registryAccess();
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "phase_liquid_lake");
        var holder = access.registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getHolder(ResourceKey.create(Registries.CONFIGURED_FEATURE, rl)).orElse(null);
        assertTrue(helper, holder != null, "configured feature academy:phase_liquid_lake introuvable");

        // La feature occupe 16x16x8 a partir de (origine - 4). Placee en (2, 8, 2),
        // elle tient entierement dans le volume de pierre 20x12x20.
        BlockPos origin = new BlockPos(2, 8, 2);

        int stoneBefore = 0;
        for (int x = origin.getX(); x < origin.getX() + 16; x++) {
            for (int y = origin.getY() - 4; y < origin.getY() + 4; y++) {
                for (int z = origin.getZ(); z < origin.getZ() + 16; z++) {
                    if (helper.getBlockState(new BlockPos(x, y, z))
                            .is(net.minecraft.world.level.block.Blocks.STONE)) {
                        stoneBefore++;
                    }
                }
            }
        }
        assertTrue(helper, stoneBefore > 0,
                "la structure stone_vault n'a pas ete chargee [pierre = " + stoneBefore + "]");

        boolean placedOk = holder.value().place(
                helper.getLevel(),
                helper.getLevel().getChunkSource().getGenerator(),
                net.minecraft.util.RandomSource.create(20260921L),
                helper.absolutePos(origin));
        assertTrue(helper, placedOk, "la feature a refuse de se placer dans la pierre");

        int fluidBlocks = 0;
        int airAfter = 0;
        java.util.Set<String> autres = new java.util.TreeSet<>();
        for (int x = origin.getX(); x < origin.getX() + 16; x++) {
            for (int y = origin.getY() - 4; y < origin.getY() + 4; y++) {
                for (int z = origin.getZ(); z < origin.getZ() + 16; z++) {
                    var st = helper.getBlockState(new BlockPos(x, y, z));
                    if (st.is(ModBlocks.PHASE_LIQUID_BLOCK.get())) fluidBlocks++;
                    else if (st.isAir()) airAfter++;
                    else if (!st.is(net.minecraft.world.level.block.Blocks.STONE)) autres.add(st.toString());
                }
            }
        }
        assertTrue(helper, fluidBlocks > 0,
                "aucun bloc academy:phase_liquid genere dans la poche"
                        + " [air = " + airAfter + ", autres = " + autres + "]");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void phaseLiquidBiomeModifierRespectsConfig(GameTestHelper helper) {
        var access = helper.getLevel().getServer().registryAccess();
        var biomes = access.registryOrThrow(Registries.BIOME);
        var placed = access.registryOrThrow(Registries.PLACED_FEATURE);

        var plains = biomes.getHolder(ResourceKey.create(Registries.BIOME,
                ResourceLocation.withDefaultNamespace("plains"))).orElse(null);
        assertTrue(helper, plains != null, "biome minecraft:plains introuvable");
        var lake = placed.getHolder(ResourceKey.create(Registries.PLACED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "phase_liquid_lake"))).orElse(null);
        assertTrue(helper, lake != null, "placed feature academy:phase_liquid_lake introuvable");

        boolean present = plains.value().getGenerationSettings().features().stream()
                .anyMatch(list -> list.contains(lake));

        if (cn.academy.Config.generatePhaseLiquid) {
            assertTrue(helper, present,
                    "academy:phase_liquid_lake absent de minecraft:plains alors que "
                            + "generatePhaseLiquid = true (biome modifier non applique)");
        } else {
            assertTrue(helper, !present,
                    "academy:phase_liquid_lake present dans minecraft:plains alors que "
                            + "generatePhaseLiquid = false");
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
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "blocks/" + ore);
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

    // ------------------------------------------------------------------
    // Biome modifier : les minerais doivent etre effectivement injectes dans
    // les biomes, et l'option de config doit etre respectee.
    // ------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void oreBiomeModifierRespectsConfig(GameTestHelper helper) {
        var access = helper.getLevel().getServer().registryAccess();
        var biomes = access.registryOrThrow(Registries.BIOME);
        var placed = access.registryOrThrow(Registries.PLACED_FEATURE);

        var plainsKey = ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("plains"));
        var oreKey = ResourceKey.create(Registries.PLACED_FEATURE,
                ResourceLocation.tryParse(AcademyCraft.MOD_ID + ":crystal_ore"));

        var plains = biomes.getHolder(plainsKey).orElse(null);
        assertTrue(helper, plains != null, "biome minecraft:plains introuvable");
        var ore = placed.getHolder(oreKey).orElse(null);
        assertTrue(helper, ore != null, "placed feature academy:crystal_ore introuvable");

        boolean present = plains.value().getGenerationSettings().features().stream()
                .anyMatch(list -> list.contains(ore));

        if (cn.academy.Config.generateOres) {
            assertTrue(helper, present,
                    "academy:crystal_ore absent des generation settings de minecraft:plains "
                            + "alors que generateOres = true (biome modifier non applique)");
        } else {
            assertTrue(helper, !present,
                    "academy:crystal_ore present dans minecraft:plains alors que generateOres = false");
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // Config : les valeurs doivent etre chargees (pas les 0 par defaut).
    // ------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void configValuesAreLoaded(GameTestHelper helper) {
        assertTrue(helper, cn.academy.Config.controlPointMax >= 1.0d,
                "controlPointMax non charge depuis la config : " + cn.academy.Config.controlPointMax);
        assertTrue(helper, cn.academy.Config.damageScale >= 0.0d,
                "damageScale non charge depuis la config : " + cn.academy.Config.damageScale);
        assertTrue(helper, cn.academy.Config.controlPointSyncInterval >= 1,
                "controlPointSyncInterval non charge depuis la config : "
                        + cn.academy.Config.controlPointSyncInterval);
        helper.succeed();
    }
}
