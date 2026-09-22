package cn.academy.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Per-player ability state: learning progress per category and the Control Point resource
 * consumed by active skills. Port of original DevelopData/AbilityData, simplified.
 */
public class AbilityData implements INBTSerializable<CompoundTag> {

    private static final String TAG_SKILLS = "skills";
    private static final String TAG_SKILL_EXPS = "skillExps";
    private static final String TAG_LEVEL_PROGRESS = "levelProgress";
    private static final String TAG_COOLDOWNS = "cooldowns";

    /**
     * Part de la progression d'un niveau qui doit etre remplie pour monter.
     *
     * L'original multipliait le nombre de competences du niveau par 0,666 — ou par
     * 1,333 au niveau 4, le dernier avant le maximum, pour le rendre plus long. Ces
     * deux nombres sont repris tels quels : c'est ce qui donne au niveau 4 sa duree.
     */
    private static final float PROGRESS_PER_SKILL = 0.666f;

    /** Au niveau 4, le palier est double : c'est le dernier avant le maximum. */
    private static final float LAST_LEVEL_FACTOR = 1.333f;

    /** Niveau a partir duquel la progression est plus longue, comme dans l'original. */
    private static final int LAST_LEVEL = 4;

    private final Map<String, Integer> categoryLevels = new HashMap<>();

    /** Competences apprises, sous la forme {@code <categorie>.<competence>}. */
    private final Set<String> learnedSkills = new HashSet<>();

    /**
     * Experience de chaque competence, de 0 a 1.
     *
     * Meme cle que les competences apprises. L'original tenait un tableau indexe par
     * l'identifiant de la competence ; le nom, lui, ne bouge pas quand le registre
     * change.
     */
    private final Map<String, Float> skillExps = new HashMap<>();

    /**
     * Avancement verse dans le niveau en cours, par categorie.
     *
     * L'original n'avait qu'une categorie et donc qu'un seul compteur. Comme le port
     * tient un niveau par categorie, il lui faut un avancement par categorie — sans
     * quoi gagner de l'experience dans une categorie ferait monter les autres.
     */
    private final Map<String, Float> levelProgress = new HashMap<>();

    /**
     * Recharges en cours, par competence, en ticks restants.
     *
     * Portage de {@code CooldownData}. L'original reduisait chaque compteur d'un tick
     * par tick et oubliait ceux qui tombaient a zero, ce qui est exactement ce que fait
     * {@link #tickCooldowns()}.
     *
     * Un ecart assume : l'original ne sauvegardait pas ses recharges, donc se
     * reconnecter les effacait. Ici elles voyagent avec le reste de la donnee du
     * joueur — l'effet visible est le meme, sauf qu'on ne peut plus les effacer en
     * relancant le jeu. Elles sont en revanche bien oubliees a la mort, comme dans
     * l'original ({@code onPlayerDead}).
     */
    private final Map<String, Integer> cooldowns = new HashMap<>();

    /**
     * Sources d'interference actives, par nom. Non sauvegarde.
     *
     * Portage de {@code CPData.interfSources} : une machine pose une source, et
     * la source repond elle-meme si elle brouille encore. Une source qui repond
     * non est retiree au tick suivant, ce qui evite d'accumuler des entrees
     * fantomes quand un brouilleur est casse ou desactive.
     */
    private final Map<String, BooleanSupplier> interferenceSources = new HashMap<>();

    /**
     * Resultat mis en cache, et lui sauvegarde.
     *
     * C'est la seule chose que le client peut connaitre : il n'a pas les sources.
     * Le drapeau voyage donc dans la sauvegarde pour que le HUD puisse le lire,
     * exactement comme {@code interfering} dans l'original.
     */
    private boolean interfered;

    private float controlPoint = cn.academy.Config.startingControlPoint();
    private float maxControlPoint = (float) cn.academy.Config.controlPointMax;

    /** Plafond courant, relu depuis la config a chaque appel (rechargement a chaud). */
    public static float configuredMax() {
        return (float) cn.academy.Config.controlPointMax;
    }

    public int getCategoryLevel(Category category) {
        return categoryLevels.getOrDefault(category.getName(), 0);
    }

    /**
     * Fixe le niveau d'une categorie et remet son avancement a zero.
     *
     * La remise a zero est celle de l'original ({@code setLevel} vidait
     * {@code expAddedThisLevel}) : sans elle, l'avancement accumule pour le niveau
     * precedent ferait monter le suivant d'un coup.
     */
    public void setCategoryLevel(Category category, int level) {
        categoryLevels.put(category.getName(), level);
        levelProgress.remove(category.getName());
    }

    public boolean hasLearned(Category category) {
        return getCategoryLevel(category) > 0;
    }

    public float getControlPoint() {
        return controlPoint;
    }

    public float getMaxControlPoint() {
        return maxControlPoint;
    }

    /**
     * Reapplique le plafond de la config. Appele a chaque tick par
     * {@code AbilityEvents} : un rechargement de config a chaud est donc pris en
     * compte sur les joueurs deja connectes.
     */
    public void clampToConfiguredMax() {
        maxControlPoint = configuredMax();
        if (controlPoint > maxControlPoint) controlPoint = maxControlPoint;
    }

    public boolean consumeControlPoint(float amount) {
        if (controlPoint < amount) return false;
        controlPoint -= amount;
        return true;
    }

    public void tickRegen(float amount) {
        controlPoint = Math.min(maxControlPoint, controlPoint + amount);
    }

    public void copyFrom(AbilityData other) {
        categoryLevels.clear();
        categoryLevels.putAll(other.categoryLevels);
        controlPoint = other.controlPoint;
        maxControlPoint = other.maxControlPoint;
        // Les sources d'interference ne sont pas copiees : elles appartiennent a un
        // monde et a des machines precises, et les brouilleurs encore en place les
        // reposeront dans la dizaine de ticks qui suit.
        interferenceSources.clear();
        interfered = false;
    }

    // ------------------------------------------------------------------
    // Interferences
    // ------------------------------------------------------------------

    /**
     * Ajoute une source d'interference.
     *
     * Appeler plusieurs fois avec le meme nom remplace la source precedente, ce qui
     * rend l'appel idempotent : une machine repose la sienne a chaque cycle sans
     * avoir a verifier si elle y est deja.
     */
    public void addInterference(String key, BooleanSupplier condition) {
        interferenceSources.put(key, condition);
    }

    /** Retire une source d'interference, si elle existe. */
    public void removeInterference(String key) {
        interferenceSources.remove(key);
    }

    /**
     * Retire les sources qui ne brouillent plus et met a jour le drapeau.
     *
     * A appeler cote serveur a chaque tick. Reprend la boucle de {@code CPData.tick} :
     * c'est le seul endroit ou les sources sont evaluees, donc une source coute une
     * evaluation par tick et rien de plus.
     */
    public void refreshInterference() {
        Iterator<Map.Entry<String, BooleanSupplier>> iterator = interferenceSources.entrySet().iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().getValue().getAsBoolean()) iterator.remove();
        }
        interfered = !interferenceSources.isEmpty();
    }

    /** Vrai si le joueur est brouille : il ne peut pas utiliser ses competences. */
    public boolean isInterfered() {
        return interfered;
    }

    /** Nombre de sources actives, pour les tests et le debogage. */
    public int interferenceSourceCount() {
        return interferenceSources.size();
    }

    // ------------------------------------------------------------------
    // Competences apprises
    // ------------------------------------------------------------------

    /**
     * Vrai si cette competence a ete apprise au developpeur.
     *
     * L'original tenait un {@code BitSet} indexe par l'identifiant de la competence,
     * ce qui supposait une seule categorie par joueur. Le port autorise plusieurs
     * categories, donc la cle porte le nom de la categorie en plus de celui de la
     * competence : deux categories peuvent avoir une competence du meme nom, et rien
     * n'obligerait non plus les identifiants a rester alignes d'une version a
     * l'autre. Le nom, lui, ne bouge pas.
     */
    public boolean isSkillLearned(Skill skill) {
        if (skill == null || skill.getCategory() == null) return false;
        return learnedSkills.contains(skillKey(skill));
    }

    /** Marque une competence comme apprise. Retourne vrai si cela a change quelque chose. */
    public boolean learnSkill(Skill skill) {
        if (skill == null || skill.getCategory() == null) return false;
        return learnedSkills.add(skillKey(skill));
    }

    /** Oublie une competence. Sert surtout aux tests et au debogage. */
    public boolean forgetSkill(Skill skill) {
        if (skill == null || skill.getCategory() == null) return false;
        return learnedSkills.remove(skillKey(skill));
    }

    /** Les competences apprises d'une categorie, dans l'ordre du registre. */
    public List<Skill> getLearnedSkills(Category category) {
        List<Skill> out = new ArrayList<>();
        if (category == null) return out;
        for (Skill skill : category.getSkills()) {
            if (isSkillLearned(skill)) out.add(skill);
        }
        return out;
    }

    /** Nombre de competences apprises, toutes categories confondues. */
    public int getLearnedSkillCount() {
        return learnedSkills.size();
    }

    // ------------------------------------------------------------------
    // Experience, et progression de niveau
    // ------------------------------------------------------------------

    /**
     * Experience d'une competence, de 0 a 1.
     *
     * Rend 0 pour une competence d'une categorie que le joueur n'a pas apprise :
     * c'est la traduction de la garde de l'original, qui rendait 0 des que la
     * competence n'appartenait pas a la categorie du joueur.
     */
    public float getSkillExp(Skill skill) {
        if (skill == null || skill.getCategory() == null) return 0f;
        if (!hasLearned(skill.getCategory())) return 0f;
        return skillExps.getOrDefault(skillKey(skill), 0f);
    }

    /**
     * Ajoute de l'experience a une competence, et la verse au niveau en cours.
     *
     * Reprend {@code addSkillExp} de l'original, y compris deux details qui peuvent
     * surprendre :
     *
     * - l'experience de la competence est <b>plafonnee</b> a 1, mais l'avancement du
     *   niveau recoit le montant complet. Une competence deja saturee continue donc
     *   de faire progresser le niveau.
     * - la competence est apprise du meme coup. En pratique elle l'est deja, puisque
     *   l'activation exige qu'elle le soit ; c'est une ceinture de securite de
     *   l'original, conservee.
     *
     * Le multiplicateur de la competence y est applique : l'original le posait dans
     * {@code AbilityContext}, au moment de l'appel. Le faire ici evite qu'un appelant
     * l'oublie.
     */
    public void addSkillExp(Skill skill, float amount) {
        if (skill == null || skill.getCategory() == null || amount <= 0f) return;

        learnSkill(skill);

        float effective = amount * skill.getExpIncrSpeed();
        String key = skillKey(skill);
        float current = skillExps.getOrDefault(key, 0f);
        skillExps.put(key, Math.min(1f, current + effective));

        addLevelProgress(skill.getCategory(), effective);
    }

    /** Verse de l'avancement dans le niveau en cours d'une categorie. */
    private void addLevelProgress(Category category, float amount) {
        String key = category.getName();
        float scaled = amount * (float) cn.academy.Config.progressIncrRate;
        levelProgress.merge(key, scaled, Float::sum);
    }

    /** Avancement brut verse au niveau en cours, avant division par le palier. */
    public float getRawLevelProgress(Category category) {
        return levelProgress.getOrDefault(category.getName(), 0f);
    }

    /**
     * Nombre de competences utilisables du niveau en cours.
     *
     * C'est le palier de l'original ({@code getLevelTotalExp}) : une unite par
     * competence <b>non passive</b> du niveau courant — les passives ne se declenchent
     * pas, donc elles ne peuvent pas servir a progresser. Un niveau sans competence
     * utilisable rend 0, et {@link #getLevelProgress} le traite comme un palier deja
     * franchi : c'est le cas, dans le port, des niveaux dont les competences ne sont
     * pas encore portees. Cela s'ouvrira au fur et a mesure.
     */
    private int levelThreshold(Category category) {
        int level = getCategoryLevel(category);
        int count = 0;
        for (Skill skill : category.getSkills()) {
            if (!skill.isPassive() && skill.getLevel() == level) count++;
        }
        return count;
    }

    /**
     * Avancement du niveau en cours, de 0 a 1.
     *
     * Reprend {@code getLevelProgress} : le palier est le nombre de competences du
     * niveau multiplie par 0,666, ou par 1,333 au niveau 4. Un palier nul vaut
     * avancement complet.
     */
    public float getLevelProgress(Category category) {
        int level = getCategoryLevel(category);
        float factor = level == LAST_LEVEL ? LAST_LEVEL_FACTOR : PROGRESS_PER_SKILL;
        float threshold = levelThreshold(category) * factor;
        if (threshold <= 0f) return 1f;
        return Math.min(1f, getRawLevelProgress(category) / threshold);
    }

    /**
     * Le joueur peut-il monter cette categorie d'un cran ?
     *
     * Reprend {@code canLevelUp} : au maximum il n'y a plus rien a faire, et sinon il
     * faut avoir rempli le palier du niveau en cours. C'est ce qui fait qu'on monte en
     * <b>utilisant</b> ses competences, et non en attendant.
     */
    public boolean canLevelUp(Category category) {
        if (category == null) return false;
        if (getCategoryLevel(category) >= cn.academy.ability.develop.DevelopActionLevel.MAX_LEVEL) return false;
        return getLevelProgress(category) >= 1f;
    }

    /**
     * Verse de l'avancement sans passer par une competence.
     *
     * Sert aux commandes de debogage et aux tests : c'est le pendant de
     * {@code maxOutLevelProgress} de l'original, qui remplissait le niveau d'un coup.
     */
    public void maxOutLevelProgress(Category category) {
        if (category == null) return;
        levelProgress.put(category.getName(), Float.MAX_VALUE);
    }

    // ------------------------------------------------------------------
    // Recharges
    // ------------------------------------------------------------------

    /** Ticks de recharge restants pour cette competence, 0 si elle est prete. */
    public int getCooldown(Skill skill) {
        if (skill == null || skill.getCategory() == null) return 0;
        return cooldowns.getOrDefault(skillKey(skill), 0);
    }

    /** Vrai si la competence n'est pas encore prete. */
    public boolean isOnCooldown(Skill skill) {
        return getCooldown(skill) > 0;
    }

    /**
     * Lance la recharge d'une competence.
     *
     * Reprend {@code CooldownData.set} : une recharge plus longue que celle en cours
     * la remplace, une plus courte ne la raccourcit pas. Sans cela, enchainer deux
     * usages ferait tomber la recharge a celle du dernier tir.
     *
     * Une duree nulle ou negative n'entre rien : une competence sans recharge ne doit
     * pas laisser de compteur derriere elle.
     */
    public void setCooldown(Skill skill, int ticks) {
        if (skill == null || skill.getCategory() == null || ticks <= 0) return;
        String key = skillKey(skill);
        cooldowns.merge(key, ticks, Math::max);
    }

    /**
     * Fait avancer toutes les recharges d'un tick.
     *
     * A appeler cote serveur a chaque tick du joueur, comme la boucle de
     * {@code CooldownData}.
     */
    public void tickCooldowns() {
        if (cooldowns.isEmpty()) return;
        Iterator<Map.Entry<String, Integer>> iterator = cooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            int left = entry.getValue() - 1;
            if (left <= 0) iterator.remove();
            else entry.setValue(left);
        }
    }

    /** Oublie toutes les recharges. Appele a la mort du joueur, comme l'original. */
    public void clearCooldowns() {
        cooldowns.clear();
    }

    /** Nombre de competences en recharge, pour les tests et le debogage. */
    public int getCooldownCount() {
        return cooldowns.size();
    }

    private static String skillKey(Skill skill) {
        return skill.getCategory().getName() + "." + skill.getName();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        CompoundTag levels = new CompoundTag();
        categoryLevels.forEach(levels::putInt);
        tag.put("levels", levels);
        tag.putFloat("cp", controlPoint);
        tag.putFloat("maxCp", maxControlPoint);
        // Le drapeau voyage pour que le HUD du client puisse l'afficher ; les
        // sources, elles, n'ont aucun sens hors du serveur.
        tag.putBoolean("interfered", interfered);

        // Les competences apprises voyagent aussi : l'ecran du developpeur et
        // l'arbre doivent savoir quoi griser, et le client ne peut pas le deviner.
        ListTag skills = new ListTag();
        for (String key : learnedSkills) {
            skills.add(StringTag.valueOf(key));
        }
        tag.put(TAG_SKILLS, skills);

        // L'experience aussi : l'arbre de competences l'affiche, et c'est elle qui
        // dit au joueur ou il en est de sa progression.
        CompoundTag exps = new CompoundTag();
        skillExps.forEach(exps::putFloat);
        tag.put(TAG_SKILL_EXPS, exps);

        CompoundTag progress = new CompoundTag();
        levelProgress.forEach(progress::putFloat);
        tag.put(TAG_LEVEL_PROGRESS, progress);

        CompoundTag cds = new CompoundTag();
        cooldowns.forEach(cds::putInt);
        tag.put(TAG_COOLDOWNS, cds);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        categoryLevels.clear();
        CompoundTag levels = tag.getCompound("levels");
        for (String key : levels.getAllKeys()) {
            categoryLevels.put(key, levels.getInt(key));
        }
        controlPoint = tag.getFloat("cp");
        maxControlPoint = tag.contains("maxCp") ? tag.getFloat("maxCp") : configuredMax();
        interfered = tag.getBoolean("interfered");

        learnedSkills.clear();
        ListTag skills = tag.getList(TAG_SKILLS, Tag.TAG_STRING);
        for (int i = 0; i < skills.size(); i++) {
            learnedSkills.add(skills.getString(i));
        }

        skillExps.clear();
        CompoundTag exps = tag.getCompound(TAG_SKILL_EXPS);
        for (String key : exps.getAllKeys()) {
            skillExps.put(key, exps.getFloat(key));
        }

        levelProgress.clear();
        CompoundTag progress = tag.getCompound(TAG_LEVEL_PROGRESS);
        for (String key : progress.getAllKeys()) {
            levelProgress.put(key, progress.getFloat(key));
        }

        cooldowns.clear();
        CompoundTag cds = tag.getCompound(TAG_COOLDOWNS);
        for (String key : cds.getAllKeys()) {
            cooldowns.put(key, cds.getInt(key));
        }
    }
}
