package cn.academy.ability.client.arc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Le decalage de vue de l'original, celui qui porte l'eclair jusqu'a la main de son tireur.
 *
 * <p>Deux jeux de nombres, ceux de LambdaLib2, et un repere : celui de l'arc. Tout se relit ici,
 * parce que c'est la partie du moteur dont l'effet ne se voit qu'a l'ecran — et deux reglages
 * s'y sont deja trompes.
 */
class ArcViewTest {

    private static final double[] FROM = { 10, 64, -20 };
    private static final double[] ABOVE = { 0, 1, 0 };

    /** Un eclair qui part vers le nord (−Z), a l'horizontale. */
    private static double[] north(double[] from) {
        return new double[] { from[0], from[1], from[2] - 20 };
    }

    @Test
    @DisplayName("les deux decalages sont ceux de LambdaLib2, au chiffre pres")
    void lesDecalagesSontCeuxDeLOriginal() {
        // ViewOptimize : fpOffsetX -0.05, fpOffsetY -0.25, fpOffsetZ 0.2, puis
        // tpOffsetX 0.15, tpOffsetY -0.8, tpOffsetZ 0.23 — et son commentaire dit ce qu'ils font :
        // « transforms the origin to the player's hand in thirdPerson or firstPerson ».
        assertEquals(-0.05, ArcView.FIRST_PERSON[0], 1e-9, "le long de l'arc");
        assertEquals(-0.25, ArcView.FIRST_PERSON[1], 1e-9, "dans sa hauteur");
        assertEquals(0.2, ArcView.FIRST_PERSON[2], 1e-9, "sur son cote");
        assertEquals(0.15, ArcView.THIRD_PERSON[0], 1e-9);
        assertEquals(-0.8, ArcView.THIRD_PERSON[1], 1e-9);
        assertEquals(0.23, ArcView.THIRD_PERSON[2], 1e-9);
    }

    @Test
    @DisplayName("vu de l'exterieur, le depart tombe sur la main : 0,82 bloc des pieds")
    void vuDeLExterieurLeDepartTombeSurLaMain() {
        // Les yeux sont a 1,62 bloc. Le decalage de vue externe descend de 0,8 : le depart est
        // donc a 0,82 bloc des pieds, la hauteur d'une main qui pend. C'est le point que le
        // joueur a decrit — celui ou les jambes, le torse et le bras se rejoignent. La sienne
        // valait 1,07, puis 0,75 : les deux etaient a cote.
        double[] origin = ArcView.fix(FROM, north(FROM), ABOVE, ArcView.THIRD_PERSON)[0];

        assertEquals(FROM[0] + ArcView.THIRD_PERSON[2], origin[0], 1e-9, "du cote de la main");
        assertEquals(FROM[1] + ArcView.THIRD_PERSON[1], origin[1], 1e-9, "0,8 bloc sous les yeux");
        assertEquals(FROM[2] - ArcView.THIRD_PERSON[0], origin[2], 1e-9, "et en avant, le long de l'arc");
        assertEquals(0.82, 1.62 + ArcView.THIRD_PERSON[1], 1e-9, "soit 0,82 bloc au-dessus des pieds");
    }

    @Test
    @DisplayName("le decalage se mesure dans le repere de l'arc, pas dans celui du monde")
    void leDecalageSeMesureDansLeRepereDeLArc() {
        // C'est ce qui rend l'illusion stable, et ce qu'aucun depart pose sur le corps ne donne :
        // en visant le ciel, le decalage s'applique dans le repere de l'arc, donc il descend et
        // avance — et l'attaque ne part plus du personnage, exactement comme l'original.
        //
        // A 45 degres vers le haut, l'axe de l'arc vaut (0 ; 0,707 ; -0,707), sa hauteur
        // (0 ; 0,707 ; 0,707) et son cote (1 ; 0 ; 0).
        double[] from = { 0, 100, 0 };
        double[] to = { 0, 110.6066, -10.6066 };
        double[] origin = ArcView.fix(from, to, ABOVE, ArcView.THIRD_PERSON)[0];

        double diagonal = 0.70710678;
        assertEquals(ArcView.THIRD_PERSON[2], origin[0] - from[0], 1e-6, "du cote de la main");
        assertEquals((ArcView.THIRD_PERSON[0] + ArcView.THIRD_PERSON[1]) * diagonal,
                origin[1] - from[1], 1e-6, "descend");
        assertEquals((-ArcView.THIRD_PERSON[0] + ArcView.THIRD_PERSON[1]) * diagonal,
                origin[2] - from[2], 1e-6, "et avance");
    }

    @Test
    @DisplayName("ni la direction ni la longueur ne changent")
    void niDirectionNiLongueur() {
        double[] to = north(FROM);
        double[][] fixed = ArcView.fix(FROM, to, ABOVE, ArcView.FIRST_PERSON);

        for (int i = 0; i < 3; i++) {
            assertEquals(to[i] - FROM[i], fixed[1][i] - fixed[0][i], 1e-9, "l'arc glisse entier");
        }
    }

    @Test
    @DisplayName("un eclair de longueur nulle reste ou il est")
    void unEclairNulResteOuIlEst() {
        // Sans longueur il n'y a pas de repere : plutot que de rendre un point nul, on ne bouge
        // rien du tout.
        double[][] fixed = ArcView.fix(FROM, FROM.clone(), ABOVE, ArcView.THIRD_PERSON);

        assertEquals(FROM[0], fixed[0][0], 1e-9);
        assertEquals(FROM[1], fixed[0][1], 1e-9);
        assertEquals(FROM[2], fixed[0][2], 1e-9);
    }
}
