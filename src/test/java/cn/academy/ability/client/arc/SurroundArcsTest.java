package cn.academy.ability.client.arc;

import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les arcs d'entourage : leurs trois gabarits et la boite ou ils se sement.
 *
 * <p>Ce sont les nombres d'{@code EntitySurroundArc} de l'original, et rien d'autre. Les figer
 * ici evite qu'un reglage de dessin ne les fasse deriver sans qu'on s'en apercoive.
 */
class SurroundArcsTest {

    @Test
    void lesTroisGabaritsSontCeuxDuJoueur() {
        // L'original en semait quatre, six et cinq arcs de 1,5 a 2, 3 a 4 et 3,5 a 4,5 blocs.
        // Le joueur a rallonge les portees le 30/09, en attendant de les voir a l'oeuvre.
        assertEquals(4, SurroundArcs.THIN.count(), "le fin seme quatre arcs");
        assertEquals(6, SurroundArcs.NORMAL.count(), "le moyen en seme six");
        assertEquals(5, SurroundArcs.BOLD.count(), "le gras en seme cinq");

        assertEquals(2.0, SurroundArcs.THIN.minLength(), 1e-6);
        assertEquals(6.0, SurroundArcs.THIN.maxLength(), 1e-6);
        assertEquals(4.0, SurroundArcs.NORMAL.minLength(), 1e-6);
        assertEquals(8.0, SurroundArcs.NORMAL.maxLength(), 1e-6);
        assertEquals(6.0, SurroundArcs.BOLD.minLength(), 1e-6);
        assertEquals(12.0, SurroundArcs.BOLD.maxLength(), 1e-6);
    }

    @Test
    void lesMotifsDEntourageSontCeuxDeLoriginal() {
        // Sa fabrique partait des memes valeurs par defaut que les cinq motifs de la genese
        // d'arc — retrecissement du trait 0,7, opacite 0,9 — et n'en changeait que cinq.
        assertPattern(ArcPattern.SURROUND_THIN, 0.2, 0.8, 0.7);
        assertPattern(ArcPattern.SURROUND_NORMAL, 0.3, 0.8, 0.7);
        assertPattern(ArcPattern.SURROUND_BOLD, 0.35, 1.2, 0.45);

        assertTrue(ArcPattern.all().contains(ArcPattern.SURROUND_THIN)
                        && ArcPattern.all().contains(ArcPattern.SURROUND_BOLD),
                "les gabarits doivent se retrouver par leur nom, comme les cinq autres");
        assertEquals(ArcPattern.SURROUND_THIN, ArcPattern.byName("surround_thin"));
    }

    @Test
    void laBoiteEstCelleDemandee() {
        RandomSource random = RandomSource.create(1L);
        Vec3 centre = new Vec3(10, 64, -3);

        List<Vec3> points = SurroundArcs.spread(centre, 0.8, 0, 2.6, 5, random);

        assertEquals(5, points.size());
        for (Vec3 point : points) {
            assertEquals(10, point.x, 0.4, "la largeur se repartit de part et d'autre du centre");
            assertEquals(-3, point.z, 0.4);
            assertTrue(point.y >= 64 && point.y <= 66.6, "la hauteur va du bas au haut donnes");
        }
    }

    @Test
    void lesPointsNeTombentPasTousAuMemeEndroit() {
        // Sans cela l'essaim ne serait qu'un seul arc, dessine plusieurs fois au meme endroit.
        List<Vec3> points = SurroundArcs.spread(Vec3.ZERO, 0.8, 0, 2.6, 6, RandomSource.create(2L));
        assertNotEquals(points.get(0), points.get(1));
    }

    @Test
    void lePointDeDepartReculeDEUneLongueurDArc() {
        // Un arc nait dans la boite et file dans une direction tiree. Le point recule d'une
        // longueur d'arc, pas de deux : l'eclair peut encore depasser de la moitie de lui-meme,
        // ce que le joueur a demande a voir, sans aller dans le bloc d'a cote.
        assertTrue(SurroundArcs.inset(1.0, MACHINE) >= 0.5,
                "la boite de la machine reste large");
        assertEquals(0.0, SurroundArcs.inset(1.0, SurroundArcs.BOLD), 1e-6,
                "des arcs de 3,5 a 4,5 dans un cube de un : il ne reste rien a retrecir");
        assertEquals(0.0, SurroundArcs.inset(0.5, SurroundArcs.BOLD), 1e-6,
                "et un reste negatif vaut zero, jamais un recul");

        // La promesse : demi-boite plus longueur d'arc, cela reste dans le cube, plus la moitie
        // de l'arc.
        assertTrue(SurroundArcs.inset(1.0, MACHINE) / 2.0 + MACHINE.maxLength()
                        <= 0.5 + MACHINE.maxLength() / 2.0,
                "un arc ne depasse jamais de plus de la moitie de sa taille");
    }

    /** Le gabarit de la charge : huit arcs courts et fins autour d'un bloc. */
    private static final SurroundArcs.Gabarit MACHINE =
            new SurroundArcs.Gabarit(ArcPattern.SURROUND_MICRO, 8, 0.2, 0.4);

    private static void assertPattern(ArcPattern pattern, double width, double maxOffset,
                                      double branchFactor) {
        assertEquals(width, pattern.width(), 1e-6);
        assertEquals(maxOffset, pattern.maxOffset(), 1e-6);
        assertEquals(branchFactor, pattern.branchFactor(), 1e-6);
        assertEquals(3, pattern.passes());
        assertEquals(0.9, pattern.widthShrink(), 1e-6);
        assertEquals(0.7, pattern.lengthShrink(), 1e-6);
        assertEquals(0.9, pattern.alphaShrink(), 1e-6);
    }
}