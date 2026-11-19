package cn.academy.ability.client.md;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ou un rayon se dessine, une fois pose.
 *
 * <p>C'est la partie du portage qui manquait, et que le joueur a signalee deux fois : le rayon
 * nait aux yeux de son tireur, donc dans la camera, et il remplit l'ecran. L'original ne le
 * laissait pas la — son rendu appliquait l'optimisation de vue de ses eclairs, et seuls les
 * rayons nes sur une bille y echappaient.
 *
 * <p>Ce qui se verifie ici, c'est le <b>decalage</b>, et non le rendu : les deux points sortent
 * de {@link MdRayView#place}, qui ne connait ni Minecraft ni la camera, et se relit donc sans
 * lancer un jeu. La decision — « de quel cote de l'ecran suis-je en train de regarder » — reste
 * dans {@code spawn}, ou elle a besoin du client.
 */
class MdRayViewTest {

    private static final double EPSILON = 1e-9;

    /** Un tireur debout, les yeux a 1,62 bloc, qui regarde vers le sud. */
    private static final double[] EYE = { 0, 1.62, 0 };
    private static final double[] TARGET = { 0, 1.62, 15 };

    /** La verticale de l'ecran : celle du monde, tant qu'on ne penche pas la tete. */
    private static final double[] ABOVE = { 0, 1, 0 };

    @Test
    @DisplayName("un rayon ne sur une bille ne bouge pas d'un pouce")
    void leRayonDUneBilleNeBougePas() {
        double[][] placed = MdRayView.place(MdRayKind.SMALL, EYE, TARGET, true, ABOVE);

        assertArrayEquals(EYE, placed[0], EPSILON, "il nait sur la bille, et il y reste");
        assertArrayEquals(TARGET, placed[1], EPSILON);
    }

    @Test
    @DisplayName("le pre-rayon se recolle a la main de son tireur, comme l'eclair")
    void lePreRayonSeRecolleALaMainDeSonTireur() {
        double[][] placed = MdRayView.place(MdRayKind.BARRAGE_PRE_HIT, EYE, TARGET, true, ABOVE);

        // Les nombres de l'original, dans le repere du rayon — le long de lui, dans sa hauteur,
        // puis sur son cote — et ils sont ceux des eclairs, puisque c'est le meme `ArcView` qui
        // les pose. Un regard vers le sud a sa droite a l'ouest : d'ou le x negatif.
        assertEquals(-0.2, placed[0][0], EPSILON, "0,2 bloc sur le cote droit");
        assertEquals(1.37, placed[0][1], EPSILON, "et 0,25 sous les yeux");
        assertEquals(-0.05, placed[0][2], EPSILON, "et 5 cm en arriere, comme la vue interne");
    }

    @Test
    @DisplayName("vu de l'exterieur, le meme rayon part de la main qui pend")
    void vuDeLExterieurIlPartDeLaMainQuiPend() {
        double[][] placed = MdRayView.place(MdRayKind.BARRAGE_PRE_HIT, EYE, TARGET, false, ABOVE);

        // Le decalage de la vue externe, celui de l'eclair d'un autre joueur : 0,8 bloc plus bas,
        // soit 0,82 au-dessus des pieds — la ou la main se rejoint au corps.
        assertEquals(-0.23, placed[0][0], EPSILON);
        assertEquals(0.82, placed[0][1], EPSILON, "la main qui pend, et non l'oeil");
        assertEquals(0.15, placed[0][2], EPSILON);
    }

    @Test
    @DisplayName("la pointe du rayon reste sur ce qu'il vise")
    void laPointeDuRayonResteSurCeQuilVise() {
        // Le rendu des rayons de l'original ne decalait que le DEPART — « Don't fix end to get
        // accurate pointing direction » — et c'est ce que le joueur a demande : le pre-rayon
        // finit sur la bille de silicium, et la gerbe part de la bille elle-meme. Avec la pointe
        // decalee avec le depart, les deux ne se rejoignaient plus.
        double[][] own = MdRayView.place(MdRayKind.BARRAGE_PRE_HIT, EYE, TARGET, true, ABOVE);
        double[][] other = MdRayView.place(MdRayKind.BARRAGE_PRE_HIT, EYE, TARGET, false, ABOVE);

        assertArrayEquals(TARGET, own[1], EPSILON, "la pointe ne bouge pas d'un millimetre");
        assertArrayEquals(TARGET, other[1], EPSILON, "et pas davantage vu de l'exterieur");

        // La direction, elle, change d'un rien — le depart a bouge, la pointe est restee. C'est
        // le prix de la pointe juste, et celui de l'original.
        assertTrue(own[0][1] != EYE[1], "mais le depart, lui, a bien glisse");
    }

    @Test
    @DisplayName("les deux vues ne posent pas le rayon au meme endroit")
    void lesDeuxVuesNePosentPasLeRayonAuMemeEndroit() {
        // Le point du joueur : le rayon tombait sur l'axe qui va de la camera a son personnage,
        // et il le voyait donc naitre dans sa tete. Les deux vues doivent bien se distinguer.
        double[][] own = MdRayView.place(MdRayKind.BARRAGE_PRE_HIT, EYE, TARGET, true, ABOVE);
        double[][] other = MdRayView.place(MdRayKind.BARRAGE_PRE_HIT, EYE, TARGET, false, ABOVE);

        assertTrue(other[0][1] < own[0][1], "la vue externe pose le rayon plus bas");
        assertFalse(java.util.Arrays.equals(own[0], other[0]));
    }
}
