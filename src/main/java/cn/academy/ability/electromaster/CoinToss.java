package cn.academy.ability.electromaster;

/**
 * Le lancer de piece du railgun, portage de l'{@code EntityCoinThrowing} de l'original : ce qu'il
 * faut savoir de son vol pour decider si le tir est permis.
 *
 * <p>La piece monte, retombe, et le railgun ne part que si elle est deja <b>redescendue</b> — plus
 * de {@link #READY} de son vol. C'est ce que l'original demandait (<code>coin.getProgress() &gt;
 * 0.7</code>) : le joueur lance la piece, et il a le temps de la retombee pour tirer a travers. Une
 * piece trop fraiche — encore dans sa montee — ne sert a rien, et une piece retombee au sol revient
 * simplement dans l'inventaire.
 *
 * <h2>La courbe</h2>
 *
 * <p>Celle de l'original, telle quelle. La montee compte pour la <b>premiere moitie</b> du vol :
 * {@code (INITVEL - motionY) / INITVEL * 0.5} — donc 0 au lancer, et 0,5 au sommet, quand la vitesse
 * verticale s'annule. La descente compte pour la seconde : {@code 0.5 + (maxHt - posY) / (maxHt -
 * initHt) * 0.5}, donc 0,5 au sommet et 1 quand la piece revient a sa hauteur de depart.
 *
 * <p>PURE, et c'est pour cela qu'elle vit ici : l'entite qui porte le vol n'a que des champs, et le
 * railgun ne lit que la meme fonction. Un vol qui « ne monte pas assez » ou une fenetre de tir trop
 * courte se relisent donc en JUnit, sans monde ni joueur.
 */
public final class CoinToss {

    /** La vitesse verticale du lancer : {@code INITVEL} de l'original. */
    public static final double INIT_VEL = 0.92;

    /** Et sa gravite, par tick. */
    public static final double GRAVITY = 0.06;

    /**
     * La vie maximale d'une piece en vol : cent vingt ticks, comme {@code MAXLIFE}.
     *
     * <p>C'est le filet : une piece qui ne retombe pas — un joueur qui vole, une piece lancee dans
     * le vide — revient dans l'inventaire au bout de six secondes, au lieu de rester a jamais.
     */
    public static final int MAX_LIFE = 120;

    /**
     * La part du vol a partir de laquelle le railgun peut partir : {@code 0.7} de l'original.
     *
     * <p>Elle laisse le temps de la retombee — un peu plus d'un tiers du vol — et rien de la montee.
     */
    public static final double READY = 0.7;

    private CoinToss() {
    }

    /**
     * Ou en est la piece dans son vol, de 0 a 1.
     *
     * @param motionY la vitesse verticale du moment, signee — negative en descente
     * @param maxHt   la plus haute hauteur atteinte
     * @param posY    sa hauteur actuelle
     * @param initHt  sa hauteur au lancer
     */
    public static double progress(double motionY, double maxHt, double posY, double initHt) {
        if (motionY > 0) {
            // La montee : la vitesse restante est la part du vol qui reste.
            return (INIT_VEL - motionY) / INIT_VEL * 0.5;
        }
        // La descente : la hauteur perdue depuis le sommet, sur la hauteur totale du vol.
        double fall = maxHt - initHt;
        if (fall <= 0) return 1.0;
        return Math.min(1.0, 0.5 + (maxHt - posY) / fall * 0.5);
    }

    /** Le railgun part-il ? La piece doit avoir depasse {@link #READY} de son vol. */
    public static boolean isReady(double progress) {
        return progress > READY;
    }
}
