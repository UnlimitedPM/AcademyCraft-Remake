package cn.academy.ability;

import java.util.ArrayList;
import java.util.List;

/**
 * Les lignes du menu F4, celui de l'original.
 *
 * <p>Portage de {@code DebugConsole} : ses trois etats tournent a chaque appui sur F4 — rien,
 * les <b>informations du developpeur</b>, puis l'<b>etat des competences</b>. Ce sont ses deux
 * ecrans de texte, <b>mot pour mot</b> : ses libelles, ses espaces, ses noms internes
 * ({@code electromaster}, {@code arc_gen}) et ses nombres non arrondis.
 *
 * <p>Le port les avait TRADUITS, avec des colonnes calculees ; le joueur a vu tout de suite que
 * ce n'etait plus ce menu-la (« la maintenant le menu F4 ne ressemble plus au vrai menu F4 »).
 * C'est un ecran de developpeur : il se lit dans les captures de l'original, pas dans une
 * traduction, et ses noms bruts SONT ce qu'il montre.
 *
 * <p>Les nombres de points de controle et de surcharge ne se lisent <b>que</b> la : l'original
 * n'a jamais rien affiche de tel ailleurs, et surtout pas dans un coin de l'ecran.
 *
 * <p>Trois ecarts, assumes et dits plutot qu'inventes : le port n'avait pas d'etat « activated »
 * quand cette classe est nee (il en a un depuis), il n'a pas d'accesseur de maximum de surcharge
 * « brut » (il le calcule), et comme il autorise plusieurs categories, l'ecran montre la plus
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

    /**
     * Les libelles des mesures, avec leurs espaces, EXACTEMENT comme l'original les ecrivait
     * dans ses formats ({@code "CP:       %.0f/..."} en compte sept).
     */
    public static final String CP_LABEL = "CP:       ";
    public static final String OVERLOAD_LABEL = "Overload: ";
    public static final String CAN_USE_LABEL = "CPData.canUseAbility: ";
    public static final String ACTIVATED_LABEL = "CPData.activated: ";
    public static final String ADD_CP_LABEL = "CPData.addMaxCP: ";
    public static final String INTERFERING_LABEL = "CPData.interfering: ";
    public static final String PROGRESS_LABEL = " AData.levelProgress: ";

    /** La largeur de la colonne des noms de competences, chez l'original. */
    public static final int SKILL_COLUMN = 30;

    private DebugConsoleLines() {
    }

    /**
     * L'ecran d'informations, ou {@code category} vaut {@code null} si le joueur n'en a aucune.
     *
     * <p>Les nombres passent par {@link String#format} SANS locale, comme chez l'original : sur
     * un systeme francais ils sortent donc avec une virgule (« 33,33% »), et c'est ce que le
     * joueur voyait dans le vrai mod. Le forcer en point changerait l'ecran qu'il connait.
     */
    public static List<String> info(AbilityData data, Category category) {
        List<String> lines = new ArrayList<>();
        lines.add(TITLE);
        if (category == null) {
            lines.add(NO_CATEGORY);
            return lines;
        }

        float addCp = data.getAddMaxControlPoint();
        float addOverload = data.getAddMaxOverload();

        lines.add(category.getName());
        lines.add("Level " + data.getCategoryLevel(category));
        lines.add(CP_LABEL + String.format("%.0f/%.0f(%.1f+%.1f)",
                data.getControlPoint(), data.getMaxControlPoint(),
                data.getRawMaxControlPoint(), addCp));
        lines.add(OVERLOAD_LABEL + String.format("%.0f/%.0f(%.1f+%.1f)",
                data.getOverload(), data.getMaxOverload(),
                data.getMaxOverload() - addOverload, addOverload));
        lines.add(CAN_USE_LABEL + canUseAbility(data));
        lines.add(ACTIVATED_LABEL + data.isActivated());
        lines.add(ADD_CP_LABEL + addCp);
        lines.add(INTERFERING_LABEL + data.isInterfered());
        lines.add(PROGRESS_LABEL + String.format("%.2f%%", data.getLevelProgress(category) * 100.0f));
        return lines;
    }

    /**
     * L'ecran des competences : leur nom, garni jusqu'a trente caracteres, puis leur experience
     * ou l'aveu qu'elles ne sont pas apprises.
     *
     * <p>L'original ecrivait {@code s.getName()} : le nom INTERNE, pas celui de l'arbre. C'est
     * ce qui fait de cet ecran un ecran de developpeur, et le port l'avait perdu en le traduisant.
     */
    public static List<String> skills(AbilityData data, List<Category> categories) {
        List<String> lines = new ArrayList<>();
        lines.add(TITLE);
        lines.add(SKILL_STATUS);

        // L'original n'avait qu'une categorie, donc une seule liste sans titre. Le port en
        // autorise plusieurs : quand il y en a plus d'une, chacune annonce la sienne, sinon la
        // liste serait un melange sans repere — et des competences sembleraient manquer.
        boolean named = categories.size() > 1;
        for (Category category : categories) {
            if (named) lines.add(category.getName());
            for (Skill skill : category.getSkills()) {
                StringBuilder row = new StringBuilder(skill.getName());
                // Le nombre d'espaces de l'original : {@code 30 - longueur}, sans minimum. Un nom
                // plus long que la colonne pousse donc sa valeur au lieu de la voir coupee.
                for (int i = 0; i < SKILL_COLUMN - skill.getName().length(); i++) {
                    row.append(' ');
                }
                row.append(data.isSkillLearned(skill)
                        ? String.format("%.1f%%", data.getSkillExp(skill) * 100.0f)
                        : NOT_LEARNED);
                lines.add(row.toString());
            }
        }
        return lines;
    }

    /**
     * Le joueur peut-il se servir de ses competences ?
     *
     * <p>C'est la regle de {@code CPData.canUseAbility}, que le paquet applique a l'appui :
     * l'aptitude allumee, pas de surcharge pleine, pas de brouillage.
     */
    public static boolean canUseAbility(AbilityData data) {
        return data.isActivated() && !data.isOverloadRecovering() && !data.isInterfered();
    }
}
