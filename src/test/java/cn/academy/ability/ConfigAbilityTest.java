package cn.academy.ability;

import cn.academy.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les reglages d'aptitude de la config : tables de reserve, multiplicateurs et interrupteur par
 * competence.
 *
 * <p>Ces trois familles viennent toutes de l'original ({@code init_cp}, {@code add_cp},
 * {@code init_overload}, {@code add_overload}, {@code cp_consume_speed},
 * {@code overload_consume_speed}, {@code exp_incr_speed} et le {@code enabled} de chaque
 * competence), et elles partagent le meme risque : un reglage qu'on ecrit, qu'on relit, et que
 * personne n'applique. Un test qui change la valeur et regarde le resultat est la seule facon de
 * savoir qu'un reglage sert vraiment a quelque chose.
 *
 * <p>Les champs statiques de {@link Config} sont remis a leur valeur d'origine apres chaque test :
 * une suite unitaire se partage le meme processus.
 */
class ConfigAbilityTest {

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

    /** Un joueur d'une categorie de test au niveau voulu, avec la competence apprise. */
    private static AbilityData learning(int level, Skill skill) {
        TestCategory category = new TestCategory("test");
        category.add(skill);
        AbilityData data = new AbilityData();
        data.setCategoryLevel(category, level);
        data.learnSkill(skill);
        return data;
    }

    // --- Valeurs d'origine, remises apres chaque test -------------------

    private final float[] initCp = Config.initCp;
    private final float[] addCp = Config.addCp;
    private final float[] initOverload = Config.initOverload;
    private final float[] addOverload = Config.addOverload;
    private final double cpCostScale = Config.cpCostScale;
    private final double overloadCostScale = Config.overloadCostScale;
    private final double cooldownScale = Config.cooldownScale;
    private final double expScale = Config.expScale;
    private final List<String> disabledSkills = Config.disabledSkills;

    @AfterEach
    void restoreConfig() {
        Config.initCp = initCp;
        Config.addCp = addCp;
        Config.initOverload = initOverload;
        Config.addOverload = addOverload;
        Config.cpCostScale = cpCostScale;
        Config.overloadCostScale = overloadCostScale;
        Config.cooldownScale = cooldownScale;
        Config.expScale = expScale;
        Config.disabledSkills = disabledSkills;
    }

    // --- Les quatre tables ---------------------------------------------

    @Test
    @DisplayName("les plafonds viennent de la config, pas de constantes figees")
    void lesTablesDeLaConfigSontLues() {
        Config.initCp = new float[]{10f, 20f, 30f, 40f, 50f, 60f};
        Config.addCp = new float[]{1f, 2f, 3f, 4f, 5f, 6f};
        Config.initOverload = new float[]{7f, 8f, 9f, 10f, 11f, 12f};
        Config.addOverload = new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f};

        assertEquals(30f, AbilityData.baseMaxControlPoint(2), 1.0e-4f);
        assertEquals(4f, AbilityData.maxAddControlPoint(3), 1.0e-4f);
        assertEquals(9f, AbilityData.baseMaxOverload(2), 1.0e-4f);
        assertEquals(0.5f, AbilityData.maxAddOverload(4), 1.0e-4f);

        // Et le joueur, pas seulement la table.
        assertEquals(50f, learning(4, new DummySkill("skill")).getRawMaxControlPoint(), 1.0e-4f);
        assertEquals(11f, learning(4, new DummySkill("skill")).getMaxOverload(), 1.0e-4f);
    }

    @Test
    @DisplayName("un niveau hors bornes ne sort pas de la table")
    void lesBornesSuiventLaTable() {
        Config.initOverload = new float[]{5f, 5f, 5f, 5f, 5f, 99f};

        assertEquals(5f, AbilityData.baseMaxOverload(-3), 1.0e-4f);
        assertEquals(99f, AbilityData.baseMaxOverload(9), 1.0e-4f);
    }

    // --- Les multiplicateurs de cout -----------------------------------

    @Test
    @DisplayName("cpCostScale multiplie ce qui est paye")
    void leCoutEnCpSuitLeMultiplicateur() {
        AbilityData data = learning(5, new DummySkill("skill"));
        float before = data.getControlPoint();

        Config.cpCostScale = 2.0d;
        assertTrue(data.perform(100f, 0f), "la reserve du niveau 5 paie 200");
        assertEquals(before - 200f, data.getControlPoint(), 1.0e-3f);
    }

    @Test
    @DisplayName("un cout trop cher pour la reserve est refuse, multiplicateur compris")
    void unCoutDoublePeutEtreRefuse() {
        AbilityData data = learning(5, new DummySkill("skill"));
        float before = data.getControlPoint();

        Config.cpCostScale = 2.0d;
        assertFalse(data.perform(before, 0f), "deux fois la reserve entiere ne passe pas");
        assertEquals(before, data.getControlPoint(), 1.0e-3f);

        Config.cpCostScale = 0.5d;
        assertTrue(data.perform(before, 0f), "a moitie prix, la reserve suffit");
        assertEquals(before / 2f, data.getControlPoint(), 1.0e-3f);
    }

    @Test
    @DisplayName("overloadCostScale multiplie le surcout")
    void leSurcoutSuitSonMultiplicateur() {
        AbilityData data = learning(5, new DummySkill("skill"));

        Config.overloadCostScale = 3.0d;
        assertTrue(data.perform(0f, 10f));
        assertEquals(30f, data.getOverload(), 1.0e-3f);
    }

    @Test
    @DisplayName("un cout force suit aussi les multiplicateurs")
    void lePaiementForceSuitLesMultiplicateurs() {
        // Les teleportations paient ainsi : elles ne peuvent pas echouer sur la reserve, mais
        // elles ne doivent pas pour autant echapper au reglage.
        AbilityData data = learning(1, new DummySkill("skill"));

        Config.cpCostScale = 2.0d;
        Config.overloadCostScale = 2.0d;
        data.performForced(100f, 5f);

        assertEquals(AbilityData.baseMaxControlPoint(1) - 200f, data.getControlPoint(), 1.0e-3f);
        assertEquals(10f, data.getOverload(), 1.0e-3f);
    }

    // --- Recharge et experience ----------------------------------------

    @Test
    @DisplayName("cooldownScale multiplie la duree de la recharge")
    void laRechargeSuitSonMultiplicateur() {
        DummySkill skill = new DummySkill("skill");
        AbilityData data = learning(1, skill);

        Config.cooldownScale = 2.0d;
        data.setCooldown(skill, 100);

        assertEquals(200, data.getCooldown(skill));
        assertEquals(200, data.getCooldownTotal(skill),
                "le gris de l'icone doit suivre la duree REELLE, pas la duree de base");
    }

    @Test
    @DisplayName("une recharge multipliee ne tombe jamais a zero")
    void uneRechargeResteAuMoinsUnTick() {
        DummySkill skill = new DummySkill("skill");
        AbilityData data = learning(1, skill);

        Config.cooldownScale = 0.001d;
        data.setCooldown(skill, 10);

        assertEquals(1, data.getCooldown(skill), "poser une recharge doit laisser un compteur");
    }

    @Test
    @DisplayName("expScale multiplie l'experience gagnee")
    void lExperienceSuitSonMultiplicateur() {
        DummySkill skill = new DummySkill("skill");
        AbilityData data = learning(1, skill);

        Config.expScale = 2.0d;
        data.addSkillExp(skill, 0.25f);

        assertEquals(0.5f, data.getSkillExp(skill), 1.0e-3f);
    }

    // --- L'interrupteur par competence ---------------------------------

    @Test
    @DisplayName("une competence citee par son nom court ne se lance plus")
    void leNomCourtSuffitADesactiver() {
        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("arc_gen");
        category.add(skill);

        assertTrue(skill.isEnabled(), "par defaut, tout est actif");

        Config.disabledSkills = List.of("arc_gen");
        assertFalse(skill.isEnabled());
        assertFalse(skill.canControl(), "une competence coupee ne se range pas sur une touche");
    }

    @Test
    @DisplayName("le nom complet, categorie comprise, marche aussi")
    void leNomCompletDesactiveAussi() {
        TestCategory category = new TestCategory("electromaster");
        DummySkill skill = new DummySkill("arc_gen");
        category.add(skill);

        Config.disabledSkills = List.of("electromaster.arc_gen");
        assertFalse(skill.isEnabled());
    }

    @Test
    @DisplayName("un nom complet ne coupe que sa categorie")
    void leNomCompletNeCoupePasLesAutresCategories() {
        TestCategory one = new TestCategory("electromaster");
        TestCategory other = new TestCategory("meltdowner");
        DummySkill skill = new DummySkill("arc_gen");
        DummySkill sameName = new DummySkill("arc_gen");
        one.add(skill);
        other.add(sameName);

        Config.disabledSkills = List.of("electromaster.arc_gen");
        assertFalse(skill.isEnabled());
        assertTrue(sameName.isEnabled(), "l'autre categorie garde sa competence");
    }

    @Test
    @DisplayName("un nom inconnu ne coupe rien")
    void unNomInconnuNeCoupeRien() {
        TestCategory category = new TestCategory("test");
        DummySkill skill = new DummySkill("arc_gen");
        category.add(skill);

        Config.disabledSkills = List.of("arc-gen", "ArcGen", "electromaster.arc");
        assertTrue(skill.isEnabled(), "les noms sont ceux de la declaration, a la lettre pres");
    }
}
