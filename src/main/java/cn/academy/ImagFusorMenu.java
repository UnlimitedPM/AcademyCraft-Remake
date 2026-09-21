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
 * Ecran du fusor d'Imag.
 *
 * Cinq emplacements comme le {@code ContainerImagFusor} de la 1.12.2 : entree,
 * sortie, unites de phase en entree, unites vides en sortie, et batterie. Les
 * deux emplacements d'unites sont separes parce que l'unite pleine et l'unite
 * vide sont deux objets distincts dans le port, alors que l'original jouait sur
 * les metadonnees d'un objet unique.
 *
 * <h2>Position des emplacements</h2>
 *
 * Les constantes sont publiques pour que {@link ImagFusorScreen} dessine ses
 * fonds de casse au meme endroit, sans recopier les coordonnees. La zone de la
 * machine fait 176 de large et s'arrete a 70 de haut, le titre de l'inventaire du
 * joueur commencant juste apres.
 */
public class ImagFusorMenu extends AbstractContainerMenu {

    public static final int INPUT_X = 44;
    public static final int OUTPUT_X = 96;
    public static final int IMAG_INPUT_X = 44;
    public static final int ENERGY_INPUT_X = 70;
    public static final int IMAG_OUTPUT_X = 96;

    /** Ligne haute : entree et sortie. */
    public static final int TOP_SLOT_Y = 26;

    /** Ligne basse : unites de phase, batterie, unites vides. */
    public static final int BOTTOM_SLOT_Y = 52;

    private static final int MACHINE_SLOTS = ImagFusorBlockEntity.SLOT_COUNT;

    private final ImagFusorBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    private final SyncedInt workCounter;
    private final SyncedInt liquidAmount;
    private final SyncedInt energyStored;
    private final SyncedInt working;

    public ImagFusorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, buf.readBlockPos()));
    }

    private static ImagFusorBlockEntity resolveBlockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof ImagFusorBlockEntity fusor) return fusor;
        throw new IllegalStateException("No ImagFusorBlockEntity at " + pos);
    }

    public ImagFusorMenu(int containerId, Inventory playerInventory, ImagFusorBlockEntity blockEntity) {
        super(ModMenus.IMAG_FUSOR.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        addSlot(new SlotItemHandler(blockEntity.getInventory(), ImagFusorBlockEntity.SLOT_INPUT,
                INPUT_X, TOP_SLOT_Y));
        addSlot(new SlotItemHandler(blockEntity.getInventory(), ImagFusorBlockEntity.SLOT_OUTPUT,
                OUTPUT_X, TOP_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        addSlot(new SlotItemHandler(blockEntity.getInventory(), ImagFusorBlockEntity.SLOT_IMAG_INPUT,
                IMAG_INPUT_X, BOTTOM_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.MATTER_UNIT_PHASE.get());
            }
        });
        addSlot(new SlotItemHandler(blockEntity.getInventory(), ImagFusorBlockEntity.SLOT_ENERGY_INPUT,
                ENERGY_INPUT_X, BOTTOM_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
            }
        });
        addSlot(new SlotItemHandler(blockEntity.getInventory(), ImagFusorBlockEntity.SLOT_IMAG_OUTPUT,
                IMAG_OUTPUT_X, BOTTOM_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
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

        this.workCounter = new SyncedInt(blockEntity::getWorkCounter);
        this.liquidAmount = new SyncedInt(blockEntity::getLiquidAmount);
        this.energyStored = new SyncedInt(blockEntity::getEnergyStored);
        this.working = new SyncedInt(() -> blockEntity.isWorking() ? 1 : 0);
        addDataSlot(workCounter);
        addDataSlot(liquidAmount);
        addDataSlot(energyStored);
        addDataSlot(working);
    }

    // ------------------------------------------------------------------
    // Ce que l'ecran affiche
    // ------------------------------------------------------------------

    /** Avancement de la fusion, entre 0 et 1. */
    public float getProgress() {
        return Math.min(1.0f, (float) workCounter.value() / ImagFusorBlockEntity.WORK_TICKS);
    }

    public boolean isWorking() {
        return working.value() != 0;
    }

    public int getLiquidAmount() {
        return liquidAmount.value();
    }

    public int getTankSize() {
        return ImagFusorBlockEntity.TANK_SIZE;
    }

    public int getEnergyStored() {
        return energyStored.value();
    }

    public int getMaxEnergyStored() {
        return (int) ImagFusorBlockEntity.BUFFER_SIZE;
    }

    public boolean isLinkedToNode() {
        return blockEntity.isLinked();
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
            // De la machine vers l'inventaire du joueur.
            if (!moveItemStackTo(stackInSlot, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // Du joueur vers la machine : chaque objet va a l'emplacement qui
            // l'accepte, sinon rien ne bouge.
            boolean moved;
            if (stackInSlot.is(ModItems.MATTER_UNIT_PHASE.get())) {
                moved = moveItemStackTo(stackInSlot, ImagFusorBlockEntity.SLOT_IMAG_INPUT,
                        ImagFusorBlockEntity.SLOT_IMAG_INPUT + 1, false);
            } else if (stackInSlot.getCapability(ForgeCapabilities.ENERGY).isPresent()) {
                moved = moveItemStackTo(stackInSlot, ImagFusorBlockEntity.SLOT_ENERGY_INPUT,
                        ImagFusorBlockEntity.SLOT_ENERGY_INPUT + 1, false);
            } else {
                moved = moveItemStackTo(stackInSlot, ImagFusorBlockEntity.SLOT_INPUT,
                        ImagFusorBlockEntity.SLOT_INPUT + 1, false);
            }
            if (!moved) return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.IMAG_FUSOR.get());
    }
}
