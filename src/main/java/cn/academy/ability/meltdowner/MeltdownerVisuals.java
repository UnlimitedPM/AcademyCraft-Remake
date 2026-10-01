package cn.academy.ability.meltdowner;

import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Le faisceau du meltdowner et l'essaim de sa charge : des nombres, et rien d'autre.
 *
 * <p>L'original tenait les deux bouts de son faisceau dans son entite ({@code EntityMDRay}) et son
 * essaim dans le tick client de son contexte. Les voici sortis, pour qu'ils se relisent en test :
 * ni Minecraft ni horloge ici, seulement des points et des tirages.
 *
 * <h2>Le faisceau penche, et c'est l'original</h2>
 *
 * <p>Il part des <b>yeux</b> — {@code spawner.getPositionEyes(1F).add(spawner.getLookVec())}, donc
 * a hauteur d'yeux et un bloc devant — et il finit <b>aux pieds</b>, trente blocs plus loin :
 * {@code spawner.getPositionVector().add(spawner.getLookVec() * length)}. Les deux points ne
 * prennent donc pas la meme origine, et un tir horizontal descend de 1,62 bloc sur sa longueur —
 * un peu plus de trois degres. C'est repris tel quel, comme le reste : c'est ce que le joueur a
 * vu pendant cinq ans, et le redresser serait une retouche, pas un portage.
 *
 * <p>La ou le faisceau est plus court qu'il ne frappe : il s'arrete a trente blocs, alors que ses
 * degats portent a cinquante. L'original aussi.
 *
 * <h2>L'essaim</h2>
 *
 * <p>Pendant la charge, des grains de plasma tournent autour du joueur et montent lentement. Le
 * tirage de l'original, transcrit tel quel : un grain par tour de boucle, la boucle allant de deux
 * ou trois jusqu'a <b>zero compris</b>. Or son {@code rangei(2, 3)} <b>exclut sa borne haute</b> —
 * {@code from + nextInt(to - from)}, comme l'ecrit LambdaLib2, donc {@code nextInt(1)} — et rendait
 * donc toujours deux : la boucle tournait trois fois, invariablement. Ce n'est pas une lecture
 * approximative : le meme {@code rangei} borne les traits de la salve, ou le port l'a deja fige.
 * Voir {@link #swarmCount}.
 *
 * <p>Ou ils se posent depend de qui regarde, et c'est encore l'original : celui qui charge les voit
 * <b>a ses pieds</b>, les autres les voient <b>a hauteur d'yeux</b> — de quoi lui epargner un
 * nuage devant la figure, et de quoi montrer aux voisins que quelque chose se prepare.
 */
public final class MeltdownerVisuals {

    /** La longueur du faisceau, en blocs : le {@code length} de l'original, trente. */
    public static final double BEAM_LENGTH = 30.0;

    /**
     * Le dezoom de la charge, en degres de champ de vision a son maximum.
     *
     * <p>L'original ne le donnait pas a son meltdowner : il ralentissait la marche de celui qui
     * charge, et le champ de vision se tirait tout seul avec elle. Le port ne touche pas au
     * deplacement — c'est deja comme cela qu'il a porte le dezoom de l'orage — donc la vue
     * s'elargit directement.
     *
     * <p>La valeur est la moitie de celle de l'orage, a la demande du joueur : vingt degres, la
     * ou l'orage en prend quarante.
     */
    public static final float FOV_DEGREES = 20f;

    // --- L'ESSAIM DE LA CHARGE ---

    /** Les bornes du tirage, avant le « zero compris » de l'original. */
    public static final int SWARM_BASE_MIN = 2;
    public static final int SWARM_BASE_MAX = 3;

    /** Le rayon du cercle ou tournent les grains : de 0,7 a 1 bloc autour du joueur. */
    public static final double SWARM_RADIUS_MIN = 0.7;
    public static final double SWARM_RADIUS_MAX = 1.0;

    /** Et leur hauteur : de 1,2 bloc sous le repere jusqu'a son niveau. */
    public static final double SWARM_HEIGHT_MIN = -1.2;
    public static final double SWARM_HEIGHT_MAX = 0.0;

    /** La hauteur d'yeux, ajoutee pour les autres joueurs : le {@code 1.6} de l'original. */
    public static final double SWARM_EYE_HEIGHT = 1.6;

    /** Le grain part de biais : trois centimetres par seconde au plus, dans les deux sens. */
    public static final double SWARM_SPEED = 0.03;

    /** Et il monte toujours un peu : de 1 a 5 centimetres par seconde. */
    public static final double SWARM_RISE_MIN = 0.01;
    public static final double SWARM_RISE_MAX = 0.05;

    private MeltdownerVisuals() {}

    /**
     * Le depart du faisceau : les yeux, un bloc devant.
     *
     * <p>L'unite du regard de l'original n'est pas une distance choisie : {@code getLookVec()}
     * rend un vecteur <b>unitaire</b>, donc c'est bien ce bloc-la qui est avance.
     */
    public static Vec3 beamFrom(Vec3 eye, Vec3 look) {
        return eye.add(look);
    }

    /** Et sa pointe : les <b>pieds</b>, la longueur plus loin. */
    public static Vec3 beamTo(Vec3 feet, Vec3 look, double length) {
        return feet.add(look.scale(length));
    }

    /**
     * Combien de grains l'essaim seme par tick : <b>trois</b>, toujours.
     *
     * <p>Le tirage de l'original etait {@code rangei(2, 3)} — qui rend deux, sa borne haute etant
     * exclue — puis une boucle {@code to 0} qui descendait jusqu'a zero <b>compris</b>, soit un
     * grain de plus : trois tours, sans hasard. Le port ecrit les deux pour que le compte se lise,
     * mais il n'y a rien a tirer ici : semer deux grains la ou l'original en semait trois se
     * verrait, l'essaim serait d'un tiers plus clair.
     */
    public static int swarmCount(Random random) {
        return SWARM_BASE_MIN + random.nextInt(SWARM_BASE_MAX - SWARM_BASE_MIN) + 1;
    }

    /** L'ecart d'un grain au joueur : sur son cercle, et sous sa hauteur. */
    public static Vec3 swarmOffset(double radius, double angle, double height) {
        return new Vec3(radius * Math.sin(angle), height, radius * Math.cos(angle));
    }

    /** Son rayon : de 0,7 a 1 bloc. */
    public static double swarmRadius(Random random) {
        return SWARM_RADIUS_MIN + random.nextDouble() * (SWARM_RADIUS_MAX - SWARM_RADIUS_MIN);
    }

    /** Son angle sur le cercle, en radians : un tour complet. */
    public static double swarmAngle(Random random) {
        return random.nextDouble() * Math.PI * 2;
    }

    /** Et sa hauteur, sous le repere : de -1,2 a 0. */
    public static double swarmHeight(Random random) {
        return SWARM_HEIGHT_MIN + random.nextDouble() * (SWARM_HEIGHT_MAX - SWARM_HEIGHT_MIN);
    }

    /** Sa vitesse : de biais au hasard, et toujours un peu vers le haut. */
    public static Vec3 swarmVelocity(Random random) {
        return new Vec3(spread(random),
                SWARM_RISE_MIN + random.nextDouble() * (SWARM_RISE_MAX - SWARM_RISE_MIN),
                spread(random));
    }

    private static double spread(Random random) {
        return (random.nextDouble() * 2 - 1) * SWARM_SPEED;
    }
}
