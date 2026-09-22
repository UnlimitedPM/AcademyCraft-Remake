package cn.academy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cn.academy.crafting.MetalFormerMode;
import cn.academy.crafting.MetalFormerRecipes;
import cn.academy.energy.EnergyReceiver;
import cn.academy.energy.NodeFinder;
import cn.academy.sound.MachineLoops;
import cn.academy.sound.MachineSounds;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Formeur de metal : le premier consommateur du reseau energetique Imag.
 *
 * Portage de {@code TileMetalFormer}. Il transforme un objet en un autre, dans
 * l'un de ses quatre modes, en consommant 13,3 unites d'energie par tick pendant
 * 60 ticks — soit 798 unites par objet. Ces chiffres sont ceux de l'original.
 *
 * <h2>Comment il obtient son energie</h2>
 *
 * Le formeur est un recepteur : il se raccorde a un noeud proche (comme le
 * generateur solaire), et le noeud pousse l'energie dans son tampon de 3 000
 * unites a raison de 50 maximum par tick. Il peut aussi tirer depuis l'objet
 * pose dans son emplacement de batterie.
 *
 * <h2>Le cycle de travail</h2>
 *
 * Quand une recette correspond a l'objet d'entree, le formeur consomme son
 * energie et fait avancer un compteur. Si l'energie manque, le travail est
 * abandonne et repart de zero au tick suivant : l'original ne mettait pas le
 * compteur en pause, il le remettait a zero. Un formeur sous-alimente ne
 * progresse donc pas lentement, il ne progresse pas du tout.
 */
public class MetalFormerBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity
        implements MenuProvider, EnergyReceiver {

    public static final int SLOT_IN = 0;
    public static final int SLOT_OUT = 1;
    public static final int SLOT_BATTERY = 2;
    public static final int SLOT_COUNT = 3;

    /** Tampon interne. {@code TileMetalFormer} : 3000. */
    public static final double BUFFER_SIZE = 3000.0d;

    /** Energie acceptee par tick. {@code IFConstants.LATENCY_MK1} = 50. */
    public static final double BANDWIDTH = 50.0d;

    /** Duree d'une transformation, en ticks. */
    public static final int WORK_TICKS = 60;

    /** Cout par tick pendant la transformation. */
    public static final double CONSUME_PER_TICK = 13.3d;

    /**
     * Cadence de recherche de recette quand la machine est au repos.
     * L'original comptait 5 ticks avant de regarder a nouveau.
     */
    private static final int RECIPE_POLL_TICKS = 5;

    /** Cadence de synchronisation vers le client, pour la barre de progression. */
    private static final int SYNC_INTERVAL = 10;

    /** Cadence de recherche d'un noeud, en ticks. */
    private static final int NODE_SEARCH_INTERVAL = 100;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            // On refuse de mettre quoi que ce soit dans la sortie : c'est la
            // machine qui la remplit.
            if (slot == SLOT_OUT) return false;
            return true;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private double energy;

    private MetalFormerMode mode = MetalFormerMode.PLATE;

    /** Vrai quand la machine est raccordee a un noeud du reseau. */
    private boolean linked;

    /** Recette en cours, ou {@code null} si la machine est au repos. */
    @Nullable
    private MetalFormerRecipes.Recipe current;

    private int workCounter;
    private int pollCounter;
    private int syncCounter;
    private int searchCounter;

    /**
     * L'etat de marche tel qu'il vient de la balise de synchronisation, donc tel que le
     * client le lit. Le client n'a ni recette ni energie : c'est cette valeur-la, et non
     * {@link #isWorking()}, qui allume la boucle sonore de la machine.
     */
    private boolean syncedWorking;

    public MetalFormerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.METAL_FORMER.get(), pos, state);
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    public static void tick(Level level, BlockPos pos, BlockState state, MetalFormerBlockEntity former) {
        if (level.isClientSide) {
            // Chez le client, il n'y a rien a faire avancer : seulement un son a faire
            // tourner tant que la machine travaille. L'etat vient de la balise de
            // synchronisation, que le serveur renvoie dix fois par seconde.
            MachineSounds.tick(level, pos, MachineLoops.forMachine(MachineLoops.METAL_FORMER),
                    former.syncedWorking);
            return;
        }
        if (!(level instanceof ServerLevel server)) return;

        former.drawFromBattery();

        if (former.current != null) {
            former.continueWork();
        } else {
            former.lookForRecipe();
        }

        if (++former.searchCounter >= NODE_SEARCH_INTERVAL) {
            former.searchCounter = 0;
            former.setLinked(NodeFinder.ensureLinked(server, pos));
        }

        if (++former.syncCounter >= SYNC_INTERVAL) {
            former.syncCounter = 0;
            former.syncToClient(level, pos);
        }
    }

    /** La machine tire depuis l'objet de l'emplacement batterie, si besoin. */
    private void drawFromBattery() {
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
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

    /** Avance le travail d'un tick, ou abandonne la recette si l'energie manque. */
    private void continueWork() {
        if (isActionBlocked() || energy < CONSUME_PER_TICK) {
            // L'original remettait le compteur a zero et oubliait la recette :
            // une machine sous-alimentee ne progresse pas du tout, elle ne
            // progresse pas lentement.
            current = null;
            workCounter = 0;
            setChanged();
            return;
        }

        energy -= CONSUME_PER_TICK;
        workCounter++;
        setChanged();

        if (workCounter >= WORK_TICKS) finishWork();
    }

    /** Cherche une recette correspondant a l'objet d'entree, a intervalle regulier. */
    private void lookForRecipe() {
        if (++pollCounter < RECIPE_POLL_TICKS) return;
        pollCounter = 0;

        current = MetalFormerRecipes.get(inventory.getStackInSlot(SLOT_IN), mode);
        workCounter = 0;
        if (current != null) setChanged();
    }

    /** Consomme l'entree et depose le resultat. */
    private void finishWork() {
        ItemStack input = inventory.getStackInSlot(SLOT_IN);
        ItemStack output = inventory.getStackInSlot(SLOT_OUT);

        // On remplace les piles au lieu de les modifier en place : un
        // ItemStackHandler peut rendre une copie, et une modification sur une
        // copie serait silencieusement perdue.
        int remaining = input.getCount() - current.inputCount();
        inventory.setStackInSlot(SLOT_IN,
                remaining <= 0 ? ItemStack.EMPTY : input.copyWithCount(remaining));

        if (output.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUT, current.createOutput());
        } else {
            inventory.setStackInSlot(SLOT_OUT, output.copyWithCount(output.getCount() + current.outputCount()));
        }

        current = null;
        workCounter = 0;
        setChanged();
    }

    /** Vrai si la sortie ne peut pas recevoir le resultat, ou si l'entree a change. */
    private boolean isActionBlocked() {
        if (current == null) return true;
        if (!current.accepts(inventory.getStackInSlot(SLOT_IN), mode)) return true;
        return !current.fits(inventory.getStackInSlot(SLOT_OUT));
    }

    // ------------------------------------------------------------------
    // Etat, pour l'ecran
    // ------------------------------------------------------------------

    public MetalFormerMode getMode() {
        return mode;
    }

    /**
     * Change de mode. Appele par le bouton de l'ecran, donc uniquement cote
     * serveur : le mode est ensuite renvoye au client par la synchronisation.
     */
    public void cycleMode(int delta) {
        mode = mode.cycle(delta);
        // Changer de mode peut invalider la recette en cours.
        current = null;
        workCounter = 0;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Avancement de la transformation, entre 0 et 1. */
    public double getWorkProgress() {
        return isWorking() ? (double) workCounter / WORK_TICKS : 0.0d;
    }

    public int getWorkCounter() {
        return workCounter;
    }

    public boolean isWorking() {
        return current != null && !isActionBlocked();
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

    /** Vrai si la machine est raccordee a un noeud du reseau. */
    public boolean isLinked() {
        return linked;
    }

    /**
     * Change l'etat de raccordement et previent le client.
     *
     * Le drapeau n'est pas dans le menu mais dans le block entity : il faut donc
     * une mise a jour de bloc, sinon l'ecran continuerait d'afficher l'ancien etat.
     */
    private void setLinked(boolean value) {
        if (linked == value) return;
        linked = value;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Renseigne le tampon directement (utilise par les tests). */
    public void setEnergy(double value) {
        energy = Math.min(BUFFER_SIZE, Math.max(0.0d, value));
        setChanged();
    }

    /** Force la recherche d'une recette au prochain tick, sans attendre le delai. */
    public void resetRecipePolling() {
        pollCounter = RECIPE_POLL_TICKS;
    }

    public ItemStackHandler getInventory() {
        return inventory;
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
    // Synchronisation
    // ------------------------------------------------------------------

    private void syncToClient(Level level, BlockPos pos) {
        level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", inventory.serializeNBT());
        tag.putDouble("energy", energy);
        tag.putInt("mode", mode.ordinal());
        tag.putInt("work", workCounter);
        tag.putBoolean("linked", linked);
        // L'etat de marche voyage avec le reste : c'est ce que le client lit pour savoir
        // s'il doit faire tourner la boucle sonore de la machine, sans un paquet de plus.
        tag.putBoolean("working", isWorking());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("inventory"));
        energy = Math.min(BUFFER_SIZE, Math.max(0.0d, tag.getDouble("energy")));
        mode = MetalFormerMode.byOrdinal(tag.getInt("mode"));
        workCounter = tag.getInt("work");
        linked = tag.getBoolean("linked");
        syncedWorking = tag.getBoolean("working");
        // La recette n'est pas sauvegardee : elle est retrouvee au premier tick
        // a partir de l'objet d'entree, ce qui evite de stocker un identifiant
        // de recette qui pourrait disparaitre entre deux versions.
        current = null;
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
        return new MetalFormerMenu(containerId, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.academy.metal_former");
    }
}
