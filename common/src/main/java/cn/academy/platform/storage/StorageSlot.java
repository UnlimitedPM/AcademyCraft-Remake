package cn.academy.platform.storage;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Emplacement d'interface adosse a un {@link ItemStorage}.
 *
 * <p>Remplace {@code SlotItemHandler} de Forge : les menus n'importent alors plus aucune classe
 * de chargeur. Les sous-classes anonymes des menus continuent de surcharger {@code mayPlace}.
 */
public class StorageSlot extends Slot {
    private final ItemStorage storage;
    private final int slotIndex;

    public StorageSlot(ItemStorage storage, int slotIndex, int x, int y) {
        super(new StorageContainer(storage), slotIndex, x, y);
        this.storage = storage;
        this.slotIndex = slotIndex;
    }

    @Override
    public ItemStack getItem() {
        return storage.getStackInSlot(slotIndex);
    }

    @Override
    public void set(ItemStack stack) {
        storage.setStackInSlot(slotIndex, stack);
        setChanged();
    }

    @Override
    public ItemStack remove(int amount) {
        return storage.extractItem(slotIndex, amount, false);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return storage.isItemValid(slotIndex, stack);
    }

    @Override
    public int getMaxStackSize() {
        return storage.getSlotLimit(slotIndex);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.min(getMaxStackSize(), stack.getMaxStackSize());
    }

    @Override
    public boolean mayPickup(Player player) {
        return !storage.extractItem(slotIndex, 1, true).isEmpty();
    }

    @Override
    public void setChanged() {
        storage.setChanged();
    }

    /** Conteneur minimal exige par le constructeur de {@link Slot}. */
    private static final class StorageContainer implements Container {
        private final ItemStorage storage;

        private StorageContainer(ItemStorage storage) {
            this.storage = storage;
        }

        @Override
        public int getContainerSize() {
            return storage.getSlots();
        }

        @Override
        public boolean isEmpty() {
            for (int slot = 0; slot < storage.getSlots(); slot++) {
                if (!storage.getStackInSlot(slot).isEmpty()) return false;
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return storage.getStackInSlot(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return storage.extractItem(slot, amount, false);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return storage.extractItem(slot, storage.getSlotLimit(slot), false);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            storage.setStackInSlot(slot, stack);
        }

        @Override
        public void setChanged() {
            storage.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (int slot = 0; slot < storage.getSlots(); slot++) {
                storage.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }
}
