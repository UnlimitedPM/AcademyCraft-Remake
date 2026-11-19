package cn.academy.energy;

/**
 * Machine capable de fournir de l'energie au reseau Imag.
 *
 * Portage de {@code IWirelessGenerator} de la 1.12.2. Un generateur n'a pas
 * besoin de savoir ou va son energie : il appartient a un noeud, et c'est le
 * noeud qui vient la chercher a chaque tick.
 */
public interface EnergyGenerator {

    /**
     * Retire de l'energie du generateur et la rend.
     *
     * @param requested quantite demandee par le noeud
     * @return quantite effectivement fournie, dans {@code [0, requested]}.
     *         Une valeur plus grande que la demande est ramenee a la demande par
     *         l'appelant : l'original se contentait d'un avertissement dans le
     *         journal, ce qui laissait le noeud deborder.
     */
    double provideEnergy(double requested);

    /** Energie maximale transmissible par tick. */
    double getBandwidth();
}
