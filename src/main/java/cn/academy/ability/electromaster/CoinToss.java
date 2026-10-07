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
 * <h2>Tout se lit sur l'AGE, et c'est ce qui rend le vol juste des deux cotes</h2>
 *
 * <p>Le port tenait d'abord une hauteur et une vitesse dans l'entite, avancees tick par tick ; la
 * <b>copie cliente</b> d'une entite ne connait ni l'une ni l'autre — elle nait du paquet de creation
 * — donc son vol repartait de zero, et il fallait lui envoyer sa hauteur par le reseau. Le joueur l'a
 * vu tout de suite : « visuellement la piece se deplace a 20 fps au lieu des 60 ».
 *
 * <p>L'age suffit a tout dire : la vitesse du tick {@code t} est {@code INIT_VEL - GRAVITY * t}, et
 * sa hauteur est la somme des vitesses d'avant. Les deux cotes calculent donc la MEME chose a partir
 * du meme age, sans rien se dire, et le vol se dessine a chaque image.
 *
 * <p>Tout est PUR, et c'est pour cela que ca vit ici : un vol qui « ne monte pas assez » ou une
 * fenetre de tir trop courte se relisent en JUnit, sans monde ni joueur.
 */
public final class CoinToss {

    /** La vitesse verticale du lancer : {@code INITVEL} de l'original. */
    public static final double INIT_VEL = 0.92;

    /** Et sa gravite, par tick. */
    public static final double GRAVITY = 0.06;

    /**
     * La vie maximale d'une piece en vol : cent vingt ticks, comme {@code MAXLIFE}.
     *
     * <p>C'est le filet de l'original, et il ne sert plus a rien : le vol dure {@link #LAND_TICK}
     * ticks, toujours, puisqu'il ne depend de rien d'autre que de l'age. Il reste pour qu'une piece ne
     * puisse pas rester ouverte a jamais si {@link #finished} change un jour.
     */
    public static final int MAX_LIFE = 120;

    /**
     * La part du vol a partir de laquelle le railgun peut partir : {@code 0.7} de l'original.
     *
     * <p>Elle laisse le temps de la retombee, et rien de la montee.
     */
    public static final double READY = 0.7;

    /** Le tick de la plus haute hauteur : le dernier ou la vitesse est encore positive. */
    public static final int APEX_TICK = firstTickWhere(t -> velocity(t) <= 0) - 1;

    /** Le tick ou elle revient a sa hauteur de depart, en descendant. */
    public static final int LAND_TICK = firstTickWhere(t -> height(t) <= 0 && velocity(t) < 0);

    /** Et le premier tick ou le tir est ouvert : la borne de {@link #READY}, sur la retombee. */
    public static final int READY_TICK = firstTickWhere(t -> progress(t) > READY);

    private CoinToss() {
    }

    /** La vitesse verticale au tick {@code ticks} du vol : positive en montee, negative en descente. */
    public static double velocity(int ticks) {
        return INIT_VEL - GRAVITY * ticks;
    }

    /** Sa hauteur RELATIVE a celle du lancer, au tick {@code ticks} : la somme des vitesses. */
    public static double height(double ticks) {
        return INIT_VEL * ticks - GRAVITY * ticks * (ticks + 1) / 2.0;
    }

    /** La hauteur du sommet, relative a celle du lancer. */
    public static double apexHeight() {
        return height(APEX_TICK);
    }

    /**
     * Ou en est la piece dans son vol, de 0 a 1.
     *
     * <p>C'est la courbe de l'original. La montee compte pour la <b>premiere moitie</b> : la vitesse
     * restante est la part du vol qui reste. La descente compte pour la seconde : la hauteur perdue
     * depuis le sommet, sur la hauteur totale du vol. Donc 0 au lancer, 0,5 au sommet, 1 au retour.
     */
    public static double progress(int ticks) {
        if (velocity(ticks) > 0) {
            return GRAVITY * ticks / INIT_VEL * 0.5;
        }
        double apex = apexHeight();
        if (apex <= 0) return 1.0;
        return Math.min(1.0, 0.5 + (apex - height(ticks)) / apex * 0.5);
    }

    /** Le railgun part-il ? La piece doit avoir depasse {@link #READY} de son vol. */
    public static boolean isReady(int ticks) {
        return progress(ticks) > READY;
    }

    /** La piece a-t-elle fini son vol ? Elle est alors revenue a sa hauteur de depart, ou en dessous. */
    public static boolean finished(int ticks) {
        return ticks >= LAND_TICK || ticks > MAX_LIFE;
    }

    /** Le premier tick ou la condition est vraie, en partant de 1 — et jamais avant. */
    private static int firstTickWhere(java.util.function.IntPredicate condition) {
        for (int ticks = 1; ticks <= MAX_LIFE; ticks++) {
            if (condition.test(ticks)) return ticks;
        }
        return MAX_LIFE;
    }
}
