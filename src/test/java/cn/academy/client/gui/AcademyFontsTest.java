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
    @DisplayName("la largeur annoncee est la somme des avances, caractere par caractere")
    void laLargeurEstLaSommeDesAvances() {
        assertEquals(0, AcademyFonts.width("", AcademyFonts.GUI_SIZE), "un texte vide ne fait rien");

        // C'est ce que le dessin pose : une avance par caractere, arrondie a chaque fois. Une somme
        // arrondie a la fin donnerait un texte centré un pixel a cote de sa boite.
        assertEquals(4 * AcademyFonts.width("H", AcademyFonts.GUI_SIZE),
                AcademyFonts.width("HHHH", AcademyFonts.GUI_SIZE), "quatre H font quatre fois un H");
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
        // La police du jeu fait sept pixels de capitale. C'est la mesure qui dit si le texte est
        // lisible a cote du sien, et c'est elle que le reglage de taille fait bouger : la borne
        // basse attrape le quart perdu (4,4 px), la haute une taille qui deborderait les pas de
        // ligne des ecrans.
        int height = AcademyFonts.capitalHeight(AcademyFonts.GUI_SIZE);
        assertTrue(height >= 5, "une capitale de " + height + " px est trop courte");
        assertTrue(height <= 9, "et une capitale de " + height + " px ne tiendrait pas dans les lignes");
    }
}
