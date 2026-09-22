package cn.academy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cn.academy.ability.AbilityCapability;
import cn.academy.energy.EnergyReceiver;
import cn.academy.energy.NodeFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Brouilleur d'aptitudes : dans son rayon, personne ne peut utiliser ses
 * competences.
 *
 * Portage de {@code TileAbilityInterferer}, dont la tuile d'origine etait en Scala.
 *
 * <h2>Ce qu'il coute</h2>
 *
 * {@code rayon²} unites d'energie <b>toutes les 10 ticks</b>, soit
 * {@code rayon² / 10} par tick. C'est la formule de l'original, et elle rend la
 * taille du rayon chere : a 10 blocs cela fait 10 par tick, a 100 blocs 1000 par
 * tick, soit bien plus que ce qu'un noeud peut fournir. Un grand brouilleur n'est
 * donc pas une question de place, mais de centrale.
 *
 * Si le tampon ne suit pas, le brouilleur s'eteint de lui-meme au lieu de
 * brouiller gratuitement : c'est le comportement de l'original, qui coupait
 * {@code enabled} quand le paiement echouait.
 *
 * <h2>Qui est brouille</h2>
 *
 * Les joueurs dans le rayon, sauf ceux en creatif et sauf le poseur du bloc : on
 * ne se brouille pas soi-meme. La liste blanche editable de l'original n'est pas
 * portee, elle demandait un champ de saisie ; seul le poseur y figure.
 */
public class AbilityInterfererBlockEntity extends BlockEntity implements MenuProvider, EnergyReceiver {

    /** Emplacement de l'objet d'energie. */
    public static final int SLOT_BATTERY = 0;

    /** Tampon d'energie. {@code TileAbilityInterferer} : 10000. */
    public static final double BUFFER_SIZE = 10_000.0d;

    /** Energie acceptee par tick. {@code IFConstants.LATENCY_MK1} = 50. */
    public static final double BANDWIDTH = 50.0d;

    /** Rayon minimal et maximal, bornes de l'original. */
    public static final int MIN_RANGE = 10;
    public static final int MAX_RANGE = 100;

    /** Pas du reglage de rayon. */
    public static final int RANGE_STEP = 5;

    /** Periode de paiement et d'application, en ticks. */
    private static final int CYCLE_INTERVAL = 10;

    /** Cadence de recherche d'un noeud, en ticks. */
    private static final int NODE_SEARCH_INTERVAL = 100;

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return stack.isEmpty() || stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private double energy;

    private boolean enabled;

    private int range = MIN_RANGE;

    /** Poseur du bloc : il n'est jamais brouille par sa propre machine. */
    @Nullable
    private String placer;

    private boolean linked;

    /** Joueurs actuellement brouilles par cette machine, pour pouvoir les relacher. */
    private final Map<UUID, ServerPlayer> affected = new HashMap<>();

    private int cycleCounter;
    private int searchCounter;
    private int syncCounter;

    public AbilityInterfererBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ABILITY_INTERFERER.get(), pos, state);
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    public static void tick(Level level, BlockPos pos, BlockState state, AbilityInterfererBlockEntity machine) {
        if (!(level instanceof ServerLevel server)) return;

        machine.drawFromBattery();

        if (++machine.cycleCounter >= CYCLE_INTERVAL) {
            machine.cycleCounter = 0;
            machine.runCycle(server);
        }

        if (++machine.searchCounter >= NODE_SEARCH_INTERVAL) {
            machine.searchCounter = 0;
            boolean nowLinked = NodeFinder.ensureLinked(server, pos);
            if (nowLinked != machine.linked) {
                machine.linked = nowLinked;
                machine.setChanged();
            }
        }

        if (++machine.syncCounter >= CYCLE_INTERVAL) {
            machine.syncCounter = 0;
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    /**
     * Un cycle : on paie, puis on applique ou on relache.
     *
     * Le paiement vient en premier : si le tampon ne suit pas, rien n'est brouille
     * et la machine s'eteint, plutot que de brouiller a credit.
     */
    private void runCycle(ServerLevel level) {
        if (!enabled) {
            releaseAll(level);
            applyBlockState(level);
            return;
        }

        double cost = (double) range * range;
        if (energy < cost) {
            energy = 0.0d;
            enabled = false;
            setChanged();
            releaseAll(level);
            applyBlockState(level);
            return;
        }

        energy -= cost;
        setChanged();

        apply(level);
        applyBlockState(level);
    }

    /** Brouille les joueurs du rayon qui ne sont pas proteges. */
    private void apply(ServerLevel level) {
        String key = sourceKey(level);
        AABB box = boxAround();

        Set<UUID> stillAffected = new HashSet<>();
        for (Player player : level.getEntitiesOfClass(Player.class, box)) {
            if (!(player instanceof ServerPlayer serverPlayer)) continue;
            if (isProtected(player)) continue;

            stillAffected.add(player.getUUID());
            affected.put(player.getUUID(), serverPlayer);
            player.getCapability(AbilityCapability.ABILITY_DATA).ifPresent(data ->
                    data.addInterference(key, this::isStillInterfering));
        }

        // Les joueurs sortis du rayon ne doivent plus etre brouilles.
        for (UUID uuid : new HashSet<>(affected.keySet())) {
            if (stillAffected.contains(uuid)) continue;
            release(level, uuid);
        }
    }

    /** Vrai si la source doit encore brouiller, d'apres son porteur. */
    private boolean isStillInterfering() {
        return enabled && !isRemoved();
    }

    private boolean isProtected(Player player) {
        if (player.isCreative()) return true;
        return placer != null && placer.equals(player.getGameProfile().getName());
    }

    /** Detache la source d'un joueur. */
    private void release(ServerLevel level, UUID uuid) {
        ServerPlayer player = affected.remove(uuid);
        if (player == null) return;
        player.getCapability(AbilityCapability.ABILITY_DATA)
                .ifPresent(data -> data.removeInterference(sourceKey(level)));
    }

    /** Detache la source de tous les joueurs brouilles. */
    private void releaseAll(ServerLevel level) {
        for (UUID uuid : new HashSet<>(affected.keySet())) release(level, uuid);
    }

    /** Cle unique de cette machine, reprise de {@code sourceName} de l'original. */
    private String sourceKey(ServerLevel level) {
        return "interferer@" + level.dimension().location() + " " + worldPosition;
    }

    /** Zone d'effet : un cube centre sur le bloc, de cote deux fois le rayon. */
    private AABB boxAround() {
        return new AABB(worldPosition).inflate(range);
    }

    /** L'objet pose dans l'emplacement batterie alimente le tampon au besoin. */
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

    /** Allume le bloc quand il brouille effectivement. */
    private void applyBlockState(Level level) {
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof AbilityInterfererBlock)) return;
        if (state.getValue(AbilityInterfererBlock.ON) != enabled) {
            level.setBlock(worldPosition, state.setValue(AbilityInterfererBlock.ON, enabled), 3);
        }
    }

    // ------------------------------------------------------------------
    // Commandes, depuis l'ecran
    // ------------------------------------------------------------------

    /** Allume ou eteint la machine. Ne peut s'allumer que si le tampon peut payer. */
    public void setEnabled(boolean value) {
        if (value && energy < (double) range * range) return;
        if (enabled == value) return;
        enabled = value;
        setChanged();
        // Le cycle suivant applique ou relache ; pas besoin d'attendre pour l'etat
        // visuel, en revanche, donc on le met a jour tout de suite.
        if (level instanceof ServerLevel server && !enabled) releaseAll(server);
        if (level != null) applyBlockState(level);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setRange(int value) {
        range = Math.max(MIN_RANGE, Math.min(MAX_RANGE, value));
        setChanged();
    }

    public void adjustRange(int delta) {
        setRange(range + delta);
    }

    public int getRange() {
        return range;
    }

    /** Cout d'un cycle, en unites d'energie. */
    public double getCycleCost() {
        return (double) range * range;
    }

    /** Cout ramene au tick, plus parlant pour le joueur. */
    public double getCostPerTick() {
        return getCycleCost() / CYCLE_INTERVAL;
    }

    public String getPlacer() {
        return placer == null ? "" : placer;
    }

    public int getAffectedCount() {
        return affected.size();
    }

    /** Retient le poseur : il ne sera jamais brouille par sa machine. */
    public void setPlacer(Player player) {
        if (placer != null || player == null) return;
        placer = player.getGameProfile().getName();
        setChanged();
    }

    // ------------------------------------------------------------------
    // Etat, pour l'ecran
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
    // Persistance
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", inventory.serializeNBT());
        tag.putDouble("energy", energy);
        tag.putBoolean("enabled", enabled);
        tag.putInt("range", range);
        tag.putBoolean("linked", linked);
        if (placer != null) tag.putString("placer", placer);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("inventory"));
        energy = Math.min(BUFFER_SIZE, Math.max(0.0d, tag.getDouble("energy")));
        // Une machine rechargee repart a l'arret : elle doit de toute facon payer
        // son premier cycle, et personne n'a pu etre brouille pendant l'absence.
        enabled = false;
        range = Math.max(MIN_RANGE, Math.min(MAX_RANGE, tag.getInt("range")));
        if (range == 0) range = MIN_RANGE;
        linked = tag.getBoolean("linked");
        placer = tag.contains("placer") ? tag.getString("placer") : null;
    }

    @Override
    public void setRemoved() {
        // Le block entity s'en va : plus personne ne doit rester brouille.
        if (level instanceof ServerLevel server) releaseAll(server);
        super.setRemoved();
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
        return new AbilityInterfererMenu(containerId, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.academy.ability_interferer");
    }
}
