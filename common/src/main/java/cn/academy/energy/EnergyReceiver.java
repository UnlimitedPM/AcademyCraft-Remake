package cn.academy.energy;

/**
 * Machine qui consomme l'energie du reseau Imag.
 *
 * Portage de {@code IWirelessReceiver} de la 1.12.2. Comme pour un generateur,
 * la machine ne connait pas son noeud : c'est le noeud qui vient pousser
 * l'energie chaque tick.
 */
public interface EnergyReceiver {

    /** Energie qu'il reste a remplir dans la machine. Toujours positive ou nulle. */
    double getRequiredEnergy();

    /**
     * Pousse de l'energie dans la machine.
     *
     * @param amount quantite proposee, toujours positive
     * @return l'energie <b>non</b> acceptee. L'appelant en deduit ce qui est
     *         reellement entre, ce qui evite d'avoir a faire confiance deux fois
     *         au meme chiffre.
     */
    double injectEnergy(double amount);

    /** Energie maximale acceptee par tick. */
    double getBandwidth();
}
