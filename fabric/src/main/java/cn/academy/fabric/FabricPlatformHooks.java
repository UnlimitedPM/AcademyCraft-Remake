package cn.academy.fabric;

import cn.academy.platform.PlatformHooks;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Crochets Fabric pour le code de common/.
 *
 * <p>Fabric n'a pas de NetworkHooks : pour transporter une position jusqu'au client, on passe un
 * {@link ExtendedScreenHandlerFactory} qui ecrit les memes donnees que le {@code buf.writeBlockPos}
 * des menus Forge.
 */
final class FabricPlatformHooks {

    private FabricPlatformHooks() {
    }

    static void install() {
        PlatformHooks.installMenuOpener(FabricPlatformHooks::openMenu);
    }

    private static void openMenu(ServerPlayer player, MenuProvider provider,
                                 java.util.function.Consumer<FriendlyByteBuf> writer) {
        player.openMenu(new ExtendedScreenHandlerFactory() {
            @Override
            public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
                writer.accept(buf);
            }

            @Override
            public Component getDisplayName() {
                return provider.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p) {
                return provider.createMenu(containerId, inventory, p);
            }
        });
    }
}
