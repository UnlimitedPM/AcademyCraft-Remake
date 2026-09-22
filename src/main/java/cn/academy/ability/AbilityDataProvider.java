package cn.academy.ability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AbilityDataProvider implements ICapabilitySerializable<CompoundTag> {

    private final AbilityData data = new AbilityData();
    private final LazyOptional<AbilityData> optional = LazyOptional.of(() -> data);

    /**
     * Le proprietaire est pose ici, une fois, a l'attachement de la capacite.
     *
     * <p>Le fournisseur est construit dans {@code AbilityEvents}, qui a le joueur sous la
     * main : sans ce passage, les donnees ne sauraient pas a qui accorder les succes qui
     * se declenchent en leur sein.
     */
    public AbilityDataProvider(net.minecraft.world.entity.player.Player owner) {
        data.setOwner(owner);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == AbilityCapability.ABILITY_DATA ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.deserializeNBT(nbt);
    }
}
