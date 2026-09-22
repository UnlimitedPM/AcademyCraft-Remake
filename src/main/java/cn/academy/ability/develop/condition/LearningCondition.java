package cn.academy.ability.develop.condition;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.DeveloperType;
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
     * @param data      l'etat du joueur
     * @param skill     la competence visee, dont depend la reponse pour une condition
     *                  de niveau
     * @param developer la qualite de la machine utilisee : l'original passait le
     *                  developeur entier ({@code IDeveloper}), dont seule cette qualite
     *                  servait. Le portable est simplement une autre facon de la fournir.
     */
    boolean accepts(AbilityData data, Skill skill, DeveloperType developer);

    /** Pourquoi la condition n'est pas remplie, dans la langue du joueur. */
    Component describe(Skill skill);
}
