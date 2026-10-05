package cn.academy.ability.client.tp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le sens de l'image sur un carre tourne vers l'oeil.
 *
 * <p>C'est ce qu'aucune des quatre portes ne regarde, et c'est exactement ce qui s'est rate : les
 * fragments de formule se dessinaient a l'envers, et seul le joueur l'a vu. Dans Minecraft, v vaut
 * zero en HAUT de l'image — le croire en bas retourne le glyphe sans rien casser d'autre.
 */
class BillboardTest {

    @Test
    @DisplayName("le haut de l'image est en haut, et la gauche a gauche")
    void leSensDeLImage() {
        for (int corner = 0; corner < Billboard.corners(); corner++) {
            if (Billboard.high(corner) > 0) {
                assertEquals(0f, Billboard.v(corner), 1e-6,
                        "un coin en haut de l'ecran lit le HAUT de l'image");
            } else {
                assertEquals(1f, Billboard.v(corner), 1e-6,
                        "un coin en bas lit le bas de l'image");
            }

            if (Billboard.along(corner) < 0) {
                assertEquals(0f, Billboard.u(corner), 1e-6, "un coin a gauche lit la gauche");
            } else {
                assertEquals(1f, Billboard.u(corner), 1e-6, "un coin a droite lit la droite");
            }
        }
    }

    @Test
    @DisplayName("les quatre coins font un carre centre, et les quatre coins de l'image")
    void lesQuatreCoins() {
        assertEquals(4, Billboard.corners(), "un carre a quatre coins");

        double alongSum = 0;
        double highSum = 0;
        for (int corner = 0; corner < Billboard.corners(); corner++) {
            assertTrue(Math.abs(Billboard.along(corner)) == 1, "chaque coin est a un bord");
            assertTrue(Math.abs(Billboard.high(corner)) == 1, "et sur un bord en hauteur");
            alongSum += Billboard.along(corner);
            highSum += Billboard.high(corner);
        }

        assertEquals(0, alongSum, 1e-9, "deux coins a gauche, deux a droite : le carre est centre");
        assertEquals(0, highSum, 1e-9, "deux en haut, deux en bas : il l'est aussi en hauteur");
    }
}
