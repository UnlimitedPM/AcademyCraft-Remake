package cn.academy.ability.teleporter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

/**
 * Ou l'on atterrit quand on vise un bloc avec la teleportation au marqueur.
 *
 * <p>Le portage suivait l'original : la face regardee donne le point d'atterrissage, donc on se
 * pose toujours <b>devant</b> le bloc vise. Le joueur, 22/10, a demande autre chose pour deux cas
 * qui n'en font qu'un : « si je vise le dernier bloc du mur en hauteur, je sois teleporte dessus »
 * et « si je regarde un bloc au dessus de moi [...] je puisse me teleporter au dessus du bloc ».
 * Son motif est concret : monter sur l'eolienne sans se jeter en l'air et retomber dessus.
 *
 * <p>La regle est donc : <b>quand le dessus du bloc vise est degage, on s'y tient</b> — c'est
 * exactement ce que les deux cas ont en commun (le dernier bloc d'un mur, et un bloc isole
 * au-dessus de soi). Un bloc au milieu d'un mur garde son atterrissage devant la face, comme
 * avant.
 *
 * <h2>La place qu'il faut</h2>
 *
 * <p>Se tenir sur un bloc demande <b>deux</b> blocs libres : celui des pieds et celui de la tete
 * (un joueur fait 1,8 de haut). Le dessus n'est donc propose que si les deux sont libres : un
 * dernier bloc avec un seul bloc d'air au-dessus ne s'escalade pas, on atterrit devant lui.
 *
 * <p>Et le saut final — le joueur : « je crois que je suis teleporte un demi bloc au dessus de la
 * ou je vise, et moi je voudrais un bloc complet uniquement dans les situations ou il y a au moins
 * trois blocs de hauteur disponible » — n'est ajoute que si <b>trois</b> blocs sont libres a
 * l'arrivee : le bloc du saut, plus les deux de la tete. Avec deux blocs libres on reste donc a
 * hauteur de surface, avec trois ou plus on arrive un bloc au-dessus et l'on retombe.
 *
 * <p>La classe ne connait ni monde ni joueur : le predicat dit si un bloc est occupe (une forme de
 * collision non vide), et la hauteur d'yeux est passee en parametre. C'est ce qui la rend
 * verifiable sans lancer le jeu.
 */
public final class MarkTeleportLanding {

    /** La hauteur du saut ajoute quand la place le permet : un bloc plein. */
    public static final double LIFT = 1.0;

    /** Les blocs libres qu'il faut pour s'autoriser ce saut (le saut + la tete). */
    public static final int LIFT_CLEARANCE = 3;

    private MarkTeleportLanding() {
    }

    /**
     * Le point d'atterrissage, <b>aux pieds</b>.
     *
     * @param face     la face regardee
     * @param point    le point d'impact du regard
     * @param pos      le bloc touche
     * @param blocked  dit si un bloc est occupe
     * @param eyeHeight la hauteur des yeux du joueur, retiree du point de tete
     */
    public static Vec3 feet(Direction face, Vec3 point, BlockPos pos,
                            Predicate<BlockPos> blocked, double eyeHeight) {
        BlockPos head = pos.above();
        // Le dessus du bloc, quand il est degage, l'emporte : on s'y tient, au centre du bloc.
        // Le centre evite de se poser sur l'arete quand on vise un cote.
        if (!blocked.test(head) && !blocked.test(head.above())) {
            return lift(new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5), blocked);
        }
        // Sinon la face visee, avec les decalages de toutes les teleportations visees.
        return lift(LandingSite.onFeet(face, point, pos, blocked, eyeHeight), blocked);
    }

    /**
     * Le saut d'un bloc, quand trois blocs sont libres a l'arrivee.
     *
     * <p>Le predicat est consulte sur la colonne du point, a partir de son bloc : si l'un des trois
     * est occupe, aucun saut — mieux vaut arriver a la surface que la tete dans un plafond.
     */
    public static Vec3 lift(Vec3 feet, Predicate<BlockPos> blocked) {
        BlockPos base = BlockPos.containing(feet);
        for (int i = 0; i < LIFT_CLEARANCE; i++) {
            if (blocked.test(base.above(i))) return feet;
        }
        return feet.add(0, LIFT, 0);
    }
}
