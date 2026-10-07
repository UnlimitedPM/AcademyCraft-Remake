package cn.academy.ability.electromaster;

/**
 * Le lancer de piece du railgun, portage de l'{@code EntityCoinThrowing} de l'original : le vol
 * entier, et ce qu'il faut en savoir pour decider si le tir est permis.
 *
 * <p>La piece monte, retombe, et le railgun ne part que si elle est deja <b>redescendue</b> — plus
 * de {@link #READY} de son vol. C'est ce que l'original demandait ({@code coin.getProgress() > 0.7}) :
 * le joueur lance la piece, et il a la fenetre de la retombee pour tirer a travers. Une piece trop
 * fraiche — encore dans sa montee — ne sert a rien, et une piece retombee revient simplement dans
 * l'inventaire.
 *
 * <p>DEUX CHOSES dependent du JOUEUR, et pas de la piece : l'elan avec lequel il la lance — une piece
 * jetee en plein saut monte beaucoup plus haut, parce que la vitesse verticale du saut s'ajoute a
 * celle du lancer — et l'endroit ou il a la main quand elle redescend, puisqu'elle est rattrapee plus
 * tot s'il est monte. Les deux sont des parametres des fonctions d'ici, et pas des constantes : c'est
 * ce qui les rend lisibles en JUnit, sans monde ni joueur.
 *
 * <h2>Tout se lit sur l'AGE</h2>
 *
 * <p>Le port tenait d'abord une hauteur et une vitesse dans l'entite, avancees tick par tick ; la
 * <b>copie cliente</b> d'une entite ne connait ni l'une ni l'autre — elle nait du paquet de creation
 * — donc son vol repartait de zero, et il fallait lui envoyer sa hauteur par le reseau. Le joueur l'a
 * vu tout de suite : « visuellement la piece se deplace a 20 fps au lieu des 60 ».
 *
 * <p>L'age suffit a tout dire : la vitesse du tick {@code t} est {@code INIT_VEL - GRAVITY * t}, et
 * sa hauteur est la somme des vitesses d'avant. C'est le SERVEUR qui s'en sert pour poser la piece a
 * chaque tick — voir {@code EntityCoinThrowing.tick} — et le client, lui, n'a plus rien a recalculer :
 * il interpole la position qui lui arrive.
 *
 * <p>Tout est PUR, et c'est pour cela que ca vit ici : un vol qui « ne monte pas assez » ou une
 * fenetre de tir trop courte se relisent en JUnit, sans monde ni joueur.
 */
public final class CoinToss {

    /** La vitesse verticale du lancer, sans l'elan du joueur : {@code INITVEL} de l'original. */
    public static final double INIT_VEL = 0.92;

    /** Et sa gravite, par tick. */
    public static final double GRAVITY = 0.06;

    /**
     * La vie maximale d'une piece en vol : cent vingt ticks, comme {@code MAXLIFE}.
     *
     * <p>C'est le filet de l'original, et il ne sert presque a rien : le vol dure une quarantaine de
     * ticks au plus, puisqu'il ne depend que de son age et de son elan. Il reste pour qu'une piece ne
     * puisse pas rester ouverte a jamais — c'est lui qui arrete celle que son lanceur, parti trop
     * haut et trop vite, ne rattraperait pas.
     */
    public static final int MAX_LIFE = 120;

    /**
     * La part du vol a partir de laquelle le railgun peut partir : {@code 0.7} de l'original.
     *
     * <p>Elle laisse le temps de la retombee, et rien de la montee.
     */
    public static final double READY = 0.7;

    // --- LES BORNES DU LANCER A L'ARRET ---
    //
    // Le vol se lit sur l'age, mais aussi sur la vitesse a laquelle il est parti : une piece lancee
    // pendant un saut n'a donc pas de bornes fixes, et le tir s'ouvre plus tard. Celles-ci sont
    // celles du lancer a l'arret — le seul cas qui en ait, et celui qu'un joueur immobile voit tous
    // les jours.

    /** Le tick de la plus haute hauteur : le dernier ou la vitesse est encore positive. */
    public static final int APEX_TICK = apexTick(INIT_VEL);

    /** Le tick ou elle revient a sa hauteur de depart, en descendant. */
    public static final int LAND_TICK = landTick(INIT_VEL);

    /** Et le premier tick ou le tir est ouvert : la borne de {@link #READY}, sur la retombee. */
    public static final int READY_TICK = firstTickWhere(t -> isReady(t, INIT_VEL));

    private CoinToss() {
    }

    /**
     * La vitesse verticale au tick {@code ticks}, pour un lancer parti a {@code launchVel}.
     *
     * <p>{@code launchVel} vaut {@link #INIT_VEL} plus la vitesse verticale du joueur au moment du
     * lancer : l'original posait {@code motionY = player.motionY} avant d'y ajouter son {@code INITVEL}.
     */
    public static double velocity(double ticks, double launchVel) {
        return launchVel - GRAVITY * ticks;
    }

    /** Sa hauteur RELATIVE a celle du lancer, au tick {@code ticks} : la somme des vitesses. */
    public static double height(double ticks, double launchVel) {
        return launchVel * ticks - GRAVITY * ticks * (ticks + 1) / 2.0;
    }

    /** Le dernier tick ou elle monte encore. */
    public static int apexTick(double launchVel) {
        return firstTickWhere(t -> velocity(t, launchVel) <= 0) - 1;
    }

    /** Et sa hauteur a ce moment-la, relative a celle du lancer. */
    public static double apexHeight(double launchVel) {
        return height(apexTick(launchVel), launchVel);
    }

    /**
     * Ou en est la piece dans son vol, de 0 a 1.
     *
     * <p>C'est la courbe de l'original. La montee compte pour la <b>premiere moitie</b> : la vitesse
     * qu'il lui reste, rapportee a {@link #INIT_VEL} — donc le sommet vaut toujours 0,5, meme pour un
     * lancer parti plus vite, qui commence alors sous zero. La descente compte pour la seconde : la
     * hauteur perdue depuis le sommet, sur la hauteur totale du vol. Donc 0 au lancer a l'arret, 0,5
     * au sommet, 1 au retour.
     */
    public static double progress(double ticks, double launchVel) {
        if (velocity(ticks, launchVel) > 0) {
            return (INIT_VEL - velocity(ticks, launchVel)) / INIT_VEL * 0.5;
        }
        double apex = apexHeight(launchVel);
        if (apex <= 0) return 1.0;
        return Math.min(1.0, 0.5 + (apex - height(ticks, launchVel)) / apex * 0.5);
    }

    /** Le railgun part-il ? La piece doit avoir depasse {@link #READY} de son vol. */
    public static boolean isReady(double ticks, double launchVel) {
        return progress(ticks, launchVel) > READY;
    }

    /** Le tick ou elle revient a sa hauteur de depart, ou passe en dessous. */
    public static int landTick(double launchVel) {
        return firstTickWhere(t -> height(t, launchVel) <= 0 && velocity(t, launchVel) < 0);
    }

    /**
     * La piece a-t-elle fini son vol ?
     *
     * <p>Oui des qu'elle redescend au niveau de la main de son lanceur <b>maintenant</b> — et pas a
     * celui d'ou elle est partie. {@code handDrop} est ce que cette main a bouge depuis le lancer :
     * zero si le joueur n'a pas bouge, positif s'il est monte (et elle est alors rattrapee plus tot),
     * negatif s'il est descendu (et elle continue de tomber avec lui). C'est le
     * {@code posY < player.posY} de l'original.
     */
    public static boolean hasLanded(double ticks, double launchVel, double handDrop) {
        return velocity(ticks, launchVel) < 0 && height(ticks, launchVel) <= handDrop;
    }

    /** Le premier tick ou la condition est vraie, en partant de 1 — et jamais avant. */
    private static int firstTickWhere(java.util.function.IntPredicate condition) {
        for (int ticks = 1; ticks <= MAX_LIFE; ticks++) {
            if (condition.test(ticks)) return ticks;
        }
        return MAX_LIFE;
    }
}
