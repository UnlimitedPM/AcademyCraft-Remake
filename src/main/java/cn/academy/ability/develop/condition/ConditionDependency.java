package cn.academy.ability.develop.condition;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.network.chat.Component;

/**
 * Une competence doit avoir ete apprise avant celle-ci.
 *
 * Portage de {@code DevConditionDep}. L'original y ajoutait un seuil d'experience
 * dans la competence parente ({@code requiredExp}) ; le port n'a pas encore
 * l'experience par competence, donc la dependance se limite a « apprise ou non ».
 * Le jour ou l'experience arrivera, c'est cette classe qu'il faudra rouvrir.
 */
public final class ConditionDependency implements LearningCondition {

    private final Skill dependency;

    public ConditionDependency(Skill dependency) {
        this.dependency = dependency;
    }

    public Skill getDependency() {
        return dependency;
    }

    @Override
    public boolean accepts(AbilityData data, Skill skill) {
        return data.isSkillLearned(dependency);
    }

    @Override
    public Component describe(Skill skill) {
        return Component.translatable("academy.learn.dependency", dependency.getDisplayName());
    }
}
