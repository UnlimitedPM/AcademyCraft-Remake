package cn.academy.platform.storage;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * Adaptateur Forge : expose un {@link IItemHandler} sous l'interface neutre.
 *
 * <p>Vit dans le module forge/ ; NeoForge 1.20.1 garde les packages net.minecraftforge, donc la
 * meme classe sert aux deux chargeurs Forge-like.
 */
public final class ForgeItemStorage implements ItemStorage {
    private final IItemHandler handler;

    private ForgeItemStorage(IItemHandler handler) {
        this.handler = handler;
    }

    public static ItemStorage of(IItemHandler handler) {
        return new ForgeItemStorage(handler);
    }

    public IItemHandler handler() {
        return handler;
    }

    @Override
    public int getSlots() {
        return handler.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return handler.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return handler.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return handler.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return handler.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return handler.isItemValid(slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (handler instanceof IItemHandlerModifiable modifiable) {
            modifiable.setStackInSlot(slot, stack);
        } else {
            throw new UnsupportedOperationException("Stockage Forge non modifiable : " + handler.getClass().getName());
        }
    }
}
