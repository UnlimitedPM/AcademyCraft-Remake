package cn.academy.platform.storage;

import net.minecraft.world.item.ItemStack;

/**
 * Inventaire neutre, calque sur les methodes communes aux stockages d'objets Forge et Fabric.
 *
 * <p>Le code de common/ et les menus parlent cette interface. Cote Forge, elle est implementee
 * par un adaptateur autour de {@code ItemStackHandler} ; cote Fabric, par un stockage maison.
 */
public interface ItemStorage {

    int getSlots();

    ItemStack getStackInSlot(int slot);

    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    ItemStack extractItem(int slot, int amount, boolean simulate);

    int getSlotLimit(int slot);

    boolean isItemValid(int slot, ItemStack stack);

    void setStackInSlot(int slot, ItemStack stack);

    /** Signale un changement de contenu (sauvegarde, synchronisation). */
    default void setChanged() {
    }
}
