package cn.academy.ability.develop.condition;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.DeveloperType;
import net.minecraft.network.chat.Component;

/**
 * La machine doit etre d'une qualite suffisante pour enseigner cette competence.
 *
 * <p>Portage de {@code DevConditionDeveloperType}. C'est la condition qui explique tout le
 * systeme des machines : l'original ne la posait pas a la main, il la deduisait du
 * <b>niveau</b> de la competence — les niveaux 1 et 2 tiennent dans l'objet portable, le 3
 * demande la machine normale, et les niveaux 4 et 5 la machine avancee. Voir
 * {@link Skill#getMinimumDeveloperType()}.
 *
 * <p>L'ordre de l'enumeration {@link DeveloperType} <b>est</b> la qualite : une machine
 * avancee sait tout ce que sait une machine modeste, et la comparaison des ordinaux le dit
 * sans table de correspondance — comme dans l'original.
 */
public final class ConditionDeveloperType implements LearningCondition {

    private final DeveloperType required;

    public ConditionDeveloperType(DeveloperType required) {
        this.required = required;
    }

    /** La qualite minimale exigee. */
    public DeveloperType getRequired() {
        return required;
    }

    @Override
    public boolean accepts(AbilityData data, Skill skill, DeveloperType developer) {
        return developer.ordinal() >= required.ordinal();
    }

    @Override
    public Component describe(Skill skill) {
        return Component.translatable("academy.learn.developer",
                Component.translatable(required.nameKey()));
    }
}
