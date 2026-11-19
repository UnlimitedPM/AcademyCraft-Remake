package cn.academy.ability.client.tp;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * La visee du saut traversant : la distance qu'on lui donne avant de partir.
 *
 * <p>Le saut traversant est la seule teleportation du port qui se <b>regle</b>. Chez l'original, on
 * gardait la touche enfoncee et la distance se choisissait de deux facons, qui se cumulaient :
 * elle partait de sa portee maximale, la <b>molette</b> la faisait monter ou descendre d'un bloc
 * par cran, et on relachait pour partir. Le fantome suivait ce reglage, et c'est tout l'interet de
 * la marque : on voyait ou l'on ressortirait <b>de l'autre cote du mur</b> avant de s'y engager.
 *
 * <p>Le port sautait d'un coup a sa portee maximale, sans rien laisser regler. C'est ce que le
 * joueur a vu : « le premier pouvoir de teleportation me TP directement quand j'appuie, donc je
 * n'ai pas le temps de rester appuye pour voir ou le fantome me mettra ».
 *
 * <p>La distance vit chez le client — c'est lui qui a la molette — et elle voyage jusqu'au serveur
 * par {@code TeleportDistancePacket} a chaque cran : le serveur garde donc exactement la meme, et
 * c'est lui qui, au relachement, calcule la destination et fait le saut.
 */
@OnlyIn(Dist.CLIENT)
public final class TeleportAim {

    /** La distance minimale ou la molette peut descendre, comme l'original. */
    public static final double MIN_DISTANCE = 0.5;

    /** Ce qu'un cran de molette vaut, en blocs. L'original : 1. */
    public static final double NOTCH = 1.0;

    /** La distance visee, ou {@code -1} quand aucun saut traversant ne se prepare. */
    private static double distance = -1;

    private TeleportAim() {
    }

    /** Ouvre la visee : on part toujours de la portee maximale, comme l'original. */
    public static void begin(double maxDistance) {
        distance = maxDistance;
    }

    /** Ferme la visee : le saut est parti, ou la touche est relachee. */
    public static void end() {
        distance = -1;
    }

    /** Un saut traversant se prepare-t-il ? */
    public static boolean active() {
        return distance >= 0;
    }

    /** La distance visee, en blocs. */
    public static double distance() {
        return distance;
    }

    /**
     * Un cran de molette, et la distance qui en resulte.
     *
     * <p>Fonction pure, donc verifiable : un cran monte ou descend d'un bloc, et rien ne sort des
     * bornes — ni sous le minimum de l'original, ni au-dela de la portee de la competence.
     *
     * @param current la distance visee avant le cran
     * @param notches ce que la molette a tourne : 1 pour un cran vers le haut, -1 vers le bas
     * @param max     la portee maximale, celle de la competence
     */
    public static double scrolled(double current, double notches, double max) {
        double wanted = current + notches;
        return Math.min(max, Math.max(MIN_DISTANCE, wanted));
    }

    /** Fait tourner la molette et rend la nouvelle distance. */
    public static double scroll(double notches, double max) {
        distance = scrolled(distance, notches, max);
        return distance;
    }
}
