package cn.academy.ability.preset.client;

import cn.academy.ability.preset.PresetData;
import net.minecraft.Util;
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

    /**
     * Quand le preréglage en service a changé pour la dernière fois, et celui d'avant.
     *
     * <p>C'est l'horloge du rappel que la barre de CP montre pendant deux secondes. L'original
     * la posait dans son gestionnaire de touche, donc <b>seulement</b> quand le joueur changeait
     * de préréglage lui-même ; ici elle se lit sur la synchronisation du serveur, ce qui couvre
     * aussi les changements venus d'ailleurs (l'écran des préréglages, un changement de catégorie
     * qui remet le premier en service).
     *
     * <p>Le premier envoi du serveur n'est PAS un changement : la valeur de départ de
     * {@link PresetData} vaut zéro, et un joueur qui se reconnecte sur son troisième préréglage
     * verrait sinon le rappel s'allumer sans avoir rien touché. D'où {@link #seen}.
     */
    private static boolean seen;
    private static long switchAt = -1L;
    private static long previousSwitchAt = -1L;

    private ClientPresetData() {}

    /** Remplace tout : le serveur est la seule source de vérité. */
    public static void update(CompoundTag tag) {
        int before = DATA.getCurrentId();
        DATA.deserializeNBT(tag);
        int after = DATA.getCurrentId();

        if (!seen) {
            // Première synchronisation : c'est un état de départ, pas un geste.
            seen = true;
            return;
        }
        if (after != before) {
            previousSwitchAt = switchAt;
            switchAt = Util.getMillis();
        }
    }

    public static PresetData get() {
        return DATA;
    }

    /** La compétence qu'une touche allume en ce moment, ou {@code null}. */
    public static String skillAt(int key) {
        return DATA.getCurrent().nameAt(key);
    }

    /** Quand le préreglage en service a changé, ou {@code -1} s'il n'a jamais changé. */
    public static long switchAt() {
        return switchAt;
    }

    /** Quand il avait changé la fois d'avant, ou {@code -1} : c'est ce qui décide du fondu. */
    public static long previousSwitchAt() {
        return previousSwitchAt;
    }

    /** Oublie tout : on quitte le monde, l'horloge du rappel n'a plus de sens. */
    public static void reset() {
        seen = false;
        switchAt = -1L;
        previousSwitchAt = -1L;
    }
}
