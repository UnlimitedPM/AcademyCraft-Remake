package cn.academy.energy;

/**
 * Les trois qualites de noeud sans fil.
 *
 * Portage fidele de l'enum {@code BlockNode.NodeType} de la 1.12.2 : memes
 * valeurs, memes noms d'identifiant.
 *
 * <dl>
 *   <dt>{@code maxEnergy}</dt><dd>energie stockable par le noeud</dd>
 *   <dt>{@code bandwidth}</dt><dd>energie que le noeud accepte d'echanger par tick</dd>
 *   <dt>{@code range}</dt><dd>portee du signal, en blocs (distance au Matrix)</dd>
 *   <dt>{@code maxLinks}</dt><dd>nombre de generateurs/recepteurs pouvant etre
 *       raccordes a ce noeud (voir {@code NodeConn.getCapacity()} dans
 *       l'original ; a ne pas confondre avec la capacite du Matrix, qui compte
 *       les noeuds du reseau)</dd>
 * </dl>
 */
public enum NodeType {

    BASIC("basic", 15_000, 150, 9, 5),
    STANDARD("standard", 50_000, 300, 12, 10),
    ADVANCED("advanced", 200_000, 900, 19, 20);

    private final String id;
    private final int maxEnergy;
    private final int bandwidth;
    private final int range;
    private final int maxLinks;

    NodeType(String id, int maxEnergy, int bandwidth, int range, int maxLinks) {
        this.id = id;
        this.maxEnergy = maxEnergy;
        this.bandwidth = bandwidth;
        this.range = range;
        this.maxLinks = maxLinks;
    }

    /** Suffixe d'identifiant du bloc associe ({@code node_basic}, ...). */
    public String getId() {
        return id;
    }

    public int getMaxEnergy() {
        return maxEnergy;
    }

    public int getBandwidth() {
        return bandwidth;
    }

    public int getRange() {
        return range;
    }

    public int getMaxLinks() {
        return maxLinks;
    }

    /**
     * Palette de texture a utiliser selon le taux de remplissage (0 a 4).
     *
     * Reprend la formule de l'original : {@code min(4, round(4 * energy / max))},
     * soit cinq paliers egaux (0 %, 25 %, 50 %, 75 %, 100 %).
     */
    public int energyLevelFor(double energy) {
        if (energy <= 0.0d) return 0;
        return (int) Math.min(4, Math.round(4.0d * energy / maxEnergy));
    }
}
