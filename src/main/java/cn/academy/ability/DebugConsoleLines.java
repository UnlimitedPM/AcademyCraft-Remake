package cn.academy.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

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

    /** Le mot devant le niveau d'aptitude. */
    public static final String LEVEL = "Level %s";

    /** Les libelles des mesures, en anglais ; les autres langues les traduisent. */
    public static final String CP = "CP:";
    public static final String OVERLOAD = "Overload:";
    public static final String CAN_USE = "Can use ability:";
    public static final String ACTIVATED = "Ability on:";
    public static final String ADD_CP = "CP bonus:";
    public static final String INTERFERING = "Jammed:";
    public static final String PROGRESS = "Level progress:";

    /**
     * Les cles de langue de ces textes.
     *
     * <p>Le menu F4 restait anglais : ses mots etaient des constantes, et rien ne les traduisait.
     * Le joueur a demande qu'il soit traduit EN ENTIER — « le melange entre francais et anglais me
     * fait penser qu'il aurait ete plus simple de laisser en anglais direct ». Le nom de la
     * categorie et celui des competences passent donc par leurs cles habituelles
     * ({@code getDisplayKey}), et les mesures par les leurs.
     */
    public static final String KEY_TITLE = "ac.debug.title";
    public static final String KEY_NO_CATEGORY = "ac.debug.no_category";
    public static final String KEY_SKILL_STATUS = "ac.debug.skill_status";
    public static final String KEY_NOT_LEARNED = "ac.debug.not_learned";
    public static final String KEY_LEVEL = "ac.debug.level";
    public static final String KEY_CP = "ac.debug.cp";
    public static final String KEY_OVERLOAD = "ac.debug.overload";
    public static final String KEY_CAN_USE = "ac.debug.can_use";
    public static final String KEY_ACTIVATED = "ac.debug.activated";
    public static final String KEY_ADD_CP = "ac.debug.add_cp";
    public static final String KEY_INTERFERING = "ac.debug.interfering";
    public static final String KEY_PROGRESS = "ac.debug.progress";
    public static final String KEY_YES = "ac.debug.yes";
    public static final String KEY_NO = "ac.debug.no";

    /** La largeur de la colonne des noms de competences, chez l'original. */
    public static final int SKILL_COLUMN = 30;

    /** La colonne ou commencent les valeurs d'une mesure, libelle compris. */
    public static final int LABEL_COLUMN = 22;

    private DebugConsoleLines() {
    }

    /**
     * Un texte, traduit si un traducteur est donne, et son texte anglais sinon.
     *
     * <p>C'est ce qui permet a cette classe de rester sans type Minecraft : les tests unitaires
     * passent {@code null} et lisent l'anglais, le client passe {@code Component.translatable}.
     */
    private static String text(String key, String english, Function<String, String> translate) {
        return translate == null ? english : translate.apply(key);
    }

    /**
     * L'ecran d'informations, ou {@code category} vaut {@code null} si le joueur n'en a aucune.
     */
    public static List<String> info(AbilityData data, Category category) {
        return info(data, category, null);
    }

    /** Le meme, avec les mots traduits par la fonction donnee. */
    public static List<String> info(AbilityData data, Category category,
                                    Function<String, String> translate) {
        List<String> lines = new ArrayList<>();
        lines.add(text(KEY_TITLE, TITLE, translate));
        if (category == null) {
            lines.add(text(KEY_NO_CATEGORY, NO_CATEGORY, translate));
            return lines;
        }

        float addCp = data.getAddMaxControlPoint();
        float addOverload = data.getAddMaxOverload();

        // Le nom de la categorie se traduit comme dans l'arbre des competences.
        lines.add(translate == null ? category.getName() : translate.apply(category.getDisplayKey()));
        lines.add(String.format(Locale.ROOT, text(KEY_LEVEL, LEVEL, translate),
                Integer.toString(data.getCategoryLevel(category))));
        lines.add(measure(text(KEY_CP, CP, translate),
                String.format(Locale.ROOT, "%.0f/%.0f(%.1f+%.1f)",
                        data.getControlPoint(), data.getMaxControlPoint(),
                        data.getRawMaxControlPoint(), addCp)));
        lines.add(measure(text(KEY_OVERLOAD, OVERLOAD, translate),
                String.format(Locale.ROOT, "%.0f/%.0f(%.1f+%.1f)",
                        data.getOverload(), data.getMaxOverload(),
                        data.getMaxOverload() - addOverload, addOverload)));
        lines.add(measure(text(KEY_CAN_USE, CAN_USE, translate),
                yesNo(canUseAbility(data), translate)));
        lines.add(measure(text(KEY_ACTIVATED, ACTIVATED, translate),
                yesNo(data.isActivated(), translate)));
        lines.add(measure(text(KEY_ADD_CP, ADD_CP, translate), decimal(addCp)));
        lines.add(measure(text(KEY_INTERFERING, INTERFERING, translate),
                yesNo(data.isInterfered(), translate)));
        lines.add(measure(text(KEY_PROGRESS, PROGRESS, translate),
                String.format(Locale.ROOT, "%.2f%%", data.getLevelProgress(category) * 100.0f)));
        return lines;
    }

    /**
     * Une mesure, son libelle aligne a la colonne des valeurs.
     *
     * <p>L'original ecrivait ses espaces a la main dans chaque format ({@code "CP:       "}); avec
     * des libelles traduits, leurs longueurs changent, donc la colonne se calcule. Un libelle plus
     * long que la colonne pousse la valeur au lieu d'etre coupe : rien ne doit disparaitre.
     */
    private static String measure(String label, String value) {
        StringBuilder out = new StringBuilder(label);
        out.append(" ".repeat(Math.max(1, LABEL_COLUMN - label.length())));
        return out.append(value).toString();
    }

    /** Un booleen, dans la langue du joueur. */
    private static String yesNo(boolean value, Function<String, String> translate) {
        return value ? text(KEY_YES, "true", translate) : text(KEY_NO, "false", translate);
    }

    /**
     * Un nombre, avec au plus cinq decimales et sans zeros inutiles.
     *
     * <p>Les ajouts grandissent par petites touches : a la septieme decimale ils ne disent plus
     * rien a personne, et l'original les montrait avec une seule. Cinq suffisent a voir la
     * progression, et {@code 0.7162399} s'ecrit alors {@code 0.71624} au lieu de noyer la ligne.
     */
    public static String decimal(float value) {
        String text = String.format(Locale.ROOT, "%.5f", value);
        if (text.indexOf('.') < 0) return text;
        text = text.replaceAll("0+$", "");
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }

    /** L'ecran des competences : leur nom, aligne, et leur experience ou leur absence. */
    public static List<String> skills(AbilityData data, List<Category> categories) {
        return skills(data, categories, null);
    }

    /** Le meme, avec les mots traduits par la fonction donnee. */
    public static List<String> skills(AbilityData data, List<Category> categories,
                                      Function<String, String> translate) {
        List<String> lines = new ArrayList<>();
        lines.add(text(KEY_TITLE, TITLE, translate));
        lines.add(text(KEY_SKILL_STATUS, SKILL_STATUS, translate));
        String notLearned = text(KEY_NOT_LEARNED, NOT_LEARNED, translate);

        // L'original n'avait qu'une categorie, donc une seule liste sans titre. Le port en
        // autorise plusieurs : quand il y en a plus d'une, chacune annonce la sienne, sinon la
        // liste serait un melange sans repere — et des competences sembleraient manquer.
        boolean named = categories.size() > 1;
        for (Category category : categories) {
            if (named) {
                lines.add(translate == null
                        ? category.getName()
                        : translate.apply(category.getDisplayKey()));
            }
            for (Skill skill : category.getSkills()) {
                // Le nom AFFICHE, celui de l'arbre des competences : sans ca, le menu montrait
                // `threatening_teleport` au milieu d'un ecran francais.
                String name = translate == null
                        ? skill.getName()
                        : translate.apply(skill.getDisplayKey());
                StringBuilder row = new StringBuilder(name);
                row.append(" ".repeat(Math.max(1, SKILL_COLUMN - name.length())));
                row.append(data.isSkillLearned(skill)
                        ? String.format(Locale.ROOT, "%.1f%%", data.getSkillExp(skill) * 100.0f)
                        : notLearned);
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
