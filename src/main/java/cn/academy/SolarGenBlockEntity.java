package cn.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Port of original TileSolarGen: charges an inserted energy-storing item during daytime
 * while exposed to the sky, and pushes any surplus Forge Energy to adjacent blocks.
 */
public class SolarGenBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_BATTERY = 0;
    private static final int CAPACITY = 32000;
    private static final int GENERATION_PER_TICK = 20;
    private static final int MAX_EXTRACT = 200;

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final EnergyStorage energy = new EnergyStorage(CAPACITY, 0, MAX_EXTRACT);

    private final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> inventory);
    private final LazyOptional<EnergyStorage> energyCap = LazyOptional.of(() -> energy);

    public SolarGenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_GEN.get(), pos, state);
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, SolarGenBlockEntity be) {
        if (level.isClientSide) return;

        if (be.canGenerate(level, pos)) {
            be.energy.receiveEnergy(GENERATION_PER_TICK, false);
        }

        be.chargeBatterySlot();
        be.pushToNeighbors(level, pos);
    }

    private boolean canGenerate(net.minecraft.world.level.Level level, BlockPos pos) {
        long time = level.getDayTime() % 24000;
        boolean isDay = time >= 0 && time <= 12500;
        return isDay && !level.isRaining() && level.canSeeSky(pos.above());
    }

    private void chargeBatterySlot() {
        var stack = inventory.getStackInSlot(SLOT_BATTERY);
        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(cap -> {
            int accepted = cap.receiveEnergy(Math.min(MAX_EXTRACT, energy.getEnergyStored()), false);
            if (accepted > 0) energy.extractEnergy(accepted, false);
        });
    }

    private void pushToNeighbors(net.minecraft.world.level.Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (energy.getEnergyStored() <= 0) return;
            var neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null) continue;
            neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(cap -> {
                if (!cap.canReceive()) return;
                int toSend = Math.min(MAX_EXTRACT, energy.getEnergyStored());
                int accepted = cap.receiveEnergy(toSend, false);
                if (accepted > 0) energy.extractEnergy(accepted, false);
            });
        }
    }

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
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

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", inventory.serializeNBT());
        tag.putInt("energy", energy.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("inventory"));
        energy.receiveEnergy(tag.getInt("energy"), false);
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
