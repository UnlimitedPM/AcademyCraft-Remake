package cn.academy.ability.develop;

import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.condition.ConditionDependency;
import cn.academy.ability.develop.condition.ConditionLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ce que porte une competence : son niveau, son prix, et ses liens.
 *
 * La formule du prix est reprise de l'original et vaut la peine d'etre fichee :
 * {@code 3 + niveau²/2}, arrondi a l'entier inferieur. Une competence de niveau 5
 * coute donc cinq fois plus de stimulations qu'une competence de niveau 1.
 */
class SkillMetadataTest {

    private static final class TestCategory extends Category {
        TestCategory(String name) {
            super(name);
        }

        void add(Skill skill) {
            addSkill(skill);
        }
    }

    private static final class DummySkill extends Skill {
        DummySkill(String name, int level) {
            super(name, level);
        }
    }

    @Test
    void lePrixSuitLeCarreDuNiveau() {
        TestCategory category = new TestCategory("test");
        int[] expected = {3, 3, 5, 7, 11, 15};

        for (int level = 0; level <= 5; level++) {
            DummySkill skill = new DummySkill("niv" + level, level);
            category.add(skill);
            assertEquals(expected[level], skill.getLearningStims(),
                    "stimulations pour une competence de niveau " + level);
        }
    }

    @Test
    void leNiveauEstLisible() {
        DummySkill skill = new DummySkill("trois", 3);

        assertEquals(3, skill.getLevel());
    }

    @Test
    void uneCompetenceSansNiveauVautZero() {
        // Reserve aux doublures de test : une vraie competence declare son niveau,
        // et un test separe verifie qu'aucune competence livree n'est restee a zero.
        DummySkill skill = new DummySkill("sansNiveau", 0);

        assertEquals(0, skill.getLevel());
    }

    @Test
    void laConditionDeNiveauEstToujoursPosee() {
        DummySkill skill = new DummySkill("nimporte", 2);

        assertEquals(1, skill.getConditions().size());
        assertTrue(skill.getConditions().get(0) instanceof ConditionLevel);
    }

    @Test
    void laCleDeLangueSuitLeNomDeLaCategorie() {
        TestCategory category = new TestCategory("electromaster");
        DummySkill skill = new DummySkill("arc_gen", 1);
        category.add(skill);

        assertEquals("ac.ability.electromaster.arc_gen.name", skill.getDisplayKey());
    }

    @Test
    void uneDependancePoseAussiSaCondition() {
        TestCategory category = new TestCategory("test");
        DummySkill parent = new DummySkill("parent", 1);
        DummySkill child = new DummySkill("enfant", 1);
        category.add(parent);
        category.add(child);

        child.addDependency(parent);

        assertEquals(1, child.getDependencies().size());
        assertSame(parent, child.getDependencies().get(0));
        assertEquals(2, child.getConditions().size(), "la condition de niveau, plus la dependance");
        assertTrue(child.getConditions().get(1) instanceof ConditionDependency);
    }

    @Test
    void uneCompetenceNePeutPasDependreDEelleMeme() {
        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("seule", 1);
        category.add(skill);

        // La boucle rendrait la competence inapprenable sans rien dire : mieux vaut
        // le refus au chargement qu'un mystere en jeu.
        assertThrows(IllegalArgumentException.class, () -> skill.addDependency(skill));
    }

    @Test
    void unParentNeSePoseQuUneFois() {
        TestCategory category = new TestCategory("test");
        DummySkill first = new DummySkill("premier", 1);
        DummySkill second = new DummySkill("second", 1);
        DummySkill child = new DummySkill("enfant", 1);
        category.add(first);
        category.add(second);
        category.add(child);

        child.setParent(first);

        assertSame(first, child.getParent());
        assertFalse(child.isRoot());
        assertThrows(IllegalStateException.class, () -> child.setParent(second));
    }

    @Test
    void uneCompetenceSansParentEstUneRacine() {
        DummySkill skill = new DummySkill("racine", 1);

        assertTrue(skill.isRoot());
    }
}
