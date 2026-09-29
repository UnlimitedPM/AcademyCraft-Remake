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
 * qui se voit. {@code HAND_DOWN = 0.87} pose le depart a 0,75 bloc des pieds — c'est-a-dire
 * sur la main elle-meme, telle qu'on la voit pendre en vue de troisieme personne, devant le
 * joueur a l'ecran. Les deux reglages precedents se sont trompes : 0,22 mettait l'eclair a
 * l'epaule (1,40 bloc) et 0,55 encore au-dessus de la main (1,07 bloc, soit la poitrine).
 *
 * <p><b>Les deux decalages horizontaux se calculent sur la direction horizontale du
 * regard</b>, jamais sur le regard entier. Un bras ne monte pas quand on leve les yeux au
 * ciel : avec un avant pris sur le regard complet, l'origine suivait le tangage et grimpait
 * jusqu'au cou des qu'on visait vers le haut — mesure en jeu, 1,24 bloc au lieu de 0,75.
 * Le cote droit se trouve par {@code avant x vertical}, qui donne bien la droite du joueur,
 * verifie par un test : une erreur de signe poserait l'eclair dans la mauvaise main sans
 * rien casser.
 */
public final class ArcOrigins {

    /** Devant les yeux : la main se tient en avant du visage. */
    public static final double HAND_FORWARD = 0.45;

    /** Du cote du bras, en blocs : les 0,3125 bloc du modele. */
    public static final double HAND_RIGHT = 0.31;

    /** Et plus bas que les yeux : 1,62 d'yeux moins 0,75 de main qui pend. */
    public static final double HAND_DOWN = 0.87;

    /** La hauteur des yeux dans le modele du joueur : {@code HAND_DOWN} se compte depuis la. */
    public static final double EYE_HEIGHT = 1.62;

    private ArcOrigins() {}

    /** Le point de depart d'un eclair : la main, vue depuis les yeux et la direction du regard. */
    public static Vec3 hand(Vec3 eye, Vec3 look) {
        // Tout le bras se lit a l'horizontale : c'est la direction du corps, pas celle du
        // regard. Un regard vertical ne dit plus de quel cote le joueur est tourne ; la
        // main reste alors le long du corps, ce qui vaut mieux qu'une direction inventee.
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1e-6) {
            return eye.add(-HAND_RIGHT, -HAND_DOWN, 0);
        }

        Vec3 forward = flat.normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();

        return eye
                .add(forward.scale(HAND_FORWARD))
                .add(right.scale(HAND_RIGHT))
                .add(0, -HAND_DOWN, 0);
    }
}
