package cn.academy.worldgen;

import cn.academy.AcademyCraft;
import cn.academy.Config;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Biome modifier "academy:configured_features" : ajoute une liste de placed
 * features aux biomes, mais seulement si la configuration l'autorise.
 *
 * Pourquoi ne pas utiliser directement {@code forge:add_features} ? Parce qu'un
 * biome modifier JSON ne peut pas lire la config du mod : les options
 * {@code generateOres} et {@code generatePhaseLiquid} de la 1.12.2 n'auraient
 * sinon aucun effet.
 *
 * Format JSON, identique a forge:add_features :
 * <pre>
 * {
 *   "type": "academy:configured_features",
 *   "biomes": "#minecraft:is_overworld",
 *   "features": [ "academy:crystal_ore" ],
 *   "step": "underground_ores",
 *   "onlyWhen": "ores"        // "ores" | "phase_liquid" | "always"
 * }
 * </pre>
 */
public record ConfigurableFeatureBiomeModifier(
        HolderSet<Biome> biomes,
        HolderSet<PlacedFeature> features,
        Decoration step,
        Gate gate) implements BiomeModifier {

    /** Reglage de configuration dont depend l'application du modifier. */
    public enum Gate {
        /** Toujours applique. */
        ALWAYS,
        /** Applique seulement si {@code general.generateOres} est vrai. */
        ORES,
        /** Applique seulement si {@code general.generatePhaseLiquid} est vrai. */
        PHASE_LIQUID;

        public static final Codec<Gate> CODEC = com.mojang.serialization.Codec.STRING
                .xmap(s -> switch (s) {
                    case "ores" -> ORES;
                    case "phase_liquid" -> PHASE_LIQUID;
                    case "always" -> ALWAYS;
                    default -> throw new IllegalArgumentException("Valeur 'onlyWhen' inconnue : " + s);
                }, gate -> switch (gate) {
                    case ALWAYS -> "always";
                    case ORES -> "ores";
                    case PHASE_LIQUID -> "phase_liquid";
                });

        public boolean isOpen() {
            return switch (this) {
                case ALWAYS -> true;
                case ORES -> Config.generateOres;
                case PHASE_LIQUID -> Config.generatePhaseLiquid;
            };
        }
    }

    public static final DeferredRegister<Codec<? extends BiomeModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, AcademyCraft.MOD_ID);

    public static final RegistryObject<Codec<ConfigurableFeatureBiomeModifier>> CODEC = SERIALIZERS.register(
            "configured_features",
            () -> RecordCodecBuilder.create(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(ConfigurableFeatureBiomeModifier::biomes),
                    PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(ConfigurableFeatureBiomeModifier::features),
                    Decoration.CODEC.fieldOf("step").forGetter(ConfigurableFeatureBiomeModifier::step),
                    Gate.CODEC.optionalFieldOf("onlyWhen", Gate.ALWAYS)
                            .forGetter(ConfigurableFeatureBiomeModifier::gate)
            ).apply(builder, ConfigurableFeatureBiomeModifier::new))
    );

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD) return;
        if (!gate.isOpen()) return;
        if (!biomes.contains(biome)) return;
        var generationSettings = builder.getGenerationSettings();
        features.forEach(feature -> generationSettings.addFeature(step, feature));
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return CODEC.get();
    }
}
