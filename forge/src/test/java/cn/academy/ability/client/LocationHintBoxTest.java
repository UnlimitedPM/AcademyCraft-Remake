package cn.academy.ability.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La boite d'explication de la liste des marques : elle se taille sur son texte.
 *
 * <p>Le fichier de l'original lui donne trois cent trente-cinq unites de large — une valeur ecrite
 * pour l'anglais, ou « overworld » tient en une ligne. En francais, et des que la dimension et les
 * coordonnees se separent, elle ne suffit plus : le moteur faisait alors DEFILER le texte au lieu de
 * l'ecrire, ce que le joueur a signale le 22/10 — « le carre avec les infos devrait grandir pour que
 * le texte entier rentre dedans plutot que d'avoir du texte deroulant ».
 *
 * <p>La regle est celle du {@code MessageTab} de l'original : la largeur du plus long des textes
 * plus deux marges, une hauteur de ligne par texte. Ce fichier la fige.
 */
class LocationHintBoxTest {

    /** L'echelle de la racine des documents de l'ecran : 0,24. */
    private static final double SCALE = 0.24;

    @Test
    @DisplayName("la boite grandit avec le texte qu'elle porte")
    void laBoiteGranditAvecSonTexte() {
        double[] court = LocationTeleportScreen.infoSize(2, 100, SCALE);
        double[] longTexte = LocationTeleportScreen.infoSize(2, 300, SCALE);

        // La largeur se mesure depuis les pixels de l'ecran : cent pixels de texte font cent
        // pixels, donc un peu plus de quatre cents unites du document, plus les deux marges.
        assertEquals(100 / SCALE + 40, court[0], 1.0E-6);
        assertTrue(longTexte[0] > court[0], "un texte deux fois plus long fait une boite plus large");
        assertEquals(300 / SCALE + 40, longTexte[0], 1.0E-6);

        // Et elle depasse la largeur du fichier des que le texte ne tient plus dedans : c'est
        // exactement ce qui manquait.
        assertTrue(longTexte[0] > 335.83, "la largeur du fichier ne doit plus etre un plafond");
    }

    @Test
    @DisplayName("une ligne de plus, une hauteur de ligne de plus")
    void uneLigneDePlusUneHauteurDePlus() {
        double[] deux = LocationTeleportScreen.infoSize(2, 200, SCALE);
        double[] quatre = LocationTeleportScreen.infoSize(4, 200, SCALE);

        // Quarante-deux unites par ligne, vingt de marge en haut et en bas : les nombres du
        // MessageTab de l'original.
        assertEquals(2 * 42.0 + 40, deux[1], 1.0E-6);
        assertEquals(4 * 42.0 + 40, quatre[1], 1.0E-6);
        assertEquals(2 * 42.0, quatre[1] - deux[1], 1.0E-6);

        // Les quatre renseignements tiennent donc : la dimension, les coordonnees, le prix, et le
        // refus s'il y en a un.
        assertEquals(quatre[0], LocationTeleportScreen.infoSize(4, 200, SCALE)[0], 1.0E-6,
                "le nombre de lignes ne change pas la largeur");
    }

    @Test
    @DisplayName("une echelle absente ne fait pas une boite infinie")
    void uneEchelleAbsenteNeFaitPasUneBoiteInfinie() {
        // L'echelle vient du parent, et vaut zero tant que l'arbre n'est pas pose : la boite se lit
        // alors comme si elle etait a l'echelle un, au lieu de partir a l'infini.
        double[] size = LocationTeleportScreen.infoSize(2, 120, 0.0);
        assertEquals(120 + 40, size[0], 1.0E-6);
        assertEquals(2 * 42.0 + 40, size[1], 1.0E-6);
    }
}
