package cn.academy.fabric;

import cn.academy.ability.develop.PortableDevData;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * L'apprentissage du developeur portable, chez Fabric.
 *
 * <p>Forge l'attache au joueur par une capacite, qui part avec la sauvegarde. Il n'y a pas
 * d'equivalent ici : la donnee vit donc dans une table du serveur, creee au premier usage et
 * rangee par identifiant de joueur.
 *
 * <p><b>Limite assumee</b> : elle ne survit pas a un arret du serveur. Ce qui est perdu, c'est un
 * apprentissage <b>en cours</b> — le meme que le rechargement fait deja echouer chez Forge, ou la
 * capacite perd l'action decidee ; les niveaux et les competences, eux, appartiennent a l'aptitude
 * du joueur et ne bougent pas.
 */
final class FabricPortableDevData {

    private static final Map<UUID, PortableDevData> DATA = new WeakHashMap<>();

    private FabricPortableDevData() {
    }

    /** La donnee du joueur, creee au premier appel et rattachee a lui. */
    static PortableDevData of(net.minecraft.world.entity.player.Player player) {
        if (player == null) return null;
        PortableDevData data = DATA.computeIfAbsent(player.getUUID(), id -> new PortableDevData());
        data.setOwner(player);
        return data;
    }

    /** Fait avancer l'apprentissage d'un tick, comme le tick de joueur de Forge. */
    static void tick(ServerPlayer player) {
        PortableDevData data = of(player);
        if (data != null) data.tick(player);
    }
}
