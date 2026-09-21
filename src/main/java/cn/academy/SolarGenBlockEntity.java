package cn.academy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cn.academy.energy.EnergyGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Generateur solaire. Portage de {@code TileSolarGen} et de {@code TileGeneratorBase}.
 *
 * Il produit 3 unites par tick en plein jour avec vue sur le ciel, 0,6 sous la
 * pluie, rien la nuit. Ces valeurs etaient auparavant inventees dans le port
 * (32 000 de tampon, 20 par tick) : elles sont ici remises sur celles de
 * l'original ({@code brightLev * 3.0}, tampon 1 000, bande passante 100 =
 * {@code IFConstants.LATENCY_MK2}).
 *
 * Le tampon est un {@code double}, comme dans l'original : 0,6 par tick sous la
 * pluie ne survivrait pas a un tampon entier.
 *
 * <h2>Deux façons de fournir son energie</h2>
 *
 * Le generateur appartient a un <b>noeud sans fil</b> — il en cherche un tout
 * seul et s'y raccorde — et le noeud vient chercher l'energie chaque tick via
 * {@link #provideEnergy}. Il pousse aussi son surplus vers les blocs adjacents
 * qui exposent du Forge Energy : c'est un ajout du port, l'original n'ayant que
 * le sans-fil, mais cela evite de devoir poser un noeud pour tester le bloc.
 */
public class SolarGenBlockEntity extends BlockEntity implements MenuProvider, EnergyGenerator {

    public static final int SLOT_BATTERY = 0;

    /** Tampon du generateur, reprit de TileSolarGen. */
    private static final double BUFFER_SIZE = 1000.0d;

    /** Energie transmissible par tick. IFConstants.LATENCY_MK2. */
    private static final double BANDWIDTH = 100.0d;

    /** Production par tick en plein soleil. */
    private static final double GENERATION = 3.0d;

    /** Facteur applique sous la pluie. */
    private static final double RAIN_FACTOR = 0.2d;

    /** Cadence de recherche d'un noeud, en ticks. */
    private static final int NODE_SEARCH_INTERVAL = 100;

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private double energy;

    /** Vrai quand ce generateur est raccorde a un noeud du reseau. */
    private boolean linked;

    private int searchTicker;

    private final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> inventory);
    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(ForgeEnergyAdapter::new);

    public SolarGenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_GEN.get(), pos, state);
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    public static void tick(Level level, BlockPos pos, BlockState state, SolarGenBlockEntity be) {
        if (!(level instanceof ServerLevel server)) return;

        double produced = be.getGeneration(level, pos);
        if (produced > 0.0d) be.addEnergy(produced);

        be.chargeBatterySlot();
        be.pushToNeighbors(level, pos);

        if (++be.searchTicker >= NODE_SEARCH_INTERVAL) {
            be.searchTicker = 0;
            be.ensureLinked(server);
        }
    }

    /**
     * Production d'un tick. Reprise de {@code TileSolarGen.getGeneration} :
     * jour et vue sur le ciel, divisé par cinq sous la pluie.
     */
    private double getGeneration(Level level, BlockPos pos) {
        if (!canGenerate(level, pos)) return 0.0d;
        return GENERATION * (level.isRaining() ? RAIN_FACTOR : 1.0d);
    }

    private boolean canGenerate(Level level, BlockPos pos) {
        long time = level.getDayTime() % 24000L;
        boolean isDay = time >= 0L && time <= 12500L;
        return isDay && level.canSeeSky(pos.above());
    }

    /** Ajoute a l'energie du tampon, sans jamais le depasser. */
    private void addEnergy(double amount) {
        energy = Math.min(BUFFER_SIZE, energy + amount);
        setChanged();
    }

    /** Renseigne le tampon directement (utilise par les tests). */
    public void setEnergy(double amount) {
        energy = Math.min(BUFFER_SIZE, Math.max(0.0d, amount));
        setChanged();
    }

    /** Slot batterie : le tampon recharge l'objet insere. */
    private void chargeBatterySlot() {
        var stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (stack.isEmpty() || energy <= 0.0d) return;

        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(cap -> {
            int accepted = cap.receiveEnergy((int) Math.min(BANDWIDTH, energy), false);
            if (accepted > 0) {
                energy -= accepted;
                setChanged();
            }
        });
    }

    /** Pousse le surplus vers les blocs adjacents qui acceptent du Forge Energy. */
    private void pushToNeighbors(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (energy <= 0.0d) return;
            var neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null) continue;
            neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(cap -> {
                if (!cap.canReceive()) return;
                int accepted = cap.receiveEnergy((int) Math.min(BANDWIDTH, energy), false);
                if (accepted > 0) {
                    energy -= accepted;
                    setChanged();
                }
            });
        }
    }

    // ------------------------------------------------------------------
    // EnergyGenerator
    // ------------------------------------------------------------------

    @Override
    public double provideEnergy(double requested) {
        if (requested <= 0.0d) return 0.0d;
        double given = Math.min(requested, energy);
        if (given > 0.0d) {
            energy -= given;
            setChanged();
        }
        return given;
    }

    @Override
    public double getBandwidth() {
        return BANDWIDTH;
    }

    // ------------------------------------------------------------------
    // Raccordement au noeud
    // ------------------------------------------------------------------

    public boolean isLinked() {
        return linked;
    }

    /**
     * Verifie le raccordement et le repare au besoin. Le travail lui-meme est
     * dans {@link cn.academy.energy.NodeFinder}, partage avec toutes les autres
     * machines du reseau.
     */
    private void ensureLinked(ServerLevel level) {
        setLinked(cn.academy.energy.NodeFinder.ensureLinked(level, worldPosition));
    }

    /**
     * Change l'etat de raccordement et previent le client.
     *
     * Le drapeau n'est pas dans un menu mais dans le block entity : il faut donc
     * une mise a jour de bloc, sinon l'ecran continuerait d'afficher l'ancien etat
     * jusqu'au prochain rechargement du monde.
     */
    private void setLinked(boolean value) {
        if (linked == value) return;
        linked = value;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ------------------------------------------------------------------
    // Capacites Forge
    // ------------------------------------------------------------------

    public int getEnergyStored() {
        return (int) energy;
    }

    public int getMaxEnergyStored() {
        return (int) BUFFER_SIZE;
    }

    /** Vue Forge Energy du tampon : les machines voisines peuvent venir y puiser. */
    private final class ForgeEnergyAdapter implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return 0; // un generateur ne se recharge pas par ce chemin
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            if (toExtract <= 0) return 0;
            double given = Math.min(toExtract, energy);
            if (!simulate && given > 0.0d) {
                energy -= given;
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
            return (int) BUFFER_SIZE;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
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
        tag.putBoolean("linked", linked);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("inventory"));
        energy = Math.min(BUFFER_SIZE, Math.max(0.0d, tag.getDouble("energy")));
        linked = tag.getBoolean("linked");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SolarGenMenu(containerId, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.academy.solar_gen");
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }
}
