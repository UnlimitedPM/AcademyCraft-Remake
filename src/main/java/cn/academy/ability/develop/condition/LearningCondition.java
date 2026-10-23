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

    /**
     * Vrai quand la reponse depend de la machine utilisee.
     *
     * <p>Une seule condition est dans ce cas : celle qui demande une qualite de developpeur. Les
     * autres ne parlent que du joueur — son niveau, ses dependances — et se repondent donc partout,
     * meme la ou il n'y a aucune machine. C'est ce que l'ecran du terminal a besoin de savoir :
     * l'application du skill tree n'a pas de developpeur, et lui en passer un faux ferait afficher
     * une exigence qui n'a pas ete evaluee.
     */
    default boolean needsDeveloper() {
        return false;
    }

    /** Pourquoi la condition n'est pas remplie, dans la langue du joueur. */
    Component describe(Skill skill);
}
