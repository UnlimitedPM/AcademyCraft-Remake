package cn.academy.ability.preset;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Fournit les préréglages d'un joueur, et les sauvegarde avec lui. */
public class PresetDataProvider implements ICapabilitySerializable<CompoundTag> {

    private final PresetCapability.Holder holder = new PresetCapability.Holder();
    private final LazyOptional<PresetCapability.Holder> optional = LazyOptional.of(() -> holder);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == PresetCapability.PRESET_DATA ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return holder.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        holder.deserializeNBT(nbt);
    }
}
