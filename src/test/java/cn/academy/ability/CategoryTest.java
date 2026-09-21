package cn.academy.ability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitaires de {@link Category} : attribution des identifiants, detection
 * des doublons, recherche. Aucune instance de Minecraft necessaire.
 */
class CategoryTest {

    /** Skill minimal, juste de quoi peupler une categorie. */
    static final class DummySkill extends Skill {
        DummySkill(String name) { super(name); }
    }

    /** Skill qui declare un cout, pour verifier la valeur par defaut. */
    static final class CostlySkill extends Skill {
        CostlySkill(String name) { super(name); }
        @Override public float getCpCost() { return 42f; }
    }

    @Test
    @DisplayName("les skills recoivent des identifiants sequentiels dans l'ordre d'ajout")
    void skillsGetSequentialIds() {
        Category category = new Category("test");
        DummySkill first = new DummySkill("first");
        DummySkill second = new DummySkill("second");

        category.addSkill(first);
        category.addSkill(second);

        assertEquals(0, first.getId());
        assertEquals(1, second.getId());
        assertSame(category, first.getCategory());
        assertEquals(2, category.getSkills().size());
    }

    @Test
    @DisplayName("getSkill retrouve par identifiant et par nom, null sinon")
    void lookupByIdAndName() {
        Category category = new Category("test");
        DummySkill skill = new DummySkill("arc_gen");
        category.addSkill(skill);

        assertSame(skill, category.getSkill(0));
        assertSame(skill, category.getSkill("arc_gen"));
        assertNull(category.getSkill(1), "identifiant hors bornes");
        assertNull(category.getSkill(-1), "identifiant negatif");
        assertNull(category.getSkill("inconnu"));
    }

    @Test
    @DisplayName("ajouter deux skills du meme nom leve une exception")
    void duplicateSkillIsRejected() {
        Category category = new Category("test");
        category.addSkill(new DummySkill("dup"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> category.addSkill(new DummySkill("dup")));
        assertTrue(ex.getMessage().contains("dup"), "le message doit citer le skill fautif : " + ex.getMessage());
    }

    @Test
    @DisplayName("la liste des skills est non modifiable")
    void skillsListIsImmutable() {
        Category category = new Category("test");
        category.addSkill(new DummySkill("a"));

        assertThrows(UnsupportedOperationException.class,
                () -> category.getSkills().add(new DummySkill("b")));
    }

    @Test
    @DisplayName("un skill est actif par defaut, sans cout")
    void skillDefaults() {
        DummySkill skill = new DummySkill("plain");

        assertFalse(skill.isPassive(), "un skill est actif tant qu'il ne declare pas le contraire");
        assertEquals(0f, skill.getCpCost(), 0f);
        assertEquals("plain", skill.getName());
    }

    @Test
    @DisplayName("getCpCost est bien surcharge par les skills qui consomment")
    void cpCostIsOverridable() {
        assertEquals(42f, new CostlySkill("costly").getCpCost(), 0f);
    }

    @Test
    @DisplayName("un skill passif se declare en surchargeant isPassive")
    void passiveSkill() {
        Skill passive = new Skill("passive") {
            @Override public boolean isPassive() { return true; }
        };

        assertTrue(passive.isPassive());
        assertNotNull(passive.getName());
    }
}
