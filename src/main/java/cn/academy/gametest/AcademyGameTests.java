package cn.academy.gametest;

import cn.academy.AbilityInterfererBlock;
import cn.academy.AbilityInterfererBlockEntity;
import cn.academy.AcademyCraft;
import cn.academy.DeveloperBlockEntity;
import cn.academy.ImagFusorBlockEntity;
import cn.academy.MetalFormerBlockEntity;
import cn.academy.ability.develop.DevelopProgress.DevState;
import cn.academy.ModBlocks;
import cn.academy.ModFluids;
import cn.academy.ModItems;
import cn.academy.PhaseGeneratorBlock;
import cn.academy.PhaseGeneratorBlockEntity;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
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

        // Les identifiants sont ceux de l'original : c'est ce que le jeu ecrit sous le nom de
        // l'objet, et ce que les recettes, succes et tutoriels nomment.
        assertTrue(helper, "phase_gen".equals(net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(ModItems.PHASE_GENERATOR.get()).getPath()),
                "le generateur de phase s'appelle phase_gen");
        assertTrue(helper, "dev_advanced".equals(net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(ModItems.DEV_ADVANCED_ITEM.get()).getPath()),
                "le developpeur avance s'appelle dev_advanced");
        // Et le chargeur de debogage a ete retire : rien ne le nomme plus.
        assertTrue(helper, !net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("academy", "debug_charger")),
                "le chargeur de debogage a ete retire");
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
        // Et il porte son block entity : c'est lui qui fait dessiner les nappes du liquide.
        // Sans lui, l'imag phase serait un bloc noir.
        assertTrue(helper, helper.getBlockEntity(rel) != null,
                "le bloc de fluide doit porter un block entity pour ses nappes");
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

    /**
     * La machine annonce au client qu'elle travaille, et c'est tout ce qu'il lui faut.
     *
     * C'est la seule chose que le serveur doit dire pour que la machine sonne : le client
     * n'a ni recette ni energie, il ne peut donc pas recalculer `isWorking()`. Le port fait
     * voyager l'etat dans la balise de synchronisation, celle que la machine envoie deja
     * dix fois par seconde pour sa barre de progression — donc sans un paquet de plus.
     *
     * Le test lit la balise comme le ferait un client : c'est le contrat, et rien d'autre.
     * Il verifie les deux sens, parce qu'une machine qui se tait doit le dire aussi — sans
     * quoi le client ferait tourner une boucle pour un bloc au repos.
     */
    @GameTest(template = "empty")
    public static void lesMachinesAnnoncentQuandEllesTravaillent(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.METAL_FORMER.get());
        var former = (MetalFormerBlockEntity) helper.getBlockEntity(rel);

        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(rel);

        // Au repos, la balise est muette.
        assertFalse(helper, former.getUpdateTag().getBoolean("working"),
                "une machine au repos ne doit pas s'annoncer en marche");

        // Recette INCISE : une plaque de fer renforcee donne six aiguilles, comme dans le
        // test du reseau.
        former.getInventory().setStackInSlot(MetalFormerBlockEntity.SLOT_IN,
                new ItemStack(ModItems.REINFORCED_IRON_PLATE.get()));
        former.cycleMode(1); // PLATE -> INCISE
        former.setEnergy(3000.0d);

        // Cinq ticks pour trouver la recette, puis le premier tick de travail.
        for (int i = 0; i < 6; i++) {
            MetalFormerBlockEntity.tick(level, abs, helper.getBlockState(rel), former);
        }
        assertTrue(helper, former.isWorking(), "la machine doit travailler");
        assertTrue(helper, former.getUpdateTag().getBoolean("working"),
                "et le dire au client, qui n'a pas de quoi le recalculer");

        // Une fois l'entree consommee il n'y a plus rien a transformer : la machine se
        // tait, et le dit.
        for (int i = 0; i < 120; i++) {
            MetalFormerBlockEntity.tick(level, abs, helper.getBlockState(rel), former);
        }
        assertFalse(helper, former.isWorking(), "la machine doit avoir fini son travail");
        assertFalse(helper, former.getUpdateTag().getBoolean("working"),
                "et elle ne doit plus s'annoncer en marche");
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

        // La colonne est videe d'abord, et largement : les tests posent jusqu'a huit
        // piliers, donc une colonne laissee par un test precedent peut monter plus haut
        // que celle qu'on construit. Sans ce nettoyage, le test des sept piliers tombait
        // sur les dix blocs du test des huit, se croyait complet, produisait de
        // l'energie — et gardait ces 100 points dans son tampon alors qu'il verifiait
        // qu'il restait vide. Cela n'arrivait que selon l'ordre des tests, donc il
        // suffisait d'en ajouter un pour le voir apparaitre.
        for (int dy = 0; dy <= 14; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(lower.offset(0, dy, dz),
                        net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        }

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

    // ------------------------------------------------------------------
    // Le developeur d'aptitudes
    // ------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void developerValuesFollowItsType(GameTestHelper helper) {
        BlockPos normalRel = new BlockPos(0, 1, 0);
        BlockPos advancedRel = new BlockPos(2, 1, 0);
        helper.setBlock(normalRel, ModBlocks.DEV_NORMAL.get().defaultBlockState()
                .setValue(cn.academy.DeveloperBlock.PART, cn.academy.DeveloperBlock.DevPart.BASE));
        helper.setBlock(advancedRel, ModBlocks.DEV_ADVANCED.get().defaultBlockState()
                .setValue(cn.academy.DeveloperBlock.PART, cn.academy.DeveloperBlock.DevPart.BASE));

        var normal = (DeveloperBlockEntity) helper.getBlockEntity(normalRel);
        var advanced = (DeveloperBlockEntity) helper.getBlockEntity(advancedRel);

        // Chiffres de DeveloperType : normal tampon 50000 / bande passante 100,
        // avance tampon 200000 / bande passante 300.
        assertValue(helper, 50000, normal.getMaxEnergyStored(), "tampon du developeur normal");
        assertClose(helper, 100.0d, normal.getBandwidth(), "bande passante du normal");
        assertValue(helper, 200000, advanced.getMaxEnergyStored(), "tampon du developeur avance");
        assertClose(helper, 300.0d, advanced.getBandwidth(), "bande passante de l'avance");

        // L'avance va plus vite : 15 ticks par stimulation contre 20.
        assertValue(helper, 20, cn.academy.ability.develop.DeveloperType.NORMAL.getTps(), "tps normal");
        assertValue(helper, 15, cn.academy.ability.develop.DeveloperType.ADVANCED.getTps(), "tps avance");
        assertClose(helper, 35.0d, cn.academy.ability.develop.DeveloperType.NORMAL.getEnergyPerTick(),
                "cout par tick du normal");
        assertClose(helper, 40.0d, cn.academy.ability.develop.DeveloperType.ADVANCED.getEnergyPerTick(),
                "cout par tick de l'avance");

        assertClose(helper, 0.0d, normal.injectEnergy(1000.0d), "tout doit etre accepte");
        assertClose(helper, 1000.0d, normal.getEnergy(), "energie stockee");
        assertClose(helper, 49000.0d, normal.getRequiredEnergy(), "besoin restant");
        helper.succeed();
    }

    /** Le multi-bloc n'a qu'un block entity, sur son ancrage. */
    @GameTest(template = "empty")
    public static void developerOnlyTheAnchorHasABlockEntity(GameTestHelper helper) {
        BlockPos anchor = new BlockPos(1, 1, 1);
        helper.setBlock(anchor, ModBlocks.DEV_NORMAL.get().defaultBlockState()
                .setValue(cn.academy.DeveloperBlock.FACING, net.minecraft.core.Direction.NORTH)
                .setValue(cn.academy.DeveloperBlock.PART, cn.academy.DeveloperBlock.DevPart.BASE));

        assertValue(helper, true, helper.getBlockEntity(anchor) instanceof DeveloperBlockEntity,
                "l'ancrage doit porter le block entity");

        // Le leurre juste au-dessus, calcule comme le fait setPlacedBy.
        BlockPos dummy = anchor.above(cn.academy.DeveloperBlock.DevPart.F_TOP.h)
                .relative(net.minecraft.core.Direction.SOUTH, cn.academy.DeveloperBlock.DevPart.F_TOP.dist);
        helper.setBlock(dummy, ModBlocks.DEV_NORMAL.get().defaultBlockState()
                .setValue(cn.academy.DeveloperBlock.FACING, net.minecraft.core.Direction.NORTH)
                .setValue(cn.academy.DeveloperBlock.PART, cn.academy.DeveloperBlock.DevPart.F_TOP));

        assertValue(helper, null, helper.getBlockEntity(dummy),
                "les leurres ne doivent pas porter de block entity");
        helper.succeed();
    }

    /** Les huit parties du multi-bloc doivent retrouver l'ancrage. */
    @GameTest(template = "empty")
    public static void developerAnchorIsFoundFromEveryPart(GameTestHelper helper) {
        BlockPos anchorRel = new BlockPos(1, 1, 1);
        helper.setBlock(anchorRel, ModBlocks.DEV_NORMAL.get().defaultBlockState()
                .setValue(cn.academy.DeveloperBlock.PART, cn.academy.DeveloperBlock.DevPart.BASE));
        BlockPos anchorAbs = helper.absolutePos(anchorRel);

        for (net.minecraft.core.Direction facing : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            for (cn.academy.DeveloperBlock.DevPart part : cn.academy.DeveloperBlock.DevPart.values()) {
                var state = ModBlocks.DEV_NORMAL.get().defaultBlockState()
                        .setValue(cn.academy.DeveloperBlock.FACING, facing)
                        .setValue(cn.academy.DeveloperBlock.PART, part);

                // setPlacedBy pose les leurres « au fond » de la face choisie.
                BlockPos partAbs = anchorAbs.above(part.h)
                        .relative(facing.getOpposite(), part.dist);

                assertValue(helper, anchorAbs, cn.academy.DeveloperBlock.anchorOf(partAbs, state),
                        "ancrage retrouve depuis " + part + " facing " + facing);
            }
        }
        helper.succeed();
    }

    /**
     * Le test de bout en bout : un joueur lance un apprentissage, le developeur
     * tire son energie, et la categorie monte d'un niveau.
     */
    @GameTest(template = "empty")
    public static void developerDevelopsACategory(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.DEV_NORMAL.get().defaultBlockState()
                .setValue(cn.academy.DeveloperBlock.PART, cn.academy.DeveloperBlock.DevPart.BASE));
        var developer = (DeveloperBlockEntity) helper.getBlockEntity(rel);
        BlockPos abs = helper.absolutePos(rel);

        var player = fakePlayer(helper);
        var category = cn.academy.ability.CategoryManager.INSTANCE.getCategory(2);
        assertTrue(helper, category != null, "la troisieme categorie doit exister");
        int categoryId = category.getCategoryId();

        assertValue(helper, 0, levelOf(player, category), "aucune categorie apprise au depart");
        assertTrue(helper, developer.startDeveloping(player, categoryId),
                "l'apprentissage doit pouvoir demarrer");

        // Cout du niveau 0 vers 1 : 5 stimulations, 700 d'energie chacune.
        assertValue(helper, 5, developer.getMaxStim(), "stimulations pour le premier niveau");
        assertClose(helper, 3500.0d,
                cn.academy.ability.develop.DeveloperType.NORMAL.getTotalCost(developer.getMaxStim()),
                "cout total du premier niveau");

        developer.setEnergy(50000.0d);

        // 5 stimulations de 21 ticks chacune (l'original comptait tps + 1).
        for (int i = 0; i < 200 && developer.getState() == DevState.DEVELOPING; i++) {
            DeveloperBlockEntity.tick(level, abs, helper.getBlockState(rel), developer);
        }

        assertValue(helper, DevState.DONE, developer.getState(),
                "l'apprentissage doit avoir abouti");
        assertValue(helper, 1, levelOf(player, category), "la categorie doit etre apprise");
        helper.succeed();
    }

    /** Sans energie, l'apprentissage echoue et rien n'est appris. */
    @GameTest(template = "empty")
    public static void developerFailsWithoutEnergy(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.DEV_NORMAL.get().defaultBlockState()
                .setValue(cn.academy.DeveloperBlock.PART, cn.academy.DeveloperBlock.DevPart.BASE));
        var developer = (DeveloperBlockEntity) helper.getBlockEntity(rel);
        BlockPos abs = helper.absolutePos(rel);

        var player = fakePlayer(helper);
        var category = cn.academy.ability.CategoryManager.INSTANCE.getCategory(1);
        assertTrue(helper, category != null, "la deuxieme categorie doit exister");

        assertTrue(helper, developer.startDeveloping(player, category.getCategoryId()),
                "l'apprentissage doit pouvoir demarrer");

        // Un seul tick suffit : le tampon est vide, le developeur ne peut rien tirer.
        DeveloperBlockEntity.tick(level, abs, helper.getBlockState(rel), developer);

        assertValue(helper, DevState.FAILED, developer.getState(),
                "sans energie l'apprentissage doit echouer");
        assertValue(helper, 0, levelOf(player, category), "et rien ne doit etre appris");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // Developpeur : apprendre une competence
    // ------------------------------------------------------------------

    /**
     * Le test de bout en bout : une competence s'apprend au developpeur.
     *
     * La categorie d'abord, la competence ensuite : c'est l'enchainement que le
     * joueur suit en jeu, et il verifie au passage que le niveau de la categorie
     * ouvre bien la competence du meme niveau.
     */
    @GameTest(template = "empty")
    public static void developerLearnsASkill(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.DEV_NORMAL.get().defaultBlockState()
                .setValue(cn.academy.DeveloperBlock.PART, cn.academy.DeveloperBlock.DevPart.BASE));
        var developer = (DeveloperBlockEntity) helper.getBlockEntity(rel);
        BlockPos abs = helper.absolutePos(rel);

        var player = fakePlayer(helper);
        player.getAbilities().instabuild = false;

        // Categorie 3 = vecmanip, la seule que les autres tests du developpeur ne
        // touchent pas (ils prennent les categories 1 et 2). Les tests partagent le
        // meme faux joueur et peuvent tourner en parallele : deux tests sur la meme
        // categorie se marcheraient dessus.
        var category = cn.academy.ability.CategoryManager.INSTANCE.getCategory(3);
        assertTrue(helper, category != null, "la quatrieme categorie doit exister");
        assertValue(helper, "vecmanip", category.getName(), "categorie attendue");

        var skill = category.getSkill("vec_accel");
        assertTrue(helper, skill != null, "vec_accel doit etre portee");
        assertValue(helper, 2, skill.getLevel(), "vec_accel est une competence de niveau 2");
        assertValue(helper, 5, skill.getLearningStims(), "prix de vec_accel : 3 + 2*2/2");

        // vec_accel descend du choc dirige : le developpeur refuse une competence dont la
        // parente n'est pas apprise, et c'est la seule des competences du port a avoir un
        // parent au meme niveau que son point d'entree.
        var parent = category.getSkill("dir_shock");
        assertTrue(helper, parent != null, "dir_shock doit etre portee");

        // L'etat de depart est pose ici et non suppose : le joueur est partage.
        setCategoryLevel(player, category, 0);
        forgetSkill(player, skill);
        forgetSkill(player, parent);

        // Sans le niveau, la competence est refusee.
        assertFalse(helper, developer.startDeveloping(player, category.getCategoryId(), skill.getId()),
                "au niveau 0, une competence de niveau 2 ne doit pas s'ouvrir");
        assertValue(helper, DevState.IDLE, developer.getState(),
                "un refus ne doit pas laisser la machine en marche");

        // Avec le niveau, elle s'ouvre — mais il faut d'abord sa parente : vec_accel
        // descend du choc dirige, comme body_intensify a besoin de arc_gen (la regle
        // elle-meme est verifiee par developerSkillHonoursItsDependency).
        setCategoryLevel(player, category, 2);
        learnSkill(player, parent);
        assertTrue(helper, developer.startDeveloping(player, category.getCategoryId(), skill.getId()),
                "avec le niveau 2 et sa parente, la competence doit s'ouvrir");
        assertValue(helper, skill.getId(), developer.getSkillId(), "la cible est la competence");
        assertValue(helper, 5, developer.getMaxStim(), "cinq stimulations pour vec_accel");

        developer.setEnergy(50000.0d);

        // 5 stimulations de 21 ticks chacune (l'original comptait tps + 1).
        for (int i = 0; i < 300 && developer.getState() == DevState.DEVELOPING; i++) {
            DeveloperBlockEntity.tick(level, abs, helper.getBlockState(rel), developer);
        }

        assertValue(helper, DevState.DONE, developer.getState(),
                "l'apprentissage doit aboutir");
        assertTrue(helper, skillLearned(player, skill), "la competence doit etre apprise");
        assertValue(helper, -1, developer.getSkillId(), "la cible est effacee une fois termine");

        // On relance : la meme competence ne s'apprend pas deux fois.
        assertFalse(helper, developer.startDeveloping(player, category.getCategoryId(), skill.getId()),
                "une competence deja apprise ne se reapprend pas");

        // Le niveau ne monte plus tout seul : il faut avoir rempli le palier en
        // utilisant ses competences. vecmanip au niveau 2 a maintenant DEUX competences
        // utilisables — vec_accel et vec_deviation — donc le palier vaut 2 * 0,666,
        // et l'avancement est encore a zero.
        assertFalse(helper, developer.startDeveloping(player, category.getCategoryId()),
                "sans avancement, la categorie ne doit pas monter");

        // Un usage d'experience, comme le ferait l'activation de la competence. 1,5 et
        // non 1,0 : l'experience d'une competence plafonne a 1, mais l'avancement du
        // niveau recoit le montant complet — c'est ce qui permet de franchir un palier
        // de plus d'un point.
        player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .ifPresent(data -> data.addSkillExp(skill, 1.5f));

        assertTrue(helper, developer.startDeveloping(player, category.getCategoryId()),
                "une fois le palier rempli, la categorie monte");
        assertValue(helper, -1, developer.getSkillId(),
                "un apprentissage de niveau ne vise aucune competence");
        developer.abort();

        // L'ecran doit pouvoir expliquer le refus, et proposer la montee quand elle est
        // ouverte : c'est la meme donnee qui alimente la barre de palier, le bouton
        // grise et son infobulle. Un ecran qui refuserait sans rien dire ferait croire
        // a une machine cassee.
        var menu = new cn.academy.DeveloperMenu(0, player.getInventory(), developer);
        assertValue(helper, 1f, menu.getLevelProgress(category.getCategoryId()),
                "le palier doit apparaitre rempli sur l'ecran");
        assertTrue(helper, menu.canLevelUp(category.getCategoryId()),
                "l'ecran doit proposer la montee");

        setCategoryLevel(player, category, 2);
        assertValue(helper, 0f, menu.getLevelProgress(category.getCategoryId()),
                "changer de niveau doit vider le palier a l'ecran aussi");
        assertFalse(helper, menu.canLevelUp(category.getCategoryId()),
                "et l'ecran doit refuser la montee");

        // On remet la categorie comme on l'a trouvee : le joueur est partage.
        forgetSkill(player, skill);
        forgetSkill(player, parent);
        setCategoryLevel(player, category, 0);
        helper.succeed();
    }

    /** Une competence dont la dependance n'est pas apprise est refusee, puis acceptee. */
    @GameTest(template = "empty")
    public static void developerSkillHonoursItsDependency(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.DEV_NORMAL.get().defaultBlockState()
                .setValue(cn.academy.DeveloperBlock.PART, cn.academy.DeveloperBlock.DevPart.BASE));
        var developer = (DeveloperBlockEntity) helper.getBlockEntity(rel);

        var player = fakePlayer(helper);

        // Categorie 0 = electromaster : body_intensify y porte DEUX dependances, et
        // l'original demandait toute l'experience dans chacune (l'arc, et le branchement
        // d'une machine). Le seuil est ce qu'il y a de plus facile a perdre en recopiant
        // l'arbre : « apprise » ne suffit pas, il faut s'en etre servi.
        var category = cn.academy.ability.CategoryManager.INSTANCE.getCategory(0);
        var arc = category.getSkill("arc_gen");
        var charging = category.getSkill("charging");
        var child = category.getSkill("body_intensify");
        assertTrue(helper, arc != null && charging != null && child != null,
                "les trois competences doivent etre portees");

        assertValue(helper, 3, child.getLevel(), "body_intensify est de niveau 3");
        assertValue(helper, 2, child.getDependencies().size(), "deux dependances");
        assertTrue(helper, child.getDependencies().contains(arc), "l'arc en fait partie");
        assertTrue(helper, child.getDependencies().contains(charging),
                "et le branchement aussi");
        assertValue(helper, 7, child.getLearningStims(), "prix de body_intensify : 3 + 3*3/2");

        setCategoryLevel(player, category, 3);
        forgetSkill(player, arc);
        forgetSkill(player, charging);

        assertFalse(helper, developer.startDeveloping(player, category.getCategoryId(), child.getId()),
                "sans les dependances, la competence ne doit pas s'ouvrir");

        // L'arc appris ne suffit pas : il en manque une.
        learnSkill(player, arc);
        assertFalse(helper, developer.startDeveloping(player, category.getCategoryId(), child.getId()),
                "une seule des deux dependances ne suffit pas");

        // Apprendre le branchement ne suffit pas non plus : l'original demandait TOUTE
        // l'experience dans chacune des deux.
        learnSkill(player, charging);
        assertFalse(helper, developer.startDeveloping(player, category.getCategoryId(), child.getId()),
                "apprendre le branchement ne suffit pas, il faut l'avoir pousse a fond");

        player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .ifPresent(data -> {
                    data.addSkillExp(arc, 1f);
                    data.addSkillExp(charging, 1f);
                });

        assertTrue(helper, developer.startDeveloping(player, category.getCategoryId(), child.getId()),
                "les deux dependances a fond : la competence s'ouvre");
        assertValue(helper, 7, developer.getMaxStim(), "sept stimulations pour body_intensify");

        // On n'attend pas la fin : ce test porte sur l'ouverture, pas sur le deroule.
        developer.abort();
        forgetSkill(player, arc);
        forgetSkill(player, charging);
        setCategoryLevel(player, category, 0);
        helper.succeed();
    }

    /**
     * Une machine normale n'enseigne pas les competences hautes.
     *
     * L'original deduisait la machine exigee du seul <b>niveau</b> de la competence : les
     * niveaux 1 et 2 tiennent dans l'objet portable, le 3 demande la machine normale, et
     * les niveaux 4 et 5 la machine avancee. Le railgun est de niveau 4 : ses deux
     * dependances satisfaites, il ne reste que la machine pour refuser — et c'est bien
     * elle que l'ecran nomme dans son infobulle.
     */
    @GameTest(template = "empty")
    public static void unDevelopeurNormalNEnseignePasLesCompetencesHautes(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.DEV_NORMAL.get().defaultBlockState()
                .setValue(cn.academy.DeveloperBlock.PART, cn.academy.DeveloperBlock.DevPart.BASE));
        var developer = (DeveloperBlockEntity) helper.getBlockEntity(rel);

        assertValue(helper, cn.academy.ability.develop.DeveloperType.NORMAL,
                developer.getDeveloperType(), "la machine posee est la normale");

        // Un apprenti a lui : les autres tests du developeur partagent le leur, et une
        // categorie montee ici ne doit pas les deranger.
        var player = ownPlayer(helper, "apprenti");
        var category = cn.academy.ability.CategoryManager.INSTANCE.getCategory(0);
        var railgun = category.getSkill("railgun");
        var thunderBolt = category.getSkill("thunder_bolt");
        var magManip = category.getSkill("mag_manip");
        var body = category.getSkill("body_intensify");
        var arc = category.getSkill("arc_gen");
        var charging = category.getSkill("charging");
        assertTrue(helper, railgun != null && thunderBolt != null && magManip != null
                        && body != null && arc != null && charging != null,
                "les competences de l'electromaster doivent etre portees");
        assertValue(helper, 4, railgun.getLevel(), "le railgun est de niveau 4");

        var data = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .resolve().orElseThrow();
        data.setCategoryLevel(category, 4);
        // Tout ce qui pourrait bloquer le railgun est satisfait : ses deux dependances,
        // apprises et poussees a fond.
        data.learnSkill(thunderBolt);
        data.learnSkill(magManip);
        data.addSkillExp(thunderBolt, 1f);
        data.addSkillExp(magManip, 1f);

        assertFalse(helper,
                developer.startDeveloping(player, category.getCategoryId(), railgun.getId()),
                "une machine normale ne doit pas enseigner une competence de niveau 4");

        // Et le refus est nomme : c'est la machine, pas autre chose — c'est ce que l'ecran
        // affichera en rouge sur la ligne de la competence.
        var blocker = cn.academy.ability.develop.LearningHelper.firstBlocker(data, railgun,
                cn.academy.ability.develop.DeveloperType.NORMAL);
        assertTrue(helper,
                blocker instanceof cn.academy.ability.develop.condition.ConditionDeveloperType,
                "le blocage doit venir de la machine : " + blocker);
        assertValue(helper, cn.academy.ability.develop.DeveloperType.ADVANCED,
                ((cn.academy.ability.develop.condition.ConditionDeveloperType) blocker).getRequired(),
                "et il doit nommer la machine qu'il faut");

        // La meme machine enseigne sans broncher ce qui est a son niveau : le niveau 3
        // demande la machine normale, et c'est exactement ce qu'elle est.
        data.setCategoryLevel(category, 3);
        data.learnSkill(arc);
        data.learnSkill(charging);
        data.addSkillExp(arc, 1f);
        data.addSkillExp(charging, 1f);
        assertTrue(helper,
                developer.startDeveloping(player, category.getCategoryId(), body.getId()),
                "une competence de niveau 3 doit s'ouvrir sur une machine normale");
        developer.abort();
        helper.succeed();
    }

    /**
     * Le developpeur portable : l'objet tient dans la main, et l'apprentissage avec.
     *
     * Meme ecran, meme deroule, meme prix que la machine — seule la source de l'energie
     * change. C'est ce qui permet a l'objet d'etre un developeur sans etre une machine, et
     * ce que l'original obtenait avec son interface `IDeveloper`.
     */
    @GameTest(template = "empty")
    public static void lePortableApprendDansLaMain(GameTestHelper helper) {
        var player = ownPlayer(helper, "portatif");

        // L'objet, charge a ras bord : c'est lui qui paie.
        var portable = new ItemStack(ModItems.DEVELOPER_PORTABLE.get());
        cn.academy.DeveloperPortableItem.charge(portable,
                cn.academy.DeveloperPortableItem.MAX_ENERGY);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, portable);

        var data = player
                .getCapability(cn.academy.ability.develop.PortableDevCapability.PORTABLE_DEV)
                .resolve().orElseThrow();
        var ability = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .resolve().orElseThrow();
        var category = cn.academy.ability.CategoryManager.INSTANCE.getCategory(0);

        // Un objet tenu ne se raccorde a aucun reseau : l'ecran le dit.
        assertFalse(helper, data.isLinked(), "un objet tenu ne se raccorde pas au reseau");
        assertValue(helper, (int) cn.academy.ability.develop.DeveloperType.PORTABLE.getEnergy(),
                data.getMaxEnergyStored(), "le tampon est celui du type portable");

        assertTrue(helper, data.startDeveloping(player, category.getCategoryId(), -1),
                "l'apprentissage doit demarrer");
        assertValue(helper, 5, data.getProgressData().getMaxStimulations(),
                "cinq stimulations pour le premier niveau");

        double before = cn.academy.DeveloperPortableItem.getEnergy(portable);
        // Cinq stimulations de 26 ticks (25 + 1, la cadence de l'original), payees 30 par
        // tick — le prix du type portable, reparti sur ses tps.
        for (int i = 0; i < 300 && data.getState() == DevState.DEVELOPING; i++) {
            cn.academy.ability.develop.PortableDevTracker.tick(player, data);
        }

        assertValue(helper, DevState.DONE, data.getState(), "l'apprentissage doit aboutir");
        assertValue(helper, 1, ability.getCategoryLevel(category), "la categorie doit etre apprise");

        double spent = before - cn.academy.DeveloperPortableItem.getEnergy(portable);
        assertTrue(helper, spent >= 3800.0d && spent <= 4000.0d,
                "l'objet doit avoir paye le prix de l'original : " + spent);
        helper.succeed();
    }

    /**
     * Un portable a sec, ou range : l'apprentissage s'arrete.
     *
     * L'original ne trouvait plus d'objet a qui demander l'energie, et marquait l'echec —
     * exactement comme une machine que le reseau ne suit plus. Et un objet ne peut pas
     * enseigner ce que sa qualite ne permet pas : les competences de niveau 3 demandent la
     * machine normale.
     */
    @GameTest(template = "empty")
    public static void lePortableSArreteQuandLaMainSeVide(GameTestHelper helper) {
        var player = ownPlayer(helper, "portatif_a_sec");
        var data = player
                .getCapability(cn.academy.ability.develop.PortableDevCapability.PORTABLE_DEV)
                .resolve().orElseThrow();
        var ability = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .resolve().orElseThrow();
        var category = cn.academy.ability.CategoryManager.INSTANCE.getCategory(0);

        // Sans l'objet en main, l'apprentissage ne demarre meme pas.
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        assertFalse(helper, data.startDeveloping(player, category.getCategoryId(), -1),
                "sans objet, il n'y a rien pour payer");

        // Avec un objet vide, il demarre puis echoue au premier tick.
        var empty = new ItemStack(ModItems.DEVELOPER_PORTABLE.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, empty);
        assertTrue(helper, data.startDeveloping(player, category.getCategoryId(), -1),
                "l'objet present suffit a demarrer");

        cn.academy.ability.develop.PortableDevTracker.tick(player, data);

        assertValue(helper, DevState.FAILED, data.getState(), "sans energie, tout est perdu");
        assertValue(helper, 0, ability.getCategoryLevel(category), "et rien n'est appris");

        // Un portable n'enseigne pas une competence de niveau 3 : il faut la machine
        // normale. La categorie est montee pour que le niveau ne soit pas la raison du
        // refus, et l'objet recharge pour qu'il ne soit pas l'energie non plus.
        var body = category.getSkill("body_intensify");
        assertTrue(helper, body != null, "body_intensify doit etre portee");
        ability.setCategoryLevel(category, 3);
        cn.academy.DeveloperPortableItem.charge(empty, cn.academy.DeveloperPortableItem.MAX_ENERGY);

        assertFalse(helper, data.startDeveloping(player, category.getCategoryId(), body.getId()),
                "un portable n'enseigne pas une competence de niveau 3");

        ability.setCategoryLevel(category, 0);
        helper.succeed();
    }

    /**
     * Changer de categorie : la machine avancee, une bobine en main, un facteur au sac.
     *
     * C'est le troisieme apprentissage du developpeur, et le seul qu'une machine normale ne
     * sait pas mener. L'original demandait trois choses a la fois : un joueur deja avance
     * (niveau 3), la bobine magnetique en main — elle est consommee — et un facteur
     * d'induction d'une autre categorie dans l'inventaire, qui dit vers quoi on bascule et
     * disparait lui aussi.
     *
     * Le port doit en plus choisir <b>quelle</b> categorie on quitte : l'original n'en
     * avait qu'une, ici c'est la plus haute. Le JUnit fige ce choix, ce test le fait
     * aboutir pour de vrai, jusqu'a la consommation des deux objets.
     */
    @GameTest(template = "empty")
    public static void changerDeCategorieOublieLAncienneEtConsommeLesObjets(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.DEV_ADVANCED.get().defaultBlockState()
                .setValue(cn.academy.DeveloperBlock.PART, cn.academy.DeveloperBlock.DevPart.BASE));
        var developer = (DeveloperBlockEntity) helper.getBlockEntity(rel);
        BlockPos abs = helper.absolutePos(rel);

        var player = ownPlayer(helper, "transfuge");
        var ability = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .resolve().orElseThrow();
        var from = cn.academy.ability.CategoryManager.INSTANCE.getCategory(0); // electromaster
        var to = cn.academy.ability.CategoryManager.INSTANCE.getCategory(1);   // meltdowner
        ability.setCategoryLevel(from, 3);
        ability.setCategoryLevel(to, 0);

        // Sans rien en main, rien n'est possible.
        assertTrue(helper, cn.academy.ability.develop.DevelopActionReset.find(player,
                        cn.academy.ability.develop.DeveloperType.ADVANCED) == null,
                "sans bobine, rien ne s'ouvre");

        // La bobine seule ne suffit pas : c'est le facteur qui dit vers quoi on bascule.
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(ModItems.MAGNETIC_COIL.get()));
        assertTrue(helper, cn.academy.ability.develop.DevelopActionReset.find(player,
                        cn.academy.ability.develop.DeveloperType.ADVANCED) == null,
                "il manque le facteur");

        player.getInventory().add(new ItemStack(ModItems.FACTOR_MELT.get()));

        // La machine normale ne sait pas le faire, meme avec tout ce qu'il faut.
        assertTrue(helper, cn.academy.ability.develop.DevelopActionReset.find(player,
                        cn.academy.ability.develop.DeveloperType.NORMAL) == null,
                "une machine normale ne change pas de categorie");

        var reset = cn.academy.ability.develop.DevelopActionReset.find(player,
                cn.academy.ability.develop.DeveloperType.ADVANCED);
        assertTrue(helper, reset != null, "la machine avancee, la bobine et le facteur : c'est ouvert");
        assertValue(helper, from, reset.getAbandoned(), "on quitte la categorie la plus haute");
        assertValue(helper, to, reset.getAdopted(), "pour celle du facteur");
        assertValue(helper, 30, reset.getStimulations(player), "dix stimulations par niveau oublie");

        // Et l'apprentissage va jusqu'au bout, sur la machine.
        developer.setEnergy(200_000.0d);
        assertTrue(helper, developer.startDeveloping(player, reset), "la machine doit accepter");
        for (int i = 0; i < 800 && developer.getState() == DevState.DEVELOPING; i++) {
            DeveloperBlockEntity.tick(helper.getLevel(), abs, helper.getBlockState(rel), developer);
        }

        assertValue(helper, DevState.DONE, developer.getState(), "l'apprentissage doit aboutir");
        assertValue(helper, 0, ability.getCategoryLevel(from), "l'ancienne categorie est oubliee");
        assertValue(helper, 2, ability.getCategoryLevel(to),
                "la nouvelle prend le niveau precedent, un cran en moins");
        assertTrue(helper, player.getMainHandItem().isEmpty(), "la bobine est consommee");
        assertTrue(helper, cn.academy.FactorItem.otherCategoryIn(player, from) == null,
                "et le facteur a disparu lui aussi");

        // Un changement de categorie efface aussi les prereglages : les competences qu'ils
        // allumaient ne sont plus apprises, donc les garder n'aurait pas de sens.
        var presets = cn.academy.ability.preset.PresetTracker.of(player);
        assertTrue(helper, presets != null, "le joueur doit porter la donnee des prereglages");
        assertTrue(helper, presets.getPreset(2).isEmpty(), "et elle doit avoir ete videe");
        helper.succeed();
    }

    /**
     * Les prereglages : quatre touches, quatre prereglages, une donnee de joueur.
     *
     * <p>Le modele se relit en JUnit ; ce qui ne se lit qu'avec un jeu, c'est que la donnee
     * soit bien <b>attachee au joueur</b>, qu'elle survive a la mort, et qu'un changement de
     * categorie l'efface — l'original le faisait sur l'evenement de changement de categorie.
     */
    @GameTest(template = "empty")
    public static void lesPrereglagesViventSurLeJoueur(GameTestHelper helper) {
        var player = ownPlayer(helper, "preregle");

        var data = cn.academy.ability.preset.PresetTracker.of(player);
        assertTrue(helper, data != null, "la capacite doit etre attachee au joueur");
        assertValue(helper, 0, data.getCurrentId(), "le premier prereglage est en service");
        assertTrue(helper, data.getPreset(1).isEmpty(), "et rien n'est range");

        // Le serveur range ce que le client lui demande, et rien d'autre.
        data.getPreset(1).assign(2, "arc_gen");
        assertValue(helper, "arc_gen", data.getPreset(1).nameAt(2), "competence rangee");
        assertTrue(helper, data.getPreset(0).isEmpty(), "et dans le bon prereglage");

        data.switchNext();
        assertValue(helper, 1, data.getCurrentId(), "la touche de changement avance d'un cran");

        // Les quatre prereglages sont sauvegardes avec le joueur, donc la donnee survit a
        // tout ce qui la relit depuis son etiquettes.
        var reread = new cn.academy.ability.preset.PresetData();
        reread.deserializeNBT(java.util.Objects.requireNonNull(data).serializeNBT());
        assertValue(helper, "arc_gen", reread.getPreset(1).nameAt(2), "l'aller-retour garde tout");
        assertValue(helper, 1, reread.getCurrentId(), "y compris le prereglage en service");

        cn.academy.ability.preset.PresetTracker.clearOnCategoryChange(player);
        assertTrue(helper, data.getPreset(1).isEmpty(), "un changement de categorie efface tout");
        assertValue(helper, 0, data.getCurrentId(), "et remet le premier en service");
        helper.succeed();
    }

    /**
     * Les deux gestes du client, du cote du serveur.
     *
     * <p>Le client ne fait que demander : c'est ce chemin-ci qui change la donnee, et qui la
     * renvoie ensuite complete. Un ecran qui se tromperait ne peut donc rien casser — il
     * sera corrige par le prochain envoi du serveur.
     */
    @GameTest(template = "empty")
    public static void leServeurRangeCeQueLeClientDemande(GameTestHelper helper) {
        var player = ownPlayer(helper, "preregle_serveur");
        var data = cn.academy.ability.preset.PresetTracker.of(player);
        assertTrue(helper, data != null, "la capacite doit etre attachee");

        cn.academy.ability.preset.network.PresetActionPacket.apply(player,
                cn.academy.ability.preset.network.PresetActionPacket.Action.ASSIGN, 2, 1, "railgun");
        assertValue(helper, "railgun", data.getPreset(2).nameAt(1), "la competence doit etre rangee");
        assertTrue(helper, data.getPreset(0).isEmpty(), "et dans le prereglage demande");

        // Le meme geste avec un nom nul libere la touche : c'est ainsi qu'on efface.
        cn.academy.ability.preset.network.PresetActionPacket.apply(player,
                cn.academy.ability.preset.network.PresetActionPacket.Action.ASSIGN, 2, 1, null);
        assertTrue(helper, data.getPreset(2).isEmpty(), "une touche se libere");

        // Un prereglage hors bornes est refuse, et un changement ramene dans le tour.
        cn.academy.ability.preset.network.PresetActionPacket.apply(player,
                cn.academy.ability.preset.network.PresetActionPacket.Action.ASSIGN, 9, 0, "arc_gen");
        assertTrue(helper, data.getPreset(0).isEmpty(), "un prereglage hors bornes ne range rien");

        cn.academy.ability.preset.network.PresetActionPacket.apply(player,
                cn.academy.ability.preset.network.PresetActionPacket.Action.SWITCH, 6, 0, null);
        assertValue(helper, 2, data.getCurrentId(), "un changement revient dans le tour");

        cn.academy.ability.preset.network.PresetActionPacket.apply(player,
                cn.academy.ability.preset.network.PresetActionPacket.Action.SWITCH, -1, 0, null);
        assertValue(helper, 2, data.getCurrentId(), "et un identifiant negatif ne fait rien");
        helper.succeed();
    }

    /**
     * Les tutoriels livres, et les objets qui les ouvrent.
     *
     * <p>Le contenu lui-meme se relit dans les ressources, en JUnit. Ce que JUnit ne
     * peut pas faire, c'est interroger les registres — et les tutoriels s'ouvrent par
     * des noms d'objets ecrits a la main, dont {@code academy:dev_normal}, un nom garde
     * de la 1.12.2. Un nom faux ne ferait rien tomber : il fermerait son tutoriel pour
     * toujours, sans un mot.
     */
    @GameTest(template = "empty")
    public static void lesTutorielsOuvrentParDesObjetsQuiExistent(GameTestHelper helper) {
        int named = 0;
        for (var entry : cn.academy.terminal.tutorial.TutorialLibrary.entries()) {
            for (String id : entry.requiredItems()) {
                named++;
                var key = net.minecraft.resources.ResourceLocation.tryParse(id);
                assertTrue(helper, key != null, "nom d'objet illisible : " + id);
                assertTrue(helper,
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(key),
                        "objet inconnu du tutoriel " + entry.id() + " : " + id);
            }
        }
        assertValue(helper, 17, named, "les objets ouvreurs de l'original");

        // Le contenu se lit aussi en jeu, et pas seulement en JUnit : c'est le seul
        // fichier livre que le jeu relit a l'ouverture d'un ecran.
        var welcome = cn.academy.terminal.tutorial.TutorialLibrary.load("welcome", "en_us");
        assertFalse(helper, welcome.isEmpty(), "le tutoriel d'accueil doit se lire en jeu");
        assertTrue(helper, welcome.contentFor("Misaka").length() > 20, "et porter du contenu");

        // L'application MisakaCloud : enregistree, et installee d'office comme l'original.
        var app = cn.academy.terminal.AppRegistry.INSTANCE.getByName("tutorial");
        assertTrue(helper, app != null, "l'application MisakaCloud doit etre enregistree");
        assertTrue(helper, app.isPreInstalled(), "et installee d'office, comme l'original");
        assertTrue(helper, app.getAppId() >= 0, "son identifiant est attribue a l'enregistrement");

        var player = ownPlayer(helper, "tutorial-reader");
        var data = player.getCapability(cn.academy.terminal.TerminalCapability.TERMINAL_DATA)
                .orElseThrow(() -> new IllegalStateException("le faux joueur doit porter la donnee"));
        data.install();
        assertTrue(helper, data.isInstalled(app),
                "le terminal doit offrir les applications installees d'office");

        helper.succeed();
    }

    /**
     * Les tutoriels s'ouvrent quand l'objet est obtenu, et ne se referment plus.
     *
     * <p>C'est le balayage du serveur (portage de celui de `TutorialData`, toutes les trois
     * ticks) : il regarde ce que le joueur a, et ouvre ce qui peut l'etre. Le port
     * regardait auparavant ce que le joueur <b>portait</b> au moment d'ouvrir l'ecran :
     * ranger son lingot dans un coffre refermait le tutoriel. L'original, lui, retenait ce
     * qui avait ete obtenu — et c'est ce que ce test protege.
     */
    @GameTest(template = "empty")
    public static void lesTutorielsSOuvrentQuandLObjetEstObtenu(GameTestHelper helper) {
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), 110);

        var player = ownPlayer(helper, "tutorial-keeper");
        // Le monde est partage et sauvegarde : le cadeau tombE au sol lors d'une execution
        // ratee y reste, et l'execution suivante en compte deux. D'ou un coin a soi, nettoye
        // avant de mesurer — la regle de tous les tests qui font tomber quelque chose.
        clearCorridor(helper, abs, 4);
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);

        var data = player.getCapability(
                        cn.academy.terminal.tutorial.TutorialCapability.TUTORIAL_DATA)
                .orElseThrow(() -> new IllegalStateException("le faux joueur doit porter la donnee"));
        data.reset();
        player.getInventory().clearContent();
        assertFalse(helper, data.isTerminalGiven(), "au depart, le cadeau n'a pas ete fait");

        // Rien en main : les tutoriels a objet restent fermes. Le cadeau de l'original, lui,
        // se fait au premier balayage — l'objet MisakaCloud tombe une fois, et une seule.
        cn.academy.terminal.tutorial.TutorialTracker.scan(player, data);
        assertFalse(helper, data.isUnlocked("ores"), "sans objet, le tutoriel reste ferme");
        assertFalse(helper, data.isUnlocked("solar_generator"), "et aucun autre ne s'ouvre");
        assertTrue(helper, data.isTerminalGiven(), "le premier balayage fait le cadeau");

        var drops = helper.getLevel().getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(4),
                entity -> entity.getItem().is(ModItems.TUTORIAL.get()));
        assertValue(helper, 1, drops.size(), "l'objet MisakaCloud doit tomber, une fois");

        // Un minerai de fer dans l'inventaire ouvre le tutoriel des minerais, et lui seul.
        player.getInventory().add(new ItemStack(ModBlocks.IMAGSIL_ORE.get()));
        cn.academy.terminal.tutorial.TutorialTracker.scan(player, data);
        assertTrue(helper, data.isUnlocked("ores"), "l'objet obtenu ouvre le tutoriel");
        assertFalse(helper, data.isUnlocked("solar_generator"), "un objet, un tutoriel");

        // Le ranger ne le referme pas : c'est ce qui distingue « obtenu » de « porte ».
        player.getInventory().clearContent();
        cn.academy.terminal.tutorial.TutorialTracker.scan(player, data);
        assertTrue(helper, data.isUnlocked("ores"),
                "un tutoriel ouvert ne se referme plus, meme l'objet range");

        // Et le cadeau ne se refait pas : il est acquis, comme dans l'original.
        var again = helper.getLevel().getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(4),
                entity -> entity.getItem().is(ModItems.TUTORIAL.get()));
        assertValue(helper, 1, again.size(), "et une seule fois");

        for (var drop : again) {
            drop.discard();
        }
        data.reset();
        helper.succeed();
    }

    /**
     * Les evenements de son annonces et enregistres sont les memes.
     *
     * <p>Un son se declare deux fois : dans {@code sounds.json}, qui dit quels fichiers
     * jouer, et dans le registre, qui lui donne un nom. Un evenement enregistre mais absent
     * du fichier se joue dans un <b>silence total</b>, sans la moindre erreur — et
     * l'inverse, un evenement annonce mais non enregistre, ne se joue pas du tout. JUnit
     * relit bien le fichier, mais il ne peut pas interroger le registre : c'est donc ici, et
     * nulle part ailleurs, que les deux listes se rencontrent.
     */
    @GameTest(template = "empty")
    public static void lesSonsDeclaresSontCeuxDuRegistre(GameTestHelper helper) {
        var declared = new java.util.TreeSet<String>();
        try (var in = AcademyGameTests.class.getResourceAsStream("/assets/academy/sounds.json")) {
            if (in == null) throw new IllegalStateException("sounds.json doit etre livre");
            var reader = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8);
            var root = com.google.gson.JsonParser.parseReader(reader);
            for (var entry : root.getAsJsonObject().entrySet()) {
                declared.add(entry.getKey());
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("sounds.json doit se lire", e);
        }

        var registered = new java.util.TreeSet<String>();
        for (var holder : cn.academy.ModSounds.all()) {
            assertTrue(helper, holder.isPresent(),
                    "l'evenement doit etre enregistre : " + holder.getId());
            registered.add(holder.getId().getPath());
        }

        assertValue(helper, 44, registered.size(), "les evenements de l'original");
        assertTrue(helper, declared.equals(registered),
                "chaque evenement annonce doit etre enregistre, et l'inverse : annonces "
                        + declared + ", enregistres " + registered);

        helper.succeed();
    }

    /** Niveau d'une categorie, pose directement : c'est le developpeur qui le monte en jeu. */
    private static void setCategoryLevel(Player player, cn.academy.ability.Category category, int level) {
        player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .ifPresent(data -> data.setCategoryLevel(category, level));
    }

    /** Vrai si ce joueur a appris cette competence. */
    private static boolean skillLearned(Player player, cn.academy.ability.Skill skill) {
        return player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .map(data -> data.isSkillLearned(skill))
                .orElse(false);
    }

    /** Apprend une competence a ce joueur, sans passer par le developpeur. */
    private static void learnSkill(Player player, cn.academy.ability.Skill skill) {
        player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .ifPresent(data -> data.learnSkill(skill));
    }

    /**
     * Oublie une competence sur ce joueur.
     *
     * Le faux joueur est un <b>singleton partage par tous les tests</b>, et ceux qui
     * se ressemblent peuvent tourner en parallele. Un test qui apprend une
     * competence doit donc la rendre en partant, et surtout ne pas remettre a zero
     * toute la progression : il effacerait le niveau qu'un autre test est en train
     * d'utiliser, et cet autre test echouerait a la fin de son apprentissage, avec
     * un message qui n'a l'air de rien avoir a faire avec celui qui a casse.
     */
    private static void forgetSkill(Player player, cn.academy.ability.Skill skill) {
        player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .ifPresent(data -> data.forgetSkill(skill));
    }

    /**
     * Un joueur simule, pour les tests qui ont besoin d'un porteur d'aptitudes.
     *
     * C'est un {@code FakePlayer} et non le joueur simule du framework : ce dernier
     * s'annonce comme un vrai joueur, ce qui declenche les gestionnaires de
     * connexion du mod — et l'envoi d'un paquet de synchronisation sur une connexion
     * sans canal leve une exception. Un faux joueur reste en dehors de la liste des
     * joueurs, donc rien de tout cela ne se declenche.
     */
    private static net.minecraft.server.level.ServerPlayer fakePlayer(GameTestHelper helper) {
        return net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
    }

    /**
     * Un faux joueur <b>a soi</b>, pour les tests qui ont besoin d'un decor stable.
     *
     * <p>Le faux joueur du framework est un singleton : sa position et son orientation sont
     * celles du dernier test qui les a posees, et deux tests qui tournent en parallele se
     * les reprennent l'un a l'autre. C'est ainsi qu'un test a cru viser le nord alors qu'un
     * voisin venait de le retourner. Celui-ci porte un nom different par test, donc personne
     * d'autre ne le deplace — et il n'entre pas non plus dans la liste des joueurs.
     */
    private static net.minecraft.server.level.ServerPlayer ownPlayer(GameTestHelper helper, String name) {
        var uuid = java.util.UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return net.minecraftforge.common.util.FakePlayerFactory.get(
                helper.getLevel(), new com.mojang.authlib.GameProfile(uuid, name));
    }

    /**
     * Nettoie le couloir d'un test avant qu'il ne mesure quoi que ce soit.
     *
     * <p>Le monde de test est partage, et il est <b>sauvegarde entre deux executions</b> :
     * un test qui echoue avant de ranger ses betes les laisse dans le monde, et l'execution
     * suivante les retrouve. Quatre vaches se sont ainsi accumulees au meme endroit, une par
     * execution ratee du meme test, et se disputaient la cible d'un rayon.
     *
     * <p>La boite est ancree sur la <b>structure du test</b>, jamais sur le faux joueur :
     * celui-ci est partage, et un autre test peut l'avoir deplace ailleurs entre-temps. Une
     * boite centree sur le joueur emporte alors les betes du voisin avec les siennes, et
     * c'est <b>son</b> test qui tombe.
     *
     * <p>Et elle se pose <b>en hauteur</b> : les tests qui ont besoin d'un monde a eux
     * travaillent quarante blocs au-dessus de la zone commune (voir {@link #aboveTestArea}),
     * ou rien de ce qu'un voisin pose ne peut se trouver.
     */
    private static void clearCorridor(GameTestHelper helper, BlockPos abs, int length) {
        var corridor = new net.minecraft.world.phys.AABB(
                abs.getX() - 1, abs.getY() - 1, abs.getZ() - 1,
                abs.getX() + 4, abs.getY() + 5, abs.getZ() + length);
        for (var leftover : helper.getLevel().getEntitiesOfClass(
                net.minecraft.world.entity.Entity.class, corridor)) {
            leftover.discard();
        }
    }

    /**
     * Le coin de monde d'un test qui a besoin d'etre seul, a une hauteur qui n'appartient
     * qu'a lui.
     *
     * <p>Le serveur de test place les structures cote a cote, et leurs cellules se touchent
     * presque : une bete posee par un test voisin tombe facilement au meme endroit que la
     * notre, et c'est alors la sienne qui se fait frapper — ou la notre qui ne l'est pas.
     * Il y a donc <b>une altitude par test</b>, par multiples de quarante : leurs couloirs
     * ne peuvent plus se croiser, et le nettoyage de l'un ne peut plus emporter les betes de
     * l'autre — ce qui arrivait quand deux tests partageaient la meme hauteur.
     *
     * <p>Attention : quarante blocs plus haut, on est <b>dans la pierre</b>. Bon pour un test
     * qui ne regarde que des entites, mauvais pour un test qui fait un lancer de rayon sur les
     * blocs — celui-la reste au niveau de la structure.
     */
    private static BlockPos aboveTestArea(GameTestHelper helper, BlockPos relative, int height) {
        return helper.absolutePos(relative).above(height);
    }

    /** Niveau d'une categorie pour un joueur, ou 0 s'il n'a pas la capacite. */
    private static int levelOf(net.minecraft.world.entity.player.Player player,
                               cn.academy.ability.Category category) {
        return player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .map(data -> data.getCategoryLevel(category))
                .orElse(0);
    }

    // ------------------------------------------------------------------
    // Aptitudes : ce que l'electromaster peut attirer
    // ------------------------------------------------------------------

    /**
     * La classification des metaux de l'electromaster.
     *
     * Elle se lit dans les registres, donc elle ne peut pas se tester en JUnit. C'est
     * pourtant elle qui decide ce que `mag_movement` et `mag_manip` peuvent accrocher,
     * et une faute de nom dans les listes de la config ne se verrait qu'en jeu, sous la
     * forme d'une competence qui refuse de s'accrocher sans rien dire.
     */
    @GameTest(template = "empty")
    public static void lesMetauxReconnusSontCeuxDeLOriginal(GameTestHelper helper) {
        var normal = net.minecraft.world.level.block.Blocks.IRON_BLOCK;
        var weak = net.minecraft.world.level.block.Blocks.HOPPER;

        assertTrue(helper, cn.academy.ability.electromaster.MetalTargets.isNormalMetalBlock(normal),
                "un bloc de fer est un metal franc");
        assertTrue(helper, cn.academy.ability.electromaster.MetalTargets.isMetalBlock(
                        net.minecraft.world.level.block.Blocks.RAIL),
                "un rail aussi");
        assertTrue(helper, cn.academy.ability.electromaster.MetalTargets.isWeakMetalBlock(weak),
                "un entonnoir est faiblement metallique");
        assertFalse(helper, cn.academy.ability.electromaster.MetalTargets.isMetalBlock(
                        net.minecraft.world.level.block.Blocks.STONE),
                "la pierre n'attire pas");

        // Les blocs faiblement metalliques demandent soixante pour cent d'experience :
        // sans cela, un debutant s'accrocherait a n'importe quelle machine.
        assertTrue(helper, cn.academy.ability.electromaster.MetalTargets.canHook(normal, 0f),
                "un metal franc accroche a tout niveau");
        assertFalse(helper, cn.academy.ability.electromaster.MetalTargets.canHook(weak, 0.5f),
                "pas une machine a moitie d'experience");
        assertTrue(helper, cn.academy.ability.electromaster.MetalTargets.canHook(weak, 0.6f),
                "mais oui au seuil de l'original");

        helper.succeed();
    }

    /** Les entites metalliques, qui se lisent aussi dans les registres. */
    @GameTest(template = "empty")
    public static void lesEntitesMetalliquesSontCellesDeLOriginal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(new BlockPos(2, 1, 2));

        var minecart = new net.minecraft.world.entity.vehicle.Minecart(
                net.minecraft.world.entity.EntityType.MINECART, level);
        minecart.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        var cow = new net.minecraft.world.entity.animal.Cow(
                net.minecraft.world.entity.EntityType.COW, level);
        cow.moveTo(abs.getX() + 1.5, abs.getY(), abs.getZ() + 0.5);

        assertTrue(helper, cn.academy.ability.electromaster.MetalTargets.isMetallic(minecart),
                "un wagonnet est metallique");
        assertFalse(helper, cn.academy.ability.electromaster.MetalTargets.isMetallic(cow),
                "une vache, non");

        helper.succeed();
    }

    /**
     * La lignee de {@code mag_movement}.
     *
     * <p>Ce que le joueur a demande et qu'aucun test unitaire ne peut voir : le premier bloc
     * se paie comme dans l'original — son vrai CP a chaque tick, meme quand le regard l'a
     * quitte — le surcout de l'ouverture ne redescend plus tant qu'on est accroche, chaque
     * nouveau bloc verse son dixieme de pourcent une seule fois, et le trajet entier se verse
     * a la fin avec son plancher de 0,5 %.
     */
    @GameTest(template = "empty")
    public static void laLigneeDuMagMovementSePaieParBloc(GameTestHelper helper) {
        var skill = cn.academy.ability.electromaster.ElectromasterCategory.MAG_MOVEMENT;
        var iron = net.minecraft.world.level.block.Blocks.IRON_BLOCK;
        BlockPos floor = new BlockPos(2, 1, 2);
        BlockPos abs = aboveTestArea(helper, floor, 320);
        BlockPos eyes = new BlockPos(floor.getX(), floor.getY() + 320 + 1, floor.getZ());

        // La chambre du test est nettoyee AVANT de poser quoi que ce soit : le monde des tests
        // est partage et sauvegarde, donc un bloc de fer laisse par l'execution precedente
        // serait accroche a la place de celui qu'on vient de poser. La dalle qui porte le faux
        // joueur (un cran sous les yeux) n'est pas touchee.
        for (int dz = 0; dz <= 6; dz++) {
            for (int dx = -1; dx <= 5; dx++) {
                for (int dy = 0; dy <= 2; dy++) {
                    helper.setBlock(eyes.offset(dx, dy, dz),
                            net.minecraft.world.level.block.Blocks.AIR);
                }
            }
        }

        var player = ownPlayer(helper, "lineage-rider");
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 180f, 0f);
        var data = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .orElseThrow(() -> new IllegalStateException("le faux joueur doit porter la donnee"));
        data.setCategoryLevel(skill.getCategory(), 2);
        data.learnSkill(skill);
        data.setControlPoint(data.getMaxControlPoint());

        // Le premier bloc : trois blocs devant les yeux, et le regard pile dessus.
        BlockPos premier = eyes.offset(0, 0, 3);
        helper.setBlock(premier, iron);
        lookAt(player, helper.absolutePos(premier));

        skill.onStart(player, data);
        // Le surcout de l'ouverture est deja pose par l'activation ; on le repose ici pour
        // lire ce que le premier bloc y ajoute : rien.
        data.setOverload(30f);
        assertTrue(helper, skill.onHoldTick(player, data, 1), "le maintien s'accroche au fer");
        assertValue(helper, 1, data.getHoldLineageSize(skill), "un seul bloc dans la lignee");
        assertClose(helper, 30f, data.getOverload(),
                "le premier bloc ne coute pas de surcout de plus");
        double apresUnTick = data.getControlPoint();
        assertTrue(helper, apresUnTick < data.getMaxControlPoint(),
                "et le maintien se paie des le premier tick : " + apresUnTick);

        // Le regard monte au ciel : l'ancre reste, et la depense continue. C'est le vrai mod,
        // et c'est ce que le joueur a vu manquer — une fois accroche, detourner les yeux
        // n'arrete ni la traction ni le paiement.
        player.setXRot(-60f);
        for (int tick = 2; tick <= 6; tick++) {
            assertTrue(helper, skill.onHoldTick(player, data, tick), "la traction tient sans viser");
        }
        assertTrue(helper, data.getControlPoint() < apresUnTick,
                "et elle continue de se payer : " + data.getControlPoint());
        assertValue(helper, 1, data.getHoldLineageSize(skill), "sans nouveau bloc vise");
        assertClose(helper, 30f, data.getOverload(), "et le surcout reste epingle");

        // Le deuxieme bloc : sur le cote, dans l'axe du regard. Il verse son dixieme de
        // pourcent, une seule fois, et ne coute aucun surcout de plus.
        BlockPos second = eyes.offset(4, 0, 0);
        helper.setBlock(second, iron);
        lookAt(player, helper.absolutePos(second));
        float expAvant = data.getSkillExp(skill);
        assertTrue(helper, skill.onHoldTick(player, data, 7), "le maintien prend le deuxieme bloc");
        assertValue(helper, 2, data.getHoldLineageSize(skill), "deux blocs dans la lignee");
        assertClose(helper, expAvant + cn.academy.ability.electromaster.MagMovementSkill
                        .EXP_PER_NEW_BLOCK, data.getSkillExp(skill), "un dixieme de pourcent");
        assertClose(helper, 30f, data.getOverload(), "et aucun surcout de plus");

        // Le reviser ne redonne rien : c'est le meme bloc de la lignee.
        float expStable = data.getSkillExp(skill);
        assertTrue(helper, skill.onHoldTick(player, data, 8), "le maintien continue");
        assertClose(helper, expStable, data.getSkillExp(skill), "reviser un bloc ne redonne rien");

        // La fin : le trajet entier, avec son plancher de 0,5 %, verse une seule fois — et
        // c'est la recompense du premier bloc, celle de l'original.
        float avantLaFin = data.getSkillExp(skill);
        skill.onHoldEnd(player, data, 8);
        assertTrue(helper, data.getSkillExp(skill) >= avantLaFin + 0.005f,
                "le premier bloc verse le trajet, plancher de 0,5 % : " + data.getSkillExp(skill));

        player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .ifPresent(gone -> gone.forgetSkill(skill));
        helper.succeed();
    }

    /** Tourne le regard d'un faux joueur vers un point du monde, des yeux. */
    private static void lookAt(net.minecraft.world.entity.Entity player, BlockPos target) {
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                net.minecraft.world.phys.Vec3.atCenterOf(target));
    }

    /**
     * L'aiguille du lancer d'objet, le seul objet que l'original distinguait.
     *
     * Le bonus se lit sur un objet reel, donc dans les registres : c'est pourquoi il est
     * ici et pas en JUnit.
     */
    @GameTest(template = "empty")
    public static void uneAiguilleLanceeFaitPlusMal(GameTestHelper helper) {
        var skill = cn.academy.ability.teleporter.TeleporterCategory.THREATENING_TELEPORT;
        var data = new cn.academy.ability.AbilityData();

        float needle = skill.damage(data, new ItemStack(ModItems.NEEDLE.get()));
        float stick = skill.damage(data, new ItemStack(net.minecraft.world.item.Items.STICK));

        assertClose(helper, needle, stick * 1.5f, "une aiguille fait moitie plus mal");
        assertClose(helper, stick, skill.damage(data), "et les autres objets font le degat de base");

        helper.succeed();
    }

    /** Les billes de plasma d'un joueur, en l'air — celles de sa propre bombe, et rien d'autre. */
    private static java.util.List<cn.academy.entity.EntityMdBall> ballsOf(
            GameTestHelper helper, net.minecraft.world.entity.player.Player player) {
        return helper.getLevel().getEntitiesOfClass(cn.academy.entity.EntityMdBall.class,
                player.getBoundingBox().inflate(4), ball -> ball.spawner() == player);
    }

    /**
     * La salve de la bombe a fragmentation.
     *
     * C'est la partie de la competence qu'aucun test unitaire ne peut voir : les billes
     * sont posees pendant le maintien, elles partent a la fin, et ce qu'elles touchent
     * depend d'un lancer de rayon dans le monde. Le faux joueur est partage par tous les
     * tests, donc la competence se rend en partant.
     */
    @GameTest(template = "empty")
    public static void lesBillesDeLaBombeTouchentCeQuellesVisent(GameTestHelper helper) {
        var skill = cn.academy.ability.meltdowner.MeltdownerCategory.SCATTER_BOMB;
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(6, 3, 2);
        BlockPos abs = aboveTestArea(helper, rel, 80);

        var player = ownPlayer(helper, "scatter_bomber");
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);

        // Le couloir est nettoye avant de poser la cible, jamais apres : une bete laissee la
        // par une execution ratee prendrait la salve a la place de la notre.
        clearCorridor(helper, abs, 16);

        // Et il se debarrasse de ce qui traine : le monde des tests est PARTAGE, et un voisin
        // qui deborde de sa cellule y laisse des fois de la pierre — c'est ce qui a fait tomber
        // ce test une fois, sur un mur apparu dans son couloir. La boite est large de sept blocs
        // parce qu'une bille se tient jusqu'a 1,3 bloc de cote et 1,2 bloc plus bas que son
        // porteur, et longue de neuf pour couvrir la ligne jusqu'aux yeux de la vache. Elle
        // s'ecrit en ABSOLU, sur le niveau : c'est la seule ecriture dont on sache ou elle tombe.
        for (int dz = -2; dz <= 6; dz++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    level.setBlockAndUpdate(abs.offset(dx, dy, dz),
                            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
            }
        }

        // Une vache trois blocs devant, dans l'axe du regard.
        var cow = new net.minecraft.world.entity.animal.Cow(
                net.minecraft.world.entity.EntityType.COW, level);
        cow.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 3.5, 0f, 0f);
        level.addFreshEntity(cow);

        var data = new cn.academy.ability.AbilityData();
        // Presque au maximum d'experience : les billes prennent la vache en chasse sans
        // dependre du hasard, et il reste de la place pour voir la salve en verser.
        data.setCategoryLevel(skill.getCategory(), 1);
        data.learnSkill(skill);
        data.addSkillExp(skill, 0.9f);

        // La ponte, par le vrai crochet du maintien : une bille toutes les dix ticks
        // pendant quatre secondes, chacune payee par l'entretien.
        for (int tick = 1; tick <= 80; tick++) {
            skill.onHoldTick(player, data, tick);
        }
        assertValue(helper, 7, ballsOf(helper, player).size(), "sept billes posees en quatre secondes");

        float before = cow.getHealth();

        // Avant la salve : la ligne de la bille vers la vache. Elle se verifie a part, comme
        // pour la bombe a electrons — un echec ne dirait pas, sinon, si c'est le tir ou la
        // ligne qui est en faute.
        var ahead = ballsOf(helper, player).get(0);
        var gunFrom = new net.minecraft.world.phys.Vec3(ahead.getX(),
                ahead.getY() + cn.academy.ability.meltdowner.MdBallVisuals.RENDER_HEIGHT,
                ahead.getZ());
        var gunTo = new net.minecraft.world.phys.Vec3(
                cow.getX(), cow.getY() + cow.getEyeHeight(), cow.getZ());
        var wall = level.clip(new net.minecraft.world.level.ClipContext(gunFrom, gunTo,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        assertFalse(helper, wall.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK,
                "aucun mur sur la ligne de la bille : " + wall.getLocation() + " dans "
                        + level.getBlockState(net.minecraft.core.BlockPos.containing(
                                wall.getLocation()))
                        + " ; de " + gunFrom + " vers " + gunTo + " ; bille " + ahead.position());
        var seen = cn.academy.ability.TargetingUtil.findEntityAlong(player, gunFrom, gunTo,
                e -> !(e instanceof cn.academy.entity.EntityMdBall));
        assertTrue(helper, seen == cow, "la vache est dans la ligne de la bille : vu "
                + (seen == null ? "rien" : seen.getName().getString())
                + " depuis " + gunFrom + " vers " + gunTo);

        // Fin du maintien : les billes partent.
        skill.onHoldEnd(player, data, 80);

        assertTrue(helper, cow.getHealth() < before,
                "une vache a trois blocs doit encaisser la salve entiere : " + cow.getHealth()
                        + " contre " + before);
        assertTrue(helper, data.getSkillExp(skill) > 0.9f, "et la salve rapporte son experience");
        assertValue(helper, 0, ballsOf(helper, player).size(),
                "et les billes ont toutes disparu en partant");

        cow.discard();
        player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .ifPresent(gone -> gone.forgetSkill(skill));
        helper.succeed();
    }

    /**
     * La bille de silicium, la premiere entite du mod.
     *
     * Son cycle de vie ne se lit qu'avec un monde : elle vole, elle se pose sur un bloc,
     * elle disparait dix ticks plus tard. C'est aussi ce que la salve de rayons ira
     * chercher — une bille <b>posee</b>, et rien d'autre — donc le drapeau qui distingue
     * les deux etats merite d'etre fige ici.
     */
    @GameTest(template = "empty")
    public static void laBilleLanceeFlottePuisTombeEtSePose(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos floor = new BlockPos(2, 1, 2);
        helper.setBlock(floor, net.minecraft.world.level.block.Blocks.STONE);
        BlockPos abs = helper.absolutePos(floor);

        var ball = new cn.academy.entity.EntitySilbarn(level, fakePlayer(helper));
        // Pose juste au-dessus du sol, immobile : sans gravite elle ne bouge pas d'un
        // millimetre, ce qui rend le test lisible.
        ball.moveTo(abs.getX() + 0.5, abs.getY() + 2.0, abs.getZ() + 0.5, 0f, 0f);
        ball.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

        assertValue(helper, 50, cn.academy.entity.EntitySilbarn.GRAVITY_DELAY,
                "deux secondes et demie de flottement");
        assertClose(helper, 0.12d, cn.academy.entity.EntitySilbarn.GRAVITY, "gravite de l'original");
        assertFalse(helper, ball.isHit(), "elle part non posee");
        assertTrue(helper, ball.isPickable(), "et donc visable");

        // Les cinquante premiers ticks : elle flotte, immobile.
        for (int i = 0; i < cn.academy.entity.EntitySilbarn.GRAVITY_DELAY; i++) {
            ball.tick();
        }
        assertClose(helper, abs.getY() + 2.0, ball.getY(), "aucune gravite avant son heure");
        assertFalse(helper, ball.isHit(), "et toujours pas posee");

        // Ensuite elle tombe : une dizaine de ticks plus tard elle touche le sol.
        int fallen = 0;
        while (!ball.isHit() && fallen < 100) {
            ball.tick();
            fallen++;
        }
        assertTrue(helper, ball.isHit(), "elle doit finir par se poser");
        assertFalse(helper, ball.isPickable(), "une bille posee ne se vise plus");
        // Elle se pose sur le bloc, et non dedans : c'est son BAS qui touche, donc son centre
        // s'arrete un demi-cote au-dessus. L'original, dont la boite montait de ses pieds alors
        // que son modele etait centre, s'enfoncait a moitie dans la terre.
        assertTrue(helper, Math.abs(ball.getY()
                        - (abs.getY() + 1.0 + cn.academy.entity.SilbarnVisuals.HIT_SIZE / 2.0)) < 0.01,
                "et elle doit s'etre posee sur le bloc, pas dedans : y=" + ball.getY());

        // Puis elle disparait dix ticks apres s'etre posee, comme l'original.
        for (int i = 0; i < cn.academy.entity.EntitySilbarn.HIT_LIFETIME; i++) {
            assertFalse(helper, ball.isRemoved(), "encore la au tick " + i + " apres le contact");
            ball.tick();
        }
        assertTrue(helper, ball.isRemoved(), "posee, elle disparait dix ticks plus tard");

        helper.succeed();
    }

    /** L'objet se lance et pose une bille dans le monde. */
    @GameTest(template = "empty")
    public static void lancerLaBilleFaitApparaitreUneBille(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = aboveTestArea(helper, new BlockPos(3, 1, 3), 40);

        var player = ownPlayer(helper, "silbarn_thrower");
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);
        // Ce joueur n'appartient qu'a ce test : en creatif pour ne pas consommer l'objet,
        // et il n'y a personne d'autre a qui le rendre.
        player.getAbilities().instabuild = true;

        var stack = new ItemStack(ModItems.SILBARN.get());
        var before = level.getEntitiesOfClass(cn.academy.entity.EntitySilbarn.class,
                player.getBoundingBox().inflate(8.0));

        var result = ModItems.SILBARN.get().use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND);

        assertValue(helper, net.minecraft.world.InteractionResult.SUCCESS, result.getResult(),
                "le lancer doit aboutir");
        var after = level.getEntitiesOfClass(cn.academy.entity.EntitySilbarn.class,
                player.getBoundingBox().inflate(8.0));
        assertValue(helper, before.size() + 1, after.size(), "une bille de plus dans le monde");
        assertValue(helper, 1, stack.getCount(), "et rien de consomme en creatif");

        for (var ball : after) ball.discard();
        player.getAbilities().instabuild = false;
        helper.succeed();
    }

    /**
     * La boite de la bille, et la visee qu'elle sert.
     *
     * <p>C'est ce que le joueur a demande : viser une bille pour amorcer la salve de rayons
     * demandait de la toucher au centimetre pres, et ce n'etait pas jouable. Le port double donc
     * sa boite de collision — un ecart assume, et le seul du mod qui rende une chose plus facile
     * qu'a l'original.
     *
     * <p>Ce test la mesure, puis verifie que le rayon de visee la trouve <b>vraiment</b> : la
     * bille est posee a trente centimetres du trait, la ou la boite de l'original — vingt
     * centimetres de demi-largeur — l'aurait manquee.
     */
    @GameTest(template = "empty")
    public static void laBilleEstPlusLargeAViserQueDansLeVraiMod(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = aboveTestArea(helper, new BlockPos(3, 1, 3), 50);
        // Vingt-cinq centimetres a cote du trait : au-dela des vingt centimetres de demi-largeur
        // de l'original, en deca des trente du port.
        final double AIM_OFFSET = 0.25;

        var player = ownPlayer(helper, "silbarn_aimer");
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);

        var ball = new cn.academy.entity.EntitySilbarn(level, player);
        // Sur la trajectoire du REGARD, puis decalee lateralement — la ou la boite de l'original
        // (vingt centimetres de demi-largeur) l'aurait manquee, et ou celle du port l'attrape. Le
        // decalage se mesure par rapport au regard et non a l'axe du monde : un faux joueur a un
        // lacet qui derive de quelques degres, et un decalage pose en X se serait ajoute a cette
        // derive (vecu : le rayon passait soixante centimetres a cote).
        var eye = player.getEyePosition(1f);
        var look = player.getViewVector(1f);
        var side = look.cross(new net.minecraft.world.phys.Vec3(0, 1, 0)).normalize();
        var at = eye.add(look.scale(3.0)).add(side.scale(AIM_OFFSET));
        ball.setPos(at.x, at.y, at.z);
        ball.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        level.addFreshEntity(ball);

        assertClose(helper, cn.academy.entity.SilbarnVisuals.HIT_SIZE, ball.getBbWidth(),
                "la boite du port");
        assertTrue(helper, ball.getBbWidth() > 0.4,
                "plus large que les quarante centimetres de l'original, et c'est voulu");

        // Et elle est CENTREE sur la bille, la ou le modele se dessine. Par defaut, la boite d'une
        // entite monte de ses pieds : celle de l'original portait donc quarante centimetres trop
        // haut, et viser ce qu'on voyait ratait le tir — exactement ce que le joueur a decrit.
        var box = ball.getBoundingBox();
        assertClose(helper, ball.getY(), box.getCenter().y,
                "la boite est centree sur la bille, pas posee au-dessus");
        assertClose(helper, ball.getY() - cn.academy.entity.SilbarnVisuals.HIT_SIZE / 2.0, box.minY,
                "son bas est sous la bille");
        assertClose(helper, ball.getY() + cn.academy.entity.SilbarnVisuals.HIT_SIZE / 2.0, box.maxY,
                "et son haut au-dessus");

        // Le regard passe a trente centimetres du centre : dedans pour le port, dehors pour
        // l'original. C'est exactement la difference que le joueur est alle chercher.
        var found = cn.academy.ability.TargetingUtil.findEntityInSight(player,
                cn.academy.ability.meltdowner.RayBarrageSkill.DISPLAY_RANGE);
        assertValue(helper, ball, found, "le regard trouve la bille, meme visee a cote"
                + " (oeil " + player.getEyePosition(1f) + ", regard " + player.getViewVector(1f)
                + ", bille " + ball.position() + ", boite " + ball.getBoundingBox()
                + ", posee " + ball.isHit() + ")");

        ball.discard();
        helper.succeed();
    }
    /**
     * Les deux visages de la salve de rayons.
     *
     * C'est la seule competence du mod qui change de nature selon ce que le regard trouve :
     * un tir simple sur ce qu'elle voit, ou une salve en cone si elle tombe sur une bille
     * de silicium <b>en vol</b>. Les deux se lisent dans le monde, donc ici — et le second
     * verifie du meme coup que la bille est bien consommee par la salve.
     */
    @GameTest(template = "empty")
    public static void laSalveDeRayonsTireOuFaucheSelonCeQuElleTrouve(GameTestHelper helper) {
        var skill = cn.academy.ability.meltdowner.MeltdownerCategory.RAY_BARRAGE;
        ServerLevel level = helper.getLevel();
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), 120);

        var player = ownPlayer(helper, "barrage_thrower");
        // Regard vers +Z, comme le lacet zero de Minecraft, et un peu vers le sol : une
        // vache au sol se vise en baissant les yeux, elle n'est pas a hauteur de tete.
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 10f);

        var data = new cn.academy.ability.AbilityData();
        data.setCategoryLevel(skill.getCategory(), 1);
        data.learnSkill(skill);

        // Le monde et le faux joueur sont partages par tous les tests, qui peuvent tourner
        // en parallele : on nettoie donc le couloir avant de mesurer.
        clearCorridor(helper, abs, 8);

        // --- Sans bille : un tir simple, sur ce que le regard trouve.
        var cow = new net.minecraft.world.entity.animal.Cow(
                net.minecraft.world.entity.EntityType.COW, level);
        cow.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 4.5, 0f, 0f);
        level.addFreshEntity(cow);

        skill.onActivate(player, data);
        assertFalse(helper, cow.isAlive(),
                "sans bille, un tir simple de 25 points doit tuer une vache");

        // --- Avec une bille en vol devant le regard : la salve fauche le cone.
        cow.discard();

        // Le regard se releve <b>avant</b> de poser la bille : elle est placée dans l'axe, donc
        // la rotation doit déjà être celle du tir. Une bille posée dans l'axe d'un regard
        // baissé passerait sous le trait, et le test mesurerait le tir simple en croyant
        // mesurer la salve.
        player.moveTo(player.getX(), player.getY(), player.getZ(), 0f, 0f);

        var ball = new cn.academy.entity.EntitySilbarn(level, player);
        // Un dixième sous l'œil : une bille qu'on vient de lancer vole exactement là, et le
        // test ne dépend alors pas de l'orientation du faux joueur — sur laquelle il ne faut
        // pas compter (voir clearCorridor).
        net.minecraft.world.phys.Vec3 look = player.getViewVector(1f);
        net.minecraft.world.phys.Vec3 ballAt = player.getEyePosition(1f)
                .add(look.scale(3.0)).add(0, -0.1, 0);
        ball.moveTo(ballAt.x, ballAt.y, ballAt.z, 0f, 0f);
        ball.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        level.addFreshEntity(ball);

        var target = new net.minecraft.world.entity.animal.Cow(
                net.minecraft.world.entity.EntityType.COW, level);
        target.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 6.5, 0f, 0f);
        level.addFreshEntity(target);

        // Ce que le regard trouve vraiment : c'est ce qui decide de la branche prise, et le
        // dire evite de chercher ailleurs quand un decor etranger s'est glisse dans le coin.
        assertTrue(helper,
                cn.academy.ability.TargetingUtil.findEntityInSight(player, 20.0) == ball,
                "le regard doit trouver la bille, et rien d'autre : "
                        + cn.academy.ability.TargetingUtil.findEntityInSight(player, 20.0));

        // Le cone ne demande pas que le regard touche la cible : il part de l'oeil du
        // lanceur et s'ouvre sur 55 degres, donc une bete au sol y est meme si le trait
        // passe au-dessus d'elle.
        float before = target.getHealth();
        skill.onActivate(player, data);

        assertTrue(helper, ball.isHit(), "la bille trouvee doit exploser");
        assertTrue(helper, target.getHealth() < before,
                "et la salve doit faucher ce qui est dans le cone");
        // Une vache a dix points de vie : la salve au minimum en fait exactement dix, donc
        // elle y passe. C'est ce qui rend l'assertion precise — une salve qui ne ferait
        // que cinq points la laisserait vivante.
        assertFalse(helper, target.isAlive(),
                "10 points a l'experience minimale, sur une bete qui en a 10");

        ball.discard();
        target.discard();
        helper.succeed();
    }

    /**
     * Ou le scintillement fait atterrir.
     *
     * Le saut de douze blocs et la direction se lisent en test unitaire ; ce qui ne se lit
     * qu'avec un monde, c'est <b>l'atterrissage</b> : le trajet s'arrete sur la premiere face
     * et le point d'arrivee se pose dessus. C'est aussi la partie qui decide si le joueur
     * ressort devant un mur ou dedans.
     *
     * Le mur est pose a deux blocs du joueur, et pas plus loin : au-dela de son propre
     * carre de terrain, un test n'est plus seul — les cellules des structures voisines se
     * touchent presque, et une bete laissee la par un autre test arreterait le saut avant
     * nous. Tout ce qui se mesure ici tient donc dans les deux blocs qui sont les notres.
     */
    @GameTest(template = "empty")
    public static void leScintillementAtterritSurCeQuIlTrouve(GameTestHelper helper) {
        var flashing = cn.academy.ability.teleporter.TeleporterCategory.FLASHING;
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos rel = new BlockPos(2, 1, 3);

        var player = ownPlayer(helper, "flasher");
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);

        clearCorridor(helper, abs, 4);

        var data = new cn.academy.ability.AbilityData();
        data.setCategoryLevel(flashing.getCategory(), 1);
        data.learnSkill(flashing);

        // --- Un mur deux blocs devant : on s'arrete sur sa face, pas dedans.
        for (int y = 0; y < 3; y++) {
            helper.setBlock(rel.offset(0, y, 0), net.minecraft.world.level.block.Blocks.STONE);
        }
        BlockPos wall = helper.absolutePos(rel);

        net.minecraft.world.phys.Vec3 onWall = flashing.destination(player, data,
                cn.academy.ability.teleporter.FlashingSkill.FORWARD);

        assertTrue(helper, Math.abs(onWall.z - (wall.getZ() - 0.6)) < 0.01,
                "soixante centimetres devant la face nord : z=" + onWall.z
                        + " pour un mur a " + wall.getZ());
        assertTrue(helper, Math.abs(onWall.y - (wall.getY() + 1.7)) < 0.01,
                "et a hauteur de tete du bloc vise : y=" + onWall.y);

        // --- Le saut, lui, part bien de la ou le joueur regarde : en se retournant, la meme
        // direction vise l'autre bout.
        player.moveTo(player.getX(), player.getY(), player.getZ(), 180f, 0f);
        player.setYHeadRot(180f);
        net.minecraft.world.phys.Vec3 behind = flashing.destination(player, data,
                cn.academy.ability.teleporter.FlashingSkill.FORWARD);
        assertTrue(helper, behind.z < abs.getZ(),
                "dos au mur, avancer c'est reculer : z=" + behind.z);

        helper.succeed();
    }

    /**
     * La teleportation a la marque, et ceux qu'elle emmene.
     *
     * C'est la seule competence du port qui deplace un <b>groupe</b>, et c'est ce qui se lit
     * le moins bien en test unitaire : chacun garde son ecart avec le lanceur, donc le groupe
     * arrive en formation et pas empile. La marque, elle, se pose et se relit comme le reste
     * de la donnee du joueur.
     */
    @GameTest(template = "empty")
    public static void laTeleportationEmporteLaCompagnie(GameTestHelper helper) {
        var skill = cn.academy.ability.teleporter.TeleporterCategory.LOCATION_TELEPORT;
        ServerLevel level = helper.getLevel();
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), 160);

        var player = ownPlayer(helper, "traveller");
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);

        clearCorridor(helper, abs, 4);

        var data = new cn.academy.ability.AbilityData();
        data.setCategoryLevel(skill.getCategory(), 1);
        data.learnSkill(skill);
        // Assez d'experience pour que le prix soit au plus bas, et de la reserve pour payer.
        data.addSkillExp(skill, 1f);

        // Le compagnon et la marque sont dans le carre de terrain du test : au-dela, on est
        // dans la pierre, et le serveur repousse le joueur hors du bloc ou il a atterri.
        // Un compagnon juste a cote, et une marque douze blocs plus loin : le groupe part
        // avec son ecart, donc les deux doivent se retrouver decales de la meme distance.
        var chicken = new net.minecraft.world.entity.animal.Chicken(
                net.minecraft.world.entity.EntityType.CHICKEN, level);
        chicken.moveTo(abs.getX() + 2.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);
        level.addFreshEntity(chicken);

        double targetX = abs.getX() + 12.5;
        data.addMark("au loin", cn.academy.ability.teleporter.LocationMark.of(level),
                targetX, abs.getY(), abs.getZ() + 0.5);
        assertValue(helper, 1, data.getMarks().size(), "la marque doit etre posee");

        double offset = chicken.getX() - player.getX();
        int reserveBefore = (int) data.getControlPoint();
        assertTrue(helper, skill.perform(player, data, 0), "le saut doit aboutir");

        // Le faux joueur du framework refuse d'etre deplace — Forge neutralise la connexion
        // de son `FakePlayer`, et `teleportTo` ne fait rien sur lui. C'est donc le compagnon
        // qui montre ou le groupe est arrive : il garde son ecart avec le lanceur, donc s'il
        // est a la marque plus cet ecart, c'est que le saut a bien eu lieu, formation
        // comprise. C'est aussi pourquoi les autres competences de teleportation du port
        // n'ont pas de GameTest : on ne peut y observer que ceux qu'elles emmenent.
        assertTrue(helper, Math.abs(chicken.getX() - (targetX + offset)) < 0.01,
                "le compagnon doit arriver a la marque, decale de son ecart : x=" + chicken.getX()
                        + " pour une marque a " + targetX + " et un ecart de " + offset);
        assertTrue(helper, Math.abs(chicken.getY() - abs.getY()) < 0.01,
                "et a la hauteur de la marque : y=" + chicken.getY());
        assertTrue(helper, chicken.isAlive(), "la compagnie arrive entiere");

        // Le prix du voyage, sa recharge et son experience.
        assertTrue(helper, data.getControlPoint() < reserveBefore,
                "le saut se paie sur la reserve : " + data.getControlPoint() + " contre " + reserveBefore);
        assertValue(helper, skill.cooldown(data), data.getCooldown(skill),
                "et il pose sa recharge lui-meme, avec ce qu'il a porte");
        assertTrue(helper, data.getSkillExp(skill) > 0.9f, "un saut verse son experience");

        chicken.discard();

        // Une marque d'une AUTRE dimension : accessible, mais le serveur peut ne pas connaitre
        // ce monde. C'est le cas d'une marque posee sur un monde qu'un mod a emporte — le nom
        // est tout ce qu'une marque retient, donc il faut le lui demander avant d'y aller.
        assertTrue(helper, skill.canCrossDimension(data),
                "a pleine experience, on peut traverser une dimension");
        data.addMark("nulle part", "academy:nulle_part", targetX, abs.getY(), abs.getZ() + 0.5);
        int reserveAvant = (int) data.getControlPoint();
        assertFalse(helper, skill.perform(player, data, 1),
                "une dimension que le serveur ne connait pas ne s'atteint pas");
        assertValue(helper, reserveAvant, (int) data.getControlPoint(),
                "et un refus ne se paie pas");

        var teleport = cn.academy.ability.teleporter.LocationTeleportSkill.class;
        assertTrue(helper,
                cn.academy.ability.teleporter.LocationTeleportSkill.dimensionOf(player,
                        data.getMark(1)) == null,
                "le serveur ne connait pas cette dimension-la");
        assertTrue(helper,
                cn.academy.ability.teleporter.LocationTeleportSkill.dimensionOf(player,
                        data.getMark(0)) != null,
                "mais il connait la sienne, celle ou la marque a ete posee");
        // Et celles du jeu : un serveur en charge trois, et le nether en fait partie. C'est
        // la preuve que `dimensionOf` sait lire un nom de dimension du jeu, pas seulement le
        // sien — c'est-a-dire que la traversee a de quoi aboutir.
        assertTrue(helper,
                cn.academy.ability.teleporter.LocationTeleportSkill.dimensionOf(player,
                        new cn.academy.ability.teleporter.LocationMark("nether", "minecraft:the_nether",
                                0, 64, 0)) != null,
                "le nether doit exister sur un serveur en charge");

        helper.succeed();
    }

    /**
     * Le choc dirige : ce qu'un poing ferme fait a ce qu'il trouve, et ce qu'il coute.
     *
     * Le JUnit fige les courbes et la poussee a partir des deux points de visee ; ce qui ne
     * se lit qu'avec un monde, c'est le <b>rayon</b> — est-ce que le coup trouve vraiment la
     * bete posee devant, et rien d'autre — et le fait que la recharge ne se pose qu'au
     * contact. Le seuil d'un quart d'experience, lui, se voit tres bien : sous le seuil le
     * coup ne fait que bousculer la victime, au-dessus il la souleve et l'envoie valser.
     */
    @GameTest(template = "empty")
    public static void leChocDirigeNeFaitAttendreQueSilTouche(GameTestHelper helper) {
        var dirShock = cn.academy.ability.vecmanip.VecmanipCategory.DIRECTED_SHOCK;
        ServerLevel level = helper.getLevel();
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), 200);

        var player = ownPlayer(helper, "shocker");
        // Regard vers +Z (le lacet zero de Minecraft) et un peu vers le sol : une vache au
        // sol se vise en baissant les yeux, elle n'est pas a hauteur de tete.
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 10f);

        var data = new cn.academy.ability.AbilityData();
        data.setCategoryLevel(dirShock.getCategory(), 1);
        data.learnSkill(dirShock);

        clearCorridor(helper, abs, 6);

        // --- Un poing dans le vide : les deux ressources partent, et rien d'autre.
        assertTrue(helper, data.perform(dirShock.getCpCost(data), dirShock.getOverloadCost(data)),
                "un coup dans le vide se paie quand meme");
        dirShock.onActivateCharged(player, data, 10);

        assertValue(helper, 0, data.getCooldown(dirShock),
                "ne rien toucher ne fait attendre personne : la recharge n'est posee qu'au contact");
        assertClose(helper, 0.0010d, data.getSkillExp(dirShock),
                "et le geste ne rapporte que l'experience du coup dans le vide");

        // --- Sans un quart d'experience, le coup ne fait que bousculer.
        //
        // Une vache ici : elle a dix points de vie, et le coup en fait sept a l'experience
        // minimale — elle survit donc, de justesse, a ce qu'on veut mesurer.
        var nudged = new net.minecraft.world.entity.animal.Cow(
                net.minecraft.world.entity.EntityType.COW, level);
        nudged.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 2.5, 0f, 0f);
        level.addFreshEntity(nudged);

        // Ce que le regard trouve vraiment : c'est le rayon de trois blocs qui est en jeu ici.
        assertTrue(helper, cn.academy.ability.TargetingUtil.findEntityInSight(player, 3.0) == nudged,
                "le regard doit trouver la bete a deux blocs et demie, et rien d'autre : "
                        + cn.academy.ability.TargetingUtil.findEntityInSight(player, 3.0));

        float healthBefore = nudged.getHealth();
        dirShock.onActivateCharged(player, data, 10);

        assertTrue(helper, Math.abs((healthBefore - 7f) - nudged.getHealth()) < 0.02,
                "7 points au depart de la courbe, trouve " + (healthBefore - nudged.getHealth()));
        // Sous le seuil, la poussee ne s'applique pas : il ne reste que la bousculade d'un
        // vingt-quatrieme de bloc, horizontale. C'est la que se voit le seuil d'experience.
        assertTrue(helper, nudged.getDeltaMovement().z > 0.2,
                "la bete est bousculee vers l'avant : " + nudged.getDeltaMovement());
        assertClose(helper, 0d, nudged.getDeltaMovement().y,
                "mais elle ne quitte pas le sol");
        assertValue(helper, dirShock.cooldown(data), data.getCooldown(dirShock),
                "un coup qui touche fait attendre, lui");

        nudged.discard();

        // --- Passe le seuil, la meme chose mais la victime s'envole.
        //
        // Un zombie et non une vache : a six dixiemes d'experience le coup fait 11,8
        // points, et une vache n'en a que dix — elle mourrait, et le test ne pourrait
        // plus mesurer ses degats. Le zombie en a vingt, donc il survit a l'experience
        // meme ou le coup fait le plus mal.
        var volunteer = new net.minecraft.world.entity.monster.Zombie(
                net.minecraft.world.entity.EntityType.ZOMBIE, level);
        volunteer.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 2.5, 0f, 0f);
        level.addFreshEntity(volunteer);

        // Ce que le regard trouve, encore une fois : la vache ecartee ne doit plus etre
        // dans le couloir, sans quoi c'est elle qui prendrait le coup.
        assertTrue(helper, cn.academy.ability.TargetingUtil.findEntityInSight(player, 3.0) == volunteer,
                "le regard doit trouver le zombie, et plus la vache ecartee : "
                        + cn.academy.ability.TargetingUtil.findEntityInSight(player, 3.0));

        var experienced = new cn.academy.ability.AbilityData();
        experienced.setCategoryLevel(dirShock.getCategory(), 1);
        experienced.learnSkill(dirShock);
        experienced.addSkillExp(dirShock, 0.6f);

        healthBefore = volunteer.getHealth();
        dirShock.onActivateCharged(player, experienced, 10);

        assertTrue(helper, Math.abs(11.8d - (healthBefore - volunteer.getHealth())) < 0.02,
                "11,8 points a six dixiemes d'experience, trouve "
                        + (healthBefore - volunteer.getHealth()));
        assertTrue(helper, volunteer.getDeltaMovement().z > 0.5,
                "la bete part en arriere, et bien plus loin qu'une simple bousculade : "
                        + volunteer.getDeltaMovement());
        assertTrue(helper, volunteer.getDeltaMovement().y > 0.3,
                "et la poussee la souleve : " + volunteer.getDeltaMovement());

        volunteer.discard();
        helper.succeed();
    }

    /**
     * L'onde de choc : ce qu'elle fait au sol devant elle, et a ce qui s'y trouve.
     *
     * <p>Le JUnit fige ses courbes, son axe et ses cinq colonnes ; ce qui ne se lit qu'avec
     * un monde, c'est la <b>marche</b> — est-ce que le sol est vraiment ecrase, est-ce que
     * les trois blocs au-dessus du marcheur sont vraiment casses, et est-ce qu'un corps
     * dans le rang est vraiment touche puis souleve.
     *
     * <p>Le sol est bati ici, et pas seulement parce que l'onde en a besoin : a cette
     * altitude le monde est de la pierre pleine, donc une bande d'un bloc de large et de
     * l'air au-dessus donnent un couloir <b>previsible</b>, ou seuls le bloc du marcheur et
     * sa colonne comptent. Le terrain alentour, lui, reste de la pierre, que l'onde attaque
     * aussi des qu'elle sort du couloir — c'est pourquoi toutes les verifications portent
     * sur les quatre premiers pas.
     */
    @GameTest(template = "empty")
    public static void lOndeDeChocEffondreLeSolDevantElle(GameTestHelper helper) {
        var shock = cn.academy.ability.vecmanip.VecmanipCategory.GROUNDSHOCK;
        ServerLevel level = helper.getLevel();
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), 240);

        // La hauteur du test s'ajoute a la position <b>relative</b> du sol : `aboveTestArea`
        // deplace l'absolu, alors que `setBlock` et `getBlockState` comptent depuis la
        // structure. `abs` est a la relative 1 + 240, et le marcheur part du bloc qui est
        // sous les pieds — donc de la relative 240. Se tromper d'un bloc ici ne casse pas
        // le test : il le rend faux, en le faisant travailler un bloc au-dessus du sol.
        int base = 240;

        var player = ownPlayer(helper, "quaker");
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);
        // Le sol d'abord : un faux joueur pose dans le vide n'est pas « au sol » tant que
        // rien ne l'y a fait tomber, et l'onde ne partirait pas du tout.
        player.setOnGround(true);

        var data = new cn.academy.ability.AbilityData();
        data.setCategoryLevel(shock.getCategory(), 1);
        data.learnSkill(shock);

        clearCorridor(helper, abs, 14);
        buildWalkingFloor(helper, 13, base);

        // Un cube de terre dans le couloir, au-dessus du marcheur : c'est la que se voit la
        // casse des trois blocs de la colonne, qui ne depend d'aucun tirage.
        helper.setBlock(new BlockPos(2, base + 1, 5),
                net.minecraft.world.level.block.Blocks.DIRT);

        // Une vache sur le sol, quatre blocs devant : le marcheur la trouvera en passant.
        var cow = new net.minecraft.world.entity.animal.Cow(
                net.minecraft.world.entity.EntityType.COW, level);
        cow.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 4.5, 0f, 0f);
        level.addFreshEntity(cow);

        double reserveBefore = data.getControlPoint();
        float healthBefore = cow.getHealth();
        assertTrue(helper, player.onGround(), "le faux joueur doit etre pose au sol");
        assertTrue(helper, reserveBefore >= shock.consumption(data),
                "assez de reserve pour l'onde : " + reserveBefore + " pour " + shock.consumption(data));
        shock.onActivateCharged(player, data, 10);

        assertTrue(helper, data.getControlPoint() < reserveBefore,
                "le prix doit avoir ete paye : " + data.getControlPoint() + " contre " + reserveBefore);

        // Le sol a ete ecrase : la pierre est devenue de la pierre taillee, ou a disparu
        // quand le tirage de la casse est tombe (trois chances sur dix par bloc).
        var firstStep = helper.getBlockState(new BlockPos(2, base, 3));
        assertFalse(helper, firstStep.is(net.minecraft.world.level.block.Blocks.STONE),
                "le premier pas doit avoir ecrase le sol, trouve " + firstStep);
        var flattened = helper.getBlockState(new BlockPos(2, base, 4));
        assertFalse(helper, flattened.is(net.minecraft.world.level.block.Blocks.STONE),
                "la pierre du couloir doit avoir ete ecrasee, trouve " + flattened);
        assertTrue(helper, flattened.is(net.minecraft.world.level.block.Blocks.COBBLESTONE)
                        || flattened.isAir(),
                "en pierre taillee, ou en air si elle a ete cassee : " + flattened);

        // La colonne de trois blocs, elle, est cassee sans tirage.
        assertTrue(helper, helper.getBlockState(new BlockPos(2, base + 1, 5)).isAir(),
                "le cube de terre au-dessus du marcheur doit avoir ete casse : "
                        + helper.getBlockState(new BlockPos(2, base + 1, 5)));

        // La vache : 4 points au depart de la courbe, et soulevee.
        //
        // Pas de verification sur la poussee horizontale : le coup de degats lui-meme en
        // donne une (la 1.20.1 recule tout ce qui est frappe de 0,4 dans l'axe de
        // l'attaquant), et l'original faisait comme le port — il ne posait que la
        // composante verticale, en laissant celle-la. C'est bien la montee qui vient de
        // l'onde, et c'est elle qui se verifie.
        assertTrue(helper, Math.abs((healthBefore - 4f) - cow.getHealth()) < 0.02,
                "4 points au depart de la courbe, trouve " + (healthBefore - cow.getHealth()));
        assertTrue(helper, cow.getDeltaMovement().y > 0.4,
                "la bete est soulevee : " + cow.getDeltaMovement());

        // L'experience : 0,001 pour le coup et 0,002 pour la bete, une seule fois.
        assertClose(helper, 0.003d, data.getSkillExp(shock),
                "un corps touche rapporte 0,002, et le coup 0,001");
        assertValue(helper, shock.cooldown(data), data.getCooldown(shock),
                "et l'onde pose sa recharge elle-meme");

        // La marche ne ramasse rien : l'original ne laissait des butins qu'a sa passe finale.
        assertValue(helper, 0, itemsAround(helper, abs, 14).size(),
                "aucun butin ne doit tomber pendant la marche");

        cow.discard();
        helper.succeed();
    }

    /** L'onde ne part pas d'un saut : en l'air, rien du tout, et rien de facture. */
    @GameTest(template = "empty")
    public static void lOndeDeChocNePartPasDeLAir(GameTestHelper helper) {
        var shock = cn.academy.ability.vecmanip.VecmanipCategory.GROUNDSHOCK;
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), 280);
        int base = 280;

        var player = ownPlayer(helper, "jumper");
        // Trois blocs au-dessus du sol, et pas au sol : c'est tout le sujet du test.
        player.moveTo(abs.getX() + 0.5, abs.getY() + 3, abs.getZ() + 0.5, 0f, 0f);
        player.setOnGround(false);

        var data = new cn.academy.ability.AbilityData();
        data.setCategoryLevel(shock.getCategory(), 1);
        data.learnSkill(shock);

        clearCorridor(helper, abs, 14);
        buildWalkingFloor(helper, 5, base);

        double reserveBefore = data.getControlPoint();
        float overloadBefore = data.getOverload();
        shock.onActivateCharged(player, data, 10);

        assertFalse(helper, player.onGround(), "le joueur doit etre en l'air");
        assertTrue(helper, helper.getBlockState(new BlockPos(2, base, 4))
                        .is(net.minecraft.world.level.block.Blocks.STONE),
                "le sol ne doit pas avoir bouge d'un bloc");
        assertValue(helper, 0, data.getCooldown(shock),
                "et rien ne doit avoir ete pose en recharge");
        assertClose(helper, 0d, data.getSkillExp(shock), "ni d'experience versee");
        assertClose(helper, reserveBefore, data.getControlPoint(),
                "ni un point de reserve depense");
        // Le surcout non plus : c'est tout l'objet de `Skill#paysOnEffect`, sans lequel le
        // paquet paierait le prix avant que l'onde ait pu constater qu'elle ne part pas.
        assertClose(helper, overloadBefore, data.getOverload(),
                "ni un point de surcout charge");
        helper.succeed();
    }

    /**
     * A pleine experience, l'onde ramasse : la passe finale casse le sol meuble du carre et
     * le laisse tomber.
     *
     * <p>Le cube de terre est pose <b>derriere</b> le joueur : la marche part devant et ne
     * le touchera donc jamais, alors que le carre de la passe finale l'englobe. C'est ce qui
     * separe les deux passes du test precedent.
     */
    @GameTest(template = "empty")
    public static void lOndeDeChocRamasseLeSolMeubleALaMaitrise(GameTestHelper helper) {
        var shock = cn.academy.ability.vecmanip.VecmanipCategory.GROUNDSHOCK;
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), 320);
        int base = 320;

        var player = ownPlayer(helper, "master");
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);
        player.setOnGround(true);

        var data = new cn.academy.ability.AbilityData();
        data.setCategoryLevel(shock.getCategory(), 1);
        data.learnSkill(shock);
        data.addSkillExp(shock, 1f);

        clearCorridor(helper, abs, 16);
        buildWalkingFloor(helper, 5, base);
        // De la terre grasse, molle, derriere le joueur — et de la pierre dure devant lui,
        // qui doit survivre a la passe finale.
        helper.setBlock(new BlockPos(2, base, 1),
                net.minecraft.world.level.block.Blocks.COARSE_DIRT);

        shock.onActivateCharged(player, data, 10);

        assertTrue(helper, helper.getBlockState(new BlockPos(2, base, 1)).isAir(),
                "la terre derriere le joueur doit avoir ete cassee par la passe finale : "
                        + helper.getBlockState(new BlockPos(2, base, 1)));
        // Le sol bati a la colonne du joueur, lui, n'est jamais marche : il sert de
        // temoin pour la pierre, que la passe finale ne doit pas toucher.
        assertTrue(helper, helper.getBlockState(new BlockPos(2, base, 2))
                        .is(net.minecraft.world.level.block.Blocks.STONE),
                "la pierre, elle, est trop dure pour la passe finale : "
                        + helper.getBlockState(new BlockPos(2, base, 2)));

        var items = itemsAround(helper, abs, 16);
        assertTrue(helper, items.stream().anyMatch(item -> item.getItem().is(
                        net.minecraft.world.item.Items.COARSE_DIRT)),
                "et la passe finale, elle, laisse tomber le butin : " + items);

        helper.succeed();
    }

    /**
     * L'onde de choc dirigee : ce qu'elle fait de ce qu'elle trouve devant elle.
     *
     * <p>Le JUnit fige ses courbes, son cube et ses paliers de durete ; ce qui ne se lit
     * qu'avec un monde, c'est le <b>rayon</b> qui choisit le centre, ce qu'il devient une
     * fois choisi, et surtout qui il projette. Une obsidienne sert de temoin : a 50 de
     * durete elle resiste a une onde de faible experience, ce qui prouve du meme coup que
     * le rayon l'a bien trouvee — c'est elle le centre, et non un caillou voisin.
     */
    @GameTest(template = "empty")
    public static void lOndeDirigeeProjetteAutourDeSaCible(GameTestHelper helper) {
        var blast = cn.academy.ability.vecmanip.VecmanipCategory.DIRECTED_BLASTWAVE;
        ServerLevel level = helper.getLevel();
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), 360);
        int base = 360;

        var player = ownPlayer(helper, "blaster");
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);

        var data = new cn.academy.ability.AbilityData();
        data.setCategoryLevel(blast.getCategory(), 1);
        data.learnSkill(blast);

        clearCorridor(helper, abs, 8);

        // Le couloir du rayon, puis le mur : a trois blocs, l'obsidienne est ce que le
        // regard trouve, donc le centre de l'onde.
        for (int dz = 2; dz <= 4; dz++) {
            helper.setBlock(new BlockPos(2, base + 2, dz),
                    net.minecraft.world.level.block.Blocks.AIR);
        }
        helper.setBlock(new BlockPos(2, base + 2, 5),
                net.minecraft.world.level.block.Blocks.OBSIDIAN);

        var zombie = new net.minecraft.world.entity.monster.Zombie(
                net.minecraft.world.entity.EntityType.ZOMBIE, level);
        zombie.moveTo(abs.getX() + 2.5, abs.getY(), abs.getZ() + 3.5, 0f, 0f);
        level.addFreshEntity(zombie);

        // Un objet au sol, dans le cube : l'onde de l'original projetait tout ce qu'elle
        // trouvait, pas seulement ce qui vit.
        var loose = new net.minecraft.world.entity.item.ItemEntity(level,
                abs.getX() - 1.5, abs.getY(), abs.getZ() + 3.5,
                new ItemStack(net.minecraft.world.item.Items.STONE, 3));
        loose.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        level.addFreshEntity(loose);

        double reserveBefore = data.getControlPoint();
        float healthBefore = zombie.getHealth();
        // Le prix se paie a l'activation, comme le fait le paquet : la competence, elle, ne
        // s'occupe que de son effet.
        assertTrue(helper, data.perform(blast.getCpCost(data), blast.getOverloadCost(data)),
                "la reserve doit suffire au coup");
        blast.onActivateCharged(player, data, 10);

        // L'obsidienne tient : a faible experience, l'onde ne brise que jusqu'a 2,9.
        assertTrue(helper, helper.getBlockState(new BlockPos(2, base + 2, 5))
                        .is(net.minecraft.world.level.block.Blocks.OBSIDIAN),
                "une onde de faible experience ne doit pas entamer l'obsidienne : "
                        + helper.getBlockState(new BlockPos(2, base + 2, 5)));

        // Le zombie : 10 points, et il part en arriere et en l'air.
        assertTrue(helper, Math.abs((healthBefore - 10f) - zombie.getHealth()) < 0.02,
                "10 points au depart de la courbe, trouve " + (healthBefore - zombie.getHealth()));
        assertTrue(helper, zombie.getDeltaMovement().z > 0.5,
                "la bete part en arriere : " + zombie.getDeltaMovement());
        assertTrue(helper, zombie.getDeltaMovement().y > 0.3,
                "et la poussee la souleve : " + zombie.getDeltaMovement());
        assertTrue(helper, zombie.getY() > abs.getY(),
                "le dixieme de bloc qui la decolle du sol vaut bien 0,1 : " + zombie.getY());

        // L'objet, lui, ne peut pas etre blesse — mais il part comme le reste.
        assertTrue(helper, loose.getDeltaMovement().length() > 0.5,
                "un objet au sol est projete aussi : " + loose.getDeltaMovement());
        assertTrue(helper, loose.isAlive(), "et il n'est pas blesse : il ne peut pas l'etre");

        // L'experience : 0,0025 quand la vague a trouve quelqu'un, une seule fois.
        assertClose(helper, 0.0025d, data.getSkillExp(blast),
                "deux corps projetes valent le meme gain qu'un seul");
        assertTrue(helper, data.getControlPoint() < reserveBefore,
                "et le coup se paie : " + data.getControlPoint() + " contre " + reserveBefore);
        // Pas de verification de recharge ici : c'est le paquet qui la pose (voir
        // `getCooldownTicks`), et le test unitaire la fige. Le declenchement direct, lui,
        // ne la pose pas — l'onde dirigee n'a pas a la porter elle-meme, son cout etant
        // paye a l'activation quoi qu'il arrive.

        zombie.discard();
        loose.discard();
        helper.succeed();
    }

    /**
     * A la maitrise, l'onde ramasse : le bloc casse laisse son propre objet.
     *
     * <p>Deuxieme phase : un regard qui ne trouve rien. Le centre se pose alors au bout du
     * regard, a quatre blocs, et un bloc pose <b>a cote</b> de ce point — donc hors du rayon
     * lui-meme — doit y passer. C'est le seul moyen de voir ce repli depuis le jeu.
     */
    @GameTest(template = "empty")
    public static void lOndeDirigeeRamasseALaMaitrise(GameTestHelper helper) {
        var blast = cn.academy.ability.vecmanip.VecmanipCategory.DIRECTED_BLASTWAVE;
        // 370 et non 400 : la structure de test part de y = -60, et le monde s'arrete a
        // 320. Plus haut, `setBlock` ne fait rien et `getBlockState` rend de l'air — le
        // test passe alors sans rien tester, ce qui est pire qu'un echec.
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), 370);
        int base = 370;

        var player = ownPlayer(helper, "master_blaster");
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);

        var data = new cn.academy.ability.AbilityData();
        data.setCategoryLevel(blast.getCategory(), 1);
        data.learnSkill(blast);
        data.addSkillExp(blast, 1f);

        clearCorridor(helper, abs, 8);

        for (int dz = 2; dz <= 4; dz++) {
            helper.setBlock(new BlockPos(2, base + 2, dz),
                    net.minecraft.world.level.block.Blocks.AIR);
        }
        helper.setBlock(new BlockPos(2, base + 2, 5),
                net.minecraft.world.level.block.Blocks.OBSIDIAN);

        blast.onActivateCharged(player, data, 10);

        assertTrue(helper, helper.getBlockState(new BlockPos(2, base + 2, 5)).isAir(),
                "a pleine experience, l'obsidienne y passe : "
                        + helper.getBlockState(new BlockPos(2, base + 2, 5)));
        assertTrue(helper, itemsAround(helper, abs, 8).stream()
                        .anyMatch(item -> item.getItem().is(net.minecraft.world.item.Items.OBSIDIAN)),
                "et elle laisse son propre objet, sans tirage : " + itemsAround(helper, abs, 8));

        // --- Regard dans le vide : le centre se pose a quatre blocs, au bout du regard.
        for (var leftover : itemsAround(helper, abs, 8)) {
            leftover.discard();
        }
        for (int dy = 2; dy <= 6; dy++) {
            helper.setBlock(new BlockPos(2, base + dy, 2),
                    net.minecraft.world.level.block.Blocks.AIR);
        }
        // Le centre tombe sur le coin du bloc (3, base + 5, 3) : un bloc pose la y passe,
        // alors que le rayon, lui, monte le long de la colonne voisine.
        helper.setBlock(new BlockPos(3, base + 5, 3),
                net.minecraft.world.level.block.Blocks.STONE);

        var empty = new cn.academy.ability.AbilityData();
        empty.setCategoryLevel(blast.getCategory(), 1);
        empty.learnSkill(blast);
        player.moveTo(player.getX(), player.getY(), player.getZ(), 0f, -90f);
        blast.onActivateCharged(player, empty, 10);

        assertTrue(helper, helper.getBlockState(new BlockPos(3, base + 5, 3)).isAir(),
                "sans cible, le centre se pose au bout du regard — et le bloc pose a cote y passe");
        assertClose(helper, 0.0012d, empty.getSkillExp(blast),
                "une onde qui ne trouve personne ne rapporte que 0,0012");
        helper.succeed();
    }

    /**
     * Le retour de sang : un contact de deux blocs, et rien du tout sans contact.
     *
     * <p>C'est le test de {@code Skill#paysOnEffect}. Le prix du contact se decide dans son
     * effet — pas au paquet — parce qu'il depend de ce que la main trouve : sans cible,
     * <b>rien</b> n'est facture, ni reserve, ni surcout, ni recharge, ni experience. Un
     * goulem de fer sert de victime : 100 points de vie, donc il survit aux 30 degats et le
     * test peut les chiffrer.
     */
    @GameTest(template = "empty")
    public static void leRetourDeSangNeSePaieQueSilTouche(GameTestHelper helper) {
        var blood = cn.academy.ability.vecmanip.VecmanipCategory.BLOOD_RETROGRADE;
        ServerLevel level = helper.getLevel();
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), 350);

        var player = ownPlayer(helper, "bloodletter");
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);

        var data = new cn.academy.ability.AbilityData();
        data.setCategoryLevel(blood.getCategory(), 1);
        data.learnSkill(blood);

        clearCorridor(helper, abs, 8);

        var golem = new net.minecraft.world.entity.animal.IronGolem(
                net.minecraft.world.entity.EntityType.IRON_GOLEM, level);
        golem.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 2.0, 0f, 0f);
        level.addFreshEntity(golem);

        assertTrue(helper, blood.touch(player) == golem,
                "la main doit trouver le goulem a un bloc et demi : " + blood.touch(player));

        double reserveBefore = data.getControlPoint();
        float overloadBefore = data.getOverload();
        float healthBefore = golem.getHealth();
        blood.onActivateCharged(player, data, 5);

        assertTrue(helper, Math.abs((healthBefore - 30f) - golem.getHealth()) < 0.02,
                "30 points au depart de la courbe, trouve " + (healthBefore - golem.getHealth()));
        assertClose(helper, 0.002d, data.getSkillExp(blood), "un contact verse son experience");
        assertValue(helper, blood.cooldown(data), data.getCooldown(blood),
                "et il pose sa recharge lui-meme");
        assertTrue(helper, data.getControlPoint() < reserveBefore,
                "le contact se paie : " + data.getControlPoint() + " contre " + reserveBefore);
        assertTrue(helper, data.getOverload() > overloadBefore,
                "et il charge la reserve de surcout : " + data.getOverload()
                        + " contre " + overloadBefore);

        // --- Rien sous la main : la main ne touche plus rien du tout.
        golem.moveTo(abs.getX() + 20.0, abs.getY(), abs.getZ(), 0f, 0f);
        var empty = new cn.academy.ability.AbilityData();
        empty.setCategoryLevel(blood.getCategory(), 1);
        empty.learnSkill(blood);

        double emptyReserve = empty.getControlPoint();
        float emptyOverload = empty.getOverload();
        assertTrue(helper, blood.touch(player) == null,
                "sans cible, la main ne doit rien trouver : " + blood.touch(player));
        blood.onActivateCharged(player, empty, 5);

        assertClose(helper, 0d, empty.getSkillExp(blood), "sans contact, pas d'experience");
        assertValue(helper, 0, empty.getCooldown(blood), "ni de recharge");
        assertClose(helper, emptyReserve, empty.getControlPoint(), "ni de reserve depensee");
        assertClose(helper, emptyOverload, empty.getOverload(), "ni de surcout charge");

        golem.discard();
        helper.succeed();
    }

    /**
     * Ce que vecmanip accepte de toucher, et a quel prix.
     *
     * <p>La classification demande les registres du jeu — donc un GameTest — et c'est elle qui
     * decide de tout : ce qui est arrete, ce qui est ignore, et ce que chaque prise coute. Elle
     * se lit dans les registres, jamais en dur, et une entree de config mal ecrite ne doit pas
     * la faire tomber.
     *
     * <p>Le cas de la potion est le plus interessant : sa difficulte de 1,4 ne peut venir que
     * de la <b>deuxieme</b> entree de la config, donc ce test dit aussi que toute la liste est
     * lue — l'original, lui, n'en gardait qu'une.
     */
    @GameTest(template = "empty")
    public static void lesEntitesDevieesSontCellesDeLaConfig(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), 300);

        var player = ownPlayer(helper, "classifier");
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);

        // Les trois entrees de la config par defaut.
        var arrow = new net.minecraft.world.entity.projectile.Arrow(
                net.minecraft.world.entity.EntityType.ARROW, level);
        arrow.moveTo(abs.getX(), abs.getY(), abs.getZ(), 0f, 0f);
        assertClose(helper, 1.0d,
                cn.academy.ability.vecmanip.EntityAffection.affect(arrow).difficulty(),
                "une fleche vaut 1,0");

        var potion = new net.minecraft.world.entity.projectile.ThrownPotion(
                net.minecraft.world.entity.EntityType.POTION, level);
        potion.moveTo(abs.getX(), abs.getY(), abs.getZ(), 0f, 0f);
        assertClose(helper, 1.4d,
                cn.academy.ability.vecmanip.EntityAffection.affect(potion).difficulty(),
                "une potion vaut 1,4 : la deuxieme entree de la config est donc lue");

        var snowball = new net.minecraft.world.entity.projectile.Snowball(
                net.minecraft.world.entity.EntityType.SNOWBALL, level);
        snowball.moveTo(abs.getX(), abs.getY(), abs.getZ(), 0f, 0f);
        assertClose(helper, 0.1d,
                cn.academy.ability.vecmanip.EntityAffection.affect(snowball).difficulty(),
                "une boule de neige ne vaut presque rien");

        // Rien de vivant : c'est la liste d'exclusion de l'original, ou "living" et "mob"
        // etaient deja ecrits ainsi. On arrete ce qui vole, pas ce qui marche.
        var zombie = new net.minecraft.world.entity.monster.Zombie(
                net.minecraft.world.entity.EntityType.ZOMBIE, level);
        assertTrue(helper, cn.academy.ability.vecmanip.EntityAffection.affect(zombie).excluded(),
                "une creature est exclue");
        var cow = new net.minecraft.world.entity.animal.Cow(
                net.minecraft.world.entity.EntityType.COW, level);
        assertTrue(helper, cn.academy.ability.vecmanip.EntityAffection.affect(cow).excluded(),
                "et une bete aussi : tout ce qui vit est exclu");

        var loose = new net.minecraft.world.entity.item.ItemEntity(level, abs.getX(), abs.getY(),
                abs.getZ(), new ItemStack(net.minecraft.world.item.Items.STONE, 1));
        assertTrue(helper, cn.academy.ability.vecmanip.EntityAffection.affect(loose).excluded(),
                "un objet au sol est exclu par son nom");

        // Ni vivant ni exclu, ni dans la liste : la difficulte par defaut, 1,0.
        var boat = new net.minecraft.world.entity.vehicle.Boat(
                net.minecraft.world.entity.EntityType.BOAT, level);
        boat.moveTo(abs.getX(), abs.getY(), abs.getZ(), 0f, 0f);
        var affect = cn.academy.ability.vecmanip.EntityAffection.affect(boat);
        assertFalse(helper, affect.excluded(), "une barque n'est pas exclue");
        assertClose(helper, 1.0d, affect.difficulty(), "et vaut la difficulte par defaut");

        // Le repere : une entite deviee le reste, meme apres un tour dans son NBT.
        assertFalse(helper, cn.academy.ability.vecmanip.EntityAffection.isMarked(boat),
                "rien n'est marque au depart");
        cn.academy.ability.vecmanip.EntityAffection.mark(boat);
        assertTrue(helper, cn.academy.ability.vecmanip.EntityAffection.isMarked(boat),
                "et le repere tient");
        var tag = boat.getPersistentData().copy();
        assertTrue(helper, tag.getBoolean("ac_vm_deviated"),
                "le repere est bien le tag de l'original : " + tag);

        helper.succeed();
    }

    /**
     * La veille de la deviation : ce qu'elle arrete, ce qu'elle ignore, et ce qu'elle coute.
     *
     * <p>Trois entites autour du joueur, dont une seule doit bouger : une fleche (arretee et
     * marquee), une creature et un objet au sol (exclus, donc intacts). Le deuxieme tick
     * verifie le repere : une entite deja deviee ne rapporte plus rien.
     *
     * <p>La reduction de degats se lit juste apres, sur le meme maintien : c'est le m\u00eame
     * etat qui l'autorise, un maintien ouvert, et le test le dit en la lisant par
     * {@code AbilityEvents} — le crochet par lequel le jeu la declenche.
     */
    @GameTest(template = "empty")
    public static void laDeviationArreteCeQuiEntreEtAmortitLesCoups(GameTestHelper helper) {
        var deviation = cn.academy.ability.vecmanip.VecmanipCategory.VEC_DEVIATION;
        ServerLevel level = helper.getLevel();
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), 340);

        var player = ownPlayer(helper, "deviator");
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);
        clearCorridor(helper, abs, 10);

        // Un maintien ouvert, comme le paquet l'ouvre : c'est lui qui autorise tout.
        var data = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .orElseThrow(() -> new IllegalStateException("le faux joueur doit porter la donnee"));
        data.setCategoryLevel(deviation.getCategory(), 2);
        data.learnSkill(deviation);
        deviation.onStart(player, data);
        data.beginCharge(deviation);

        var arrow = new net.minecraft.world.entity.projectile.Arrow(
                net.minecraft.world.entity.EntityType.ARROW, level);
        arrow.moveTo(abs.getX() + 0.5, abs.getY() + 1.0, abs.getZ() + 3.0, 0f, 0f);
        arrow.setDeltaMovement(0.0, 0.0, -1.0);
        level.addFreshEntity(arrow);

        var zombie = new net.minecraft.world.entity.monster.Zombie(
                net.minecraft.world.entity.EntityType.ZOMBIE, level);
        zombie.moveTo(abs.getX() + 2.5, abs.getY(), abs.getZ() + 2.5, 0f, 0f);
        zombie.setDeltaMovement(0.0, 0.0, -1.0);
        level.addFreshEntity(zombie);

        double reserveBefore = data.getControlPoint();
        float overloadBefore = data.getOverload();
        assertTrue(helper, deviation.onHoldTick(player, data, 1), "la veille doit tenir");

        // La fleche : arretee net, et marquee.
        assertClose(helper, 0d, arrow.getDeltaMovement().length(), "la fleche doit etre arretee");
        assertClose(helper, 0.0d, arrow.getBaseDamage(), "et ne plus pouvoir blesser personne");
        assertTrue(helper, cn.academy.ability.vecmanip.EntityAffection.isMarked(arrow),
                "et elle est marquee comme deviee");
        assertClose(helper, 0.001d, data.getSkillExp(deviation),
                "0,001 par point de difficulte : une fleche vaut 1,0");

        // La creature et la bete, elles, n'ont pas bouge : elles sont exclues.
        assertClose(helper, -1.0d, zombie.getDeltaMovement().z, "une creature n'est pas arretee");
        assertFalse(helper, cn.academy.ability.vecmanip.EntityAffection.isMarked(zombie),
                "et elle n'est pas marquee");

        // Le deuxieme tick ne rapporte plus rien : la fleche est marquee.
        assertTrue(helper, deviation.onHoldTick(player, data, 2), "la veille tient toujours");
        assertClose(helper, 0.001d, data.getSkillExp(deviation),
                "une entite deja deviee ne rapporte plus d'experience");
        assertTrue(helper, data.getOverload() > overloadBefore + 14f,
                "chaque prise se paie en surcout : " + data.getOverload());
        assertTrue(helper, data.getControlPoint() < reserveBefore - 0.9f,
                "et l'entretien se paie par tick : " + data.getControlPoint());

        // La reduction de degats, par le crochet du jeu. Le gain d'experience du coup est
        // verse AVANT que la reduction ne soit calculee, comme dans l'original : le coup
        // s'amortit donc avec l'experience qu'il vient de donner, et 10 points tombent a
        // 5,965 et non a 6 — a 0,001 d'experience la part epargnee vaut 0,4035.
        var hurt = new net.minecraftforge.event.entity.living.LivingHurtEvent(player,
                player.damageSources().generic(), 10f);
        cn.academy.ability.AbilityEvents.onLivingHurt(hurt);
        assertTrue(helper, Math.abs(5.965f - hurt.getAmount()) < 0.01f,
                "10 points doivent tomber a 5,965 : la deviation epargne 40 %");

        // Et hors du maintien, elle ne fait plus rien du tout.
        data.cancelCharge(deviation);
        var after = new net.minecraftforge.event.entity.living.LivingHurtEvent(player,
                player.damageSources().generic(), 10f);
        cn.academy.ability.AbilityEvents.onLivingHurt(after);
        assertClose(helper, 10.0d, after.getAmount(),
                "sans maintien, les degats passent entiers");

        arrow.discard();
        zombie.discard();
        helper.succeed();
    }

    /**
     * La reflexion de vecteur : ce qu'elle fait de different de la deviation.
     *
     * <p>Meme veille, meme repere, mais l'effet est l'inverse : une fleche est <b>renvoyee</b>
     * vers le point que le regard touche, a sa vitesse, et une boule de feu est <b>remplacee</b>
     * — le tir change de camp. Le deuxieme tick verifie le repere partage par les deux
     * competences : ce qui est deja renvoye ne rapporte plus rien.
     */
    @GameTest(template = "empty")
    public static void laReflexionRenvoieCeQuiVole(GameTestHelper helper) {
        var reflection = cn.academy.ability.vecmanip.VecmanipCategory.VEC_REFLECTION;
        ServerLevel level = helper.getLevel();
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), 360);

        var player = ownPlayer(helper, "reflector");
        // Le couloir est nettoye AVANT de placer le joueur : ce qui reste d'une execution
        // ratee ferait echouer le compte des boules de feu juste apres.
        clearCorridor(helper, abs, 10);
        // Le regard vers +Z : c'est de ce cote que tout doit repartir.
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);

        var data = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .orElseThrow(() -> new IllegalStateException("le faux joueur doit porter la donnee"));
        data.setCategoryLevel(reflection.getCategory(), 4);
        data.learnSkill(reflection);
        reflection.onStart(player, data);
        data.beginCharge(reflection);

        var arrow = new net.minecraft.world.entity.projectile.Arrow(
                net.minecraft.world.entity.EntityType.ARROW, level);
        arrow.moveTo(abs.getX() + 0.5, abs.getY() + 1.0, abs.getZ() + 3.0, 0f, 0f);
        arrow.setDeltaMovement(0.0, 0.0, -1.0);
        level.addFreshEntity(arrow);

        var ball = new net.minecraft.world.entity.projectile.SmallFireball(
                net.minecraft.world.entity.EntityType.SMALL_FIREBALL, level);
        ball.moveTo(abs.getX() + 1.5, abs.getY() + 1.0, abs.getZ() + 3.0, 0f, 0f);
        ball.setDeltaMovement(0.0, 0.0, -0.5);
        level.addFreshEntity(ball);

        double reserveBefore = data.getControlPoint();
        assertTrue(helper, reflection.onHoldTick(player, data, 1), "la veille doit tenir");

        // La fleche repart vers le regard, a sa vitesse, et porte le repere.
        assertClose(helper, 1.0d, arrow.getDeltaMovement().length(),
                "une fleche renvoyee garde sa vitesse");
        assertTrue(helper, arrow.getDeltaMovement().z > 0.9,
                "et repart la ou le regard touche : " + arrow.getDeltaMovement());
        assertTrue(helper, cn.academy.ability.vecmanip.EntityAffection.isMarked(arrow),
                "et elle est marquee comme renvoyee");
        assertClose(helper, 0.0016d, data.getSkillExp(reflection),
                "0,0008 par point de difficulte, pour deux prises de difficulte 1,0");

        // La boule de feu, elle, a ete remplacee : une seule boule dans le couloir, vivante,
        // marquee, et relancee vers le regard.
        var balls = level.getEntitiesOfClass(net.minecraft.world.entity.projectile.SmallFireball.class,
                new net.minecraft.world.phys.AABB(abs).inflate(6));
        assertClose(helper, 1d, balls.size(),
                "l'ancienne boule a disparu, une neuve a pris sa place");
        assertFalse(helper, balls.get(0) == ball, "et c'est bien une autre boule");
        assertTrue(helper, cn.academy.ability.vecmanip.EntityAffection.isMarked(balls.get(0)),
                "marquee a son tour, pour ne pas etre renvoyee deux fois");
        assertTrue(helper, balls.get(0).getDeltaMovement().z > 0.4,
                "et relancee vers le regard : " + balls.get(0).getDeltaMovement());
        assertFalse(helper, ball.isAlive(), "l'ancienne boule, elle, est morte");

        // Les deux prises se paient en reserve : 10,7 CP par entite de difficulte 1.
        assertTrue(helper, data.getControlPoint() < reserveBefore - 21f,
                "deux prises a payer : " + data.getControlPoint());

        // Le deuxieme tick ne rapporte plus rien : les deux sont marquees.
        double expAfterFirst = data.getSkillExp(reflection);
        assertTrue(helper, reflection.onHoldTick(player, data, 2), "la veille tient toujours");
        assertClose(helper, expAfterFirst, data.getSkillExp(reflection),
                "une entite deja renvoyee ne rapporte plus d'experience");

        arrow.discard();
        balls.get(0).discard();
        helper.succeed();
    }

    /**
     * La reflexion des coups : elle renvoie une part des degats a qui les a donnes, et
     * annule le coup quand la part renvoyee couvre ce qu'elle a pris.
     *
     * <p>Le renvoi est lu <b>avant</b> que l'experience du coup ne soit versee, comme dans
     * l'original : a l'ouverture, 10 points de degats sont donc rendus a 6 pile, et non a
     * 6,024 comme le voudrait l'experience gagnee au passage.
     */
    @GameTest(template = "empty")
    public static void laReflexionRendLesCoups(GameTestHelper helper) {
        var reflection = cn.academy.ability.vecmanip.VecmanipCategory.VEC_REFLECTION;
        ServerLevel level = helper.getLevel();
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), 320);

        var player = ownPlayer(helper, "reflector-of-hits");
        clearCorridor(helper, abs, 6);
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);

        var data = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .orElseThrow(() -> new IllegalStateException("le faux joueur doit porter la donnee"));
        data.setCategoryLevel(reflection.getCategory(), 4);
        data.learnSkill(reflection);
        reflection.onStart(player, data);
        data.beginCharge(reflection);

        var zombie = new net.minecraft.world.entity.monster.Zombie(
                net.minecraft.world.entity.EntityType.ZOMBIE, level);
        zombie.moveTo(abs.getX() + 2.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);
        level.addFreshEntity(zombie);

        // Un coup de 10 donne par le zombie : 60 % repartent, donc 6 points, et il en reste 4.
        double reserveBefore = data.getControlPoint();
        var hurt = new net.minecraftforge.event.entity.living.LivingHurtEvent(player,
                player.damageSources().mobAttack(zombie), 10f);
        cn.academy.ability.AbilityEvents.onLivingHurt(hurt);
        assertClose(helper, 4.0d, hurt.getAmount(), "10 points dont 60 % repartent");
        assertTrue(helper, zombie.getHealth() < 15f && zombie.getHealth() > 13f,
                "et l'attaquant encaisse les 6 points renvoyes : " + zombie.getHealth());
        assertClose(helper, 0.004d, data.getSkillExp(reflection),
                "0,0004 par point encaisse");
        assertTrue(helper, data.getControlPoint() < reserveBefore - 7f,
                "un coup de 10 points coute 7,1 CP : " + data.getControlPoint());

        // Au maximum la part renvoyee vaut 120 % : le coup est entierement rendu, donc
        // annule — le porteur ne prend rien du tout.
        data.addSkillExp(reflection, 1f);
        var absorbed = new net.minecraftforge.event.entity.living.LivingHurtEvent(player,
                player.damageSources().mobAttack(zombie), 10f);
        cn.academy.ability.AbilityEvents.onLivingHurt(absorbed);
        assertClose(helper, 0.0d, absorbed.getAmount(), "un renvoi complet ne laisse rien passer");
        assertTrue(helper, absorbed.isCanceled(), "et le coup est annule");

        // Hors du maintien, plus rien : les degats passent entiers, et rien n'est annule.
        data.cancelCharge(reflection);
        var after = new net.minecraftforge.event.entity.living.LivingHurtEvent(player,
                player.damageSources().mobAttack(zombie), 10f);
        cn.academy.ability.AbilityEvents.onLivingHurt(after);
        assertClose(helper, 10.0d, after.getAmount(), "sans maintien, les degats passent entiers");
        assertFalse(helper, after.isCanceled(), "et rien n'est annule");

        zombie.discard();
        helper.succeed();
    }

    /**
     * Les ailes de tempete : le vol est ouvert pendant le maintien, et rendu a la fin.
     *
     * <p>Le deplacement lui-meme ne se verifie pas ici : c'est le client qui le pousse, comme
     * dans l'original. Ce qui se verifie, c'est la note — la charge gratuite, le premier tick
     * de vol qui paie et qui rapporte, la reserve qui manque et qui termine, et le vol rendu.
     */
    @GameTest(template = "empty")
    public static void lesAilesDeTempeteAutorisentLeVolEtFacturentLeVol(GameTestHelper helper) {
        var wing = cn.academy.ability.vecmanip.VecmanipCategory.STORM_WING;
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), 300);

        var player = ownPlayer(helper, "winged");
        clearCorridor(helper, abs, 6);
        player.moveTo(abs.getX() + 0.5, abs.getY() + 20, abs.getZ() + 0.5, 0f, 0f);

        var data = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .orElseThrow(() -> new IllegalStateException("le faux joueur doit porter la donnee"));
        data.setCategoryLevel(wing.getCategory(), 3);
        data.learnSkill(wing);

        assertFalse(helper, player.getAbilities().mayfly, "un joueur de survie ne vole pas");

        // L'ouverture, comme le paquet la fait : le surcout d'abord, puis le maintien.
        assertTrue(helper, data.perform(wing.getCpCost(), wing.getOverloadCost(data)),
                "le prix d'ouverture se paie");
        wing.onStart(player, data);
        data.beginCharge(wing);
        assertTrue(helper, player.getAbilities().mayfly, "les ailes ouvrent le vol");

        // La charge : septante ticks, gratuits, sans experience.
        double reserveBefore = data.getControlPoint();
        for (int tick = 1; tick <= 70; tick++) {
            assertTrue(helper, wing.onHoldTick(player, data, tick), "la charge tient au tick " + tick);
        }
        assertClose(helper, reserveBefore, data.getControlPoint(), "la charge ne coute rien");
        assertClose(helper, 0.0d, data.getSkillExp(wing), "et ne rapporte rien");

        // Le premier tick de vol : l'experience, la reserve, et le vol qui tient.
        assertTrue(helper, wing.onHoldTick(player, data, 71), "les ailes volent");
        assertClose(helper, 0.00005d, data.getSkillExp(wing), "0,00005 par tick de vol");
        assertTrue(helper, data.getControlPoint() < reserveBefore - 1.4f,
                "et 40 CP sur 2800 valent 1,43 : " + data.getControlPoint());

        // Plus de reserve, plus d'ailes : c'est la fin du maintien.
        while (data.getControlPoint() > 0f) {
            assertTrue(helper, data.consumeControlPoint(data.getControlPoint()),
                    "vider la reserve doit marcher");
        }
        assertFalse(helper, wing.onHoldTick(player, data, 72), "la reserve vide termine le vol");

        // Et la fin ordinaire rend le vol, avec la recharge de l'original.
        cn.academy.ability.AbilityEvents.endHeld(player, data, wing);
        assertFalse(helper, player.getAbilities().mayfly, "le vol est rendu a la fin");
        assertTrue(helper, data.isOnCooldown(wing), "et la recharge est posee");

        helper.succeed();
    }

    /**
     * Les ailes maladroites : sous 15 % d'experience, elles cassent ce qu'elles trouvent.
     *
     * <p>Tout le volume de casse est rempli de neige — vingt et un blocs de cote, un millier
     * de positions — parce que l'original en tire quarante au hasard : sans un volume plein,
     * un tirage ne tomberait presque jamais sur un bloc tendre, et le test ne dirait rien. Le
     * cas « avec de l'experience » passe en premier, l'experience ne s'oubliant pas.
     */
    @GameTest(template = "empty")
    public static void lesAilesMaladroitesCassentCeQuEllesTrouvent(GameTestHelper helper) {
        var wing = cn.academy.ability.vecmanip.VecmanipCategory.STORM_WING;
        int height = 260;
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), height);

        var expert = ownPlayer(helper, "clumsy-no");
        clearCorridor(helper, abs, 6);
        expert.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);
        var expertData = expert.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .orElseThrow(() -> new IllegalStateException("le faux joueur doit porter la donnee"));
        expertData.setCategoryLevel(wing.getCategory(), 3);
        expertData.learnSkill(wing);
        expertData.addSkillExp(wing, 0.2f);
        assertFalse(helper, wing.clumsy(expertData), "a 0,2 d'experience, les ailes sont sures");

        var clumsy = ownPlayer(helper, "clumsy-yes");
        clumsy.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);
        var clumsyData = clumsy.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .orElseThrow(() -> new IllegalStateException("le faux joueur doit porter la donnee"));
        clumsyData.setCategoryLevel(wing.getCategory(), 3);
        clumsyData.learnSkill(wing);
        assertTrue(helper, wing.clumsy(clumsyData), "et a zero, elles ne le sont pas");

        // Le volume de casse, dans le repere RELATIF de la structure : le joueur est pose a
        // la relative 1 + hauteur, donc le volume est centre la.
        for (int dx = -10; dx <= 10; dx++) {
            for (int dy = -10; dy <= 10; dy++) {
                for (int dz = -10; dz <= 10; dz++) {
                    helper.setBlock(new BlockPos(2 + dx, 1 + height + dy, 2 + dz),
                            net.minecraft.world.level.block.Blocks.SNOW_BLOCK);
                }
            }
        }

        wing.onHoldTick(expert, expertData, 5);
        assertValue(helper, net.minecraft.world.level.block.Blocks.SNOW_BLOCK,
                helper.getBlockState(new BlockPos(2, 1 + height, 2)).getBlock(),
                "un joueur experimente ne casse rien");

        wing.onHoldTick(clumsy, clumsyData, 5);
        int broken = 0;
        for (int dx = -10; dx <= 10; dx++) {
            for (int dy = -10; dy <= 10; dy++) {
                for (int dz = -10; dz <= 10; dz++) {
                    if (helper.getBlockState(new BlockPos(2 + dx, 1 + height + dy, 2 + dz))
                            .isAir()) {
                        broken++;
                    }
                }
            }
        }
        assertTrue(helper, broken >= 20,
                "les ailes d'un debutant cassent ce qu'elles trouvent : " + broken + " blocs");

        helper.succeed();
    }

    /**
     * Les ailes pleines : a cent pour cent d'experience, l'ouverture repousse tout ce qui est
     * a moins de six blocs.
     */
    @GameTest(template = "empty")
    public static void lesAilesPleinesRepoussentALOuverture(GameTestHelper helper) {
        var wing = cn.academy.ability.vecmanip.VecmanipCategory.STORM_WING;
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), 220);

        var player = ownPlayer(helper, "full-wing");
        clearCorridor(helper, abs, 6);
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);

        var data = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .orElseThrow(() -> new IllegalStateException("le faux joueur doit porter la donnee"));
        data.setCategoryLevel(wing.getCategory(), 3);
        data.learnSkill(wing);
        data.addSkillExp(wing, 1f);
        assertTrue(helper, wing.blows(data), "a pleine experience, l'ouverture repousse");
        assertValue(helper, (int) wing.chargeTicks(data), 30, "et les ailes s'ouvrent en trente ticks");

        assertTrue(helper, data.perform(wing.getCpCost(), wing.getOverloadCost(data)),
                "le prix d'ouverture se paie");
        wing.onStart(player, data);
        data.beginCharge(wing);

        var cow = new net.minecraft.world.entity.animal.Cow(
                net.minecraft.world.entity.EntityType.COW, helper.getLevel());
        cow.moveTo(abs.getX() + 3.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);
        cow.setDeltaMovement(0, 0, 0);
        helper.getLevel().addFreshEntity(cow);

        // Le dernier tick de la charge ne repousse encore rien...
        assertTrue(helper, wing.onHoldTick(player, data, 30), "la charge tient");
        assertClose(helper, 0.0d, cow.getDeltaMovement().length(), "et rien n'a bouge");

        // ... et le suivant ouvre les ailes : la bete part a l'oppose du joueur.
        assertTrue(helper, wing.onHoldTick(player, data, 31), "les ailes s'ouvrent");
        assertTrue(helper, cow.getDeltaMovement().length() >= 0.5,
                "le souffle repousse : " + cow.getDeltaMovement());
        assertTrue(helper, cow.getDeltaMovement().x > 0,
                "et il repousse bien de l'autre cote : " + cow.getDeltaMovement());

        cow.discard();
        helper.succeed();
    }

    /**
     * Le canon a plasma : la charge se paie, le tir part, la boule vole et explose.
     *
     * <p>C'est la seule competence du port a trois temps, et le seul test qui va jusqu'au
     * bout : la charge par tick, le relachement qui ne part que sur une charge complete, la
     * boule posee quinze blocs au-dessus de la tete, le vol d'un bloc par tick, et ce qui
     * reste de ce qu'elle a trouve.
     *
     * <p>Le point vise est un <b>mur de pierre</b> monte dix blocs devant, et pas une bete :
     * un tir qui doit tomber sur une creature depend de ce que le rayon trouve sur sa route,
     * et ce qu'il trouve — une bete projetee la par une execution precedente — n'est pas
     * toujours la sienne. Vecu a la troisieme execution, ou le test a echoue sur la vie de sa
     * cible sans rien dire d'autre. La bete du test est donc <b>a cote</b> du mur, dans le
     * rayon de l'explosion.
     *
     * <p>L'altitude est un multiple de vingt qui n'appartient a personne : l'explosion est
     * large de douze blocs, et un couloir voisin n'a rien a y faire.
     */
    @GameTest(template = "empty")
    public static void leCanonAPlasmaTireEtExploseOuIlTombe(GameTestHelper helper) {
        var cannon = cn.academy.ability.vecmanip.VecmanipCategory.PLASMA_CANNON;
        ServerLevel level = helper.getLevel();
        int height = 180;
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), height);

        var player = ownPlayer(helper, "plasma-gunner");
        clearCorridor(helper, abs, 20);
        // Le regard a plat : c'est ainsi que le rayon touche le mur.
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);

        // Le mur : trois blocs de large sur trois de haut, a dix blocs devant. Le repere
        // RELATIF compte la hauteur du test (voir le commentaire du sol du couloir).
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                helper.setBlock(new BlockPos(2 + dx, 1 + height + dy, 12),
                        net.minecraft.world.level.block.Blocks.STONE);
            }
        }

        var data = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .orElseThrow(() -> new IllegalStateException("le faux joueur doit porter la donnee"));
        data.setCategoryLevel(cannon.getCategory(), 5);
        data.learnSkill(cannon);
        assertTrue(helper, data.perform(cannon.getCpCost(), cannon.getOverloadCost(data)),
                "le prix d'ouverture se paie");
        cannon.onStart(player, data);
        data.beginCharge(cannon);

        // La boule est posee a l'appui, quinze blocs au-dessus de la tete.
        var ball = data.getHoldPoint(cannon);
        assertTrue(helper, ball != null, "la boule doit etre posee a l'ouverture");
        assertClose(helper, abs.getY() + 15.0d, ball.y, "quinze blocs au-dessus de la tete");

        // La bete, a cote du mur : elle ne gene pas la visee, et l'explosion la prend.
        var zombie = new net.minecraft.world.entity.monster.Zombie(
                net.minecraft.world.entity.EntityType.ZOMBIE, level);
        zombie.moveTo(abs.getX() + 3.5, abs.getY(), abs.getZ() + 10.5, 0f, 0f);
        level.addFreshEntity(zombie);
        var zombiesAround = level.getEntitiesOfClass(
                net.minecraft.world.entity.monster.Zombie.class,
                new net.minecraft.world.phys.AABB(abs).inflate(30));
        assertTrue(helper, zombiesAround.contains(zombie),
                "la bete doit etre dans le monde : " + zombiesAround.size() + " betes autour");

        // La charge : soixante ticks payes par tick, et rien de verse avant le tir.
        double reserveBefore = data.getControlPoint();
        for (int tick = 1; tick < 60; tick++) {
            assertTrue(helper, cannon.onHoldTick(player, data, tick),
                    "la charge tient au tick " + tick);
        }
        assertClose(helper, 0.0d, data.getSkillExp(cannon), "rien n'est verse avant le tir");
        assertTrue(helper, data.getControlPoint() < reserveBefore - 37f,
                "et la charge se paie : " + data.getControlPoint());

        // Le relachement, sur une charge complete : la boule part, et l'experience est versee.
        assertTrue(helper, cannon.onRelease(player, data, 60),
                "une charge complete fait partir la boule");
        assertClose(helper, 0.008d, data.getSkillExp(cannon), "0,008 au tir");

        // Et elle vise bien le mur : la face touchee est a dix blocs du joueur, et dans la
        // largeur du mur. Sans cette assertion, un tir qui part ailleurs laisserait le test
        // echouer sur la vie de sa cible, sans dire pourquoi.
        var destination = data.getHoldOrigin(cannon);
        assertTrue(helper, destination != null
                        && Math.abs(destination.z - (abs.getZ() + 10.0)) < 0.01
                        && Math.abs(destination.x - (abs.getX() + 0.5)) < 1.5,
                "la boule doit viser le mur : " + destination);

        // Le vol : un bloc par tick, et il finit sur le mur.
        int flown = 0;
        boolean flying = true;
        while (flying && flown < 100) {
            flown++;
            flying = cannon.onHoldTick(player, data, 60 + flown);
        }
        assertFalse(helper, flying, "la boule finit par tomber");
        assertTrue(helper, flown > 5, "et elle vole vraiment : " + flown + " ticks");
        assertFalse(helper, zombie.isAlive(),
                "ce qui se trouve a dix blocs de l'arrivee n'y survit pas");

        // La fin ordinaire, avec la recharge la plus longue du port.
        cn.academy.ability.AbilityEvents.endHeld(player, data, cannon);
        assertTrue(helper, data.isOnCooldown(cannon), "et la recharge est posee");

        zombie.discard();
        helper.succeed();
    }

    /**
     * La detection de minerais : un eclat qui se paie, un regard qui a lieu, et rien du tout
     * quand la reserve manque.
     *
     * <p>C'est la seule competence du port qui paie <b>dans son effet</b> : le paquet
     * d'activation ne la touche pas. Le test tient donc les deux bouts — la reserve pleine, ou
     * l'eclat aveugle et pose sa recharge ; la reserve vide, ou il ne se passe rien du tout.
     */
    @GameTest(template = "empty")
    public static void laDetectionDeMineraisPaieSonRegard(GameTestHelper helper) {
        var detect = cn.academy.ability.electromaster.ElectromasterCategory.MINE_DETECT;
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), 140);

        var player = ownPlayer(helper, "ore-seer");
        clearCorridor(helper, abs, 4);
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);

        var data = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .orElseThrow(() -> new IllegalStateException("le faux joueur doit porter la donnee"));
        data.setCategoryLevel(detect.getCategory(), 3);
        data.learnSkill(detect);

        double reserveBefore = data.getControlPoint();
        float overloadBefore = data.getOverload();
        detect.onActivate(player, data);

        var blindness = player.getEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
        assertTrue(helper, blindness != null, "l'eclat aveugle un instant");
        assertValue(helper, 100, blindness.getDuration(), "cent ticks, comme le regard");
        assertClose(helper, 0.008d, data.getSkillExp(detect), "et verse 0,008 d'experience");
        assertTrue(helper, data.isOnCooldown(detect), "puis pose sa recharge");
        // Quarante-cinq secondes au depart, moins l'experience que l'eclat vient de verser :
        // la recharge est lue APRES le gain, comme dans l'original, ou elle vaut donc 896 et
        // non 900 des le premier eclat.
        assertTrue(helper, data.getCooldown(detect) >= 895 && data.getCooldown(detect) <= 900,
                "quarante-cinq secondes au depart : " + data.getCooldown(detect));
        assertTrue(helper, data.getControlPoint() < reserveBefore - 53f,
                "1500 CP sur 2800 valent 53,57 : " + data.getControlPoint());
        assertTrue(helper, data.getOverload() > overloadBefore + 199f,
                "et 200 de surcout, verbatim : " + data.getOverload());

        // Sans reserve : ni regard, ni experience, ni recharge. C'est tout l'interet du
        // paiement dans l'effet, et c'est la moitie du test.
        var poor = ownPlayer(helper, "ore-seer-poor");
        poor.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);
        var poorData = poor.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .orElseThrow(() -> new IllegalStateException("le faux joueur doit porter la donnee"));
        poorData.setCategoryLevel(detect.getCategory(), 3);
        poorData.learnSkill(detect);
        while (poorData.getControlPoint() > 0f) {
            assertTrue(helper, poorData.consumeControlPoint(poorData.getControlPoint()),
                    "vider la reserve doit marcher");
        }

        detect.onActivate(poor, poorData);
        assertFalse(helper, poor.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),
                "sans reserve, le regard n'a pas lieu");
        assertClose(helper, 0.0d, poorData.getSkillExp(detect), "et rien n'est verse");
        assertFalse(helper, poorData.isOnCooldown(detect), "ni aucune recharge");

        // Ce que l'oeil sait reconnaitre, enfin : c'est le monde qui le dit, donc un GameTest.
        var targets = cn.academy.ability.electromaster.MetalTargets.class;
        assertTrue(helper, cn.academy.ability.electromaster.MetalTargets.isOreBlock(
                        net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState()),
                "un minerai de fer est un minerai");
        assertTrue(helper, cn.academy.ability.electromaster.MetalTargets.isOreBlock(
                        net.minecraft.world.level.block.Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState()),
                "et un minerai des profondeurs aussi");
        assertTrue(helper, cn.academy.ability.electromaster.MetalTargets.isOreBlock(
                        cn.academy.ModBlocks.CONSTRAINT_METAL_ORE.get().defaultBlockState()),
                "le minerai de la maison ne dit pas ore dans son nom — l'original le tenait par sa classe");
        assertTrue(helper, cn.academy.ability.electromaster.MetalTargets.isOreBlock(
                        net.minecraft.world.level.block.Blocks.ANCIENT_DEBRIS.defaultBlockState()),
                "et les debris antiques sont le minerai de la netherite, sans le mot ore non plus");
        assertFalse(helper, cn.academy.ability.electromaster.MetalTargets.isOreBlock(
                        net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()),
                "la pierre n'en est pas un");
        assertValue(helper, 0, cn.academy.ability.electromaster.MetalTargets.harvestTier(
                net.minecraft.world.level.block.Blocks.COAL_ORE.defaultBlockState()),
                "le charbon se creuse a la main");
        assertValue(helper, 1, cn.academy.ability.electromaster.MetalTargets.harvestTier(
                net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState()),
                "le fer demande la pierre");
        assertValue(helper, 2, cn.academy.ability.electromaster.MetalTargets.harvestTier(
                net.minecraft.world.level.block.Blocks.DIAMOND_ORE.defaultBlockState()),
                "le diamant demande le fer");
        assertValue(helper, 3, cn.academy.ability.electromaster.MetalTargets.harvestTier(
                net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState()),
                "et l'obsidienne demande le diamant");

        helper.succeed();
    }

    /**
     * La manipulation magnetique d'un bloc : arracher, tenir, jeter, reposer.
     *
     * <p>Le test suit le bloc du monde a l'entite et retour, parce que c'est tout le sujet :
     * un bloc metallique quitte sa place, devient une entite qui se tient deux blocs devant
     * les yeux, part vers ce que le regard touche et se repose en chemin. Le lancer paie sa
     * reserve, son surcout, son experience et sa recharge ; l'arrachage, lui, ne paie rien.
     *
     * <p>Il verifie aussi le refus : les mains vides et rien de metallique dans les dix blocs
     * du regard, la competence ne s'ouvre pas — l'original refusait sans le dire, dans son
     * {@code canActivate}.
     */
    @GameTest(template = "empty")
    public static void laManipulationArracheEtRelanceUnBlocDeFer(GameTestHelper helper) {
        var manip = cn.academy.ability.electromaster.ElectromasterCategory.MAG_MANIP;
        var iron = net.minecraft.world.level.block.Blocks.IRON_BLOCK;
        var stone = net.minecraft.world.level.block.Blocks.STONE;
        int height = 100;
        BlockPos floor = new BlockPos(2, 1, 2);
        // `abs` ne sert qu'aux entites, qui vivent en coordonnees absolues ; les blocs se
        // posent en RELATIF, altitude du test comprise (voir le commentaire du sol du couloir).
        BlockPos abs = aboveTestArea(helper, floor, height);
        BlockPos eyes = new BlockPos(floor.getX(), floor.getY() + height + 1, floor.getZ());

        // Le monde de test est partage et sauvegarde : ce test range son couloir avant de
        // mesurer quoi que ce soit, sinon le bloc de fer laisse par l'execution precedente
        // serait arrache a la place de celui qu'il vient de poser. La dalle du test (le
        // relatif y 1) reste : elle porte le faux joueur.
        clearCorridor(helper, abs, 12);
        for (int dz = 0; dz <= 12; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = 0; dy <= 3; dy++) {
                    helper.setBlock(eyes.offset(dx, dy, dz),
                            net.minecraft.world.level.block.Blocks.AIR);
                }
            }
        }
        // Et la colonne que ce test <b>compte</b> a la fin, qui descend plus bas que le couloir :
        // un lancer qui finit mal laisse son bloc de fer un cran plus bas, la ou le nettoyage
        // large ci-dessus ne va pas. Deux executions ratees en ont laisse deux, et la troisieme
        // les a comptes : « attendu 1, trouve 2 ».
        //
        // Elle s'arrete a la dalle, et pas un cran sous elle : l'effacer ferait tomber le faux
        // joueur, qui ne pourrait plus rien lancer du tout — « attendu 1, trouve 0 ». C'est aussi
        // pour cela que le comptage de la fin commence a dy 0 : sous la dalle, ce n'est plus le
        // couloir du test, c'est son plancher.
        //
        // Elle couvre toute la LARGEUR du couloir — trois blocs, comme le mur qui l'arrete — et
        // non la seule colonne du milieu : un lancer qui derive de quatre centimetres pose son
        // bloc sur la colonne voisine, et le test le comptait alors pour rien (vecu : « pose a
        // (1.96, 42.13, 23.5) », un bloc a cote du mur).
        for (int dz = -2; dz <= 16; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 4; dy++) {
                    helper.setBlock(eyes.offset(dx, dy, dz),
                            net.minecraft.world.level.block.Blocks.AIR);
                }
            }
        }

        // Rien a tenir : les mains vides, et le regard remonte un couloir deja nettoye — neuf
        // blocs d'air, soit la portee exacte des dix pas de la sonde.
        var empty = ownPlayer(helper, "nothing-to-hold");
        empty.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        empty.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 9.5, 180f, 0f);
        var emptyData = empty.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .orElseThrow(() -> new IllegalStateException("le faux joueur doit porter la donnee"));
        emptyData.setCategoryLevel(manip.getCategory(), 2);
        emptyData.learnSkill(manip);
        assertFalse(helper, manip.canStart(empty, emptyData),
                "mains vides et rien de metallique : la competence refuse de s'ouvrir");
        assertFalse(helper, cn.academy.ability.electromaster.MagManipSkill.accepts(stone),
                "et la pierre ne s'arrache pas");
        assertTrue(helper, cn.academy.ability.electromaster.MagManipSkill.accepts(iron),
                "le fer, si");

        // Le decor : le bloc de fer au niveau du regard a trois blocs, et un mur de pierre
        // trois blocs plus loin. Le mur arrete le lancer et fixe le point vise.
        var player = ownPlayer(helper, "block-thrower");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);
        var data = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .orElseThrow(() -> new IllegalStateException("le faux joueur doit porter la donnee"));
        data.setCategoryLevel(manip.getCategory(), 2);
        data.learnSkill(manip);
        double reserveBefore = data.getControlPoint();
        float overloadBefore = data.getOverload();

        BlockPos ironAt = eyes.offset(0, 0, 3);
        helper.setBlock(ironAt, iron);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 3; dy++) {
                helper.setBlock(eyes.offset(dx, dy, 6), stone);
            }
        }

        var box = new net.minecraft.world.phys.AABB(
                abs.getX() - 4, abs.getY() - 2, abs.getZ() - 4,
                abs.getX() + 5, abs.getY() + 8, abs.getZ() + 13);

        assertTrue(helper, manip.canStart(player, data), "un bloc de fer dans le regard : on le prend");
        manip.onStart(player, data);
        assertTrue(helper, helper.getBlockState(ironAt).isAir(),
                "l'arrachage retire le bloc du monde, et ne paie rien");
        assertClose(helper, reserveBefore, data.getControlPoint(), "la reserve n'a pas bouge");
        var carried = helper.getLevel().getEntitiesOfClass(
                cn.academy.entity.EntityMagManipBlock.class, box);
        assertValue(helper, 1, carried.size(), "le bloc est devenu une entite");
        var block = carried.get(0);
        assertTrue(helper, block.getBlockState().is(iron), "qui porte l'etat du bloc de fer");

        // Le maintien : le bloc se tient devant les yeux, un dixieme sous la tete.
        var target = cn.academy.ability.electromaster.MagManipVisuals.carryTarget(
                player.getEyePosition(1f), player.getViewVector(1f));
        for (int i = 0; i < 40; i++) {
            assertTrue(helper, manip.onHoldTick(player, data, i), "le maintien a toujours son bloc");
            block.tick();
        }
        assertTrue(helper, block.position().distanceTo(target) < 0.5,
                "le bloc se tient deux blocs devant les yeux : " + block.position()
                        + " contre " + target);

        // Le lancer : il paie, verse son experience, pose sa recharge, et part.
        assertFalse(helper, manip.onRelease(player, data, 40),
                "relacher termine le maintien, que le bloc parte ou non");
        assertTrue(helper, data.getControlPoint() < reserveBefore - 4.5,
                "140 CP sur 2800 valent 5 : " + data.getControlPoint());
        assertTrue(helper, data.getOverload() > overloadBefore + 34f,
                "et 35 de surcout, verbatim : " + data.getOverload());
        assertClose(helper, 0.005d, data.getSkillExp(manip), "pour 0,005 d'experience");
        assertTrue(helper, data.isOnCooldown(manip), "et une recharge est posee");
        assertValue(helper, 60, data.getCooldown(manip), "de soixante ticks au depart");
        assertTrue(helper, block.getDeltaMovement().length() > 0.4,
                "et le bloc part vers le mur : " + block.getDeltaMovement());

        // La pose : il retombe sur le mur, s'y colle, et n'est plus une entite.
        for (int i = 0; i < 60 && block.isAlive(); i++) {
            block.tick();
        }
        var pose = block.position();
        assertFalse(helper, block.isAlive(), "le bloc s'est pose et l'entite a disparu — position "
                + pose + ", vitesse " + block.getDeltaMovement()
                + ", mur a z " + (abs.getZ() + 6));
        assertValue(helper, 0, helper.getLevel().getEntitiesOfClass(
                        cn.academy.entity.EntityMagManipBlock.class, box).size(),
                "aucun bloc de fer ne reste en l'air");
        int placed = 0;
        StringBuilder ou = new StringBuilder();
        for (int dz = -2; dz <= 16; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 4; dy++) {
                    if (helper.getBlockState(eyes.offset(dx, dy, dz)).is(iron)) {
                        placed++;
                        ou.append(" (dx ").append(dx).append(", dy ").append(dy)
                                .append(", dz ").append(dz).append(")");
                    }
                }
            }
        }
        // Le mur est a dz 6 : le bloc doit s'arreter AVANT lui, pas le traverser. Sa vitesse a
        // double — voir MagManipVisuals.STEPS — donc c'est cette avance double que le rayon de
        // pose doit couvrir, et le test verifie ici qu'aucun mur d'un bloc n'est saute.
        assertValue(helper, 1, placed, "et on le retrouve pose dans le couloir" + ou
                + " — pose a " + pose);
        assertTrue(helper, ou.toString().contains("dz 5") || ou.toString().contains("dz 6"),
                "et il s'arrete au mur, pas derriere : " + ou);

        helper.succeed();
    }

    /**
     * La PORTEE du lancer, sur un sol plat, a une experience donnee.
     *
     * <p>Le joueur a fait le test dans les deux versions : a 45 degres vers le haut, sur une
     * ligne droite, le bloc du port tombe 11 blocs plus loin avec 11 % d'experience, et celui
     * du vrai mod 18 blocs plus loin avec 7 %. Un lancer qui va plus loin avec MOINS
     * d'experience ne peut pas venir des nombres du lancer : la vitesse suit la meme courbe
     * dans les deux versions — 0,5 a 1 bloc par tick — et la gravite est la meme, 0,04 par
     * tick, ecrite dans les deux sources.
     *
     * <p>Ce test fige donc ce que le port fait pour de bon, sur un sol plat et sans mur :
     * la distance horizontale entre le joueur et le bloc pose, comparee a la portee theorique
     * d'un tir a 45 degres,
     *
     * <pre>R = (v cos) / g * (v sin + racine(v^2 sin^2 + 2 g h))</pre>
     *
     * <p>avec {@code h} la hauteur du bloc au-dessus du sol. Si le port s'en ecarte, c'est lui
     * qui a un probleme, et le message du test dit de combien.
     *
     * <p>MESURE DU 30/09 : 8,67 blocs en 24 ticks pour un calcul de 9,11 — l'ecart venant des deux
     * degres de plus que fait le lancer, parce que le bloc n'est pas tout a fait pose sur son point
     * de portage quand il part.
     *
     * <p>MAIS CE N'ETAIT PAS LA BONNE COMPARAISON, et le joueur l'a montre en poussant les deux
     * versions a 100 % d'experience : 55 blocs chez l'original, 29 ici. L'original avancait son
     * bloc DEUX FOIS par tick — son {@code Rigidbody} le deplacait, et son propre {@code onUpdate}
     * ajoutait encore le meme mouvement a sa position. Voir {@code MagManipVisuals.STEPS}, qui
     * porte maintenant cette avance double dans le port : la vitesse vraie est {@code speed * 2} et
     * la gravite vraie {@code 0,04 * 2}.
     */
    @GameTest(template = "empty")
    public static void laPorteeDuLancerEstCelleDuCalcul(GameTestHelper helper) {
        var manip = cn.academy.ability.electromaster.ElectromasterCategory.MAG_MANIP;
        var iron = net.minecraft.world.level.block.Blocks.IRON_BLOCK;
        var stone = net.minecraft.world.level.block.Blocks.STONE;
        var air = net.minecraft.world.level.block.Blocks.AIR;
        int height = 100;
        BlockPos floor = new BlockPos(2, 1, 2);
        BlockPos abs = aboveTestArea(helper, floor, height);
        BlockPos eyes = new BlockPos(floor.getX(), floor.getY() + height + 1, floor.getZ());

        // Un sol plat, large et long, et de l'air haut : le bloc doit retomber dessus, pas sur un
        // decor — et il monte maintenant a plus de cinq blocs, l'avance double y compris.
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 40; dz++) {
                helper.setBlock(eyes.offset(dx, -1, dz), stone);
                for (int dy = 0; dy <= 14; dy++) {
                    helper.setBlock(eyes.offset(dx, dy, dz), air);
                }
            }
        }

        var player = ownPlayer(helper, "portee-du-lancer");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);
        var data = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .orElseThrow(() -> new IllegalStateException("le faux joueur doit porter la donnee"));
        data.setCategoryLevel(manip.getCategory(), 2);
        data.learnSkill(manip);
        // L'experience du joueur au moment de son essai : onze pour cent.
        data.addSkillExp(manip, 0.11f);
        data.setControlPoint(data.getMaxControlPoint());

        helper.setBlock(eyes.offset(0, 0, 3), iron);
        assertTrue(helper, manip.canStart(player, data), "le bloc de fer est dans le regard");

        // Le monde de test est partage : un bloc laisse par un essai precedent y traine
        // encore, et le lancer en trouverait deux.
        var box = new net.minecraft.world.phys.AABB(
                abs.getX() - 8, abs.getY() - 3, abs.getZ() - 8,
                abs.getX() + 9, abs.getY() + 20, abs.getZ() + 45);
        for (var stray : helper.getLevel().getEntitiesOfClass(
                cn.academy.entity.EntityMagManipBlock.class, box)) {
            stray.discard();
        }

        manip.onStart(player, data);

        var carried = helper.getLevel().getEntitiesOfClass(
                cn.academy.entity.EntityMagManipBlock.class, box);
        assertValue(helper, 1, carried.size(), "le bloc est devenu une entite");
        var block = carried.get(0);

        // Le regard : quarante-cinq degres vers le haut, droit devant. Le bloc monte avec lui,
        // et il lui faut le temps de s'y poser : sa vitesse de portage s'annule sur le point
        // au carre de la distance, donc les derniers dixiemes de bloc sont longs.
        player.setXRot(-45f);
        for (int i = 0; i < 200; i++) {
            manip.onHoldTick(player, data, i);
            block.tick();
        }

        net.minecraft.world.phys.Vec3 launch = block.position();
        assertFalse(helper, manip.onRelease(player, data, 40), "le lancer termine le maintien");
        double speed = cn.academy.ability.electromaster.MagManipVisuals.flightSpeed(
                0.5 + 0.5 * 0.11);
        net.minecraft.world.phys.Vec3 velocity = block.getDeltaMovement();
        assertClose(helper, speed, velocity.length(), "la vitesse du lancer");
        // Le regard est droit devant (+Z) : la part horizontale est donc en Z, pas en X.
        assertTrue(helper,
                Math.abs(Math.hypot(velocity.x, velocity.z) - speed * Math.cos(Math.PI / 4)) < 0.03,
                "un tir a 45 degres : la part horizontale vaut la part verticale, " + velocity);

        // Le vol, tick par tick, jusqu'a la pose.
        int ticks = 0;
        while (block.isAlive() && ticks < 400) {
            block.tick();
            ticks++;
        }
        assertFalse(helper, block.isAlive(), "le bloc finit par se poser");

        double distance = Math.hypot(block.position().x - launch.x, block.position().z - launch.z);
        double drop = launch.y - block.position().y;
        // Le bloc avance DEUX FOIS son mouvement par tick — voir MagManipVisuals.STEPS : la
        // vitesse vraie est le double, et la gravite le double aussi, pour la meme chute.
        double w = speed * Math.cos(Math.PI / 4);
        double g = cn.academy.ability.electromaster.MagManipVisuals.flightGravity();
        double expected = w / g * (w + Math.sqrt(w * w + 2 * g * drop));
        assertTrue(helper, Math.abs(distance - expected) < 1.5,
                "portee " + (Math.round(distance * 100) / 100.0) + " blocs en " + ticks
                        + " ticks pour un calcul de " + (Math.round(expected * 100) / 100.0)
                        + " a la vitesse " + speed + " | depart " + launch
                        + " vitesse " + velocity);

        helper.succeed();
    }

    /**
     * Le sol du couloir : une bande de pierre d'un bloc de large, et de l'air au-dessus.
     *
     * <p>L'air sert a deux choses : il laisse la place aux cinq colonnes de l'onde, qui
     * iraient sinon creuser la pierre de toutes parts, et il rend la marche <b>previsible</b>
     * — seuls le bloc du marcheur et la colonne de trois comptent, donc l'energie ne part
     * jamais en fumee sur un tirage malheureux.
     *
     * <p>{@code base} est la hauteur <b>relative</b> du sol, altitude du test comprise : voir
     * le commentaire du premier test de l'onde.
     */
    private static void buildWalkingFloor(GameTestHelper helper, int length, int base) {
        for (int dz = 0; dz <= length; dz++) {
            helper.setBlock(new BlockPos(2, base, 2 + dz),
                    net.minecraft.world.level.block.Blocks.STONE);
            for (int dy = 1; dy <= 5; dy++) {
                for (int dx = 0; dx <= 4; dx++) {
                    helper.setBlock(new BlockPos(dx, base + dy, 2 + dz),
                            net.minecraft.world.level.block.Blocks.AIR);
                }
            }
        }
    }

    /** Les objets au sol dans le couloir d'un test, pour verifier ce qui est tombe. */
    private static java.util.List<net.minecraft.world.entity.item.ItemEntity> itemsAround(
            GameTestHelper helper, BlockPos abs, int length) {
        var box = new net.minecraft.world.phys.AABB(abs.getX() - 1, abs.getY() - 2, abs.getZ() - 1,
                abs.getX() + 5, abs.getY() + 6, abs.getZ() + length);
        return helper.getLevel().getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class, box);
    }

    // ------------------------------------------------------------------
    // Reseau energetique : le generateur de phase
    // ------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void phaseGeneratorBurnsLiquidIntoEnergy(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.PHASE_GENERATOR.get());
        var generator = (PhaseGeneratorBlockEntity) helper.getBlockEntity(rel);

        // Valeurs de TilePhaseGen : tampon 6000, bande passante LATENCY_MK1 = 50,
        // cuve 8000, 100 mB par tick a 0,5 d'energie par millibassin, soit 50.
        assertValue(helper, 6000, generator.getMaxEnergyStored(), "tampon du generateur de phase");
        assertClose(helper, 50.0d, generator.getBandwidth(), "bande passante");
        assertValue(helper, 8000, generator.getTankSize(), "capacite de la cuve");
        assertValue(helper, 100, PhaseGeneratorBlockEntity.CONSUME_PER_TICK, "phase par tick");
        assertClose(helper, 0.5d, PhaseGeneratorBlockEntity.GEN_PER_MB, "energie par millibassin");

        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(rel);

        assertFalse(helper, generator.isProducing(), "une cuve vide ne produit rien");
        for (int i = 0; i < 20; i++) {
            PhaseGeneratorBlockEntity.tick(level, abs, helper.getBlockState(rel), generator);
        }
        assertClose(helper, 0.0d, generator.getEnergy(), "sans phase, rien ne se produit");

        // 1000 mB de phase, de quoi tenir 10 ticks a 100 mB par tick.
        generator.setLiquidAmount(1000);
        assertTrue(helper, generator.isProducing(), "avec de la phase, la machine produit");

        for (int i = 0; i < 10; i++) {
            PhaseGeneratorBlockEntity.tick(level, abs, helper.getBlockState(rel), generator);
        }
        assertClose(helper, 500.0d, generator.getEnergy(), "10 ticks a 50 d'energie");
        assertValue(helper, 0, generator.getLiquidAmount(), "toute la phase doit avoir brule");
        assertFalse(helper, generator.isProducing(), "plus de phase, plus de production");
        helper.succeed();
    }

    /** Un tampon plein ne doit pas bruler de phase pour rien. */
    @GameTest(template = "empty")
    public static void phaseGeneratorStopsWhenItsBufferIsFull(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.PHASE_GENERATOR.get());
        var generator = (PhaseGeneratorBlockEntity) helper.getBlockEntity(rel);

        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(rel);

        generator.setLiquidAmount(1000);
        generator.setEnergy(6000.0d);

        for (int i = 0; i < 20; i++) {
            PhaseGeneratorBlockEntity.tick(level, abs, helper.getBlockState(rel), generator);
        }

        assertValue(helper, 1000, generator.getLiquidAmount(),
                "un tampon plein ne doit pas consommer de phase");
        assertClose(helper, 6000.0d, generator.getEnergy(), "le tampon reste plein");
        helper.succeed();
    }

    /** Les unites de phase fondent dans la cuve et ressortent vides. */
    @GameTest(template = "empty")
    public static void phaseGeneratorMeltsPhaseUnits(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.PHASE_GENERATOR.get());
        var generator = (PhaseGeneratorBlockEntity) helper.getBlockEntity(rel);

        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(rel);

        generator.getInventory().setStackInSlot(PhaseGeneratorBlockEntity.SLOT_LIQUID_IN,
                new ItemStack(ModItems.MATTER_UNIT_PHASE.get(), 2));
        // Le tampon est rempli pour que rien ne soit brule pendant la fonte : sinon
        // la cuve se viderait au fur et a mesure et le compte serait faux.
        generator.setEnergy(6000.0d);

        // Deux ticks suffisent : une unite par tick.
        PhaseGeneratorBlockEntity.tick(level, abs, helper.getBlockState(rel), generator);
        PhaseGeneratorBlockEntity.tick(level, abs, helper.getBlockState(rel), generator);

        assertValue(helper, 2000, generator.getLiquidAmount(), "deux unites fondues");
        ItemStack empties = generator.getInventory().getStackInSlot(PhaseGeneratorBlockEntity.SLOT_LIQUID_OUT);
        assertValue(helper, ModItems.MATTER_UNIT.get(), empties.getItem(), "unite vide rendue");
        assertValue(helper, 2, empties.getCount(), "deux unites vides rendues");
        helper.succeed();
    }

    /** Le palier de texture suit le remplissage de la cuve. */
    @GameTest(template = "empty")
    public static void phaseGeneratorShowsItsTankOnTheBlock(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.PHASE_GENERATOR.get());
        var generator = (PhaseGeneratorBlockEntity) helper.getBlockEntity(rel);

        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(rel);

        assertValue(helper, 0, PhaseGeneratorBlockEntity.levelFor(0), "cuve vide");
        assertValue(helper, 2, PhaseGeneratorBlockEntity.levelFor(4000), "cuve a moitie");
        assertValue(helper, 4, PhaseGeneratorBlockEntity.levelFor(8000), "cuve pleine");

        generator.setLiquidAmount(8000);
        generator.setEnergy(6000.0d);
        for (int i = 0; i < 10; i++) {
            PhaseGeneratorBlockEntity.tick(level, abs, helper.getBlockState(rel), generator);
        }

        assertValue(helper, 4, helper.getBlockState(rel).getValue(PhaseGeneratorBlock.LEVEL),
                "le bloc doit montrer la cuve pleine");
        helper.succeed();
    }

    /** Le test de bout en bout : le generateur alimente le noeud. */
    @GameTest(template = "empty")
    public static void phaseGeneratorFeedsItsNode(GameTestHelper helper) {
        BlockPos nodeRel = new BlockPos(1, 1, 0);
        BlockPos generatorRel = new BlockPos(1, 1, 2);

        helper.setBlock(nodeRel, ModBlocks.NODE_BASIC.get());
        helper.setBlock(generatorRel, ModBlocks.PHASE_GENERATOR.get());
        var node = (NodeBlockEntity) helper.getBlockEntity(nodeRel);
        var generator = (PhaseGeneratorBlockEntity) helper.getBlockEntity(generatorRel);

        ServerLevel level = helper.getLevel();
        BlockPos nodeAbs = helper.absolutePos(nodeRel);
        BlockPos generatorAbs = helper.absolutePos(generatorRel);

        for (int i = 0; i < 100; i++) {
            PhaseGeneratorBlockEntity.tick(level, generatorAbs, helper.getBlockState(generatorRel), generator);
        }
        assertValue(helper, nodeAbs, ImagNetworkData.get(level).nodeOf(generatorAbs),
                "le generateur doit avoir trouve le noeud tout seul");

        // On fixe la cuve et le tampon : la production reelle depend de la quantite
        // de phase restante, qu'on ne veut pas avoir a suivre ici.
        generator.setLiquidAmount(8000);
        node.setEnergy(0.0d);

        for (int i = 0; i < 20; i++) {
            PhaseGeneratorBlockEntity.tick(level, generatorAbs, helper.getBlockState(generatorRel), generator);
            NodeBlockEntity.serverTick(level, nodeAbs, helper.getBlockState(nodeRel), node);
        }

        assertClose(helper, 1000.0d, node.getEnergy(), "20 ticks a 50 d'energie");
        assertValue(helper, 6000, generator.getLiquidAmount(),
                "2000 mB doivent avoir brule, soit 1000 d'energie");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // Le brouilleur d'aptitudes
    // ------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void interfererValuesAndRangeSettings(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.ABILITY_INTERFERER.get());
        var machine = (AbilityInterfererBlockEntity) helper.getBlockEntity(rel);

        // Valeurs de TileAbilityInterferer : tampon 10000, bande passante
        // LATENCY_MK1 = 50, rayon entre 10 et 100.
        assertValue(helper, 10000, machine.getMaxEnergyStored(), "tampon du brouilleur");
        assertClose(helper, 50.0d, machine.getBandwidth(), "bande passante");
        assertValue(helper, 10, AbilityInterfererBlockEntity.MIN_RANGE, "rayon minimum");
        assertValue(helper, 100, AbilityInterfererBlockEntity.MAX_RANGE, "rayon maximum");

        assertFalse(helper, machine.isEnabled(), "la machine demarre a l'arret");
        assertValue(helper, AbilityInterfererBlockEntity.MIN_RANGE, machine.getRange(), "rayon de depart");

        // Le rayon se paie au carre : c'est ce qui rend un grand brouilleur cher.
        assertClose(helper, 100.0d, machine.getCycleCost(), "cout a 10 blocs");
        assertClose(helper, 10.0d, machine.getCostPerTick(), "cout par tick a 10 blocs");
        machine.setRange(50);
        assertClose(helper, 2500.0d, machine.getCycleCost(), "cout a 50 blocs");
        assertClose(helper, 250.0d, machine.getCostPerTick(), "cout par tick a 50 blocs");

        // Le rayon reste dans ses bornes.
        machine.setRange(5);
        assertValue(helper, AbilityInterfererBlockEntity.MIN_RANGE, machine.getRange(), "borne basse");
        machine.setRange(500);
        assertValue(helper, AbilityInterfererBlockEntity.MAX_RANGE, machine.getRange(), "borne haute");

        machine.adjustRange(AbilityInterfererBlockEntity.RANGE_STEP);
        assertValue(helper, AbilityInterfererBlockEntity.MAX_RANGE, machine.getRange(),
                "au-dela du maximum, le reglage ne bouge plus");
        helper.succeed();
    }

    /** Un cycle paie le rayon au carre et allume le bloc. */
    @GameTest(template = "empty")
    public static void interfererPaysForItsRange(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.ABILITY_INTERFERER.get());
        var machine = (AbilityInterfererBlockEntity) helper.getBlockEntity(rel);

        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(rel);

        machine.setEnergy(10000.0d);
        machine.setEnabled(true);
        assertTrue(helper, machine.isEnabled(), "avec de quoi payer, la machine s'allume");

        // Dix ticks pour un cycle complet.
        for (int i = 0; i < 10; i++) {
            AbilityInterfererBlockEntity.tick(level, abs, helper.getBlockState(rel), machine);
        }

        assertClose(helper, 9900.0d, machine.getEnergy(), "un cycle coute le carre du rayon");
        assertValue(helper, true, helper.getBlockState(rel).getValue(AbilityInterfererBlock.ON),
                "le bloc doit s'allumer");
        helper.succeed();
    }

    /** Sans de quoi payer, la machine s'eteint d'elle-meme au lieu de brouiller gratuitement. */
    @GameTest(template = "empty")
    public static void interfererStopsWhenItCannotPay(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.ABILITY_INTERFERER.get());
        var machine = (AbilityInterfererBlockEntity) helper.getBlockEntity(rel);

        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(rel);

        // Moins que les 100 du cycle a dix blocs.
        machine.setEnergy(50.0d);
        machine.setEnabled(true);
        assertFalse(helper, machine.isEnabled(), "sans de quoi payer un cycle, elle refuse de s'allumer");

        // De quoi payer un cycle, mais pas deux.
        machine.setEnergy(150.0d);
        machine.setEnabled(true);
        assertTrue(helper, machine.isEnabled(), "un cycle payable suffit a s'allumer");

        for (int i = 0; i < 10; i++) {
            AbilityInterfererBlockEntity.tick(level, abs, helper.getBlockState(rel), machine);
        }
        assertClose(helper, 50.0d, machine.getEnergy(), "le premier cycle est paye");
        assertTrue(helper, machine.isEnabled(), "elle a encore de quoi payer");

        for (int i = 0; i < 10; i++) {
            AbilityInterfererBlockEntity.tick(level, abs, helper.getBlockState(rel), machine);
        }
        assertFalse(helper, machine.isEnabled(), "le deuxieme cycle n'est pas payable");
        assertClose(helper, 0.0d, machine.getEnergy(), "l'original vidait le tampon en s'eteignant");
        assertValue(helper, false, helper.getBlockState(rel).getValue(AbilityInterfererBlock.ON),
                "le bloc doit s'eteindre");
        helper.succeed();
    }

    /** Le poseur n'est jamais brouille par sa propre machine. */
    @GameTest(template = "empty")
    public static void interfererNeverJamsItsOwner(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        var state = ModBlocks.ABILITY_INTERFERER.get().defaultBlockState();
        helper.setBlock(rel, ModBlocks.ABILITY_INTERFERER.get());
        var machine = (AbilityInterfererBlockEntity) helper.getBlockEntity(rel);

        var owner = fakePlayer(helper);
        machine.setPlacer(owner);
        assertValue(helper, owner.getGameProfile().getName(), machine.getPlacer(), "le poseur est retenu");

        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(rel);

        // On place le faux joueur dans le rayon, puis on fait tourner la machine.
        owner.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        level.addFreshEntity(owner);

        machine.setEnergy(10000.0d);
        machine.setEnabled(true);
        for (int i = 0; i < 10; i++) {
            AbilityInterfererBlockEntity.tick(level, abs, state, machine);
        }

        assertValue(helper, 0, machine.getAffectedCount(), "le poseur ne doit pas etre brouille");
        assertFalse(helper, isInterfered(owner), "et il doit pouvoir utiliser ses competences");

        // Le faux joueur est un singleton du niveau, partage par tous les tests : il
        // faut le retirer, sinon le prochain test le retrouve la ou on l'a laisse.
        //
        // Mais remove() fait deux degats qu'il faut reparer, sous peine de casser des
        // tests qui n'ont l'air de rien avoir a faire avec celui-ci :
        //   - il appelle invalidateCaps(), qui rend TOUTES les capacites muettes ;
        //   - il marque la creature comme retiree, donc isAlive() reste faux pour
        //     toujours, et un developpeur qui la cherche comme eleve echoue.
        // revive() repare les deux (il defait le retrait et reactive les capacites).
        // On l'eloigne ensuite pour qu'aucun test ne le retrouve dans une zone ou il
        // cherche un joueur.
        owner.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        owner.revive();
        owner.moveTo(100_000.0d, 100.0d, 100_000.0d);
        helper.succeed();
    }

    /** Vrai si ce joueur est brouille, d'apres sa donnee d'aptitudes. */
    private static boolean isInterfered(net.minecraft.world.entity.player.Player player) {
        return player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .map(cn.academy.ability.AbilityData::isInterfered)
                .orElse(false);
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
        assertTrue(helper, cn.academy.Config.controlPointStart >= 0.0d,
                "controlPointStart non charge depuis la config : "
                        + cn.academy.Config.controlPointStart);
        assertTrue(helper, cn.academy.Config.controlPointRegenSpeed >= 0.0d,
                "controlPointRegenSpeed non charge depuis la config : "
                        + cn.academy.Config.controlPointRegenSpeed);
        // La reserve, elle, ne vient plus d'un reglage plat mais des tables de l'original :
        // c'est la valeur du niveau 1 qui doit etre celle de l'init_cp, pas un 0 par defaut.
        assertTrue(helper, cn.academy.ability.AbilityData.baseMaxControlPoint(1) == 1800f,
                "plafond de reserve du niveau 1 : "
                        + cn.academy.ability.AbilityData.baseMaxControlPoint(1));
        assertTrue(helper, cn.academy.ability.AbilityData.baseMaxControlPoint(5) == 8000f,
                "plafond de reserve du niveau 5 : "
                        + cn.academy.ability.AbilityData.baseMaxControlPoint(5));
        assertTrue(helper, cn.academy.Config.damageScale >= 0.0d,
                "damageScale non charge depuis la config : " + cn.academy.Config.damageScale);
        assertTrue(helper, cn.academy.Config.controlPointRecoverCooldown >= 0,
                "controlPointRecoverCooldown non charge depuis la config : "
                        + cn.academy.Config.controlPointRecoverCooldown);
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // Le terminal de donnees
    // ------------------------------------------------------------------

    /** L'objet d'installation installe le terminal, une seule fois, et se consomme. */
    @GameTest(template = "empty")
    public static void terminalInstallerInstallsOnce(GameTestHelper helper) {
        Player player = fakePlayer(helper);
        player.getAbilities().instabuild = false;
        resetTerminal(player);

        assertTrue(helper, terminalOf(player) != null,
                "la capacite du terminal doit etre attachee au joueur");
        assertTrue(helper, ModItems.TERMINAL_INSTALLER.get() instanceof ModItems.TerminalInstallerItem,
                "l'objet doit etre celui qui installe, pas un objet inerte");
        assertFalse(helper, terminalInstalled(player), "un joueur sans terminal ne doit rien avoir");

        ItemStack installer = new ItemStack(ModItems.TERMINAL_INSTALLER.get(), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, installer);
        installer.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        assertTrue(helper, terminalInstalled(player), "l'objet doit installer le terminal");
        assertValue(helper, 2, installer.getCount(), "un objet sur trois doit etre consomme");

        // Deuxieme usage : le terminal est deja la, donc rien de plus et rien de consomme.
        installer.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        assertValue(helper, 2, installer.getCount(), "un terminal deja installe ne consomme plus rien");

        resetTerminal(player);
        helper.succeed();
    }

    /** En creatif, l'objet installe mais n'est pas consomme. */
    @GameTest(template = "empty")
    public static void terminalInstallerDoesNotConsumeInCreative(GameTestHelper helper) {
        Player player = fakePlayer(helper);
        resetTerminal(player);
        player.getAbilities().instabuild = true;

        ItemStack installer = new ItemStack(ModItems.TERMINAL_INSTALLER.get(), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, installer);
        installer.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        assertTrue(helper, terminalInstalled(player), "l'installation doit avoir lieu");
        assertValue(helper, 3, installer.getCount(), "en creatif l'objet reste");

        player.getAbilities().instabuild = false;
        resetTerminal(player);
        helper.succeed();
    }

    /**
     * Sans terminal, un objet d'application refuse de s'installer.
     *
     * C'est la regle de l'original : un objet d'application se garde tant que le
     * terminal n'est pas installe, sinon on le perdrait pour rien.
     */
    @GameTest(template = "empty")
    public static void appInstallerNeedsTheTerminal(GameTestHelper helper) {
        Player player = fakePlayer(helper);
        player.getAbilities().instabuild = false;
        resetTerminal(player);

        ItemStack app = new ItemStack(ModItems.APP_SKILL_TREE.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, app);
        app.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        assertFalse(helper, appInstalled(player), "sans terminal, l'application ne s'installe pas");
        assertValue(helper, 2, app.getCount(), "et l'objet n'est pas consomme");

        helper.succeed();
    }

    /** Avec le terminal, l'application s'installe une fois, et l'objet se consomme. */
    @GameTest(template = "empty")
    public static void appInstallerInstallsTheAppOnce(GameTestHelper helper) {
        Player player = fakePlayer(helper);
        player.getAbilities().instabuild = false;
        resetTerminal(player);

        ItemStack installer = new ItemStack(ModItems.TERMINAL_INSTALLER.get(), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, installer);
        installer.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        assertTrue(helper, terminalInstalled(player), "le terminal doit s'installer d'abord");

        ItemStack app = new ItemStack(ModItems.APP_SKILL_TREE.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, app);
        app.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        assertTrue(helper, appInstalled(player), "l'application doit s'installer");
        assertValue(helper, 1, app.getCount(), "un objet sur deux doit etre consomme");

        // Deuxieme usage : deja installee, donc rien de plus et rien de consomme.
        app.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        assertValue(helper, 1, app.getCount(), "une application deja installee ne consomme plus rien");

        resetTerminal(player);
        helper.succeed();
    }

    /** Les applications portees sont enregistrees, et elles seules. */
    @GameTest(template = "empty")
    public static void appRegistryContainsThePortedApps(GameTestHelper helper) {
        var registry = cn.academy.terminal.AppRegistry.INSTANCE;
        var about = cn.academy.terminal.app.AppAbout.INSTANCE;
        var skillTree = cn.academy.terminal.app.AppSkillTree.INSTANCE;
        var tutorial = cn.academy.terminal.app.AppTutorial.INSTANCE;
        var settings = cn.academy.terminal.app.AppSettings.INSTANCE;
        var mediaPlayer = cn.academy.terminal.app.AppMediaPlayer.INSTANCE;

        assertTrue(helper, registry.isBaked(), "le registre doit etre ferme apres l'initialisation");

        // L'ordre du registre donne les identifiants : il suit les priorites de
        // l'original, « A propos » en premier, et une application s'ajoute a la fin.
        assertValue(helper, 5, registry.size(), "cinq applications sont portees");
        assertValue(helper, "about", registry.get(0).getName(), "la premiere application");
        assertValue(helper, "skill_tree", registry.get(1).getName(), "la deuxieme application");
        assertValue(helper, "tutorial", registry.get(2).getName(), "la troisieme application");
        assertValue(helper, "settings", registry.get(3).getName(), "la quatrieme application");
        assertValue(helper, "media_player", registry.get(4).getName(), "la cinquieme application");
        assertValue(helper, 0, about.getAppId(), "l'identifiant vient de l'ordre d'enregistrement");
        assertValue(helper, 1, skillTree.getAppId(), "l'identifiant vient de l'ordre d'enregistrement");
        assertValue(helper, 2, tutorial.getAppId(), "l'identifiant vient de l'ordre d'enregistrement");
        assertValue(helper, 3, settings.getAppId(), "l'identifiant vient de l'ordre d'enregistrement");
        assertValue(helper, 4, mediaPlayer.getAppId(), "l'identifiant vient de l'ordre d'enregistrement");

        assertTrue(helper, registry.getByName("about") == about,
                "la recherche par nom doit rendre la meme instance");
        assertTrue(helper, registry.getByName("skill_tree") == skillTree,
                "la recherche par nom doit rendre la meme instance");
        assertTrue(helper, registry.getByName("tutorial") == tutorial,
                "la recherche par nom doit rendre la meme instance");
        assertTrue(helper, registry.getByName("settings") == settings,
                "la recherche par nom doit rendre la meme instance");
        assertTrue(helper, registry.getByName("media_player") == mediaPlayer,
                "la recherche par nom doit rendre la meme instance");

        assertTrue(helper, about.isPreInstalled(),
                "a propos s'installe d'office : sinon le terminal s'ouvre sur une grille vide");
        assertFalse(helper, skillTree.isPreInstalled(),
                "l'arbre s'installe avec un objet, pas d'office");
        assertTrue(helper, tutorial.isPreInstalled(),
                "MisakaCloud s'installe d'office, comme dans l'original : c'est la documentation");
        assertTrue(helper, settings.isPreInstalled(),
                "Settings s'installe d'office, comme dans l'original : sans cela, son objet"
                        + " n'ayant aucune recette, l'ecran de reglage du mod serait introuvable");

        assertValue(helper, "ac.app.about.name", about.getDisplayKey(), "cle de langue du nom");
        assertValue(helper, "ac.app.skill_tree.name", skillTree.getDisplayKey(), "cle de langue du nom");
        assertValue(helper, "ac.app.tutorial.name", tutorial.getDisplayKey(), "cle de langue du nom");
        assertValue(helper, "ac.app.settings.name", settings.getDisplayKey(), "cle de langue du nom");
        assertValue(helper, "ac.app.media_player.name", mediaPlayer.getDisplayKey(), "cle de langue du nom");

        // Et les applications qui n'ont pas de contenu a montrer ne sont pas enregistrees :
        // une icone qui n'ouvre rien serait pire qu'une icone absente. L'Emetteur de
        // frequence n'a plus d'objet utilisable depuis la 1.20.1, le raccordement s'y
        // faisant tout seul.
        assertTrue(helper, registry.getByName("freq_transmitter") == null,
                "les applications non portees ne doivent pas etre enregistrees");
        helper.succeed();
    }

    /**
     * Un objet de musique donne son morceau, et seulement dans les formes.
     *
     * <p>L'original repondait trois choses a ce clic droit : « tu n'as pas l'application »,
     * « tu l'as deja », ou « le voila ». Le port garde les trois, et le morceau ne se perd
     * qu'une fois — c'est ce que ce test suit, du refus jusqu'a la liste.
     */
    @GameTest(template = "empty")
    public static void unMorceauSInstalleParSonObjet(GameTestHelper helper) {
        Player player = ownPlayer(helper, "media");
        player.getAbilities().instabuild = false;

        // Sans l'application, l'objet se garde : c'est la premiere reponse de l'original.
        ItemStack track = new ItemStack(ModItems.MEDIA_RAILGUN.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, track);
        track.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        assertFalse(helper, mediaOf(player).isInstalled("only_my_railgun"),
                "sans l'application, le morceau ne s'installe pas");
        assertValue(helper, 2, track.getCount(), "et l'objet n'est pas consomme");

        // Le terminal, puis l'application : l'objet installe alors le morceau.
        ItemStack installer = new ItemStack(ModItems.TERMINAL_INSTALLER.get(), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, installer);
        installer.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        ItemStack app = new ItemStack(ModItems.APP_MEDIA_PLAYER.get(), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, app);
        app.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        assertTrue(helper, appInstalled(player) || installedApp(player, "media_player"),
                "l'application du lecteur doit s'installer");

        player.setItemInHand(InteractionHand.MAIN_HAND, track);
        track.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        assertTrue(helper, mediaOf(player).isInstalled("only_my_railgun"),
                "le morceau doit s'installer");
        assertValue(helper, 1, track.getCount(), "un objet sur deux doit etre consomme");

        // Et une deuxieme fois ne donne rien de plus.
        track.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        assertValue(helper, 1, track.getCount(), "un morceau deja possede ne consomme plus rien");
        assertValue(helper, 1, mediaOf(player).count(), "et la liste ne grandit pas");
        helper.succeed();
    }

    /** Les morceaux d'un joueur, ou un etat vide s'il n'a pas la capacite. */
    private static cn.academy.misc.media.MediaAcquireData mediaOf(Player player) {
        return player.getCapability(cn.academy.misc.media.MediaCapability.MEDIA_DATA)
                .map(cn.academy.misc.media.MediaCapability.Holder::get)
                .orElseGet(cn.academy.misc.media.MediaAcquireData::new);
    }

    /** Si ce joueur a installe cette application, par son nom. */
    private static boolean installedApp(Player player, String name) {
        var data = terminalOf(player);
        var app = cn.academy.terminal.AppRegistry.INSTANCE.getByName(name);
        return data != null && app != null && data.isInstalled(app);
    }

    /**
     * Les credits livres avec le mod sont lisibles au moment ou le jeu tourne.
     *
     * Le test JUnit lit le meme fichier depuis le classpath ; celui-ci verifie en
     * plus qu'il est bien dans les ressources du mod, ce qui n'est pas la meme
     * chose : un fichier present dans les sources peut manquer a l'assemblage.
     */
    @GameTest(template = "empty")
    public static void aboutCreditsArePackaged(GameTestHelper helper) {
        var document = cn.academy.terminal.about.AboutDocument.load();

        assertFalse(helper, document.isEmpty(), "le document de credits doit se lire dans le jeu");
        assertTrue(helper, document.getHeader().size() == 2, "l'en-tete de l'original fait deux lignes");
        assertTrue(helper, document.getStaff().size() >= 8, "l'equipe compte au moins huit roles");
        assertTrue(helper, document.getDonators().size() >= 93,
                "la liste des donateurs de l'original fait 93 noms, une liste tronquee doit se voir");
        assertTrue(helper, !document.toLines("info").isEmpty(), "la mise en page doit produire des lignes");
        helper.succeed();
    }

    /**
     * Les seize succes du mod sont la, et leurs quinze declencheurs aussi.
     *
     * <p>Un succes n'est qu'un fichier de donnees : celui qui cite un declencheur
     * inconnu ne fait pas tomber le jeu, il reste simplement <b>impossible a obtenir</b> —
     * et personne ne s'en apercoit avant d'avoir joue une partie entiere. Ce test le voit
     * en une seconde, et il verifie les deux moities du contrat : que chaque fichier est
     * charge, et que chaque declencheur qu'il cite est enregistre.
     */
    @GameTest(template = "empty")
    public static void lesSuccesDuModSontTousLa(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        assertTrue(helper, server != null, "un serveur doit etre la");

        var ours = server.getAdvancements().getAllAdvancements().stream()
                .filter(advancement -> advancement.getId().getNamespace().equals("academy"))
                .toList();
        assertValue(helper, 16, ours.size(), "les seize succes de l'original");

        // Chaque declencheur cite par un fichier doit repondre dans la table : c'est
        // exactement ce qu'un fichier mal orthographie casserait en silence.
        for (var advancement : ours) {
            for (var criterion : advancement.getCriteria().entrySet()) {
                // Le declencheur « impossible » de la racine n'est pas de la meme famille :
                // il ne porte pas d'identifiant, et c'est normal.
                if (!(criterion.getValue().getTrigger()
                        instanceof net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance instance)) {
                    continue;
                }
                var id = instance.getCriterion();
                assertTrue(helper, net.minecraft.advancements.CriteriaTriggers.getCriterion(id) != null,
                        "le declencheur doit etre enregistre : " + advancement.getId());
                if (id.getNamespace().equals("academy")) {
                    assertValue(helper, advancement.getId().getPath(), id.getPath(),
                            "le succes " + advancement.getId().getPath() + " cite son propre declencheur");
                }
            }
        }

        // Et la racine porte le fond d'ecran : sans lui, l'onglet des succes est vide.
        var root = server.getAdvancements().getAdvancement(cn.academy.advancements.AcademyAdvancements.id("root"));
        assertTrue(helper, root != null, "la racine doit exister");
        assertTrue(helper, root.getDisplay() != null && root.getDisplay().getBackground() != null,
                "la racine doit avoir un fond");
        helper.succeed();
    }

    /**
     * Les seize succes sont branchables, et Forge refuse de les accorder a un faux joueur.
     *
     * <p>Deux verites en une, parce qu'elles vont ensemble. La premiere : chaque fichier de
     * succes doit produire un critere <b>rattachable</b> a un declencheur enregistre — c'est
     * ce que le jeu fait a la connexion d'un joueur, et un fichier dont le declencheur ne
     * repond pas ne donnerait jamais rien. La seconde : la remise elle-meme s'arrete a la
     * porte de Forge, qui refuse les succes aux faux joueurs.
     *
     * <p>Le port ne peut donc pas prouver ici qu'un succes arrive dans l'inventaire d'un
     * joueur : cela se voit en partie. Ce qu'il prouve, c'est que la chaine est complete
     * jusqu'a cette porte — les fichiers se chargent, les criteres se rattachent, les
     * declencheurs s'appellent.
     */
    @GameTest(template = "empty")
    public static void lesSuccesSontBranchables(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var player = ownPlayer(helper, "branchement");

        int branchables = connectAdvancements(player, server.getAdvancements(),
                cn.academy.AcademyCraft.MOD_ID);
        assertValue(helper, 16, branchables, "les seize succes doivent avoir un critere branchable");

        var level3 = server.getAdvancements()
                .getAdvancement(cn.academy.advancements.AcademyAdvancements.id("ac_level_3"));
        assertTrue(helper, level3 != null, "le succes doit exister");

        // Le declencheur s'appelle sans broncher — mais Forge refuse la remise. C'est la
        // porte de Forge, pas le port : le test la nomme pour que personne ne la confonde
        // avec une regression le jour ou un vrai joueur, lui, recevra bien son succes.
        cn.academy.advancements.AcademyAdvancements.award(player,
                cn.academy.advancements.AcademyAdvancements.AC_LEVEL_3);
        assertFalse(helper, player.getAdvancements().getOrStartProgress(level3).isDone(),
                "Forge refuse les succes aux faux joueurs");
        assertFalse(helper, player.getAdvancements().award(level3, "ac_level_3"),
                "et c'est bien sa porte : la remise directe echoue aussi");
        helper.succeed();
    }

    /**
     * Branche les ecouteurs de succes de ce joueur, comme le fait une vraie connexion, et
     * rend le nombre de succes du namespace donne qui ont trouve preneur.
     *
     * <p>Un joueur normal les recoit tout seul : son gestionnaire de succes s'inscrit auprès
     * de chaque declencheur pour chaque critere qui lui reste a obtenir. Un faux joueur
     * n'entre jamais dans la liste des joueurs, donc rien de tout cela n'a lieu — et un
     * declencheur qui ne trouve pas d'auditeur rend la main sans rien dire.
     *
     * <p>La copie est volontaire : c'est exactement ce que fait le jeu, et si le port
     * s'ecartait de ce chemin, ce test ne le verrait plus.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int connectAdvancements(net.minecraft.server.level.ServerPlayer player,
                                           net.minecraft.server.ServerAdvancementManager manager,
                                           String namespace) {
        int connected = 0;
        for (var advancement : manager.getAllAdvancements()) {
            var progress = player.getAdvancements().getOrStartProgress(advancement);
            if (progress.isDone()) continue;
            boolean any = false;
            for (var entry : advancement.getCriteria().entrySet()) {
                var criterionProgress = progress.getCriterion(entry.getKey());
                if (criterionProgress == null || criterionProgress.isDone()) continue;
                var instance = entry.getValue().getTrigger();
                if (instance == null) continue;
                var trigger = net.minecraft.advancements.CriteriaTriggers.getCriterion(instance.getCriterion());
                if (trigger == null) continue;
                ((net.minecraft.advancements.CriterionTrigger) trigger).addPlayerListener(
                        player.getAdvancements(),
                        new net.minecraft.advancements.CriterionTrigger.Listener(
                                instance, advancement, entry.getKey()));
                any = true;
            }
            if (any && advancement.getId().getNamespace().equals(namespace)) connected++;
        }
        return connected;
    }

    /** Le terminal d'un joueur, ou un etat vide s'il n'a pas la capacite. */
    private static cn.academy.terminal.TerminalData terminalOf(Player player) {
        return player.getCapability(cn.academy.terminal.TerminalCapability.TERMINAL_DATA).orElse(null);
    }

    private static boolean terminalInstalled(Player player) {
        var data = terminalOf(player);
        return data != null && data.isTerminalInstalled();
    }

    private static boolean appInstalled(Player player) {
        var data = terminalOf(player);
        return data != null && data.isInstalled(cn.academy.terminal.app.AppSkillTree.INSTANCE);
    }
    /**
     * Rend le faux joueur a son etat initial.
     *
     * La fabrique de faux joueurs rend un singleton du niveau : le terminal d'un
     * test reste installe pour le suivant. Sans cette remise a zero, le test qui
     * verifie qu'un objet d'application refuse de s'installer sans terminal
     * passerait ou echouerait selon l'ordre d'execution.
     */
    private static void resetTerminal(Player player) {
        var data = terminalOf(player);
        if (data != null) data.reset();
    }

    /**
     * Le missile atteint aussi ce qui est dans le COIN de sa portee.
     *
     * <p>L'original ne filtrait que par boite : une cible a 6,4 blocs sur la diagonale est
     * atteignable avec une portee de 5, alors qu'un filtre spherique la refuserait. C'est la
     * difference que le joueur a sentie comme une portee trop courte, et ce test la fige.
     */
    @GameTest(template = "empty")
    public static void leMissileAtteintLeCoinDeSaPortee(GameTestHelper helper) {
        var skill = cn.academy.ability.meltdowner.MeltdownerCategory.ELECTRON_MISSILE;
        ServerLevel level = helper.getLevel();
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), 200);

        var player = ownPlayer(helper, "corner_missileer");
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);
        clearCorridor(helper, abs, 10);

        // 4,5 sur deux axes : plus loin que les 5 blocs de portee au depart, mais dans la
        // boite, et bien le seul vivant a portee.
        var zombie = new net.minecraft.world.entity.monster.Zombie(
                net.minecraft.world.entity.EntityType.ZOMBIE, level);
        zombie.moveTo(abs.getX() + 5.0, abs.getY(), abs.getZ() + 5.0, 0f, 0f);
        level.addFreshEntity(zombie);
        float before = zombie.getHealth();

        var data = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .resolve().orElseThrow();
        data.setCategoryLevel(skill.getCategory(), 5);
        data.learnSkill(skill);

        data.beginCharge(skill);
        skill.onStart(player, data);
        for (int tick = 0; tick <= 8; tick++) {
            assertTrue(helper, skill.onHoldTick(player, data, tick), "le maintien doit tenir");
        }

        assertTrue(helper, zombie.getHealth() < before,
                "une cible en coin doit etre atteinte : " + zombie.getHealth() + " contre " + before);

        zombie.discard();
        data.endCharge(skill);
        helper.succeed();
    }

    /**
     * La bombe a electrons : une bille devant les yeux, puis son rayon.
     *
     * <p>Le port faisait une explosion instantanee d'un rayon de trois blocs au point vise, et
     * ce n'etait pas ce que fait l'original : sa bille flottait une seconde a cote du porteur,
     * puis tirait <b>d'elle-meme</b> un rayon vers ce que le regard touche, et ne frappait que
     * ce que ce rayon rencontrait. Le test suit ces deux temps : la bille est la des le lancer,
     * et c'est elle qui frappe.
     *
     * <p>L'experience est poussee a fond pour abreger : passe 80 %, la bille ne vit plus que
     * cinq ticks et tire au troisieme, au lieu d'une seconde pleine.
     *
     * <p>La cible est posee <b>douze</b> blocs devant, et c'est un detail qui compte : la bille
     * se tient a un ecart tire au hasard, jusqu'a 1,3 bloc de cote, et c'est de <b>la</b> que
     * part le rayon. Plus la cible est loin, plus ce decalage se resserre sur le regard — a
     * douze blocs il ne reste que la moitie d'un bloc, et la cible est donc touchee quel que
     * soit le tirage. Plus pres, le test dependrait du hasard.
     */
    @GameTest(template = "empty")
    public static void laBombeAElectronsLacheUneBilleQuiTireSonRayon(GameTestHelper helper) {
        var skill = cn.academy.ability.meltdowner.MeltdownerCategory.ELECTRON_BOMB;
        ServerLevel level = helper.getLevel();
        int height = 120;
        BlockPos floor = new BlockPos(2, 1, 2);
        BlockPos abs = aboveTestArea(helper, floor, height);
        BlockPos eyes = new BlockPos(floor.getX(), floor.getY() + height + 1, floor.getZ());

        // Le couloir se creuse dans la pierre : sans cela le premier bloc devant le regard
        // arreterait le rayon, et la cible posee plus loin ne serait jamais atteinte.
        clearCorridor(helper, abs, 16);
        for (int dz = 0; dz <= 16; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -1; dy <= 3; dy++) {
                    helper.setBlock(eyes.offset(dx, dy, dz),
                            net.minecraft.world.level.block.Blocks.AIR);
                }
            }
        }

        var player = ownPlayer(helper, "bomber");
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);

        // La cible se pose exactement la ou le regard touche, et ce n'est pas un caprice : la
        // bille nait a un ecart tire au hasard — jusqu'a 1,3 bloc de cote — et son rayon part de
        // LA. Posee ailleurs, la cible serait touchee ou manquee selon ce tirage. La ou le regard
        // touche, la ligne de la bille y aboutit toujours.
        var aim = cn.academy.ability.TargetingUtil.findImpactPoint(player,
                cn.academy.ability.meltdowner.ElectronBombSkill.RANGE);

        var zombie = new net.minecraft.world.entity.monster.Zombie(
                net.minecraft.world.entity.EntityType.ZOMBIE, level);
        zombie.moveTo(aim.x, abs.getY(), aim.z, 0f, 0f);
        level.addFreshEntity(zombie);
        float before = zombie.getHealth();

        var data = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .resolve().orElseThrow();
        data.setCategoryLevel(skill.getCategory(), 5);
        data.learnSkill(skill);
        data.setSkillExp(skill, 1f);

        skill.onActivate(player, data);

        var balls = level.getEntitiesOfClass(cn.academy.entity.EntityMdBall.class,
                new net.minecraft.world.phys.AABB(abs).inflate(4));
        assertValue(helper, 1, balls.size(), "la bombe lache une bille de plasma");
        var ball = balls.get(0);
        assertTrue(helper, ball.position().distanceTo(player.position()) > 0.5,
                "qui flotte a cote de son porteur, pas dans ses pieds : " + ball.position());

        // Le tir part de la BILLE, pas de l'oeil : la cible doit donc etre dans sa ligne, de la
        // bille jusqu'ou le regard touche. On la verifie avant de tirer, sans quoi un echec ne
        // dirait pas si c'est le tir ou la ligne qui est en faute.
        net.minecraft.world.phys.Vec3 from = new net.minecraft.world.phys.Vec3(
                ball.getX(), ball.getY() + player.getEyeHeight(), ball.getZ());
        net.minecraft.world.phys.Vec3 to = cn.academy.ability.TargetingUtil.findImpactPoint(
                player, cn.academy.ability.meltdowner.ElectronBombSkill.RANGE);
        var seen = cn.academy.ability.TargetingUtil.findEntityAlong(player, from, to,
                e -> !(e instanceof cn.academy.entity.EntityMdBall));
        assertTrue(helper, seen == zombie, "la cible est dans la ligne de la bille : vu "
                + (seen == null ? "rien" : seen.getName().getString()) + " ; bille a "
                + ball.position() + ", cible a " + zombie.position() + ", vise " + to);

        for (int tick = 0; tick < 5 && ball.isAlive(); tick++) {
            ball.tick();
        }

        assertFalse(helper, ball.isAlive(), "et qui s'efface au bout de sa vie");
        assertTrue(helper, zombie.getHealth() < before,
                "apres avoir tire son rayon : " + zombie.getHealth() + " contre " + before);

        zombie.discard();
        helper.succeed();
    }

    /**
     * Le missile a electrons : il accumule ses billes, puis en envoie une sur le plus proche.
     *
     * <p>Le test appelle les ticks du maintien directement — c'est la forme que le serveur utilise,
     * et la seule qui rende l'attaque deterministe : la premiere bille tombe au tick 0, le premier
     * tir au tick 8, sur une cible posee a trois blocs.
     *
     * <p>Les billes sont de <b>vraies entites</b>, celles de la bombe a electrons : le missile ne
     * les comptait avant que dans l'etat du maintien, donc le joueur ne voyait rien tourner autour
     * de lui. C'est ce que la premiere assertion fige.
     */
    @GameTest(template = "empty")
    public static void leMissileAElectronsTireSesBilles(GameTestHelper helper) {
        var skill = cn.academy.ability.meltdowner.MeltdownerCategory.ELECTRON_MISSILE;
        ServerLevel level = helper.getLevel();
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), 160);

        var player = ownPlayer(helper, "missileer");
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);
        clearCorridor(helper, abs, 8);

        var zombie = new net.minecraft.world.entity.monster.Zombie(
                net.minecraft.world.entity.EntityType.ZOMBIE, level);
        zombie.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 3.5, 0f, 0f);
        level.addFreshEntity(zombie);
        float before = zombie.getHealth();

        var data = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .resolve().orElseThrow();
        data.setCategoryLevel(skill.getCategory(), 5);
        data.learnSkill(skill);

        data.beginCharge(skill);
        skill.onStart(player, data);
        assertTrue(helper, skill.onHoldTick(player, data, 0), "le maintien doit tenir");
        assertValue(helper, 1, ballsOf(helper, player).size(),
                "le missile pose une vraie bille de plasma, comme la bombe a electrons");
        for (int tick = 1; tick <= 8; tick++) {
            assertTrue(helper, skill.onHoldTick(player, data, tick), "le maintien doit tenir");
        }

        assertTrue(helper, zombie.getHealth() < before,
                "le missile doit avoir frappe : " + zombie.getHealth() + " contre " + before);
        assertValue(helper, 0, ballsOf(helper, player).size(), "la bille envoyee est consommee");
        assertClose(helper, 0.001d, data.getSkillExp(skill), "et le tir verse son experience");

        zombie.discard();
        data.endCharge(skill);
        helper.succeed();
    }

    /**
     * Le vol du reacteur, vu du serveur : la trajectoire posee, et l'elan qui en sort.
     *
     * <p>C'est ce que le joueur a signale : le vol paraissait lent, il s'arretait net sur sa cible,
     * et il repartait a zero au lieu de garder un peu de vitesse. La cause etait un teleport a
     * chaque tick — un paquet de position ne s'interpole pas, donc la copie cliente sautait, et la
     * quantite de mouvement posee mourait dans le teleport. C'est le piege que le deplacement
     * magnetique connaissait deja.
     *
     * <p>Ce que ce test fige est le contrat du serveur : il <b>pose</b> la position sur la
     * trajectoire et la vitesse avec elle, tick apres tick, et il ne les efface pas au dernier. Le
     * quinzieme tick doit encore porter la vitesse du vol : c'est elle qui fait depasser la cible,
     * et c'est elle que le porteur garde en sortant.
     */
    @GameTest(template = "empty")
    public static void leVolDuReacteurGardeSaVitesse(GameTestHelper helper) {
        var skill = cn.academy.ability.meltdowner.MeltdownerCategory.JET_ENGINE;
        ServerLevel level = helper.getLevel();
        // Une altitude a lui, et libre : les autres tests se sont deja partage quarante, quatre-
        // vingts, cent a trois cent soixante-dix — sans compter celles qu'ils portent dans une
        // variable, voir les « int height ». Ce test creuse dans la pierre, donc viser la hauteur
        // d'un voisin reviendrait a lui effacer son decor en pleine execution. Cent quatre-vingt-
        // dix blocs plus haut, on est encore dans le monde : au-dela de trois cent vingt, les
        // ecritures ne vont plus nulle part et ne le disent pas.
        BlockPos abs = aboveTestArea(helper, new BlockPos(2, 1, 2), 190);

        // Cent quatre-vingt-dix blocs plus haut, on est dans la pierre : la chambre et son couloir
        // sont creuses a la main, sinon la visee du relachement s'arrete au premier bloc et la
        // cible se confond avec le depart. Le monde des tests est sauvegarde, donc on nettoie
        // aussi ce qu'une execution ratee y a laisse.
        for (int dz = -3; dz <= 15; dz++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -2; dy <= 3; dy++) {
                    level.setBlockAndUpdate(abs.offset(dx, dy, dz),
                            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
            }
        }

        var player = ownPlayer(helper, "jet_engineer");
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);

        var data = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .resolve().orElseThrow();
        data.setCategoryLevel(skill.getCategory(), 4);
        data.learnSkill(skill);

        // Le relachement epingle la trajectoire — celle-la meme que le client recevra.
        assertTrue(helper, skill.onRelease(player, data, 10), "le relachement lance le vol");
        net.minecraft.world.phys.Vec3 start = data.getHoldOrigin(skill);
        net.minecraft.world.phys.Vec3 target = data.getHoldPoint(skill);
        assertTrue(helper, start != null && target != null, "les deux bouts sont poses");
        assertTrue(helper, target.distanceTo(start) > 8.0,
                "la visee part dans l'air, pas dans la pierre : " + target.distanceTo(start));

        double flown = target.distanceTo(start);
        net.minecraft.world.phys.Vec3 velocity =
                cn.academy.ability.meltdowner.JetEngineSkill.flightVelocity(start, target);

        // La chute est posee a la main : sans cela, l'effacer ne prouverait rien.
        player.fallDistance = 4.5f;

        // Premier tick : le porteur est sur la trajectoire, et la vitesse est posee.
        assertTrue(helper, skill.onHoldTick(player, data, 11), "le vol demarre");
        assertClose(helper, velocity.length(), player.position().distanceTo(start),
                "le porteur est pose sur la trajectoire");
        assertClose(helper, velocity.length(), player.getDeltaMovement().length(),
                "la quantite de mouvement du vol est posee");
        assertClose(helper, 0.0, player.fallDistance, "et la chute est effacee");

        int last = 10 + cn.academy.ability.meltdowner.JetEngineSkill.LIFETIME;
        for (int tick = 12; tick <= last; tick++) {
            assertTrue(helper, skill.onHoldTick(player, data, tick), "le vol tient au tick " + tick);
        }

        // Le dernier tick : la vitesse est toujours la, et le vol a bel et bien depasse sa cible
        // de presque toute la distance visee — c'est ce qui en fait un deplacement et non une
        // teleportation.
        assertClose(helper, velocity.length(), player.getDeltaMovement().length(),
                "la quantite de mouvement tient jusqu'au dernier tick");
        assertClose(helper, 15.0 / 8.0 * flown, player.position().distanceTo(start),
                "et le vol depasse sa cible de presque la distance visee");

        // Un tick de plus : le maintien se termine, et rien n'efface l'elan.
        assertFalse(helper, skill.onHoldTick(player, data, last + 1),
                "le vol se termine au-dela de sa vie");
        assertClose(helper, velocity.length(), player.getDeltaMovement().length(),
                "le porteur garde son elan en sortant");

        helper.succeed();
    }

    /**
     * L'experience du passif de radiation, telle que le joueur l'a relevee sur l'original.
     *
     * <p>Le passif tire son experience de la reserve : son plafond, rapporte a celui du niveau 5.
     * Le detail qui compte est que l'original lisait ce plafond-la par {@code CPData.getInitCP},
     * qui declenchait le meme evenement que la reserve — si bien que les bonus des cursus
     * s'ajoutent aux DEUX cotes de la division. Le joueur a mesure 31,1 % au premier cours et
     * 41 % au second, la ou un denominateur reste a 8000 aurait donne 35 % et 53,8 %.
     */
    @GameTest(template = "empty")
    public static void laRadiationSuitLaReserveEtSesBonus(GameTestHelper helper) {
        var meltdowner = cn.academy.ability.meltdowner.MeltdownerCategory.INSTANCE;
        var rad = cn.academy.ability.meltdowner.MeltdownerCategory.RADIATION_INTENSIFY;
        var brain = meltdowner.getSkill("brain_course");
        var advanced = meltdowner.getSkill("brain_course_advanced");

        var player = ownPlayer(helper, "radiation_reader");
        var data = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .resolve().orElseThrow();
        data.setCategoryLevel(meltdowner, 1);
        data.learnSkill(rad);

        // Sans les cursus : 1800 sur les 8000 du niveau 5, soit un peu moins d'un quart.
        assertClose(helper, 1800d / 8000d, rad.computeExp(data), "au depart, 22,5 %");
        // Avec le premier cours : (1800 + 1000) / (8000 + 1000) = 31,1 %.
        data.learnSkill(brain);
        assertClose(helper, 2800d / 9000d, rad.computeExp(data),
                "le cours de cerveau ajoute ses 1000 des deux cotes");
        // Et avec le cours avance : (1800 + 2500) / (8000 + 2500) = 41 %.
        data.learnSkill(advanced);
        assertClose(helper, 4300d / 10500d, rad.computeExp(data),
                "et le cours avance ses 1500, toujours des deux cotes");

        helper.succeed();
    }

    /**
     * La marque de radiation alourdit les coups recus, puis s'efface.
     *
     * <p>Portage de {@code MDDamageHelper} : une cible touchee par un tir du meltdowner porte la
     * marque pendant soixante ticks, et tout degat qu'elle encaisse pendant ce temps est
     * multiplie. Le test frappe un zombie avec un evenement de degats construit a la main plutot
     * qu'avec une arme : c'est le seul moyen de chiffrer exactement le facteur, et de verifier
     * qu'un coup sans marque n'est pas touche du tout.
     */
    @GameTest(template = "empty")
    public static void laMarqueDeRadiationAlourditLesCoups(GameTestHelper helper) {
        var rad = cn.academy.ability.meltdowner.MeltdownerCategory.RADIATION_INTENSIFY;

        var player = ownPlayer(helper, "radiator");
        var data = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .resolve().orElseThrow();
        data.setCategoryLevel(rad.getCategory(), 1);
        data.learnSkill(rad);

        var zombie = new net.minecraft.world.entity.monster.Zombie(
                net.minecraft.world.entity.EntityType.ZOMBIE, helper.getLevel());
        helper.getLevel().addFreshEntity(zombie);

        // Sans marque, un coup de dix points reste dix points.
        var plain = new net.minecraftforge.event.entity.living.LivingHurtEvent(zombie,
                player.damageSources().playerAttack(player), 10f);
        cn.academy.ability.AbilityEvents.onLivingHurt(plain);
        assertClose(helper, 10d, plain.getAmount(), "sans marque, le coup n'est pas touche");

        // Avec la marque, le meme coup est multiplie par le facteur du passif.
        cn.academy.ability.meltdowner.RadiationMarks.mark(zombie, data);
        assertTrue(helper, cn.academy.ability.meltdowner.RadiationMarks.isMarked(zombie),
                "un tir du meltdowner marque sa cible");
        var marked = new net.minecraftforge.event.entity.living.LivingHurtEvent(zombie,
                player.damageSources().playerAttack(player), 10f);
        cn.academy.ability.AbilityEvents.onLivingHurt(marked);
        assertClose(helper, 10d * rad.rate(data), marked.getAmount(),
                "la marque multiplie les degats recus");

        // Et elle s'efface toute seule, au bout des soixante ticks de l'original.
        for (int i = 0; i < cn.academy.ability.meltdowner.RadiationIntensifySkill.MARK_TICKS; i++) {
            cn.academy.ability.meltdowner.RadiationMarks.tick(zombie);
        }
        assertTrue(helper, !cn.academy.ability.meltdowner.RadiationMarks.isMarked(zombie),
                "la marque doit expirer");
        var after = new net.minecraftforge.event.entity.living.LivingHurtEvent(zombie,
                player.damageSources().playerAttack(player), 10f);
        cn.academy.ability.AbilityEvents.onLivingHurt(after);
        assertClose(helper, 10d, after.getAmount(), "et le coup redevient normal");

        zombie.discard();
        helper.succeed();
    }

    /**
     * Les trois cursus generiques donnent leurs bonus au joueur qui les apprend.
     *
     * <p>{@code PortedSkillsTest} fige les valeurs ; ce test-ci verifie l'autre bout, celui que
     * le joueur voit : que sa reserve les compte. C'est la seule facon de le prouver, le registre
     * des categories etant vide en test unitaire — et c'est bien l'absence de ces trois
     * competences que le joueur a signalee.
     */
    @GameTest(template = "empty")
    public static void lesCursusGeneriquesGonflentLaReserve(GameTestHelper helper) {
        var category = cn.academy.ability.CategoryManager.INSTANCE.getCategory("electromaster");
        var brain = category.getSkill("brain_course");
        var advanced = category.getSkill("brain_course_advanced");
        var mind = category.getSkill("mind_course");

        var player = ownPlayer(helper, "course_reader");
        var data = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .resolve().orElseThrow();

        // Le niveau donne la reserve du niveau, pleine : c'est l'etat d'avant les cursus.
        data.setCategoryLevel(category, 5);
        double reserve = data.getMaxControlPoint();
        double overload = data.getMaxOverload();

        data.learnSkill(brain);
        assertClose(helper, reserve + 1000d, data.getMaxControlPoint(),
                "le cours de cerveau ajoute 1000 points de reserve");
        assertClose(helper, 1d, data.getPassiveRecoverScale(),
                "et ne touche pas a la recuperation");

        data.learnSkill(advanced);
        assertClose(helper, reserve + 2500d, data.getMaxControlPoint(),
                "le cours avance ajoute 1500 de plus");
        assertClose(helper, overload + 100d, data.getMaxOverload(),
                "et 100 de surcout");

        data.learnSkill(mind);
        assertClose(helper, 1.2d, data.getPassiveRecoverScale(),
                "l'entrainement mental accelere la recuperation d'un cinquieme");

        helper.succeed();
    }

    /**
     * Les commandes de debogage de l'original, bout en bout.
     *
     * <p>Le test tape les vraies commandes dans le distributeur du serveur ({@code /aim ...})
     * et relit la donnee du joueur : c'est la seule facon de verifier le cablage Brigadier,
     * qu'aucune compilation ne voit. Un noeud mal attache, une sous-commande oubliee ou un
     * argument mal nomme ne se decouvriraient qu'a la main.
     *
     * <p>Il couvre aussi la regle du <b>seul pouvoir</b> : prendre le vecteur manipulation
     * doit lacher l'electromaster, ses competences et son niveau — c'est ce que faisait
     * {@code setCategory} de l'original, qui remplacait la categorie du joueur.
     */
    @GameTest(template = "empty")
    public static void lesCommandesAimFontCeQuEllesDisent(GameTestHelper helper) {
        var electromaster = cn.academy.ability.CategoryManager.INSTANCE.getCategory("electromaster");
        var vecmanip = cn.academy.ability.CategoryManager.INSTANCE.getCategory("vecmanip");
        var railgun = electromaster.getSkill("railgun");

        var player = ownPlayer(helper, "aim_commander");
        var data = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .resolve().orElseThrow();
        var source = player.createCommandSourceStack();
        var commands = helper.getLevel().getServer().getCommands();

        // /aim arrive desarmee : c'est son premier controle, et cheats_on le dit.
        commands.performPrefixedCommand(source, "aim cheats_on");

        commands.performPrefixedCommand(source, "aim cat electromaster");
        assertValue(helper, 1, data.getCategoryLevel(electromaster),
                "la categorie adoptee est posee au niveau 1, sinon l'aptitude reste eteinte");

        commands.performPrefixedCommand(source, "aim learn railgun");
        assertTrue(helper, data.isSkillLearned(railgun), "la competence doit etre apprise");
        assertClose(helper, 0d, data.getSkillExp(railgun), "elle part sans experience");

        commands.performPrefixedCommand(source, "aim exp railgun 1");
        assertClose(helper, 1d, data.getSkillExp(railgun), "l'experience se pose telle quelle");

        commands.performPrefixedCommand(source, "aim level 5");
        assertValue(helper, 5, data.getCategoryLevel(electromaster), "le niveau se pose");

        commands.performPrefixedCommand(source, "aim fullcp");
        assertClose(helper, data.getMaxControlPoint(), data.getControlPoint(),
                "la reserve se remplit jusqu'a son plafond");
        assertClose(helper, 0d, data.getOverload(), "et le surcout retombe a zero");

        // Un pouvoir, un seul : le second lache le premier, et tout ce qu'il savait.
        commands.performPrefixedCommand(source, "aim cat vecmanip");
        assertTrue(helper, data.getCategoryLevel(vecmanip) > 0, "le nouveau pouvoir est pose");
        assertFalse(helper, data.hasLearned(electromaster),
                "le premier pouvoir doit etre oublie : on ne peut pas en porter deux");
        assertFalse(helper, data.isSkillLearned(railgun), "et ses competences avec");

        helper.succeed();
    }
}
