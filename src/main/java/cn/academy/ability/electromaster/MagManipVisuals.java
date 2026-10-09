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
 * <li>la <b>vitesse</b> qui l'y amene : le deplacement du point suivi tel quel, plus l'ecart
 *     restant referme a {@code CARRY_PULL} de la distance, plafonne — donc le bloc ne prend
 *     jamais de retard, et il vole vers son point au lieu de s'y teleporter ;</li>
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

    /**
     * La part de l'ecart refermee par tick : 0,8. L'ecart est donc divise par cinq a chaque tick.
     *
     * <p>Elle ne s'applique QU'A L'ECART RESTANT — un point qui vient de sauter, une chute, une
     * teleportation. Un point qui avance, lui, est suivi tel quel, sans aucun retard : c'est la
     * reponse au dernier retour du joueur, « quand on bouge vite, l'animation en elle meme n'est
     * pas ralentie mais on a vraiment l'impression de voir le bloc se teleporter ».
     *
     * <p>Une vitesse proportionnelle a la distance, seule, ne peut pas suivre : a la vitesse
     * {@code v} il reste toujours a {@code v / CARRY_PULL} blocs DERRIERE le point (quatre blocs
     * et demi a la vitesse des ailes de tempete, donc derriere le joueur), et ce retard se
     * referme d'un coup des qu'il s'arrete — ce qui se lit comme un teleport. L'original avait
     * ce defaut la aussi : sa vitesse etait {@code 0,2 par tick}, avancee deux fois (voir
     * {@link #STEPS}), et ralentie au carre de la distance sous deux blocs.
     */
    public static final double CARRY_PULL = 0.8;

    /**
     * Le plafond d'un pas, en blocs par tick : 1,2.
     *
     * <p>Il ne porte QUE sur l'ecart referme, jamais sur le suivi du point : un point qui file a
     * trente blocs par seconde est suivi entierement, et le bloc ne prend donc jamais de retard,
     * quelle que soit la vitesse du joueur. Ce plafond dit seulement qu'un ecart qui vient de
     * naitre (une teleportation, une chute) se referme en VOLANT — un bloc par tick et des
     * poussieres —, et non d'un bond.
     */
    public static final double CARRY_MAX = 1.2;

    /** La portee du lancer : cinq blocs — vingt-cinq, au carre. */
    public static final double THROW_RANGE_SQ = 25.0;

    /** La gravite du bloc, par tick : le {@code motionY -= 0,04} de l'original. */
    public static final double GRAVITY = 0.04;

    /**
     * Le nombre de fois que le bloc avance de son mouvement dans un meme tick : <b>DEUX</b>.
     *
     * <p>Ce n'est pas une fantaisie, c'est l'original, et c'est ce qui manquait le plus au port :
     * le joueur a mesure 55 blocs chez lui a 100 % d'experience, contre 29 ici.
     *
     * <p>Son bloc etait une {@code EntityAdvanced} de LambdaLib, avec un {@code Rigidbody} pour
     * suiveur de mouvement : ce suiveur faisait deja
     * {@code setPosition(posX + motionX, ...)} a chaque tick. Et le {@code onUpdate} du bloc
     * ajoutait <b>encore une fois</b> ce meme mouvement a sa position :
     * {@code posX += motionX; posY += motionY; posZ += motionZ;}. Le bloc avancait donc deux fois
     * son mouvement par tick.
     *
     * <p>La gravite, elle, n'etait posee qu'une fois par tick — dans le meme {@code onUpdate} —
     * donc le bloc tombait aussi deux fois plus vite en chemin. Tout le reste en decoule : il part
     * deux fois plus vite, vole deux fois moins longtemps, et tombe deux fois plus loin que ce que
     * disent ses seuls nombres. C'est le lancer que le joueur connait, et c'est pour cela que le
     * port lui paraissait mou.
     *
     * <p>Le port n'a qu'un seul deplacement par tick — celui du moteur, qui resout les collisions
     * — alors il l'avance de {@code STEPS} fois le mouvement, et le rayon de pose couvre la meme
     * distance : un mur d'un bloc ne peut pas etre saute.
     */
    public static final int STEPS = 2;

    /** La vitesse vraie d'un bloc lance, en blocs par tick : sa vitesse, deux fois. */
    public static double flightSpeed(double speed) {
        return speed * STEPS;
    }

    /** Et la gravite qu'il subit vraiment, en blocs par tick au carre. */
    public static double flightGravity() {
        return GRAVITY * STEPS;
    }

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

    /**
     * Les arcs vivants a la fois autour du bloc.
     *
     * <p>L'original en tenait quatre, mais chacun vivait jusqu'a trente ticks et scintillait —
     * son essaim n'en montrait donc jamais quatre d'un coup. Ici un arc ne vit que trois ticks,
     * et il est visible tout du long : le joueur a vu plus d'eclairs que dans le vrai mod. Deux
     * arcs vivants, tires au hasard, en donnent le rythme.
     */
    public static final int ARCS_ALIVE = 2;

    private MagManipVisuals() {}

    /**
     * Combien d'arcs semer ce tick pour en garder {@link #ARCS_ALIVE} vivants.
     *
     * <p>Un arc d'entourage ne vit ici que trois ticks — {@link #ARC_LIFE} —, donc deux arcs
     * vivants demandent deux semis sur trois ticks : deux fois un arc, et une fois rien. C'est
     * cet arret d'un tick, invisible parce que les arcs precedents couvrent encore, qui donne le
     * gresillement plutot qu'une pluie continue.
     *
     * <p>Et c'est un TIRAGE, pas un tour de role : le premier essai semait deux arcs, puis un,
     * puis un, et ils s'allumaient donc de concert — le joueur l'a vu, « dans le vrai mod ils
     * n'apparaissent pas toujours en meme temps ». Ici chacun arrive quand il veut.
     */
    public static int arcsToSow(net.minecraft.util.RandomSource random) {
        int base = ARCS_ALIVE / ARC_LIFE;
        int extra = ARCS_ALIVE % ARC_LIFE;
        return base + (extra > 0 && random.nextFloat() < (float) extra / ARC_LIFE ? 1 : 0);
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
     * Le pas du portage : le bloc SUIT le deplacement de son point, et referme l'ecart qui reste.
     *
     * <p>Les deux termes, et pourquoi il faut les deux :
     *
     * <ul>
     * <li>le <b>deplacement du point</b> est suivi tel quel — aucun retard, quelle que soit la
     *     vitesse du joueur. C'est lui qui manquait : une vitesse proportionnelle a la distance
     *     laisse le bloc a {@code v / CARRY_PULL} blocs derriere, et le rattrapage se lit comme
     *     un teleport quand le joueur s'arrete ;</li>
     * <li>l'<b>ecart qui reste APRES ce suivi</b> (le ``residu``, c'est-a-dire ce que le bloc a
     *     encore a rattraper une fois qu'il est venu la ou le point etait) est referme a
     *     {@code CARRY_PULL} de la distance, plafonne a {@code CARRY_MAX} — donc sans jamais
     *     depasser le point, et sans jamais sauter. Un bloc deja sur son point qui suit un point
     *     qui avance n'a AUCUN residu : il ne corrige rien du tout.</li>
     * </ul>
     *
     * <p>Le bloc ne bouge donc que si son point bouge : celui qui ne bouge pas tient son bloc
     * immobile, exactement, au lieu de le faire vibrer autour.
     *
     * @param position       ou le bloc est
     * @param target         ou son point est maintenant
     * @param previousTarget ou son point etait au tick precedent (le meme, au premier)
     */
    public static Vec3 carryStep(Vec3 position, Vec3 target, Vec3 previousTarget) {
        Vec3 follow = target.subtract(previousTarget);
        Vec3 residual = target.subtract(position).subtract(follow);
        double distance = residual.length();
        if (distance < 1.0E-6) return follow;
        double speed = Math.min(CARRY_MAX, CARRY_PULL * distance);
        return follow.add(residual.scale(speed / distance));
    }

    /**
     * La vitesse du lancer : le bloc part vers ce que le regard touche, a la vitesse de
     * l'experience.
     *
     * <p>L'original prenait {@code normalize(lookPoint - entity) * speed} : un bloc qui part
     * droit vers le point vise, d'autant plus vite qu'on sait faire — de 0,5 a 1 bloc par
     * tick, et sa gravite le fait ensuite plonger.
     *
     * <p>ET SON BLOC AVANCAIT DEUX FOIS CE MOUVEMENT DANS LE MEME TICK : la vitesse qui part
     * d'ici vaut donc le double — voir {@link #STEPS}, qui explique d'ou cela vient et pourquoi
     * c'est ce que le joueur connait. C'est pour cela que le port lance deux fois moins loin que
     * le vrai mod si on ne le fait pas.
     */
    public static Vec3 throwVelocity(Vec3 position, Vec3 lookPoint, double speed) {
        Vec3 delta = lookPoint.subtract(position);
        if (delta.lengthSqr() < 1.0E-6) return Vec3.ZERO;
        return delta.normalize().scale(flightSpeed(speed));
    }

    /**
     * La rotation d'un bloc attrape, en degres, a un age donne.
     *
     * <p>L'original tirait ses deux vitesses au hasard : entre 1 et 3 degres par tick pour le
     * lacet, autant pour le tangage. Le port les deduit de l'identifiant, ce qui donne la meme
     * variete sans le hasard — et un lacet et un tangage qui ne tombent jamais ensemble, sans quoi
     * le bloc tournerait autour d'un axe fixe. D'ou le quart de degre du tangage : entier d'un
     * cote, en quarts de l'autre, ils ne peuvent pas se confondre.
     *
     * <p>L'age est celui du RENDU, et c'est un {@code double} : {@code tickCount - 1} plus le temps
     * partiel de l'image. L'original interpolait exactement comme cela
     * ({@code lerpf(e.lastYaw, e.yaw, pt)}), et sans cette avance le bloc tourne par saccades de
     * tick — vingt images par seconde au lieu de la soixantaine de l'ecran.
     */
    public static double spinYaw(double age, int entityId) {
        return (Math.floorMod(entityId, 3) + 1) * age;
    }

    public static double spinPitch(double age, int entityId) {
        return (Math.floorMod(entityId / 3, 3) + 1) * age * 0.75;
    }
}
