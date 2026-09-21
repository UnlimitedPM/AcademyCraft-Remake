package cn.academy.energy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import cn.academy.ModBlockEntities;
import cn.academy.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Noeud sans fil : stocke l'energie du reseau Imag et sert de point d'acces.
 *
 * Portage de {@code TileNode} de la 1.12.2. Deux emplacements d'objets, comme
 * l'original :
 * <ul>
 *   <li>slot 0 « in » : on y met une unite d'energie pour <b>charger</b> le noeud ;</li>
 *   <li>slot 1 « out » : on y met une unite d'energie pour la <b>charger</b>
 *       depuis le noeud.</li>
 * </ul>
 *
 * Le noeud expose aussi l'energie Forge Energy de tous les cotes, pour que les
 * generateurs et les autres mods puissent l'alimenter. La conversion est 1 pour 1
 * entre l'unite du reseau Imag et le Forge Energy ; c'est une simplification par
 * rapport a l'original, qui avait son propre systeme d'objets energetiques
 * ({@code IFItemManager}).
 */
public class NodeBlockEntity extends BlockEntity implements EnergyNode {

    public static final int SLOT_CHARGE_IN = 0;
    public static final int SLOT_CHARGE_OUT = 1;
    private static final int SLOT_COUNT = 2;

    /** Duree, en ticks, entre deux synchronisations de l'etat visuel du bloc. */
    private static final int SYNC_INTERVAL = 10;

    /**
     * Duree, en ticks, entre deux recherches de Matrix.
     *
     * Le raccordement est mene par le <b>noeud</b> et non par le Matrix : un
     * noeud qui vient d'etre pose, ou dont le Matrix a disparu, cherche tout
     * seul le Matrix actif le plus proche. L'original laissait le joueur faire
     * ce lien a la main depuis l'interface du Matrix ; sans interface, la
     * recherche automatique est l'equivalent le plus proche.
     *
     * Une fois raccorde, plus aucune recherche n'a lieu : le cout est donc nul
     * pour un reseau en fonctionnement.
     */
    private static final int LINK_CHECK_INTERVAL = 100;

    /** Energie maximale qu'un generateur ou recepteur peut echanger par tick. */
    private static final int USER_CHECK_INTERVAL = 20;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            // Seules les unites d'energie vides ont un sens ici ; les autres
            // objets sont refuses pour eviter de les perdre.
            return stack.isEmpty() || stack.is(ModItems.ENERGY_UNIT.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private double energy;

    /** Client : vrai quand le noeud est raccorde a un Matrix (affichage seul). */
    private boolean connected;

    private int syncTicker;

    private int linkTicker;

    private int userTicker;

    /** Melange generateurs et recepteurs, comme le shuffle de l'original. */
    private final Random shuffleRandom = new Random();

    private final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> inventory);
    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(ForgeEnergyAdapter::new);

    public NodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRELESS_NODE.get(), pos, state);
    }

    /** Qualite du noeud, deduite du bloc qui le porte. */
    public NodeType getNodeType() {
        return getBlockState().getBlock() instanceof NodeBlock node ? node.getType() : NodeType.BASIC;
    }

    // ------------------------------------------------------------------
    // EnergyNode
    // ------------------------------------------------------------------

    @Override
    public double getEnergy() {
        return energy;
    }

    @Override
    public void setEnergy(double value) {
        energy = clamp(value);
    }

    @Override
    public double getMaxEnergy() {
        return getNodeType().getMaxEnergy();
    }

    @Override
    public double getBandwidth() {
        return getNodeType().getBandwidth();
    }

    /** Nombre de generateurs et recepteurs que ce noeud accepte. */
    public int getCapacity() {
        return getNodeType().getMaxLinks();
    }

    /** Portee du signal : jusqu'ou un generateur ou recepteur peut se raccorder. */
    public double getRange() {
        return getNodeType().getRange();
    }

    /** Ramene une valeur dans [0, capacite]. */
    private double clamp(double value) {
        if (value <= 0.0d) return 0.0d;
        double max = getMaxEnergy();
        return Math.min(value, max);
    }

    public boolean isConnected() {
        return connected;
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, NodeBlockEntity node) {
        node.chargeFromItem();
        node.dischargeToItem();

        // Les generateurs remplissent le noeud, les recepteurs le vident. C'est
        // le pendant de NodeConn.tick() dans l'original.
        if (level instanceof ServerLevel server) node.exchangeWithUsers(server);

        // L'etat visuel n'est mis a jour que par intermittence, comme dans
        // l'original : inutile de generer une mise a jour de bloc chaque tick.
        if (++node.syncTicker >= SYNC_INTERVAL) {
            node.syncTicker = 0;
            node.syncBlockState(level, pos);
        }

        if (++node.linkTicker >= LINK_CHECK_INTERVAL) {
            node.linkTicker = 0;
            node.ensureLinked(level, pos);
        }

        if (++node.userTicker >= USER_CHECK_INTERVAL) {
            node.userTicker = 0;
            if (level instanceof ServerLevel server) node.refreshUsers(server, pos);
        }
    }

    // ------------------------------------------------------------------
    // Generateurs et recepteurs raccordes au noeud
    // ------------------------------------------------------------------

    /**
     * Un tick d'echange avec les generateurs et recepteurs raccordes.
     *
     * Le calcul lui-meme est dans {@link NodeConnection} : ici on ne fait que
     * traduire des positions en machines, et retirer les raccordements dont la
     * machine a disparu.
     */
    private void exchangeWithUsers(ServerLevel level) {
        Set<BlockPos> positions = new HashSet<>(ImagNetworkData.get(level).usersOf(worldPosition));
        if (positions.isEmpty()) return;

        List<EnergyGenerator> generators = new ArrayList<>();
        List<EnergyReceiver> receivers = new ArrayList<>();

        for (BlockPos userPos : positions) {
            BlockEntity be = level.isLoaded(userPos) ? level.getBlockEntity(userPos) : null;
            if (be instanceof EnergyGenerator generator) {
                generators.add(generator);
            } else if (be instanceof EnergyReceiver receiver) {
                receivers.add(receiver);
            } else {
                // La machine a disparu, ou elle n'echange plus rien : le
                // raccordement ne veut plus rien dire.
                ImagNetworkData.get(level).unlinkUser(userPos);
            }
        }

        if (generators.isEmpty() && receivers.isEmpty()) return;

        NodeConnection.Result result = NodeConnection.tick(this, generators, receivers, shuffleRandom);
        if (result.received() != 0.0d || result.supplied() != 0.0d) setChanged();
    }

    /**
     * Verifie que les machines raccordees existent encore et sont toujours a
     * portee, et qu'il n'y en a pas plus que la capacite du noeud.
     */
    private void refreshUsers(ServerLevel level, BlockPos pos) {
        ImagNetworkData data = ImagNetworkData.get(level);

        List<BlockPos> valid = new ArrayList<>();
        for (BlockPos userPos : new HashSet<>(data.usersOf(worldPosition))) {
            BlockEntity be = level.isLoaded(userPos) ? level.getBlockEntity(userPos) : null;
            boolean usable = (be instanceof EnergyGenerator || be instanceof EnergyReceiver)
                    && pos.distSqr(userPos) <= getRange() * getRange();
            if (usable) valid.add(userPos);
            else data.unlinkUser(userPos);
        }

        // Capacite : le NodeConn de l'original refusait simplement les
        // raccordements au-dela. Ici on retire les machines les plus eloignees,
        // qui sont les moins utiles.
        int capacity = getCapacity();
        if (valid.size() > capacity) {
            valid.sort(Comparator.comparingDouble(userPos -> pos.distSqr(userPos)));
            for (int i = capacity; i < valid.size(); i++) data.unlinkUser(valid.get(i));
        }
    }

    // ------------------------------------------------------------------
    // Raccordement au reseau
    // ------------------------------------------------------------------

    /**
     * Verifie le raccordement du noeud et le repare au besoin : detache d'un
     * Matrix disparu ou devenu hors de portee, puis recherche le Matrix actif le
     * plus proche.
     */
    private void ensureLinked(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return;
        ImagNetworkData data = ImagNetworkData.get(server);

        BlockPos matrixPos = data.matrixOf(pos);
        if (matrixPos != null) {
            MatrixBlockEntity matrix = matrixAt(level, matrixPos);
            if (matrix != null && matrix.canReach(pos)) {
                setConnected(true);
                return;
            }
            // Le Matrix a ete casse, ou son coeur a ete retire : le lien ne
            // veut plus rien dire.
            data.unlink(pos);
        }

        setConnected(false);

        MatrixBlockEntity found = findMatrix(level, pos);
        if (found != null && data.link(found.getBlockPos(), pos)) {
            setConnected(true);
        }
    }

    /**
     * Cherche le Matrix actif le plus proche dans lequel ce noeud peut entrer.
     *
     * Le rayon de recherche est la portee maximale theorique d'un Matrix, car
     * sa portee reelle depend du coeur insere, qu'on ne connait pas avant de
     * l'avoir trouve. Le parcours se fait par chunk ("block entities du chunk") et
     * non bloc par bloc : dans un rayon de 42 blocs cela fait au pire 36 chunks
     * au lieu de 700 000 positions, et seuls les chunks deja charges sont lus.
     *
     * @return le Matrix le plus proche compatible, ou {@code null}
     */
    @Nullable
    private MatrixBlockEntity findMatrix(Level level, BlockPos pos) {
        // Le rayon de recherche est la portee maximale theorique d'un Matrix, car
        // sa portee reelle depend du coeur insere, qu'on ne connait pas avant de
        // l'avoir trouve.
        return BlockEntityScan.nearest(level, pos, MatrixBlockEntity.MAX_RANGE, MatrixBlockEntity.class,
                matrix -> matrix.isWorking() && matrix.canReach(pos));
    }

    @Nullable
    private static MatrixBlockEntity matrixAt(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) return null;
        return level.getBlockEntity(pos) instanceof MatrixBlockEntity matrix ? matrix : null;
    }

    /** Slot 0 : l'energie va de l'objet vers le noeud. */
    private void chargeFromItem() {
        ItemStack stack = inventory.getStackInSlot(SLOT_CHARGE_IN);
        if (stack.isEmpty()) return;

        double missing = getMaxEnergy() - energy;
        if (missing <= 0.0d) return;

        double request = Math.min(getBandwidth(), missing);
        double taken = ModItems.EnergyUnit.discharge(stack, (float) request);
        if (taken > 0.0d) {
            setEnergy(energy + taken);
            setChanged();
        }
    }

    /** Slot 1 : l'energie va du noeud vers l'objet. */
    private void dischargeToItem() {
        ItemStack stack = inventory.getStackInSlot(SLOT_CHARGE_OUT);
        if (stack.isEmpty() || energy <= 0.0d) return;

        double available = Math.min(getBandwidth(), energy);
        float before = ModItems.EnergyUnit.getEnergy(stack);
        float after = ModItems.EnergyUnit.charge(stack, (float) available);
        double given = after - before;

        if (given > 0.0d) {
            setEnergy(energy - given);
            setChanged();
        }
    }

    /**
     * reporte l'energie sur le bloc : palier de texture et etat de connexion.
     * N'ecrit que si quelque chose change, pour ne pas declencher de mise a jour
     * de bloc inutile.
     */
    private void syncBlockState(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof NodeBlock nodeBlock)) return;

        int level0 = nodeBlock.energyLevelFor(energy);
        if (state.getValue(NodeBlock.ENERGY_LEVEL) == level0
                && state.getValue(NodeBlock.CONNECTED) == connected) {
            return;
        }
        level.setBlock(pos, state
                .setValue(NodeBlock.ENERGY_LEVEL, level0)
                .setValue(NodeBlock.CONNECTED, connected), Block.UPDATE_CLIENTS);
    }

    /** Utilise par le Matrix pour signaler l'etat de connexion. */
    public void setConnected(boolean value) {
        if (connected != value) {
            connected = value;
            setChanged();
        }
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    // ------------------------------------------------------------------
    // Capacites Forge
    // ------------------------------------------------------------------

    /**
     * Vue Forge Energy du noeud, pour que les generateurs et les autres mods
     * puissent l'alimenter. Conversion 1:1 avec l'unite du reseau Imag.
     */
    private final class ForgeEnergyAdapter implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (toReceive <= 0) return 0;
            double accepted = Math.min(toReceive, getMaxEnergy() - energy);
            if (accepted <= 0.0d) return 0;
            if (!simulate) {
                setEnergy(energy + accepted);
                setChanged();
            }
            return (int) accepted;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            if (toExtract <= 0) return 0;
            double given = Math.min(toExtract, energy);
            if (given <= 0.0d) return 0;
            if (!simulate) {
                setEnergy(energy - given);
                setChanged();
            }
            return (int) given;
        }

        @Override
        public int getEnergyStored() {
            return (int) energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) getMaxEnergy();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemHandlerCap.cast();
        if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerCap.invalidate();
        energyCap.invalidate();
    }

    // ------------------------------------------------------------------
    // Persistance
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", inventory.serializeNBT());
        tag.putDouble("energy", energy);
        tag.putBoolean("connected", connected);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("inventory"));
        energy = clamp(tag.getDouble("energy"));
        connected = tag.getBoolean("connected");
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
