package cn.academy;

import cn.academy.crafting.MetalFormerMode;
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

/**
 * Ecran du formeur de metal.
 *
 * Trois emplacements comme le {@code ContainerMetalFormer} de la 1.12.2 : entree,
 * sortie et batterie. La sortie n'accepte rien : c'est la machine qui la remplit.
 *
 * <h2>Changement de mode</h2>
 *
 * Les boutons passent par {@link #clickMenuButton}, le mecanisme vanilla des
 * boutons de conteneur. Il n'y a donc aucun paquet maison a ecrire : le clic part
 * dans un {@code ServerboundContainerButtonClickPacket}, que le serveur route
 * vers cette methode. Un paquet sur mesure aurait fait la meme chose en plus de
 * code et en plus de surface a maintenir.
 */
public class MetalFormerMenu extends AbstractContainerMenu implements WirelessMachine {

    /** Identifiant de bouton : mode precedent. */
    public static final int BUTTON_MODE_PREVIOUS = 0;

    /** Identifiant de bouton : mode suivant. */
    public static final int BUTTON_MODE_NEXT = 1;

    // Les positions de l'original, celles de son ContainerMetalFormer : l'entree a 13, la sortie a
    // 143, et la batterie a 42 sous l'entree. Le cadre du document (172 sur 187) se pose sur le
    // conteneur (207 sur 186), et c'est ce qui met chaque emplacement sur son trou ; des nombres
    // inventes les poseraient a cote. L'ancien ecran procedural avait les siens, et c'est pour lui
    // que ces constantes-la existaient.
    public static final int IN_SLOT_X = 13;
    public static final int OUT_SLOT_X = 143;
    public static final int SLOT_Y = 49;
    public static final int BATTERY_SLOT_X = 42;
    public static final int BATTERY_SLOT_Y = 80;

    private static final int MACHINE_SLOTS = MetalFormerBlockEntity.SLOT_COUNT;

    private final MetalFormerBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    @Override
    public BlockPos machinePos() {
        return blockEntity.getBlockPos();
    }

    private final SyncedInt workCounter;
    private final SyncedInt modeOrdinal;
    private final SyncedInt energyStored;
    private final SyncedInt working;

    public MetalFormerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, buf.readBlockPos()));
    }

    private static MetalFormerBlockEntity resolveBlockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof MetalFormerBlockEntity former) return former;
        throw new IllegalStateException("No MetalFormerBlockEntity at " + pos);
    }

    public MetalFormerMenu(int containerId, Inventory playerInventory, MetalFormerBlockEntity blockEntity) {
        super(ModMenus.METAL_FORMER.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        addSlot(new SlotItemHandler(blockEntity.getInventory(), MetalFormerBlockEntity.SLOT_IN, IN_SLOT_X, SLOT_Y));
        addSlot(new SlotItemHandler(blockEntity.getInventory(), MetalFormerBlockEntity.SLOT_OUT, OUT_SLOT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new SlotItemHandler(blockEntity.getInventory(), MetalFormerBlockEntity.SLOT_BATTERY, BATTERY_SLOT_X, BATTERY_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return cn.academy.energy.EnergyItems.isEnergyItem(stack);
            }
        });

        // Les slots du joueur, aux positions de l'original : la barre rapide en bas, puis les trois
        // rangees de l'inventaire de bas en haut. C'est le `mapPlayerInventory` de TechUIContainer,
        // le meme calcul que le generateur solaire et le generateur de phase.
        for (int i = 1; i < 4; i++) {
            for (int col = 0; col < 9; col++) {
                int slot = (4 - i) * 9 + col;
                addSlot(new Slot(playerInventory, slot, 6 + col * 18, 159 - i * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 6 + col * 18, 163));
        }

        this.workCounter = new SyncedInt(blockEntity::getWorkCounter);
        this.modeOrdinal = new SyncedInt(() -> blockEntity.getMode().ordinal());
        this.energyStored = new SyncedInt(blockEntity::getEnergyStored);
        this.working = new SyncedInt(() -> blockEntity.isWorking() ? 1 : 0);
        addDataSlot(workCounter);
        addDataSlot(modeOrdinal);
        addDataSlot(energyStored);
        addDataSlot(working);
    }

    // ------------------------------------------------------------------
    // Boutons
    // ------------------------------------------------------------------

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        switch (buttonId) {
            case BUTTON_MODE_PREVIOUS -> blockEntity.cycleMode(-1);
            case BUTTON_MODE_NEXT -> blockEntity.cycleMode(1);
            default -> {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Ce que l'ecran affiche
    // ------------------------------------------------------------------

    public MetalFormerMode getMode() {
        return MetalFormerMode.byOrdinal(modeOrdinal.value());
    }

    /** Avancement de la transformation, entre 0 et 1. */
    public float getProgress() {
        return Math.min(1.0f, (float) workCounter.value() / MetalFormerBlockEntity.WORK_TICKS);
    }

    public boolean isWorking() {
        return working.value() != 0;
    }

    public int getEnergyStored() {
        return energyStored.value();
    }

    public int getMaxEnergyStored() {
        return (int) MetalFormerBlockEntity.BUFFER_SIZE;
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
            // Du joueur vers la machine : une batterie va dans son emplacement,
            // tout le reste tente l'entree.
            boolean moved = cn.academy.energy.EnergyItems.isEnergyItem(stackInSlot)
                    ? moveItemStackTo(stackInSlot,
                            MetalFormerBlockEntity.SLOT_BATTERY, MetalFormerBlockEntity.SLOT_BATTERY + 1, false)
                    : moveItemStackTo(stackInSlot,
                            MetalFormerBlockEntity.SLOT_IN, MetalFormerBlockEntity.SLOT_IN + 1, false);
            if (!moved) return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.METAL_FORMER.get());
    }
}
