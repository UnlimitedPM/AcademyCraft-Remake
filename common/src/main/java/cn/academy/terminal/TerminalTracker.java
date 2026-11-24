package cn.academy.terminal;

import cn.academy.platform.PlatformHooks;
import cn.academy.platform.network.Network;
import cn.academy.terminal.network.SyncTerminalDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * La donnee du terminal d'un joueur : lecture et envoi au client.
 *
 * <p>Meme forme que {@code PresetTracker} : la donnee vit chez le chargeur (une capacite
 * attachee au joueur), le code commun la demande a {@link PlatformHooks#terminalData(Player)},
 * et l'envoi part par le transport neutre. Le cablage de la capacite reste chez le chargeur.
 *
 * <p>Un envoi explicite apres chaque changement, et pas seulement a la connexion : un objet qui
 * installe une application doit se voir tout de suite, sans attendre le prochain rafraichissement.
 */
public final class TerminalTracker {

    private TerminalTracker() {
    }

    /** Les applications installees du joueur, ou {@code null}. */
    public static TerminalData of(Player player) {
        return PlatformHooks.terminalData(player);
    }

    /** Envoie l'etat courant au joueur. A appeler apres toute installation. */
    public static void sync(ServerPlayer player) {
        sync(player, false);
    }

    /**
     * Envoie l'etat courant, en disant si le terminal vient d'etre installe.
     *
     * <p>Le drapeau n'est pas une commodite : c'est ce qui declenche l'animation d'installation
     * cote client, exactement comme le message {@code install} que l'original envoyait a son
     * propre client depuis l'objet.
     */
    public static void sync(ServerPlayer player, boolean installedNow) {
        TerminalData data = of(player);
        if (data == null) return;
        Network.sendToPlayer(player, new SyncTerminalDataPacket(data, installedNow));
    }
}
