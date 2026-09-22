package cn.academy;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

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

    private static final ForgeConfigSpec.DoubleValue PROGRESS_INCR_RATE = BUILDER
            .comment("Vitesse de progression des niveaux d'aptitude.",
                     "1.0 = la valeur de la 1.12.2 : il faut remplir un palier en utilisant",
                     "ses competences avant de pouvoir monter d'un niveau. Augmenter ce",
                     "nombre raccourcit l'attente.")
            .defineInRange("ability.progressIncrRate", 1.0d, 0.01d, 1000.0d);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    // Valeurs en cache, relues a chaque chargement/rechargement de la config.
    public static boolean generateOres = true;
    public static boolean generatePhaseLiquid = true;
    public static double damageScale = 1.0d;

    public static double controlPointMax = 100.0d;
    public static double controlPointStart = 100.0d;
    public static double controlPointRegenPerTick = 0.25d;
    public static int controlPointSyncInterval = 20;
    public static double progressIncrRate = 1.0d;

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
        progressIncrRate = PROGRESS_INCR_RATE.get();
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
