package cn.academy;

import cn.academy.energy.NodeBlock;
import cn.academy.energy.NodeBlockEntity;
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
 * Ecran du noeud sans fil.
 *
 * <p>Deux emplacements, comme le {@code ContainerNode} de la 1.12.2 : celui du haut charge le noeud
 * avec l'unite qu'on y depose, celui du bas charge l'unite. C'est tout ce que le noeud a a montrer de
 * tangible — le reste est dans son panneau d'infos, qui suit ce qu'il transporte et combien de
 * machines il porte.
 *
 * <h2>Aucune donnee synchronisee</h2>
 *
 * <p>Les valeurs affichees se lisent directement sur le block entity, comme le fait deja le
 * generateur solaire : l'energie et la charge y vivent, et une mise a jour de bloc les porte au client
 * quand elles changent. Un emplacement de donnee supplementaire ne dirait rien de plus, et il
 * faudrait le tenir a jour pour rien.
 *
 * <h2>Ce que l'original avait en plus</h2>
 *
 * <p>Le nom du noeud, son mot de passe et celui qui l'a pose : le port n'en a aucun des trois, et
 * s'en passe — le raccordement au Matrix se fait tout seul, du noeud vers le Matrix le plus proche.
 */
public class NodeMenu extends AbstractContainerMenu {

    /** L'emplacement du haut : l'unite qu'on vide dans le noeud ({@code SlotIFItem(tile, 0, 42, 10)}). */
    public static final int CHARGE_IN_X = 42;
    public static final int CHARGE_IN_Y = 10;

    /** Celui du bas : l'unite qu'on vient remplir ({@code SlotIFItem(tile, 1, 42, 80)}). */
    public static final int CHARGE_OUT_X = 42;
    public static final int CHARGE_OUT_Y = 80;

    private static final int MACHINE_SLOTS = 2;

    private final NodeBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public NodeMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, buf.readBlockPos()));
    }

    private static NodeBlockEntity resolveBlockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof NodeBlockEntity node) return node;
        throw new IllegalStateException("No NodeBlockEntity at " + pos);
    }

    public NodeMenu(int containerId, Inventory playerInventory, NodeBlockEntity blockEntity) {
        super(ModMenus.NODE.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        addSlot(new SlotItemHandler(blockEntity.getInventory(), NodeBlockEntity.SLOT_CHARGE_IN,
                CHARGE_IN_X, CHARGE_IN_Y));
        addSlot(new SlotItemHandler(blockEntity.getInventory(), NodeBlockEntity.SLOT_CHARGE_OUT,
                CHARGE_OUT_X, CHARGE_OUT_Y));

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
    }

    // ------------------------------------------------------------------
    // Ce que l'ecran affiche
    // ------------------------------------------------------------------

    /** L'energie du reseau que ce noeud tient. */
    public double energy() {
        return blockEntity.getEnergy();
    }

    public double maxEnergy() {
        return blockEntity.getMaxEnergy();
    }

    /** Combien de machines il porte, et combien il en accepte. */
    public double load() {
        return blockEntity.getLoad();
    }

    public int capacity() {
        return blockEntity.getCapacity();
    }

    /** Sa portee, en blocs : la ligne « Trans. Range » du panneau. */
    public int range() {
        return (int) blockEntity.getRange();
    }

    /**
     * Vrai si le noeud est raccorde a un Matrix.
     *
     * <p>C'est ce que suit l'animation de la page : elle bat vite quand le noeud est dans un reseau,
     * et clignote lentement quand il est seul.
     */
    public boolean isLinked() {
        return blockEntity.isConnected();
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
            // Du noeud vers l'inventaire du joueur.
            if (!moveItemStackTo(stackInSlot, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // Du joueur vers le noeud : les deux emplacements acceptent la meme unite d'energie, et
            // c'est l'emplacement du haut qui la prend en premier.
            if (!moveItemStackTo(stackInSlot, 0, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return result;
    }

    /**
     * Le noeud reste ouvert tant que le joueur est devant : les trois qualites de noeud partagent la
     * meme classe de bloc, donc c'est la classe qu'on verifie et non un bloc precis.
     */
    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof NodeBlock
                && player.distanceToSqr(pos.getX() + 0.5d, pos.getY() + 0.5d,
                        pos.getZ() + 0.5d) <= 64.0d)
                .orElse(false);
    }
}
