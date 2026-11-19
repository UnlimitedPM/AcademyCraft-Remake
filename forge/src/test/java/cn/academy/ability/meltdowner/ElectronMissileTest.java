package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les courbes du missile a electrons, relues sans Minecraft.
 *
 * <p>C'est la derniere competence du meltdowner, et la plus longue a tenir : ses couts
 * d'entretien, son tir, sa portee, sa duree et sa recharge sont autant de nombres qui viennent
 * de l'original, et qui se figent ici.
 */
class ElectronMissileTest {

    private static final ElectronMissileSkill MISSILE = MeltdownerCategory.ELECTRON_MISSILE;

    private static AbilityData dataAt(float exp) {
        AbilityData data = new AbilityData();
        data.setCategoryLevel(MeltdownerCategory.INSTANCE, 5);
        data.learnSkill(MISSILE);
        data.setSkillExp(MISSILE, exp);
        return data;
    }

    @Test
    @DisplayName("les couts sont ceux de l'original")
    void lesCoutsSontCeuxDeLOriginal() {
        // L'entretien se paie en CP par tick, de 12 a 5.
        assertEquals(12f, MISSILE.upkeep(dataAt(0f)), 0.0001f);
        assertEquals(5f, MISSILE.upkeep(dataAt(1f)), 0.0001f);
        // Le tir, en CP (60 a 25) et en surcout (9 a 4).
        assertEquals(60f, MISSILE.shotCost(dataAt(0f)), 0.0001f);
        assertEquals(25f, MISSILE.shotCost(dataAt(1f)), 0.0001f);
        assertEquals(9f, MISSILE.shotOverload(dataAt(0f)), 0.0001f);
        assertEquals(4f, MISSILE.shotOverload(dataAt(1f)), 0.0001f);
        // Et l'ouverture, qui epingle 200 de surcout sans demander un seul CP.
        assertEquals(200f, MISSILE.getOverloadCost(dataAt(0f)), 0.0001f);
        assertEquals(0f, MISSILE.getCpCost(), 0.0001f);
    }

    @Test
    @DisplayName("les courbes sont celles de l'original")
    void lesCourbesSontCellesDeLOriginal() {
        assertEquals(10f, MISSILE.damage(dataAt(0f)), 0.0001f);
        assertEquals(18f, MISSILE.damage(dataAt(1f)), 0.0001f);
        assertEquals(5f, MISSILE.range(dataAt(0f)), 0.0001f);
        assertEquals(13f, MISSILE.range(dataAt(1f)), 0.0001f);
        // Deux a dix secondes de maintien selon l'experience...
        assertEquals(80, MISSILE.getMaxHoldTicks(dataAt(0f)));
        assertEquals(200, MISSILE.getMaxHoldTicks(dataAt(1f)));
        // ... et une recharge de 35 a 20 secondes a la fin.
        assertEquals(700, MISSILE.getCooldownTicks(dataAt(0f)));
        assertEquals(400, MISSILE.getCooldownTicks(dataAt(1f)));
    }

    @Test
    @DisplayName("c'est un maintien qui verse son experience lui-meme")
    void cestUnMaintien() {
        assertTrue(MISSILE.isHeld());
        // Le tir se paie et verse son experience depuis l'effet, qui seul sait s'il a touche :
        // le paquet d'activation ne peut donc rien declarer a sa place.
        assertTrue(MISSILE.earnsExpOnEffect());
        assertEquals(0f, MISSILE.getExpGain(dataAt(0f)), 0.0001f);
        // Et il ne peut pas en accumuler plus de cinq.
        assertEquals(5, ElectronMissileSkill.MAX_BALLS);
    }

    @Test
    @DisplayName("les billes posees vivent tout le maintien possible")
    void lesBillesViventToutLeMaintien() {
        // L'original ne donnait aucune duree aux siennes et les tuait en partant : la duree du
        // port n'est qu'un filet, et il doit couvrir le maintien le plus long — deux cents ticks
        // a pleine experience — plus le tick ou la competence pourrait les tuer.
        assertTrue(ElectronMissileSkill.BALL_LIFE_TICKS > MISSILE.getMaxHoldTicks(dataAt(1f)),
                "une bille doit survivre au plus long des maintiens : "
                        + ElectronMissileSkill.BALL_LIFE_TICKS + " pour "
                        + MISSILE.getMaxHoldTicks(dataAt(1f)) + " ticks de maintien");
        assertEquals(200, MISSILE.getMaxHoldTicks(dataAt(1f)), "le plus long des maintiens");
    }
}
