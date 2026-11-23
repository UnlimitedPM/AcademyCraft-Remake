package cn.academy;

import org.jetbrains.annotations.Nullable;

import cn.academy.energy.EnergyGenerator;
import cn.academy.platform.storage.EnergyStorage;
import cn.academy.platform.storage.ItemStorage;
import cn.academy.platform.storage.SimpleItemStorage;
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
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

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
public class SolarGenBlockEntity extends BlockEntity implements MenuProvider, EnergyGenerator,
        cn.academy.StoresItems {

    public static final int SLOT_BATTERY = 0;

    /** Tampon du generateur, reprit de TileSolarGen. */
    private static final double BUFFER_SIZE = 1000.0d;

    /** Energie transmissible par tick. IFConstants.LATENCY_MK2. */
    private static final double BANDWIDTH = 100.0d;

    /** Production par tick en plein soleil. */
    private static final double GENERATION = 3.0d;

    /** Facteur applique sous la pluie. */
    private static final double RAIN_FACTOR = 0.2d;

    /** Cadence de verification du raccordement, en ticks. */
    private static final int LINK_CHECK_INTERVAL = 100;

    private final SimpleItemStorage inventory =
            new SimpleItemStorage(1, slot -> 64,
                    (slot, stack) -> stack.isEmpty() || cn.academy.energy.EnergyItems.isEnergyItem(stack),
                    this::setChanged);

    private double energy;

    /** Vrai quand ce generateur est raccorde a un noeud du reseau. */
    private boolean linked;

    private int searchTicker;

    private final BlockEntityType<?> type;
    private final MenuType<SolarGenMenu> menuType;

    public SolarGenBlockEntity(BlockEntityType<?> type, MenuType<SolarGenMenu> menuType,
                               BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.type = type;
        this.menuType = menuType;
    }

    /** Le type, pour que le bloc puisse comparer celui qu'on lui donne au tick. */
    public BlockEntityType<?> entityType() {
        return type;
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

        if (++be.searchTicker >= LINK_CHECK_INTERVAL) {
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

    /**
     * L'etat du ciel, tel que la page l'anime : le {@code SolarStatus} de l'original.
     *
     * <p>Son image est une planche de trois morceaux, et c'est cet etat qui choisit lequel :
     * {@code STRONG} le premier, {@code STOPPED} celui du milieu, {@code WEAK} le dernier.
     */
    public enum SolarStatus { STRONG, WEAK, STOPPED }

    /** Ce que le generateur produit ce tick-la, en IF, pour l'afficher. */
    public double generation() {
        return level == null ? 0.0d : getGeneration(level, getBlockPos());
    }

    /** Et l'etat du ciel, qui choisit l'image du cadre. */
    public SolarStatus sky() {
        if (level == null || !canGenerate(level, getBlockPos())) return SolarStatus.STOPPED;
        return level.isRaining() ? SolarStatus.WEAK : SolarStatus.STRONG;
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

        // L'original rechargeait l'objet par IFItemManager.charge.
        if (cn.academy.energy.EnergyItems.supports(stack)) {
            double given = cn.academy.energy.EnergyItems.push(stack,
                    Math.min(BANDWIDTH, energy));
            if (given > 0.0d) {
                energy -= given;
                setChanged();
            }
            return;
        }

        int accepted = cn.academy.platform.PlatformHooks.chargeExternalItem(
                stack, (int) Math.min(BANDWIDTH, energy), false);
        if (accepted > 0) {
            energy -= accepted;
            setChanged();
        }
    }

    /** Pousse le surplus vers les blocs adjacents qui acceptent du Forge Energy. */
    private void pushToNeighbors(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (energy <= 0.0d) return;
            int accepted = cn.academy.platform.PlatformHooks.pushEnergyToNeighbor(
                    level, pos, dir, (int) Math.min(BANDWIDTH, energy), false);
            if (accepted > 0) {
                energy -= accepted;
                setChanged();
            }
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
        setLinked(cn.academy.platform.PlatformHooks.ensureNodeLinked(level, worldPosition));
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
    // Energie neutre
    // ------------------------------------------------------------------

    public int getEnergyStored() {
        return (int) energy;
    }

    public int getMaxEnergyStored() {
        return (int) BUFFER_SIZE;
    }

    /** Vue neutre du tampon : les machines voisines peuvent venir y puiser. */
    public EnergyStorage energyStorage() {
        return new EnergyStorage() {
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
        };
    }

    // ------------------------------------------------------------------
    // Persistance
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", inventoryTag());
        tag.putDouble("energy", energy);
        tag.putBoolean("linked", linked);
    }

    private CompoundTag inventoryTag() {
        CompoundTag tag = new CompoundTag();
        inventory.save(tag);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.load(tag.getCompound("inventory"));
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
        return new SolarGenMenu(menuType, containerId, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    public ItemStorage getInventory() {
        return inventory;
    }
}
