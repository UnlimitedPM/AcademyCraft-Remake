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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Ecran de la base de l'eolienne.
 *
 * Un seul emplacement, celui de l'objet a recharger. L'helice ne se met pas ici
 * mais dans le rotor, deux blocs plus haut : c'est le rotor qui tourne.
 *
 * La position des elements est reprise de {@link SolarGenMenu}, parce que c'est
 * le meme genre de machine : un emplacement, une barre d'energie, quelques lignes
 * d'etat.
 */
public class WindgenBaseMenu extends AbstractContainerMenu {

    public static final int SLOT_X = 80;
    public static final int SLOT_Y = 53;

    private static final int MACHINE_SLOTS = 1;

    private final WindgenBaseBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    private final SyncedInt energyStored;
    private final SyncedInt completeness;
    private final SyncedInt linked;

    public WindgenBaseMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, buf.readBlockPos()));
    }

    private static WindgenBaseBlockEntity resolveBlockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof WindgenBaseBlockEntity base) return base;
        throw new IllegalStateException("No WindgenBaseBlockEntity at " + pos);
    }

    public WindgenBaseMenu(int containerId, Inventory playerInventory, WindgenBaseBlockEntity blockEntity) {
        super(ModMenus.WINDGEN_BASE.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        addSlot(new SlotItemHandler(blockEntity.getInventory(), WindgenBaseBlockEntity.SLOT_CHARGE,
                SLOT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
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

        this.energyStored = new SyncedInt(blockEntity::getEnergyStored);
        this.completeness = new SyncedInt(() -> blockEntity.getCompleteness(blockEntity.getLevel()).ordinal());
        this.linked = new SyncedInt(() -> blockEntity.isLinked() ? 1 : 0);
        addDataSlot(energyStored);
        addDataSlot(completeness);
        addDataSlot(linked);
    }

    // ------------------------------------------------------------------
    // Ce que l'ecran affiche
    // ------------------------------------------------------------------

    public int getEnergyStored() {
        return energyStored.value();
    }

    public int getMaxEnergyStored() {
        return (int) WindgenBaseBlockEntity.BUFFER_SIZE;
    }

    public WindgenBaseBlockEntity.Completeness getCompleteness() {
        WindgenBaseBlockEntity.Completeness[] values = WindgenBaseBlockEntity.Completeness.values();
        int ordinal = completeness.value();
        if (ordinal < 0 || ordinal >= values.length) return WindgenBaseBlockEntity.Completeness.BASE_ONLY;
        return values[ordinal];
    }

    public boolean isLinkedToNode() {
        return linked.value() != 0;
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
        return stillValid(access, player, ModBlocks.WINDGEN_BASE.get());
    }
}
