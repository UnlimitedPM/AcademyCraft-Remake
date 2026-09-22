package cn.academy.ability.preset;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Un préréglage : quatre touches, et ce qu'elles allument.
 *
 * <p>Portage de {@code PresetData.Preset}, avec une difference de forme : l'original gardait
 * une référence vers la compétence elle-même, le port range son <b>nom</b> — comme il le
 * fait partout ailleurs (compétences apprises, tutoriels, marques de téléportation). Un nom
 * se sauvegarde, se met en réseau et se compare sans registre, donc cette classe est pure et
 * se relit en JUnit.
 *
 * <p>C'est la réponse de l'original à la pénurie de touches : plutôt que d'en donner une à
 * chacune des trente-cinq compétences, il n'y a que <b>quatre</b> touches d'aptitude, et
 * quatre préréglages pour dire ce qu'elles font.
 */
public final class AbilityPreset {

    /** Nombre de touches d'aptitude, celui de l'original. */
    public static final int MAX_KEYS = 4;

    private final List<String> slots = new ArrayList<>(MAX_KEYS);

    public AbilityPreset() {
        for (int i = 0; i < MAX_KEYS; i++) slots.add(null);
    }

    /**
     * Met une compétence sur une touche, ou l'efface si {@code skillName} est {@code null}.
     *
     * <p>Une compétence n'apparaît qu'une fois par préréglage : la poser ailleurs la retire
     * de là où elle était. Sinon la même compétence partirait de deux touches à la fois, ce
     * que l'original évitait déjà.
     */
    public void assign(int key, @Nullable String skillName) {
        if (key < 0 || key >= MAX_KEYS) return;
        if (skillName != null) {
            int previous = slots.indexOf(skillName);
            if (previous >= 0 && previous != key) slots.set(previous, null);
        }
        slots.set(key, skillName);
    }

    /** Efface une touche. */
    public void clear(int key) {
        assign(key, null);
    }

    /** Le nom de la compétence d'une touche, ou {@code null} si la touche est libre. */
    @Nullable
    public String nameAt(int key) {
        if (key < 0 || key >= MAX_KEYS) return null;
        return slots.get(key);
    }

    public boolean hasMapping(int key) {
        return nameAt(key) != null;
    }

    /** Vrai si ce préréglage allume cette compétence. */
    public boolean contains(@Nullable String skillName) {
        return skillName != null && slots.contains(skillName);
    }

    /** Vrai si aucune touche n'est prise. */
    public boolean isEmpty() {
        for (String slot : slots) {
            if (slot != null) return false;
        }
        return true;
    }

    /** Efface tout. */
    public void clear() {
        for (int i = 0; i < MAX_KEYS; i++) slots.set(i, null);
    }

    /** L'ensemble des compétences du préréglage, pour l'affichage. */
    public List<String> mappedSkills() {
        List<String> mapped = new ArrayList<>();
        for (String slot : slots) {
            if (slot != null) mapped.add(slot);
        }
        return List.copyOf(mapped);
    }

    public AbilityPreset copy() {
        AbilityPreset copy = new AbilityPreset();
        for (int i = 0; i < MAX_KEYS; i++) copy.slots.set(i, slots.get(i));
        return copy;
    }

    // ------------------------------------------------------------------
    // Persistance
    // ------------------------------------------------------------------

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < MAX_KEYS; i++) {
            String name = slots.get(i);
            if (name != null) tag.putString(Integer.toString(i), name);
        }
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        clear();
        for (int i = 0; i < MAX_KEYS; i++) {
            if (tag.contains(Integer.toString(i))) {
                slots.set(i, tag.getString(Integer.toString(i)));
            }
        }
    }
}
