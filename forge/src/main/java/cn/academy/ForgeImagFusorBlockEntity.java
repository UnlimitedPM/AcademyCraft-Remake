package cn.academy;

import cn.academy.platform.storage.ForgeFluidHandlerAdapter;
import cn.academy.platform.storage.ForgeItemHandlerAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Fuseur Imag cote Forge : la logique est commune, seules les capabilities sont ici. */
public class ForgeImagFusorBlockEntity extends ImagFusorBlockEntity {
    private final LazyOptional<IItemHandler> itemHandlerCap;
    private final LazyOptional<IFluidHandler> fluidHandlerCap;

    public ForgeImagFusorBlockEntity(BlockEntityType<?> type, MenuType<ImagFusorMenu> menuType,
                                     BlockPos pos, BlockState state) {
        super(type, menuType, pos, state);
        this.itemHandlerCap = LazyOptional.of(() -> new ForgeItemHandlerAdapter(getInventory()));
        this.fluidHandlerCap = LazyOptional.of(() -> new ForgeFluidHandlerAdapter(getTank()));
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemHandlerCap.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidHandlerCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerCap.invalidate();
        fluidHandlerCap.invalidate();
    }
}
