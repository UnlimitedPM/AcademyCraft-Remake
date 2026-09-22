package cn.academy.ability.teleporter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

/**
 * Ou l'on atterrit sur une face de bloc, une fois le saut fini.
 *
 * <p>C'est le portage de la fin de {@code getDest} de l'original, celle que se partagent
 * toutes les teleportations visees : le point d'impact est ramene sur la <b>surface</b> de
 * la face regardee, avec le decalage qui fait qu'on se tient devant le mur plutot que
 * dedans, et une verification de la tete quand on passe par un cote.
 *
 * <p>Extrait ici parce que deux competences en dependent — la teleportation au marqueur et
 * le flashing — et qu'une divergence entre deux copies de ces decalages ne se verrait
 * qu'en jouant, sous la forme d'un joueur coince dans un mur.
 *
 * <p>La classe ne connait ni monde ni joueur : la seule chose qu'elle ne peut pas savoir,
 * c'est si la tete a la place de passer. Elle la demande, ce qui la rend verifiable en
 * test unitaire.
 */
public final class LandingSite {

    /** Face du bas : on ressort sous le bloc. */
    private static final double UNDER_SLAB = 1.0;

    /** Face du haut : on monte sur le bloc. */
    private static final double ON_TOP = 1.8;

    /** Face verticale : on se tient decale de soixante centimetres. */
    private static final double IN_FRONT = 0.6;

    /** Face verticale : on se tient a hauteur de tete du bloc vise. */
    private static final double AT_HEAD = 1.7;

    /** Face verticale encombree : on redescend d'un bloc et quart. */
    private static final double DUCK_UNDER = 1.25;

    private LandingSite() {
    }

    /**
     * Le point d'atterrissage sur la face touchee.
     *
     * @param face     la face regardee
     * @param point    le point d'impact
     * @param pos      le bloc touche, dont la hauteur sert de repere aux faces verticales
     * @param occupied dit si une tete ne passerait pas a cet endroit
     */
    public static Vec3 onBlockFace(Direction face, Vec3 point, BlockPos pos,
                                   Predicate<BlockPos> occupied) {
        double x = point.x;
        double y = point.y;
        double z = point.z;

        switch (face) {
            case DOWN -> y -= UNDER_SLAB;
            case UP -> y += ON_TOP;
            case NORTH -> {
                z -= IN_FRONT;
                y = pos.getY() + AT_HEAD;
            }
            case SOUTH -> {
                z += IN_FRONT;
                y = pos.getY() + AT_HEAD;
            }
            case WEST -> {
                x -= IN_FRONT;
                y = pos.getY() + AT_HEAD;
            }
            case EAST -> {
                x += IN_FRONT;
                y = pos.getY() + AT_HEAD;
            }
            default -> {
            }
        }

        // Les faces verticales seules peuvent avoir une tete dans le passage : l'original
        // faisait alors redescendre le point d'atterrissage d'un bloc et quart.
        if (face.getAxis().isHorizontal() && occupied.test(BlockPos.containing(x, y + 1.0, z))) {
            y -= DUCK_UNDER;
        }

        return new Vec3(x, y, z);
    }
}
