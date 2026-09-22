package cn.academy.ability.preset.client;

import cn.academy.ability.preset.PresetData;
import net.minecraft.nbt.CompoundTag;

/**
 * Les préréglages tels que le client les connait.
 *
 * <p>Même patron que {@code ClientAbilityData} et {@code ClientTutorialData} : le serveur
 * pousse, le client lit, et personne ne décide rien ici. C'est ce que liront les quatre
 * touches d'aptitude — et, avant elles, l'écran qui les règle.
 */
public final class ClientPresetData {

    private static final PresetData DATA = new PresetData();

    private ClientPresetData() {}

    /** Remplace tout : le serveur est la seule source de vérité. */
    public static void update(CompoundTag tag) {
        DATA.deserializeNBT(tag);
    }

    public static PresetData get() {
        return DATA;
    }

    /** La compétence qu'une touche allume en ce moment, ou {@code null}. */
    public static String skillAt(int key) {
        return DATA.getCurrent().nameAt(key);
    }
}
