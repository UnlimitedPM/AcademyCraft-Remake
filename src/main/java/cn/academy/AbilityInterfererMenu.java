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
 * Ecran du brouilleur d'aptitudes.
 *
 * Un emplacement pour l'objet d'energie, et quatre boutons : allumer ou eteindre,
 * reduire ou augmenter le rayon. Le rayon et l'etat sont les deux seuls reglages
 * de la machine ; la liste blanche editable de l'original demandait un champ de
 * saisie et n'est pas portee, seul le poseur y figure.
 *
 * Les boutons passent par {@link #clickMenuButton}, comme partout ailleurs : aucun
 * paquet maison. Le rayon avance par pas de {@code RANGE_STEP}, sinon il faudrait
 * un bouton par valeur entre 10 et 100.
 */
public class AbilityInterfererMenu extends AbstractContainerMenu {

    public static final int BUTTON_TOGGLE = 0;
    public static final int BUTTON_RANGE_DOWN = 1;
    public static final int BUTTON_RANGE_UP = 2;

    public static final int SLOT_X = 134;
    public static final int SLOT_Y = 22;

    private static final int MACHINE_SLOTS = 1;

    private final AbilityInterfererBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    private final SyncedInt energyStored;
    private final SyncedInt enabled;
    private final SyncedInt range;
    private final SyncedInt affected;
    private final SyncedInt linked;

    public AbilityInterfererMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, buf.readBlockPos()));
    }

    private static AbilityInterfererBlockEntity resolveBlockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof AbilityInterfererBlockEntity machine) return machine;
        throw new IllegalStateException("No AbilityInterfererBlockEntity at " + pos);
    }

    public AbilityInterfererMenu(int containerId, Inventory playerInventory, AbilityInterfererBlockEntity blockEntity) {
        super(ModMenus.ABILITY_INTERFERER.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        addSlot(new SlotItemHandler(blockEntity.getInventory(), AbilityInterfererBlockEntity.SLOT_BATTERY,
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
        this.enabled = new SyncedInt(() -> blockEntity.isEnabled() ? 1 : 0);
        this.range = new SyncedInt(blockEntity::getRange);
        this.affected = new SyncedInt(blockEntity::getAffectedCount);
        this.linked = new SyncedInt(() -> blockEntity.isLinked() ? 1 : 0);
        addDataSlot(energyStored);
        addDataSlot(enabled);
        addDataSlot(range);
        addDataSlot(affected);
        addDataSlot(linked);
    }

    // ------------------------------------------------------------------
    // Boutons
    // ------------------------------------------------------------------

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        switch (buttonId) {
            case BUTTON_TOGGLE -> blockEntity.setEnabled(!blockEntity.isEnabled());
            case BUTTON_RANGE_DOWN -> blockEntity.adjustRange(-AbilityInterfererBlockEntity.RANGE_STEP);
            case BUTTON_RANGE_UP -> blockEntity.adjustRange(AbilityInterfererBlockEntity.RANGE_STEP);
            default -> {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Ce que l'ecran affiche
    // ------------------------------------------------------------------

    public boolean isEnabled() {
        return enabled.value() != 0;
    }

    public int getRange() {
        return range.value();
    }

    public int getAffectedCount() {
        return affected.value();
    }

    public int getEnergyStored() {
        return energyStored.value();
    }

    public int getMaxEnergyStored() {
        return (int) AbilityInterfererBlockEntity.BUFFER_SIZE;
    }

    public double getCostPerTick() {
        double squared = (double) getRange() * getRange();
        return squared / 10.0d;
    }

    public boolean isLinkedToNode() {
        return linked.value() != 0;
    }

    /** Vrai si le tampon peut payer un cycle au rayon courant. */
    public boolean canPay() {
        return getEnergyStored() >= (double) getRange() * getRange();
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
            // Du joueur vers la machine : seuls les objets d'energie ont leur place
            // ici, le reste ne bouge pas.
            if (!stackInSlot.getCapability(ForgeCapabilities.ENERGY).isPresent()) return ItemStack.EMPTY;
            if (!moveItemStackTo(stackInSlot, 0, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ABILITY_INTERFERER.get());
    }
}
