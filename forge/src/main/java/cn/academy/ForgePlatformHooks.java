package cn.academy;

import cn.academy.platform.PlatformHooks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/**
 * Installation des crochets Forge/NeoForge pour le code de common/.
 *
 * <p>NeoForge 1.20.1 garde les packages net.minecraftforge : cette classe compile et fonctionne
 * sur les deux chargeurs.
 */
public final class ForgePlatformHooks {

    private ForgePlatformHooks() {
    }

    public static void install() {
        PlatformHooks.installExternalEnergyProbe(stack -> !stack.isEmpty()
                && stack.getCapability(ForgeCapabilities.ENERGY).isPresent());
    }
}
