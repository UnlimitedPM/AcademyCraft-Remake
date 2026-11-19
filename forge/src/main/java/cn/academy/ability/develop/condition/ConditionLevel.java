package cn.academy.ability.develop.condition;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.DeveloperType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

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

    /**
     * La condition se cache : c'est le {@code shouldDisplay = false} de {@code DevConditionLevel}.
     *
     * <p>Le niveau du joueur se lit deja sur le panneau de gauche de la machine, et l'original
     * n'encombrait donc pas sa fiche d'une icone de plus pour le redire. La phrase, elle, reste :
     * c'est elle qu'un ecran ecrit quand il veut expliquer pourquoi la competence est fermee.
     */
    @Override
    public boolean shouldDisplay() {
        return false;
    }

    @Nullable
    @Override
    public ResourceLocation getIcon() {
        return null;
    }

    @Nullable
    @Override
    public Component getHint() {
        return null;
    }
}
