package cn.academy.fabric;

import cn.academy.AcademyCraftCommon;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Generation du monde sous Fabric.
 *
 * <p>Forge ajoute les minerais par un biome modifier de donnees ; Fabric n'a pas cet
 * equivalent, donc il pose les memes placed features depuis le code. Les fichiers
 * {@code data/academy/worldgen/**} sont partages avec Forge : ce sont eux qui decrivent les veines.
 *
 * <p>Le lac de phase liquide reste hors de ce lot : il demande le bloc de fluide, que Fabric
 * n'enregistre pas encore.
 */
final class FabricWorldgen {

    private static final String[] ORES = {
            "constraint_metal", "crystal_ore", "imagsil_ore", "reso_ore"
    };

    private FabricWorldgen() {
    }

    static void register() {
        for (String ore : ORES) {
            ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE,
                    new ResourceLocation(AcademyCraftCommon.MOD_ID, ore));
            BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD),
                    GenerationStep.Decoration.UNDERGROUND_ORES, key);
        }
    }
}
