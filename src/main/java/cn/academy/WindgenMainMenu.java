package cn.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Ecran du rotor de l'eolienne : un seul emplacement, celui de l'helice.
 *
 * Sans helice l'eolienne ne tourne pas, meme si la colonne de piliers est
 * complete et que rien ne gene les pales. C'est le seul endroit ou l'helice
 * s'installe.
 */
public class WindgenMainMenu extends AbstractContainerMenu {

    public static final int SLOT_X = 80;
    public static final int SLOT_Y = 35;

    private static final int MACHINE_SLOTS = 1;

    private final WindgenMainBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public WindgenMainMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, buf.readBlockPos()));
    }

    private static WindgenMainBlockEntity resolveBlockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof WindgenMainBlockEntity rotor) return rotor;
        throw new IllegalStateException("No WindgenMainBlockEntity at " + pos);
    }

    public WindgenMainMenu(int containerId, Inventory playerInventory, WindgenMainBlockEntity blockEntity) {
        super(ModMenus.WINDGEN_MAIN.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        addSlot(new SlotItemHandler(blockEntity.getInventory(), WindgenMainBlockEntity.SLOT_FAN,
                SLOT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.WINDGEN_FAN.get());
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public boolean isFanInstalled() {
        return blockEntity.isFanInstalled();
    }

    public boolean isNoObstacle() {
        return blockEntity.isNoObstacle();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stackInSlot = slot.getItem();
        result = stackInSlot.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stackInSlot, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stackInSlot, 0, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.WINDGEN_MAIN.get());
    }
}
