package cn.academy;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;

/**
 * Proprietes d'item cote client (choix de texture selon l'etat).
 *
 * Doit etre appele depuis {@code FMLClientSetupEvent} : sans cet appel la
 * propriete n'est jamais enregistree et l'objet garde toujours la meme texture.
 */
public class ModItemProperties {

    public static void addCustomItemProperties() {
        // academy:energy pilote la texture de l'unite d'energie :
        // 1.0 = pleine, 0.5 = a moitie, 0.0 = vide.
        ItemProperties.register(ModItems.ENERGY_UNIT.get(),
                ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "energy"),
                (stack, level, entity, seed) -> {
                    float ratio = ModItems.EnergyUnit.getEnergy(stack) / ModItems.EnergyUnit.MAX_ENERGY;
                    if (ratio >= 1.0f) return 1.0f;
                    if (ratio > 0.0f) return 0.5f;
                    return 0.0f;
                });
    }
}
