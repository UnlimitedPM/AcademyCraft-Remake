package cn.academy.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Les lignes du menu F4, celui de l'original.
 *
 * <p>Portage de {@code DebugConsole} : ses trois etats tournent a chaque appui sur F4 — rien,
 * les <b>informations du developpeur</b>, puis l'<b>etat des competences</b>. Ce sont ses deux
 * ecrans de texte, tels quels, y compris ses largeurs de colonne.
 *
 * <p>Les nombres de points de controle et de surcharge ne se lisent <b>que</b> la : l'original
 * n'a jamais rien affiche de tel ailleurs, et surtout pas dans un coin de l'ecran.
 *
 * <p>Trois ecarts, assumes et dits plutot qu'inventes : le port n'a pas d'etat « activated »
 * (rien de tel n'a ete porte), il n'a pas de maximum de CP separe en « brut + ajoute » (seule
 * la surcharge en a un), et comme il autorise plusieurs categories, l'ecran montre la plus
 * haute — alors que l'original n'en avait qu'une.
 *
 * <p>Aucun type Minecraft ici : ce sont des chaines, relisibles en JUnit.
 */
public final class DebugConsoleLines {

    /** La premiere ligne des deux ecrans. */
    public static final String TITLE = "AcademyCraft developer info";

    /** Ce que l'original ecrit quand le joueur n'a aucune categorie. */
    public static final String NO_CATEGORY = "Ability not acquired";

    /** Le mot de la deuxieme ligne de l'ecran des competences. */
    public static final String SKILL_STATUS = "Skill status";

    /** Ce que l'original ecrit pour une competence non apprise. */
    public static final String NOT_LEARNED = "[not learned]";

    /** La largeur de la colonne des noms de competences, chez l'original. */
    public static final int SKILL_COLUMN = 30;

    private DebugConsoleLines() {
    }

    /**
     * L'ecran d'informations, ou {@code category} vaut {@code null} si le joueur n'en a aucune.
     */
    public static List<String> info(AbilityData data, Category category) {
        List<String> lines = new ArrayList<>();
        lines.add(TITLE);
        if (category == null) {
            lines.add(NO_CATEGORY);
            return lines;
        }

        float add = data.getAddMaxOverload();

        lines.add(category.getName());
        lines.add("Level " + data.getCategoryLevel(category));
        lines.add(String.format(Locale.ROOT, "CP:       %.0f/%.0f",
                data.getControlPoint(), data.getMaxControlPoint()));
        lines.add(String.format(Locale.ROOT, "Overload: %.0f/%.0f(%.1f+%.1f)",
                data.getOverload(), data.getMaxOverload(), data.getMaxOverload() - add, add));
        lines.add("CPData.canUseAbility: " + canUseAbility(data));
        lines.add("CPData.overloaded: " + data.isOverloaded());
        lines.add("CPData.addMaxOverload: " + add);
        lines.add("CPData.interfering: " + data.isInterfered());
        lines.add(String.format(Locale.ROOT, " AData.levelProgress: %.2f%%",
                data.getLevelProgress(category) * 100.0f));
        return lines;
    }

    /** L'ecran des competences : leur nom, aligne, et leur experience ou leur absence. */
    public static List<String> skills(AbilityData data, Category category) {
        List<String> lines = new ArrayList<>();
        lines.add(TITLE);
        lines.add(SKILL_STATUS);
        if (category == null) {
            return lines;
        }

        for (Skill skill : category.getSkills()) {
            String name = skill.getName();
            StringBuilder row = new StringBuilder(name);
            for (int i = name.length(); i < SKILL_COLUMN; i++) {
                row.append(' ');
            }
            row.append(data.isSkillLearned(skill)
                    ? String.format(Locale.ROOT, "%.1f%%", data.getSkillExp(skill) * 100.0f)
                    : NOT_LEARNED);
            lines.add(row.toString());
        }
        return lines;
    }

    /**
     * Le joueur peut-il se servir de ses competences ?
     *
     * <p>C'est la regle de {@code CPData.canUseAbility}, que le paquet applique a l'appui : ni
     * surcharge pleine, ni brouillage.
     */
    public static boolean canUseAbility(AbilityData data) {
        return !data.isOverloaded() && !data.isInterfered();
    }
}
