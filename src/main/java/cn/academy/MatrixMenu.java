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

    /** La plaque du haut, au milieu : le {@code SlotPlate(tile, 0, 78, 11)} de l'original. */
    public static final int PLATE_0_X = 78;
    public static final int PLATE_0_Y = 11;

    /** Celles du bas, de part et d'autre du coeur. */
    public static final int PLATE_1_X = 53;
    public static final int PLATE_1_Y = 60;
    public static final int PLATE_2_X = 104;
    public static final int PLATE_2_Y = 60;

    /** Et le coeur, entre les quatre : {@code SlotCore(tile, 3, 78, 36)}. */
    public static final int CORE_X = 78;
    public static final int CORE_Y = 36;

    /** Nombre d'emplacements du Matrix, avant les emplacements du joueur. */
    private static final int MACHINE_SLOTS = MatrixBlockEntity.SLOT_PLATE_COUNT + 1;

    /** Les trois plaques, dans l'ordre des emplacements du block entity. */
    private static final int[] PLATE_X = {PLATE_0_X, PLATE_1_X, PLATE_2_X};
    private static final int[] PLATE_Y = {PLATE_0_Y, PLATE_1_Y, PLATE_2_Y};

    private final MatrixBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    /** Le nom de celui qui a ouvert l'ecran : c'est a lui qu'on demande s'il est le poseur. */
    private final String playerName;

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
        this.playerName = playerInventory.player.getGameProfile().getName();

        for (int slot = 0; slot < MatrixBlockEntity.SLOT_PLATE_COUNT; slot++) {
            addSlot(new SlotItemHandler(blockEntity.getInventory(), slot, PLATE_X[slot], PLATE_Y[slot]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(ModItems.CONSTRAINT_PLATE.get());
                }
            });
        }

        addSlot(new SlotItemHandler(blockEntity.getInventory(), MatrixBlockEntity.SLOT_CORE, CORE_X, CORE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MatrixBlockEntity.coreLevelOf(stack) > 0;
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
    // L'identite du reseau
    // ------------------------------------------------------------------

    /** Combien de noeuds le reseau porte, sur sa capacite. */
    public int load() {
        return blockEntity.getLoad();
    }

    /** Le nom du reseau, vide tant qu'il n'a pas ete cree. */
    public String ssid() {
        return blockEntity.getSsid();
    }

    /** Son mot de passe : l'ecran ne le montre qu'au poseur, et masque. */
    public String password() {
        return blockEntity.getPassword();
    }

    /** Le nom de celui qui a pose le Matrix. */
    public String owner() {
        return blockEntity.getPlacer();
    }

    /** Vrai quand le reseau existe : c'est ce qui choisit entre « le creer » et « le montrer ». */
    public boolean initialized() {
        return blockEntity.isInitialized();
    }

    /**
     * Vrai si l'ecran doit laisser creer et modifier le reseau.
     *
     * C'est la regle de l'original — celui qui a pose le Matrix, et personne d'autre — et elle est
     * <b>du serveur</b> : un client qui se dirait proprietaire ne convaincrait pas
     * {@link cn.academy.energy.network.MatrixConfigPacket}. Ici elle ne decide que de l'affichage.
     */
    public boolean isOwner() {
        return blockEntity.isOwner(playerName);
    }

    /** Cree le reseau : le bouton « INIT », qui n'arrive qu'une fois. */
    public void initNetwork(String ssid, String password) {
        send(cn.academy.energy.network.MatrixConfigPacket.Action.INIT, ssid, password);
    }

    /** Renomme le reseau — vide, il ne se renomme pas : un reseau sans nom n'existe pas. */
    public void rename(String ssid) {
        send(cn.academy.energy.network.MatrixConfigPacket.Action.SSID, ssid, "");
    }

    /** Et change son mot de passe : vide pour l'enlever. */
    public void repassword(String password) {
        send(cn.academy.energy.network.MatrixConfigPacket.Action.PASSWORD, "", password);
    }

    private void send(cn.academy.energy.network.MatrixConfigPacket.Action action,
                      String ssid, String password) {
        cn.academy.ability.network.AbilityNetwork.CHANNEL.sendToServer(
                new cn.academy.energy.network.MatrixConfigPacket(
                        blockEntity.getBlockPos(), action, ssid, password));
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
