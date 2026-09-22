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

    // ------------------------------------------------------------------
    // Experience d'usage
    // ------------------------------------------------------------------

    /**
     * Les gains d'experience, repris de l'original.
     *
     * Les competences passives n'y sont pas : elles ne s'activent pas, donc leur
     * experience est versee depuis leur propre crochet (voir les tests suivants).
     */
    private static final Map<String, Float> EXPECTED_EXP = Map.ofEntries(
            // borne basse de lerpf(0.0048, 0.0072, experience)
            Map.entry("electromaster.arc_gen", 0.0048f),
            // un tir ; l'original doublait pour un coup au but, que le paquet ne voit pas
            Map.entry("electromaster.railgun", 0.005f),
            Map.entry("electromaster.body_intensify", 0.01f),
            Map.entry("meltdowner.electron_bomb", 0.005f),
            // montant de base : l'original multipliait par le temps de charge
            Map.entry("meltdowner.meltdowner", 0.002f),
            // 0,00014 par bloc, pour un saut d'une dizaine de blocs
            Map.entry("teleporter.penetrate_teleport", 0.00014f * 10f),
            // montant de base : l'original ajoutait 0,002 par entite traversee
            Map.entry("teleporter.shift_tp", 0.002f),
            Map.entry("vecmanip.vec_accel", 0.002f));

    @Test
    void lesGainsDExperienceSontCeuxDeLOriginal() {
        for (Skill skill : allSkills()) {
            if (skill.isPassive()) continue;
            Float expected = EXPECTED_EXP.get(fullName(skill));
            assertNotNull(expected, "gain d'experience non fige pour " + fullName(skill));
            assertEquals(expected.floatValue(), skill.getExpGain(new cn.academy.ability.AbilityData()),
                    0.000001f, "gain de " + fullName(skill));
        }
    }

    @Test
    void aucuneCompetenceActiveNeResteSansExperience() {
        // Une competence active sans gain d'experience rendrait son niveau
        // inatteignable, et cela ne se verrait qu'apres des heures de jeu.
        for (Skill skill : allSkills()) {
            if (skill.isPassive()) continue;
            assertTrue(skill.getExpGain(new cn.academy.ability.AbilityData()) > 0f,
                    fullName(skill) + " ne rapporte aucune experience");
        }
    }

    @Test
    void uneCompetencePassiveRapporteDeLExperienceParSonPropreCrochet() {
        var folding = cn.academy.ability.teleporter.TeleporterCategory.DIM_FOLDING_THEOREM;
        cn.academy.ability.AbilityData data = new cn.academy.ability.AbilityData();
        data.setCategoryLevel(folding.getCategory(), 1);

        assertTrue(folding.isPassive(), "c'est une passive : elle ne s'active pas");
        assertEquals(0f, data.getSkillExp(folding), 0.0001f, "on part de rien");

        // C'est ce que faisait l'utilitaire de teleportation de l'original.
        folding.onTeleported(data);

        assertTrue(data.getSkillExp(folding) > 0f, "une teleportation doit la faire progresser");
    }
}
