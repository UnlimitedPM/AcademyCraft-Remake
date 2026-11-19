package cn.academy.ability;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L'experience des competences, et la progression de niveau qu'elle alimente.
 *
 * C'est la regle qui fait qu'on monte en <b>utilisant</b> ses competences : remplir
 * le palier du niveau en cours demande d'en gagner. Le palier lui-meme est le
 * nombre de competences utilisables du niveau, multiplie par 0,666 — par 1,333 au
 * niveau 4, le dernier avant le maximum.
 */
class AbilityExpTest {

    private static final class TestCategory extends Category {
        TestCategory(String name) {
            super(name);
        }

        void add(Skill skill) {
            addSkill(skill);
        }
    }

    private static class DummySkill extends Skill {
        DummySkill(String name, int level) {
            super(name, level);
        }
    }

    @Test
    void lExperienceMonteEtSePlaFonneAUn() {
        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("skill", 1);
        category.add(skill);
        AbilityData data = new AbilityData();
        data.setCategoryLevel(category, 1);

        data.addSkillExp(skill, 0.25f);
        assertEquals(0.25f, data.getSkillExp(skill), 0.0001f);

        data.addSkillExp(skill, 5f);
        assertEquals(1f, data.getSkillExp(skill), 0.0001f, "l'experience d'une competence plafonne a 1");
    }

    @Test
    void uneCompetenceSatureeContinueDeFaireProgresserLeNiveau() {
        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("skill", 1);
        category.add(skill);
        AbilityData data = new AbilityData();
        data.setCategoryLevel(category, 1);

        // Detail de l'original : l'experience de la competence est bornee, mais
        // l'avancement du niveau recoit le montant complet. Sans cela, une competence
        // saturee ne ferait plus rien et le niveau deviendrait inatteignable.
        data.addSkillExp(skill, 0.5f);
        float afterFirst = data.getRawLevelProgress(category);
        data.addSkillExp(skill, 0.5f);

        assertEquals(1f, data.getSkillExp(skill), 0.0001f);
        assertTrue(data.getRawLevelProgress(category) > afterFirst,
                "l'avancement du niveau doit continuer de grandir");
    }

    @Test
    void apprendreParUsageMarqueLaCompetenceCommeApprise() {
        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("skill", 1);
        category.add(skill);
        AbilityData data = new AbilityData();

        assertFalse(data.isSkillLearned(skill), "on part de rien");
        data.addSkillExp(skill, 0.1f);

        // C'est une ceinture de securite de l'original, conservee : en pratique la
        // competence est deja apprise, puisque l'activation l'exige.
        assertTrue(data.isSkillLearned(skill));
    }

    @Test
    void lePalierEstLeNombreDeCompetencesUtilisablesDuNiveau() {
        TestCategory category = new TestCategory("test");
        category.add(new DummySkill("une", 1));
        category.add(new DummySkill("deux", 1));
        category.add(new DummySkill("trois", 2));
        AbilityData data = new AbilityData();
        data.setCategoryLevel(category, 1);

        // Deux competences de niveau 1 : le palier vaut 2 * 0,666.
        data.addSkillExp(category.getSkill("une"), 2f * 0.666f);

        assertEquals(1f, data.getLevelProgress(category), 0.0001f);
        assertTrue(data.canLevelUp(category));

        // Au niveau 2, il n'y a plus qu'une competence : le palier est plus court.
        data.setCategoryLevel(category, 2);
        assertEquals(0f, data.getLevelProgress(category), 0.0001f,
                "changer de niveau remet l'avancement a zero");
    }

    @Test
    void uneCompetencePassiveNeComptePasDansLePalier() {
        TestCategory category = new TestCategory("test");
        DummySkill passive = new DummySkill("passive", 1) {
            @Override
            public boolean isPassive() {
                return true;
            }
        };
        category.add(passive);
        AbilityData data = new AbilityData();
        data.setCategoryLevel(category, 1);

        // Seule une competence passive a ce niveau : personne ne peut la declencher,
        // donc il n'y a rien a remplir et le niveau passe.
        assertEquals(1f, data.getLevelProgress(category), 0.0001f);
        assertTrue(data.canLevelUp(category));
    }

    @Test
    void leDernierNiveauCouteDeuxFoisPlusCher() {
        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("skill", 4);
        category.add(skill);
        AbilityData data = new AbilityData();
        data.setCategoryLevel(category, 4);

        data.addSkillExp(skill, 0.666f);
        assertTrue(data.getLevelProgress(category) < 1f,
                "au niveau 4 le palier est majore : 0,666 n'y suffit plus");

        data.addSkillExp(skill, 0.667f);
        assertEquals(1f, data.getLevelProgress(category), 0.0001f);
    }

    @Test
    void auNiveauMaximalOnNeMontePlus() {
        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("skill", 5);
        category.add(skill);
        AbilityData data = new AbilityData();
        data.setCategoryLevel(category, 5);
        data.maxOutLevelProgress(category);

        assertEquals(1f, data.getLevelProgress(category), 0.0001f);
        assertFalse(data.canLevelUp(category), "le niveau 5 est le maximum");
    }

    @Test
    void unNiveauSansCompetenceUtilisablePasseDUnCoup() {
        TestCategory category = new TestCategory("test");
        category.add(new DummySkill("basse", 1));
        AbilityData data = new AbilityData();
        data.setCategoryLevel(category, 3);

        // C'est le cas, dans le port, des niveaux dont les competences ne sont pas
        // encore portees : sans ce comportement, elles seraient infranchissables.
        assertEquals(1f, data.getLevelProgress(category), 0.0001f);
        assertTrue(data.canLevelUp(category));
    }

    @Test
    void lExperienceDuneCategorieNonAppriseResteAZero() {
        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("skill", 1);
        category.add(skill);
        AbilityData data = new AbilityData();
        data.addSkillExp(skill, 0.5f);
        data.setCategoryLevel(category, 0);

        assertEquals(0f, data.getSkillExp(skill), 0.0001f,
                "sans la categorie, l'experience ne veut rien dire");
    }

    @Test
    void leMultiplicateurDeLaCompetenceSApplique() {
        TestCategory category = new TestCategory("test");
        DummySkill slow = new DummySkill("lente", 1) {
            @Override
            public float getExpIncrSpeed() {
                return 0.5f;
            }
        };
        category.add(slow);
        AbilityData data = new AbilityData();
        data.setCategoryLevel(category, 1);

        data.addSkillExp(slow, 0.4f);

        assertEquals(0.2f, data.getSkillExp(slow), 0.0001f);
        assertEquals(0.2f, data.getRawLevelProgress(category), 0.0001f,
                "le multiplicateur vaut aussi pour l'avancement du niveau");
    }

    @Test
    void unGainNulOuNegatifNeChangeRien() {
        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("skill", 1);
        category.add(skill);
        AbilityData data = new AbilityData();
        data.setCategoryLevel(category, 1);

        data.addSkillExp(skill, 0f);
        data.addSkillExp(skill, -1f);
        data.addSkillExp(null, 1f);

        assertEquals(0f, data.getSkillExp(skill), 0.0001f);
        assertEquals(0f, data.getRawLevelProgress(category), 0.0001f);
    }

    @Test
    void laSauvegardeConserveExperienceEtAvancement() {
        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("skill", 1);
        category.add(skill);
        AbilityData data = new AbilityData();
        data.setCategoryLevel(category, 1);
        data.addSkillExp(skill, 0.3f);
        float progress = data.getRawLevelProgress(category);

        AbilityData reloaded = new AbilityData();
        reloaded.deserializeNBT(data.serializeNBT());

        assertEquals(0.3f, reloaded.getSkillExp(skill), 0.0001f);
        assertEquals(progress, reloaded.getRawLevelProgress(category), 0.0001f);
    }

    @Test
    void uneSauvegardeVideNeDonneNiExperienceNiAvancement() {
        AbilityData data = new AbilityData();
        data.deserializeNBT(new CompoundTag());

        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("skill", 1);
        category.add(skill);

        assertEquals(0f, data.getSkillExp(skill), 0.0001f);
        assertEquals(0f, data.getRawLevelProgress(category), 0.0001f);
    }

    @Test
    void lAvancementNeDebordePasDuneCategorieSurLAutre() {
        TestCategory first = new TestCategory("premiere");
        TestCategory second = new TestCategory("seconde");
        DummySkill skill = new DummySkill("skill", 1);
        first.add(skill);
        second.add(new DummySkill("autre", 1));
        AbilityData data = new AbilityData();
        data.setCategoryLevel(first, 1);
        data.setCategoryLevel(second, 1);

        data.addSkillExp(skill, 0.5f);

        assertEquals(0.5f, data.getRawLevelProgress(first), 0.0001f);
        assertEquals(0f, data.getRawLevelProgress(second), 0.0001f,
                "gagner de l'experience dans une categorie ne doit pas monter les autres");
    }
}
