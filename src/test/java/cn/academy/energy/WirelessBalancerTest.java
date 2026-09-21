package cn.academy.energy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de l'algorithme d'equilibrage du Matrix.
 *
 * C'est du calcul pur : ces tests tournent en quelques millisecondes et couvrent
 * une mecanique qu'il serait autrement tres penible a verifier en jeu (il
 * faudrait poser un Matrix, plusieurs noeuds, puis observer les energies).
 *
 * Les attentes decrivent le comportement <b>reellement porte</b>, tampon
 * compris, et non celui qu'on aimerait avoir. Voir la javadoc de
 * {@link WirelessBalancer} pour l'explication du tampon.
 */
class WirelessBalancerTest {

    /** Noeud d'energie en memoire, pour les tests. */
    private static final class FakeNode implements EnergyNode {
        private double energy;
        private final double max;
        private final double bandwidth;

        FakeNode(double energy, double max, double bandwidth) {
            this.energy = energy;
            this.max = max;
            this.bandwidth = bandwidth;
        }

        @Override public double getEnergy() { return energy; }
        @Override public void setEnergy(double value) { this.energy = value; }
        @Override public double getMaxEnergy() { return max; }
        @Override public double getBandwidth() { return bandwidth; }
    }

    private static final double EPSILON = 1.0e-6;

    /** Bande passante volontairement enorme : le Matrix ne limite rien. */
    private static final double UNLIMITED = 1.0e9;

    private static List<FakeNode> nodes(FakeNode... n) {
        return new ArrayList<>(Arrays.asList(n));
    }

    /** Fait tourner l'equilibrage jusqu'a ce que les energies ne bougent plus. */
    private static double settle(List<FakeNode> list, double matrixBandwidth, double buffer, int maxTicks) {
        for (int tick = 0; tick < maxTicks; tick++) {
            double before = WirelessBalancer.totalEnergy(list);
            buffer = WirelessBalancer.balance(list, matrixBandwidth, buffer).buffer();
            if (WirelessBalancer.totalEnergy(list) == before && tick > 1) break;
        }
        return buffer;
    }

    /** Taux de remplissage, pour comparer des noeuds de capacites differentes. */
    private static double percentOf(FakeNode node) {
        return node.getEnergy() / node.getMaxEnergy();
    }

    // ------------------------------------------------------------------
    // Premier tick : un noeud ne peut ceder que ce que le tampon contient
    // ------------------------------------------------------------------

    @Test
    @DisplayName("tampon vide : le noeud deficient se charge, le noeud riche ne cede rien")
    void firstTickOnlyCharges() {
        FakeNode full = new FakeNode(1000, 1000, 1000);
        FakeNode empty = new FakeNode(0, 1000, 1000);

        WirelessBalancer.Result result = WirelessBalancer.balance(nodes(full, empty), UNLIMITED, 0);

        assertEquals(1000, full.getEnergy(), EPSILON,
                "avec un tampon a 0, le clamp empeche le noeud riche de ceder");
        assertEquals(500, empty.getEnergy(), EPSILON);
        assertEquals(500, result.buffer(), EPSILON, "le tampon enregistre ce qui a ete credite");
    }

    @Test
    @DisplayName("tampon crediteur : le noeud riche peut enfin ceder")
    void depositsAllowWithdrawals() {
        FakeNode full = new FakeNode(1000, 1000, 1000);
        FakeNode empty = new FakeNode(0, 1000, 1000);
        List<FakeNode> list = nodes(full, empty);

        double buffer = WirelessBalancer.balance(list, UNLIMITED, 0).buffer();
        assertEquals(500, buffer, EPSILON, "tick 1 : le noeud vide se charge, le tampon est credite");

        WirelessBalancer.balance(list, UNLIMITED, buffer);

        assertEquals(750, full.getEnergy(), EPSILON, "tick 2 : le noeud riche peut enfin ceder");
        assertEquals(750, empty.getEnergy(), EPSILON);
    }

    @Test
    @DisplayName("l'energie et le tampon avancent du meme montant (invariant)")
    void bufferMovesWithEnergy() {
        FakeNode a = new FakeNode(1234, 5000, 2000);
        FakeNode b = new FakeNode(0, 800, 300);
        FakeNode c = new FakeNode(700, 700, 5000);
        List<FakeNode> list = nodes(a, b, c);

        double energyBefore = WirelessBalancer.totalEnergy(list);
        WirelessBalancer.Result result = WirelessBalancer.balance(list, 400, 0);
        double energyAfter = WirelessBalancer.totalEnergy(list);

        assertEquals(energyAfter - energyBefore, result.buffer(), EPSILON);
    }

    @Test
    @DisplayName("QUIRK HERITE : tampon a 0, le premier tick cree de l'energie")
    void zeroBufferFirstTickCreatesEnergy() {
        FakeNode full = new FakeNode(1000, 1000, 1000);
        FakeNode empty = new FakeNode(0, 1000, 1000);
        List<FakeNode> list = nodes(full, empty);

        double before = WirelessBalancer.totalEnergy(list);            // 1000
        WirelessBalancer.Result result = WirelessBalancer.balance(list, UNLIMITED, 0);
        double after = WirelessBalancer.totalEnergy(list);             // 1500

        // Comportement de la 1.12.2, reproduit volontairement : voir la javadoc
        // de WirelessBalancer. Ce test existe pour que le jour ou on decide de
        // corriger ce comportement, le changement soit explicite.
        assertEquals(1500, after, EPSILON, "le reseau a credite 500 en plus de l'energie deplacee");
        assertEquals(500, after - before, EPSILON);
        assertEquals(result.buffer(), after - before, EPSILON);

        // Ce qui compte pour la suite : a partir du deuxieme tick, plus rien
        // n'est cree, l'energie totale ne bouge plus.
        double afterTick2 = settle(list, UNLIMITED, result.buffer(), 20);
        double energyTick2 = WirelessBalancer.totalEnergy(list);
        assertTrue(afterTick2 >= 0);
        assertEquals(after, energyTick2, EPSILON,
                "apres le premier tick, l'energie totale doit se stabiliser : " + after + " -> " + energyTick2);
    }

    // ------------------------------------------------------------------
    // Convergence
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a terme tous les noeuds atteignent le meme taux de remplissage")
    void convergesToSamePercentage() {
        FakeNode full = new FakeNode(1000, 1000, 1000);
        FakeNode empty = new FakeNode(0, 1000, 1000);
        List<FakeNode> list = nodes(full, empty);

        settle(list, UNLIMITED, 0, 50);

        assertEquals(percentOf(full), percentOf(empty), 1.0e-3,
                "taux differents : " + percentOf(full) + " et " + percentOf(empty));
    }

    @Test
    @DisplayName("des capacites differentes visent le meme taux, pas la meme valeur")
    void differentCapacitiesTargetSamePercentage() {
        FakeNode small = new FakeNode(1000, 1000, 100000);
        FakeNode big = new FakeNode(0, 3000, 100000);
        List<FakeNode> list = nodes(small, big);

        settle(list, UNLIMITED, 0, 200);

        assertEquals(percentOf(small), percentOf(big), 1.0e-3,
                "petit = " + small.getEnergy() + "/1000, grand = " + big.getEnergy() + "/3000");
        assertTrue(big.getEnergy() > small.getEnergy(),
                "a taux egal, le noeud de plus grande capacite stocke plus");
    }

    @Test
    @DisplayName("un reseau deja equilibre ne bouge pas")
    void alreadyBalancedIsLeftAlone() {
        FakeNode a = new FakeNode(500, 1000, 1000);
        FakeNode b = new FakeNode(250, 500, 1000);
        List<FakeNode> list = nodes(a, b);

        WirelessBalancer.Result result = WirelessBalancer.balance(list, UNLIMITED, 0);

        assertEquals(500, a.getEnergy(), EPSILON);
        assertEquals(250, b.getEnergy(), EPSILON);
        assertEquals(0, result.transferred(), EPSILON);
    }

    // ------------------------------------------------------------------
    // Limites de bande passante
    // ------------------------------------------------------------------

    @Test
    @DisplayName("la bande passante du Matrix borne le total deplace par tick")
    void matrixBandwidthLimitsTotalMovement() {
        FakeNode full = new FakeNode(1000, 1000, 10000);
        FakeNode empty = new FakeNode(0, 1000, 10000);
        List<FakeNode> list = nodes(full, empty);

        // On part d'un tampon crediteur pour que les deux noeuds puissent bouger.
        WirelessBalancer.balance(list, 100, 500);

        double moved = Math.abs(full.getEnergy() - 1000) + Math.abs(empty.getEnergy());
        assertTrue(moved <= 100 + EPSILON, "deplacement total " + moved + " > bande passante 100");
    }

    @Test
    @DisplayName("la bande passante du noeud limite son propre mouvement")
    void nodeBandwidthLimitsItsOwnTransfer() {
        // Tampon crediteur : le noeud riche est autorise a ceder.
        FakeNode stingy = new FakeNode(1000, 1000, 30);
        FakeNode empty = new FakeNode(0, 1000, 10000);
        List<FakeNode> list = nodes(stingy, empty);

        WirelessBalancer.balance(list, UNLIMITED, 500);

        assertTrue(stingy.getEnergy() >= 1000 - 30 - EPSILON,
                "le noeud a cede " + (1000 - stingy.getEnergy()) + " pour une bande passante de 30");
    }

    // ------------------------------------------------------------------
    // Tampon
    // ------------------------------------------------------------------

    @Test
    @DisplayName("le tampon ne depasse jamais son maximum")
    void bufferNeverExceedsMax() {
        FakeNode full = new FakeNode(100000, 100000, 100000);
        FakeNode empty = new FakeNode(0, 100000, 100000);

        double buffer = WirelessBalancer.BUFFER_MAX - 10;
        WirelessBalancer.Result result = WirelessBalancer.balance(nodes(full, empty), UNLIMITED, buffer);

        assertTrue(result.buffer() <= WirelessBalancer.BUFFER_MAX + EPSILON,
                "tampon " + result.buffer() + " > maximum " + WirelessBalancer.BUFFER_MAX);
    }

    @Test
    @DisplayName("le tampon ne devient jamais negatif")
    void bufferNeverGoesNegative() {
        FakeNode full = new FakeNode(100000, 100000, 100000);
        FakeNode empty = new FakeNode(0, 100000, 100000);

        WirelessBalancer.Result result = WirelessBalancer.balance(nodes(full, empty), UNLIMITED, 5);

        assertTrue(result.buffer() >= -EPSILON, "tampon negatif : " + result.buffer());
    }

    // ------------------------------------------------------------------
    // Cas limites
    // ------------------------------------------------------------------

    @Test
    @DisplayName("un reseau sans noeud ne fait rien")
    void emptyNetwork() {
        WirelessBalancer.Result result = WirelessBalancer.balance(new ArrayList<>(), 1000, 42);

        assertEquals(42, result.buffer(), EPSILON);
        assertEquals(0, result.transferred(), EPSILON);
    }

    @Test
    @DisplayName("un reseau de capacite nulle ne produit pas de NaN")
    void zeroCapacityDoesNotProduceNaN() {
        // Regression : l'original divisait par maxSum sans se proteger, et le
        // NaN se propageait a toutes les energies du reseau.
        FakeNode weird = new FakeNode(0, 0, 100);
        List<FakeNode> list = nodes(weird);

        WirelessBalancer.Result result = WirelessBalancer.balance(list, 100, 7);

        assertTrue(!Double.isNaN(weird.getEnergy()), "l'energie du noeud est devenue NaN");
        assertTrue(!Double.isNaN(result.buffer()), "le tampon est devenu NaN");
        assertEquals(0, weird.getEnergy(), EPSILON);
        assertEquals(7, result.buffer(), EPSILON);
    }

    @Test
    @DisplayName("un seul noeud reste inchange")
    void singleNodeIsUnchanged() {
        FakeNode only = new FakeNode(321, 1000, 500);

        WirelessBalancer.Result result = WirelessBalancer.balance(nodes(only), 1000, 0);

        assertEquals(321, only.getEnergy(), EPSILON);
        assertEquals(0, result.transferred(), EPSILON);
    }

    @Test
    @DisplayName("un noeud ne depasse jamais sa capacite et ne devient pas negatif")
    void nodesNeverLeaveValidRange() {
        FakeNode a = new FakeNode(5000, 5000, 5000);
        FakeNode b = new FakeNode(5000, 5000, 5000);
        FakeNode tiny = new FakeNode(0, 10, 5000);
        List<FakeNode> list = nodes(a, b, tiny);

        double buffer = 0;
        for (int tick = 0; tick < 50; tick++) {
            buffer = WirelessBalancer.balance(list, UNLIMITED, buffer).buffer();
            for (FakeNode n : list) {
                assertTrue(n.getEnergy() <= n.getMaxEnergy() + EPSILON,
                        "energie " + n.getEnergy() + " > capacite " + n.getMaxEnergy());
                assertTrue(n.getEnergy() >= -EPSILON, "energie negative : " + n.getEnergy());
            }
        }
    }
}
