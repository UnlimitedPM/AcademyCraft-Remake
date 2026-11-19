package cn.academy.command;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les regles des commandes de debogage, relues sans Minecraft.
 *
 * <p>Ce qui se verifie ici est ce qui pourrait se casser en silence : l'ordre du catalogue
 * (c'est la liste que {@code /aim help} affiche), la recherche d'une competence par nom ou par
 * identifiant, et la regle du <b>seul pouvoir</b> — celle qui demande de lacher le premier
 * avant de prendre le second.
 */
class AimCommandSpecTest {

    private static final class TestCategory extends Category {
        TestCategory(String name) {
            super(name);
        }

        void add(Skill skill) {
            addSkill(skill);
        }
    }

    private static final class DummySkill extends Skill {
        DummySkill(String name) {
            super(name, 1);
        }
    }

    private static Category category(String name, String... skills) {
        TestCategory category = new TestCategory(name);
        for (String skill : skills) category.add(new DummySkill(skill));
        return category;
    }

    @Test
    @DisplayName("le catalogue est celui de l'original, dans son ordre")
    void leCatalogueEstCeluiDeLOriginal() {
        // C'est cet ordre que /aim help affiche : le joueur doit y lire la meme liste que
        // dans le mod d'origine, et un ajout ne doit pas se glisser au milieu.
        assertEquals(List.of("help", "cat", "catlist", "learn", "learn_all", "reset",
                "learned", "skills", "fullcp", "level", "exp", "cd_clear", "maxout"),
                AimCommandSpec.COMMANDS);
    }

    @Test
    @DisplayName("une competence se trouve par son identifiant ou par son nom")
    void uneCompetenceSeTrouveParIdentifiantOuParNom() {
        Category category = category("vecmanip", "dir_shock", "vec_accel");

        assertEquals("dir_shock", AimCommandSpec.findSkill(category, "dir_shock").getName());
        assertEquals("vec_accel", AimCommandSpec.findSkill(category, "1").getName(),
                "l'original acceptait aussi l'identifiant");
        assertNull(AimCommandSpec.findSkill(category, "railgun"), "une competence d'ailleurs");
        assertNull(AimCommandSpec.findSkill(category, null), "sans nom, il n'y a rien a trouver");
    }

    @Test
    @DisplayName("adopter un pouvoir fait lacher les autres")
    void adopterUnPouvoirFaitLacherLesAutres() {
        Category vecmanip = category("vecmanip", "dir_shock");
        Category electromaster = category("electromaster", "arc_gen");
        List<Category> all = List.of(vecmanip, electromaster);
        AbilityData data = new AbilityData();
        data.setCategoryLevel(vecmanip, 3);

        List<Category> dropped = AimCommandSpec.powersToForget(data, all, electromaster);

        assertEquals(1, dropped.size());
        assertEquals("vecmanip", dropped.get(0).getName(),
                "on ne peut pas porter le vecteur manipulation et l'electromaster ensemble");
        assertTrue(AimCommandSpec.powersToForget(data, all, vecmanip).isEmpty(),
                "le pouvoir qu'on adopte n'est jamais oublie : le remettre a zero annulerait la commande");
    }

    @Test
    @DisplayName("sans pouvoir, il n'y a rien a lacher")
    void sansPouvoirRienALacher() {
        List<Category> all = List.of(category("vecmanip", "dir_shock"));

        assertTrue(AimCommandSpec.powersToForget(new AbilityData(), all, null).isEmpty());
    }

    @Test
    @DisplayName("le pouvoir en service est le plus haut")
    void lePouvoirEnServiceEstLePlusHaut() {
        Category vecmanip = category("vecmanip", "dir_shock");
        Category electromaster = category("electromaster", "arc_gen");
        List<Category> all = List.of(vecmanip, electromaster);
        AbilityData data = new AbilityData();

        assertNull(AimCommandSpec.activePower(data, all), "aucun pouvoir au depart");

        data.setCategoryLevel(vecmanip, 3);
        data.setCategoryLevel(electromaster, 1);
        assertEquals("vecmanip", AimCommandSpec.activePower(data, all).getName());

        data.setCategoryLevel(electromaster, 4);
        assertEquals("electromaster", AimCommandSpec.activePower(data, all).getName());
    }

    @Test
    @DisplayName("les bornes sont celles de l'original")
    void lesBornesSontCellesDeLOriginal() {
        assertTrue(AimCommandSpec.levelInRange(1));
        assertTrue(AimCommandSpec.levelInRange(5));
        assertFalse(AimCommandSpec.levelInRange(0), "le niveau 0 n'existe pas : c'est l'absence de pouvoir");
        assertFalse(AimCommandSpec.levelInRange(6), "l'original s'arretait au niveau 5");

        assertTrue(AimCommandSpec.expInRange(0f));
        assertTrue(AimCommandSpec.expInRange(1f));
        assertFalse(AimCommandSpec.expInRange(1.5f));
        assertFalse(AimCommandSpec.expInRange(-0.1f));
    }

    @Test
    @DisplayName("les listes gardent la forme de l'original")
    void lesListesGardentLaFormeDeLOriginal() {
        Category category = category("vecmanip", "dir_shock", "vec_accel");

        assertEquals("#0 dir_shock: ", AimCommandSpec.skillPrefix(category.getSkill("dir_shock")));
        assertEquals("#1 vecmanip: ", AimCommandSpec.categoryPrefix(1, category));
        assertEquals("dir_shock, vec_accel", AimCommandSpec.learnedLine(List.of(
                category.getSkill("dir_shock"), category.getSkill("vec_accel"))));
        assertEquals("", AimCommandSpec.learnedLine(List.of()));
    }
}
