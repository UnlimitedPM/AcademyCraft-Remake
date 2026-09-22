package cn.academy.ability.preset;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * La donnée des préréglages d'un joueur, enveloppée pour Forge.
 *
 * <p>Le contenu vit dans {@link PresetData}, qui ne connait pas Minecraft et se relit donc
 * en JUnit ; cette classe ne fait que le porter et le sauvegarder, comme les autres
 * capacités du port.
 */
public final class PresetCapability {

    public static final Capability<Holder> PRESET_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    private PresetCapability() {}

    /** L'enveloppe sauvegardable. */
    public static class Holder implements INBTSerializable<CompoundTag> {

        private final PresetData data = new PresetData();

        public PresetData get() {
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
