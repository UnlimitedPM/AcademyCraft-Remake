package cn.academy.ability.vecmanip;

import net.minecraft.world.phys.Vec3;

/**
 * La poussee de vecmanip : ce qui arrive a ce qu'une onde trouve sur son chemin.
 *
 * <p>Toutes les competences de la categorie projettent de la meme facon — une direction
 * prise entre les deux tetes, un <b>soulevement</b> ajoute a la verticale, et une force.
 * Deux nombres les separent : le choc dirige souleve de 0,6 et pousse de 0,7, l'onde de
 * choc dirigee souleve de 0,4 et pousse de 1,2. La formule, elle, est la meme.
 *
 * <p>Elle est ecrite ici, pure et hors de toute competence, pour deux raisons : elle est
 * <b>verifiable</b> (aucun type de monde, donc un test unitaire peut la lire) et elle
 * porte la <b>coquille de l'original</b> que le port a corrigee une fois pour toutes — voir
 * {@link #push}.
 */
public final class VecmanipPush {

    private VecmanipPush() {}

    /**
     * La poussee, a partir des deux points de visee.
     *
     * <p>L'original prenait la direction qui va de la tete de la cible a celle du joueur,
     * lui retranchait le soulevement en vertical, renormait, puis poussait a l'oppose. Cela
     * revient exactement a prendre la direction directe — du joueur vers la cible — et a y
     * <b>ajouter</b> le soulevement vers le haut. C'est ce qui fait qu'une cible droit
     * devant part en arriere <b>et</b> en l'air, au lieu de rester clouee au sol.
     *
     * <h2>La coquille corrigee</h2>
     *
     * <p>Le choc dirige de l'original ecrivait {@code motionZ = delta.y * -0.7f} : l'axe Z
     * recevait la composante <b>verticale</b> au lieu de l'horizontale, si bien qu'une cible
     * droit devant ne reculait pas du tout — elle montait seulement. Le port suit
     * l'intention : chaque axe avec sa composante.
     *
     * @param playerEye l'oeil de celui qui pousse
     * @param targetEye l'oeil de celui qui part
     * @param lift      le soulevement ajoute a la verticale (0,6 ; 0,4)
     * @param force     la longueur de la poussee (0,7 ; 1,2)
     */
    public static Vec3 push(Vec3 playerEye, Vec3 targetEye, double lift, double force) {
        Vec3 away = targetEye.subtract(playerEye);
        if (away.lengthSqr() == 0) return Vec3.ZERO;
        Vec3 lifted = away.normalize().add(0, lift, 0);
        return lifted.normalize().scale(force);
    }

    /**
     * La petite bousculade que les deux ondes ajoutent a leur poussee.
     *
     * <p>L'original prenait la direction du joueur vers la cible, en trois coordonnees
     * (donc un peu vers le haut si la cible est plus haut), et la mettait a 0,24. Elle ne
     * depend d'aucune experience : c'est elle qui fait bouger ce qui ne se blesse pas — un
     * objet au sol, par exemple, que l'onde de choc dirigee emporte comme le reste.
     */
    public static Vec3 shove(Vec3 playerPos, Vec3 targetPos) {
        Vec3 away = targetPos.subtract(playerPos);
        return away.lengthSqr() == 0 ? Vec3.ZERO : away.normalize().scale(SHOVE);
    }

    /** La bousculade des deux ondes : {@code 0.24} chez l'original, sur les deux. */
    public static final double SHOVE = 0.24;

    /** Et le dixieme de bloc qui decolle la cible du sol avant la poussee. */
    public static final double LIFT_OFF = 0.1;
}
