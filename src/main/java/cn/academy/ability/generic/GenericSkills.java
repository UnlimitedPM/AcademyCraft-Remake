package cn.academy.ability.generic;

import cn.academy.AcademyCraft;
import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.condition.ConditionAnySkillOfLevel;
import cn.academy.ability.develop.condition.LearningCondition;
import net.minecraft.resources.ResourceLocation;

/**
 * Les trois cursus generiques de l'original, offerts a <b>toutes</b> les categories.
 *
 * <p>Portage de {@code VanillaCategories.addGenericSkills} : chaque categorie terminait son
 * constructeur en ajoutant Brain Course (niveau 3), Brain Course Advanced (niveau 4) et Mind
 * Course (niveau 5). Ce sont des <b>passifs</b> : ils ne se lancent pas, ne se rangent pas sur
 * une touche, et donnent un bonus permanent tant qu'ils sont appris — le joueur a signale leur
 * absence, et il avait raison : sans eux, un niveau 3, 4 ou 5 n'offrait que la moitie de ce
 * qu'il promettait.
 *
 * <p>Les valeurs sont celles de l'original, au point pres : {@code +1000} de plafond de
 * reserve, {@code +1500} et {@code +100} de surcout, et {@code x1.2} sur la recuperation.
 * L'original les posait depuis un bus d'evenements ({@code CalcEvent}) ; le port les lit dans
 * les crochets de la competence (voir {@link Skill#getMaxControlPointBonus}).
 */
public final class GenericSkills {

    private GenericSkills() {}

    /**
     * Ajoute les trois a une categorie, dans l'ordre et avec les liens de l'original.
     *
     * <p>Des instances <b>neuves</b> a chaque appel : une competence n'appartient qu'a une
     * categorie (elle y est liee par son identifiant), donc les quatre categories ont chacune
     * les leurs — c'est ce que faisait deja l'original, qui construisait trois
     * {@code new Skill...()} par categorie.
     */
    public static void addTo(Category category) {
        Skill brain = new BrainCourse();
        Skill advanced = new BrainCourseAdvanced();
        Skill mind = new MindCourse();

        category.addSkill(brain);
        category.addSkill(advanced);
        category.addSkill(mind);

        // L'original les enchaine : le cours avance demande le premier, l'entrainement mental
        // demande le cours avance. Chacun exige aussi d'avoir appris UNE competence du niveau
        // correspondant (voir ConditionAnySkillOfLevel), et le niveau de la categorie que le
        // constructeur de Skill pose deja.
        advanced.setParent(brain);
        mind.setParent(advanced);
    }

    /**
     * Ce qu'une competence generique a en commun.
     *
     * <p>Elle ne se lance pas, ne se range pas sur une touche, et son nom vit dans le dossier
     * {@code generic} de l'original : sa cle de langue et son icone ne dependent donc pas de la
     * categorie qui l'accueille — les images sont deja livrees sous
     * {@code textures/abilities/generic/skills/}.
     */
    public abstract static class GenericSkill extends Skill {

        protected GenericSkill(String name, int level, LearningCondition condition) {
            super(name, level);
            addCondition(condition);
        }

        @Override
        public boolean isPassive() {
            return true;
        }

        @Override
        public boolean canControl() {
            return false;
        }

        @Override
        public String getDisplayKey() {
            return "ac.ability.generic." + getName() + ".name";
        }

        @Override
        public ResourceLocation getHintIcon() {
            return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                    "textures/abilities/generic/skills/" + getName() + ".png");
        }
    }

    /** Le cours de cerveau : 1000 points de reserve en plus, tant qu'il est appris. */
    public static final class BrainCourse extends GenericSkill {

        /** Ce qu'il ajoute au plafond, tel quel dans l'original. */
        public static final float MAX_CP_BONUS = 1000f;

        public BrainCourse() {
            super("brain_course", 3, new ConditionAnySkillOfLevel(3));
        }

        @Override
        public float getMaxControlPointBonus(AbilityData data) {
            return MAX_CP_BONUS;
        }
    }

    /** Le cours de cerveau avance : 1500 de reserve, et 100 de surcout en plus. */
    public static final class BrainCourseAdvanced extends GenericSkill {

        public static final float MAX_CP_BONUS = 1500f;
        public static final float MAX_OVERLOAD_BONUS = 100f;

        public BrainCourseAdvanced() {
            super("brain_course_advanced", 4, new ConditionAnySkillOfLevel(4));
        }

        @Override
        public float getMaxControlPointBonus(AbilityData data) {
            return MAX_CP_BONUS;
        }

        @Override
        public float getMaxOverloadBonus(AbilityData data) {
            return MAX_OVERLOAD_BONUS;
        }
    }

    /** L'entrainement mental : la reserve remonte 20 % plus vite. */
    public static final class MindCourse extends GenericSkill {

        public static final float RECOVER_SCALE = 1.2f;

        public MindCourse() {
            super("mind_course", 5, new ConditionAnySkillOfLevel(5));
        }

        @Override
        public float getControlPointRecoverScale(AbilityData data) {
            return RECOVER_SCALE;
        }
    }
}
