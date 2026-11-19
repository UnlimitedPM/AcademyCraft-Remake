package cn.academy.energy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests des echanges entre un noeud et ses generateurs / recepteurs.
 *
 * Comme {@link WirelessBalancerTest}, tout est du calcul pur : aucun monde, aucun
 * block entity. C'est ce qui permet de verifier finement des cas penibles a
 * reproduire en jeu, par exemple un recepteur qui n'accepte qu'une partie de ce
 * qu'on lui propose.
 */
class NodeConnectionTest {

    private static final double EPSILON = 1.0e-6;

    /** Melange de graine fixe : les tests doivent etre reproductibles. */
    private static Random random() {
        return new Random(4242L);
    }

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
        @Override public void setEnergy(double value) { this.energy = Math.min(value, max); }
        @Override public double getMaxEnergy() { return max; }
        @Override public double getBandwidth() { return bandwidth; }
    }

    private static final class FakeGenerator implements EnergyGenerator {
        private double available;
        private final double bandwidth;

        FakeGenerator(double available, double bandwidth) {
            this.available = available;
            this.bandwidth = bandwidth;
        }

        @Override
        public double provideEnergy(double requested) {
            double given = Math.min(requested, available);
            available -= given;
            return given;
        }

        @Override public double getBandwidth() { return bandwidth; }
    }

    private static final class FakeReceiver implements EnergyReceiver {
        private double energy;
        private final double max;
        private final double bandwidth;

        FakeReceiver(double energy, double max, double bandwidth) {
            this.energy = energy;
            this.max = max;
            this.bandwidth = bandwidth;
        }

        @Override public double getRequiredEnergy() { return max - energy; }

        @Override
        public double injectEnergy(double amount) {
            double accepted = Math.min(amount, max - energy);
            energy += accepted;
            return amount - accepted;
        }

        @Override public double getBandwidth() { return bandwidth; }
    }

    private static <T> List<T> listOf(T... items) {
        List<T> list = new ArrayList<>();
        for (T item : items) list.add(item);
        return list;
    }

    // ------------------------------------------------------------------
    // Rien a echanger
    // ------------------------------------------------------------------

    @Test
    @DisplayName("sans raccordement, le noeud ne bouge pas")
    void noUsersMeansNoMovement() {
        FakeNode node = new FakeNode(500, 1000, 100);

        NodeConnection.Result result = NodeConnection.tick(node, List.of(), List.of(), random());

        assertEquals(500, node.getEnergy(), EPSILON);
        assertEquals(0, result.received(), EPSILON);
        assertEquals(0, result.supplied(), EPSILON);
    }

    // ------------------------------------------------------------------
    // Generateurs
    // ------------------------------------------------------------------

    @Test
    @DisplayName("un generateur remplit le noeud")
    void generatorFillsNode() {
        FakeNode node = new FakeNode(0, 1000, 100);
        FakeGenerator generator = new FakeGenerator(500, 100);

        NodeConnection.Result result = NodeConnection.tick(node, listOf(generator), List.of(), random());

        assertEquals(100, node.getEnergy(), EPSILON, "limite par la bande passante du generateur");
        assertEquals(100, result.received(), EPSILON);
    }

    @Test
    @DisplayName("la bande passante du noeud borne le total, pas chaque generateur")
    void nodeBandwidthIsSharedBetweenGenerators() {
        FakeNode node = new FakeNode(0, 1000, 100);
        FakeGenerator first = new FakeGenerator(500, 100);
        FakeGenerator second = new FakeGenerator(500, 100);

        NodeConnection.Result result = NodeConnection.tick(node, listOf(first, second), List.of(), random());

        assertEquals(100, node.getEnergy(), EPSILON,
                "deux generateurs de 100 ne peuvent pas injecter 200 dans un noeud a 100");
        assertEquals(100, result.received(), EPSILON);
    }

    @Test
    @DisplayName("un generateur ne remplit jamais au-dela de la capacite du noeud")
    void nodeIsNeverOverfilled() {
        FakeNode node = new FakeNode(950, 1000, 100);
        FakeGenerator generator = new FakeGenerator(500, 100);

        NodeConnection.tick(node, listOf(generator), List.of(), random());

        assertEquals(1000, node.getEnergy(), EPSILON);
    }

    @Test
    @DisplayName("un generateur qui rend plus que demande est ramene a la demande")
    void overdeliveringGeneratorIsClamped() {
        FakeNode node = new FakeNode(0, 1000, 100);
        EnergyGenerator greedy = new EnergyGenerator() {
            @Override public double provideEnergy(double requested) { return requested * 10.0d; }
            @Override public double getBandwidth() { return 100; }
        };

        NodeConnection.tick(node, listOf(greedy), List.of(), random());

        assertEquals(100, node.getEnergy(), EPSILON,
                "l'original se contentait d'un avertissement et laissait le noeud deborder");
    }

    @Test
    @DisplayName("un generateur vide est simplement ignore")
    void emptyGeneratorIsSkipped() {
        FakeNode node = new FakeNode(0, 1000, 100);
        FakeGenerator empty = new FakeGenerator(0, 100);
        FakeGenerator full = new FakeGenerator(500, 100);

        NodeConnection.tick(node, listOf(empty, full), List.of(), random());

        assertEquals(100, node.getEnergy(), EPSILON, "le noeud doit quand meme etre servi");
    }

    // ------------------------------------------------------------------
    // Recepteurs
    // ------------------------------------------------------------------

    @Test
    @DisplayName("un recepteur vide le noeud")
    void receiverDrainsNode() {
        FakeNode node = new FakeNode(500, 1000, 100);
        FakeReceiver receiver = new FakeReceiver(0, 1000, 100);

        NodeConnection.Result result = NodeConnection.tick(node, List.of(), listOf(receiver), random());

        assertEquals(400, node.getEnergy(), EPSILON);
        assertEquals(100, receiver.energy, EPSILON);
        assertEquals(100, result.supplied(), EPSILON);
    }

    @Test
    @DisplayName("un recepteur plein ne prend rien")
    void fullReceiverTakesNothing() {
        FakeNode node = new FakeNode(500, 1000, 100);
        FakeReceiver full = new FakeReceiver(1000, 1000, 100);

        NodeConnection.Result result = NodeConnection.tick(node, List.of(), listOf(full), random());

        assertEquals(500, node.getEnergy(), EPSILON);
        assertEquals(0, result.supplied(), EPSILON);
    }

    @Test
    @DisplayName("l'energie tiree est limitee par ce que le noeud contient")
    void receiverCannotTakeMoreThanStored() {
        FakeNode node = new FakeNode(30, 1000, 100);
        FakeReceiver receiver = new FakeReceiver(0, 1000, 100);

        NodeConnection.Result result = NodeConnection.tick(node, List.of(), listOf(receiver), random());

        assertEquals(0, node.getEnergy(), EPSILON);
        assertEquals(30, receiver.energy, EPSILON);
        assertEquals(30, result.supplied(), EPSILON);
    }

    @Test
    @DisplayName("le noeud ne descend jamais sous zero")
    void nodeNeverGoesNegative() {
        FakeNode node = new FakeNode(10, 1000, 100);
        FakeReceiver receiver = new FakeReceiver(0, 1000, 100);

        NodeConnection.tick(node, List.of(), listOf(receiver), random());

        assertTrue(node.getEnergy() >= 0.0d, "energie du noeud = " + node.getEnergy());
    }

    // ------------------------------------------------------------------
    // Les deux phases dans le meme tick
    // ------------------------------------------------------------------

    @Test
    @DisplayName("un tick peut recevoir puis redistribuer, chaque phase ayant sa bande passante")
    void bothPhasesUseTheFullBandwidth() {
        FakeNode node = new FakeNode(0, 1000, 100);
        FakeGenerator generator = new FakeGenerator(500, 100);
        FakeReceiver receiver = new FakeReceiver(0, 1000, 100);

        NodeConnection.Result result = NodeConnection.tick(node, listOf(generator), listOf(receiver), random());

        // Le generateur remplit de 100, puis le recepteur repart avec les 100 :
        // c'est bien 200 qui ont bouge dans le meme tick, pas 100.
        assertEquals(200, result.received() + result.supplied(), EPSILON);
        assertEquals(0, node.getEnergy(), EPSILON,
                "tout ce qui est entre est ressorti dans le meme tick");
        assertEquals(100, receiver.energy, EPSILON);
    }

    @Test
    @DisplayName("un generateur et un recepteur sur le meme noeud ne creent ni ne detruisent rien")
    void energyIsConservedAcrossBothPhases() {
        FakeNode node = new FakeNode(250, 1000, 70);
        FakeGenerator generator = new FakeGenerator(1000, 100);
        FakeReceiver receiver = new FakeReceiver(0, 1000, 100);

        double before = node.getEnergy() + generator.available;
        NodeConnection.tick(node, listOf(generator), listOf(receiver), random());

        double after = node.getEnergy() + generator.available + receiver.energy;
        assertEquals(before, after, EPSILON, "energie avant = " + before + ", apres = " + after);
    }
}
