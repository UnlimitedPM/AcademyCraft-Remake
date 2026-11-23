package cn.academy.platform.storage;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiPredicate;
import java.util.function.IntUnaryOperator;

/**
 * Inventaire simple et neutre, equivalent de l'{@code ItemStackHandler} de Forge.
 *
 * <p>Suffisant pour les machines du mod : un nombre fixe d'emplacements, une limite par
 * emplacement, un predicat de validite et une notification de changement.
 */
public class SimpleItemStorage implements ItemStorage {
    private final NonNullList<ItemStack> stacks;
    private final IntUnaryOperator slotLimit;
    private final BiPredicate<Integer, ItemStack> validator;
    private final Runnable onChanged;

    public SimpleItemStorage(int size) {
        this(size, slot -> 64, (slot, stack) -> true, () -> {
        });
    }

    public SimpleItemStorage(int size, IntUnaryOperator slotLimit,
                             BiPredicate<Integer, ItemStack> validator, Runnable onChanged) {
        this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
        this.slotLimit = slotLimit;
        this.validator = validator;
        this.onChanged = onChanged;
    }

    @Override
    public int getSlots() {
        return stacks.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return stacks.get(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        ItemStack current = stacks.get(slot);
        int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
        if (!current.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(current, stack)) return stack;
            limit -= current.getCount();
            if (limit <= 0) return stack;
        }

        int inserted = Math.min(limit, stack.getCount());
        if (!simulate) {
            if (current.isEmpty()) {
                stacks.set(slot, stack.copyWithCount(inserted));
            } else {
                current.grow(inserted);
            }
            onChanged.run();
        }
        if (inserted >= stack.getCount()) return ItemStack.EMPTY;
        ItemStack rest = stack.copy();
        rest.shrink(inserted);
        return rest;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        ItemStack current = stacks.get(slot);
        if (current.isEmpty()) return ItemStack.EMPTY;

        int taken = Math.min(amount, current.getCount());
        ItemStack result = current.copyWithCount(taken);
        if (!simulate) {
            current.shrink(taken);
            if (current.isEmpty()) stacks.set(slot, ItemStack.EMPTY);
            onChanged.run();
        }
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        return slotLimit.applyAsInt(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return validator.test(slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        stacks.set(slot, stack);
        onChanged.run();
    }

    @Override
    public void setChanged() {
        onChanged.run();
    }

    /** Ecrit les piles dans un tag, sous la meme forme que le vanilla. */
    public void save(CompoundTag tag) {
        ContainerHelper.saveAllItems(tag, stacks);
    }

    /** Relit les piles ecrites par {@link #save(CompoundTag)}. */
    public void load(CompoundTag tag) {
        ContainerHelper.loadAllItems(tag, stacks);
    }
}
