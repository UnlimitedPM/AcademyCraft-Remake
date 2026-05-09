package cn.academy;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;

public class ModItemProperties {
    public static void addCustomItemProperties() {
        // Version corrigée sans warning : on fusionne les deux strings avec ":"
        ItemProperties.register(ModItems.ENERGY_UNIT.get(),
                new ResourceLocation(AcademyCraft.MOD_ID + ":energy"),
                (stack, level, entity, seed) -> {
                    if (stack.getTag() != null && stack.getTag().contains("ac_energy")) {
                        float energy = stack.getTag().getFloat("ac_energy");
                        float maxEnergy = 10000.0f;
                        float ratio = energy / maxEnergy;

                        if (ratio >= 1.0f) return 1.0f; // Texture "full"
                        if (ratio >= 0.1f) return 0.5f; // Texture "half"
                    }
                    return 0.0f; // Texture "empty"
                });
    }
}