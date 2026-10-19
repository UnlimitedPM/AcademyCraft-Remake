package cn.academy.energy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La partie pure de la regle energetique (portage de {@code IFItemManager}).
 *
 * <p>Ces calculs ne touchent ni registre ni pile, donc ils se verifient sans
 * demarrer le jeu. Le reste (tag NBT, barre, transfert par le noeud) est fige par
 * les GameTests.
 */
class EnergyItemsTest {

    @Test
    void laBarreSuitLeRemplissage() {
        assertEquals(0, EnergyItems.barWidth(0.0d, 10_000.0d));
        assertEquals(EnergyItems.BAR_STEPS, EnergyItems.barWidth(10_000.0d, 10_000.0d));
        assertEquals((EnergyItems.BAR_STEPS + 1) / 2,
                EnergyItems.barWidth(5_000.0d, 10_000.0d));
        // Une charge hors bornes ne deborde pas de la barre.
        assertEquals(0, EnergyItems.barWidth(-5.0d, 10_000.0d));
        assertEquals(EnergyItems.BAR_STEPS, EnergyItems.barWidth(20_000.0d, 10_000.0d));
    }

    @Test
    void uneCapaciteNulleNeMontreAucuneBarre() {
        assertEquals(0, EnergyItems.barWidth(100.0d, 0.0d));
    }

    @Test
    void laBarreDisparaitAPleineCharge() {
        // L'original : la barre etait la durabilite, et des degats nuls cachent la barre.
        assertTrue(EnergyItems.hasBar(0.0d, 10_000.0d), "a vide, la barre se voit");
        assertTrue(EnergyItems.hasBar(9_999.0d, 10_000.0d), "presque plein, encore");
        assertFalse(EnergyItems.hasBar(10_000.0d, 10_000.0d), "plein, plus de barre");
        assertFalse(EnergyItems.hasBar(0.0d, 0.0d), "pas de capacite, pas de barre");
    }

    @Test
    void laBarreVaDuRougeAuVert() {
        int empty = EnergyItems.barColor(0.0d);
        int full = EnergyItems.barColor(1.0d);

        // Rouge : composante rouge dominante, verte nulle.
        assertEquals(0xFF, (empty >> 16) & 0xFF, "une barre vide est rouge");
        assertEquals(0x00, (empty >> 8) & 0xFF, "une barre vide n'a pas de vert");

        // Vert : composante verte dominante, rouge nulle.
        assertEquals(0x00, (full >> 16) & 0xFF, "une barre pleine n'a pas de rouge");
        assertEquals(0xFF, (full >> 8) & 0xFF, "une barre pleine est verte");
        assertTrue(full != empty, "les deux bouts doivent differer");
    }
}
