package cn.academy.command;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.Skill;

import java.util.ArrayList;
import java.util.List;

/**
 * Les sous-commandes de {@code /aim}, et les regles qui les tiennent.
 *
 * <p>Portage de {@code CommandAIMBase} : l'original avait deux racines, {@code /aim} pour le
 * joueur et {@code /aimp} pour qui vise quelqu'un d'autre, avec les memes treize
 * sous-commandes de part et d'autre. Ce fichier ne porte que ce qui se relit sans Minecraft —
 * le catalogue, les bornes, la recherche d'une competence, et la regle du <b>seul pouvoir</b>
 * (voir {@link #powersToForget}).
 *
 * <p>PUR : aucune classe du jeu. Le registre des categories lui est donne <b>en
 * parametre</b>, jamais lu : en JUnit il est vide, et une decision qui en dependrait ne
 * serait pas relisible.
 */
public final class AimCommandSpec {

    /**
     * Les sous-commandes, dans l'ordre ou l'original les listait.
     *
     * <p>C'est cet ordre que suit {@code /aim help} : le joueur y lit la meme liste que dans
     * le mod d'origine, et un test le fige.
     */
    public static final List<String> COMMANDS = List.of(
            "help", "cat", "catlist", "learn", "learn_all", "reset", "learned", "skills",
            "fullcp", "level", "exp", "cd_clear", "maxout");

    /** Bornes de {@code /aim level}, celles de l'original : 1 a 5. */
    public static final int LEVEL_MIN = 1;
    public static final int LEVEL_MAX = 5;

    /** Bornes de {@code /aim exp} : l'experience d'une competence va de 0 a 1. */
    public static final float EXP_MIN = 0f;
    public static final float EXP_MAX = 1f;

    private AimCommandSpec() {}

    /**
     * Une competence par son identifiant <b>ou</b> par son nom, comme l'original.
     *
     * <p>L'original essayait d'abord {@code Integer.parseInt} et retombait sur le nom ; le
     * port suit. Les deux formes servent : le nom se lit dans {@code /aim skills}, et
     * l'identifiant est plus court a taper.
     */
    public static Skill findSkill(Category category, String token) {
        if (category == null || token == null || token.isEmpty()) return null;
        try {
            return category.getSkill(Integer.parseInt(token));
        } catch (NumberFormatException ignored) {
            // Ce n'est pas un identifiant : c'est donc un nom de competence.
        }
        return category.getSkill(token);
    }

    /**
     * Le pouvoir en service : celui dont le niveau est le plus haut, ou {@code null} si le
     * joueur n'en porte aucun.
     *
     * <p>L'original n'avait qu'une categorie et la lisait directement. Le port en autorise
     * plusieurs (les sauvegardes d'avant la regle en portent), donc il tranche par le niveau
     * et, a niveau egal, par l'ordre du registre : deux appels rendent la meme chose, et un
     * test peut donc l'affirmer.
     */
    public static Category activePower(AbilityData data, List<Category> all) {
        if (data == null) return null;
        Category best = null;
        int bestLevel = 0;
        for (Category category : all) {
            if (category == null) continue;
            int level = data.getCategoryLevel(category);
            if (level > bestLevel) {
                bestLevel = level;
                best = category;
            }
        }
        return best;
    }

    /**
     * Les pouvoirs a oublier pour n'en garder qu'un : tous ceux du joueur, sauf celui qu'il
     * adopte.
     *
     * <p>C'est la regle de l'original, ou {@code setCategory} <b>remplacait</b> la categorie :
     * on ne peut pas porter le vecteur manipulation et l'electromaster en meme temps, il faut
     * lacher le premier. Le port en fait une etape explicite pour pouvoir le dire au joueur
     * (et vider ses prereglages, qui ne veulent plus rien dire).
     */
    public static List<Category> powersToForget(AbilityData data, List<Category> all, Category adopted) {
        List<Category> out = new ArrayList<>();
        if (data == null) return out;
        for (Category category : all) {
            if (category == null || same(category, adopted)) continue;
            if (data.getCategoryLevel(category) > 0) out.add(category);
        }
        return out;
    }

    /** Deux categories sont les memes quand elles portent le meme nom. */
    public static boolean same(Category a, Category b) {
        return a != null && b != null && a.getName().equals(b.getName());
    }

    /**
     * Le debut d'une ligne de {@code /aim skills} : {@code #id nom: }.
     *
     * <p>Le libelle n'est pas ici : c'est une traduction, donc un {@code Component} que le
     * client resout. La ligne se coupe en deux pour que la partie qui se relit en test
     * (l'identifiant et le nom, qui viennent du registre) soit separee de celle qui se
     * traduit.
     */
    public static String skillPrefix(Skill skill) {
        return "#" + skill.getId() + " " + skill.getName() + ": ";
    }

    /** Le debut d'une ligne de {@code /aim catlist} : {@code #i nom: }. */
    public static String categoryPrefix(int index, Category category) {
        return "#" + index + " " + category.getName() + ": ";
    }

    /** Les competences apprises, separees par des virgules — le format de l'original. */
    public static String learnedLine(List<Skill> skills) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Skill skill : skills) {
            if (!first) sb.append(", ");
            sb.append(skill.getName());
            first = false;
        }
        return sb.toString();
    }

    public static boolean levelInRange(int level) {
        return level >= LEVEL_MIN && level <= LEVEL_MAX;
    }

    public static boolean expInRange(float exp) {
        return exp >= EXP_MIN && exp <= EXP_MAX;
    }
}
