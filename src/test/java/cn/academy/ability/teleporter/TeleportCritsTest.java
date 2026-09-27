package cn.academy.ability.teleporter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Les coups critiques des teleportations, relus sans Minecraft.
 *
 * <p>Tout le calcul est pur et prend les tirages en parametre, ce qui permet de figer un
 * critique : un tirage au hasard ne se verifie pas, alors que « ce tirage-la donne ce palier »
 * se verifie exactement.
 */
class TeleportCritsTest {

    @Test
    @DisplayName("sans les deux passifs, aucun palier n'a de chance")
    void sansLesPassifsAucuneChance() {
        // -1 : la competence n'est pas apprise. C'est la convention de l'original, et c'est ce
        // qui fait que Dimension Folding seul ne suffit pas pour les deux gros paliers.
        assertEquals(0f, TeleportCrits.chance(0, -1f, -1f), 0.0001f);
        assertEquals(0f, TeleportCrits.chance(1, -1f, -1f), 0.0001f);
        assertEquals(0f, TeleportCrits.chance(2, -1f, -1f), 0.0001f);
    }

    @Test
    @DisplayName("la chance monte avec l'experience des deux passifs")
    void laChanceMonteAvecLExperience() {
        // Palier 0 : les deux comptent. Au depart, 0,10 + 0,18 = 0,28 ; au maximum, 0,45.
        assertEquals(0.28f, TeleportCrits.chance(0, 0f, 0f), 0.0001f);
        assertEquals(0.45f, TeleportCrits.chance(0, 1f, 1f), 0.0001f);
        // Les paliers hauts ne dependent que de Space Fluctuation.
        assertEquals(0.10f, TeleportCrits.chance(1, 1f, 0f), 0.0001f);
        assertEquals(0.15f, TeleportCrits.chance(1, 1f, 1f), 0.0001f);
        assertEquals(0.03f, TeleportCrits.chance(2, 1f, 1f), 0.0001f);
    }

    @Test
    @DisplayName("le palier est le premier tirage qui passe")
    void lePalierEstLePremierTirageQuiPasse() {
        // Un tirage nul passe le palier vise, un tirage plein le rate : chaque palier se fige
        // donc separement. Les paliers hauts ne dependent que de Space Fluctuation, et il est
        // ici au maximum (Dimension Folding n'est pas appris).
        assertEquals(0, TeleportCrits.tier(-1f, 1f, new float[] {0f, 1f, 1f}));
        assertEquals(1, TeleportCrits.tier(-1f, 1f, new float[] {1f, 0f, 1f}));
        assertEquals(2, TeleportCrits.tier(-1f, 1f, new float[] {1f, 1f, 0f}));
        assertEquals(-1, TeleportCrits.tier(-1f, 1f, new float[] {1f, 1f, 1f}));
    }

    @Test
    @DisplayName("un critique multiplie les degats du palier")
    void unCritiqueMultiplieLesDegats() {
        assertEquals(10f, TeleportCrits.damage(10f, -1), 0.0001f);
        assertEquals(13f, TeleportCrits.damage(10f, 0), 0.0001f);
        assertEquals(16f, TeleportCrits.damage(10f, 1), 0.0001f);
        assertEquals(26f, TeleportCrits.damage(10f, 2), 0.0001f);
    }

    @Test
    @DisplayName("un palier inconnu se refuse au lieu de rendre zero")
    void unPalierInconnuSeRefuse() {
        assertThrows(IllegalArgumentException.class, () -> TeleportCrits.chance(3, 1f, 1f));
    }

    @Test
    @DisplayName("l'experience des critiques est celle de l'original")
    void lExperienceDesCritiquesEstCelleDeLOriginal() {
        // 0,005 par palier pour Dimension Folding, et 0,0001 pour Space Fluctuation, quelle que
        // soit la force du coup.
        assertEquals(0.005f, TeleportCrits.expForDimFolding(0), 0.000001f);
        assertEquals(0.010f, TeleportCrits.expForDimFolding(1), 0.000001f);
        assertEquals(0.015f, TeleportCrits.expForDimFolding(2), 0.000001f);
        assertEquals(0.0001f, TeleportCrits.expForSpaceFluct(), 0.0000001f);
    }
}
