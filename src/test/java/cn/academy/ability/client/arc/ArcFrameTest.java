package cn.academy.ability.client.arc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le repere d'un eclair.
 *
 * <p>C'est ici qu'etait la faute qui faisait partir l'eclair de travers : le rendu passait
 * deux fois la meme direction comme axes lateraux du motif, au lieu de deux
 * perpendiculaires. Rien ne le disait a la compilation, et a l'ecran l'eclair s'effilochait
 * en haut a gauche au lieu de partir de la main. Trois directions unitaires, qui se coupent
 * a angle droit : voila ce qui doit toujours tenir, pour n'importe quelle visee.
 */
class ArcFrameTest {

    private static final double[] FROM = { 10, 64, 20 };
    private static final double[] UP = { 0, 1, 0 };

    /** La visee, corrigee par un peu de lacet et de tangage, pour couvrir les cas. */
    private static double[] direction(double yaw, double pitch) {
        double cos = Math.cos(Math.toRadians(pitch));
        return new double[] { cos * Math.sin(Math.toRadians(yaw)), Math.sin(Math.toRadians(pitch)),
                cos * Math.cos(Math.toRadians(yaw)) };
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double size(double[] v) {
        return Math.sqrt(dot(v, v));
    }

    @Test
    @DisplayName("les trois directions sont unitaires et se coupent a angle droit")
    void lesTroisDirectionsSontPerpendiculaires() {
        // C'etait le defaut : deux directions confondues. On balaie donc les visees — le
        // droit devant, en biais, vers le sol, vers le ciel — et pour chacune les trois
        // directions doivent etre unitaires et se couper a angle droit.
        for (double yaw : new double[] { 0, 45, 90, 180, -120 }) {
            for (double pitch : new double[] { -80, -45, -10, 0, 20, 60, 89 }) {
                double[] to = { FROM[0], FROM[1], FROM[2] };
                double[] unit = direction(yaw, pitch);
                to[0] += unit[0] * 8;
                to[1] += unit[1] * 8;
                to[2] += unit[2] * 8;

                ArcFrame frame = ArcFrame.between(FROM, to, UP);
                assertNotNull(frame, "lacet " + yaw + " tangage " + pitch);

                String ou = " (lacet " + yaw + ", tangage " + pitch + ")";
                assertEquals(1, size(frame.axis()), 1e-9, "axe unitaire" + ou);
                assertEquals(1, size(frame.side()), 1e-9, "cote unitaire" + ou);
                assertEquals(1, size(frame.up()), 1e-9, "haut unitaire" + ou);
                assertEquals(0, dot(frame.axis(), frame.side()), 1e-9, "axe et cote" + ou);
                assertEquals(0, dot(frame.axis(), frame.up()), 1e-9, "axe et haut" + ou);
                assertEquals(0, dot(frame.side(), frame.up()), 1e-9, "cote et haut" + ou);
            }
        }
    }

    @Test
    @DisplayName("l'axe suit bien les deux bouts")
    void lAxeSuitLesDeuxBouts() {
        ArcFrame frame = ArcFrame.between(FROM, new double[] { 10, 64, 28 }, UP);

        assertEquals(0, frame.axis()[0], 1e-9);
        assertEquals(0, frame.axis()[1], 1e-9);
        assertEquals(1, frame.axis()[2], 1e-9, "l'arc part vers le sud : l'axe aussi");
    }

    @Test
    @DisplayName("un tir droit devant garde ses deux ecarts dans le plan de l'ecran")
    void unTirDroitDevantGardeSesEcartsDansLEcran() {
        // Le motif ecarte l'eclair dans ses deux directions laterales. Si l'une d'elles part
        // vers l'oeil, l'ecart ne se voit pas et l'eclair perd la moitie de son zigzag :
        // pour un tir droit devant, les deux doivent donc rester perpendiculaires au regard.
        double[] look = direction(30, 15);
        double[] to = { FROM[0] + look[0] * 10, FROM[1] + look[1] * 10, FROM[2] + look[2] * 10 };
        ArcFrame frame = ArcFrame.between(FROM, to, UP);

        // La verticale vraie de l'ecran, pour la visee : le regard croise la verticale du
        // monde, et le repere de l'ecran est (regard, cote, haut de l'ecran).
        double[] right = { look[2], 0, -look[0] };
        double rightSize = size(right);
        right = new double[] { right[0] / rightSize, right[1] / rightSize, right[2] / rightSize };
        double[] screenUp = { right[1] * look[2] - right[2] * look[1],
                right[2] * look[0] - right[0] * look[2], right[0] * look[1] - right[1] * look[0] };

        // Le plan du motif est celui de l'axe et de la verticale d'ecran : ses deux
        // directions sont donc dans le plan de l'ecran, et l'une des deux est cette
        // verticale. L'autre lui est perpendiculaire, et perpendiculaire au regard.
        assertEquals(0, Math.abs(dot(frame.up(), look)), 1e-6,
                "la verticale du motif est dans le plan de l'ecran");
        assertTrue(Math.abs(dot(frame.side(), screenUp)) < 1e-6,
                "et le cote du motif est perpendiculaire a la hauteur de l'ecran");
    }

    @Test
    @DisplayName("un arc sans longueur n'a pas de repere")
    void unArcSansLongueurNAPasDeRepere() {
        assertNull(ArcFrame.between(FROM, FROM, UP));
        assertNull(ArcFrame.between(FROM, new double[] { FROM[0], FROM[1] + 1e-9, FROM[2] }, UP));
    }

    @Test
    @DisplayName("un arc vertical garde quand meme un repere utilisable")
    void unArcVerticalGardeUnRepere() {
        // Droit au-dessus de la tete, la verticale de l'ecran ne dit plus rien : le repere
        // doit se rabattre sur une perpendiculaire plutot que de rendre des directions nulles.
        ArcFrame frame = ArcFrame.between(FROM, new double[] { FROM[0], FROM[1] + 5, FROM[2] }, UP);

        assertNotNull(frame);
        assertEquals(1, size(frame.side()), 1e-9);
        assertEquals(1, size(frame.up()), 1e-9);
        assertEquals(0, dot(frame.side(), frame.up()), 1e-9);
    }

    @Test
    @DisplayName("un coin du motif tombe ou on l'attend")
    void unCoinDuMotifTombeOuOnLAttend() {
        ArcFrame frame = ArcFrame.between(FROM, new double[] { FROM[0] + 4, FROM[1], FROM[2] }, UP);

        // Le long de l'axe : l'origine du motif est le point de depart, et sa portee est le
        // point d'arrivee.
        double[] start = frame.point(FROM, 0, 0, 0);
        double[] end = frame.point(FROM, 4, 0, 0);

        assertEquals(FROM[0], start[0], 1e-9);
        assertEquals(FROM[1], start[1], 1e-9);
        assertEquals(FROM[2], start[2], 1e-9);
        assertEquals(FROM[0] + 4, end[0], 1e-9);
        assertEquals(FROM[1], end[1], 1e-9);
        assertEquals(FROM[2], end[2], 1e-9);

        // Le Y du motif est la LARGEUR du ruban, et le Z sa normale. Dans l'original, cette
        // largeur venait de crossProduct(direction, (0,0,1)), donc du Y local : la face du
        // ruban regarde le cote. Les echanger couche les rubans a plat, et l'eclair prend
        // l'apparence d'une lame vue de dessus — le joueur l'a vu tout de suite.
        double[] width = frame.point(FROM, 0, 0.5, 0);
        double[] normal = frame.point(FROM, 0, 0, 0.5);
        for (int i = 0; i < 3; i++) {
            assertEquals(frame.up()[i] * 0.5, width[i] - FROM[i], 1e-9,
                    "la largeur du ruban suit la hauteur de l'ecran");
            assertEquals(frame.side()[i] * 0.5, normal[i] - FROM[i], 1e-9,
                    "et sa normale suit le cote");
        }
    }
}
