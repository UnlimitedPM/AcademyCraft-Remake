package cn.academy.ability.develop.condition;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.network.chat.Component;

/**
 * Une condition a remplir pour apprendre une competence.
 *
 * Portage de {@code IDevCondition}. L'original y rangeait aussi de quoi dessiner
 * un indice dans l'arbre : une icone, un texte, et un drapeau « afficher ou non ».
 * Ici une condition sait seulement si elle est remplie et comment le dire ; c'est
 * l'ecran qui decide de la place qu'il donne a cette phrase.
 *
 * Chaque condition est <b>immutable</b> et posee une fois pour toutes a la
 * construction de la competence, comme dans l'original ou {@code addDevCondition}
 * n'etait appele qu'au chargement des categories.
 */
public interface LearningCondition {

    /**
     * La condition est-elle remplie ?
     *
     * @param data  l'etat du joueur
     * @param skill la competence visee, dont depend la reponse pour une condition
     *              de niveau
     */
    boolean accepts(AbilityData data, Skill skill);

    /** Pourquoi la condition n'est pas remplie, dans la langue du joueur. */
    Component describe(Skill skill);
}
