package cn.academy.ability.electromaster;

import net.minecraft.world.phys.Vec3;

/**
 * Ce que fait un bloc attrape : ou il se tient, comment il y va, et comment il tourne.
 *
 * <p>Trois calculs, tous les trois repris de l'original, et tous les trois sans type de monde
 * pour qu'on puisse les relire :
 *
 * <ul>
 * <li>le <b>point de portage</b> — deux blocs devant les yeux, un dixieme sous la tete ;</li>
 * <li>la <b>vitesse</b> qui l'y amene : 0,2 par tick, ralentie a moins de deux blocs pour ne
 *     pas osciller autour du point, ce que l'original faisait avec un {@code dist < 4} sur une
 *     distance <b>au carre</b> ;</li>
 * <li>et sa <b>rotation</b> : l'original tirait deux vitesses au hasard entre 1 et 3 degres
 *     par tick, une pour le lacet et une pour le tangage.</li>
 * </ul>
 *
 * <p>Le port garde la rotation mais pas le hasard : elle se deduit de l'identifiant de
 * l'entite, donc deux blocs attrapes ne tournent pas au meme rythme, et un test peut la lire.
 */
public final class MagManipVisuals {

    /** Le point de portage : deux blocs devant les yeux. */
    public static final double CARRY_DISTANCE = 2.0;

    /** Et un dixieme de bloc sous la tete, comme l'original. */
    public static final double CARRY_DROP = 0.1;

    /** La vitesse du portage, par tick. */
    public static final double CARRY_PULL = 0.2;

    /** Sous deux blocs — quatre, au carre — elle ralentit proportionnellement. */
    public static final double CARRY_SLOW_SQ = 4.0;

    /** La portee du lancer : cinq blocs — vingt-cinq, au carre. */
    public static final double THROW_RANGE_SQ = 25.0;

    /**
     * Le cote du cube ou gresille l'electricite, autour du bloc tenu.
     *
     * <p>Un bloc, fois le {@code sizeMultiplyer} de 1,3 de l'original : sa boite a points etait
     * faite des dimensions de ce qu'elle entourait, multipliees par ce facteur. Et centree sur le
     * bloc, donc etendue de plus ou moins 0,65 dans les trois directions.
     */
    public static final double SURROUND_CUBE = 1.3;

    /** La vie d'un arc d'entourage, en ticks — celle de {@code SurroundArcs}. */
    public static final int ARC_LIFE = 3;

    /** Les arcs vivants a la fois : les quatre de l'original, un {@code ArcType.THIN}. */
    public static final int ARCS_ALIVE = 4;

    private MagManipVisuals() {}

    /**
     * Combien d'arcs semer ce tick pour en garder {@link #ARCS_ALIVE} vivants.
     *
     * <p>L'original avait quatre arcs vivants a la fois, mais chacun vivait jusqu'a trente ticks :
     * il n'en reensemencait donc qu'un de temps en temps, au fil des morts. Un arc d'entourage ne
     * vit ici que trois ticks — {@link #ARC_LIFE} —, donc les quatre tiennent en trois ticks de
     * semis : deux, puis un, puis un. C'est ce decalage qui fait un gresillement continu plutot
     * qu'une salve qui s'allume et s'eteint trois fois par seconde.
     *
     * <p>Le compte est pose pour que la moyenne tombe sur {@link #ARCS_ALIVE} quel que soit le
     * couple de constantes : la part entiere, plus un tant que le reste n'est pas epuise.
     */
    public static int arcsToSow(long tick) {
        return ARCS_ALIVE / ARC_LIFE
                + (Math.floorMod(tick, ARC_LIFE) < ARCS_ALIVE % ARC_LIFE ? 1 : 0);
    }

    /**
     * Ou le bloc se tient : deux blocs devant les yeux, un dixieme sous la tete.
     *
     * <p>C'est l'original au mot pres, {@code entityHeadPos(player) - (0, 0.1, 0) + look * 2}.
     * La ligne suivante de son {@code updateMoveTo} calculait une direction horizontale qu'il
     * ne lisait jamais : le port ne la reprend pas.
     */
    public static Vec3 carryTarget(Vec3 eye, Vec3 look) {
        return eye.add(0, -CARRY_DROP, 0).add(look.scale(CARRY_DISTANCE));
    }

    /**
     * La vitesse qui amene le bloc au point de portage.
     *
     * <p>Portage de l'{@code ActMoveTo} de l'original : la direction du point, a 0,2 par tick,
     * et cette vitesse multipliee par {@code distSq / 4} sous quatre — donc elle s'annule
     * exactement sur le point, au lieu de le depasser d'un cote puis de l'autre.
     */
    public static Vec3 carryVelocity(Vec3 position, Vec3 target) {
        Vec3 delta = target.subtract(position);
        double distSq = delta.lengthSqr();
        if (distSq < 1.0E-6) return Vec3.ZERO;
        double scale = CARRY_PULL * (distSq < CARRY_SLOW_SQ ? distSq / CARRY_SLOW_SQ : 1.0);
        return delta.normalize().scale(scale);
    }

    /**
     * La vitesse du lancer : le bloc part vers ce que le regard touche, a la vitesse de
     * l'experience.
     *
     * <p>L'original prenait {@code normalize(lookPoint - entity) * speed} : un bloc qui part
     * droit vers le point vise, d'autant plus vite qu'on sait faire — de 0,5 a 1 bloc par
     * tick, et sa gravite le fait ensuite plonger.
     */
    public static Vec3 throwVelocity(Vec3 position, Vec3 lookPoint, double speed) {
        Vec3 delta = lookPoint.subtract(position);
        if (delta.lengthSqr() < 1.0E-6) return Vec3.ZERO;
        return delta.normalize().scale(speed);
    }

    /**
     * La rotation d'un bloc attrape, en degres, a ce tick.
     *
     * <p>L'original tirait ses deux vitesses au hasard : entre 1 et 3 degres par tick pour le
     * lacet, autant pour le tangage. Le port les deduit de l'identifiant, ce qui donne la meme
     * variete sans le hasard — et un lacet et un tangage qui ne tombent jamais ensemble, sans
     * quoi le bloc tournerait autour d'un axe fixe. D'ou le quart de degre du tangage : entier
     * d'un cote, en quarts de l'autre, ils ne peuvent pas se confondre.
     */
    public static double spinYaw(int tickCount, int entityId) {
        return (Math.floorMod(entityId, 3) + 1) * tickCount;
    }

    public static double spinPitch(int tickCount, int entityId) {
        return (Math.floorMod(entityId / 3, 3) + 1) * tickCount * 0.75;
    }
}
