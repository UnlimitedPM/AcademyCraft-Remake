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
    private final float requiredExp;

    public ConditionDependency(Skill dependency) {
        this(dependency, 0f);
    }

    public ConditionDependency(Skill dependency, float requiredExp) {
        this.dependency = dependency;
        this.requiredExp = requiredExp;
    }

    public Skill getDependency() {
        return dependency;
    }

    /**
     * Part de l'experience a atteindre dans la competence parente.
     *
     * Reprend {@code DevConditionDep.requiredExp} de l'original, ou une parente
     * declaree sans seuil valait 0 — donc « apprise » suffisait. L'original n'en
     * demandait pas toujours : le railgun voulait 30 % du thunder bolt, la detection
     * de minerais toute l'experience de la manipulation d'un bloc. Les seuils portes
     * sont figes par {@code PortedSkillsTest}.
     */
    public float getRequiredExp() {
        return requiredExp;
    }

    @Override
    public boolean accepts(AbilityData data, Skill skill) {
        return data.isSkillLearned(dependency) && data.getSkillExp(dependency) >= requiredExp;
    }

    @Override
    public Component describe(Skill skill) {
        if (requiredExp <= 0f) {
            return Component.translatable("academy.learn.dependency", dependency.getDisplayName());
        }
        return Component.translatable("academy.learn.dependency_exp",
                dependency.getDisplayName(), Math.round(requiredExp * 100f));
    }
}
