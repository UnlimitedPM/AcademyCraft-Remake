package cn.academy.energy;

/**
 * Un objet qui stocke de l'energie Imag.
 *
 * <p>Portage de l'interface {@code cn.academy.energy.api.item.ImagEnergyItem} de la
 * 1.12.2. L'original en avait DEUX implementations ({@code ItemEnergyBase} pour
 * l'unite d'energie, {@code ItemDeveloper} pour le developpeur portable) et un
 * seul gestionnaire, {@code IFItemManager} : c'est ce gestionnaire qui decidait de
 * la quantite stockee, de la bande passante et de l'affichage du niveau.
 *
 * <p>En 1.20.1 la regle vit dans {@link EnergyItems}, pour la meme raison : une
 * seule definition de « combien cet objet peut porter, et a quelle vitesse ».
 */
public interface ImagEnergyItem {

    /** Capacite du tampon, en IF. */
    double getMaxEnergy();

    /** Quantite qu'un objet accepte ou rend par tick, en IF. */
    double getBandwidth();
}
