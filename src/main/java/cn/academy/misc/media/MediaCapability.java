package cn.academy.misc.media;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * La donnee des morceaux d'un joueur, enveloppee pour Forge.
 *
 * <p>Le contenu vit dans {@link MediaAcquireData}, qui ne connait pas Minecraft et se relit
 * donc en JUnit ; cette classe ne fait que le porter et le sauvegarder, comme les autres
 * capacites du port.
 */
public final class MediaCapability {

    public static final Capability<Holder> MEDIA_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    private MediaCapability() {}

    /** L'enveloppe sauvegardable. */
    public static class Holder implements INBTSerializable<CompoundTag> {

        private final MediaAcquireData data = new MediaAcquireData();

        public MediaAcquireData get() {
            return data;
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            data.deserializeNBT(nbt);
        }

        public void copyFrom(Holder other) {
            data.deserializeNBT(other.data.serializeNBT());
        }
    }
}
