package cn.academy;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;

public class ModItemProperties {
    public static void addCustomItemProperties() {
        ItemProperties.register(ModItems.ENERGY_UNIT.get(), new ResourceLocation(AcademyCraft.MOD_ID, "energy"),
                (stack, level, entity, seed) -> {
                    // Ici on simulera la texture pour le test.
                    // Plus tard, on lira la vraie valeur NBT de l'énergie.
                    return 0.0f; // 0.0 = vide, 0.5 = moité, 1.0 = plein
                });
    }
}