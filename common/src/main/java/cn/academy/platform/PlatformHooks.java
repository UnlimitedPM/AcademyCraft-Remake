package cn.academy.platform;

import net.minecraft.world.item.ItemStack;

/**
 * Petits crochets de plateforme, installes par chaque module de chargeur.
 *
 * <p>Le code de {@code common/} ne doit jamais importer une API de chargeur. Quand une regle
 * commune a besoin d'une information que seul le chargeur connait, elle passe par ici. Le defaut
 * est neutre : sans installation, aucun objet exterieur n'est reconnu.
 */
public final class PlatformHooks {

    /** Dit si une pile porte un stockage d'energie fourni par le chargeur (Forge Energy, etc.). */
    @FunctionalInterface
    public interface ExternalEnergyProbe {
        boolean isExternalEnergyItem(ItemStack stack);
    }

    private static ExternalEnergyProbe externalEnergyProbe = stack -> false;

    private PlatformHooks() {
    }

    public static void installExternalEnergyProbe(ExternalEnergyProbe probe) {
        externalEnergyProbe = probe;
    }

    public static boolean hasExternalEnergy(ItemStack stack) {
        return !stack.isEmpty() && externalEnergyProbe.isExternalEnergyItem(stack);
    }
}
