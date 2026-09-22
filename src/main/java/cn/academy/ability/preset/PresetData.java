package cn.academy.ability.preset;

import net.minecraft.nbt.CompoundTag;

/**
 * Les quatre préréglages d'un joueur, et celui qui est actif.
 *
 * <p>Portage de {@code PresetData}, réduit à ce qui n'est pas une donnée de Minecraft : le
 * port range cette donnée dans une <b>capacité du joueur</b> (voir
 * {@code PresetCapability}), et cette classe-ci n'est que le contenu — donc un test unitaire
 * peut la manipuler.
 *
 * <p>Un changement de catégorie <b>efface tout</b>, comme dans l'original : les
 * compétences rangées dans les préréglages ne sont plus apprises, donc les garder n'aurait
 * pas de sens.
 */
public final class PresetData {

    /** Nombre de préréglages, celui de l'original. */
    public static final int MAX_PRESETS = 4;

    private final AbilityPreset[] presets = new AbilityPreset[MAX_PRESETS];

    private int current;

    public PresetData() {
        for (int i = 0; i < MAX_PRESETS; i++) presets[i] = new AbilityPreset();
    }

    public int getCurrentId() {
        return current;
    }

    /** Le préréglage actif : c'est lui que les quatre touches allument. */
    public AbilityPreset getCurrent() {
        return presets[current];
    }

    public AbilityPreset getPreset(int id) {
        if (id < 0 || id >= MAX_PRESETS) return presets[0];
        return presets[id];
    }

    /** Rend actif un préréglage. Les identifiants hors bornes sont ramenés dans le tour. */
    public void switchTo(int id) {
        if (id < 0) return;
        current = id % MAX_PRESETS;
    }

    /** Passe au préréglage suivant, en boucle : c'est ce que fait la touche de changement. */
    public void switchNext() {
        switchTo(current + 1);
    }

    public void setPreset(int id, AbilityPreset preset) {
        if (id < 0 || id >= MAX_PRESETS) return;
        presets[id] = preset;
    }

    /** Efface les quatre préréglages, et remet le premier en service. */
    public void clearAll() {
        for (int i = 0; i < MAX_PRESETS; i++) presets[i] = new AbilityPreset();
        current = 0;
    }

    public PresetData copy() {
        PresetData copy = new PresetData();
        for (int i = 0; i < MAX_PRESETS; i++) copy.presets[i] = presets[i].copy();
        copy.current = current;
        return copy;
    }

    // ------------------------------------------------------------------
    // Persistance
    // ------------------------------------------------------------------

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("preset", current);
        for (int i = 0; i < MAX_PRESETS; i++) {
            tag.put("p" + i, presets[i].serializeNBT());
        }
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        switchTo(tag.getInt("preset"));
        for (int i = 0; i < MAX_PRESETS; i++) {
            presets[i] = new AbilityPreset();
            if (tag.contains("p" + i)) {
                presets[i].deserializeNBT(tag.getCompound("p" + i));
            }
        }
    }
}
