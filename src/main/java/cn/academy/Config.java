package cn.academy;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

/**
 * Configuration du mod (fichier {@code config/academy-common.toml}).
 *
 * Portage de l'ancien {@code config/academy-craft-data.conf} (HOCON) de la 1.12.2.
 * L'original exposait beaucoup plus de reglages via un systeme de donnees complexe
 * (courbes de CP par niveau, difficultes par entite pour VecManip, activation
 * competence par competence...). On ne garde ici que les reglages que le port
 * honore reellement, plutot que d'exposer des valeurs mortes.
 *
 * Les champs statiques sont initialises a leur valeur par defaut : un appel avant
 * le chargement de la config (datapack, biome modifier) reste donc coherent.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Config {

    private Config() {}

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ------------------------------------------------------------------
    // General
    // ------------------------------------------------------------------
    private static final ForgeConfigSpec.BooleanValue GEN_ORES = BUILDER
            .comment("Genere les minerais d'AcademyCraft dans l'Overworld.",
                     "Equivalent de genOres dans la 1.12.2.")
            .define("general.generateOres", true);

    private static final ForgeConfigSpec.BooleanValue GEN_PHASE_LIQUID = BUILDER
            .comment("Genere des poches de liquide Imag Phase sous terre.",
                     "Equivalent de genPhaseLiquid dans la 1.12.2.")
            .define("general.generatePhaseLiquid", true);

    private static final ForgeConfigSpec.DoubleValue DAMAGE_SCALE = BUILDER
            .comment("Multiplicateur global des degats infliges par les competences.",
                     "Equivalent de ac.ability.calc_global.damage_scale dans la 1.12.2.")
            .defineInRange("general.damageScale", 1.0d, 0.0d, 1000.0d);

    // ------------------------------------------------------------------
    // Ability (Control Points)
    // ------------------------------------------------------------------
    private static final ForgeConfigSpec.DoubleValue CP_MAX = BUILDER
            .comment("Control Points maximum d'un joueur.")
            .defineInRange("ability.controlPointMax", 100.0d, 1.0d, 1_000_000.0d);

    private static final ForgeConfigSpec.DoubleValue CP_START = BUILDER
            .comment("Control Points au premier lancement (plafonnes a controlPointMax).")
            .defineInRange("ability.controlPointStart", 100.0d, 0.0d, 1_000_000.0d);

    private static final ForgeConfigSpec.DoubleValue CP_REGEN_PER_TICK = BUILDER
            .comment("Control Points regeneres par tick (20 ticks = 1 seconde).",
                     "0.25 = 5 CP par seconde, la valeur de la 1.12.2.")
            .defineInRange("ability.controlPointRegenPerTick", 0.25d, 0.0d, 1000.0d);

    private static final ForgeConfigSpec.IntValue CP_SYNC_INTERVAL = BUILDER
            .comment("Frequence (en ticks) de synchronisation des CP vers le client.")
            .defineInRange("ability.controlPointSyncInterval", 20, 1, 200);

    private static final ForgeConfigSpec.IntValue OVERLOAD_RECOVER_COOLDOWN = BUILDER
            .comment("Ticks d'attente avant que le surcout ne redescende.",
                     "Repris de ac.ability.data.overload_recover_cooldown de la 1.12.2.")
            .defineInRange("ability.overloadRecoverCooldown", 32, 0, 20_000);

    private static final ForgeConfigSpec.DoubleValue OVERLOAD_RECOVER_SPEED = BUILDER
            .comment("Vitesse de recuperation du surcout. 1.0 = celle de la 1.12.2,",
                     "soit environ un dixieme de seconde par point sur une reserve pleine.")
            .defineInRange("ability.overloadRecoverSpeed", 1.0d, 0.0d, 1000.0d);

    /**
     * Ce que l'electromaster attire, repris de {@code normalMetalBlocks} de l'original.
     *
     * Ces blocs s'attrapent a tout niveau d'experience.
     */
    private static final List<String> DEFAULT_METAL_BLOCKS = List.of(
            "minecraft:rail",
            "minecraft:iron_bars",
            "minecraft:iron_block",
            "minecraft:activator_rail",
            "minecraft:detector_rail",
            "minecraft:golden_rail",
            "minecraft:sticky_piston",
            "minecraft:piston");

    /**
     * Les blocs faiblement metalliques, repris de {@code weakMetalBlocks}.
     *
     * Il faut soixante pour cent d'experience pour s'y accrocher : une machine ou un
     * minerai de fer ne s'attrapent pas du premier coup.
     */
    private static final List<String> DEFAULT_WEAK_METAL_BLOCKS = List.of(
            "minecraft:dispenser",
            "minecraft:hopper",
            "minecraft:iron_ore",
            "minecraft:deepslate_iron_ore");

    /** Les entites metalliques, reprises de {@code metalEntities}. */
    private static final List<String> DEFAULT_METAL_ENTITIES = List.of(
            "minecraft:minecart",
            "minecraft:chest_minecart",
            "minecraft:furnace_minecart",
            "minecraft:tnt_minecart",
            "minecraft:hopper_minecart",
            "minecraft:spawner_minecart",
            "minecraft:commandblock_minecart",
            "minecraft:villager_golem");

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> METAL_BLOCKS = BUILDER
            .comment("Blocs franchement metalliques, que l'electromaster attire a tout niveau.",
                     "Noms de blocs, comme minecraft:iron_block.",
                     "Repris de normalMetalBlocks de la 1.12.2.")
            .defineList("ability.metalBlocks", DEFAULT_METAL_BLOCKS, o -> o instanceof String);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> WEAK_METAL_BLOCKS = BUILDER
            .comment("Blocs faiblement metalliques, attires a partir de 60 % d'experience.",
                     "Repris de weakMetalBlocks de la 1.12.2.")
            .defineList("ability.weakMetalBlocks", DEFAULT_WEAK_METAL_BLOCKS, o -> o instanceof String);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> METAL_ENTITIES = BUILDER
            .comment("Entites metalliques, que l'electromaster peut attirer.",
                     "Repris de metalEntities de la 1.12.2.")
            .defineList("ability.metalEntities", DEFAULT_METAL_ENTITIES, o -> o instanceof String);

    private static final ForgeConfigSpec.DoubleValue PROGRESS_INCR_RATE = BUILDER
            .comment("Vitesse de progression des niveaux d'aptitude.",
                     "1.0 = la valeur de la 1.12.2 : il faut remplir un palier en utilisant",
                     "ses competences avant de pouvoir monter d'un niveau. Augmenter ce",
                     "nombre raccourcit l'attente.")
            .defineInRange("ability.progressIncrRate", 1.0d, 0.01d, 1000.0d);

    private static final ForgeConfigSpec.BooleanValue DESTROY_BLOCKS = BUILDER
            .comment("Autorise les competences a casser des blocs.",
                     "Portage de generic.destroyBlocks de la 1.12.2 : les competences qui",
                     "effacent des blocs le consultent avant de le faire. Seules celles qui",
                     "le demandaient chez l'original le lisent — l'onde de choc pour",
                     "l'instant ; les rayons miniers, eux, cassaient sans rien demander.")
            .define("general.destroyBlocks", true);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> AFFECTED_ENTITIES = BUILDER
            .comment("Entites que vecmanip peut devier ou reflechir, avec leur difficulte.",
                     "Ecriture : \"<id>=<difficulte>\", par exemple minecraft:arrow=1.0 ;",
                     "un id seul vaut la difficulte par defaut, 1.0.",
                     "La difficulte multiplie le cout en surcout et l'experience gagnee.",
                     "Repris de ac.ability.category.vecmanip.common.affected_entities du 1.12.2,",
                     "dont la liste d'objets n'a pas d'equivalent dans une config plate.")
            .defineList("ability.affectedEntities",
                    List.of("minecraft:arrow=1.0", "minecraft:potion=1.4", "minecraft:snowball=0.1"),
                    o -> o instanceof String);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_ENTITIES = BUILDER
            .comment("Entites que vecmanip ne touche jamais.",
                     "Des ids d'entites, plus deux mots-cles : \"living\" (tout ce qui vit)",
                     "et \"mob\" (les creatures). Repris de ...common.affected_entities.excluded,",
                     "ou les deux mots-cles s'ecrivaient deja ainsi.")
            .defineList("ability.excludedEntities",
                    List.of("minecraft:item", "minecraft:xp_bottle", "living", "mob"),
                    o -> o instanceof String);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    // Valeurs en cache, relues a chaque chargement/rechargement de la config.
    public static boolean generateOres = true;
    public static boolean generatePhaseLiquid = true;
    public static double damageScale = 1.0d;

    public static double controlPointMax = 100.0d;
    public static double controlPointStart = 100.0d;
    public static double controlPointRegenPerTick = 0.25d;
    public static int controlPointSyncInterval = 20;
    public static int overloadRecoverCooldown = 32;
    public static double overloadRecoverSpeed = 1.0d;
    public static double progressIncrRate = 1.0d;
    public static boolean destroyBlocks = true;

    /** Les listes de metaux, relues a chaque chargement de config. */
    public static List<String> metalBlocks = List.of();
    public static List<String> weakMetalBlocks = List.of();
    public static List<String> metalEntities = List.of();

    /** Ce que vecmanip peut devier, et ce qu'il ne touche jamais. */
    public static List<String> affectedEntities = List.of();
    public static List<String> excludedEntities = List.of();

    /** Valeur de {@link #controlPointStart}, clampee sous le maximum. */
    public static float startingControlPoint() {
        return (float) Math.min(controlPointStart, controlPointMax);
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        generateOres = GEN_ORES.get();
        generatePhaseLiquid = GEN_PHASE_LIQUID.get();
        damageScale = DAMAGE_SCALE.get();
        controlPointMax = CP_MAX.get();
        controlPointStart = CP_START.get();
        controlPointRegenPerTick = CP_REGEN_PER_TICK.get();
        controlPointSyncInterval = CP_SYNC_INTERVAL.get();
        overloadRecoverCooldown = OVERLOAD_RECOVER_COOLDOWN.get();
        overloadRecoverSpeed = OVERLOAD_RECOVER_SPEED.get();
        progressIncrRate = PROGRESS_INCR_RATE.get();
        destroyBlocks = DESTROY_BLOCKS.get();
        // Une nouvelle instance de liste a chaque chargement : c'est ce que
        // MetalTargets surveille pour reconstruire ses ensembles.
        metalBlocks = List.copyOf(METAL_BLOCKS.get());
        weakMetalBlocks = List.copyOf(WEAK_METAL_BLOCKS.get());
        metalEntities = List.copyOf(METAL_ENTITIES.get());
        affectedEntities = List.copyOf(AFFECTED_ENTITIES.get());
        excludedEntities = List.copyOf(EXCLUDED_ENTITIES.get());
    }

    /**
     * Validation d'un identifiant d'item, conservee du template MDK : utile des
     * qu'on exposera des listes d'items en config (comme l'ancien
     * {@code affected_entities} de VecManip).
     */
    @SuppressWarnings("unused")
    private static boolean validateItemName(final Object obj) {
        return obj instanceof final String itemName
                && net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(ResourceLocation.tryParse(itemName));
    }
}
