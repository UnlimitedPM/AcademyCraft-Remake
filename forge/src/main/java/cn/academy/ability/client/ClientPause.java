package cn.academy.ability.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * La pause du jeu, telle que les crochets clients doivent la lire.
 *
 * <p>Echap n'arrete que le <b>serveur</b>, en solo : Forge tire {@code ClientTickEvent} avant
 * meme de regarder la pause, et le client continuait donc de piloter ses effets. Trois degats
 * ont ete payes la meme semaine, tous les trois invisibles dans les portes de test (aucune ne
 * charge une classe cliente) :
 *
 * <ul>
 * <li>les <b>ailes de tempete</b> empilaient leur flottement — 0,078 par tick — jusqu'a faire
 *     decoller le joueur au retour : « je m'envole comme une fusee » ;</li>
 * <li>les <b>eclairs</b> continuaient de scintiller derriere le menu, et l'onde du renfort de
 *     s'egrener ;</li>
 * <li>et la <b>manipulation magnetique cumulait</b> ses arcs : ils meurent quand le temps du
 *     MONDE les depasse, et ce temps ne s'ecoule plus pendant une pause — alors que le semis,
 *     lui, repartait a chaque tick. Meme cause pour tout ce qui est seme au tick et ramasse au
 *     temps du monde.</li>
 * </ul>
 *
 * <p>REGLE : <b>tout crochet client qui pilote quelque chose</b> — un deplacement, une
 * animation, un semis, un balayage — commence par {@code if (ClientPause.frozen()) return;}.
 * Ce qui se contente de <b>dessiner</b> n'a rien a demander : les rendus lisent l'etat, donc ils
 * suivent ce que le tick a fige. C'est ainsi que le monde se fige lui-meme : le jeu saute
 * {@code level.tickEntities}, {@code level.tick} et le moteur de particules.
 *
 * <p>En MULTIJOUEUR la pause ne vaut rien ({@code Minecraft.isPaused} demande un serveur local
 * non publie), et c'est voulu : la-bas le monde continue de tourner, donc les effets doivent
 * suivre.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientPause {

    private ClientPause() {}

    /** Vrai quand le jeu est en pause, et que rien de ce qui pilote ne doit avancer. */
    public static boolean frozen() {
        return Minecraft.getInstance().isPaused();
    }
}
