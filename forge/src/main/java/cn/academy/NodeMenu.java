package cn.academy;

import cn.academy.energy.NodeBlock;
import cn.academy.energy.NodeBlockEntity;
import cn.academy.energy.WirelessNode;
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
public class NodeMenu extends AbstractContainerMenu implements WirelessNode {

    /** L'emplacement du haut : l'unite qu'on vide dans le noeud ({@code SlotIFItem(tile, 0, 42, 10)}). */
    public static final int CHARGE_IN_X = 42;
    public static final int CHARGE_IN_Y = 10;

    /** Celui du bas : l'unite qu'on vient remplir ({@code SlotIFItem(tile, 1, 42, 80)}). */
    public static final int CHARGE_OUT_X = 42;
    public static final int CHARGE_OUT_Y = 80;

    private static final int MACHINE_SLOTS = 2;

    private final NodeBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    /** Le nom de celui qui a ouvert l'ecran : c'est a lui qu'on demande s'il est le poseur. */
    private final String playerName;

    @Override
    public BlockPos nodePos() {
        return blockEntity.getBlockPos();
    }

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
        this.playerName = playerInventory.player.getGameProfile().getName();

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

    /** Le nom du noeud, celui que le Matrix lira et que l'interface montre. */
    public String nodeName() {
        return blockEntity.getNodeName();
    }

    /** Son mot de passe : l'ecran ne le montre qu'a son poseur, et masque. */
    public String password() {
        return blockEntity.getPassword();
    }

    /** Le nom de celui qui a pose le noeud. */
    public String owner() {
        return blockEntity.getPlacer();
    }

    /**
     * Vrai si l'ecran doit laisser modifier le nom et le mot de passe.
     *
     * C'est la meme regle que celle de l'original — celui qui a pose le noeud, et personne d'autre —
     * et elle est belle et bien <b>du serveur</b> : un client qui se dirait proprietaire ne
     * convaincrait pas {@link cn.academy.energy.network.NodeConfigPacket}. Ici elle ne sert qu'a
     * decider si les champs s'ecrivent.
     */
    public boolean isOwner() {
        return blockEntity.isOwner(playerName);
    }

    /**
     * Demande un nouveau nom au serveur.
     *
     * L'ecran ne l'ecrit pas lui-meme : le noeud appartient a son poseur, et c'est le serveur qui
     * tranche. Le nom local ne change donc qu'une fois la reponse revenue — c'est ce qui empeche
     * un ecran d'afficher une chose que le serveur a refusee.
     */
    public void rename(String name) {
        send(false, name);
    }

    /** Et un nouveau mot de passe — vide pour l'enlever. */
    public void repassword(String value) {
        send(true, value);
    }

    private void send(boolean password, String value) {
        cn.academy.ability.network.AbilityNetwork.CHANNEL.sendToServer(
                new cn.academy.energy.network.NodeConfigPacket(
                        blockEntity.getBlockPos(), password, value));
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
