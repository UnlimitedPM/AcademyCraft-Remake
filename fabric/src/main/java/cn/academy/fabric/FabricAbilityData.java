package cn.academy.fabric;

import cn.academy.ability.AbilityData;
import cn.academy.platform.PlatformHooks;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Donnees d'aptitude cote Fabric.
 *
 * <p>Fabric n'attache pas de capacite aux joueurs : on tient donc une table en memoire, une
 * entree par joueur. Les donnees sont creees a la premiere demande, ce qui evite d'avoir a
 * suivre les connexions.
 *
 * <p><b>Limite assumee :</b> la table n'est pas encore sauvegardee. Elle survit a un changement
 * de dimension mais pas a une deconnexion. L'etape suivante est une piece jointe persistante
 * (API Data Attachment) ; en attendant, le systeme d'aptitudes n'est pas encore branche cote
 * Fabric, donc rien de joueur ne depend de cette persistance.
 */
final class FabricAbilityData implements PlatformHooks.AbilityDataAccess {

    private static final Map<UUID, AbilityData> DATA = new HashMap<>();

    static void install() {
        PlatformHooks.installAbilityData(new FabricAbilityData());
        // Sans cela, l'entree garde le joueur en vie apres son depart.
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> DATA.remove(handler.getPlayer().getUUID()));
    }

    @Override
    public AbilityData get(Player player) {
        if (player == null) return null;
        AbilityData data = DATA.get(player.getUUID());
        if (data == null) {
            data = new AbilityData();
            data.setOwner(player);
            DATA.put(player.getUUID(), data);
        }
        return data;
    }

    @Override
    public void set(Player player, AbilityData data) {
        if (player == null || data == null) return;
        AbilityData current = get(player);
        if (current != data) current.copyFrom(data);
    }
}
