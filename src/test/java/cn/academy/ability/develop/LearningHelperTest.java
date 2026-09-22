package cn.academy.ability.develop;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.condition.ConditionDependency;
import cn.academy.ability.develop.condition.ConditionLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les regles d'apprentissage d'une competence.
 *
 * Tout est pur : une categorie de test, des competences de test, un etat de joueur.
 * Aucune de ces regles n'a besoin d'un jeu demarre, ce qui est justement l'interet
 * de les avoir sorties de la machine.
 */
class LearningHelperTest {

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

    private static final class Fixture {
        final TestCategory category = new TestCategory("test");
        final AbilityData data = new AbilityData();

        Fixture(Skill... skills) {
            for (Skill skill : skills) {
                category.add(skill);
            }
        }
    }

    @Test
    void leNiveauDeLaCategorieOuvreOuFermeLaCompetence() {
        DummySkill skill = new DummySkill("trois", 3);
        Fixture fixture = new Fixture(skill);

        assertFalse(LearningHelper.canLearn(fixture.data, skill), "niveau 0 pour une competence de niveau 3");
        fixture.data.setCategoryLevel(fixture.category, 2);
        assertFalse(LearningHelper.canLearn(fixture.data, skill), "niveau 2, il en manque un");
        fixture.data.setCategoryLevel(fixture.category, 3);
        assertTrue(LearningHelper.canLearn(fixture.data, skill), "le niveau atteint ouvre la competence");
    }

    @Test
    void leBlocageEstCeluiDuNiveau() {
        DummySkill skill = new DummySkill("trois", 3);
        Fixture fixture = new Fixture(skill);

        // La premiere condition de toute competence est celle du niveau, comme dans
        // l'original ou le constructeur de Skill la posait.
        assertInstanceOf(ConditionLevel.class, LearningHelper.firstBlocker(fixture.data, skill));

        fixture.data.setCategoryLevel(fixture.category, 3);
        assertNull(LearningHelper.firstBlocker(fixture.data, skill), "plus rien ne bloque");
    }

    @Test
    void uneDependanceDoitEtreAppriseAvant() {
        DummySkill parent = new DummySkill("parent", 1);
        DummySkill child = new DummySkill("enfant", 1);
        child.addDependency(parent);
        Fixture fixture = new Fixture(parent, child);
        fixture.data.setCategoryLevel(fixture.category, 1);

        assertFalse(LearningHelper.canLearn(fixture.data, child), "la dependance n'est pas apprise");

        fixture.data.learnSkill(parent);
        assertTrue(LearningHelper.canLearn(fixture.data, child), "une fois la dependance apprise, c'est ouvert");
    }

    @Test
    void leBlocageNommeLaDependanceManquante() {
        DummySkill parent = new DummySkill("parent", 1);
        DummySkill child = new DummySkill("enfant", 1);
        child.addDependency(parent);
        Fixture fixture = new Fixture(parent, child);
        fixture.data.setCategoryLevel(fixture.category, 1);

        var blocker = LearningHelper.firstBlocker(fixture.data, child);

        assertNotNull(blocker);
        assertInstanceOf(ConditionDependency.class, blocker);
        assertEquals(parent, ((ConditionDependency) blocker).getDependency());
    }

    @Test
    void uneDependanceNonAppriseDansUneAutreCategorieNeComptePas() {
        TestCategory other = new TestCategory("autre");
        DummySkill parent = new DummySkill("parent", 1);
        DummySkill sameName = new DummySkill("parent", 1);
        other.add(sameName);

        DummySkill child = new DummySkill("enfant", 1);
        child.addDependency(parent);
        Fixture fixture = new Fixture(parent, child);

        // Apprendre la competence homonyme de l'autre categorie ne doit rien ouvrir :
        // c'est la competence elle-meme qui compte, pas son nom.
        fixture.data.learnSkill(sameName);
        fixture.data.setCategoryLevel(fixture.category, 1);

        assertTrue(fixture.data.isSkillLearned(sameName));
        assertFalse(fixture.data.isSkillLearned(parent));
        assertFalse(LearningHelper.canLearn(fixture.data, child));
    }

    @Test
    void toutesLesConditionsDoiventPasser() {
        DummySkill parent = new DummySkill("parent", 2);
        DummySkill child = new DummySkill("enfant", 3);
        child.addDependency(parent);
        Fixture fixture = new Fixture(parent, child);

        fixture.data.setCategoryLevel(fixture.category, 3);
        assertFalse(LearningHelper.canLearn(fixture.data, child), "le niveau suffit, pas la dependance");

        fixture.data.setCategoryLevel(fixture.category, 1);
        fixture.data.learnSkill(parent);
        assertFalse(LearningHelper.canLearn(fixture.data, child), "la dependance suffit, pas le niveau");

        fixture.data.setCategoryLevel(fixture.category, 3);
        assertTrue(LearningHelper.canLearn(fixture.data, child), "les deux ensemble");
    }

    @Test
    void uneCompetenceDeNiveauZeroEstToujoursAccessible() {
        DummySkill skill = new DummySkill("gratuite", 0);
        Fixture fixture = new Fixture(skill);

        assertTrue(LearningHelper.canLearn(fixture.data, skill));
    }

    @Test
    void uneCompetenceSansParentSeMontreMemeAuNiveauZero() {
        DummySkill root = new DummySkill("racine", 3);
        Fixture fixture = new Fixture(root);

        assertTrue(LearningHelper.canBePotentiallyLearned(fixture.data, root),
                "une racine se montre : c'est ce vers quoi on progresse");
    }

    @Test
    void uneCompetenceSeMontreSiSonParentEstApprise() {
        DummySkill parent = new DummySkill("parent", 1);
        DummySkill child = new DummySkill("enfant", 5);
        child.setParent(parent);
        Fixture fixture = new Fixture(parent, child);

        assertFalse(LearningHelper.canBePotentiallyLearned(fixture.data, child),
                "niveau insuffisant et parent non appris");

        fixture.data.setCategoryLevel(fixture.category, 1);
        fixture.data.learnSkill(parent);

        assertTrue(LearningHelper.canBePotentiallyLearned(fixture.data, child),
                "la suite de la chaine se montre des que le parent est appris");
    }

    @Test
    void uneCompetenceAppriseSeMontreToujours() {
        DummySkill parent = new DummySkill("parent", 1);
        DummySkill child = new DummySkill("apprise", 5);
        child.setParent(parent);
        Fixture fixture = new Fixture(parent, child);

        assertFalse(LearningHelper.canBePotentiallyLearned(fixture.data, child),
                "niveau insuffisant et parent non appris");

        fixture.data.learnSkill(child);

        assertTrue(LearningHelper.canBePotentiallyLearned(fixture.data, child),
                "une competence apprise ne disparait pas de l'ecran");
    }
}
