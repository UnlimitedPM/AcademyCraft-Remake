package cn.academy.ability.develop;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Fournit {@link PortableDevData} a un joueur, et le sauvegarde avec lui. */
public class PortableDevDataProvider implements ICapabilitySerializable<CompoundTag> {

    private final PortableDevData data = new PortableDevData();
    private final LazyOptional<PortableDevData> optional = LazyOptional.of(() -> data);

    /**
     * Le porteur est pose ici, et n'est pas sauvegarde : c'est ce qui permet a
     * l'avancement de lire l'objet tenu sans stocker une reference qui ne veut rien dire
     * hors du jeu.
     */
    public PortableDevDataProvider(Player owner) {
        data.setOwner(owner);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == PortableDevCapability.PORTABLE_DEV ? optional.cast() : LazyOptional.empty();
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
