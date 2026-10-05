package cn.academy.ability.client.vm;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * L'ombre des anneaux des tornades, et des ailes de tempete.
 *
 * <p>L'original posait une normale par quad — {@code glNormal3d(x0, y0, 0)} — et son pipeline les
 * eclairait de la lumiere <b>par defaut</b> de GL, celle qui vient de l'oeil. C'est ce qui rend
 * l'ombre mobile : elle suit le regard du joueur. Le port reprend les deux, la normale et la
 * lumiere de l'oeil, et c'est ce que ces deux verifications figent.
 */
class TornadoShadeTest {

    /** Le repere d'un anneau sans transformation : la normale est celle du tampon. */
    private static final Matrix4f IDENTITY = new Matrix4f();

    @Test
    void uneFaceTourneeVersLOeilEstEclairee() {
        // Un oeil au-dessus, qui regarde vers le bas : la normale (0, 1, 0) du tampon lui fait face.
        float eclaire = TornadoRenderer.shade(IDENTITY, 0, 1, new Vector3f(0, -1, 0));

        assertEquals(TornadoRenderer.SHADE_MAX, eclaire, 1e-6f,
                "la face qui regarde l'oeil est au plus clair");
    }

    @Test
    void uneFaceQuiTourneLeDosALOeilPrendLOmbre() {
        // Le meme oeil, mais en dessous : il regarde vers le haut, et la normale lui tourne le dos.
        float ombre = TornadoRenderer.shade(IDENTITY, 0, 1, new Vector3f(0, 1, 0));

        assertEquals(0.55f, ombre, 1e-6f, "et celle qui lui tourne le dos prend l'ombre");
    }

    @Test
    void lOmbreSuitLeRegard() {
        // Un oeil de cote : la normale du tampon ne lui fait plus ni face ni dos.
        float deCote = TornadoRenderer.shade(IDENTITY, 0, 1, new Vector3f(1, 0, 0));
        float auDessus = TornadoRenderer.shade(IDENTITY, 0, 1, new Vector3f(0, -1, 0));

        assertEquals(true, auDessus > deCote,
                "tourner le regard doit deplacer l'ombre : " + auDessus + " contre " + deCote);
    }
}
