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
        // academy:energy pilote la texture des objets energetiques :
        // 1.0 = pleine, 0.5 = a moitie, 0.0 = vide. Les deux objets rechargeables
        // de l'original (unite d'energie ET developpeur portable) ont leur jeu
        // de textures (empty/half/full) ; sans cette propriete, ils gardent la
        // texture « empty » quel que soit leur niveau.
        ItemProperties.register(ModItems.ENERGY_UNIT.get(), ENERGY_PROPERTY, ModItemProperties::energyState);
        ItemProperties.register(ModItems.DEVELOPER_PORTABLE.get(), ENERGY_PROPERTY,
                ModItemProperties::energyState);
    }

    private static final ResourceLocation ENERGY_PROPERTY =
            ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "energy");

    private static float energyState(net.minecraft.world.item.ItemStack stack,
                                     net.minecraft.client.multiplayer.ClientLevel level,
                                     net.minecraft.world.entity.LivingEntity entity, int seed) {
        float ratio = (float) (cn.academy.energy.EnergyItems.getEnergy(stack)
                / cn.academy.energy.EnergyItems.maxEnergy(stack));
        if (ratio >= 1.0f) return 1.0f;
        if (ratio > 0.0f) return 0.5f;
        return 0.0f;
    }
}
