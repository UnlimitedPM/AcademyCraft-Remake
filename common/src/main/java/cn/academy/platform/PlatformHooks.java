package cn.academy.platform;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

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

    /** Ouvre un menu avec les donnees d'ouverture propres au menu (position du bloc, etc.). */
    @FunctionalInterface
    public interface MenuOpener {
        void open(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> writer);
    }

    private static ExternalEnergyProbe externalEnergyProbe = stack -> false;
    private static MenuOpener menuOpener = (player, provider, writer) -> player.openMenu(provider);

    private PlatformHooks() {
    }

    public static void installExternalEnergyProbe(ExternalEnergyProbe probe) {
        externalEnergyProbe = probe;
    }

    public static void installMenuOpener(MenuOpener opener) {
        menuOpener = opener;
    }

    public static boolean hasExternalEnergy(ItemStack stack) {
        return !stack.isEmpty() && externalEnergyProbe.isExternalEnergyItem(stack);
    }

    public static void openMenu(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> writer) {
        menuOpener.open(player, provider, writer);
    }
}
