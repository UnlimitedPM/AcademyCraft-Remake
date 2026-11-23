package cn.academy;

import cn.academy.platform.PlatformHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.NetworkHooks;

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
        PlatformHooks.installMenuOpener((player, provider, writer) ->
                NetworkHooks.openScreen(player, provider, writer));
        PlatformHooks.installNodeLinker(cn.academy.energy.NodeFinder::ensureLinked);
        PlatformHooks.installExternalEnergyIO(new PlatformHooks.ExternalEnergyIO() {
            @Override
            public int chargeItem(ItemStack stack, int amount, boolean simulate) {
                return stack.getCapability(ForgeCapabilities.ENERGY)
                        .map(storage -> storage.receiveEnergy(amount, simulate))
                        .orElse(0);
            }

            @Override
            public int extractItem(ItemStack stack, int amount, boolean simulate) {
                return stack.getCapability(ForgeCapabilities.ENERGY)
                        .map(storage -> storage.extractEnergy(amount, simulate))
                        .orElse(0);
            }

            @Override
            public int pushToNeighbor(Level level, BlockPos pos, Direction side, int amount, boolean simulate) {
                BlockEntity neighbor = level.getBlockEntity(pos.relative(side));
                if (neighbor == null) return 0;
                return neighbor.getCapability(ForgeCapabilities.ENERGY, side.getOpposite())
                        .map(storage -> storage.receiveEnergy(amount, simulate))
                        .orElse(0);
            }
        });
    }
}
