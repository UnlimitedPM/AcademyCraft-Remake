package cn.academy.ability.client.arc;

import net.minecraft.world.phys.Vec3;

/**
 * Ou part un eclair.
 *
 * <p><b>Attention, ce n'est pas ce que faisait l'original.</b> Il posait son arc aux YEUX du
 * joueur ({@code setPosition(player.posX, player.posY + eyeHeight, player.posZ)}) et le
 * long de son regard : l'eclair partait donc de la camera, sans origine visible.
 *
 * <p>Ici l'arc part de la MAIN. Les trois nombres viennent de la geometrie du modele du
 * joueur, qui est fixe : le bras s'attache a 1,375 bloc au-dessus des pieds, mesure 0,75
 * bloc de long, et se tient a 0,3125 bloc du corps. Une main qui pend est donc a
 * {@code y + 0.625}, et une main tenue devant soi — la pose d'un lanceur — se tient vers
 * {@code y + 1.05}. Les yeux, eux, sont a 1,62.
 *
 * <p>La hauteur est le seul de ces trois nombres qu'on ait vraiment a choisir : c'est elle
 * qui se voit. {@code HAND_DOWN = 0.55} pose le depart un peu au-dessus de la main tendue,
 * donc nettement sous l'epaule — la ou il faut. Le premier reglage le posait a 0,22, soit
 * 1,40 bloc, c'est-a-dire a la hauteur de l'epaule : on croyait alors que l'eclair flottait
 * a cote du joueur au lieu de sortir de sa main.
 *
 * <p>Le decalage se calcule sur la direction <b>horizontale</b> du regard : un bras ne se
 * deplace pas quand on leve les yeux au ciel, et un cote calcule sur un regard vertical
 * serait indetermine. Le cote droit se trouve par {@code regard x vertical}, qui donne bien
 * la droite du joueur — verifie par un test, parce qu'une erreur de signe poserait l'eclair
 * dans la mauvaise main sans rien casser.
 */
public final class ArcOrigins {

    /** Devant les yeux : la main se tient en avant du visage. */
    public static final double HAND_FORWARD = 0.45;

    /** Du cote du bras, en blocs : les 0,3125 bloc du modele. */
    public static final double HAND_RIGHT = 0.31;

    /** Et plus bas que les yeux : 1,62 d'yeux moins 1,05 de main tendue. */
    public static final double HAND_DOWN = 0.55;

    private ArcOrigins() {}

    /** Le point de depart d'un eclair : la main, vue depuis les yeux et la direction du regard. */
    public static Vec3 hand(Vec3 eye, Vec3 look) {
        Vec3 forward = look.normalize();

        // Le cote se calcule sans la composante verticale du regard : un bras ne monte pas
        // quand on regarde le ciel.
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 right;
        if (flat.lengthSqr() < 1e-6) {
            // Regard droit au sol ou au ciel : le cote n'est plus lisible, et la droite
            // habituelle vaut mieux qu'une direction nulle.
            right = new Vec3(-1, 0, 0);
        } else {
            right = flat.cross(new Vec3(0, 1, 0)).normalize();
        }

        return eye
                .add(forward.scale(HAND_FORWARD))
                .add(right.scale(HAND_RIGHT))
                .add(0, -HAND_DOWN, 0);
    }
}
