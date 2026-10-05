package cn.academy.ability.vecmanip;

import cn.academy.ability.Skill;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les competences qui s'ACTIVENT et se DESACTIVENT, plutot que de se tenir.
 *
 * <p>C'est la quatrieme famille de l'original, et elle n'est pas devinable : elle vient de ce que
 * son gestionnaire d'activation faisait de sa touche. Trois competences de vecmanip en sont — la
 * deviation, le renvoi et les ailes de tempete —, et le joueur a demande la meme chose que chez lui
 * apres les avoir tenues pour rien.
 *
 * <p>Ce test fige la liste, comme les autres tests de competences figent les niveaux et les
 * dependances : une competence qui basculerait par erreur obligerait a tenir sa touche pour rien,
 * ou s'arreterait a chaque relachement.
 */
class ToggleSkillsTest {

    @Test
    @DisplayName("les trois bascules de vecmanip en sont, et se tiennent")
    void lesTroisBasculesDeVecmanip() {
        for (Skill skill : List.of(VecmanipCategory.VEC_DEVIATION, VecmanipCategory.VEC_REFLECTION,
                VecmanipCategory.STORM_WING)) {
            assertTrue(skill.isToggle(), skill.getName() + " se bascule");
            assertTrue(skill.isHeld(), "et c'est un maintien : " + skill.getName());
            assertFalse(skill.isChargeable(), "mais pas une charge : " + skill.getName());
        }
    }

    @Test
    @DisplayName("les autres maintiens se tiennent a la touche")
    void lesAutresMaintiensNeBasculentPas() {
        for (Skill skill : List.of(VecmanipCategory.PLASMA_CANNON,
                cn.academy.ability.meltdowner.MeltdownerCategory.LIGHT_SHIELD,
                cn.academy.ability.electromaster.ElectromasterCategory.MAG_MOVEMENT,
                cn.academy.ability.electromaster.ElectromasterCategory.MAG_MANIP,
                cn.academy.ability.electromaster.ElectromasterCategory.CHARGING,
                cn.academy.ability.meltdowner.MeltdownerCategory.JET_ENGINE)) {
            assertTrue(skill.isHeld(), skill.getName() + " se tient");
            assertFalse(skill.isToggle(), "et ne se bascule pas : " + skill.getName());
        }
    }

    @Test
    @DisplayName("une charge ne se bascule pas non plus")
    void uneChargeNeSeBasculePas() {
        for (Skill skill : List.of(VecmanipCategory.DIRECTED_SHOCK, VecmanipCategory.GROUNDSHOCK,
                VecmanipCategory.DIRECTED_BLASTWAVE, VecmanipCategory.VEC_ACCEL)) {
            assertTrue(skill.isChargeable(), skill.getName() + " se charge");
            assertFalse(skill.isToggle(), "et ne se bascule pas : " + skill.getName());
        }
    }
}
