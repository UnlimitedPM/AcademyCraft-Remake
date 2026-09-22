package cn.academy.ability.develop;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.condition.LearningCondition;

/**
 * Les regles d'apprentissage des competences.
 *
 * Portage de {@code LearningHelper}. Disponible des deux cotes : le client s'en
 * sert pour griser les lignes de l'ecran, le serveur pour refuser un
 * apprentissage. C'est le meme code, donc les deux ne peuvent pas se contredire —
 * ce qui est exactement la raison d'etre de cette classe.
 */
public final class LearningHelper {

    private LearningHelper() {}

    /**
     * Le joueur peut-il apprendre cette competence maintenant ?
     *
     * L'original passait aussi le developeur, pour une condition sur son type. Le
     * port n'a pas encore cette condition — elle demanderait le developeur portable,
     * qui n'existe pas ici — donc l'apprentissage ne depend que du joueur. C'est la
     * signature qu'il faudra rouvrir pour la condition de type.
     */
    public static boolean canLearn(AbilityData data, Skill skill) {
        for (LearningCondition condition : skill.getConditions()) {
            if (!condition.accepts(data, skill)) return false;
        }
        return true;
    }

    /**
     * La competence vaut-elle la peine d'etre montree ?
     *
     * Reprend {@code canBePotentiallyLearned} : on montre une competence si le
     * joueur a le niveau pour elle, s'il l'a deja apprise, ou si la competence dont
     * elle depend est apprise — autrement dit la suivante de la chaine.
     *
     * Une competence <b>sans parent</b> est donc toujours montree, meme au niveau 0 :
     * c'est une racine, ce vers quoi on progresse. Les autres n'apparaissent qu'une
     * fois la chaine ouverte.
     */
    public static boolean canBePotentiallyLearned(AbilityData data, Skill skill) {
        if (data.getCategoryLevel(skill.getCategory()) >= skill.getLevel()) return true;
        if (data.isSkillLearned(skill)) return true;
        Skill parent = skill.getParent();
        return parent == null || data.isSkillLearned(parent);
    }

    /** La premiere condition qui bloque, ou {@code null} si la competence est accessible. */
    public static LearningCondition firstBlocker(AbilityData data, Skill skill) {
        for (LearningCondition condition : skill.getConditions()) {
            if (!condition.accepts(data, skill)) return condition;
        }
        return null;
    }
}
