package cn.academy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les chiffres de l'eolienne, relus sans Minecraft.
 *
 * <p>La hauteur exigee et la vitesse des pales viennent de l'original. Ce sont eux qui
 * decident si une eolienne tourne : huit piliers au moins, la base au pied, et soixante
 * degres par seconde.
 */
class WindgenStructureTest {

    @Test
    @DisplayName("il faut huit piliers, et pas plus de quarante")
    void laHauteurExigeeVientDeLOriginal() {
        assertEquals(8, WindgenStructure.MIN_PILLARS);
        assertEquals(40, WindgenStructure.MAX_PILLARS);
    }

    @Test
    @DisplayName("une colonne trop courte ou sans base ne porte pas le rotor")
    void laColonneDoitEtreComplete() {
        assertFalse(WindgenStructure.isComplete(7, true), "sept piliers : trop court");
        assertTrue(WindgenStructure.isComplete(8, true), "huit piliers : la colonne tient");
        assertTrue(WindgenStructure.isComplete(40, true), "quarante piliers : le maximum");
        assertFalse(WindgenStructure.isComplete(8, false), "sans base au pied, ce n'est pas une colonne");
        assertFalse(WindgenStructure.isComplete(0, false), "rien du tout");
    }

    @Test
    @DisplayName("les pales tournent en six secondes, et seulement si la colonne tient")
    void lesPalesTournentSurUneColonneComplete() {
        assertEquals(60.0f, WindgenStructure.spinSpeed(true), 0.001f);
        assertEquals(0.0f, WindgenStructure.spinSpeed(false), 0.001f);

        // Une seconde : soixante degres. Six secondes : un tour complet, et on repart a zero.
        assertEquals(60.0f, WindgenStructure.spin(0f, 1.0d, true), 0.001f);
        assertEquals(0.0f, WindgenStructure.spin(0f, 6.0d, true), 0.001f);
        assertEquals(180.0f, WindgenStructure.spin(45f, 2.25d, true), 0.001f);
    }

    @Test
    @DisplayName("une eolienne a l'arret garde ses pales ou elles sont")
    void uneEolienneArreteeNeReculePas() {
        // L'original accumulait l'angle cote client : c'est ce qui evite qu'une eolienne
        // qui s'arrete remette ses pales a la verticale.
        assertEquals(123.0f, WindgenStructure.spin(123f, 2.0d, false), 0.001f);
    }
}
