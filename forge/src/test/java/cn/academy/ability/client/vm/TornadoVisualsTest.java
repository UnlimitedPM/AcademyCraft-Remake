package cn.academy.ability.client.vm;

import cn.academy.util.ImprovedNoise;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La forme des tornades de vecmanip.
 *
 * <p>Ces nombres se recopient de l'original et se relisent sans Minecraft : la hauteur des anneaux,
 * l'eventail des ailes, la montee de l'opacite. Une faute ici ne se verrait qu'a l'ecran, sous la
 * forme d'une tornade plate, d'un eventail de travers, ou d'un effet qui ne s'efface jamais.
 */
class TornadoVisualsTest {

    private static TornadoVisuals.Tornado tornade(long seed) {
        return TornadoVisuals.tornado(RandomSource.create(seed), 2, 0.16, 1, 2);
    }

    @Test
    @DisplayName("les anneaux tiennent dans la hauteur, et se chevauchent")
    void lesAnneauxTiennentDansLaHauteur() {
        TornadoVisuals.Tornado tornado = tornade(1);

        assertFalse(tornado.rings().isEmpty(), "une tornade a des anneaux");

        for (TornadoVisuals.Ring ring : tornado.rings()) {
            // Le dernier anneau peut DEPASSER la hauteur d'un pas : l'original ajoutait avant de
            // tester, donc son compte s'arrete au premier pas qui l'a fait sortir. C'est cet
            // anneau-la qui ferme le sommet, et le port le garde.
            assertTrue(ring.y() > 0 && ring.y() <= tornado.height() + tornado.step(),
                    "un anneau se tient dans la hauteur, au pas pres : " + ring.y() + " pour "
                            + tornado.height());
            assertTrue(ring.width() > 0, "et il a une largeur : " + ring.width());
            assertTrue(ring.phase() >= 0 && ring.phase() < 360,
                    "sa rotation de depart est un tour complet au plus : " + ring.phase());
            assertTrue(ring.sizeScale() >= 0.9 && ring.sizeScale() <= 1.7,
                    "et son echelle est celle des deux tirages de l'original : " + ring.sizeScale());
        }
    }

    @Test
    @DisplayName("la meme graine donne la meme tornade")
    void laFormeEstToujoursLaMeme() {
        TornadoVisuals.Tornado first = tornade(2);
        TornadoVisuals.Tornado again = tornade(2);
        TornadoVisuals.Tornado other = tornade(3);

        assertEquals(first.rings(), again.rings(), "deux clients qui tirent la meme graine");
        assertEquals(first.timeOffset(), again.timeOffset(),
                "voient la meme tornade — sans quoi elles ne tourneraient pas au meme rythme");
        assertNotEquals(first.rings(), other.rings(), "et une autre graine en donne une autre");
    }

    @Test
    @DisplayName("le decalage de temps se tire avant le premier anneau")
    void leDecalageSeTireEnPremier() {
        // L'original declarait ses champs avant son bloc d'initialisation : c'est cet ordre que le
        // port doit suivre, sans quoi deux clients tireraient des tornades differentes.
        RandomSource random = RandomSource.create(4);
        double expected = random.nextDouble() * 20;
        TornadoVisuals.Tornado tornado = TornadoVisuals.tornado(
                RandomSource.create(4), 2, 0.16, 1, 2);

        assertEquals(expected, tornado.timeOffset(), 1e-12,
                "le premier tirage est celui du decalage de temps");
    }

    @Test
    @DisplayName("les ailes sont quatre fuseaux couples vers l'arriere")
    void lesAilesSontQuatreFuseaux() {
        TornadoVisuals.Layout wings = TornadoVisuals.wings(RandomSource.create(5));

        assertEquals(4, wings.parts().size(), "quatre fuseaux, comme l'original");
        assertEquals(TornadoVisuals.WINGS_TILT_X, wings.tiltX(),
                "et l'assemblage est couche vers l'arriere");
        assertTrue(wings.tiltX() < 0, "donc vers l'arriere, pas vers l'avant");

        // La table de l'original, ligne pour ligne : c'est elle qui fait l'eventail. Les deux
        // fuseaux du devant se replient d'un cote, ceux du derriere de l'autre — d'ou l'asymetrie
        // des deux derniers, dont les deux rotations partent en sens contraire.
        double[][] table = {
                { -0.1, -0.3, 0.1, TornadoVisuals.WINGS_FAN, TornadoVisuals.WINGS_FAN },
                { 0.1, -0.3, 0.1, -TornadoVisuals.WINGS_FAN, -TornadoVisuals.WINGS_FAN },
                { -0.1, -0.5, -0.1, -TornadoVisuals.WINGS_FAN, TornadoVisuals.WINGS_FAN },
                { 0.1, -0.5, -0.1, TornadoVisuals.WINGS_FAN, -TornadoVisuals.WINGS_FAN },
        };
        for (int i = 0; i < 4; i++) {
            TornadoVisuals.Part part = wings.parts().get(i);

            assertEquals(table[i][0], part.tx(), 1e-9, "fuseau " + i + " : son ecart en large");
            assertEquals(table[i][1], part.ty(), 1e-9, "fuseau " + i + " : sa hauteur");
            assertEquals(table[i][2], part.tz(), 1e-9, "fuseau " + i + " : son recul");
            assertEquals(table[i][3], part.rotateY(), 1e-9, "fuseau " + i + " : son lacet");
            assertEquals(table[i][4], part.rotateZ(), 1e-9, "fuseau " + i + " : son roulis");

            assertFalse(part.tornado().rings().isEmpty(), "chaque fuseau a ses anneaux");
        }
    }

    @Test
    @DisplayName("la colonne du canon est une seule, et large")
    void laColonneDuCanonEstUneSeule() {
        // La boule a quinze blocs : la colonne a donc toute la place de l'original.
        TornadoVisuals.Layout cannon = TornadoVisuals.cannon(RandomSource.create(6),
                TornadoVisuals.CANNON_HEIGHT + TornadoVisuals.CANNON_GAP);

        assertEquals(1, cannon.parts().size(), "une seule colonne");
        assertEquals(0, cannon.tiltX(), "dresssee, sans inclinaison");
        assertEquals(0, cannon.parts().get(0).rotateY(), "et sans rotation locale");

        TornadoVisuals.Tornado tornado = cannon.parts().get(0).tornado();
        assertEquals(TornadoVisuals.CANNON_HEIGHT, tornado.height(), "douze blocs de haut");
        assertEquals(TornadoVisuals.CANNON_SIZE, tornado.size(), "huit de large");
        assertTrue(tornado.rings().size() > 20, "et donc beaucoup d'anneaux : "
                + tornado.rings().size());
    }

    /**
     * La colonne s'arrete sous la boule, meme quand la boule se noue bas.
     *
     * <p>C'est la grotte du joueur : sa boule se noue sous le plafond au lieu de le traverser, et
     * la colonne raccourcit avec elle — sinon elle traverserait la pierre a sa place.
     */
    @Test
    @DisplayName("la colonne raccourcit quand la boule se noue bas")
    void laColonneRaccourcitAvecLaBoule() {
        double bas = 7;  // la boule est a sept blocs du sol
        TornadoVisuals.Tornado court = TornadoVisuals.cannon(RandomSource.create(6), bas)
                .parts().get(0).tornado();

        assertEquals(bas - TornadoVisuals.CANNON_GAP, court.height(), 1e-9,
                "elle monte jusqu'a trois blocs sous la boule");
        assertTrue(court.height() < TornadoVisuals.CANNON_HEIGHT, "donc plus courte qu'a l'original");
        // Et sa largeur suit sa hauteur : une colonne courte serait trapue autrement.
        assertEquals(TornadoVisuals.CANNON_SIZE * (court.height() / TornadoVisuals.CANNON_HEIGHT),
                court.size(), 1e-9, "sa largeur suit sa hauteur");
        assertTrue(court.size() < TornadoVisuals.CANNON_SIZE, "donc plus etroite");
    }

    /**
     * L'echelle du canon : un a ciel ouvert, et moins sous un plafond.
     *
     * <p>C'est la part de la colonne qui reste a dresser, et c'est elle qui sert aussi aux
     * <b>boules de plasma</b> : le joueur a demande qu'elles suivent — « il faudrait aussi un peu
     * reduire la taille des boules pour que ce soit dans le meme ordre que la grande tornade ». Si
     * elle changeait d'un cote sans l'autre, les deux ne seraient plus du meme ordre.
     */
    @Test
    @DisplayName("l'echelle du canon suit la place qu'il a")
    void lEchelleSuitLaPlaceDisponible() {
        assertEquals(1.0, TornadoVisuals.cannonScale(
                TornadoVisuals.CANNON_HEIGHT + TornadoVisuals.CANNON_GAP), 1e-9,
                "quinze blocs de place : l'echelle de l'original, un");
        assertTrue(TornadoVisuals.cannonScale(20) <= 1.0,
                "plus de place ne fait pas une colonne plus grande que douze");

        // Sept blocs : trois de colonne sur douze, soit un quart.
        assertEquals((7 - TornadoVisuals.CANNON_GAP) / TornadoVisuals.CANNON_HEIGHT,
                TornadoVisuals.cannonScale(7), 1e-9, "sept blocs de place : trois douziemes");
        // Et c'est bien l'echelle de la colonne dessinee, pas un nombre a cote.
        TornadoVisuals.Tornado court = TornadoVisuals.cannon(RandomSource.create(6), 7)
                .parts().get(0).tornado();
        assertEquals(TornadoVisuals.cannonScale(7), court.size() / TornadoVisuals.CANNON_SIZE, 1e-9,
                "la largeur de la colonne EST cette echelle");
        assertTrue(TornadoVisuals.cannonScale(4) >= TornadoVisuals.MIN_CANNON_HEIGHT
                / TornadoVisuals.CANNON_HEIGHT, "sous la boule, la colonne ne disparait pas");
    }

    @Test
    @DisplayName("le pied est presque droit, le sommet part dans tous les sens")
    void leGondolementGranditAvecLaHauteur() {
        double t = 3.5;

        for (double ny = 0; ny <= 1.0001; ny += 0.1) {
            double amplitude = 0.3 + Math.pow(ny * 2, 1.4);
            double[] wobble = TornadoVisuals.wobble(ny, t);

            assertTrue(Math.abs(wobble[0]) <= amplitude + 1e-9 && Math.abs(wobble[1]) <= amplitude + 1e-9,
                    "le gondolement est borne par l'amplitude de l'original : " + amplitude);
        }

        assertTrue(Math.abs(TornadoVisuals.wobble(1, t)[0]) > 0.3,
                "au sommet, l'amplitude depasse de loin celle du pied");
        assertNotEquals(TornadoVisuals.wobble(0.5, 1)[0], TornadoVisuals.wobble(0.5, 2)[0],
                "et le temps la fait changer : c'est ce qui donne le mouvement");
    }

    @Test
    @DisplayName("l'opacite monte, se tient, et redescend")
    void lOpaciteSuitSaCourbe() {
        assertEquals(0, TornadoVisuals.wingsChargeAlpha(0, 30), 1e-6,
                "une charge qui commence ne montre rien");
        assertEquals(TornadoVisuals.WINGS_ALPHA, TornadoVisuals.wingsChargeAlpha(30, 30), 1e-6,
                "et une charge finie montre les ailes ouvertes");
        assertEquals(TornadoVisuals.WINGS_ALPHA, TornadoVisuals.wingsFadeAlpha(0), 1e-6,
                "la fin part de l'opacite pleine");
        assertEquals(0, TornadoVisuals.wingsFadeAlpha(TornadoVisuals.WINGS_FADE_TICKS), 1e-6,
                "et s'eteint en quinze ticks");

        assertEquals(0, TornadoVisuals.cannonRiseAlpha(0), 1e-6, "la colonne se dresse du sol");
        assertEquals(TornadoVisuals.CANNON_ALPHA,
                TornadoVisuals.cannonRiseAlpha(TornadoVisuals.CANNON_RISE_TICKS), 1e-6,
                "en vingt ticks");
        assertEquals(0, TornadoVisuals.cannonFadeAlpha(TornadoVisuals.CANNON_FADE_TICKS), 1e-6,
                "et meurt en vingt autres");
        assertEquals(0.7, TornadoVisuals.DRAW_ALPHA, 1e-9,
                "le facteur de dessin de l'original, qui attenue tout de trois dixiemes");
    }

    @Test
    @DisplayName("le bruit est celui de Perlin, et il se repete tous les 256")
    void leBruitEstCeluiDePerlin() {
        assertEquals(0, ImprovedNoise.noise(0, 0, 0), 1e-12, "l'origine ne bruite rien");

        for (double x = -3; x < 3; x += 0.17) {
            for (double y = -2; y < 2; y += 0.23) {
                double n = ImprovedNoise.noise(x, y, 0.4);
                assertTrue(n >= -1.0001 && n <= 1.0001, "un bruit borne : " + n);
                assertEquals(n, ImprovedNoise.noise(x + 256, y, 0.4), 1e-9,
                        "et periodique de 256, comme la table de l'original");
            }
        }

        List<Double> values = List.of(ImprovedNoise.noise(0.1, 0.2), ImprovedNoise.noise(0.3, 0.4));
        assertNotEquals(values.get(0), values.get(1), "deux points differents bruitent autrement");
    }
}
