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
            Map.entry("electromaster.thunder_bolt", 4),
            Map.entry("electromaster.thunder_clap", 5),
            Map.entry("electromaster.charging", 1),
            Map.entry("electromaster.mag_movement", 2),
            Map.entry("meltdowner.electron_bomb", 1),
            Map.entry("meltdowner.light_shield", 2),
            Map.entry("meltdowner.scatter_bomb", 2),
            Map.entry("meltdowner.meltdowner", 3),
            Map.entry("meltdowner.mine_ray_basic", 3),
            Map.entry("meltdowner.mine_ray_expert", 4),
            Map.entry("meltdowner.mine_ray_luck", 5),
            Map.entry("meltdowner.jet_engine", 4),
            Map.entry("meltdowner.ray_barrage", 4),
            Map.entry("teleporter.dim_folding_theorem", 1),
            Map.entry("teleporter.threatening_teleport", 1),
            Map.entry("teleporter.mark_teleport", 2),
            Map.entry("teleporter.flesh_ripping", 3),
            Map.entry("teleporter.penetrate_teleport", 2),
            Map.entry("teleporter.shift_tp", 4),
            Map.entry("teleporter.flashing", 5),
            Map.entry("teleporter.location_teleport", 3),
            Map.entry("vecmanip.vec_accel", 2),
            Map.entry("vecmanip.dir_shock", 1),
            Map.entry("vecmanip.ground_shock", 1),
            Map.entry("vecmanip.vec_reflection", 4));
    /** Les dependances de l'original dont les deux bouts sont portes. */
    private static final Map<String, List<String>> EXPECTED_DEPENDENCIES = Map.ofEntries(
            Map.entry("electromaster.body_intensify", List.of("electromaster.arc_gen")),
            Map.entry("electromaster.thunder_bolt", List.of("electromaster.arc_gen")),
            Map.entry("electromaster.railgun", List.of("electromaster.thunder_bolt")),
            Map.entry("electromaster.thunder_clap", List.of("electromaster.thunder_bolt")),
            Map.entry("electromaster.charging", List.of("electromaster.arc_gen")),
            Map.entry("electromaster.mag_movement", List.of("electromaster.arc_gen")),
            Map.entry("teleporter.dim_folding_theorem", List.of("teleporter.threatening_teleport")),
            Map.entry("teleporter.penetrate_teleport", List.of("teleporter.threatening_teleport")),
            Map.entry("teleporter.mark_teleport", List.of("teleporter.threatening_teleport")),
            // La seule qui demande deux parentes : savoir marquer, et savoir traverser.
            Map.entry("teleporter.flesh_ripping",
                    List.of("teleporter.mark_teleport", "teleporter.penetrate_teleport")),
            Map.entry("meltdowner.light_shield", List.of("meltdowner.electron_bomb")),
            Map.entry("meltdowner.scatter_bomb", List.of("meltdowner.electron_bomb")),
            // La deuxieme a deux parentes : la bombe pour le plasma, le bouclier pour
            // l'avoir tenu. L'original les demandait toutes les deux a 0,8.
            Map.entry("meltdowner.meltdowner",
                    List.of("meltdowner.light_shield", "meltdowner.scatter_bomb")),
            Map.entry("meltdowner.mine_ray_basic", List.of("meltdowner.meltdowner")),
            Map.entry("meltdowner.mine_ray_expert", List.of("meltdowner.mine_ray_basic")),
            Map.entry("meltdowner.mine_ray_luck", List.of("meltdowner.mine_ray_expert")),
            Map.entry("meltdowner.jet_engine", List.of("meltdowner.meltdowner")),
            Map.entry("meltdowner.ray_barrage", List.of("meltdowner.meltdowner")),
            Map.entry("teleporter.flashing", List.of("teleporter.shift_tp")),
            // La troisieme a deux parentes : savoir traverser un mur, et savoir marquer.
            Map.entry("teleporter.location_teleport",
                    List.of("teleporter.penetrate_teleport", "teleporter.mark_teleport")),
            // Le choc dirige est la racine de vecmanip : c'est de lui que descendent
            // l'acceleration de vecteur et l'onde de choc chez l'original.
            Map.entry("vecmanip.vec_accel", List.of("vecmanip.dir_shock")),
            Map.entry("vecmanip.ground_shock", List.of("vecmanip.dir_shock")));

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
                "le port compte autant de competences que la table en fige : une de plus "
                        + "ou de moins doit se voir ici");
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
            List<String> expected = EXPECTED_DEPENDENCIES.get(fullName(skill));
            if (expected == null) {
                assertTrue(skill.getDependencies().isEmpty(),
                        fullName(skill) + " ne devrait dependre de rien dans le port");
                continue;
            }
            // Une competence peut avoir plusieurs parentes : l'original en demandait deux
            // a la dechirure (marquer et traverser). Ce qui compte est l'ensemble, pas
            // l'ordre dans lequel les conditions ont ete posees.
            Set<String> found = new HashSet<>();
            for (Skill dependency : skill.getDependencies()) {
                found.add(fullName(dependency));
            }
            assertEquals(Set.copyOf(expected), found,
                    "dependances de " + fullName(skill));
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
            // un tir qui ne touche rien ; 0,005 s'il touche quelque chose, verse par
            // l'effet lui-meme
            Map.entry("electromaster.thunder_bolt", 0.003f),
            Map.entry("electromaster.thunder_clap", 0.003f),
            Map.entry("meltdowner.electron_bomb", 0.005f),
            // 0,002 multiplie par le facteur de charge de 1,2 a pleine charge
            Map.entry("meltdowner.meltdowner", 0.0024f),
            // un tir, avec ou sans bille
            Map.entry("meltdowner.ray_barrage", 0.005f),
            // 0,00014 par bloc, pour un saut d'une dizaine de blocs
            Map.entry("teleporter.penetrate_teleport", 0.00014f * 10f),
            // 0,0006 pour un lancer dans le vide ; 0,003 quand l'objet frappe, verse par
            // l'effet lui-meme
            Map.entry("teleporter.threatening_teleport", 0.0006f),
            // montant de base : l'original ajoutait 0,002 par entite traversee
            Map.entry("teleporter.shift_tp", 0.002f),
            Map.entry("vecmanip.vec_accel", 0.002f));

    @Test
    void lesGainsDExperienceSontCeuxDeLOriginal() {
        for (Skill skill : allSkills()) {
            // Les passives et les competences tenues n'ont pas de gain a l'activation :
            // elles versent leur experience depuis leur propre crochet, le seul endroit
            // ou elles savent que quelque chose s'est produit. Les competences qui le
            // declarent (voir Skill#earnsExpOnEffect) non plus, et pour la meme raison.
            if (skill.isPassive() || skill.isHeld() || skill.earnsExpOnEffect()) continue;
            Float expected = EXPECTED_EXP.get(fullName(skill));
            assertNotNull(expected, "gain d'experience non fige pour " + fullName(skill));
            assertEquals(expected.floatValue(), skill.getExpGain(charged(skill)),
                    0.000001f, "gain de " + fullName(skill));
        }
    }

    @Test
    void aucuneCompetenceActiveNeResteSansExperience() {
        // Une competence active sans gain d'experience rendrait son niveau
        // inatteignable, et cela ne se verrait qu'apres des heures de jeu.
        for (Skill skill : allSkills()) {
            if (skill.isPassive() || skill.isHeld() || skill.earnsExpOnEffect()) continue;
            assertTrue(skill.getExpGain(charged(skill)) > 0f,
                    fullName(skill) + " ne rapporte aucune experience");
        }
    }

    /**
     * Les surcouts, repris de l'original.
     *
     * Le surcout est la seule des deux ressources dont l'echelle de l'original tient
     * telle quelle dans le port : les couts en CP, eux, supposent une reserve de
     * plusieurs milliers de points et restent ceux du port.
     */
    private static final Map<String, Float> EXPECTED_OVERLOAD = Map.ofEntries(
            Map.entry("electromaster.arc_gen", 18f),
            Map.entry("electromaster.railgun", 180f),
            Map.entry("electromaster.body_intensify", 200f),
            Map.entry("electromaster.thunder_bolt", 50f),
            Map.entry("electromaster.thunder_clap", 390f),
            Map.entry("electromaster.charging", 65f),
            Map.entry("electromaster.mag_movement", 60f),
            Map.entry("teleporter.threatening_teleport", 18f),
            Map.entry("teleporter.mark_teleport", 40f),
            Map.entry("teleporter.flesh_ripping", 60f),
            Map.entry("meltdowner.electron_bomb", 200f),
            Map.entry("meltdowner.meltdowner", 200f),
            Map.entry("meltdowner.light_shield", 110f),
            Map.entry("meltdowner.scatter_bomb", 80f),
            Map.entry("meltdowner.mine_ray_basic", 200f),
            Map.entry("meltdowner.mine_ray_expert", 300f),
            Map.entry("meltdowner.mine_ray_luck", 350f),
            Map.entry("meltdowner.jet_engine", 60f),
            Map.entry("meltdowner.ray_barrage", 300f),
            Map.entry("teleporter.flashing", 250f),
            Map.entry("teleporter.location_teleport", 240f),
            Map.entry("vecmanip.dir_shock", 18f),
            Map.entry("vecmanip.ground_shock", 15f),
            Map.entry("teleporter.penetrate_teleport", 80f),
            Map.entry("teleporter.shift_tp", 40f),
            Map.entry("vecmanip.vec_accel", 30f));

    @Test
    void lesSurcoutsSontCeuxDeLOriginal() {
        for (Skill skill : allSkills()) {
            if (skill.isPassive()) continue;
            Float expected = EXPECTED_OVERLOAD.get(fullName(skill));
            assertNotNull(expected, "surcout non fige pour " + fullName(skill));
            assertEquals(expected.floatValue(), skill.getOverloadCost(charged(skill)),
                    0.0001f, "surcout de " + fullName(skill));
        }
    }

    @Test
    void aucuneCompetenceActiveNeResteSansSurcout() {
        // Une competence active qui ne chargerait pas la reserve serait gratuite en
        // surcout : elle pourrait etre enchainee sans jamais mettre le joueur en
        // surcharge, ce que l'original ne permettait a aucune.
        for (Skill skill : allSkills()) {
            if (skill.isPassive()) continue;
            assertTrue(skill.getOverloadCost(charged(skill)) > 0f,
                    fullName(skill) + " ne charge pas la reserve de surcout");
        }
    }

    /**
     * Un etat d'usage : une competence qui se charge est chargee au maximum.
     *
     * Sans cela, les gains d'experience d'une competence chargee seraient mesures au
     * repos, donc multiplies par son facteur de charge le plus bas — ce qui figerait
     * une valeur qui n'est celle d'aucun tir reel. Et une courbe qui depend de la
     * charge se lit avec la charge au maximum, comme un tir tenu.
     */
    private static AbilityData charged(Skill skill) {
        AbilityData data = new AbilityData();
        if (skill.isChargeable()) {
            data.beginCharge(skill);
            for (int i = 0; i < skill.getMaxChargeTicks(data); i++) {
                data.tickCharges();
            }
            data.endCharge(skill);
        }
        return data;
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
