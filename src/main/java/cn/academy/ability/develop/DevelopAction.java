package cn.academy.ability.develop;

import net.minecraft.world.entity.player.Player;

/**
 * Un apprentissage que le developeur peut mener a bien.
 *
 * Portage de {@code IDevelopAction} de la 1.12.2, allege de son parametre
 * {@code IDeveloper} : dans l'original, la seule action portee ici ne s'en servait
 * que pour lire le type du developeur, et la seule verification qui en dependait
 * ne regardait en fait que l'etat du joueur. Le parametre n'apportait donc rien
 * qu'un risque de dependance circulaire.
 *
 * Un apprentissage se deroule en <b>stimulations</b> : chacune dure
 * {@code tps} ticks et coute {@code cps} unites d'energie, reparties sur ces
 * ticks. C'est l'unite de temps et de prix de tout le systeme.
 */
public interface DevelopAction {

    /** Nombre de stimulations necessaires a cet apprentissage. */
    int getStimulations(Player player);

    /**
     * Vrai si l'apprentissage peut aboutir. Verifie a la fin, pas au debut : dans
     * l'original le joueur pouvait lancer un apprentissage qui devenait impossible
     * en cours de route, et il echouait alors en perdant ce qu'il avait paye.
     */
    boolean validate(Player player);

    /** Applique l'apprentissage. Appele une seule fois, apres une validation reussie. */
    void onLearned(Player player);
}
