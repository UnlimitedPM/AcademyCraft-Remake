package cn.academy.terminal.tutorial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TutorialOpeningTest {
    @Test
    void lesLogosApparaissentDansLOrdreDeLOriginal() {
        double[] starts = { 1.75, 1.3, 0.65, 0.1 };
        for (int index = 0; index < starts.length; index++) {
            assertEquals(0, TutorialOpening.logoAlpha(index, starts[index]), 1.0e-9);
            assertEquals(0.5, TutorialOpening.logoAlpha(index, starts[index] + 0.15), 1.0e-9);
            assertEquals(1, TutorialOpening.logoAlpha(index, starts[index] + 0.3), 1.0e-9);
        }
        assertEquals(0, TutorialOpening.panelAlpha(1.75), 1.0e-9);
        assertEquals(1, TutorialOpening.panelAlpha(2.05), 1.0e-9);
    }

    @Test
    void leLogoMonteEntreSeptEtOnzeDixiemes() {
        assertEquals(63, TutorialOpening.logoY(0), 1.0e-9);
        assertEquals(63, TutorialOpening.logoY(0.7), 1.0e-9);
        assertEquals(13.5, TutorialOpening.logoY(0.9), 1.0e-9);
        assertEquals(-36, TutorialOpening.logoY(1.1), 1.0e-9);
        assertEquals(-36, TutorialOpening.logoY(10), 1.0e-9);
    }

    @Test
    void lesTraitsPoussentPuisLiberentLeCentre() {
        assertEquals(0, TutorialOpening.lineEnd(0.4), 1.0e-9);
        assertEquals(250, TutorialOpening.lineEnd(0.55), 1.0e-9);
        assertEquals(500, TutorialOpening.lineEnd(0.7), 1.0e-9);
        assertEquals(50, TutorialOpening.lineStart(0.7), 1.0e-9);
        assertEquals(125, TutorialOpening.lineStart(0.8), 1.0e-9);
        assertEquals(200, TutorialOpening.lineStart(0.9), 1.0e-9);
    }

    @Test
    void laListeAttendLaFinDeLIntroduction() {
        assertFalse(TutorialOpening.listVisible(0));
        assertFalse(TutorialOpening.listVisible(2.4));
        assertTrue(TutorialOpening.listVisible(2.401));
    }

    @Test
    void lesLogosSEffacentEnTroisDixiemesAuPremierChoix() {
        assertEquals(1, TutorialOpening.closingAlpha(0), 1.0e-9);
        assertEquals(0.5, TutorialOpening.closingAlpha(0.15), 1.0e-9);
        assertEquals(0, TutorialOpening.closingAlpha(0.3), 1.0e-9);
        assertEquals(0, TutorialOpening.closingAlpha(10), 1.0e-9);
    }
}