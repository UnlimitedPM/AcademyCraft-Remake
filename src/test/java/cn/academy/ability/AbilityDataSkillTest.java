package cn.academy.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les competences apprises dans {@link AbilityData}.
 *
 * L'original indexait un {@code BitSet} par l'identifiant de la competence, ce qui
 * supposait une seule categorie par joueur. Le port autorise plusieurs categories,
 * donc la cle porte aussi le nom de la categorie — et c'est ce que verifient les
 * premiers tests, parce que c'est la seule chose qui pouvait se perdre en changeant
 * de representation.
 */
class AbilityDataSkillTest {

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
    void apprendreUneCompetenceLaMarqueCommeApprise() {
        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("skill", 1);
        category.add(skill);
        AbilityData data = new AbilityData();

        assertFalse(data.isSkillLearned(skill));
        assertEquals(0, data.getLearnedSkillCount());

        assertTrue(data.learnSkill(skill), "la premiere fois change l'etat");
        assertTrue(data.isSkillLearned(skill));
        assertEquals(1, data.getLearnedSkillCount());
        assertFalse(data.learnSkill(skill), "la seconde ne change rien");
    }

    @Test
    void oublierUneCompetenceLaRetire() {
        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("skill", 1);
        category.add(skill);
        AbilityData data = new AbilityData();
        data.learnSkill(skill);

        assertTrue(data.forgetSkill(skill));
        assertFalse(data.isSkillLearned(skill));
        assertFalse(data.forgetSkill(skill), "oublier deux fois ne change rien");
    }

    @Test
    void deuxCategoriesPeuventAvoirUneCompetenceDuMemeNom() {
        TestCategory first = new TestCategory("premiere");
        TestCategory second = new TestCategory("seconde");
        DummySkill inFirst = new DummySkill("skill", 1);
        DummySkill inSecond = new DummySkill("skill", 1);
        first.add(inFirst);
        second.add(inSecond);
        AbilityData data = new AbilityData();

        data.learnSkill(inFirst);

        assertTrue(data.isSkillLearned(inFirst));
        assertFalse(data.isSkillLearned(inSecond),
                "meme nom, autre categorie : c'est une autre competence");
    }

    @Test
    void uneCompetenceSansCategorieNEstJamaisApprise() {
        AbilityData data = new AbilityData();
        DummySkill orphan = new DummySkill("orpheline", 1);

        assertFalse(data.isSkillLearned(orphan));
        assertFalse(data.learnSkill(orphan), "rien ne doit pouvoir s'apprendre hors categorie");
        assertEquals(0, data.getLearnedSkillCount());

        assertFalse(data.isSkillLearned(null));
        assertFalse(data.learnSkill(null));
    }

    @Test
    void laListeDesCompetencesApprisesSuitLOrdreDuRegistre() {
        TestCategory category = new TestCategory("test");
        DummySkill first = new DummySkill("un", 1);
        DummySkill second = new DummySkill("deux", 1);
        DummySkill third = new DummySkill("trois", 1);
        category.add(first);
        category.add(second);
        category.add(third);
        AbilityData data = new AbilityData();
        data.learnSkill(third);
        data.learnSkill(first);

        List<Skill> learned = data.getLearnedSkills(category);

        assertEquals(2, learned.size());
        assertEquals(first, learned.get(0), "l'ordre du registre, pas celui de l'apprentissage");
        assertEquals(third, learned.get(1));
    }

    @Test
    void uneCategorieSansCompetenceAppriseRendUneListeVide() {
        TestCategory category = new TestCategory("test");
        category.add(new DummySkill("skill", 1));
        AbilityData data = new AbilityData();

        assertTrue(data.getLearnedSkills(category).isEmpty());
        assertTrue(data.getLearnedSkills(null).isEmpty());
    }

    @Test
    void laSauvegardeConserveLesCompetencesApprises() {
        TestCategory category = new TestCategory("test");
        DummySkill learned = new DummySkill("apprise", 1);
        DummySkill untouched = new DummySkill("vierge", 1);
        category.add(learned);
        category.add(untouched);
        AbilityData data = new AbilityData();
        data.setCategoryLevel(category, 3);
        data.learnSkill(learned);

        AbilityData reloaded = new AbilityData();
        reloaded.deserializeNBT(data.serializeNBT());

        assertEquals(3, reloaded.getCategoryLevel(category), "le niveau voyage aussi");
        assertTrue(reloaded.isSkillLearned(learned));
        assertFalse(reloaded.isSkillLearned(untouched));
        assertEquals(1, reloaded.getLearnedSkillCount());
    }

    @Test
    void uneSauvegardeVideNapprendRien() {
        AbilityData data = new AbilityData();
        data.deserializeNBT(new CompoundTag());

        assertEquals(0, data.getLearnedSkillCount());
    }

    @Test
    void uneCompetenceInconnueEstConservee() {
        // Cas reel : une competence retiree du mod apres qu'un joueur l'a apprise.
        // La donnee ne doit pas exploser a la relecture, ni perdre les autres.
        CompoundTag tag = new CompoundTag();
        ListTag skills = new ListTag();
        skills.add(StringTag.valueOf("disparue.skill"));
        skills.add(StringTag.valueOf("test.apprise"));
        tag.put("skills", skills);

        AbilityData data = new AbilityData();
        data.deserializeNBT(tag);

        assertEquals(2, data.getLearnedSkillCount());
    }

    @Test
    void laSauvegardeNeMelangePasLesNiveauxEtLesCompetences() {
        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("skill", 1);
        category.add(skill);
        AbilityData data = new AbilityData();
        data.setCategoryLevel(category, 2);

        // Un niveau sans competence apprise, et l'inverse : les deux champs doivent
        // rester independants, sinon une relecture ferait apparaitre une competence
        // que personne n'a apprise.
        AbilityData reloaded = new AbilityData();
        reloaded.deserializeNBT(data.serializeNBT());

        assertEquals(2, reloaded.getCategoryLevel(category));
        assertEquals(0, reloaded.getLearnedSkillCount());
    }
}
