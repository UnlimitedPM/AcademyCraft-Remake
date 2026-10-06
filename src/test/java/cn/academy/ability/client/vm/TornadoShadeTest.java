package cn.academy.ability.client.vm;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * L'ombre des anneaux des tornades, et des ailes de tempete.
 *
 * <p>L'original posait une normale par quad — {@code glNormal3d(x0, y0, 0)} — et son pipeline les
 * eclairait de la lumiere <b>par defaut</b> de GL. Le port avait d'abord refait cette lumiere avec
 * une lampe posee sur l'oeil ; elle vient maintenant du <b>soleil</b>, sa direction et sa hauteur,
 * ce que le joueur a demande. C'est ce que ces verifications figent : la face qui regarde le soleil
 * est au plus clair, celle qui lui tourne le dos au plancher, et la nuit eteint tout le monde.
 */
class TornadoShadeTest {

    /** Le repere d'un anneau sans transformation : la normale est celle du tampon. */
    private static final Matrix4f IDENTITY = new Matrix4f();

    /** Un quad dont la normale du tampon est celle-la, eclaire par le soleil donne. */
    private static float lit(double x0, double y0, TornadoRenderer.Sun sun) {
        return TornadoRenderer.shade(TornadoRenderer.normal(IDENTITY, x0, y0), sun);
    }

    @Test
    void uneFaceTourneeVersLeSoleilEstEclairee() {
        // Midi : le soleil est au-dessus, et la normale (0, 1, 0) du tampon lui fait face.
        TornadoRenderer.Sun midi = TornadoRenderer.sun(0);

        assertVec(midi.direction(), new Vec3(0, 1, 0), "a midi, le soleil est droit dessus");
        assertEquals(1.0, midi.strength(), 1e-9, "et il donne toute sa lumiere");
        assertEquals(TornadoRenderer.SHADE_MAX, lit(0, 1, midi), 1e-6f,
                "la face qui regarde le soleil est au plus clair");
    }

    @Test
    void uneFaceQuiTourneLeDosAuSoleilPrendLOmbre() {
        TornadoRenderer.Sun midi = TornadoRenderer.sun(0);

        // Le meme quad, mais retourne : sa normale pointe vers le bas, loin du soleil.
        assertEquals(0.72f, lit(0, -1, midi), 1e-6f, "et celle qui lui tourne le dos prend l'ombre");
        // De biais, entre les deux.
        float deCote = lit(1, 0, midi);
        assertEquals(0.72f, deCote, 1e-6f, "une face couchée ne prend rien non plus");
        assertEquals(true, lit(0, 1, midi) > deCote,
                "et l'ombre se voit : " + lit(0, 1, midi) + " contre " + deCote);
    }

    @Test
    void leSoleilSeLeveDansLePlanXYEtSaHauteurEteintLesAiles() {
        // Le ciel de la 1.20.1 tourne autour de l'axe des X : le soleil se leve a l'est, passe au
        // zenith a midi, et se couche a l'ouest — sans jamais sortir du plan X/Y.
        TornadoRenderer.Sun aube = TornadoRenderer.sun(Math.PI / 2);
        TornadoRenderer.Sun midi = TornadoRenderer.sun(0);
        TornadoRenderer.Sun nuit = TornadoRenderer.sun(Math.PI);

        assertVec(aube.direction(), new Vec3(1, 0, 0), "au lever, il est a l'horizon");
        assertVec(nuit.direction(), new Vec3(0, -1, 0), "et la nuit, sous nos pieds");
        for (TornadoRenderer.Sun sun : new TornadoRenderer.Sun[] { aube, midi, nuit }) {
            assertEquals(0.0, sun.direction().z, 1e-9, "aucun soleil ne sort du plan X/Y");
        }

        // Sa hauteur est ce qui eteint les ailes : plein jour au zenith, le plancher de nuit en
        // dessous de l'horizon, et rien entre les deux qui passe sous ce plancher.
        assertEquals(1.0, midi.strength(), 1e-9, "a midi, toute la lumiere");
        assertEquals(TornadoRenderer.NIGHT_MIN, aube.strength(), 1e-9,
                "a l'horizon, deja la nuit");
        assertEquals(TornadoRenderer.NIGHT_MIN, nuit.strength(), 1e-9, "et sous terre, la nuit");
        assertEquals(true, midi.strength() > aube.strength(), "le jour eclaire plus que l'aube");
    }

    @Test
    void laMemeFaceChangeDAspectQuandLeJoueurTourne() {
        // Les ailes tiennent au dos : c'est donc le CORPS qui decide de leur normale. Le meme quad,
        // au meme instant, ne prend pas la meme lumiere selon le cote ou il se trouve, et c'est ce
        // qui fait bouger l'ombre quand on tourne — ce que le joueur avait demande de garder.
        TornadoRenderer.Sun aube = TornadoRenderer.sun(Math.PI / 2);

        float versLeSoleil = lit(1, 0, aube);
        float aLOppose = lit(-1, 0, aube);
        assertEquals(true, versLeSoleil > aLOppose,
                "la face tournee vers le soleil est la plus claire : "
                        + versLeSoleil + " contre " + aLOppose);
    }

    /** Les vecteurs sont egaux a la tolerance pres. */
    private static void assertVec(Vec3 expected, Vec3 actual, String what) {
        assertEquals(expected.x, actual.x, 1e-9, what + " (x)");
        assertEquals(expected.y, actual.y, 1e-9, what + " (y)");
        assertEquals(expected.z, actual.z, 1e-9, what + " (z)");
    }
}
