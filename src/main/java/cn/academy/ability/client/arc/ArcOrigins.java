package cn.academy.ability.client.arc;

import net.minecraft.world.phys.Vec3;

/**
 * Ou part un eclair.
 *
 * <p><b>Attention, ce n'est pas ce que faisait l'original.</b> Il posait son arc aux YEUX du
 * joueur ({@code setPosition(player.posX, player.posY + eyeHeight, player.posZ)}) et le
 * long de son regard : l'eclair partait donc de la camera, c'est-a-dire du milieu de
 * l'ecran, sans origine visible. C'est ce que le joueur a vu et n'a pas aime.
 *
 * <p>Ici l'arc part de la MAIN, c'est-a-dire decale du cote du bras qui tient la baguette,
 * un peu en avant et un peu plus bas que les yeux. L'arc va ensuite de la main jusqu'au
 * point vise, donc le trait est legerement en diagonale — ce que le joueur decrit, et ce
 * qu'il demande.
 *
 * <p>Le decalage se calcule sur la direction <b>horizontale</b> du regard, et non sur le
 * regard entier : un bras ne se deplace pas quand on leve les yeux au ciel, et un cote
 * calcule sur un regard vertical serait indetermine. Le cote droit se trouve par
 * {@code regard x vertical}, qui donne bien la droite du joueur — verifie par un test, parce
 * qu'une erreur de signe poserait l'eclair dans la mauvaise main sans rien casser.
 */
public final class ArcOrigins {

    /** Devant les yeux : la main est un peu en avant du visage. */
    public static final double HAND_FORWARD = 0.35;

    /** Du cote du bras qui tient l'objet, en blocs. */
    public static final double HAND_RIGHT = 0.30;

    /** Et un peu plus bas que les yeux. */
    public static final double HAND_DOWN = 0.22;

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
