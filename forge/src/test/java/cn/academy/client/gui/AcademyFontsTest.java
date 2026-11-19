package cn.academy.client.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La police de l'interface : sa taille, et la largeur qu'elle annonce.
 *
 * <p>Le port a dessine son texte avec la police du jeu jusqu'a recemment, en laissant le moteur
 * arrondir la taille demandee : les écrans ont donc tous été posés pour une capitale de sept pixels
 * et des pas de ligne de dix à vingt. La police du système est arrivée après, et avec elle une
 * <b>échelle fausse</b> : la taille demandée était divisée par la cellule du videau, qui fait le
 * quart de plus que le cadratin, si bien que tout sortait un quart trop petit — quatre pixels et
 * demi de capitale au lieu de sept, et un « H » de 4,6 pixels au lieu de 6. Le joueur l'a dit en
 * cinq mots : « un peu petite, pas assez large ».
 *
 * <p>Ces trois tests tiennent ce qui se mesure sans le jeu : la largeur annoncée est celle qui se
 * dessine, elle suit la taille demandée, et les capitales ont la hauteur de celles du jeu.
 */
class AcademyFontsTest {

    @Test
    @DisplayName("la largeur est la somme exacte des avances, arrondie une seule fois")
    void laLargeurEstLaSommeDesAvances() {
        assertEquals(0, AcademyFonts.width("", AcademyFonts.GUI_SIZE), "un texte vide ne fait rien");

        // L'original n'arrondissait AUCUNE avance : il avancait de la valeur exacte de chaque lettre
        // (x += info.width * scale, en flottants) et laissait la position fractionnaire au dessin.
        // Le port arrondissait chaque avance a un pixel entier, ce qui perdait un demi-pixel par
        // lettre : une ligne de trente caracteres sortait une dizaine de pixels plus courte que
        // celle du vrai mod. C'est ce que le joueur a vu — « un peu plus etire vers la droite ».
        double one = AcademyFonts.advance('H', AcademyFonts.GUI_SIZE);
        assertEquals((int) Math.round(2 * one), AcademyFonts.width("HH", AcademyFonts.GUI_SIZE),
                "deux H font deux avances exactes");
        assertEquals((int) Math.round(4 * one), AcademyFonts.width("HHHH", AcademyFonts.GUI_SIZE),
                "et quatre en font quatre, arrondies une seule fois au bout");
    }

    @Test
    @DisplayName("une taille deux fois plus grande fait un texte deux fois plus large")
    void laLargeurSuitLaTaille() {
        int small = AcademyFonts.width("Hello", AcademyFonts.GUI_SIZE);
        int large = AcademyFonts.width("Hello", 2 * AcademyFonts.GUI_SIZE);

        assertTrue(small > 0, "un mot a une largeur");
        assertTrue(large > small, "et il grandit avec la taille demandee");
        assertTrue(Math.abs(large - 2 * small) <= "Hello".length(),
                "deux fois la taille, deux fois la largeur, a l'arrondi d'un caractere pres");
    }

    @Test
    @DisplayName("les capitales sont aussi hautes que celles du jeu")
    void lesCapitalesSontAussiHautesQueCellesDuJeu() {
        // La police du jeu fait sept pixels de capitale. La notre vaut 5,5 px a la cellule de
        // l'interface — c'est la mesure qui dit si le texte est lisible a cote du sien, et c'est
        // elle que le reglage de la cellule fait bouger : la borne basse attrape une cellule qui
        // aurait perdu ses lettres, la haute une taille qui deborderait les pas de ligne des ecrans.
        int height = AcademyFonts.capitalHeight(AcademyFonts.GUI_SIZE);
        assertTrue(height >= 5, "une capitale de " + height + " px est trop courte");
        assertTrue(height <= 9, "et une capitale de " + height + " px ne tiendrait pas dans les lignes");
    }

    @Test
    @DisplayName("la taille d'un texte EST la cellule, bornee des deux cotes")
    void laCelluleEstLaTaille() {
        // Les pages du mod demandent des cellules de 6, 8, 9, 10, 12 et 13 pixels : c'est deja
        // l'unite de l'original, dont le `FontOption(n)` dessine des quads de n pixels de cote. Il
        // n'y a donc rien a traduire, seulement de quoi borner un document abime.
        for (double cell : new double[]{6, 8, 9, 10, 12, 13}) {
            assertEquals((int) cell, AcademyFonts.cell(cell),
                    "une cellule de " + cell + " px se dessine telle quelle");
        }

        assertEquals(AcademyFonts.GUI_SIZE, AcademyFonts.cell(AcademyFonts.GUI_SIZE),
                "la cellule de l'interface est deja la bonne unite");
        assertTrue(AcademyFonts.cell(1) >= 5, "sous cinq pixels, la lettre n'existe plus");
        assertTrue(AcademyFonts.cell(400) <= 33, "et au-dela on demanderait plus que la gravure");
    }
}
