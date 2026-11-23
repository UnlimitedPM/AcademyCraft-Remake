package cn.academy;

import cn.academy.platform.storage.ForgeEnergyStorageAdapter;
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
 * SolarGen cote Forge/NeoForge : la logique et le stockage sont communs, cette classe n'ajoute
 * que les capabilities Forge.
 */
public class ForgeSolarGenBlockEntity extends SolarGenBlockEntity {
    private final LazyOptional<IItemHandler> itemHandlerCap;
    private final LazyOptional<IEnergyStorage> energyCap;

    public ForgeSolarGenBlockEntity(BlockEntityType<?> type, MenuType<SolarGenMenu> menuType,
                                    BlockPos pos, BlockState state) {
        super(type, menuType, pos, state);
        this.itemHandlerCap = LazyOptional.of(() -> new ForgeItemHandlerAdapter(getInventory()));
        this.energyCap = LazyOptional.of(() -> new ForgeEnergyStorageAdapter(energyStorage()));
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
