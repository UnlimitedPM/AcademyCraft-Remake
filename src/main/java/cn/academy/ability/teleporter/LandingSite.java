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

    /** La hauteur du saut d'arrivee, quand la place le permet : un bloc plein. */
    public static final double LIFT = 1.0;

    /** Les blocs libres qu'il faut pour s'autoriser ce saut (le saut + la tete). */
    public static final int LIFT_CLEARANCE = 3;

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

    /**
     * Le meme point, mais <b>aux pieds</b> : celui ou l'on se tient.
     *
     * <p>{@link #onBlockFace} situe la <b>tete</b> — ses hauteurs sont celles d'un joueur debout,
     * un bloc et huit dixiemes au-dessus d'un plancher — parce que c'est ainsi que l'original les
     * lisait. Mais ce point sert a deux choses : poser le joueur, et poser la marque qui montre ou
     * il arrivera. La marque, elle, marque un <b>endroit ou l'on se tient</b>, donc ses pieds :
     * sans ce retrait d'une hauteur d'yeux, le fantome se tenait un bloc et demi au-dessus du bloc
     * vise — en l'air, plus loin que lui — et le joueur tombait de cette hauteur en arrivant.
     */
    public static Vec3 onFeet(Direction face, Vec3 point, BlockPos pos,
                              Predicate<BlockPos> occupied, double eyeHeight) {
        return onBlockFace(face, point, pos, occupied).subtract(0, eyeHeight, 0);
    }

    /**
     * Le meme point, mais dont la hauteur des faces verticales se lit sur le <b>point
     * d'impact</b> et non sur le bloc touche.
     *
     * <p>C'est le scintillement, et c'est la fin de son {@code getDest} d'origine :
     * {@code y = mop.hitVec.y + 1.7}. La teleportation au marqueur, elle, lit
     * {@code mop.getBlockPos.getY + 1.7} — deux conventions differentes pour deux competences
     * differentes, et les confondre met le joueur <b>dans le sol</b> : viser le cote du bloc sur
     * lequel on se tient donnait un point un bloc sous la surface, ou le fantome disparaissait
     * (il est teste en profondeur) et le saut arrivait sous terre. C'est ce que le joueur a vu le
     * 22/10 : « parfois le phantome ne s'affiche pas, et si je me teleporte, je me teleporte dans
     * le sol ».
     */
    public static Vec3 onImpact(Direction face, Vec3 point, Predicate<BlockPos> occupied) {
        double x = point.x;
        double y = point.y;
        double z = point.z;

        switch (face) {
            case DOWN -> y -= UNDER_SLAB;
            case UP -> y += ON_TOP;
            case NORTH -> {
                z -= IN_FRONT;
                y += AT_HEAD;
            }
            case SOUTH -> {
                z += IN_FRONT;
                y += AT_HEAD;
            }
            case WEST -> {
                x -= IN_FRONT;
                y += AT_HEAD;
            }
            case EAST -> {
                x += IN_FRONT;
                y += AT_HEAD;
            }
            default -> {
            }
        }

        if (face.getAxis().isHorizontal() && occupied.test(BlockPos.containing(x, y + 1.0, z))) {
            y -= DUCK_UNDER;
        }

        return new Vec3(x, y, z);
    }

    /** Le meme point d'impact, mais aux pieds : voir {@link #onFeet}. */
    public static Vec3 onImpactFeet(Direction face, Vec3 point, Predicate<BlockPos> occupied,
                                    double eyeHeight) {
        return onImpact(face, point, occupied).subtract(0, eyeHeight, 0);
    }

    /**
     * Le saut d'un bloc, quand trois blocs sont libres a l'arrivee.
     *
     * <p>Demande du joueur : le scintillement et la teleportation au marqueur arrivent un bloc
     * au-dessus de la surface, mais seulement s'il reste la place pour la tete. Le predicat est
     * consulte sur la colonne du point, a partir de son bloc : si l'un des trois est occupe, aucun
     * saut — mieux vaut arriver a la surface que la tete dans un plafond.
     */
    public static Vec3 lift(Vec3 feet, Predicate<BlockPos> blocked) {
        BlockPos base = BlockPos.containing(feet);
        for (int i = 0; i < LIFT_CLEARANCE; i++) {
            if (blocked.test(base.above(i))) return feet;
        }
        return feet.add(0, LIFT, 0);
    }
}
