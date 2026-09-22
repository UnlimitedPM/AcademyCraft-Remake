package cn.academy.ability.develop;

/**
 * Les deux qualites de developeur d'aptitudes.
 *
 * Portage de {@code DeveloperType} de la 1.12.2, sans sa variante PORTABLE : dans
 * l'original celle-ci etait un objet et non un bloc, et l'objet correspondant
 * n'est pas encore porte.
 *
 * <dl>
 *   <dt>{@code bandwidth}</dt><dd>energie acceptee par tick depuis le reseau</dd>
 *   <dt>{@code energy}</dt><dd>taille du tampon</dd>
 *   <dt>{@code tps}</dt><dd>ticks par stimulation ; plus c'est petit, plus
 *       l'apprentissage est rapide</dd>
 *   <dt>{@code cps}</dt><dd>energie consommee par stimulation, repartie sur les
 *       {@code tps} ticks</dd>
 * </dl>
 *
 * Les chiffres sont ceux de l'original : un developeur avance a un tampon quatre
 * fois plus gros et va plus vite, mais chaque stimulation lui coute un peu moins
 * cher. Le vrai avantage de l'avance est donc la vitesse, pas l'economie.
 */
public enum DeveloperType {

    NORMAL(100.0d, 50_000.0d, 20, 700.0d),
    ADVANCED(300.0d, 200_000.0d, 15, 600.0d);

    private final double bandwidth;
    private final double energy;
    private final int tps;
    private final double cps;

    DeveloperType(double bandwidth, double energy, int tps, double cps) {
        this.bandwidth = bandwidth;
        this.energy = energy;
        this.tps = tps;
        this.cps = cps;
    }

    public double getBandwidth() {
        return bandwidth;
    }

    public double getEnergy() {
        return energy;
    }

    /** Ticks par stimulation. */
    public int getTps() {
        return tps;
    }

    /** Energie consommee par stimulation. */
    public double getCps() {
        return cps;
    }

    /** Energie consommee par tick pendant une stimulation. */
    public double getEnergyPerTick() {
        return cps / tps;
    }

    /**
     * Energie totale d'un apprentissage.
     *
     * C'est ce que l'ecran affiche avant de lancer : sans ce chiffre le joueur
     * n'aurait aucun moyen de savoir ce qu'une aptitude va lui couter.
     */
    public double getTotalCost(int stimulations) {
        return cps * stimulations;
    }
}
