package cn.academy;

import javax.annotation.Nullable;

import cn.academy.energy.EnergyGenerator;
import cn.academy.energy.NodeFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Base de l'eolienne : c'est elle qui produit.
 *
 * Portage de {@code TileWindGenBase}. Tampon de 20 000, bande passante 300
 * ({@code IFConstants.LATENCY_MK3}), et une production qui depend de l'altitude.
 *
 * <h2>Ce qu'il faut pour produire</h2>
 *
 * Trois conditions, toutes reprises de l'original :
 * <ul>
 *   <li>la colonne doit etre complete : au moins 8 piliers entre la base et le
 *       rotor, et pas plus de 40 ;</li>
 *   <li>l'helice doit etre installee dans le rotor ;</li>
 *   <li>rien ne doit gener la rotation des pales.</li>
 * </ul>
 *
 * <h2>La production augmente avec l'altitude</h2>
 *
 * {@code lerp(0.5, 1, (y - 70) / 90) * 15}, ou {@code y} est l'altitude du rotor.
 * Autrement dit 7,5 par tick a 70 blocs ou en dessous, 15 a 160 blocs et plus,
 * lineairement entre les deux. Monter la tour de piliers n'est donc pas
 * decoratif, et c'est pour cela que 40 piliers sont autorises.
 */
public class WindgenBaseBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity
        implements MenuProvider, EnergyGenerator {

    /** Emplacement de l'objet a recharger. L'helice se met dans le rotor, pas ici. */
    public static final int SLOT_CHARGE = 0;

    /** Tampon du generateur. {@code TileWindGenBase} : 20000. */
    public static final double BUFFER_SIZE = 20_000.0d;

    /** Energie transmissible par tick. {@code IFConstants.LATENCY_MK3} = 300. */
    public static final double BANDWIDTH = 300.0d;

    /** Production maximale, atteinte en altitude. */
    public static final double MAX_GENERATION_SPEED = 15.0d;

    /** Nombre de piliers exiges entre la base et le rotor. */
    public static final int MIN_PILLARS = 8;

    /** Nombre de piliers au-dela duquel la colonne est refusee. */
    public static final int MAX_PILLARS = 40;

    /** Altitude a partir de laquelle la production est maximale. */
    private static final double FULL_HEIGHT = 160.0d;

    /** Altitude en dessous de laquelle la production est minimale. */
    private static final double MIN_HEIGHT = 70.0d;

    /** Cadence de revision de la structure, en ticks. */
    private static final int STRUCTURE_CHECK_INTERVAL = 10;

    /** Cadence de recherche d'un noeud, en ticks. */
    private static final int NODE_SEARCH_INTERVAL = 100;

    /** Etat de la colonne, repris de {@code TileWindGenBase.Completeness}. */
    public enum Completeness {
        /** Rien au-dessus de la base. */
        BASE_ONLY,
        /** Des piliers, mais pas de rotor au sommet. */
        NO_TOP,
        /** Colonne complete et helice installee, mais rien ne tourne. */
        COMPLETE_NOT_WORKING,
        /** Tout est en place et l'eolienne produit. */
        COMPLETE
    }

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private double energy;

    private Completeness completeness = Completeness.BASE_ONLY;

    /** Rotor trouve lors de la derniere revision, ou {@code null}. */
    @Nullable
    private BlockPos rotorPos;

    private boolean linked;

    private int checkCounter;
    private int searchCounter;
    private int syncCounter;

    public WindgenBaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WINDGEN_BASE.get(), pos, state);
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    public static void tick(Level level, BlockPos pos, BlockState state, WindgenBaseBlockEntity base) {
        if (!(level instanceof ServerLevel server)) return;

        if (++base.checkCounter >= STRUCTURE_CHECK_INTERVAL) {
            base.checkCounter = 0;
            base.completeness = base.scanStructure(level, pos);
            base.applyPoweredState(level, pos);
        }

        double produced = base.getSimulatedGeneration(level);
        if (produced > 0.0d) {
            base.energy = Math.min(BUFFER_SIZE, base.energy + produced);
            base.setChanged();
        }

        base.chargeInsertedItem();

        if (++base.searchCounter >= NODE_SEARCH_INTERVAL) {
            base.searchCounter = 0;
            boolean nowLinked = NodeFinder.ensureLinked(server, pos);
            // Un generateur raccorde mais qui ne produit rien n'a pas besoin
            // d'etre signale : c'est le rotor qui compte pour l'affichage.
            if (nowLinked != base.linked) {
                base.linked = nowLinked;
                base.setChanged();
            }
        }

        if (++base.syncCounter >= 40) {
            base.syncCounter = 0;
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    /**
     * Remonte la colonne : piliers, puis rotor. S'arrete des que la colonne sort
     * de ce qui est attendu, ou au-dela du nombre maximal de piliers.
     */
    private Completeness scanStructure(Level level, BlockPos pos) {
        rotorPos = null;
        int pillars = 0;

        // La base occupe deux blocs : le premier pilier est deux blocs plus haut.
        for (int step = 0; step <= MAX_PILLARS + 1; step++) {
            BlockPos probe = pos.offset(0, 2 + step, 0);
            if (!level.isLoaded(probe)) return Completeness.NO_TOP;

            BlockState state = level.getBlockState(probe);
            if (state.is(ModBlocks.WINDGEN_PILLAR.get())) {
                if (++pillars > MAX_PILLARS) return Completeness.NO_TOP;
                continue;
            }

            if (state.is(ModBlocks.WINDGEN_MAIN.get())) {
                if (!(level.getBlockEntity(probe) instanceof WindgenMainBlockEntity rotor)) {
                    return Completeness.NO_TOP;
                }
                rotorPos = probe.immutable();
                return pillars >= MIN_PILLARS ? Completeness.COMPLETE : Completeness.NO_TOP;
            }

            return pillars == 0 ? Completeness.BASE_ONLY : Completeness.NO_TOP;
        }
        return Completeness.NO_TOP;
    }

    /** Vrai si tout est en place pour produire. */
    private boolean shouldGenerate(Level level) {
        if (completeness != Completeness.COMPLETE || rotorPos == null) return false;
        if (!(level.getBlockEntity(rotorPos) instanceof WindgenMainBlockEntity rotor)) return false;
        return rotor.isFanInstalled() && rotor.isNoObstacle();
    }

    /** Etat affiche : une colonne complete mais a l'arret se distingue d'une colonne incomplete. */
    public Completeness getCompleteness(Level level) {
        if (completeness == Completeness.COMPLETE && !shouldGenerate(level)) {
            return Completeness.COMPLETE_NOT_WORKING;
        }
        return completeness;
    }

    /**
     * Production d'un tick, ou 0 si rien ne tourne.
     *
     * Reprend {@code lerp(0.5, 1, clamp01((y - 70) / 90)) * MAX_GENERATION_SPEED}
     * de l'original, avec l'altitude du rotor et non celle de la base.
     */
    public double getSimulatedGeneration(Level level) {
        if (!shouldGenerate(level) || rotorPos == null) return 0.0d;

        double heightFactor = Mth.lerp(
                (float) Mth.clamp((rotorPos.getY() - MIN_HEIGHT) / (FULL_HEIGHT - MIN_HEIGHT), 0.0d, 1.0d),
                0.5f, 1.0f);
        return heightFactor * MAX_GENERATION_SPEED;
    }

    /** L'objet pose dans la base se recharge depuis le tampon. */
    private void chargeInsertedItem() {
        ItemStack stack = inventory.getStackInSlot(SLOT_CHARGE);
        if (stack.isEmpty() || energy <= 0.0d) return;

        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(cap -> {
            int accepted = cap.receiveEnergy((int) Math.min(BANDWIDTH, energy), false);
            if (accepted > 0) {
                energy -= accepted;
                setChanged();
            }
        });
    }

    /** Allume la base quand elle produit, pour que cela se voie. */
    private void applyPoweredState(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof WindgenBaseBlock)) return;
        if (state.getValue(WindgenBaseBlock.HALF) != net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
            return;
        }

        boolean powered = shouldGenerate(level);
        if (state.getValue(WindgenBaseBlock.POWERED) != powered) {
            level.setBlock(pos, state.setValue(WindgenBaseBlock.POWERED, powered), 3);
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
    // Etat, pour les tests et l'affichage
    // ------------------------------------------------------------------

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

    @Nullable
    public BlockPos getRotorPos() {
        return rotorPos;
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
        tag.putInt("completeness", completeness.ordinal());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("inventory"));
        energy = Math.min(BUFFER_SIZE, Math.max(0.0d, tag.getDouble("energy")));
        linked = tag.getBoolean("linked");
        completeness = completenessOf(tag.getInt("completeness"));
        // La structure est revue au premier tick : inutile de sauvegarder la
        // position du rotor, elle pourrait avoir bouge pendant l'absence.
        rotorPos = null;
    }

    private static Completeness completenessOf(int ordinal) {
        Completeness[] values = Completeness.values();
        if (ordinal < 0 || ordinal >= values.length) return Completeness.BASE_ONLY;
        return values[ordinal];
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
        return new WindgenBaseMenu(containerId, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.academy.windgen_base");
    }
}
