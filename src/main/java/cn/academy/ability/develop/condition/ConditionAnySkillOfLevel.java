package cn.academy.ability.develop.condition;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.DeveloperType;
import net.minecraft.network.chat.Component;

/**
 * Le joueur doit avoir appris <b>au moins une</b> competence d'un niveau donne.
 *
 * <p>Portage de {@code DevConditionAnySkillOfLevel} : c'est la condition des cursus
 * generiques de l'original (Brain Course, Brain Course Advanced, Mind Course), qui ne
 * demandaient pas une competence precise mais « n'importe laquelle de ce niveau-la ». Elle
 * n'aurait aucun sens pour une competence ordinaire, dont la place dans l'arbre dit deja ce
 * qu'il faut savoir.
 */
public final class ConditionAnySkillOfLevel implements LearningCondition {

    private final int level;

    public ConditionAnySkillOfLevel(int level) {
        this.level = level;
    }

    @Override
    public boolean accepts(AbilityData data, Skill skill, DeveloperType developer) {
        if (data == null || skill == null || skill.getCategory() == null) return false;
        // L'original parcourait les competences DU NIVEAU de sa categorie et s'arretait a la
        // premiere apprise. Le port lit les apprises de la categorie : le resultat est le
        // meme, et il n'y a pas de categorie a fouiller deux fois.
        for (Skill candidate : data.getLearnedSkills(skill.getCategory())) {
            if (candidate.getLevel() == level) return true;
        }
        return false;
    }

    @Override
    public Component describe(Skill skill) {
        return Component.translatable("academy.learn.any_skill_of_level", level);
    }

    /** Le niveau demande, pour les tests et l'ecran. */
    public int getLevel() {
        return level;
    }
}
