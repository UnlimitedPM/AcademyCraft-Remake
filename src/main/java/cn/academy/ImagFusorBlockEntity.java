package cn.academy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cn.academy.crafting.ImagFusorRecipes;
import cn.academy.energy.EnergyReceiver;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Fusor d'Imag : il affine les cristaux en les baignant dans de la phase liquide.
 *
 * Portage de {@code TileImagFusor}. C'est le second consommateur du reseau, et le
 * premier a consommer aussi un fluide.
 *
 * <h2>Deux ressources</h2>
 *
 * De l'energie du reseau (2000 de tampon, 50 par tick) et de la phase liquide
 * (cuve de 8000 mB). La phase n'arrive pas par un tuyau : on glisse une unite de
 * phase dans l'emplacement dedie, elle fond pour 1000 mB, et l'unite vide
 * ressort dans l'emplacement de sortie des unites. C'est ainsi que l'original
 * remplissait sa cuve, et 8000 mB pour un cristal de haute purete veut donc dire
 * huit unites.
 *
 * <h2>Cinq emplacements</h2>
 *
 * Entree, sortie, unites de phase en entree, unites vides en sortie, et batterie.
 * Les deux emplacements d'unites sont separes parce que l'unite pleine et
 * l'unite vide ne sont pas le meme objet dans le port : l'original jouait sur les
 * metadonnees d'un objet unique.
 */
public class ImagFusorBlockEntity extends BlockEntity implements MenuProvider, EnergyReceiver {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_IMAG_INPUT = 2;
    public static final int SLOT_ENERGY_INPUT = 3;
    public static final int SLOT_IMAG_OUTPUT = 4;
    public static final int SLOT_COUNT = 5;

    /** Tampon d'energie. {@code TileImagFusor} : 2000. */
    public static final double BUFFER_SIZE = 2000.0d;

    /** Energie acceptee par tick. {@code IFConstants.LATENCY_MK1} = 50. */
    public static final double BANDWIDTH = 50.0d;

    /** Capacite de la cuve, en millibassins. */
    public static final int TANK_SIZE = 8000;

    /** Phase obtenue en fondant une unite. */
    public static final int PER_UNIT = 1000;

    /** Duree d'une fusion : {@code WORK_SPEED = 1/120}, donc 120 ticks. */
    public static final int WORK_TICKS = 120;

    /** Energie consommee par tick pendant la fusion. */
    public static final double CONSUME_PER_TICK = 12.0d;

    /** Cadence de recherche de recette quand la machine est au repos. */
    private static final int RECIPE_POLL_TICKS = 10;

    /** Cadence de synchronisation vers le client. */
    private static final int SYNC_INTERVAL = 5;

    /** Cadence de recherche d'un noeud, en ticks. */
    private static final int NODE_SEARCH_INTERVAL = 100;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (stack.isEmpty()) return false;
            return switch (slot) {
                case SLOT_INPUT -> true;
                case SLOT_IMAG_INPUT -> stack.is(ModItems.MATTER_UNIT_PHASE.get());
                case SLOT_ENERGY_INPUT -> stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
                // La sortie et les unites vides sont remplies par la machine.
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
            return isPhaseLiquid(stack.getFluid());
        }
    };

    private double energy;

    /** Vrai quand la machine est raccordee a un noeud du reseau. */
    private boolean linked;

    @Nullable
    private ImagFusorRecipes.Recipe currentRecipe;

    private int workCounter;
    private int pollCounter;
    private int syncCounter;
    private int searchCounter;

    /** Vrai au dernier etat connu du bloc, pour n'ecrire que sur changement. */
    private boolean lit;

    private final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> inventory);
    private final LazyOptional<IFluidHandler> fluidHandlerCap = LazyOptional.of(() -> tank);

    public ImagFusorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IMAG_FUSOR.get(), pos, state);
    }

    /** Vrai si ce fluide est celui de la phase. Source et courant partagent le type. */
    public static boolean isPhaseLiquid(Fluid fluid) {
        return fluid.getFluidType() == ModFluids.PHASE_LIQUID_TYPE.get();
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    public static void tick(Level level, BlockPos pos, BlockState state, ImagFusorBlockEntity fusor) {
        if (!(level instanceof ServerLevel server)) return;

        fusor.meltUnit();
        fusor.drawFromBattery();

        if (fusor.currentRecipe != null) {
            fusor.continueWork(level, pos);
        } else {
            fusor.lookForRecipe();
        }

        if (++fusor.searchCounter >= NODE_SEARCH_INTERVAL) {
            fusor.searchCounter = 0;
            fusor.setLinked(NodeFinder.ensureLinked(server, pos));
        }

        if (++fusor.syncCounter >= SYNC_INTERVAL) {
            fusor.syncCounter = 0;
            level.sendBlockUpdated(pos, state, fusor.getBlockState(), 3);
        }
    }

    /**
     * Fait fondre une unite de phase en tete de l'emplacement dedie, si la cuve a
     * la place et que l'unite vide peut etre rangee.
     */
    private void meltUnit() {
        ItemStack units = inventory.getStackInSlot(SLOT_IMAG_INPUT);
        if (units.isEmpty()) return;
        if (tank.getFluidAmount() + PER_UNIT > TANK_SIZE) return;

        ItemStack empties = inventory.getStackInSlot(SLOT_IMAG_OUTPUT);
        if (!empties.isEmpty() && !empties.is(ModItems.MATTER_UNIT.get())) return;
        if (!empties.isEmpty() && empties.getCount() >= empties.getMaxStackSize()) return;

        // On simule avant de consommer l'unite : si la cuve n'accepte pas ce
        // fluide, il ne faut pas perdre l'unite pour rien.
        FluidStack toMelt = new FluidStack(ModFluids.SOURCE_PHASE_LIQUID.get(), PER_UNIT);
        if (tank.fill(toMelt, IFluidHandler.FluidAction.SIMULATE) < PER_UNIT) return;
        tank.fill(toMelt, IFluidHandler.FluidAction.EXECUTE);

        inventory.setStackInSlot(SLOT_IMAG_INPUT, units.copyWithCount(units.getCount() - 1));
        inventory.setStackInSlot(SLOT_IMAG_OUTPUT, empties.isEmpty()
                ? new ItemStack(ModItems.MATTER_UNIT.get())
                : empties.copyWithCount(empties.getCount() + 1));
        setChanged();
    }

    /** La machine tire depuis l'objet de l'emplacement batterie, si besoin. */
    private void drawFromBattery() {
        ItemStack stack = inventory.getStackInSlot(SLOT_ENERGY_INPUT);
        if (stack.isEmpty() || energy >= BUFFER_SIZE) return;

        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(cap -> {
            int wanted = (int) Math.min(BANDWIDTH, BUFFER_SIZE - energy);
            int taken = cap.extractEnergy(wanted, false);
            if (taken > 0) {
                energy += taken;
                setChanged();
            }
        });
    }

    /**
     * Avance la fusion d'un tick, ou abandonne si une condition n'est plus
     * remplie : entree disparue, energie insuffisante, cuve trop vide, ou sortie
     * qui refuserait le resultat.
     *
     * L'abandon remet l'avancement a zero, comme l'original. Une machine
     * sous-alimentee ne progresse donc pas lentement, elle ne progresse pas.
     */
    private void continueWork(Level level, BlockPos pos) {
        if (isActionBlocked() || energy < CONSUME_PER_TICK
                || tank.getFluidAmount() < currentRecipe.liquid()) {
            currentRecipe = null;
            workCounter = 0;
            setLit(level, pos, false);
            setChanged();
            return;
        }

        energy -= CONSUME_PER_TICK;
        workCounter++;
        setLit(level, pos, true);

        if (workCounter >= WORK_TICKS) finishWork(level, pos);
        else setChanged();
    }

    /** Cherche une recette correspondant a l'objet d'entree, a intervalle regulier. */
    private void lookForRecipe() {
        if (++pollCounter < RECIPE_POLL_TICKS) return;
        pollCounter = 0;

        currentRecipe = ImagFusorRecipes.get(inventory.getStackInSlot(SLOT_INPUT));
        workCounter = 0;
        if (currentRecipe != null) setChanged();
    }

    /** Preleve la phase, consomme l'entree et depose le resultat. */
    private void finishWork(Level level, BlockPos pos) {
        tank.drain(currentRecipe.liquid(), IFluidHandler.FluidAction.EXECUTE);

        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        int remaining = input.getCount() - currentRecipe.inputCount();
        inventory.setStackInSlot(SLOT_INPUT, remaining <= 0 ? ItemStack.EMPTY : input.copyWithCount(remaining));

        ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
        inventory.setStackInSlot(SLOT_OUTPUT, output.isEmpty()
                ? currentRecipe.createOutput()
                : output.copyWithCount(output.getCount() + currentRecipe.outputCount()));

        currentRecipe = null;
        workCounter = 0;
        // L'original remettait le compteur de recherche a zero pour enchainer
        // directement l'objet suivant sans pause.
        pollCounter = RECIPE_POLL_TICKS;
        setLit(level, pos, false);
        setChanged();
    }

    /** Vrai si la fusion ne peut pas continuer : entree ou sortie inutilisable. */
    private boolean isActionBlocked() {
        if (currentRecipe == null) return true;
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (!currentRecipe.matches(input)) return true;
        if (input.getCount() < currentRecipe.inputCount()) return true;
        return !currentRecipe.fits(inventory.getStackInSlot(SLOT_OUTPUT));
    }

    /** Vrai si la machine est en train de fondre. */
    public boolean isWorking() {
        return currentRecipe != null && !isActionBlocked();
    }

    // ------------------------------------------------------------------
    // Etat, pour l'ecran
    // ------------------------------------------------------------------

    /** Avancement de la fusion, entre 0 et 1. */
    public double getWorkProgress() {
        return isWorking() ? (double) workCounter / WORK_TICKS : 0.0d;
    }

    public int getWorkCounter() {
        return workCounter;
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

    private void setLinked(boolean value) {
        if (linked == value) return;
        linked = value;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Allume le bloc pendant la fusion, pour que la machine se voie de loin. */
    private void setLit(Level level, BlockPos pos, boolean value) {
        if (lit == value) return;
        lit = value;

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ImagFusorBlock && state.getValue(ImagFusorBlock.LIT) != value) {
            level.setBlock(pos, state.setValue(ImagFusorBlock.LIT, value), 3);
        }
    }

    // ------------------------------------------------------------------
    // EnergyReceiver
    // ------------------------------------------------------------------

    @Override
    public double getRequiredEnergy() {
        return BUFFER_SIZE - energy;
    }

    @Override
    public double injectEnergy(double amount) {
        double accepted = Math.min(amount, BUFFER_SIZE - energy);
        if (accepted > 0.0d) {
            energy += accepted;
            setChanged();
        }
        return amount - accepted;
    }

    @Override
    public double getBandwidth() {
        return BANDWIDTH;
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
        tag.putInt("work", workCounter);
        tag.putBoolean("linked", linked);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("inventory"));
        tank.readFromNBT(tag.getCompound("tank"));
        energy = Math.min(BUFFER_SIZE, Math.max(0.0d, tag.getDouble("energy")));
        workCounter = tag.getInt("work");
        linked = tag.getBoolean("linked");
        // La recette n'est pas sauvegardee : elle est retrouvee au premier tick a
        // partir de l'objet d'entree, ce qui evite de stocker un rang de recette
        // qui pourrait ne plus rien designer apres une mise a jour.
        currentRecipe = null;
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
        return new ImagFusorMenu(containerId, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.academy.imag_fusor");
    }
}
