package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le passif de radiation : son facteur, et l'experience qu'il tire de la reserve.
 *
 * <p>Les deux choses qui pourraient se casser en silence : le facteur (1,4 a 1,8) et le fait que
 * son experience ne s'apprend pas mais se calcule — c'est le {@code expCustomized} de l'original.
 */
class RadiationIntensifyTest {

    private static final RadiationIntensifySkill RAD = MeltdownerCategory.RADIATION_INTENSIFY;

    private static AbilityData dataAt(int level) {
        AbilityData data = new AbilityData();
        data.setCategoryLevel(MeltdownerCategory.INSTANCE, level);
        data.learnSkill(RAD);
        return data;
    }

    @Test
    @DisplayName("le facteur va de 1,4 a 1,8")
    void leFacteurVaDe14A18() {
        // L'experience du passif est la part de reserve debloquee : au niveau 5 la reserve vaut
        // celle du niveau 5, donc l'experience vaut 1 et le facteur est au maximum.
        assertEquals(1.8f, RAD.rate(dataAt(5)), 0.0001f);
        // Au niveau 1, la reserve part de 1800 sur les 8000 du niveau 5 : un quart du chemin.
        assertEquals(1.4f + 0.225f * 0.4f, RAD.rate(dataAt(1)), 0.0001f);
    }

    @Test
    @DisplayName("son experience se calcule au lieu de s'apprendre")
    void sonExperienceSeCalcule() {
        AbilityData data = dataAt(1);

        assertTrue(RAD.hasComputedExp());
        // Le port n'enregistre aucune experience pour lui : la valeur vient du plafond de
        // reserve du joueur, ajouts et cursus compris.
        assertEquals(1800f / 8000f, RAD.computeExp(data), 0.0001f);
        assertEquals(RAD.computeExp(data), data.getSkillExp(RAD), 0.0001f,
                "et c'est bien elle que la donnee rend");
    }

    @Test
    @DisplayName("c'est un passif, et il ne se range pas sur une touche")
    void cestUnPassif() {
        assertTrue(RAD.isPassive());
        assertTrue(!RAD.canControl(), "une touche ne pourrait rien lancer");
    }
}
