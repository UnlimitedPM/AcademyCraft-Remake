package cn.academy.ability.develop.condition;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.DeveloperType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

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
        // Sans machine, la question n'a pas de reponse : l'application du skill tree n'en a pas, et
        // elle ecarte donc cette condition avant de la poser (voir {@link #needsDeveloper()}). Ce
        // garde est la pour que l'oubli ne tue plus le jeu — le joueur a plante en cliquant sur une
        // competence du terminal, sur un `developer.ordinal()` appele sur rien.
        if (developer == null) return false;
        return developer.ordinal() >= required.ordinal();
    }

    @Override
    public boolean needsDeveloper() {
        return true;
    }

    @Override
    public Component describe(Skill skill) {
        return Component.translatable("academy.learn.developer",
                Component.translatable(required.nameKey()));
    }

    /**
     * L'icone de la machine exigee.
     *
     * <p>Le {@code getIcon} de {@code DevConditionDeveloperType}, qui rendait
     * {@code type.texture} : l'image de l'objet portable, ou celle du bloc de la machine. C'est ce
     * dessin qui disait au joueur, sans un mot, devant quelle machine il devait retourner.
     */
    @Nullable
    @Override
    public ResourceLocation getIcon() {
        return required.getIcon();
    }

    /** Le nom de la machine, tel que le survol de son icone l'ecrit. */
    @Nullable
    @Override
    public Component getHint() {
        return Component.translatable(required.nameKey());
    }
}
