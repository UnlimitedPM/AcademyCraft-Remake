package cn.academy.misc.media;

import cn.academy.misc.media.network.SyncMediaPacket;
import cn.academy.platform.PlatformHooks;
import cn.academy.platform.network.Network;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Les morceaux d'un joueur : lecture et envoi au client.
 *
 * <p>Meme patron que les autres donnees de joueur du port : la donnee vit chez le chargeur (une
 * capacite attachee au joueur), et le code commun la demande a
 * {@link PlatformHooks#mediaData(Player)}. Le client en a besoin pour afficher ce qu'il possede, et
 * c'est le serveur seul qui decide de l'ajouter.
 */
public final class MediaTracker {

    private MediaTracker() {
    }

    /** La donnee d'un joueur, ou {@code null} s'il n'en a pas. */
    @Nullable
    public static MediaAcquireData of(Player player) {
        return PlatformHooks.mediaData(player);
    }

    /** Pousse la liste au client, qui affiche ce que le joueur possede. */
    public static void sync(ServerPlayer player) {
        MediaAcquireData data = of(player);
        if (data == null) return;
        Network.sendToPlayer(player, new SyncMediaPacket(data));
    }
}
