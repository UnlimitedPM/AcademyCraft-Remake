package cn.academy.ability.client;

import net.minecraft.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * L'horloge des effets du client : le temps reel, <b>moins les pauses</b>.
 *
 * <h2>Pourquoi elle existe</h2>
 *
 * <p>Les rayons du plasma et leurs etincelles ne vivent pas au tick mais <b>a la milliseconde</b>,
 * comme chez l'original dont les courbes etaient ecrites en millisecondes : ils poussent pendant les
 * deux premiers dixiemes de seconde, s'effilent sur les derniers, et leur lueur tremble a chaque
 * image. Leur duree se lisait donc avec {@code Util.getMillis()}, une horloge qui <b>ne connait pas
 * la pause</b>.
 *
 * <p>Et la pause du solo n'arrete que le <b>serveur</b> : l'ecran continue de se redessiner derriere
 * le menu, donc chaque image rejouait la courbe d'un cran de plus — le joueur voyait son railgun
 * continuer de s'animer tout seul, « quand je mets mon jeu en pause, le railgun continue quand meme
 * son animation au lieu de se stopper ». Meme cause, et meme remede, que les trois degats racontes
 * dans {@link ClientPause} : la garde {@code if (ClientPause.frozen()) return;} ne suffisait pas ici,
 * parce que ce n'est pas un crochet qui pilotait l'animation — c'est le <b>rendu</b> qui la rejouait
 * en lisant une horloge qui courait toujours.
 *
 * <h2>Comment elle s'arrete</h2>
 *
 * <p>Elle retranche au temps reel la duree <b>cumulee</b> des pauses. L'instant qu'elle rend est donc
 * continu : il ne recule jamais, ne saute pas au retour du menu, et vaut exactement ce qu'il valait a
 * l'instant ou la pause s'est ouverte. Une bombe posee avant la pause explose donc exactement quand
 * elle devait exploser, une fois la pause finie.
 *
 * <p>La pause est lue <b>a l'appel</b> plutot que comptee image par image : rien n'oblige l'appelant a
 * venir pendant la pause, et une pause dont aucune image ne se serait servee serait tout de meme
 * defalquee — c'est le premier appel d'apres qui la solde.
 *
 * <p>En <b>multijoueur</b>, {@code Minecraft.isPaused} est faux ({@code ClientPause} le rappelle) :
 * l'horloge y est donc simplement l'horloge du temps reel, et rien ne se fige — c'est voulu, le monde
 * continue de tourner la-bas.
 *
 * <p>Et ce n'est <b>pas</b> une horloge de jeu : elle ne remplace ni le temps du monde
 * ({@code level.getGameTime()}, qui porte la vie des eclairs) ni le tick du client. Les effets qui
 * vieillissent au tick continuent de le faire — voir {@code ArcRenderer} et {@code ClientArcs}.
 */
@OnlyIn(Dist.CLIENT)
public final class EffectClock {

    /** L'instant ou la pause en cours s'est ouverte, ou zero s'il n'y en a pas. */
    private static long frozenSince;

    /** Ce que les pauses ont pris au total, en millisecondes. */
    private static long frozenTotal;

    private EffectClock() {
    }

    /**
     * L'instant des effets, en millisecondes.
     *
     * <p>Pendant une pause, il vaut ce qu'il valait a l'ouverture de cette pause : tout ce qui se
     * dessine dessus se fige, et reprend ou il en etait.
     */
    public static long now() {
        long real = Util.getMillis();
        if (ClientPause.frozen()) {
            if (frozenSince == 0) frozenSince = real;
            return frozenSince - frozenTotal;
        }
        if (frozenSince != 0) {
            // La pause vient de finir, et on ne l'avait peut-etre jamais vue passer : elle se solde
            // ici, en entier, avant de rendre l'heure.
            frozenTotal += real - frozenSince;
            frozenSince = 0;
        }
        return real - frozenTotal;
    }
}
