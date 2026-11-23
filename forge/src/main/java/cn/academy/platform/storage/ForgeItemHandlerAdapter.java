package cn.academy.platform.storage;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * Adaptateur inverse : expose un {@link ItemStorage} neutre comme {@code IItemHandler} Forge.
 *
 * <p>Necessaire pour les capabilities : le block entity garde un stockage commun, et Forge ne
 * voit que cette vue.
 */
public final class ForgeItemHandlerAdapter implements IItemHandlerModifiable {
    private final ItemStorage storage;

    public ForgeItemHandlerAdapter(ItemStorage storage) {
        this.storage = storage;
    }

    @Override
    public int getSlots() {
        return storage.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return storage.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return storage.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return storage.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return storage.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return storage.isItemValid(slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        storage.setStackInSlot(slot, stack);
    }
}
