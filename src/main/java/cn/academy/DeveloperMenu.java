package cn.academy;

import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.develop.DevelopActionLevel;
import cn.academy.ability.develop.DeveloperType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Ecran du developeur d'aptitudes.
 *
 * Il n'y a aucun emplacement : le joueur ne depose rien, il choisit une aptitude
 * et paie en energie. Les categories sont listees avec leur niveau et le cout de
 * la prochaine etape, et un bouton par categorie lance l'apprentissage.
 *
 * <h2>Boutons plutot que selection</h2>
 *
 * Les boutons passent par {@link #clickMenuButton}, le mecanisme vanilla des
 * boutons de conteneur : aucun paquet maison. Un bouton par categorie evite aussi
 * d'avoir a stocker une selection, ce qui aurait demande un emplacement de donnee
 * de plus pour rien.
 *
 * <h2>Ce qui traverse le reseau</h2>
 *
 * Le niveau de chaque categorie, l'avancement, l'etat et l'energie. Le nombre de
 * categories est connu des deux cotes (c'est le meme registre), donc les
 * emplacements de donnee sont ajoutes dans le meme ordre sur le serveur et sur le
 * client : c'est leur <b>ordre</b> qui les identifie, pas leur nom.
 */
public class DeveloperMenu extends AbstractContainerMenu {

    private final DeveloperBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    /** Un niveau par categorie enregistree, dans l'ordre du registre. */
    private final SyncedInt[] categoryLevels;

    private final SyncedInt progress;
    private final SyncedInt state;
    private final SyncedInt developingCategory;
    private final SyncedInt energyStored;

    public DeveloperMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, buf.readBlockPos()));
    }

    private static DeveloperBlockEntity resolveBlockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof DeveloperBlockEntity developer) return developer;
        throw new IllegalStateException("No DeveloperBlockEntity at " + pos);
    }

    public DeveloperMenu(int containerId, Inventory playerInventory, DeveloperBlockEntity blockEntity) {
        super(ModMenus.DEVELOPER.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        Player player = playerInventory.player;
        var categories = CategoryManager.INSTANCE.getCategories();
        this.categoryLevels = new SyncedInt[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            categoryLevels[i] = new SyncedInt(() ->
                    player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                            .map(data -> data.getCategoryLevel(category))
                            .orElse(0));
            addDataSlot(categoryLevels[i]);
        }

        this.progress = new SyncedInt(() -> (int) Math.round(blockEntity.getProgress() * 1000.0d));
        this.state = new SyncedInt(() -> blockEntity.getState().ordinal());
        this.developingCategory = new SyncedInt(blockEntity::getCategoryId);
        this.energyStored = new SyncedInt(blockEntity::getEnergyStored);
        addDataSlot(progress);
        addDataSlot(state);
        addDataSlot(developingCategory);
        addDataSlot(energyStored);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 120 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 178));
        }
    }

    // ------------------------------------------------------------------
    // Boutons
    // ------------------------------------------------------------------

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        return blockEntity.startDeveloping(serverPlayer, buttonId);
    }

    // ------------------------------------------------------------------
    // Ce que l'ecran affiche
    // ------------------------------------------------------------------

    public int getCategoryCount() {
        return categoryLevels.length;
    }

    public Category getCategory(int index) {
        return CategoryManager.INSTANCE.getCategory(index);
    }

    public int getCategoryLevel(int index) {
        if (index < 0 || index >= categoryLevels.length) return 0;
        return categoryLevels[index].value();
    }

    /** Niveau maximal, au-dela duquel il n'y a plus rien a developper. */
    public int getMaxLevel() {
        return DevelopActionLevel.MAX_LEVEL;
    }

    /** Stimulations necessaires pour faire monter la categorie d'un cran. */
    public int getStimulationsFor(int index) {
        return 5 * (getCategoryLevel(index) + 1);
    }

    /** Energie totale que coutera la prochaine etape de cette categorie. */
    public double getCostFor(int index) {
        return blockEntity.getDeveloperType().getTotalCost(getStimulationsFor(index));
    }

    public double getEnergyPerTick() {
        return blockEntity.getDeveloperType().getEnergyPerTick();
    }

    public DeveloperType getDeveloperType() {
        return blockEntity.getDeveloperType();
    }

    public float getProgress() {
        return Math.min(1.0f, progress.value() / 1000.0f);
    }

    public DeveloperBlockEntity.DevState getState() {
        DeveloperBlockEntity.DevState[] values = DeveloperBlockEntity.DevState.values();
        int ordinal = state.value();
        if (ordinal < 0 || ordinal >= values.length) return DeveloperBlockEntity.DevState.IDLE;
        return values[ordinal];
    }

    public boolean isDeveloping() {
        return getState() == DeveloperBlockEntity.DevState.DEVELOPING;
    }

    public int getDevelopingCategory() {
        return developingCategory.value();
    }

    public int getEnergyStored() {
        return energyStored.value();
    }

    public int getMaxEnergyStored() {
        return blockEntity.getMaxEnergyStored();
    }

    public boolean isLinkedToNode() {
        return blockEntity.isLinked();
    }

    // ------------------------------------------------------------------
    // Sans emplacement a deplacer
    // ------------------------------------------------------------------

    /**
     * Aucun emplacement n'appartient a la machine, donc rien a transferer :
     * tout va d'un cote a l'autre de l'inventaire du joueur.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stackInSlot = slot.getItem();
        result = stackInSlot.copy();

        boolean fromMain = index < slots.size() - 9;
        if (fromMain) {
            if (!moveItemStackTo(stackInSlot, slots.size() - 9, slots.size(), false)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stackInSlot, 0, slots.size() - 9, false)) return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, blockEntity.getBlockState().getBlock());
    }
}
