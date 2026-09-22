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
 *
 * <h2>L'ordre est le sens</h2>
 *
 * La condition qui decide si une machine sait enseigner une competence compare les
 * <b>ordinaux</b> : {@code developer.ordinal() >= requis.ordinal()}. Une machine avancee
 * sait donc tout ce que sait une machine modeste, sans table de correspondance — c'est
 * exactement ce que faisait l'original. <b>L'ordre de declaration fait partie du
 * contrat</b> : PORTABLE d'abord, puis NORMAL, puis ADVANCED.
 *
 * <p>Le portable est le plus modeste des trois — plus lent (25 ticks par stimulation
 * contre 20) et plus cher (750 par stimulation contre 700), avec un tampon de 10 000 et
 * non 50 000. C'est le prix a payer pour l'avoir dans sa poche.
 */
public enum DeveloperType {

    PORTABLE(50.0d, 10_000.0d, 25, 750.0d),
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
     * La cle de langue du nom de la machine.
     *
     * La classe reste sans Minecraft — c'est ce qui permet de la relire en JUnit — donc
     * elle rend une cle et non un texte. C'est la condition de qualite qui s'en sert pour
     * dire au joueur quelle machine il lui faut.
     */
    public String nameKey() {
        return switch (this) {
            case PORTABLE -> "academy.developer.type.portable";
            case NORMAL -> "academy.developer.type.normal";
            case ADVANCED -> "academy.developer.type.advanced";
        };
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
