package cn.academy.ability.develop;

import net.minecraft.world.entity.player.Player;

/**
 * Un apprentissage que le developeur peut mener a bien.
 *
 * Portage de {@code IDevelopAction} de la 1.12.2. Les deux actions portees ne
 * s'interessent qu'a une chose du developeur : sa <b>qualite</b> — une competence haute
 * demande une machine haute, et l'original le verifiait dans la validation, au dernier
 * moment, du meme cote que le reste des conditions.
 *
 * Un apprentissage se deroule en <b>stimulations</b> : chacune dure
 * {@code tps} ticks et coute {@code cps} unites d'energie, reparties sur ces
 * ticks. C'est l'unite de temps et de prix de tout le systeme.
 */
public interface DevelopAction {

    /**
     * La categorie visee.
     *
     * <p>Le port range la cible d'un apprentissage dans ces deux nombres, et les sauvegarde
     * a la place de l'action elle-meme : c'est ce qui permet a une machine rechargee de
     * savoir ce qu'elle etait en train de faire.
     */
    int getCategoryId();

    /** La competence visee, ou -1 pour le niveau de la categorie. */
    int getSkillId();

    /** Nombre de stimulations necessaires a cet apprentissage. */
    int getStimulations(Player player);

    /**
     * Vrai si l'apprentissage peut aboutir. Verifie a la fin, pas au debut : dans
     * l'original le joueur pouvait lancer un apprentissage qui devenait impossible
     * en cours de route, et il echouait alors en perdant ce qu'il avait paye.
     *
     * @param developer la qualite de la machine utilisee
     */
    boolean validate(Player player, DeveloperType developer);

    /** Applique l'apprentissage. Appele une seule fois, apres une validation reussie. */
    void onLearned(Player player);
}
