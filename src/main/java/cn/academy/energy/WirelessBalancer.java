package cn.academy.energy;

import java.util.List;

/**
 * Algorithme d'equilibrage du Matrix sans fil.
 *
 * Port fidele de la boucle de {@code WirelessNet.tick()} de la 1.12.2 : le Matrix
 * redistribue l'energie entre tous les noeuds de son reseau afin qu'ils
 * atteignent tous <b>le meme pourcentage</b> de leur capacite, dans la limite de
 * sa propre bande passante et de celle de chaque noeud.
 *
 * C'est une fonction pure : aucun acces au monde, aux block entities ou a
 * Forge. Tout se teste donc en JUnit, sans lancer Minecraft (voir
 * {@code WirelessBalancerTest}).
 *
 * <h2>Le tampon, et sa bizarrerie</h2>
 *
 * Le reseau entretient un tampon compris entre 0 et {@link #BUFFER_MAX}. Chaque
 * deplacement {@code delta} est applique <b>a la fois</b> au noeud et au tampon :
 *
 * <pre>
 *   buffer += delta;
 *   node.setEnergy(cur + delta);
 * </pre>
 *
 * Consequences, toutes heritees de l'original et reproduites telles quelles :
 * <ul>
 *   <li>un noeud ne peut pas <i>ceder</i> d'energie tant que le tampon est a 0
 *       (le clamp {@code buffer + delta < 0} annule le deplacement) ;</li>
 *   <li>un noeud peut en revanche <i>recevoir</i> de l'energie avec un tampon a
 *       0, ce qui augmente le tampon du meme montant : le reseau cree alors de
 *       l'energie au lieu de la deplacer ;</li>
 *   <li>l'invariant reellement respecte est
 *       {@code energie_finale - energie_initiale == tampon_final - tampon_initial}.</li>
 * </ul>
 *
 * L'energie est bien conservee sur un cycle complet (un noeud recoit, un autre
 * cede), mais un reseau demarre tampon vide avec un noeud a charger en gagne.
 * Ce comportement est celui de la 1.12.2. Le test
 * {@code zeroBufferTransfersCreteEnergie} le fige explicitement pour qu'un
 * futur changement soit une decision consciente et non une accident.
 */
public final class WirelessBalancer {

    private WirelessBalancer() {}

    /**
     * Tampon accumule par le reseau : l'energie "en transit" entre deux ticks.
     * Repris de {@code WirelessNet.BUFFER_MAX}.
     */
    public static final double BUFFER_MAX = 2000.0d;

    /** Resultat d'un tick d'equilibrage. */
    public record Result(double buffer, double transferred) {}

    /**
     * Equilibre les noeuds en place.
     *
     * <p>L'ordre de la liste compte : c'est lui qui decide quels noeuds sont
     * servis en premier quand la bande passante du Matrix est insuffisante pour
     * tout egaliser d'un coup. L'appelant est donc invite a melanger la liste
     * avant l'appel (l'original faisait un {@code Collections.shuffle}), sinon
     * ce sont toujours les memes noeuds qui sont servis.
     *
     * @param nodes           noeuds du reseau ; leur energie est modifiee en place
     * @param matrixBandwidth energie que le Matrix peut deplacer par tick
     * @param buffer          tampon du reseau au tick precedent
     * @return le nouveau tampon et la quantite reellement deplacee
     */
    public static Result balance(List<? extends EnergyNode> nodes, double matrixBandwidth, double buffer) {
        double sum = 0.0d;
        double maxSum = 0.0d;
        for (EnergyNode node : nodes) {
            sum += node.getEnergy();
            maxSum += node.getMaxEnergy();
        }

        // L'original ne se protegeait pas contre un reseau sans capacite : la
        // division produisait un NaN qui se propageait a toutes les energies.
        if (maxSum <= 0.0d) {
            return new Result(buffer, 0.0d);
        }

        double percent = sum / maxSum;
        double transferLeft = matrixBandwidth;
        double transferred = 0.0d;

        for (EnergyNode node : nodes) {
            double current = node.getEnergy();
            double target = node.getMaxEnergy() * percent;

            double delta = target - current;
            // On ne depasse ni la bande passante restante du Matrix, ni celle du noeud.
            delta = Math.signum(delta) * Math.min(Math.abs(delta), Math.min(transferLeft, node.getBandwidth()));

            if (buffer + delta > BUFFER_MAX) {
                delta = BUFFER_MAX - buffer;
            } else if (buffer + delta < 0.0d) {
                delta = -buffer;
            }

            transferLeft -= Math.abs(delta);
            buffer += delta;
            transferred += delta;
            node.setEnergy(current + delta);

            if (transferLeft == 0.0d) break;
        }

        return new Result(buffer, transferred);
    }

    /**
     * Energie totale stockee dans un ensemble de noeuds.
     * Utile pour verifier qu'un equilibrage ne cree ni ne detruit d'energie.
     */
    public static double totalEnergy(List<? extends EnergyNode> nodes) {
        double total = 0.0d;
        for (EnergyNode node : nodes) {
            total += node.getEnergy();
        }
        return total;
    }
}
