package cn.academy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cn.academy.energy.EnergyGenerator;
import cn.academy.energy.NodeFinder;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Generateur de phase : il brule de la phase liquide pour produire de l'energie.
 *
 * Portage de {@code TilePhaseGen}. C'est la troisieme source du reseau, et le
 * debouche naturel du fusor : celui-ci affine les cristaux en consommant de la
 * phase, celui-ci brule la phase en energie.
 *
 * Tampon de 6000, bande passante 50 ({@code IFConstants.LATENCY_MK1}), cuve de
 * 8000 mB. Chaque tick il aspire jusqu'a 100 mB, ce qui donne 50 unites
 * d'energie, et s'arrete des que son tampon est plein — il ne brule donc jamais
 * de phase pour rien.
 *
 * <h2>Les unites de phase</h2>
 *
 * Comme le fusor, il ne se remplit pas par un tuyau : on glisse une unite de
 * phase dans son premier emplacement, elle fond pour 1000 mB, et l'unite vide
 * ressort dans le second. Le troisieme emplacement sert a recharger un objet
 * d'energie directement, comme les autres generateurs.
 */
public class PhaseGeneratorBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity
        implements MenuProvider, EnergyGenerator {

    public static final int SLOT_LIQUID_IN = 0;
    public static final int SLOT_LIQUID_OUT = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_COUNT = 3;

    /** Tampon d'energie. {@code TilePhaseGen} : 6000. */
    public static final double BUFFER_SIZE = 6000.0d;

    /** Energie transmissible par tick. {@code IFConstants.LATENCY_MK1} = 50. */
    public static final double BANDWIDTH = 50.0d;

    /** Capacite de la cuve, en millibassins. */
    public static final int TANK_SIZE = 8000;

    /** Phase apportee par une unite. */
    public static final int PER_UNIT = 1000;

    /** Phase aspiree par tick au maximum. */
    public static final int CONSUME_PER_TICK = 100;

    /** Energie produite par millibassin. */
    public static final double GEN_PER_MB = 0.5d;

    /** Palier de texture, de 0 (cuve vide) a 4 (cuve pleine). */
    private static final int LEVELS = 4;

    private static final int SYNTH_INTERVAL = 10;
    private static final int NODE_SEARCH_INTERVAL = 100;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (stack.isEmpty()) return false;
            return switch (slot) {
                case SLOT_LIQUID_IN -> stack.is(ModItems.MATTER_UNIT_PHASE.get());
                case SLOT_OUTPUT -> stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
                // Les unites vides sont posees par la machine.
                default -> false;
            };
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    /** Cuve de phase liquide. Elle n'accepte que le fluide du mod. */
    private final FluidTank tank = new FluidTank(TANK_SIZE) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return ImagFusorBlockEntity.isPhaseLiquid(stack.getFluid());
        }
    };

    private double energy;

    private boolean linked;

    private int syncCounter;
    private int searchCounter;

    private final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> inventory);
    private final LazyOptional<IFluidHandler> fluidHandlerCap = LazyOptional.of(() -> tank);

    public PhaseGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHASE_GENERATOR.get(), pos, state);
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    public static void tick(Level level, BlockPos pos, BlockState state, PhaseGeneratorBlockEntity generator) {
        if (!(level instanceof ServerLevel server)) return;

        generator.meltUnit();
        generator.generate();
        generator.chargeOutput();

        if (++generator.searchCounter >= NODE_SEARCH_INTERVAL) {
            generator.searchCounter = 0;
            boolean nowLinked = NodeFinder.ensureLinked(server, pos);
            if (nowLinked != generator.linked) {
                generator.linked = nowLinked;
                generator.setChanged();
            }
        }

        if (++generator.syncCounter >= SYNTH_INTERVAL) {
            generator.syncCounter = 0;
            generator.applyLevel(level, pos);
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    /** Fait fondre une unite de phase, si la cuve a la place et que la sortie suit. */
    private void meltUnit() {
        ItemStack units = inventory.getStackInSlot(SLOT_LIQUID_IN);
        if (units.isEmpty()) return;
        if (tank.getFluidAmount() + PER_UNIT > TANK_SIZE) return;

        ItemStack empties = inventory.getStackInSlot(SLOT_LIQUID_OUT);
        if (!empties.isEmpty() && !empties.is(ModItems.MATTER_UNIT.get())) return;
        if (!empties.isEmpty() && empties.getCount() >= empties.getMaxStackSize()) return;

        FluidStack toMelt = new FluidStack(ModFluids.SOURCE_PHASE_LIQUID.get(), PER_UNIT);
        if (tank.fill(toMelt, IFluidHandler.FluidAction.SIMULATE) < PER_UNIT) return;
        tank.fill(toMelt, IFluidHandler.FluidAction.EXECUTE);

        inventory.setStackInSlot(SLOT_LIQUID_IN, units.copyWithCount(units.getCount() - 1));
        inventory.setStackInSlot(SLOT_LIQUID_OUT, empties.isEmpty()
                ? new ItemStack(ModItems.MATTER_UNIT.get())
                : empties.copyWithCount(empties.getCount() + 1));
        setChanged();
    }

    /**
     * Brule de la phase pour remplir le tampon.
     *
     * Reprend {@code TilePhaseGen.getGeneration} : la quantite aspiree est bornee
     * a la fois par {@code CONSUME_PER_TICK} et par la place restante dans le
     * tampon, convertie en millibassins. Un tampon plein n'aspire donc rien.
     */
    private void generate() {
        double room = BUFFER_SIZE - energy;
        if (room < GEN_PER_MB) return;

        int maxDrain = (int) Math.min(CONSUME_PER_TICK, room / GEN_PER_MB);
        if (maxDrain <= 0) return;

        int drained = tank.drain(maxDrain, IFluidHandler.FluidAction.EXECUTE).getAmount();
        if (drained <= 0) return;

        energy = Math.min(BUFFER_SIZE, energy + drained * GEN_PER_MB);
        setChanged();
    }

    /** L'objet pose dans la sortie se recharge depuis le tampon. */
    private void chargeOutput() {
        ItemStack stack = inventory.getStackInSlot(SLOT_OUTPUT);
        if (stack.isEmpty() || energy <= 0.0d) return;

        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(cap -> {
            int accepted = cap.receiveEnergy((int) Math.min(BANDWIDTH, energy), false);
            if (accepted > 0) {
                energy -= accepted;
                setChanged();
            }
        });
    }

    /**
     * Reporte le remplissage de la cuve sur le bloc, pour que la machine se voie
     * de loin.
     *
     * Choix du port : l'original n'avait qu'une seule texture et aucun palier. Le
     * port avait genere cinq variantes et les faisait defiler au clic, ce qui
     * n'avait aucun sens ; elles servent desormais a montrer la cuve, et le clic
     * ouvre l'ecran.
     */
    private void applyLevel(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof PhaseGeneratorBlock)) return;

        int wanted = levelFor(tank.getFluidAmount());
        if (state.getValue(PhaseGeneratorBlock.LEVEL) != wanted) {
            level.setBlock(pos, state.setValue(PhaseGeneratorBlock.LEVEL, wanted), 3);
        }
    }

    /** Palier de texture pour un remplissage donne, de 0 a 4. */
    public static int levelFor(int liquidAmount) {
        if (liquidAmount <= 0) return 0;
        return (int) Math.min(LEVELS, Math.round((double) LEVELS * liquidAmount / TANK_SIZE));
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
    // Etat, pour l'ecran
    // ------------------------------------------------------------------

    /** Vrai si la machine produit, c'est-a-dire si elle a de la phase a bruler. */
    public boolean isProducing() {
        return tank.getFluidAmount() > 0 && energy < BUFFER_SIZE;
    }

    public int getLiquidAmount() {
        return tank.getFluidAmount();
    }

    public int getTankSize() {
        return tank.getCapacity();
    }

    /** Remplit la cuve directement (utilise par les tests). */
    public void setLiquidAmount(int amount) {
        tank.setFluid(amount <= 0 ? FluidStack.EMPTY
                : new FluidStack(ModFluids.SOURCE_PHASE_LIQUID.get(), Math.min(amount, TANK_SIZE)));
        setChanged();
    }

    public double getEnergy() {
        return energy;
    }

    public int getEnergyStored() {
        return (int) energy;
    }

    public int getMaxEnergyStored() {
        return (int) BUFFER_SIZE;
    }

    /** Renseigne le tampon directement (utilise par les tests). */
    public void setEnergy(double value) {
        energy = Math.min(BUFFER_SIZE, Math.max(0.0d, value));
        setChanged();
    }

    public boolean isLinked() {
        return linked;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    // ------------------------------------------------------------------
    // Capacites Forge
    // ------------------------------------------------------------------

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemHandlerCap.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidHandlerCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerCap.invalidate();
        fluidHandlerCap.invalidate();
    }

    // ------------------------------------------------------------------
    // Persistance
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", inventory.serializeNBT());
        tag.put("tank", tank.writeToNBT(new CompoundTag()));
        tag.putDouble("energy", energy);
        tag.putBoolean("linked", linked);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("inventory"));
        tank.readFromNBT(tag.getCompound("tank"));
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

    // ------------------------------------------------------------------
    // Menu
    // ------------------------------------------------------------------

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PhaseGeneratorMenu(containerId, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.academy.phase_gen");
    }
}
