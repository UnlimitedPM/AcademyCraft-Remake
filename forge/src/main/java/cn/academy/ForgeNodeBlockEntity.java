package cn.academy;

import cn.academy.energy.NodeBlockEntity;
import cn.academy.platform.storage.ForgeItemHandlerAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Noeud sans fil cote Forge/NeoForge : la logique est commune, cette classe ne rajoute que les
 * capabilities (objets et Forge Energy). La conversion reste 1:1 avec l'unite du reseau Imag.
 */
public class ForgeNodeBlockEntity extends NodeBlockEntity {
    private final LazyOptional<IItemHandler> itemHandlerCap;
    private final LazyOptional<IEnergyStorage> energyCap;

    public ForgeNodeBlockEntity(BlockEntityType<?> type, MenuType<NodeMenu> menuType,
                                BlockPos pos, BlockState state) {
        super(type, menuType, pos, state);
        this.itemHandlerCap = LazyOptional.of(() -> new ForgeItemHandlerAdapter(getInventory()));
        this.energyCap = LazyOptional.of(EnergyAdapter::new);
    }

    private final class EnergyAdapter implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (toReceive <= 0) return 0;
            double accepted = Math.min(toReceive, getMaxEnergy() - getEnergy());
            if (accepted <= 0.0d) return 0;
            if (!simulate) {
                setEnergy(getEnergy() + accepted);
                setChanged();
            }
            return (int) accepted;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            if (toExtract <= 0) return 0;
            double given = Math.min(toExtract, getEnergy());
            if (given <= 0.0d) return 0;
            if (!simulate) {
                setEnergy(getEnergy() - given);
                setChanged();
            }
            return (int) given;
        }

        @Override
        public int getEnergyStored() {
            return (int) getEnergy();
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) getMaxEnergy();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemHandlerCap.cast();
        if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerCap.invalidate();
        energyCap.invalidate();
    }
}
