package cn.academy;

import cn.academy.energy.WirelessMachine;
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

public class SolarGenMenu extends AbstractContainerMenu implements WirelessMachine {

    private final SolarGenBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    /**
     * Le tampon, synchronise au client.
     *
     * <p>Le generateur change d'energie a chaque tick, et le block entity ne previent personne : le
     * client gardait donc la valeur du chargement du chunk, c'est-a-dire zero. L'ecran affichait un
     * panneau qui ne produit rien alors qu'il produisait. Un {@link SyncedInt} porte la valeur du
     * serveur au client ; l'ancien {@code DataSlot} ne le faisait pas, son {@code set} ne faisait
     * rien.
     */
    private final SyncedInt energyStored;

    @Override
    public BlockPos machinePos() {
        return blockEntity.getBlockPos();
    }

    public SolarGenMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, buf.readBlockPos()));
    }

    private static SolarGenBlockEntity resolveBlockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof SolarGenBlockEntity solarGen) return solarGen;
        throw new IllegalStateException("No SolarGenBlockEntity at " + pos);
    }

    public SolarGenMenu(int containerId, Inventory playerInventory, SolarGenBlockEntity blockEntity) {
        super(ModMenus.SOLAR_GEN.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        // L'emplacement de la batterie : le `SlotIFItem(tile, SLOT_BATTERY, 42, 81)` de l'original.
        addSlot(new SlotItemHandler(blockEntity.getInventory(), SolarGenBlockEntity.SLOT_BATTERY, 42, 81) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return cn.academy.energy.EnergyItems.isEnergyItem(stack);
            }
        });

        // Les slots du joueur, aux positions de l'original : la barre rapide en bas, puis les trois
        // rangees de l'inventaire de bas en haut. C'est le `mapPlayerInventory` de TechUIContainer.
        for (int i = 1; i < 4; i++) {
            for (int col = 0; col < 9; col++) {
                int slot = (4 - i) * 9 + col;
                addSlot(new Slot(playerInventory, slot, 6 + col * 18, 159 - i * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 6 + col * 18, 163));
        }

        this.energyStored = new SyncedInt(blockEntity::getEnergyStored);
        addDataSlot(energyStored);
    }

    public int getEnergyStored() {
        return energyStored.value();
    }

    public int getMaxEnergyStored() {
        return blockEntity.getMaxEnergyStored();
    }

    /** Ce que le generateur produit ce tick-la, en IF : la ligne « Gen. speed » du panneau. */
    public double generation() {
        return blockEntity.generation();
    }

    /** Et l'etat du ciel, qui choisit l'image du cadre anime de la page. */
    public SolarGenBlockEntity.SolarStatus sky() {
        return blockEntity.sky();
    }

    /**
     * Vrai si le generateur est raccorde a un noeud du reseau.
     *
     * L'etat est lu directement sur le block entity : il est synchronise par la
     * mise a jour de bloc que le generateur envoie quand il change, ce qui evite
     * un emplacement de donnee supplementaire.
     */
    public boolean isLinkedToNode() {
        return blockEntity.isLinked();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();
            if (index == 0) {
                if (!moveItemStackTo(stackInSlot, 1, slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stackInSlot, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.SOLAR_GEN.get());
    }
}
