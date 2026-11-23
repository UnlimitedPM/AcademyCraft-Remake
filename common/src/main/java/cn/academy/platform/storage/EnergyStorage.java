package cn.academy.platform.storage;

/**
 * Stockage d'energie neutre, calque sur les cinq operations utiles de Forge Energy.
 *
 * <p>Les generateurs et les machines du mod n'utilisent que celles-ci. Forge les expose par une
 * capability ; Fabric les utilisera directement, sans capability.
 */
public interface EnergyStorage {

    int receiveEnergy(int maxReceive, boolean simulate);

    int extractEnergy(int maxExtract, boolean simulate);

    int getEnergyStored();

    int getMaxEnergyStored();

    boolean canExtract();

    boolean canReceive();
}
