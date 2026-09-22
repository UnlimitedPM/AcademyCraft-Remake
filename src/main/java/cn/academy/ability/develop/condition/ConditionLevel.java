package cn.academy.ability.develop.condition;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.DeveloperType;
import net.minecraft.network.chat.Component;

/**
 * Le joueur doit avoir atteint le niveau de la competence dans sa categorie.
 *
 * Portage de {@code DevConditionLevel}. La seule adaptation est la ou le port
 * differe de l'original : celui-ci ne connaissait qu'une categorie par joueur, donc
 * un seul niveau a consulter. Le port tient un niveau par categorie, et la
 * condition lit donc celui de la categorie de la competence visee.
 *
 * Cette condition est posee sur <b>toutes</b> les competences, y compris celles de
 * niveau 0 qui sont donc toujours accessibles — c'est ce que faisait le
 * constructeur de {@code Skill} dans l'original.
 */
public final class ConditionLevel implements LearningCondition {

    public static final ConditionLevel INSTANCE = new ConditionLevel();

    private ConditionLevel() {}

    @Override
    public boolean accepts(AbilityData data, Skill skill, DeveloperType developer) {
        // La machine ne dit rien du niveau : c'est le joueur qui apprend.
        return data.getCategoryLevel(skill.getCategory()) >= skill.getLevel();
    }

    @Override
    public Component describe(Skill skill) {
        return Component.translatable("academy.learn.level", skill.getLevel());
    }
}
