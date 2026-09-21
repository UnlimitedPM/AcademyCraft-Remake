package cn.academy.energy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cn.academy.ModBlockEntities;
import cn.academy.ModItems;
import cn.academy.MatrixMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Matrix sans fil : le coeur du reseau energetique Imag.
 *
 * Portage de {@code TileMatrix} + {@code WirelessNet} de la 1.12.2.
 *
 * Le Matrix ne stocke pas d'energie lui-meme. Il tient le <b>tampon</b> du
 * reseau (l'energie en transit) et, a chaque tick, redistribue l'energie entre
 * tous les noeuds raccordes pour qu'ils atteignent le meme taux de remplissage.
 * C'est {@link WirelessBalancer} qui fait le calcul ; ce block entity ne fait
 * que lui fournir la liste des noeuds et sa bande passante.
 *
 * <h2>Quatre emplacements</h2>
 *
 * Comme l'original : trois plaques de contrainte (emplacements 0 a 2) et un
 * coeur de matrice (emplacement 3, un seul objet par emplacement comme en
 * 1.12.2). Le Matrix ne fonctionne que si <b>les trois plaques sont presentes</b>
 * et qu'un coeur est insere. C'est ce que teste {@link #isWorking()}, et tout le
 * reste (portee, capacite, bande passante) en decoule.
 *
 * <h2>Ou est le block entity ?</h2>
 *
 * Le Matrix est un multi-bloc de 2x2x2. Le block entity n'existe que sur la
 * partie d'ancrage ({@code B_F_R}) ; {@code MatrixBlock.newBlockEntity} renvoie
 * {@code null} pour les sept autres. Sans cela on aurait huit block entities
 * pour un seul Matrix, chacun avec son propre tampon.
 */
public class MatrixBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_PLATE_FIRST = 0;
    public static final int SLOT_PLATE_COUNT = 3;
    public static final int SLOT_CORE = 3;
    private static final int SLOT_COUNT = 4;

    /**
     * Portee maximale theorique : 24 * sqrt(3), soit environ 41,6 blocs.
     * C'est le rayon de recherche des noeuds qui cherchent un Matrix a
     * rejoindre, puisqu'ils ignorent la portee du Matrix avant de l'avoir trouve.
     */
    public static final double MAX_RANGE = 24.0d * Math.sqrt(3.0d);

    /** Cadence de verification des liens, en ticks. */
    private static final int CHECK_INTERVAL = 20;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public int getSlotLimit(int slot) {
            // L'original limitait chaque emplacement a un objet.
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (stack.isEmpty()) return true;
            return slot == SLOT_CORE
                    ? coreLevelOf(stack) > 0
                    : stack.is(ModItems.CONSTRAINT_PLATE.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            // Changer le coeur ou les plaques change la portee et la capacite :
            // il faut reviser les liens sans attendre la prochaine verification.
            needsRelink = true;
            setChanged();
        }
    };

    /** Energie en transit entre deux ticks. Repris de {@code WirelessNet.buffer}. */
    private double buffer;

    /** Force une revision des liens au prochain tick (coeur ou plaques changes). */
    private boolean needsRelink = true;

    private int checkTicker;

    /** Melange les noeuds avant l'equilibrage, comme le {@code shuffle} d'origine. */
    private final Random shuffleRandom = new Random();

    public MatrixBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MATRIX.get(), pos, state);
    }

    // ------------------------------------------------------------------
    // Etat derive de l'inventaire
    // ------------------------------------------------------------------

    /**
     * Niveau du coeur insere : 1 pour {@code mat_core_0}, 2 pour
     * {@code mat_core_1}, 3 pour {@code mat_core_2}, 0 si l'emplacement est vide.
     *
     * L'original lisait les metadonnees d'un objet unique
     * ({@code getItemDamage() + 1}) ; le port a eclate {@code mat_core} en trois
     * objets distincts, d'ou cette correspondance explicite.
     */
    public static int coreLevelOf(ItemStack stack) {
        if (stack.is(ModItems.MAT_CORE_0.get())) return 1;
        if (stack.is(ModItems.MAT_CORE_1.get())) return 2;
        if (stack.is(ModItems.MAT_CORE_2.get())) return 3;
        return 0;
    }

    public int getCoreLevel() {
        return coreLevelOf(inventory.getStackInSlot(SLOT_CORE));
    }

    /** Nombre de plaques de contrainte presentes (0 a 3). */
    public int getPlateCount() {
        int count = 0;
        for (int slot = SLOT_PLATE_FIRST; slot < SLOT_PLATE_FIRST + SLOT_PLATE_COUNT; slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) count++;
        }
        return count;
    }

    /** Le Matrix n'est actif que coeur insere et les trois plaques en place. */
    public boolean isWorking() {
        return isWorking(getCoreLevel(), getPlateCount());
    }

    /**
     * Le calcul est expose en statique pour que l'ecran puisse l'appliquer aux
     * valeurs qu'il recoit par le reseau, sans avoir a recopier les formules.
     */
    public static boolean isWorking(int coreLevel, int plateCount) {
        return coreLevel > 0 && plateCount == SLOT_PLATE_COUNT;
    }

    /** Nombre de noeuds que le Matrix accepte. 8 par niveau de coeur. */
    public int getCapacity() {
        return capacityFor(getCoreLevel(), getPlateCount());
    }

    public static int capacityFor(int coreLevel, int plateCount) {
        return isWorking(coreLevel, plateCount) ? 8 * coreLevel : 0;
    }

    /** Energie deplacable par tick. 60 fois le niveau de coeur, au carre. */
    public double getBandwidth() {
        return bandwidthFor(getCoreLevel(), getPlateCount());
    }

    public static double bandwidthFor(int coreLevel, int plateCount) {
        return isWorking(coreLevel, plateCount) ? coreLevel * coreLevel * 60.0d : 0.0d;
    }

    /** Portee du signal, en blocs. 24 par racine du niveau de coeur. */
    public double getRange() {
        return rangeFor(getCoreLevel(), getPlateCount());
    }

    public static double rangeFor(int coreLevel, int plateCount) {
        return isWorking(coreLevel, plateCount) ? 24.0d * Math.sqrt(coreLevel) : 0.0d;
    }

    public double getBuffer() {
        return buffer;
    }

    /** Renseigne le tampon directement (utilise par les tests). */
    public void setBuffer(double value) {
        buffer = value;
    }

    /** Vrai si un noeud a cette position est assez proche pour etre raccorde. */
    public boolean canReach(BlockPos nodePos) {
        if (!isWorking()) return false;
        double range = getRange();
        return worldPosition.distSqr(nodePos) <= range * range;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, MatrixBlockEntity matrix) {
        if (!(level instanceof ServerLevel server)) return;

        boolean dueForCheck = ++matrix.checkTicker >= CHECK_INTERVAL;
        if (dueForCheck) matrix.checkTicker = 0;

        if (dueForCheck || matrix.needsRelink) {
            matrix.needsRelink = false;
            matrix.refreshLinks(server);
        }

        if (matrix.isWorking()) matrix.balance(server);
    }

    /**
     * Verifie que chaque noeud raccorde existe encore et est toujours a portee,
     * et qu'il n'y en a pas plus que la capacite. Les liens morts sont retires.
     */
    private void refreshLinks(ServerLevel level) {
        ImagNetworkData data = ImagNetworkData.get(level);

        List<BlockPos> valid = new ArrayList<>();
        for (BlockPos nodePos : new HashSet<>(data.nodesOf(worldPosition))) {
            NodeBlockEntity node = nodeAt(level, nodePos);
            if (node == null || !canReach(nodePos)) {
                data.unlink(nodePos);
                if (node != null) node.setConnected(false);
                continue;
            }
            valid.add(nodePos);
        }

        // Capacite : l'original refusait simplement les liens au-dela. Ici on
        // retire les noeuds les plus eloignes, qui sont les moins utiles.
        int capacity = getCapacity();
        if (valid.size() > capacity) {
            valid.sort(Comparator.comparingDouble(nodePos -> worldPosition.distSqr(nodePos)));
            for (int i = capacity; i < valid.size(); i++) {
                BlockPos dropped = valid.get(i);
                data.unlink(dropped);
                NodeBlockEntity node = nodeAt(level, dropped);
                if (node != null) node.setConnected(false);
            }
            valid = valid.subList(0, capacity);
        }

        for (BlockPos nodePos : valid) {
            NodeBlockEntity node = nodeAt(level, nodePos);
            if (node != null) node.setConnected(true);
        }
    }

    /**
     * Un tick d'equilibrage : construit la liste des noeuds, la melange, et
     * delegue le calcul a {@link WirelessBalancer}.
     */
    private void balance(ServerLevel level) {
        List<EnergyNode> nodes = new ArrayList<>();
        for (BlockPos nodePos : ImagNetworkData.get(level).nodesOf(worldPosition)) {
            NodeBlockEntity node = nodeAt(level, nodePos);
            if (node != null && canReach(nodePos)) nodes.add(node);
        }
        if (nodes.isEmpty()) return;

        // L'original melangeait la liste pour ne pas toujours servir les memes
        // noeuds en premier quand la bande passante ne suffit pas a tout egaliser.
        Collections.shuffle(nodes, shuffleRandom);

        double previous = buffer;
        buffer = WirelessBalancer.balance(nodes, getBandwidth(), buffer).buffer();
        if (buffer != previous) setChanged();
    }

    @Nullable
    private static NodeBlockEntity nodeAt(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) return null;
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof NodeBlockEntity node ? node : null;
    }

    // ------------------------------------------------------------------
    // Persistance
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", inventory.serializeNBT());
        tag.putDouble("buffer", buffer);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("inventory"));
        buffer = tag.getDouble("buffer");
        needsRelink = true;
    }

    // ------------------------------------------------------------------
    // Ouverture de l'ecran
    // ------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.academy.matrix");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MatrixMenu(containerId, playerInventory, this);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
}
