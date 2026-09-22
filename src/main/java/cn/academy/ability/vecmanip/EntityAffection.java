package cn.academy.ability.vecmanip;

import java.util.ArrayList;
import java.util.List;

import cn.academy.Config;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Ce que vecmanip peut devier, et ce qu'il ne touche jamais.
 *
 * <p>Portage de l'{@code EntityAffection} de l'original, que deux competences consultent —
 * la deviation et la reflexion. Il repond a deux questions : cette entite m'interesse-t-elle,
 * et si oui, a quelle <b>difficulte</b> ? La difficulte multiplie ce qu'elle coute en surcout
 * et ce qu'elle rapporte en experience, ce qui vaut la peine d'une fleche (1,0), d'une potion
 * (1,4) et beaucoup moins celle d'une boule de neige (0,1).
 *
 * <h2>La config du port est plate</h2>
 *
 * L'original lisait une liste d'objets <code>{ name, difficulty }</code> ; une config plate
 * n'a que des listes de chaines, d'ou l'ecriture <code>"minecraft:arrow=1.0"</code> — un id
 * seul valant la difficulte par defaut. La liste d'exclusion, elle, reprend exactement celle
 * de l'original : des ids d'entites, plus les deux mots-cles {@code living} et {@code mob}.
 *
 * <p>Par defaut, donc, <b>rien de vivant n'est affecte</b> — les creatures comme les
 * projectiles vivants sont exclus — et seules les projectiles non vivantes le sont, ce qui est
 * bien l'intention de l'original : on arrete ce qui vole, pas ce qui marche.
 *
 * <h2>Le repere</h2>
 *
 * Une entite deviee est <b>marquee</b>, dans ses donnees persistantes : elle ne sera plus
 * touchee, ni tout de suite, ni apres un rechargement, ni par une seconde competence. C'est
 * ce qui empeche une cible immobile de rapporter de l'experience a chaque tick, et le repere
 * est repris de l'original, tag compris.
 *
 * <p>Non porte : la seconde liste de l'original, un {@code visited} par contexte. Elle ne
 * servait qu'a eviter de reexaminer ce qui venait d'etre marque — ce que le repere fait deja,
 * et pour toujours.
 */
public final class EntityAffection {

    /** Difficulte d'une entite affectee quand la config n'en dit rien. */
    public static final float DEFAULT_DIFFICULTY = 1f;

    /** Tout ce qui vit : le mot-cle d'exclusion de l'original. */
    public static final String KEYWORD_LIVING = "living";

    /** Les creatures : l'autre mot-cle d'exclusion de l'original. */
    public static final String KEYWORD_MOB = "mob";

    /** Le repere pose sur une entite deja deviee, dans ses donnees persistantes. */
    public static final String MARK = "ac_vm_deviated";

    /** Ce qu'une entite vaut devant l'onde. */
    public record Affect(boolean excluded, float difficulty) {

        /** L'entite est exclue : ni effet, ni cout, ni experience. */
        public static final Affect EXCLUDED = new Affect(true, 0f);

        public static Affect affected(float difficulty) {
            return new Affect(false, difficulty);
        }
    }

    /** Une entree de config : un nom d'entite, et sa difficulte. */
    public record Entry(String name, float difficulty) {}

    private static List<String> affectedFrom;
    private static List<String> excludedFrom;
    private static List<Entry> entries = List.of();
    private static List<String> exclusions = List.of();

    private EntityAffection() {}

    // ------------------------------------------------------------------
    // La config, et sa lecture
    // ------------------------------------------------------------------

    /**
     * Lit une entree de config.
     *
     * <p>Deux ecritures, et deux refus : un id seul vaut la difficulte par defaut, et une
     * entree illisible — vide, sans nom, ou avec un nombre qui n'en est pas un — rend
     * {@code null} plutot que de faire tomber le mod au chargement. Une faute de frappe dans
     * la config ne doit pas empecher de jouer.
     */
    public static Entry parseEntry(String raw) {
        if (raw == null) return null;
        String text = raw.trim();
        if (text.isEmpty()) return null;

        int cut = text.indexOf('=');
        if (cut < 0) return new Entry(text, DEFAULT_DIFFICULTY);

        String name = text.substring(0, cut).trim();
        String number = text.substring(cut + 1).trim();
        if (name.isEmpty() || number.isEmpty()) return null;

        try {
            return new Entry(name, Float.parseFloat(number));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // La classification
    // ------------------------------------------------------------------

    /** Ce qu'une entite vaut : exclue, ou affectee avec sa difficulte. */
    public static Affect affect(Entity entity) {
        ensureBuilt();

        for (String exclusion : exclusions) {
            if (excludes(exclusion, entity)) return Affect.EXCLUDED;
        }
        for (Entry entry : entries) {
            if (matches(entry.name(), entity)) return Affect.affected(entry.difficulty());
        }
        return Affect.affected(DEFAULT_DIFFICULTY);
    }

    private static boolean excludes(String name, Entity entity) {
        if (KEYWORD_LIVING.equalsIgnoreCase(name)) return entity instanceof LivingEntity;
        if (KEYWORD_MOB.equalsIgnoreCase(name)) return entity instanceof Mob;
        return matches(name, entity);
    }

    /**
     * Vrai si cette entite est du type nomme.
     *
     * <p>{@code EntityType.byString} comprend aussi les <b>etiquettes</b> ({@code #...}),
     * ce qui laisse une config exprimer « toutes les fleches » sans les nommer une par une.
     */
    private static boolean matches(String name, Entity entity) {
        return EntityType.byString(name)
                .map(type -> type == entity.getType())
                .orElse(false);
    }

    // ------------------------------------------------------------------
    // Le repere
    // ------------------------------------------------------------------

    /** Marque une entite comme deja deviee. Le repere suit l'entite, meme rechargee. */
    public static void mark(Entity entity) {
        entity.getPersistentData().putBoolean(MARK, true);
    }

    public static boolean isMarked(Entity entity) {
        return entity.getPersistentData().getBoolean(MARK);
    }

    /**
     * Reconstruit les ensembles si la config a change.
     *
     * <p>La config remplace ses listes par de nouvelles instances a chaque chargement :
     * comparer les references suffit, comme pour les metaux de l'electromaster. Une faute de
     * frappe dans un nom d'entite est ignoree, et l'entite tombe alors dans la difficulte par
     * defaut.
     *
     * <h2>Sixieme coquille de l'original corrigee</h2>
     *
     * <p>Son {@code .find { case (klass, _) => klass != null }} lisait la liste de difficultes
     * avec un {@code find} la ou il fallait un {@code filter} : sur les trois entrees de la
     * config par defaut, <b>une seule</b> etait retenue — la premiere, la fleche — et une
     * potion valait donc 1,0 au lieu de 1,4, une boule de neige 1,0 au lieu de 0,1. Le port
     * lit toute la liste, et un test le verifie : c'est ce qui donne leur prix aux projectiles
     * que vecmanip arrete.
     */
    private static void ensureBuilt() {
        if (affectedFrom == Config.affectedEntities && excludedFrom == Config.excludedEntities) return;
        affectedFrom = Config.affectedEntities;
        excludedFrom = Config.excludedEntities;

        List<Entry> parsed = new ArrayList<>();
        for (String raw : affectedFrom) {
            Entry entry = parseEntry(raw);
            if (entry != null) parsed.add(entry);
        }
        entries = List.copyOf(parsed);
        exclusions = List.copyOf(excludedFrom);
    }
}
