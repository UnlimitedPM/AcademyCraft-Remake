package cn.academy.gametest;

import cn.academy.AcademyCraft;
import cn.academy.ImagFusorBlockEntity;
import cn.academy.MetalFormerBlockEntity;
import cn.academy.ModBlocks;
import cn.academy.ModFluids;
import cn.academy.ModItems;
import cn.academy.WindgenBaseBlockEntity;
import cn.academy.WindgenMainBlockEntity;
import cn.academy.energy.ImagNetworkData;
import cn.academy.energy.MatrixBlockEntity;
import cn.academy.energy.NodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
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
        // Un echantillon couvrant chaque famille de recette.
        for (String id : new String[]{"academy:constraint_plate", "academy:machine_frame", "academy:solar_gen",
                "academy:node_basic", "academy:energy_unit",
                // Recipes alternatives et objets d'application, portes depuis
                // default.recipe de la 1.12.2 et longtemps manquants ici.
                "academy:calc_chip2", "academy:energy_unit2", "academy:energy_unit3",
                "academy:dev_normal2", "academy:app_skill_tree",
                "academy:app_media_player", "academy:app_freq_transmitter"}) {
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
        long smelting = helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .filter(r -> r.getType() == RecipeType.SMELTING)
                .filter(r -> r.getId().getNamespace().equals(AcademyCraft.MOD_ID))
                .count();
        // 48 recettes de craft et 3 de fonte (default.recipe de la 1.12.2).
        assertTrue(helper, crafting >= 48,
                "seulement " + crafting + " recipes de crafting 'academy' chargees (>= 48 attendues)");
        assertTrue(helper, smelting >= 3,
                "seulement " + smelting + " recipes de fonte 'academy' chargees (>= 3 attendues)");
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

    private static void assertFalse(GameTestHelper helper, boolean condition, String message) {
        helper.assertFalse(condition, message);
    }

    /** Comparaison numerique avec tolerance, pour les energies et les distances. */
    private static void assertClose(GameTestHelper helper, double expected, double actual, String what) {
        helper.assertTrue(Math.abs(expected - actual) < 1.0e-6,
                what + " : attendu " + expected + ", trouve " + actual);
    }

    /** Comparaison exacte, pour les enums et les proprietes de bloc. */
    private static void assertValue(GameTestHelper helper, Object expected, Object actual, String what) {
        helper.assertTrue(java.util.Objects.equals(expected, actual),
                what + " : attendu " + expected + ", trouve " + actual);
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
    // Reseau energetique : les noeuds sans fil
    // ------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void wirelessNodesAreBlockEntities(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.NODE_BASIC.get());

        var be = helper.getBlockEntity(rel);
        assertTrue(helper, be instanceof cn.academy.energy.NodeBlockEntity,
                "node_basic doit porter un NodeBlockEntity, trouve : " + be);

        var node = (cn.academy.energy.NodeBlockEntity) be;
        assertValue(helper, cn.academy.energy.NodeType.BASIC, node.getNodeType(), "qualite du noeud");
        assertClose(helper, 15000.0d, node.getMaxEnergy(), "capacite du noeud basic");
        assertClose(helper, 150.0d, node.getBandwidth(), "bande passante du noeud basic");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wirelessNodeTypeComesFromTheBlock(GameTestHelper helper) {
        BlockPos standard = new BlockPos(1, 1, 1);
        BlockPos advanced = new BlockPos(2, 1, 1);
        helper.setBlock(standard, ModBlocks.NODE_STANDARD.get());
        helper.setBlock(advanced, ModBlocks.NODE_ADVANCED.get());

        assertValue(helper, cn.academy.energy.NodeType.STANDARD,
                ((cn.academy.energy.NodeBlockEntity) helper.getBlockEntity(standard)).getNodeType(),
                "node_standard");
        assertValue(helper, cn.academy.energy.NodeType.ADVANCED,
                ((cn.academy.energy.NodeBlockEntity) helper.getBlockEntity(advanced)).getNodeType(),
                "node_advanced");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wirelessNodeStoresAndClampsEnergy(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.NODE_BASIC.get());
        var node = (cn.academy.energy.NodeBlockEntity) helper.getBlockEntity(rel);

        node.setEnergy(5000.0d);
        assertClose(helper, 5000.0d, node.getEnergy(), "energie stockee");

        // Au-dela de la capacite, la valeur est ramenee au maximum.
        node.setEnergy(1.0e9d);
        assertClose(helper, 15000.0d, node.getEnergy(), "energie plafonnee a la capacite");

        // Sous zero, on retombe a zero.
        node.setEnergy(-42.0d);
        assertClose(helper, 0.0d, node.getEnergy(), "energie negative ramenee a zero");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wirelessNodeEnergySurvivesReload(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.NODE_ADVANCED.get());
        var node = (cn.academy.energy.NodeBlockEntity) helper.getBlockEntity(rel);

        node.setEnergy(12345.0d);
        node.setConnected(true);

        var reloaded = new cn.academy.energy.NodeBlockEntity(helper.absolutePos(rel),
                helper.getBlockState(rel));
        reloaded.load(node.saveWithoutMetadata());

        assertClose(helper, 12345.0d, reloaded.getEnergy(), "energie apres rechargement");
        assertTrue(helper, reloaded.isConnected(), "etat de connexion apres rechargement");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wirelessNodeUpdatesItsBlockState(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.NODE_BASIC.get());
        var node = (cn.academy.energy.NodeBlockEntity) helper.getBlockEntity(rel);

        assertValue(helper, 0,
                helper.getBlockState(rel).getValue(cn.academy.energy.NodeBlock.ENERGY_LEVEL),
                "palier initial");

        // La synchronisation de l'etat visuel n'a lieu que tous les 10 ticks,
        // comme dans l'original : on fait donc avancer le tick manuellement.
        node.setEnergy(15000.0d);
        for (int i = 0; i < 10; i++) {
            cn.academy.energy.NodeBlockEntity.serverTick(
                    helper.getLevel(), helper.absolutePos(rel), helper.getBlockState(rel), node);
        }
        assertValue(helper, 4,
                helper.getBlockState(rel).getValue(cn.academy.energy.NodeBlock.ENERGY_LEVEL),
                "palier apres remplissage complet");

        node.setEnergy(0.0d);
        for (int i = 0; i < 10; i++) {
            cn.academy.energy.NodeBlockEntity.serverTick(
                    helper.getLevel(), helper.absolutePos(rel), helper.getBlockState(rel), node);
        }
        assertValue(helper, 0,
                helper.getBlockState(rel).getValue(cn.academy.energy.NodeBlock.ENERGY_LEVEL),
                "palier apres vidage");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wirelessNodeChargesAndDischargesEnergyUnits(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.NODE_BASIC.get());
        var node = (cn.academy.energy.NodeBlockEntity) helper.getBlockEntity(rel);

        // Slot 0 : une unite d'energie pleine charge le noeud.
        net.minecraft.world.item.ItemStack full = new net.minecraft.world.item.ItemStack(ModItems.ENERGY_UNIT.get());
        cn.academy.ModItems.EnergyUnit.charge(full, cn.academy.ModItems.EnergyUnit.MAX_ENERGY);
        node.getInventory().setStackInSlot(cn.academy.energy.NodeBlockEntity.SLOT_CHARGE_IN, full);

        cn.academy.energy.NodeBlockEntity.serverTick(
                helper.getLevel(), helper.absolutePos(rel), helper.getBlockState(rel), node);

        assertTrue(helper, node.getEnergy() > 0.0d,
                "le noeud aurait du se charger depuis l'unite d'energie, energie = " + node.getEnergy());
        assertTrue(helper, cn.academy.ModItems.EnergyUnit.getEnergy(full) < cn.academy.ModItems.EnergyUnit.MAX_ENERGY,
                "l'unite d'energie aurait du se decharger");

        // Slot 1 : le noeud charge une unite vide. On vide d'abord le slot 0,
        // sinon l'unite de charge continue d'alimenter le noeud pendant qu'il se
        // decharge, et les deux mouvements s'annulent.
        node.getInventory().setStackInSlot(cn.academy.energy.NodeBlockEntity.SLOT_CHARGE_IN,
                net.minecraft.world.item.ItemStack.EMPTY);
        node.setEnergy(10000.0d);
        net.minecraft.world.item.ItemStack empty = new net.minecraft.world.item.ItemStack(ModItems.ENERGY_UNIT.get());
        node.getInventory().setStackInSlot(cn.academy.energy.NodeBlockEntity.SLOT_CHARGE_OUT, empty);

        cn.academy.energy.NodeBlockEntity.serverTick(
                helper.getLevel(), helper.absolutePos(rel), helper.getBlockState(rel), node);

        assertTrue(helper, cn.academy.ModItems.EnergyUnit.getEnergy(empty) > 0.0f,
                "l'unite vide aurait du se charger depuis le noeud");
        assertTrue(helper, node.getEnergy() < 10000.0d,
                "le noeud aurait du se decharger, energie = " + node.getEnergy());
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // Reseau energetique : le Matrix sans fil
    // ------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void matrixHasABlockEntity(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.MATRIX.get());

        var be = helper.getBlockEntity(rel);
        assertTrue(helper, be instanceof MatrixBlockEntity,
                "matrix doit porter un MatrixBlockEntity, trouve : " + be);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void matrixNeedsACoreAndThreePlates(GameTestHelper helper) {
        MatrixBlockEntity matrix = placeMatrix(helper, new BlockPos(1, 1, 1));

        assertTrue(helper, !matrix.isWorking(), "un Matrix vide ne doit pas fonctionner");
        assertValue(helper, 0, matrix.getCapacity(), "capacite sans coeur");
        assertClose(helper, 0.0d, matrix.getBandwidth(), "bande passante sans coeur");
        assertClose(helper, 0.0d, matrix.getRange(), "portee sans coeur");

        // Les trois plaques sans coeur ne suffisent pas.
        for (int slot = 0; slot < MatrixBlockEntity.SLOT_PLATE_COUNT; slot++) {
            matrix.getInventory().setStackInSlot(slot, new ItemStack(ModItems.CONSTRAINT_PLATE.get()));
        }
        assertTrue(helper, !matrix.isWorking(), "trois plaques sans coeur ne suffisent pas");

        matrix.getInventory().setStackInSlot(MatrixBlockEntity.SLOT_CORE, new ItemStack(ModItems.MAT_CORE_1.get()));
        assertTrue(helper, matrix.isWorking(), "coeur plus trois plaques = Matrix actif");

        // Une plaque en moins et tout s'arrete.
        matrix.getInventory().setStackInSlot(1, ItemStack.EMPTY);
        assertTrue(helper, !matrix.isWorking(), "deux plaques ne suffisent pas");
        helper.succeed();
    }

    /**
     * Les chiffres viennent de TileMatrix : capacite 8 x niveau, bande passante
     * 60 x niveau au carre, portee 24 x racine du niveau.
     */
    @GameTest(template = "empty")
    public static void matrixStatsFollowTheCoreLevel(GameTestHelper helper) {
        MatrixBlockEntity matrix = placeMatrix(helper, new BlockPos(1, 1, 1));
        for (int slot = 0; slot < MatrixBlockEntity.SLOT_PLATE_COUNT; slot++) {
            matrix.getInventory().setStackInSlot(slot, new ItemStack(ModItems.CONSTRAINT_PLATE.get()));
        }

        matrix.getInventory().setStackInSlot(MatrixBlockEntity.SLOT_CORE, new ItemStack(ModItems.MAT_CORE_0.get()));
        assertValue(helper, 1, matrix.getCoreLevel(), "niveau de mat_core_0");
        assertValue(helper, 8, matrix.getCapacity(), "capacite niveau 1");
        assertClose(helper, 60.0d, matrix.getBandwidth(), "bande passante niveau 1");
        assertClose(helper, 24.0d, matrix.getRange(), "portee niveau 1");

        matrix.getInventory().setStackInSlot(MatrixBlockEntity.SLOT_CORE, new ItemStack(ModItems.MAT_CORE_1.get()));
        assertValue(helper, 2, matrix.getCoreLevel(), "niveau de mat_core_1");
        assertValue(helper, 16, matrix.getCapacity(), "capacite niveau 2");
        assertClose(helper, 240.0d, matrix.getBandwidth(), "bande passante niveau 2");
        assertClose(helper, 24.0d * Math.sqrt(2.0d), matrix.getRange(), "portee niveau 2");

        matrix.getInventory().setStackInSlot(MatrixBlockEntity.SLOT_CORE, new ItemStack(ModItems.MAT_CORE_2.get()));
        assertValue(helper, 3, matrix.getCoreLevel(), "niveau de mat_core_2");
        assertValue(helper, 24, matrix.getCapacity(), "capacite niveau 3");
        assertClose(helper, 540.0d, matrix.getBandwidth(), "bande passante niveau 3");
        assertClose(helper, MatrixBlockEntity.MAX_RANGE, matrix.getRange(), "portee niveau 3");
        helper.succeed();
    }

    /** La portee depend du coeur : un noeud trop loin ne peut pas etre raccorde. */
    @GameTest(template = "empty")
    public static void matrixRangeLimitsLinking(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        MatrixBlockEntity matrix = placeMatrix(helper, rel);
        for (int slot = 0; slot < MatrixBlockEntity.SLOT_PLATE_COUNT; slot++) {
            matrix.getInventory().setStackInSlot(slot, new ItemStack(ModItems.CONSTRAINT_PLATE.get()));
        }
        matrix.getInventory().setStackInSlot(MatrixBlockEntity.SLOT_CORE, new ItemStack(ModItems.MAT_CORE_0.get()));

        BlockPos anchor = helper.absolutePos(rel);
        // Portee 24 au niveau 1 : a 10 blocs on est dedans, a 30 on est dehors.
        assertTrue(helper, matrix.canReach(anchor.offset(10, 0, 0)), "un noeud a 10 blocs doit etre a portee");
        assertTrue(helper, matrix.canReach(anchor.offset(0, 10, 10)), "la portee tient compte de la hauteur");
        assertTrue(helper, !matrix.canReach(anchor.offset(30, 0, 0)), "un noeud a 30 blocs doit etre hors portee");

        // Sans coeur la portee est nulle : plus rien n'est a portee.
        matrix.getInventory().setStackInSlot(MatrixBlockEntity.SLOT_CORE, ItemStack.EMPTY);
        assertTrue(helper, !matrix.canReach(anchor.offset(1, 0, 0)), "sans coeur rien n'est a portee");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void matrixAcceptsOnlyPlatesAndCores(GameTestHelper helper) {
        MatrixBlockEntity matrix = placeMatrix(helper, new BlockPos(1, 1, 1));
        var inventory = matrix.getInventory();
        ItemStack stone = new ItemStack(net.minecraft.world.item.Items.STONE);

        assertTrue(helper, inventory.isItemValid(0, new ItemStack(ModItems.CONSTRAINT_PLATE.get())),
                "une plaque de contrainte doit etre acceptee en emplacement 0");
        assertTrue(helper, inventory.isItemValid(2, new ItemStack(ModItems.CONSTRAINT_PLATE.get())),
                "une plaque de contrainte doit etre acceptee en emplacement 2");
        assertTrue(helper, !inventory.isItemValid(0, stone),
                "la pierre ne doit pas entrer dans un emplacement de plaque");

        assertTrue(helper, inventory.isItemValid(MatrixBlockEntity.SLOT_CORE, new ItemStack(ModItems.MAT_CORE_2.get())),
                "un coeur de matrice doit etre accepte");
        assertTrue(helper, !inventory.isItemValid(MatrixBlockEntity.SLOT_CORE, stone),
                "la pierre ne doit pas entrer dans l'emplacement du coeur");

        // Un emplacement de plaque n'accepte pas un coeur : les deux roles sont
        // distincts, comme dans l'original.
        assertTrue(helper, !inventory.isItemValid(1, new ItemStack(ModItems.MAT_CORE_0.get())),
                "un coeur ne doit pas entrer dans un emplacement de plaque");
        helper.succeed();
    }

    /** Un Matrix sans coeur ne peut atteindre personne : ses liens doivent tomber. */
    @GameTest(template = "empty")
    public static void matrixDropsLinksItCannotHonour(GameTestHelper helper) {
        BlockPos matrixRel = new BlockPos(0, 1, 0);
        BlockPos nodeRel = new BlockPos(2, 1, 2);
        MatrixBlockEntity matrix = placeMatrix(helper, matrixRel);
        helper.setBlock(nodeRel, ModBlocks.NODE_BASIC.get());

        ServerLevel level = helper.getLevel();
        ImagNetworkData data = ImagNetworkData.get(level);
        BlockPos matrixAbs = helper.absolutePos(matrixRel);
        BlockPos nodeAbs = helper.absolutePos(nodeRel);
        data.link(matrixAbs, nodeAbs);
        assertTrue(helper, data.isLinked(nodeAbs), "le lien de depart doit exister");

        MatrixBlockEntity.serverTick(level, matrixAbs, helper.getBlockState(matrixRel), matrix);

        assertTrue(helper, !data.isLinked(nodeAbs),
                "un Matrix inactif doit lacher ses noeuds au lieu de les garder pour rien");
        helper.succeed();
    }

    /**
     * Le vrai test du reseau : deux noeuds a des niveaux tres differents doivent
     * se rapprocher. C'est le seul endroit ou le Matrix, le registre des liens et
     * l'algorithme d'equilibrage travaillent ensemble.
     */
    @GameTest(template = "empty")
    public static void matrixBalancesItsLinkedNodes(GameTestHelper helper) {
        BlockPos matrixRel = new BlockPos(0, 1, 0);
        BlockPos fullRel = new BlockPos(1, 1, 0);
        BlockPos emptyRel = new BlockPos(2, 1, 0);

        MatrixBlockEntity matrix = placeMatrix(helper, matrixRel);
        for (int slot = 0; slot < MatrixBlockEntity.SLOT_PLATE_COUNT; slot++) {
            matrix.getInventory().setStackInSlot(slot, new ItemStack(ModItems.CONSTRAINT_PLATE.get()));
        }
        matrix.getInventory().setStackInSlot(MatrixBlockEntity.SLOT_CORE, new ItemStack(ModItems.MAT_CORE_2.get()));

        helper.setBlock(fullRel, ModBlocks.NODE_BASIC.get());
        helper.setBlock(emptyRel, ModBlocks.NODE_BASIC.get());
        NodeBlockEntity full = (NodeBlockEntity) helper.getBlockEntity(fullRel);
        NodeBlockEntity empty = (NodeBlockEntity) helper.getBlockEntity(emptyRel);

        full.setEnergy(full.getMaxEnergy());
        empty.setEnergy(0.0d);

        ServerLevel level = helper.getLevel();
        ImagNetworkData data = ImagNetworkData.get(level);
        BlockPos matrixAbs = helper.absolutePos(matrixRel);
        data.link(matrixAbs, helper.absolutePos(fullRel));
        data.link(matrixAbs, helper.absolutePos(emptyRel));

        // On mesure sur l'ensemble du reseau et non sur nos deux noeuds : les
        // tests partagent le meme monde, un noeud voisin peut venir s'ajouter.
        java.util.List<NodeBlockEntity> network = new java.util.ArrayList<>();
        for (BlockPos nodePos : data.nodesOf(matrixAbs)) {
            if (level.getBlockEntity(nodePos) instanceof NodeBlockEntity member) network.add(member);
        }
        double totalBefore = 0.0d;
        for (NodeBlockEntity member : network) totalBefore += member.getEnergy();
        double bufferBefore = matrix.getBuffer();

        for (int i = 0; i < 300; i++) {
            MatrixBlockEntity.serverTick(level, matrixAbs, helper.getBlockState(matrixRel), matrix);
        }

        assertTrue(helper, empty.getEnergy() > 0.0d,
                "le noeud vide aurait du recevoir de l'energie, energie = " + empty.getEnergy());
        assertTrue(helper, full.getEnergy() < full.getMaxEnergy(),
                "le noeud plein aurait du ceder de l'energie, energie = " + full.getEnergy());

        double gap = Math.abs(full.getEnergy() - empty.getEnergy());
        assertTrue(helper, gap < 500.0d,
                "les deux noeuds auraient du converger, ecart restant = " + gap);

        // L'invariant du reseau : ce que les noeuds ont gagne ou perdu se
        // retrouve dans le tampon, ni plus ni moins.
        double totalAfter = 0.0d;
        for (NodeBlockEntity member : network) totalAfter += member.getEnergy();
        assertClose(helper, totalAfter - totalBefore, matrix.getBuffer() - bufferBefore,
                "energie deplacee vers le tampon");
        helper.succeed();
    }

    /** Un noeud pose a cote d'un Matrix actif doit s'y raccorder tout seul. */
    @GameTest(template = "empty")
    public static void nodeFindsANearbyMatrix(GameTestHelper helper) {
        BlockPos matrixRel = new BlockPos(1, 1, 0);
        // Le noeud est colle au Matrix : c'est forcement le plus proche, donc
        // celui qu'il doit choisir.
        BlockPos nodeRel = new BlockPos(2, 1, 0);

        MatrixBlockEntity matrix = placeMatrix(helper, matrixRel);
        for (int slot = 0; slot < MatrixBlockEntity.SLOT_PLATE_COUNT; slot++) {
            matrix.getInventory().setStackInSlot(slot, new ItemStack(ModItems.CONSTRAINT_PLATE.get()));
        }
        matrix.getInventory().setStackInSlot(MatrixBlockEntity.SLOT_CORE, new ItemStack(ModItems.MAT_CORE_2.get()));

        helper.setBlock(nodeRel, ModBlocks.NODE_BASIC.get());
        NodeBlockEntity node = (NodeBlockEntity) helper.getBlockEntity(nodeRel);

        ServerLevel level = helper.getLevel();
        BlockPos nodeAbs = helper.absolutePos(nodeRel);

        // La recherche n'a lieu que tous les 100 ticks : on avance le tick a la main.
        for (int i = 0; i < 100; i++) {
            NodeBlockEntity.serverTick(level, nodeAbs, helper.getBlockState(nodeRel), node);
        }

        assertValue(helper, helper.absolutePos(matrixRel), ImagNetworkData.get(level).matrixOf(nodeAbs),
                "Matrix trouve par le noeud");
        assertTrue(helper, node.isConnected(), "le noeud doit se savoir raccorde");
        helper.succeed();
    }

    /**
     * Un Matrix sans coeur ne doit raccrocher personne.
     *
     * L'assertion porte sur <b>notre</b> Matrix et non sur l'absence de
     * raccordement du noeud : les tests partagent le meme monde, et un Matrix
     * bien equipe d'une structure voisine serait a portee de recherche. Le
     * voisinage est donc ignore volontairement, on ne juge que notre bloc.
     */
    @GameTest(template = "empty")
    public static void nodeIgnoresAnInactiveMatrix(GameTestHelper helper) {
        BlockPos matrixRel = new BlockPos(1, 1, 0);
        BlockPos nodeRel = new BlockPos(2, 1, 0);

        placeMatrix(helper, matrixRel);
        helper.setBlock(nodeRel, ModBlocks.NODE_BASIC.get());
        NodeBlockEntity node = (NodeBlockEntity) helper.getBlockEntity(nodeRel);

        ServerLevel level = helper.getLevel();
        BlockPos nodeAbs = helper.absolutePos(nodeRel);
        for (int i = 0; i < 200; i++) {
            NodeBlockEntity.serverTick(level, nodeAbs, helper.getBlockState(nodeRel), node);
        }

        assertValue(helper, 0, ImagNetworkData.get(level).nodeCount(helper.absolutePos(matrixRel)),
                "un Matrix sans coeur ne doit attirer aucun noeud");
        // On ne verifie pas que le noeud se croit deconnecte : il a pu se
        // raccorder a un Matrix d'une structure voisine, ce qui est legitime.
        // Ce qui compte ici est que le notre n'ait rien attire.
        helper.succeed();
    }

    /**
     * Le Matrix est un multi-bloc 2x2x2 : le clic du joueur peut tomber sur
     * n'importe laquelle des huit parties, mais le block entity et l'ecran sont
     * sur l'ancrage. On verifie donc que chacune des parties sait retrouver
     * l'ancrage, pour les huit orientations possibles.
     */
    // ------------------------------------------------------------------
    // Reseau energetique : le formeur de metal, premier consommateur
    // ------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void metalFormerIsAnEnergyReceiver(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.METAL_FORMER.get());
        var former = (MetalFormerBlockEntity) helper.getBlockEntity(rel);

        // Valeurs de TileMetalFormer : tampon 3000, bande passante LATENCY_MK1 = 50.
        assertValue(helper, 3000, former.getMaxEnergyStored(), "tampon du formeur");
        assertClose(helper, 50.0d, former.getBandwidth(), "bande passante du formeur");
        assertClose(helper, 3000.0d, former.getRequiredEnergy(), "besoin d'un formeur vide");

        // injectEnergy rend ce qu'elle n'a PAS pris.
        assertClose(helper, 0.0d, former.injectEnergy(1000.0d), "tout doit etre accepte");
        assertClose(helper, 1000.0d, former.getEnergy(), "energie stockee");
        assertClose(helper, 2000.0d, former.getRequiredEnergy(), "besoin restant");

        // Le tampon ne deborde pas : l'excedent est rendu a l'appelant.
        assertClose(helper, 1000.0d, former.injectEnergy(3000.0d), "excedent rendu");
        assertClose(helper, 3000.0d, former.getEnergy(), "tampon plein");
        assertClose(helper, 0.0d, former.getRequiredEnergy(), "plus rien a remplir");
        assertClose(helper, 500.0d, former.injectEnergy(500.0d), "un tampon plein refuse tout");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void metalFormerFindsANearbyNode(GameTestHelper helper) {
        BlockPos nodeRel = new BlockPos(1, 1, 0);
        BlockPos formerRel = new BlockPos(1, 1, 2);

        helper.setBlock(nodeRel, ModBlocks.NODE_BASIC.get());
        helper.setBlock(formerRel, ModBlocks.METAL_FORMER.get());
        var former = (MetalFormerBlockEntity) helper.getBlockEntity(formerRel);

        ServerLevel level = helper.getLevel();
        BlockPos formerAbs = helper.absolutePos(formerRel);

        for (int i = 0; i < 100; i++) {
            MetalFormerBlockEntity.tick(level, formerAbs, helper.getBlockState(formerRel), former);
        }

        assertValue(helper, helper.absolutePos(nodeRel), ImagNetworkData.get(level).nodeOf(formerAbs),
                "noeud trouve par le formeur");
        assertTrue(helper, former.isLinked(), "le formeur doit se savoir raccorde");
        helper.succeed();
    }

    /**
     * Le vrai test de bout en bout du reseau : l'energie poussee par un noeud doit
     * faire tourner la machine, qui doit transformer son entree.
     */
    @GameTest(template = "empty")
    public static void metalFormerRunsOnNetworkEnergy(GameTestHelper helper) {
        BlockPos nodeRel = new BlockPos(1, 1, 0);
        BlockPos formerRel = new BlockPos(1, 1, 2);

        helper.setBlock(nodeRel, ModBlocks.NODE_BASIC.get());
        helper.setBlock(formerRel, ModBlocks.METAL_FORMER.get());
        var node = (NodeBlockEntity) helper.getBlockEntity(nodeRel);
        var former = (MetalFormerBlockEntity) helper.getBlockEntity(formerRel);

        ServerLevel level = helper.getLevel();
        BlockPos nodeAbs = helper.absolutePos(nodeRel);
        BlockPos formerAbs = helper.absolutePos(formerRel);

        for (int i = 0; i < 100; i++) {
            MetalFormerBlockEntity.tick(level, formerAbs, helper.getBlockState(formerRel), former);
        }
        assertValue(helper, nodeAbs, ImagNetworkData.get(level).nodeOf(formerAbs),
                "le formeur doit avoir trouve le noeud avant de travailler");

        // Recette INCISE : une plaque de fer renforcee donne six aiguilles.
        former.getInventory().setStackInSlot(MetalFormerBlockEntity.SLOT_IN,
                new ItemStack(ModItems.REINFORCED_IRON_PLATE.get()));
        former.cycleMode(1); // PLATE -> INCISE
        assertValue(helper, cn.academy.crafting.MetalFormerMode.INCISE, former.getMode(), "mode choisi");

        // On donne au noeud de quoi alimenter la machine : 60 ticks a 13,3, soit
        // 798 unites, plus la marge pour remplir le tampon du formeur.
        node.setEnergy(3000.0d);

        // Le formeur cherche sa recette tous les 5 ticks, puis travaille 60 ticks.
        for (int i = 0; i < 120; i++) {
            MetalFormerBlockEntity.tick(level, formerAbs, helper.getBlockState(formerRel), former);
            NodeBlockEntity.serverTick(level, nodeAbs, helper.getBlockState(nodeRel), node);
        }

        ItemStack output = former.getInventory().getStackInSlot(MetalFormerBlockEntity.SLOT_OUT);
        assertValue(helper, ModItems.NEEDLE.get(), output.getItem(), "objet produit");
        assertValue(helper, 6, output.getCount(), "quantite produite");
        assertTrue(helper, former.getInventory().getStackInSlot(MetalFormerBlockEntity.SLOT_IN).isEmpty(),
                "l'entree doit avoir ete consommee");
        assertTrue(helper, node.getEnergy() < 3000.0d,
                "le noeud aurait du fournir de l'energie, il en a " + node.getEnergy());
        helper.succeed();
    }

    /** Sans energie, la machine ne doit ni travailler ni rien produire. */
    @GameTest(template = "empty")
    public static void metalFormerDoesNothingWithoutEnergy(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.METAL_FORMER.get());
        var former = (MetalFormerBlockEntity) helper.getBlockEntity(rel);

        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(rel);

        former.getInventory().setStackInSlot(MetalFormerBlockEntity.SLOT_IN,
                new ItemStack(ModItems.REINFORCED_IRON_PLATE.get()));
        former.cycleMode(1);

        for (int i = 0; i < 120; i++) {
            MetalFormerBlockEntity.tick(level, abs, helper.getBlockState(rel), former);
        }

        assertFalse(helper, former.isWorking(), "sans energie la machine ne doit pas travailler");
        assertTrue(helper, former.getInventory().getStackInSlot(MetalFormerBlockEntity.SLOT_OUT).isEmpty(),
                "rien ne doit avoir ete produit");
        assertValue(helper, 1, former.getInventory().getStackInSlot(MetalFormerBlockEntity.SLOT_IN).getCount(),
                "l'entree doit etre intacte");
        assertClose(helper, 0.0d, former.getWorkProgress(), "aucun avancement");
        helper.succeed();
    }

    /** Le mode decide de la recette : la meme entree ne donne pas le meme resultat. */
    @GameTest(template = "empty")
    public static void metalFormerFollowsItsMode(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.METAL_FORMER.get());
        var former = (MetalFormerBlockEntity) helper.getBlockEntity(rel);

        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(rel);

        // Recette PLATE : deux plaques de fer renforcees donnent trois pieces.
        former.getInventory().setStackInSlot(MetalFormerBlockEntity.SLOT_IN,
                new ItemStack(ModItems.REINFORCED_IRON_PLATE.get(), 2));
        former.setEnergy(3000.0d);

        for (int i = 0; i < 120; i++) {
            MetalFormerBlockEntity.tick(level, abs, helper.getBlockState(rel), former);
        }

        ItemStack output = former.getInventory().getStackInSlot(MetalFormerBlockEntity.SLOT_OUT);
        assertValue(helper, ModItems.COIN.get(), output.getItem(), "objet produit en mode PLATE");
        assertValue(helper, 3, output.getCount(), "quantite produite en mode PLATE");

        // En mode PLATE avec une seule plaque, la recette ne s'applique pas.
        former.getInventory().setStackInSlot(MetalFormerBlockEntity.SLOT_OUT, ItemStack.EMPTY);
        former.getInventory().setStackInSlot(MetalFormerBlockEntity.SLOT_IN,
                new ItemStack(ModItems.REINFORCED_IRON_PLATE.get(), 1));
        former.setEnergy(3000.0d);

        for (int i = 0; i < 120; i++) {
            MetalFormerBlockEntity.tick(level, abs, helper.getBlockState(rel), former);
        }

        assertTrue(helper, former.getInventory().getStackInSlot(MetalFormerBlockEntity.SLOT_OUT).isEmpty(),
                "une seule plaque ne suffit pas pour la recette PLATE");
        helper.succeed();
    }

    /** La sortie est reservee a la machine : on ne doit pas pouvoir y poser d'objet. */
    @GameTest(template = "empty")
    public static void metalFormerProtectsItsOutputSlot(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.METAL_FORMER.get());
        var former = (MetalFormerBlockEntity) helper.getBlockEntity(rel);

        assertFalse(helper, former.getInventory().isItemValid(MetalFormerBlockEntity.SLOT_OUT,
                new ItemStack(ModItems.COIN.get())), "la sortie ne doit rien accepter");
        assertTrue(helper, former.getInventory().isItemValid(MetalFormerBlockEntity.SLOT_IN,
                new ItemStack(ModItems.REINFORCED_IRON_PLATE.get())), "l'entree doit tout accepter");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // Reseau energetique : le fusor d'Imag, second consommateur
    // ------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void imagFusorIsAnEnergyReceiver(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.IMAG_FUSOR.get());
        var fusor = (ImagFusorBlockEntity) helper.getBlockEntity(rel);

        // Valeurs de TileImagFusor : tampon 2000, bande passante LATENCY_MK1 = 50,
        // cuve 8000, 1000 mB par unite de phase.
        assertValue(helper, 2000, fusor.getMaxEnergyStored(), "tampon du fusor");
        assertClose(helper, 50.0d, fusor.getBandwidth(), "bande passante du fusor");
        assertValue(helper, 8000, fusor.getTankSize(), "capacite de la cuve");
        assertValue(helper, 1000, ImagFusorBlockEntity.PER_UNIT, "phase par unite");
        assertValue(helper, 120, ImagFusorBlockEntity.WORK_TICKS, "duree d'une fusion");
        assertClose(helper, 12.0d, ImagFusorBlockEntity.CONSUME_PER_TICK, "cout par tick");

        // injectEnergy rend ce qu'elle n'a PAS pris, d'ou le zero.
        assertClose(helper, 0.0d, fusor.injectEnergy(1000.0d), "tout doit etre accepte");
        assertClose(helper, 1000.0d, fusor.getEnergy(), "energie stockee");
        assertClose(helper, 500.0d, fusor.injectEnergy(1500.0d), "excedent rendu");
        assertClose(helper, 2000.0d, fusor.getEnergy(), "tampon plein");
        helper.succeed();
    }

    /** Les unites de phase fondent dans la cuve et ressortent vides. */
    @GameTest(template = "empty")
    public static void imagFusorMeltsPhaseUnits(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.IMAG_FUSOR.get());
        var fusor = (ImagFusorBlockEntity) helper.getBlockEntity(rel);

        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(rel);

        fusor.getInventory().setStackInSlot(ImagFusorBlockEntity.SLOT_IMAG_INPUT,
                new ItemStack(ModItems.MATTER_UNIT_PHASE.get(), 3));

        for (int i = 0; i < 3; i++) {
            ImagFusorBlockEntity.tick(level, abs, helper.getBlockState(rel), fusor);
        }

        assertValue(helper, 3000, fusor.getLiquidAmount(), "phase obtenue en fondant trois unites");
        assertTrue(helper, fusor.getInventory().getStackInSlot(ImagFusorBlockEntity.SLOT_IMAG_INPUT).isEmpty(),
                "les unites pleines doivent avoir ete consommees");

        ItemStack empties = fusor.getInventory().getStackInSlot(ImagFusorBlockEntity.SLOT_IMAG_OUTPUT);
        assertValue(helper, ModItems.MATTER_UNIT.get(), empties.getItem(), "unite vide rendue");
        assertValue(helper, 3, empties.getCount(), "nombre d'unites vides rendues");
        helper.succeed();
    }

    /** La jauge pleine, une unite de plus ne doit pas etre perdue. */
    @GameTest(template = "empty")
    public static void imagFusorDoesNotMeltIntoAFullTank(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.IMAG_FUSOR.get());
        var fusor = (ImagFusorBlockEntity) helper.getBlockEntity(rel);

        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(rel);

        fusor.setLiquidAmount(8000);
        fusor.getInventory().setStackInSlot(ImagFusorBlockEntity.SLOT_IMAG_INPUT,
                new ItemStack(ModItems.MATTER_UNIT_PHASE.get(), 1));

        for (int i = 0; i < 5; i++) {
            ImagFusorBlockEntity.tick(level, abs, helper.getBlockState(rel), fusor);
        }

        assertValue(helper, 8000, fusor.getLiquidAmount(), "la cuve reste pleine");
        assertValue(helper, 1, fusor.getInventory()
                        .getStackInSlot(ImagFusorBlockEntity.SLOT_IMAG_INPUT).getCount(),
                "l'unite doit rester intacte tant que la cuve est pleine");
        helper.succeed();
    }

    /**
     * Le vrai test de bout en bout : le noeud alimente le fusor, la cuve est
     * remplie d'unites de phase, et le cristal de basse purete doit ressortir en
     * cristal de purete moyenne apres 3000 mB et 120 ticks.
     */
    @GameTest(template = "empty")
    public static void imagFusorRefinesCrystalOnNetworkEnergy(GameTestHelper helper) {
        BlockPos nodeRel = new BlockPos(1, 1, 0);
        BlockPos fusorRel = new BlockPos(1, 1, 2);

        helper.setBlock(nodeRel, ModBlocks.NODE_BASIC.get());
        helper.setBlock(fusorRel, ModBlocks.IMAG_FUSOR.get());
        var node = (NodeBlockEntity) helper.getBlockEntity(nodeRel);
        var fusor = (ImagFusorBlockEntity) helper.getBlockEntity(fusorRel);

        ServerLevel level = helper.getLevel();
        BlockPos nodeAbs = helper.absolutePos(nodeRel);
        BlockPos fusorAbs = helper.absolutePos(fusorRel);

        for (int i = 0; i < 100; i++) {
            ImagFusorBlockEntity.tick(level, fusorAbs, helper.getBlockState(fusorRel), fusor);
        }
        assertValue(helper, nodeAbs, ImagNetworkData.get(level).nodeOf(fusorAbs),
                "le fusor doit avoir trouve le noeud avant de travailler");

        // Recette documentee dans imag_fusor.md : 3000 mB pour passer de la basse
        // purete a la purete moyenne.
        fusor.getInventory().setStackInSlot(ImagFusorBlockEntity.SLOT_INPUT,
                new ItemStack(ModItems.CRYSTAL_LOW.get()));
        fusor.setLiquidAmount(3000);
        node.setEnergy(2000.0d);

        // 120 ticks de fusion, plus la recherche de recette.
        for (int i = 0; i < 200; i++) {
            ImagFusorBlockEntity.tick(level, fusorAbs, helper.getBlockState(fusorRel), fusor);
            NodeBlockEntity.serverTick(level, nodeAbs, helper.getBlockState(nodeRel), node);
        }

        ItemStack output = fusor.getInventory().getStackInSlot(ImagFusorBlockEntity.SLOT_OUTPUT);
        assertValue(helper, ModItems.CRYSTAL_NORMAL.get(), output.getItem(), "cristal affine");
        assertValue(helper, 1, output.getCount(), "quantite produite");
        assertTrue(helper, fusor.getInventory().getStackInSlot(ImagFusorBlockEntity.SLOT_INPUT).isEmpty(),
                "le cristal de depart doit avoir ete consomme");
        assertValue(helper, 0, fusor.getLiquidAmount(), "la phase doit avoir ete consommee");
        assertTrue(helper, node.getEnergy() < 2000.0d,
                "le noeud aurait du fournir de l'energie, il en a " + node.getEnergy());
        helper.succeed();
    }

    /** Sans assez de phase, la fusion ne doit pas demarrer. */
    @GameTest(template = "empty")
    public static void imagFusorNeedsEnoughPhaseLiquid(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.IMAG_FUSOR.get());
        var fusor = (ImagFusorBlockEntity) helper.getBlockEntity(rel);

        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(rel);

        fusor.getInventory().setStackInSlot(ImagFusorBlockEntity.SLOT_INPUT,
                new ItemStack(ModItems.CRYSTAL_LOW.get()));
        fusor.setEnergy(2000.0d);
        // 2999 mB : juste un de moins que ce que la recette demande.
        fusor.setLiquidAmount(2999);

        for (int i = 0; i < 200; i++) {
            ImagFusorBlockEntity.tick(level, abs, helper.getBlockState(rel), fusor);
        }

        assertFalse(helper, fusor.isWorking(), "2999 mB ne suffisent pas pour une recette a 3000");
        assertTrue(helper, fusor.getInventory().getStackInSlot(ImagFusorBlockEntity.SLOT_OUTPUT).isEmpty(),
                "rien ne doit avoir ete produit");
        assertValue(helper, 1, fusor.getInventory().getStackInSlot(ImagFusorBlockEntity.SLOT_INPUT).getCount(),
                "le cristal de depart doit etre intact");
        assertValue(helper, 2999, fusor.getLiquidAmount(), "la phase doit etre intacte");
        helper.succeed();
    }

    /** La cuve n'accepte que la phase, et les sorties n'acceptent rien. */
    @GameTest(template = "empty")
    public static void imagFusorProtectsItsSlots(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.IMAG_FUSOR.get());
        var fusor = (ImagFusorBlockEntity) helper.getBlockEntity(rel);
        var inventory = fusor.getInventory();

        assertTrue(helper, inventory.isItemValid(ImagFusorBlockEntity.SLOT_IMAG_INPUT,
                new ItemStack(ModItems.MATTER_UNIT_PHASE.get())), "l'unite de phase doit etre acceptee");
        assertFalse(helper, inventory.isItemValid(ImagFusorBlockEntity.SLOT_IMAG_INPUT,
                new ItemStack(ModItems.MATTER_UNIT.get())), "l'unite vide ne doit pas entrer");
        assertFalse(helper, inventory.isItemValid(ImagFusorBlockEntity.SLOT_OUTPUT,
                new ItemStack(ModItems.CRYSTAL_NORMAL.get())), "la sortie ne doit rien accepter");
        assertFalse(helper, inventory.isItemValid(ImagFusorBlockEntity.SLOT_IMAG_OUTPUT,
                new ItemStack(ModItems.MATTER_UNIT.get())), "les unites vides sont posees par la machine");
        helper.succeed();
    }

    /** La cuve refuse tout fluide qui n'est pas la phase. */
    @GameTest(template = "empty")
    public static void imagFusorRefusesOtherFluids(GameTestHelper helper) {
        assertTrue(helper, ImagFusorBlockEntity.isPhaseLiquid(ModFluids.SOURCE_PHASE_LIQUID.get()),
                "le fluide du mod doit etre reconnu");
        assertTrue(helper, ImagFusorBlockEntity.isPhaseLiquid(ModFluids.FLOWING_PHASE_LIQUID.get()),
                "le fluide courant doit etre reconnu aussi");
        assertFalse(helper, ImagFusorBlockEntity.isPhaseLiquid(
                        net.minecraft.world.level.material.Fluids.WATER),
                "l'eau ne doit pas etre acceptee");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // Reseau energetique : l'eolienne, second generateur
    // ------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void windgenBaseIsAnEnergyGenerator(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.WINDGEN_BASE.get().defaultBlockState()
                .setValue(cn.academy.WindgenBaseBlock.HALF,
                        net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
        var base = (WindgenBaseBlockEntity) helper.getBlockEntity(rel);

        // Valeurs de TileWindGenBase : tampon 20000, bande passante LATENCY_MK3
        // = 300, production maximale 15 par tick.
        assertValue(helper, 20000, base.getMaxEnergyStored(), "tampon de l'eolienne");
        assertClose(helper, 300.0d, base.getBandwidth(), "bande passante de l'eolienne");
        assertClose(helper, 15.0d, WindgenBaseBlockEntity.MAX_GENERATION_SPEED, "production maximale");
        assertValue(helper, 8, WindgenBaseBlockEntity.MIN_PILLARS, "piliers minimum");
        assertValue(helper, 40, WindgenBaseBlockEntity.MAX_PILLARS, "piliers maximum");

        base.setEnergy(1000.0d);
        assertClose(helper, 250.0d, base.provideEnergy(250.0d), "energie fournie sur demande");
        assertClose(helper, 750.0d, base.getEnergy(), "reste du tampon");
        assertClose(helper, 750.0d, base.provideEnergy(900.0d), "le tampon se vide");
        assertClose(helper, 0.0d, base.provideEnergy(100.0d), "un tampon vide ne fournit rien");
        helper.succeed();
    }

    /** Seules les parties utiles portent un block entity, pas les leurres. */
    @GameTest(template = "empty")
    public static void windgenOnlyUsefulPartsHaveBlockEntities(GameTestHelper helper) {
        var lowerHalf = net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER;
        var upperHalf = net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER;

        BlockPos lower = new BlockPos(1, 1, 1);
        BlockPos upper = new BlockPos(1, 2, 1);
        helper.setBlock(lower, ModBlocks.WINDGEN_BASE.get().defaultBlockState()
                .setValue(cn.academy.WindgenBaseBlock.HALF, lowerHalf));
        helper.setBlock(upper, ModBlocks.WINDGEN_BASE.get().defaultBlockState()
                .setValue(cn.academy.WindgenBaseBlock.HALF, upperHalf));

        assertValue(helper, true, helper.getBlockEntity(lower) instanceof WindgenBaseBlockEntity,
                "la base basse doit porter le block entity");
        assertValue(helper, null, helper.getBlockEntity(upper),
                "la moitie haute ne doit pas en avoir, sinon elle aurait son propre tampon");

        BlockPos center = new BlockPos(2, 1, 1);
        BlockPos front = new BlockPos(2, 1, 2);
        helper.setBlock(center, ModBlocks.WINDGEN_MAIN.get().defaultBlockState()
                .setValue(cn.academy.WindgenMainBlock.PART, cn.academy.WindgenMainBlock.Part.CENTER));
        helper.setBlock(front, ModBlocks.WINDGEN_MAIN.get().defaultBlockState()
                .setValue(cn.academy.WindgenMainBlock.PART, cn.academy.WindgenMainBlock.Part.FRONT));

        assertValue(helper, true, helper.getBlockEntity(center) instanceof WindgenMainBlockEntity,
                "le centre du rotor doit porter le block entity");
        assertValue(helper, null, helper.getBlockEntity(front),
                "les bouts du rotor ne doivent pas en avoir, sinon il y aurait trois helices");
        helper.succeed();
    }

    /**
     * La zone balayee par les pales est un carre vertical. Le test se place loin
     * au-dessus des structures pour avoir de l'air garanti, pose un obstacle dans
     * la zone, et verifie que la reponse change.
     */
    @GameTest(template = "empty")
    public static void windgenBladeAreaIsChecked(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rotorPos = helper.absolutePos(new BlockPos(1, 1, 1)).atY(120);

        var facing = net.minecraft.core.Direction.NORTH;
        var state = ModBlocks.WINDGEN_MAIN.get().defaultBlockState()
                .setValue(cn.academy.WindgenMainBlock.FACING, facing)
                .setValue(cn.academy.WindgenMainBlock.PART, cn.academy.WindgenMainBlock.Part.CENTER);
        level.setBlock(rotorPos, state, 3);
        clearBladeArea(level, rotorPos, facing);

        assertTrue(helper, WindgenMainBlockEntity.computeNoObstacle(level, rotorPos, state),
                "de l'air partout : les pales doivent etre degagees");

        // Un bloc dans le plan, a cinq blocs du centre et trois blocs plus haut.
        var side = facing.getClockWise();
        BlockPos obstacle = rotorPos.relative(facing, 2).relative(side, 5).above(3);
        level.setBlock(obstacle, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        assertFalse(helper, WindgenMainBlockEntity.computeNoObstacle(level, rotorPos, state),
                "un bloc dans la zone doit arreter les pales");

        level.setBlock(obstacle, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        assertTrue(helper, WindgenMainBlockEntity.computeNoObstacle(level, rotorPos, state),
                "l'obstacle retire, les pales doivent repartir");
        helper.succeed();
    }

    /** Sans les huit piliers reglementaires, l'eolienne ne produit rien. */
    @GameTest(template = "empty")
    public static void windgenNeedsEnoughPillars(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos lower = helper.absolutePos(new BlockPos(1, 1, 1)).atY(80);

        WindgenBaseBlockEntity base = buildWindgen(level, lower, 7);
        WindgenMainBlockEntity rotor = rotorOf(level, lower, 7);
        assertTrue(helper, rotor != null, "le rotor doit avoir ete construit");
        rotor.getInventory().setStackInSlot(WindgenMainBlockEntity.SLOT_FAN,
                new ItemStack(ModItems.WINDGEN_FAN.get()));

        for (int i = 0; i < 20; i++) {
            WindgenBaseBlockEntity.tick(level, lower, level.getBlockState(lower), base);
        }

        assertValue(helper, WindgenBaseBlockEntity.Completeness.NO_TOP, base.getCompleteness(level),
                "sept piliers ne suffisent pas : la colonne est incomplete");
        assertClose(helper, 0.0d, base.getSimulatedGeneration(level), "aucune production");
        assertClose(helper, 0.0d, base.getEnergy(), "le tampon doit rester vide");
        helper.succeed();
    }

    /**
     * Le test de bout en bout : une colonne complete, une helice, rien devant les
     * pales, et le tampon se remplit a la vitesse prevue par la formule
     * d'altitude.
     */
    @GameTest(template = "empty")
    public static void windgenGeneratesWhenComplete(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos lower = helper.absolutePos(new BlockPos(1, 1, 1)).atY(80);

        WindgenBaseBlockEntity base = buildWindgen(level, lower, 8);
        WindgenMainBlockEntity rotor = rotorOf(level, lower, 8);
        assertTrue(helper, rotor != null, "le rotor doit avoir ete construit");
        assertTrue(helper, !rotor.isFanInstalled(), "sans helice, rien ne tourne");

        rotor.getInventory().setStackInSlot(WindgenMainBlockEntity.SLOT_FAN,
                new ItemStack(ModItems.WINDGEN_FAN.get()));
        assertTrue(helper, rotor.isFanInstalled(), "l'helice doit etre vue");

        // La revision de la zone balayee n'a lieu que tous les 20 ticks.
        for (int i = 0; i < 20; i++) {
            WindgenMainBlockEntity.tick(level, rotor.getBlockPos(), level.getBlockState(rotor.getBlockPos()),
                    rotor);
        }

        for (int i = 0; i < 20; i++) {
            WindgenBaseBlockEntity.tick(level, lower, level.getBlockState(lower), base);
        }

        assertValue(helper, WindgenBaseBlockEntity.Completeness.COMPLETE, base.getCompleteness(level),
                "la colonne doit etre reconnue complete");

        // Altitude du rotor : base a 80, deux blocs de base, huit piliers.
        int rotorY = 80 + 2 + 8;
        double expected = (0.5d + 0.5d * Math.min(1.0d, (rotorY - 70.0d) / 90.0d)) * 15.0d;
        assertClose(helper, expected, base.getSimulatedGeneration(level), "production a cette altitude");
        assertTrue(helper, base.getEnergy() > 0.0d,
                "le tampon aurait du se remplir, il est a " + base.getEnergy());
        helper.succeed();
    }

    /**
     * Construit une eolienne complete a une altitude ou l'air est garanti, et
     * rend sa base.
     *
     * Les blocs sont poses directement sur le niveau et non par
     * {@code helper.setBlock} : la colonne fait dix blocs de haut et sort de la
     * structure de test, ce qui est justement le but — on veut du vide autour des
     * pales.
     */
    private static WindgenBaseBlockEntity buildWindgen(ServerLevel level, BlockPos lower, int pillars) {
        var lowerHalf = net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER;
        var upperHalf = net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER;

        var baseState = ModBlocks.WINDGEN_BASE.get().defaultBlockState()
                .setValue(cn.academy.WindgenBaseBlock.FACING, net.minecraft.core.Direction.NORTH)
                .setValue(cn.academy.WindgenBaseBlock.HALF, lowerHalf);
        level.setBlock(lower, baseState, 3);
        level.setBlock(lower.above(), baseState.setValue(cn.academy.WindgenBaseBlock.HALF, upperHalf), 3);

        for (int i = 0; i < pillars; i++) {
            level.setBlock(lower.offset(0, 2 + i, 0),
                    ModBlocks.WINDGEN_PILLAR.get().defaultBlockState(), 3);
        }

        var facing = net.minecraft.core.Direction.NORTH;
        BlockPos center = lower.offset(0, 2 + pillars, 0);
        var rotorState = ModBlocks.WINDGEN_MAIN.get().defaultBlockState()
                .setValue(cn.academy.WindgenMainBlock.FACING, facing)
                .setValue(cn.academy.WindgenMainBlock.PART, cn.academy.WindgenMainBlock.Part.CENTER);
        level.setBlock(center, rotorState, 3);
        level.setBlock(center.relative(facing), rotorState
                .setValue(cn.academy.WindgenMainBlock.PART, cn.academy.WindgenMainBlock.Part.FRONT), 3);
        level.setBlock(center.relative(facing.getOpposite()), rotorState
                .setValue(cn.academy.WindgenMainBlock.PART, cn.academy.WindgenMainBlock.Part.BACK), 3);

        clearBladeArea(level, center, facing);
        return (WindgenBaseBlockEntity) level.getBlockEntity(lower);
    }

    /**
     * Vide le plan balaye par les pales, et charge au passage les chunks qu'il
     * traverse.
     *
     * Indispensable dans un test : les structures des autres tests sont posees
     * cote a cote dans le meme monde, et un chunk non charge compte comme un
     * obstacle. Sans ce nettoyage, le resultat dependrait de l'ordre des tests.
     */
    private static void clearBladeArea(ServerLevel level, BlockPos rotorCenter, net.minecraft.core.Direction facing) {
        final int radius = 7;
        BlockPos plane = rotorCenter.relative(facing, 2);
        net.minecraft.core.Direction side = facing.getClockWise();

        for (int along = -radius; along <= radius; along++) {
            for (int dy = -radius; dy <= radius; dy++) {
                level.setBlock(plane.relative(side, along).above(dy),
                        net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    /** Le rotor au sommet de la colonne construite par {@link #buildWindgen}. */
    private static WindgenMainBlockEntity rotorOf(ServerLevel level, BlockPos lower, int pillars) {
        BlockPos center = lower.offset(0, 2 + pillars, 0);
        return level.getBlockEntity(center) instanceof WindgenMainBlockEntity rotor ? rotor : null;
    }

    @GameTest(template = "empty")
    public static void matrixAnchorIsFoundFromEveryPart(GameTestHelper helper) {
        BlockPos anchorRel = new BlockPos(1, 1, 1);
        helper.setBlock(anchorRel, ModBlocks.MATRIX.get());
        BlockPos anchorAbs = helper.absolutePos(anchorRel);

        for (net.minecraft.core.Direction facing : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            for (cn.academy.MatrixBlock.MatrixPart part : cn.academy.MatrixBlock.MatrixPart.values()) {
                net.minecraft.world.level.block.state.BlockState state = ModBlocks.MATRIX.get()
                        .defaultBlockState()
                        .setValue(cn.academy.MatrixBlock.FACING, facing)
                        .setValue(cn.academy.MatrixBlock.PART, part);

                net.minecraft.core.Direction left = facing.getCounterClockWise();
                net.minecraft.core.Direction back = facing.getOpposite();
                BlockPos partAbs = anchorAbs.above(part.y).relative(left, part.l).relative(back, part.b);

                assertValue(helper, anchorAbs, cn.academy.MatrixBlock.anchorOf(partAbs, state),
                        "ancrage retrouve depuis " + part + " facing " + facing);
            }
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // Reseau energetique : generateurs et recepteurs raccordes a un noeud
    // ------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void solarGeneratorIsAnEnergyGenerator(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.SOLAR_GEN.get());
        var solar = (cn.academy.SolarGenBlockEntity) helper.getBlockEntity(rel);

        // Valeurs de l'original : tampon 1000, bande passante LATENCY_MK2 = 100.
        assertValue(helper, 1000, solar.getMaxEnergyStored(), "tampon du generateur solaire");
        assertClose(helper, 100.0d, solar.getBandwidth(), "bande passante du generateur solaire");

        solar.setEnergy(500.0d);
        assertClose(helper, 250.0d, solar.provideEnergy(250.0d), "energie fournie sur demande");
        assertClose(helper, 250.0d, solar.getEnergyStored(), "ce qui reste dans le tampon");

        // On ne peut pas fournir plus que ce qu'on a.
        assertClose(helper, 250.0d, solar.provideEnergy(900.0d), "le tampon se vide");
        assertClose(helper, 0.0d, solar.getEnergyStored(), "tampon vide");
        assertClose(helper, 0.0d, solar.provideEnergy(100.0d), "un tampon vide ne fournit rien");
        helper.succeed();
    }

    /** Le generateur doit trouver le noeud tout seul, comme un noeud trouve son Matrix. */
    @GameTest(template = "empty")
    public static void solarGeneratorFindsANearbyNode(GameTestHelper helper) {
        BlockPos nodeRel = new BlockPos(1, 1, 0);
        BlockPos solarRel = new BlockPos(1, 1, 2);

        helper.setBlock(nodeRel, ModBlocks.NODE_BASIC.get());
        helper.setBlock(solarRel, ModBlocks.SOLAR_GEN.get());
        var solar = (cn.academy.SolarGenBlockEntity) helper.getBlockEntity(solarRel);

        ServerLevel level = helper.getLevel();
        BlockPos solarAbs = helper.absolutePos(solarRel);

        // La recherche n'a lieu que tous les 100 ticks.
        for (int i = 0; i < 100; i++) {
            cn.academy.SolarGenBlockEntity.tick(level, solarAbs, helper.getBlockState(solarRel), solar);
        }

        assertValue(helper, helper.absolutePos(nodeRel), ImagNetworkData.get(level).nodeOf(solarAbs),
                "noeud trouve par le generateur");
        assertTrue(helper, solar.isLinked(), "le generateur doit se savoir raccorde");
        helper.succeed();
    }

    /**
     * Le vrai test du raccordement : l'energie du generateur doit arriver dans le
     * noeud. C'est le seul endroit ou le generateur, le noeud, le registre des
     * raccordements et l'algorithme d'echange travaillent ensemble.
     */
    @GameTest(template = "empty")
    public static void solarGeneratorFeedsItsNode(GameTestHelper helper) {
        BlockPos nodeRel = new BlockPos(1, 1, 0);
        BlockPos solarRel = new BlockPos(1, 1, 2);

        helper.setBlock(nodeRel, ModBlocks.NODE_BASIC.get());
        helper.setBlock(solarRel, ModBlocks.SOLAR_GEN.get());
        var node = (NodeBlockEntity) helper.getBlockEntity(nodeRel);
        var solar = (cn.academy.SolarGenBlockEntity) helper.getBlockEntity(solarRel);

        ServerLevel level = helper.getLevel();
        BlockPos nodeAbs = helper.absolutePos(nodeRel);
        BlockPos solarAbs = helper.absolutePos(solarRel);

        for (int i = 0; i < 100; i++) {
            cn.academy.SolarGenBlockEntity.tick(level, solarAbs, helper.getBlockState(solarRel), solar);
        }
        assertValue(helper, nodeAbs, ImagNetworkData.get(level).nodeOf(solarAbs),
                "le generateur doit avoir trouve le noeud avant de lui fournir quoi que ce soit");

        // On fixe le tampon : la production reelle depend de l'heure du monde et
        // du ciel au-dessus de la structure de test, deux choses qu'on ne veut pas
        // avoir a deviner ici.
        solar.setEnergy(500.0d);
        node.setEnergy(0.0d);

        // Bande passante 100 des deux cotes : 100 par tick, soit 500 en 5 ticks.
        for (int i = 0; i < 5; i++) {
            NodeBlockEntity.serverTick(level, nodeAbs, helper.getBlockState(nodeRel), node);
        }

        assertClose(helper, 500.0d, node.getEnergy(), "energie recue par le noeud");
        assertClose(helper, 0.0d, solar.getEnergyStored(), "tampon du generateur vide");
        helper.succeed();
    }

    /** Un generateur casse ne doit pas laisser de raccordement fantome. */
    @GameTest(template = "empty")
    public static void nodeDropsUsersThatDisappeared(GameTestHelper helper) {
        BlockPos nodeRel = new BlockPos(1, 1, 0);
        BlockPos solarRel = new BlockPos(1, 1, 2);

        helper.setBlock(nodeRel, ModBlocks.NODE_BASIC.get());
        helper.setBlock(solarRel, ModBlocks.SOLAR_GEN.get());
        var node = (NodeBlockEntity) helper.getBlockEntity(nodeRel);

        ServerLevel level = helper.getLevel();
        BlockPos nodeAbs = helper.absolutePos(nodeRel);
        BlockPos solarAbs = helper.absolutePos(solarRel);
        ImagNetworkData.get(level).linkUser(nodeAbs, solarAbs);
        assertTrue(helper, ImagNetworkData.get(level).userCount(nodeAbs) >= 1,
                "le raccordement de depart doit exister");

        // Le generateur disparait. Le noeud ne doit pas le garder dans sa liste :
        // sinon un generateur casse continuerait de compter dans la capacite.
        helper.setBlock(solarRel, net.minecraft.world.level.block.Blocks.AIR);
        for (int i = 0; i < 20; i++) {
            NodeBlockEntity.serverTick(level, nodeAbs, helper.getBlockState(nodeRel), node);
        }

        assertValue(helper, 0, ImagNetworkData.get(level).userCount(nodeAbs),
                "le noeud doit oublier une machine disparue");
        helper.succeed();
    }

    /** Pose un Matrix et rend son block entity, en verifiant qu'il existe. */
    private static MatrixBlockEntity placeMatrix(GameTestHelper helper, BlockPos rel) {
        helper.setBlock(rel, ModBlocks.MATRIX.get());
        var be = helper.getBlockEntity(rel);
        assertTrue(helper, be instanceof MatrixBlockEntity,
                "matrix doit porter un MatrixBlockEntity, trouve : " + be);
        return (MatrixBlockEntity) be;
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
