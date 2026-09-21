package cn.academy;

import cn.academy.energy.MatrixBlockEntity;
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
 * Ecran du Matrix sans fil.
 *
 * Quatre emplacements, comme le {@code ContainerMatrix} de la 1.12.2 : trois
 * plaques de contrainte et un coeur de matrice. La limite d'un objet par
 * emplacement vient de l'inventaire du block entity, pas du menu.
 *
 * <h2>Ce qui traverse le reseau</h2>
 *
 * Seulement trois nombres : le niveau du coeur, le nombre de plaques et le
 * tampon. Tout le reste (capacite, portee, bande passante) est recalcule par les
 * formules statiques de {@link MatrixBlockEntity}, pour ne pas avoir deux copies
 * des memes chiffres qui pourraient diverger.
 *
 * Ces nombres ne peuvent pas etre lus directement depuis le block entity cote
 * client : le contenu de l'inventaire n'est pas synchronise, un
 * {@code getCoreLevel()} sur le client renverrait toujours 0. D'ou
 * {@link SyncedInt}.
 */
public class MatrixMenu extends AbstractContainerMenu {

    private static final int PLATE_X = 53;
    private static final int PLATE_Y = 35;
    private static final int CORE_X = 125;

    /** Nombre d'emplacements du Matrix, avant les emplacements du joueur. */
    private static final int MACHINE_SLOTS = MatrixBlockEntity.SLOT_PLATE_COUNT + 1;

    private final MatrixBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    private final SyncedInt coreLevel;
    private final SyncedInt plateCount;
    private final SyncedInt buffer;

    public MatrixMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, buf.readBlockPos()));
    }

    private static MatrixBlockEntity resolveBlockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof MatrixBlockEntity matrix) return matrix;
        throw new IllegalStateException("No MatrixBlockEntity at " + pos);
    }

    public MatrixMenu(int containerId, Inventory playerInventory, MatrixBlockEntity blockEntity) {
        super(ModMenus.MATRIX.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        for (int slot = 0; slot < MatrixBlockEntity.SLOT_PLATE_COUNT; slot++) {
            addSlot(new SlotItemHandler(blockEntity.getInventory(), slot, PLATE_X + slot * 18, PLATE_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(ModItems.CONSTRAINT_PLATE.get());
                }
            });
        }

        addSlot(new SlotItemHandler(blockEntity.getInventory(), MatrixBlockEntity.SLOT_CORE, CORE_X, PLATE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MatrixBlockEntity.coreLevelOf(stack) > 0;
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

        this.coreLevel = new SyncedInt(blockEntity::getCoreLevel);
        this.plateCount = new SyncedInt(blockEntity::getPlateCount);
        this.buffer = new SyncedInt(() -> (int) Math.round(blockEntity.getBuffer()));
        addDataSlot(coreLevel);
        addDataSlot(plateCount);
        addDataSlot(buffer);
    }

    // ------------------------------------------------------------------
    // Ce que l'ecran affiche
    // ------------------------------------------------------------------
    public int getCoreLevel() {
        return coreLevel.value();
    }

    public int getPlateCount() {
        return plateCount.value();
    }

    public int getBuffer() {
        return buffer.value();
    }

    public boolean isWorking() {
        return MatrixBlockEntity.isWorking(getCoreLevel(), getPlateCount());
    }

    public int getCapacity() {
        return MatrixBlockEntity.capacityFor(getCoreLevel(), getPlateCount());
    }

    public int getRange() {
        return (int) MatrixBlockEntity.rangeFor(getCoreLevel(), getPlateCount());
    }

    public int getBandwidth() {
        return (int) MatrixBlockEntity.bandwidthFor(getCoreLevel(), getPlateCount());
    }

    // ------------------------------------------------------------------
    // Deplacements d'objets
    // ------------------------------------------------------------------

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stackInSlot = slot.getItem();
        result = stackInSlot.copy();

        if (index < MACHINE_SLOTS) {
            // Du Matrix vers l'inventaire du joueur.
            if (!moveItemStackTo(stackInSlot, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // Du joueur vers le Matrix : une plaque va aux emplacements de
            // plaques, un coeur au sien, le reste repart d'ou il vient.
            boolean moved = stackInSlot.is(ModItems.CONSTRAINT_PLATE.get())
                    ? moveItemStackTo(stackInSlot, 0, MatrixBlockEntity.SLOT_PLATE_COUNT, false)
                    : moveItemStackTo(stackInSlot, MatrixBlockEntity.SLOT_CORE, MACHINE_SLOTS, false);
            if (!moved) return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.MATRIX.get());
    }
}
