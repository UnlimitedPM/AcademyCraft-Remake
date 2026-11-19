package cn.academy.ability.client.arc;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Les eclairs tenus : un seul a la fois, et il suit le regard.
 *
 * <p>Deux defauts du port sont figes ici. Le premier : il en posait un par tick, vivant trois
 * ticks, et le joueur y a vu trois eclairs superposes. Le second : l'eclair etant pose entre
 * deux points fixes, il mettait ses dix ticks — une demi-seconde — a se retourner quand le
 * joueur regardait ailleurs.
 */
class SustainedArcsTest {

    private static final double[] ORIGIN = {0, 0, 0};

    @Test
    void unArcTenuResteUnique() {
        ClientArcs.clear();
        Random rng = new Random(1L);

        ClientArcs.sustain(ArcPattern.CHARGING, ORIGIN, new double[] {1, 0, 0}, 10, 7, 100, rng);
        ClientArcs.sustain(ArcPattern.CHARGING, ORIGIN, new double[] {2, 0, 0}, 10, 7, 101, rng);
        ClientArcs.sustain(ArcPattern.CHARGING, ORIGIN, new double[] {3, 0, 0}, 10, 7, 102, rng);

        assertEquals(1, ClientArcs.live().size(), "trois ticks, un seul eclair");
    }

    @Test
    void unArcTenuSuitLeRegard() {
        ClientArcs.clear();

        ClientArcs.sustain(ArcPattern.CHARGING, ORIGIN, new double[] {10, 0, 0}, 10, 7, 100,
                new Random(2L));
        // Le joueur se retourne : l'eclair doit pointer la tout de suite, sans attendre sa fin.
        ClientArcs.sustain(ArcPattern.CHARGING, ORIGIN, new double[] {0, 0, 8}, 10, 7, 101,
                new Random(2L));

        List<ClientArcs.LiveArc> arcs = ClientArcs.live();
        assertEquals(1, arcs.size());
        assertEquals(8.0, arcs.get(0).to()[2], 1e-9, "le bout vise a suivi");
        assertEquals(0.0, arcs.get(0).to()[0], 1e-9, "et l'ancien bout a ete oublie");
    }

    @Test
    void chaqueTireurEtChaqueMotifOntLeLeur() {
        ClientArcs.clear();
        Random rng = new Random(3L);

        ClientArcs.sustain(ArcPattern.CHARGING, ORIGIN, new double[] {1, 0, 0}, 10, 7, 100, rng);
        ClientArcs.sustain(ArcPattern.THIN_CONTINUOUS, ORIGIN, new double[] {1, 0, 0}, 10, 7, 100, rng);
        ClientArcs.sustain(ArcPattern.CHARGING, ORIGIN, new double[] {1, 0, 0}, 10, 8, 100, rng);

        assertEquals(3, ClientArcs.live().size(),
                "deux competences ou deux joueurs ne se volent pas leur eclair");
    }

    @Test
    void laVieDUnArcTenuEstCelleDeLaGeneseDArc() {
        assertEquals(10, SustainedArcs.LIFE_TICKS,
                "dix ticks, comme la genese d'arc, deja validee");
    }
}