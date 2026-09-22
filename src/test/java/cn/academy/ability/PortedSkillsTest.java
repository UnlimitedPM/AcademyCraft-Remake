package cn.academy.ability;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les competences reellement livrees avec le mod.
 *
 * Ce test lit les vraies categories, pas des doublures : il verifie donc que la
 * recopie des niveaux et des dependances depuis l'original est complete. C'est le
 * controle qui remplace le fait d'ouvrir le jeu pour regarder l'ecran du
 * developpeur — et il attrape exactement l'erreur qui serait invisible autrement :
 * une competence oubliee au niveau 0, donc offerte d'office.
 *
 * Les niveaux viennent de la 1.12.2 : {@code Skill("<nom>", <niveau>)} dans chaque
 * classe de competence, et les dependances des constructeurs de categorie. La table
 * {@code EXPECTED_LEVELS} fige les valeurs, pour qu'un changement se voie.
 */
class PortedSkillsTest {

    private static final Map<String, Integer> EXPECTED_LEVELS = Map.ofEntries(
            Map.entry("electromaster.arc_gen", 1),
            Map.entry("electromaster.body_intensify", 3),
            Map.entry("electromaster.railgun", 4),
            Map.entry("meltdowner.electron_bomb", 1),
            Map.entry("meltdowner.light_shield", 2),
            Map.entry("meltdowner.meltdowner", 3),
            Map.entry("teleporter.dim_folding_theorem", 1),
            Map.entry("teleporter.penetrate_teleport", 2),
            Map.entry("teleporter.shift_tp", 4),
            Map.entry("vecmanip.vec_accel", 2),
            Map.entry("vecmanip.vec_reflection", 4));

    /** Les dependances de l'original dont les deux bouts sont portes. */
    private static final Map<String, String> EXPECTED_DEPENDENCIES = Map.of(
            "electromaster.body_intensify", "electromaster.arc_gen",
            "meltdowner.light_shield", "meltdowner.electron_bomb",
            "meltdowner.meltdowner", "meltdowner.light_shield");

    private static List<Category> categories() {
        return List.of(
                cn.academy.ability.electromaster.ElectromasterCategory.INSTANCE,
                cn.academy.ability.meltdowner.MeltdownerCategory.INSTANCE,
                cn.academy.ability.teleporter.TeleporterCategory.INSTANCE,
                cn.academy.ability.vecmanip.VecmanipCategory.INSTANCE);
    }

    private static List<Skill> allSkills() {
        List<Skill> skills = new ArrayList<>();
        for (Category category : categories()) {
            skills.addAll(category.getSkills());
        }
        return skills;
    }

    private static String fullName(Skill skill) {
        return skill.getCategory().getName() + "." + skill.getName();
    }

    @Test
    void toutesLesCategoriesSontLa() {
        assertEquals(4, categories().size());
        for (Category category : categories()) {
            assertFalse(category.getSkills().isEmpty(), category.getName() + " sans competence");
        }
        assertEquals(EXPECTED_LEVELS.size(), allSkills().size(),
                "le port compte onze competences : une de plus ou de moins doit se voir ici");
    }

    @Test
    void aucunCompetenceNEstResteeAuNiveauZero() {
        for (Skill skill : allSkills()) {
            assertTrue(skill.getLevel() >= 1,
                    fullName(skill) + " est au niveau 0, donc offerte sans rien apprendre");
        }
    }

    @Test
    void lesNiveauxSontCeuxDeLOriginal() {
        for (Skill skill : allSkills()) {
            Integer expected = EXPECTED_LEVELS.get(fullName(skill));
            assertNotNull(expected, "niveau non fige pour " + fullName(skill));
            assertEquals(expected.intValue(), skill.getLevel(), "niveau de " + fullName(skill));
        }
    }

    @Test
    void lesPrixSuiventLeNiveau() {
        for (Skill skill : allSkills()) {
            int expected = (int) (3 + skill.getLevel() * skill.getLevel() * 0.5f);
            assertEquals(expected, skill.getLearningStims(), "stimulations de " + fullName(skill));
        }
    }

    @Test
    void lesDependancesPorteesSontCellesAttendues() {
        for (Skill skill : allSkills()) {
            String expected = EXPECTED_DEPENDENCIES.get(fullName(skill));
            if (expected == null) {
                assertTrue(skill.getDependencies().isEmpty(),
                        fullName(skill) + " ne devrait dependre de rien dans le port");
                continue;
            }
            assertEquals(1, skill.getDependencies().size(), "une seule dependance pour " + fullName(skill));
            assertEquals(expected, fullName(skill.getDependencies().get(0)));
        }
    }

    @Test
    void uneDependanceResteDansSaCategorie() {
        // L'original reliait des competences d'une meme categorie. Une dependance
        // croisee serait forcement une erreur de recopie, et elle bloquerait une
        // competence derriere une autre categorie.
        for (Skill skill : allSkills()) {
            for (Skill dependency : skill.getDependencies()) {
                assertSame(skill.getCategory(), dependency.getCategory(),
                        fullName(skill) + " depend d'une competence d'une autre categorie");
            }
        }
    }

    @Test
    void lesClesDeLangueSontUniquesEtBienFormees() {
        Set<String> seen = new HashSet<>();
        for (Skill skill : allSkills()) {
            String key = skill.getDisplayKey();
            assertTrue(key.startsWith("ac.ability."), "cle inattendue : " + key);
            assertTrue(key.endsWith(".name"), "cle inattendue : " + key);
            assertTrue(seen.add(key), "cle en double : " + key);
        }
    }

    @Test
    void chaqueNiveauAttenduCorrespondAUneCompetenceLivree() {
        // Le sens inverse du test des niveaux : une competence attendue qui aurait
        // disparu du port se verrait ici, et pas seulement dans une table.
        Map<String, Skill> found = new HashMap<>();
        for (Skill skill : allSkills()) {
            found.put(fullName(skill), skill);
        }
        for (String name : EXPECTED_LEVELS.keySet()) {
            assertNotNull(found.get(name), name + " attendue mais absente du port");
        }
    }
}
