package cn.academy.ability;

import java.util.ArrayList;
import java.util.List;

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
     * Les compétences qui creusent ou cassent, par clé, dans l'ordre de l'écran.
     *
     * <p>Des clés plutôt que les constantes des catégories : ce fichier est commun aux trois
     * chargeurs, et les catégories des uns ne se nomment pas dans le code des autres. La clé est
     * celle qui voyage déjà dans le paquet et dort dans les NBT ({@link #key}).
     *
     * <p>La mine ray y figure pour la raison inverse des autres : elle creusait <b>toujours</b>,
     * sans même regarder le verrou du serveur, et c'est ce réglage qui lui en donne un.
     */
    private static final String[] PREF_KEYS = {
            "electromaster.railgun",
            "meltdowner.meltdowner",
            "meltdowner.mine_ray_basic",
            "meltdowner.mine_ray_expert",
            "meltdowner.mine_ray_luck",
            "vecmanip.dir_blast",
            "vecmanip.ground_shock",
            "vecmanip.plasma_cannon",
            "vecmanip.storm_wing",
    };

    /**
     * Les compétences qui creusent ou cassent — donc les seules qui méritent un sous-réglage.
     *
     * <p>Une clé que le serveur ne connaît pas (catégorie absente, compétence renommée) est
     * simplement ignorée : l'écran montre une case de moins au lieu d'échouer.
     */
    public static List<Skill> prefSkills() {
        List<Skill> skills = new ArrayList<>(PREF_KEYS.length);
        for (String key : PREF_KEYS) {
            Skill skill = byKey(key);
            if (skill != null) skills.add(skill);
        }
        return skills;
    }

    /** La clé d'une compétence, telle qu'elle voyage dans le paquet et dort dans les NBT. */
    public static String key(Skill skill) {
        return skill.getCategory().getName() + "." + skill.getName();
    }

    /** La compétence d'une clé, ou {@code null} si le serveur ne la connaît pas. */
    public static Skill byKey(String key) {
        return byKey(CategoryManager.INSTANCE, key);
    }

    /**
     * Le meme, dans un registre donne.
     *
     * <p>Le registre est un parametre pour que les tests partent d'un etat propre : les
     * competences du serveur ne sont enregistrees qu'au demarrage du mod.
     */
    public static Skill byKey(CategoryManager manager, String key) {
        if (key == null || key.isEmpty()) return null;
        int dot = key.indexOf('.');
        if (dot <= 0 || dot == key.length() - 1) return null;
        Category category = manager.getCategory(key.substring(0, dot));
        if (category == null) return null;
        return category.getSkill(key.substring(dot + 1));
    }
}
