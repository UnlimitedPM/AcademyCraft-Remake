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
 * Ecran du generateur de phase.
 *
 * Trois emplacements, comme le {@code ContainerPhaseGen} de la 1.12.2 : les
 * unites de phase pleines, les unites vides en retour, et l'objet d'energie a
 * recharger.
 */
public class PhaseGeneratorMenu extends AbstractContainerMenu {

    public static final int LIQUID_IN_X = 44;
    public static final int LIQUID_OUT_X = 116;
    public static final int OUTPUT_X = 80;
    public static final int TOP_SLOT_Y = 26;
    public static final int OUTPUT_SLOT_Y = 54;

    private static final int MACHINE_SLOTS = PhaseGeneratorBlockEntity.SLOT_COUNT;

    private final PhaseGeneratorBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    private final SyncedInt energyStored;
    private final SyncedInt liquidAmount;
    private final SyncedInt producing;
    private final SyncedInt linked;

    public PhaseGeneratorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, buf.readBlockPos()));
    }

    private static PhaseGeneratorBlockEntity resolveBlockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof PhaseGeneratorBlockEntity generator) return generator;
        throw new IllegalStateException("No PhaseGeneratorBlockEntity at " + pos);
    }

    public PhaseGeneratorMenu(int containerId, Inventory playerInventory, PhaseGeneratorBlockEntity blockEntity) {
        super(ModMenus.PHASE_GENERATOR.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        addSlot(new SlotItemHandler(blockEntity.getInventory(), PhaseGeneratorBlockEntity.SLOT_LIQUID_IN,
                LIQUID_IN_X, TOP_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.MATTER_UNIT_PHASE.get());
            }
        });
        addSlot(new SlotItemHandler(blockEntity.getInventory(), PhaseGeneratorBlockEntity.SLOT_LIQUID_OUT,
                LIQUID_OUT_X, TOP_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new SlotItemHandler(blockEntity.getInventory(), PhaseGeneratorBlockEntity.SLOT_OUTPUT,
                OUTPUT_X, OUTPUT_SLOT_Y) {
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
        this.liquidAmount = new SyncedInt(blockEntity::getLiquidAmount);
        this.producing = new SyncedInt(() -> blockEntity.isProducing() ? 1 : 0);
        this.linked = new SyncedInt(() -> blockEntity.isLinked() ? 1 : 0);
        addDataSlot(energyStored);
        addDataSlot(liquidAmount);
        addDataSlot(producing);
        addDataSlot(linked);
    }

    // ------------------------------------------------------------------
    // Ce que l'ecran affiche
    // ------------------------------------------------------------------

    public int getEnergyStored() {
        return energyStored.value();
    }

    public int getMaxEnergyStored() {
        return (int) PhaseGeneratorBlockEntity.BUFFER_SIZE;
    }

    public int getLiquidAmount() {
        return liquidAmount.value();
    }

    public int getTankSize() {
        return PhaseGeneratorBlockEntity.TANK_SIZE;
    }

    public boolean isProducing() {
        return producing.value() != 0;
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
            // Du joueur vers la machine : une unite de phase va dans son
            // emplacement, un objet d'energie dans le sien, le reste ne bouge pas.
            boolean moved;
            if (stackInSlot.is(ModItems.MATTER_UNIT_PHASE.get())) {
                moved = moveItemStackTo(stackInSlot, PhaseGeneratorBlockEntity.SLOT_LIQUID_IN,
                        PhaseGeneratorBlockEntity.SLOT_LIQUID_IN + 1, false);
            } else if (stackInSlot.getCapability(ForgeCapabilities.ENERGY).isPresent()) {
                moved = moveItemStackTo(stackInSlot, PhaseGeneratorBlockEntity.SLOT_OUTPUT,
                        PhaseGeneratorBlockEntity.SLOT_OUTPUT + 1, false);
            } else {
                return ItemStack.EMPTY;
            }
            if (!moved) return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.PHASE_GENERATOR.get());
    }
}
