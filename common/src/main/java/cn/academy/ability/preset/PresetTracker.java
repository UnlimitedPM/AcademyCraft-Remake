package cn.academy.ability.preset;

import cn.academy.ability.preset.network.SyncPresetPacket;
import cn.academy.platform.PlatformHooks;
import cn.academy.platform.network.Network;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Les prereglages d'un joueur : lecture, envoi au client, remise a zero.
 *
 * <p>Meme patron que les autres donnees de joueur du port : la donnee vit chez le chargeur
 * (une capacite Forge, attachee au joueur), et le code commun ne fait que la demander a
 * {@link PlatformHooks#presetData(Player)}. Ce qui change ici, c'est que le client peut aussi
 * <b>modifier</b> la donnee : c'est lui qui connait les touches. Les deux paquets qui le
 * permettent vivent dans le paquet voisin.
 *
 * <p>Le cablage de la capacite (enregistrement, attachement, copie a la mort, envoi a la
 * connexion) reste chez le chargeur : voir {@code PresetCapabilityEvents} cote Forge.
 */
public final class PresetTracker {

    private PresetTracker() {
    }

    /** La donnee d'un joueur, ou {@code null} s'il n'en a pas. */
    public static PresetData of(Player player) {
        return PlatformHooks.presetData(player);
    }

    /** Pousse les quatre prereglages au client, qui en a besoin pour ses touches. */
    public static void sync(ServerPlayer player) {
        PresetData data = of(player);
        if (data == null) return;
        Network.sendToPlayer(player, new SyncPresetPacket(data));
    }

    /** Change la categorie et efface les prereglages, comme dans l'original. */
    public static void clearOnCategoryChange(Player player) {
        PresetData data = of(player);
        if (data == null) return;
        data.clearAll();
        // Une barre vide ne porte plus rien : ce qui courait s'eteint, comme dans l'original, dont
        // le gestionnaire de contextes disposait tout sur un changement de categorie.
        if (player instanceof ServerPlayer server) {
            PlatformHooks.endHoldsOutsidePreset(server);
            sync(server);
        }
    }
}
