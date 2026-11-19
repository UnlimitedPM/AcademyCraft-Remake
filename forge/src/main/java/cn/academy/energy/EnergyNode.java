package cn.academy.energy;

/**
 * Vue minimaliste d'un noeud d'energie, du point de vue de l'algorithme
 * d'equilibrage du Matrix.
 *
 * Portage de {@code cn.academy.energy.api.block.IWirelessNode} de la 1.12.2,
 * reduit a ce dont le calcul a besoin : les block entities du port n'ont pas a
 * implementer toute l'API d'origine pour beneficier de l'equilibrage.
 */
public interface EnergyNode {

    /** Energie actuellement stockee. */
    double getEnergy();

    /** Fixe l'energie stockee (la valeur est deja clampee par l'appelant). */
    void setEnergy(double value);

    /** Capacite maximale de ce noeud. */
    double getMaxEnergy();

    /** Energie que ce noeud accepte d'echanger par tick. */
    double getBandwidth();
}
