package cn.academy.fabric;

import cn.academy.ability.AbilityTicker;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Le cote serveur de l'aptitude, cote Fabric.
 *
 * <p>La logique est commune — voir {@link AbilityTicker} — ; ce fichier ne fait que lui donner les
 * evenements de Fabric, qui ne sont pas ceux de Forge :
 *
 * <ul>
 *   <li>le <b>tick</b> : Fabric n'a pas d'evenement « tick du joueur », donc on passe par le tick
 *       du serveur et on parcourt ses joueurs ;</li>
 *   <li>la <b>mort</b> : {@code AFTER_DEATH} ;</li>
 *   <li>les <b>degats</b> : {@code ALLOW_DAMAGE}, qui dit seulement « oui ou non » — Fabric ne
 *       laisse pas reecrire le montant. Les refus passent donc tous (le bouclier qui absorbe, la
 *       regle PvP), mais la multiplication des marques de radiation et les veilles qui
 *       <i>reduisent</i> les degats ne s'appliquent pas encore ici.</li>
 * </ul>
 *
 * <p>Ce qui manque, faute d'evenement equivalent : le reveil dans un lit, la protection de chute et
 * le tick des vivants (les marques de radiation). Voir FABRIC_PORT.md.
 */
public final class FabricAbilityEvents {

    private FabricAbilityEvents() {
    }

    public static void install() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                AbilityTicker.tick(player);
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                AbilityTicker.onDeath(player);
            }
        });

        // Le premier crochet des degats : il decide si le coup a lieu du tout.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !refuses(entity, source, amount));
    }

    /** Le coup est-il refuse en entier ? */
    private static boolean refuses(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof Player player)) return false;
        if (AbilityTicker.refusesSkillOnPlayer(player, source)) return true;
        return AbilityTicker.refusesAttack(player, source, amount);
    }
}
