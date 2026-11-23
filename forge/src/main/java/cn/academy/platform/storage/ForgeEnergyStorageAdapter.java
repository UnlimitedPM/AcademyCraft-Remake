package cn.academy.platform.storage;

import net.minecraftforge.energy.IEnergyStorage;

/**
 * Adaptateur inverse : expose un {@link EnergyStorage} neutre comme {@code IEnergyStorage} Forge.
 */
public final class ForgeEnergyStorageAdapter implements IEnergyStorage {
    private final EnergyStorage storage;

    public ForgeEnergyStorageAdapter(EnergyStorage storage) {
        this.storage = storage;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        return storage.receiveEnergy(toReceive, simulate);
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        return storage.extractEnergy(toExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        return storage.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return storage.getMaxEnergyStored();
    }

    @Override
    public boolean canExtract() {
        return storage.canExtract();
    }

    @Override
    public boolean canReceive() {
        return storage.canReceive();
    }
}
