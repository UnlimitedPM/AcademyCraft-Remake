package cn.academy.ability;

import cn.academy.ability.develop.condition.ConditionDependency;
import cn.academy.ability.develop.condition.ConditionDeveloperType;
import cn.academy.ability.develop.condition.LearningCondition;
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
            Map.entry("electromaster.mine_detect", 3),
            Map.entry("electromaster.mag_manip", 2),
            Map.entry("meltdowner.electron_bomb", 1),
            Map.entry("meltdowner.rad_intensify", 1),
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
            Map.entry("teleporter.space_fluct", 4),
            Map.entry("teleporter.flashing", 5),
            Map.entry("teleporter.location_teleport", 3),
            Map.entry("vecmanip.vec_accel", 2),
            Map.entry("vecmanip.dir_shock", 1),
            Map.entry("vecmanip.ground_shock", 1),
            Map.entry("vecmanip.dir_blast", 3),
            Map.entry("vecmanip.blood_retro", 4),
            Map.entry("vecmanip.vec_deviation", 2),
            Map.entry("vecmanip.vec_reflection", 4),
            Map.entry("vecmanip.storm_wing", 3),
            Map.entry("vecmanip.plasma_cannon", 5),
            // Les trois cursus generiques, ajoutes a CHAQUE categorie comme dans l'original
            // (`VanillaCategories.addGenericSkills`) : ils y occupent les niveaux 3, 4 et 5.
            Map.entry("electromaster.brain_course", 3),
            Map.entry("electromaster.brain_course_advanced", 4),
            Map.entry("electromaster.mind_course", 5),
            Map.entry("meltdowner.brain_course", 3),
            Map.entry("meltdowner.brain_course_advanced", 4),
            Map.entry("meltdowner.mind_course", 5),
            Map.entry("teleporter.brain_course", 3),
            Map.entry("teleporter.brain_course_advanced", 4),
            Map.entry("teleporter.mind_course", 5),
            Map.entry("vecmanip.brain_course", 3),
            Map.entry("vecmanip.brain_course_advanced", 4),
            Map.entry("vecmanip.mind_course", 5));

    /** Les dependances de l'original dont les deux bouts sont portes. */
    private static final Map<String, List<String>> EXPECTED_DEPENDENCIES = Map.ofEntries(
            // L'electromaster est complet : ses huit competences et leurs onze liens, ceux
            // que l'original posait dans son CatElectromaster.
            Map.entry("electromaster.body_intensify",
                    List.of("electromaster.arc_gen", "electromaster.charging")),
            Map.entry("electromaster.thunder_bolt",
                    List.of("electromaster.arc_gen", "electromaster.charging")),
            Map.entry("electromaster.railgun",
                    List.of("electromaster.thunder_bolt", "electromaster.mag_manip")),
            Map.entry("electromaster.thunder_clap", List.of("electromaster.thunder_bolt")),
            Map.entry("electromaster.charging", List.of("electromaster.arc_gen")),
            Map.entry("electromaster.mag_movement",
                    List.of("electromaster.arc_gen", "electromaster.charging")),
            // La manipulation d'un bloc descend de la traction, avec la moitie de son
            // experience : on n'arrache pas un bloc avant de savoir s'y accrocher.
            Map.entry("electromaster.mag_manip", List.of("electromaster.mag_movement")),
            // Et la detection de minerais descend de la manipulation, avec toute la sienne.
            Map.entry("electromaster.mine_detect", List.of("electromaster.mag_manip")),
            Map.entry("teleporter.dim_folding_theorem", List.of("teleporter.threatening_teleport")),
            Map.entry("teleporter.penetrate_teleport", List.of("teleporter.threatening_teleport")),
            Map.entry("teleporter.mark_teleport", List.of("teleporter.threatening_teleport")),
            // La seule qui demande deux parentes : savoir marquer, et savoir traverser.
            Map.entry("teleporter.flesh_ripping",
                    List.of("teleporter.mark_teleport", "teleporter.penetrate_teleport")),
            Map.entry("meltdowner.light_shield", List.of("meltdowner.electron_bomb")),
            Map.entry("meltdowner.rad_intensify", List.of("meltdowner.electron_bomb")),
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
            // Le passif des critiques descend du saut, sans seuil d'experience.
            Map.entry("teleporter.space_fluct", List.of("teleporter.shift_tp")),
            // La troisieme a deux parentes : savoir traverser un mur, et savoir marquer.
            Map.entry("teleporter.location_teleport",
                    List.of("teleporter.penetrate_teleport", "teleporter.mark_teleport")),
            // Le choc dirige est la racine de vecmanip : c'est de lui que descendent
            // l'acceleration de vecteur et l'onde de choc chez l'original.
            Map.entry("vecmanip.vec_accel", List.of("vecmanip.dir_shock")),
            Map.entry("vecmanip.ground_shock", List.of("vecmanip.dir_shock")),
            // L'onde dirigee descend de l'onde au sol : on n'apprend pas a viser avant
            // d'avoir appris a frapper le sol.
            Map.entry("vecmanip.dir_blast", List.of("vecmanip.ground_shock")),
            // Le contact descend de l'onde dirigee : on n'apprend pas a retourner le sang
            // avant d'avoir appris a ouvrir le sol.
            Map.entry("vecmanip.blood_retro", List.of("vecmanip.dir_blast")),
            // La deviation descend de l'acceleration de vecteur.
            Map.entry("vecmanip.vec_deviation", List.of("vecmanip.vec_accel")),
            // Et la reflexion de la deviation : arreter ce qui vole s'apprend avant de le
            // retourner, comme dans l'arbre de l'original.
            Map.entry("vecmanip.vec_reflection", List.of("vecmanip.vec_deviation")),
            // Les ailes de tempete descendent de l'acceleration : on n'apprend pas a voler
            // avant d'avoir appris a se propulser.
            Map.entry("vecmanip.storm_wing", List.of("vecmanip.vec_accel")),
            // Et le canon a plasma descend des ailes : c'est la competence la plus chere de
            // la categorie, et la derniere de l'arbre.
            Map.entry("vecmanip.plasma_cannon", List.of("vecmanip.storm_wing")),
            // Les cursus generiques s'enchainent, dans chaque categorie : le cours avance
            // demande le premier, l'entrainement mental demande le cours avance. Sans seuil
            // d'experience — l'original les liait sans en demander.
            Map.entry("electromaster.brain_course_advanced", List.of("electromaster.brain_course")),
            Map.entry("electromaster.mind_course", List.of("electromaster.brain_course_advanced")),
            Map.entry("meltdowner.brain_course_advanced", List.of("meltdowner.brain_course")),
            Map.entry("meltdowner.mind_course", List.of("meltdowner.brain_course_advanced")),
            Map.entry("teleporter.brain_course_advanced", List.of("teleporter.brain_course")),
            Map.entry("teleporter.mind_course", List.of("teleporter.brain_course_advanced")),
            Map.entry("vecmanip.brain_course_advanced", List.of("vecmanip.brain_course")),
            Map.entry("vecmanip.mind_course", List.of("vecmanip.brain_course_advanced")));

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

    /**
     * Chaque competence livree dit quelle machine peut l'enseigner.
     *
     * La condition est posee par le constructeur de {@code Skill}, a partir du seul
     * niveau : les niveaux 1 et 2 tiennent dans l'objet portable, le 3 demande la machine
     * normale, les niveaux 4 et 5 la machine avancee. Une competence livree sans cette
     * condition serait apprenable avec n'importe quoi — un verrou ouvert en silence.
     */
    @Test
    void chaqueCompetenceExigeLaMachineDeSonNiveau() {
        for (Skill skill : allSkills()) {
            ConditionDeveloperType condition = null;
            for (LearningCondition candidate : skill.getConditions()) {
                if (candidate instanceof ConditionDeveloperType typed) condition = typed;
            }
            assertNotNull(condition, fullName(skill) + " n'exige aucune machine");
            assertEquals(skill.getMinimumDeveloperType(), condition.getRequired(),
                    "machine exigee par " + fullName(skill));
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

    /**
     * Les seuils d'experience que l'original demandait dans la parente, quand il en
     * demandait un.
     *
     * Une dependance sans seuil se contente de « apprise » ; l'original en demandait
     * parfois davantage, et c'est presque tout l'arbre de l'electromaster : le railgun
     * voulait 30 % du thunder bolt, le corps interdit toute l'experience de l'arc, la
     * detection de minerais toute celle de la manipulation d'un bloc. Ces nombres ne se
     * voient nulle part en jeu — sinon sous la forme d'une competence qui refuse de
     * s'ouvrir — donc ils sont figes ici.
     */
    private static final Map<String, Map<String, Float>> EXPECTED_THRESHOLDS = Map.of(
            "electromaster.charging", Map.of("electromaster.arc_gen", 0.3f),
            "electromaster.mag_movement", Map.of("electromaster.charging", 0.7f),
            "electromaster.mag_manip", Map.of("electromaster.mag_movement", 0.5f),
            "electromaster.body_intensify",
                    Map.of("electromaster.arc_gen", 1f, "electromaster.charging", 1f),
            "electromaster.thunder_bolt", Map.of("electromaster.charging", 0.7f),
            "electromaster.railgun",
                    Map.of("electromaster.thunder_bolt", 0.3f, "electromaster.mag_manip", 1f),
            "electromaster.thunder_clap", Map.of("electromaster.thunder_bolt", 1f),
            "electromaster.mine_detect", Map.of("electromaster.mag_manip", 1f));

    @Test
    void lesSeuilsDeDependanceSontCeuxDeLOriginal() {
        for (Skill skill : allSkills()) {
            if (!fullName(skill).startsWith("electromaster.")) continue;

            Map<String, Float> expected = EXPECTED_THRESHOLDS.getOrDefault(fullName(skill),
                    Map.of());
            Map<String, Float> found = new HashMap<>();
            for (LearningCondition condition : skill.getConditions()) {
                if (condition instanceof ConditionDependency dependency
                        && dependency.getRequiredExp() > 0f) {
                    found.put(fullName(dependency.getDependency()),
                            dependency.getRequiredExp());
                }
            }
            assertEquals(expected, found, "seuils de " + fullName(skill));
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
            // Les trois cursus generiques sont les SEULS a partager leur cle : la meme
            // competence est ajoutee aux quatre categories, et l'original ne lui donnait
            // qu'un nom (ac.ability.generic.*) au lieu d'un par categorie.
            if (key.startsWith("ac.ability.generic.")) continue;
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

    /**
     * L'ORDRE des competences dans chaque categorie, qui est celui que le menu F4 affiche.
     *
     * <p>Le joueur l'a vu avant nous : le port rangeait les competences dans l'ordre ou elles
     * avaient ete codees, ce qui melangeait les niveaux (le saut du teleporteur avant le lancer
     * d'objet, le meltdowner avant sa bombe). C'est celui des {@code Cat*} de l'original, et les
     * trois cursus generiques ferment chaque categorie.
     */
    @Test
    void lesCompetencesSontRangeesDansLOrdreDeLOriginal() {
        assertEquals(List.of("arc_gen", "charging", "mag_movement", "mag_manip", "mine_detect",
                        "body_intensify", "thunder_bolt", "railgun", "thunder_clap",
                        "brain_course", "brain_course_advanced", "mind_course"),
                namesOf(cn.academy.ability.electromaster.ElectromasterCategory.INSTANCE));
        assertEquals(List.of("electron_bomb", "rad_intensify", "scatter_bomb", "light_shield",
                        "meltdowner", "mine_ray_basic", "ray_barrage", "jet_engine",
                        "mine_ray_expert", "mine_ray_luck", "brain_course",
                        "brain_course_advanced", "mind_course"),
                namesOf(cn.academy.ability.meltdowner.MeltdownerCategory.INSTANCE));
        assertEquals(List.of("threatening_teleport", "dim_folding_theorem", "penetrate_teleport",
                        "mark_teleport", "flesh_ripping", "location_teleport", "shift_tp",
                        "space_fluct", "flashing", "brain_course", "brain_course_advanced",
                        "mind_course"),
                namesOf(cn.academy.ability.teleporter.TeleporterCategory.INSTANCE));
        assertEquals(List.of("dir_shock", "ground_shock", "vec_accel", "vec_deviation",
                        "dir_blast", "storm_wing", "blood_retro", "vec_reflection",
                        "plasma_cannon", "brain_course", "brain_course_advanced", "mind_course"),
                namesOf(cn.academy.ability.vecmanip.VecmanipCategory.INSTANCE));
    }

    private static List<String> namesOf(Category category) {
        return category.getSkills().stream().map(Skill::getName).toList();
    }

    /**
     * Les bonus des trois cursus generiques, et leur refus de la touche d'aptitude.
     *
     * <p>Les valeurs sont celles de l'original : +1000 et +1500 de reserve, +100 de surcout, et
     * x1,2 sur la recuperation. Le joueur a rappele que ce sont des bonus, pas des pouvoirs : ils
     * ne se rangent donc pas sur une touche (voir {@code Skill.canControl}).
     */
    @Test
    void lesCursusGeneriquesDonnentLeursBonus() {
        var brain = new cn.academy.ability.generic.GenericSkills.BrainCourse();
        var advanced = new cn.academy.ability.generic.GenericSkills.BrainCourseAdvanced();
        var mind = new cn.academy.ability.generic.GenericSkills.MindCourse();

        assertEquals(1000f, brain.getMaxControlPointBonus(null), 0.0001f);
        assertEquals(1500f, advanced.getMaxControlPointBonus(null), 0.0001f);
        assertEquals(100f, advanced.getMaxOverloadBonus(null), 0.0001f);
        assertEquals(1.2f, mind.getControlPointRecoverScale(null), 0.0001f);

        for (Skill skill : List.of(brain, advanced, mind)) {
            assertTrue(skill.isPassive(), skill.getName() + " est un passif");
            assertTrue(!skill.canControl(), skill.getName() + " ne se range pas sur une touche");
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
            // arc_gen n'y est pas : l'arc verse selon ce qu'il a touche (voir hitExp/blockExp).
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
     * telle quelle dans le port : les couts en CP, eux, sont aussi ceux de l'original
     * maintenant que la reserve l'est (1800 points au niveau 1).
     */
    private static final Map<String, Float> EXPECTED_OVERLOAD = Map.ofEntries(
            Map.entry("electromaster.arc_gen", 18f),
            Map.entry("electromaster.railgun", 180f),
            Map.entry("electromaster.body_intensify", 200f),
            Map.entry("electromaster.thunder_bolt", 50f),
            Map.entry("electromaster.thunder_clap", 390f),
            Map.entry("electromaster.charging", 65f),
            Map.entry("electromaster.mag_movement", 60f),
            Map.entry("electromaster.mine_detect", 200f),
            Map.entry("electromaster.mag_manip", 35f),
            Map.entry("teleporter.threatening_teleport", 18f),
            Map.entry("teleporter.mark_teleport", 40f),
            Map.entry("teleporter.flesh_ripping", 60f),
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
            Map.entry("vecmanip.dir_blast", 50f),
            Map.entry("vecmanip.blood_retro", 55f),
            Map.entry("vecmanip.vec_deviation", 80f),
            Map.entry("vecmanip.vec_reflection", 350f),
            Map.entry("vecmanip.storm_wing", 10f),
            Map.entry("vecmanip.plasma_cannon", 500f),
            Map.entry("teleporter.penetrate_teleport", 80f),
            Map.entry("teleporter.shift_tp", 40f),
            Map.entry("vecmanip.vec_accel", 30f));

    @Test
    void lesSurcoutsSontCeuxDeLOriginal() {
        for (Skill skill : allSkills()) {
            if (skill.isPassive()) continue;
            // La bombe a electrons est la seule active qui ne charge rien : elle n'a donc
            // pas de surcout a figer. Voir aucuneCompetenceActiveNeResteSansSurcout.
            if (fullName(skill).equals("meltdowner.electron_bomb")) continue;
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
        // surcharge, ce que l'original ne permettait a aucune... sauf a une seule.
        // La bombe a electrons ne payait RIEN chez lui : ni CP, ni surcout (son
        // `s_Execute` lachait la bille et posait sa recharge, sans un seul `consume`).
        for (Skill skill : allSkills()) {
            if (skill.isPassive()) continue;
            if (fullName(skill).equals("meltdowner.electron_bomb")) continue;
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
