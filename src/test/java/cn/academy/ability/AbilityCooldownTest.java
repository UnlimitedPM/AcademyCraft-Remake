package cn.academy.ability;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les recharges de competences.
 *
 * Portage de {@code CooldownData} : un compteur par competence, qui avance d'un tick
 * par tick et disparait a zero. Deux details de l'original valent la peine d'etre
 * figes : une recharge plus longue remplace celle en cours mais une plus courte ne
 * la raccourcit pas, et une duree nulle n'entre rien du tout.
 */
class AbilityCooldownTest {

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
    void uneCompetencePreteNAPasDeRecharge() {
        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("skill", 1);
        category.add(skill);
        AbilityData data = new AbilityData();

        assertEquals(0, data.getCooldown(skill));
        assertFalse(data.isOnCooldown(skill));
        assertEquals(0, data.getCooldownCount());
    }

    @Test
    void poserUneRechargeLaRendIndisponible() {
        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("skill", 1);
        category.add(skill);
        AbilityData data = new AbilityData();

        data.setCooldown(skill, 15);

        assertEquals(15, data.getCooldown(skill));
        assertTrue(data.isOnCooldown(skill));
        assertEquals(1, data.getCooldownCount());
    }

    @Test
    void laRechargeAvanceDUnTickPuisDisparait() {
        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("skill", 1);
        category.add(skill);
        AbilityData data = new AbilityData();
        data.setCooldown(skill, 3);

        data.tickCooldowns();
        assertEquals(2, data.getCooldown(skill));
        data.tickCooldowns();
        assertEquals(1, data.getCooldown(skill));
        data.tickCooldowns();

        assertEquals(0, data.getCooldown(skill), "la competence doit etre prete");
        assertFalse(data.isOnCooldown(skill));
        assertEquals(0, data.getCooldownCount(), "et le compteur doit avoir disparu");
    }

    @Test
    void uneRechargePlusLongueRemplaceLaCourante() {
        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("skill", 1);
        category.add(skill);
        AbilityData data = new AbilityData();
        data.setCooldown(skill, 10);

        data.setCooldown(skill, 40);

        assertEquals(40, data.getCooldown(skill));
    }

    @Test
    void uneRechargePlusCourteNeRaccourcitPasLaCourante() {
        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("skill", 1);
        category.add(skill);
        AbilityData data = new AbilityData();
        data.setCooldown(skill, 40);

        // Detail repris de l'original : sans ce maximum, enchainer deux tirs ferait
        // tomber la recharge a celle du dernier, donc la raccourcirait.
        data.setCooldown(skill, 5);

        assertEquals(40, data.getCooldown(skill));
    }

    @Test
    void uneDureeNulleNEntreRien() {
        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("skill", 1);
        category.add(skill);
        AbilityData data = new AbilityData();

        data.setCooldown(skill, 0);
        data.setCooldown(skill, -5);

        assertEquals(0, data.getCooldown(skill));
        assertEquals(0, data.getCooldownCount(), "une competence sans recharge ne doit rien laisser");
    }

    @Test
    void lesRechargesDesCompetencesSontIndependantes() {
        TestCategory category = new TestCategory("test");
        DummySkill first = new DummySkill("un", 1);
        DummySkill second = new DummySkill("deux", 1);
        category.add(first);
        category.add(second);
        AbilityData data = new AbilityData();

        data.setCooldown(first, 30);

        assertEquals(30, data.getCooldown(first));
        assertEquals(0, data.getCooldown(second), "l'autre competence reste prete");
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

        data.setCooldown(inFirst, 20);

        assertEquals(20, data.getCooldown(inFirst));
        assertEquals(0, data.getCooldown(inSecond), "meme nom, autre categorie : autre recharge");
    }

    @Test
    void uneCompetenceSansCategorieNAPasDeRecharge() {
        AbilityData data = new AbilityData();
        DummySkill orphan = new DummySkill("orpheline", 1);

        data.setCooldown(orphan, 20);
        data.setCooldown(null, 20);

        assertEquals(0, data.getCooldown(orphan));
        assertEquals(0, data.getCooldownCount());
    }

    @Test
    void faireAvancerDesRechargesVidesNeFaitRien() {
        AbilityData data = new AbilityData();

        data.tickCooldowns();

        assertEquals(0, data.getCooldownCount());
    }

    @Test
    void clearOublieToutesLesRecharges() {
        TestCategory category = new TestCategory("test");
        DummySkill first = new DummySkill("un", 1);
        DummySkill second = new DummySkill("deux", 1);
        category.add(first);
        category.add(second);
        AbilityData data = new AbilityData();
        data.setCooldown(first, 30);
        data.setCooldown(second, 10);

        data.clearCooldowns();

        assertEquals(0, data.getCooldownCount());
        assertFalse(data.isOnCooldown(first));
        assertFalse(data.isOnCooldown(second));
    }

    @Test
    void laSauvegardeConserveLesRecharges() {
        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("skill", 1);
        category.add(skill);
        AbilityData data = new AbilityData();
        data.setCooldown(skill, 45);

        AbilityData reloaded = new AbilityData();
        reloaded.deserializeNBT(data.serializeNBT());

        assertEquals(45, reloaded.getCooldown(skill));
        assertTrue(reloaded.isOnCooldown(skill));
    }

    @Test
    void uneSauvegardeVideNeDonneAucuneRecharge() {
        AbilityData data = new AbilityData();
        data.deserializeNBT(new CompoundTag());

        assertEquals(0, data.getCooldownCount());
    }
}
