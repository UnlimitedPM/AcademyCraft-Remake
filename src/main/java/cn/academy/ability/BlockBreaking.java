package cn.academy.ability;

import java.util.List;

import cn.academy.ability.electromaster.ElectromasterCategory;
import cn.academy.ability.meltdowner.MeltdownerCategory;
import cn.academy.ability.vecmanip.VecmanipCategory;

/**
 * Qui a le droit de casser un bloc.
 *
 * <p>Deux verrous, et il faut les deux :
 * <ul>
 *   <li>le <b>serveur</b>, par {@code general.destroyBlocks} — c'est la règle de la partie ;</li>
 *   <li>le <b>joueur</b>, par ses réglages — « choisir de détruire les blocs ou non ne change
 *       rien au résultat car c'est le serveur qui a l'option, sauf que si il permet de casser les
 *       blocs, les joueurs doivent quand même avoir la possibilité de faire en sorte que leur
 *       pouvoir ne casse pas les blocs ».</li>
 * </ul>
 *
 * <p>Le joueur ne peut donc que <b>retirer</b> : son réglage ne rend jamais la casse possible là
 * où le serveur l'interdit. Un réglage purement client aurait menti — le serveur casse, quoi que
 * dise l'écran — donc le choix voyage par paquet et se range dans {@link AbilityData}.
 */
public final class BlockBreaking {

    private BlockBreaking() {
    }

    /**
     * La casse est-elle permise, ici et maintenant ?
     *
     * <p>{@code data} nul vaut « aucune préférence connue » — un appelant sans joueur (une
     * machine, un test) ne se voit pas refuser par un réglage qui n'existe pas.
     */
    public static boolean allowed(AbilityData data, Skill skill) {
        if (!cn.academy.Config.destroyBlocks) return false;
        return data == null || data.breaksBlocks(skill);
    }

    /**
     * Les compétences qui creusent ou cassent — donc les seules qui méritent un sous-réglage.
     *
     * <p>L'ordre est celui de l'écran : électromaster, puis meltdowner, puis vecmanip. La mine ray
     * y figure pour la raison inverse des autres : elle creusait <b>toujours</b>, sans même
     * regarder le verrou du serveur, et c'est ce réglage qui lui en donne un.
     */
    public static List<Skill> prefSkills() {
        return List.of(
                ElectromasterCategory.RAILGUN,
                MeltdownerCategory.MELTDOWNER,
                MeltdownerCategory.MINE_RAY_BASIC,
                MeltdownerCategory.MINE_RAY_EXPERT,
                MeltdownerCategory.MINE_RAY_LUCK,
                VecmanipCategory.DIRECTED_BLASTWAVE,
                VecmanipCategory.GROUNDSHOCK,
                VecmanipCategory.PLASMA_CANNON,
                VecmanipCategory.STORM_WING);
    }

    /** La clé d'une compétence, telle qu'elle voyage dans le paquet et dort dans les NBT. */
    public static String key(Skill skill) {
        return skill.getCategory().getName() + "." + skill.getName();
    }

    /** La compétence d'une clé, ou {@code null} si le serveur ne la connaît pas. */
    public static Skill byKey(String key) {
        if (key == null || key.isEmpty()) return null;
        for (Skill skill : prefSkills()) {
            if (key.equals(key(skill))) return skill;
        }
        return null;
    }
}
