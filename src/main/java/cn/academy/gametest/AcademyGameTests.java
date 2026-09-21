package cn.academy.gametest;

import cn.academy.AcademyCraft;
import cn.academy.ModBlocks;
import cn.academy.ModItems;
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
        assertTrue(helper, !ImagNetworkData.get(level).isLinked(nodeAbs),
                "un noeud fraichement pose ne doit pas etre raccorde");

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
