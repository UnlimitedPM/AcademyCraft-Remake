package cn.academy.ability.develop;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.condition.ConditionDependency;
import cn.academy.ability.develop.condition.ConditionDeveloperType;
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

    /**
     * La meilleure machine du jeu.
     *
     * Les fixtures de ce fichier ne testent pas la qualite de la machine : elles
     * s'interessent au niveau, aux dependances et au palier. Elles prennent donc la
     * machine avancee, celle qui sait tout enseigner — et un test separe s'occupe de la
     * qualite elle-meme.
     */
    private static final DeveloperType ANY_MACHINE = DeveloperType.ADVANCED;

    private static boolean canLearn(AbilityData data, Skill skill) {
        return LearningHelper.canLearn(data, skill, ANY_MACHINE);
    }

    private static cn.academy.ability.develop.condition.LearningCondition firstBlocker(
            AbilityData data, Skill skill) {
        return LearningHelper.firstBlocker(data, skill, ANY_MACHINE);
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

        assertFalse(canLearn(fixture.data, skill), "niveau 0 pour une competence de niveau 3");
        fixture.data.setCategoryLevel(fixture.category, 2);
        assertFalse(canLearn(fixture.data, skill), "niveau 2, il en manque un");
        fixture.data.setCategoryLevel(fixture.category, 3);
        assertTrue(canLearn(fixture.data, skill), "le niveau atteint ouvre la competence");
    }

    @Test
    void leBlocageEstCeluiDuNiveau() {
        DummySkill skill = new DummySkill("trois", 3);
        Fixture fixture = new Fixture(skill);

        // La premiere condition de toute competence est celle du niveau, comme dans
        // l'original ou le constructeur de Skill la posait.
        assertInstanceOf(ConditionLevel.class, firstBlocker(fixture.data, skill));

        fixture.data.setCategoryLevel(fixture.category, 3);
        assertNull(firstBlocker(fixture.data, skill), "plus rien ne bloque");
    }

    @Test
    void uneDependanceDoitEtreAppriseAvant() {
        DummySkill parent = new DummySkill("parent", 1);
        DummySkill child = new DummySkill("enfant", 1);
        child.addDependency(parent);
        Fixture fixture = new Fixture(parent, child);
        fixture.data.setCategoryLevel(fixture.category, 1);

        assertFalse(canLearn(fixture.data, child), "la dependance n'est pas apprise");

        fixture.data.learnSkill(parent);
        assertTrue(canLearn(fixture.data, child), "une fois la dependance apprise, c'est ouvert");
    }

    @Test
    void leBlocageNommeLaDependanceManquante() {
        DummySkill parent = new DummySkill("parent", 1);
        DummySkill child = new DummySkill("enfant", 1);
        child.addDependency(parent);
        Fixture fixture = new Fixture(parent, child);
        fixture.data.setCategoryLevel(fixture.category, 1);

        var blocker = firstBlocker(fixture.data, child);

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
        assertFalse(canLearn(fixture.data, child));
    }

    @Test
    void toutesLesConditionsDoiventPasser() {
        DummySkill parent = new DummySkill("parent", 2);
        DummySkill child = new DummySkill("enfant", 3);
        child.addDependency(parent);
        Fixture fixture = new Fixture(parent, child);

        fixture.data.setCategoryLevel(fixture.category, 3);
        assertFalse(canLearn(fixture.data, child), "le niveau suffit, pas la dependance");

        fixture.data.setCategoryLevel(fixture.category, 1);
        fixture.data.learnSkill(parent);
        assertFalse(canLearn(fixture.data, child), "la dependance suffit, pas le niveau");

        fixture.data.setCategoryLevel(fixture.category, 3);
        assertTrue(canLearn(fixture.data, child), "les deux ensemble");
    }

    @Test
    void uneCompetenceDeNiveauZeroEstToujoursAccessible() {
        DummySkill skill = new DummySkill("gratuite", 0);
        Fixture fixture = new Fixture(skill);

        assertTrue(canLearn(fixture.data, skill));
    }

    /**
     * La qualite de la machine : elle seule, sans le niveau ni les dependances.
     *
     * L'original deduisait la machine du <b>niveau</b> de la competence, et une machine
     * plus avancee sait toujours ce que sait une machine plus modeste. Ce test prend donc
     * une competence et fait varier la machine, en laissant tout le reste satisfait :
     * c'est le seul moyen de voir la difference.
     */
    @Test
    void laQualiteDeLaMachineDecideElleAussi() {
        DummySkill basse = new DummySkill("basse", 2);
        DummySkill haute = new DummySkill("haute", 4);
        Fixture fixture = new Fixture(basse, haute);
        fixture.data.setCategoryLevel(fixture.category, 4);

        // Niveaux 1 et 2 : le portable suffit, et c'est tout ce qu'il sait faire.
        assertTrue(canLearn(fixture.data, basse, DeveloperType.PORTABLE));
        assertTrue(canLearn(fixture.data, basse, DeveloperType.NORMAL));
        assertTrue(canLearn(fixture.data, basse, DeveloperType.ADVANCED));

        // Niveau 4 : la machine normale ne suffit plus.
        assertFalse(canLearn(fixture.data, haute, DeveloperType.PORTABLE));
        assertFalse(canLearn(fixture.data, haute, DeveloperType.NORMAL));
        assertTrue(canLearn(fixture.data, haute, DeveloperType.ADVANCED));

        // Et le refus est nomme : l'ecran doit pouvoir dire quelle machine il faut.
        var blocker = firstBlocker(fixture.data, haute, DeveloperType.NORMAL);
        assertInstanceOf(ConditionDeveloperType.class, blocker);
        assertEquals(DeveloperType.ADVANCED, ((ConditionDeveloperType) blocker).getRequired());
    }

    private static boolean canLearn(AbilityData data, Skill skill, DeveloperType developer) {
        return LearningHelper.canLearn(data, skill, developer);
    }

    private static cn.academy.ability.develop.condition.LearningCondition firstBlocker(
            AbilityData data, Skill skill, DeveloperType developer) {
        return LearningHelper.firstBlocker(data, skill, developer);
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
