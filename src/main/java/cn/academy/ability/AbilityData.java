package cn.academy.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;

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
     * Charges en cours, en ticks accumules.
     *
     * Portage du compteur que l'original tenait dans le contexte d'activation : la
     * touche reste enfoncee, le compteur avance, et la competence est executee au
     * relachement avec ce quil a accumule. Ce n'est <b>pas</b> sauvegarde : une charge
     * ne survit pas a un rechargement, comme dans l'original.
     *
     * Le compteur <b>survit au relachement</b> : c'est ce que la competence lit pour
     * savoir combien de temps elle a ete chargee, dans ses degats comme dans sa
     * recharge. Seule une nouvelle charge le remet a zero.
     */
    private final Map<Skill, Integer> chargeTicks = new HashMap<>();

    /** Les competences dont la touche est actuellement enfoncee. */
    private final Set<Skill> charging = new HashSet<>();

    /**
     * Ce qu'un maintien en cours a besoin de retenir.
     *
     * L'original rangeait cela dans le contexte d'activation, qui vivait pour un joueur
     * et une competence : le surcout a reserver, et un repere de temps pour les effets
     * qui ne doivent pas se repeter a chaque tick (le bouclier absorbe au plus une fois
     * toutes les 18 ticks). Le port n'a pas de contexte, donc ce qu'il y avait dedans
     * est range avec la donnee du joueur. Rien n'est sauvegarde : un maintien ne
     * survit pas a un rechargement, comme une charge.
     */
    public static final class Hold {

        /** Surcout a ne pas laisser redescendre tant que le maintien dure. */
        private float overload;

        /** Ticks tenus du dernier effet, ou -1 s'il n'y en a pas encore eu. */
        private int mark = -1;

        /** Ancre fixe du maintien : le bloc vise, pour une competence qui attire. */
        private net.minecraft.world.phys.Vec3 point;

        /** Cible vivante visee, 0 s'il n'y en a pas. */
        private int targetId;

        /** Ou le maintien a commence, pour les competences qui se paient en distance. */
        private net.minecraft.world.phys.Vec3 origin;

        /** Bloc en cours de minage : les rayons du meltdowner y creusent. */
        private net.minecraft.core.BlockPos block;

        /** Ce qu'il reste a user sur ce bloc, en durete. */
        private float progress;
    }

    private final Map<Skill, Hold> holds = new HashMap<>();

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
        return perform(amount, 0f);
    }

    public void tickRegen(float amount) {
        controlPoint = Math.min(maxControlPoint, controlPoint + amount);
    }

    // ------------------------------------------------------------------
    // Surcout
    // ------------------------------------------------------------------

    /**
     * Plafond de base du surcout par niveau, repris de {@code init_overload} de
     * l'original.
     *
     * C'est la meme reserve que dans la 1.12.2, a l'unite pres : contrairement aux
     * couts en CP de l'original, ceux du surcout tiennent dans une echelle que le
     * port peut reprendre telle quelle.
     */
    private static final float[] BASE_MAX_OVERLOAD = {100f, 100f, 150f, 240f, 350f, 500f};

    /**
     * Gain de plafond acquis en utilisant ses competences, repris de
     * {@code add_overload}. Le joueur gagne donc de la reserve en s'en servant.
     */
    private static final float[] MAX_ADD_OVERLOAD = {0f, 40f, 70f, 80f, 100f, 500f};

    /**
     * Part du surcout qui devient du plafond, reprise de {@code maxo_incr_rate}.
     *
     * Bornee a 10 points par activation, comme l'original.
     */
    private static final float OVERLOAD_INCR_RATE = 0.0058f;

    /** Plafond de base du surcout pour ce niveau. */
    public static float baseMaxOverload(int level) {
        return BASE_MAX_OVERLOAD[clampLevel(level)];
    }

    /** Gain de plafond maximal pour ce niveau. */
    public static float maxAddOverload(int level) {
        return MAX_ADD_OVERLOAD[clampLevel(level)];
    }

    private static int clampLevel(int level) {
        return Math.max(0, Math.min(BASE_MAX_OVERLOAD.length - 1, level));
    }

    /**
     * Niveau d'aptitude du joueur.
     *
     * L'original n'avait qu'une aptitude et donc qu'un niveau ; le port en a quatre.
     * C'est le plus haut qui compte : un joueur qui a monte une categorie au niveau 4
     * a la reserve du niveau 4, sinon sa meilleure aptitude serait penalisee par celle
     * qu'il delaisse.
     */
    public int getHighestLevel() {
        int highest = 0;
        for (int level : categoryLevels.values()) {
            highest = Math.max(highest, level);
        }
        return highest;
    }

    private float overload;
    private float addMaxOverload;

    /**
     * Faux quand le surcout a atteint son maximum : plus aucune competence ne part.
     *
     * L'original distingait ce drapeau de {@code isOverloaded()} pour l'affichage,
     * dont le temoin s'eteignait au bout du delai de recuperation alors que les
     * competences restaient bloquees. Le port garde un seul drapeau : un verrou qui
     * ne se montre plus est un verrou invisible, et le joueur ne comprendrait pas
     * pourquoi ses touches ne repondent plus alors que la barre redescend.
     */
    private boolean overloadFine = true;

    /** Ticks restants avant que le surcout ne redescende. */
    private int untilOverloadRecover;

    public float getOverload() {
        return overload;
    }

    public float getMaxOverload() {
        return baseMaxOverload(getHighestLevel()) + addMaxOverload;
    }

    /**
     * Force le surcout courant, borne a la reserve.
     *
     * Portage de {@code CPData.setOverload} : l'original s'en servait pour remettre le
     * surcout epingle d'un maintien apres que la recuperation l'a fait baisser.
     */
    public void setOverload(float value) {
        overload = Math.max(0f, Math.min(getMaxOverload(), value));
    }

    /** Part du plafond acquise en utilisant ses competences. */
    public float getAddMaxOverload() {
        return addMaxOverload;
    }

    public boolean isOverloaded() {
        return !overloadFine;
    }

    public int getUntilOverloadRecover() {
        return untilOverloadRecover;
    }

    /**
     * Verifie et applique le cout d'une activation : les deux ressources, ou rien.
     *
     * Portage de {@code CPData.perform} : le surcout n'est ajoute que si les CP ont
     * ete payes. Sans cette atomicite, une competence refusee faute de CP laisserait
     * quand meme du surcout derriere elle.
     */
    public boolean perform(float cp, float overloadCost) {
        if (controlPoint < cp) return false;
        controlPoint -= cp;
        addOverload(overloadCost);
        return true;
    }

    /**
     * Paye sans verifier la reserve, en la vidant au pire.
     *
     * Portage de {@code CPData.performWithForce} : certains couts se calculent sur ce
     * qui reste — la teleportation au marqueur coute tant par bloc, dans la limite de ce
     * qu'on a — donc il n'y a rien a refuser, seulement une reserve a vider.
     */
    public void performForced(float cp, float overloadCost) {
        controlPoint = Math.max(0f, controlPoint - cp);
        addOverload(overloadCost);
    }

    /**
     * Ajoute du surcout, en plafonnant a la reserve et en armant la recuperation.
     *
     * Portage de {@code CPData.addOverload} : atteindre le maximum met le joueur en
     * surcharge, et une part du surcout consomme devient de la reserve permanente.
     */
    private void addOverload(float amount) {
        if (amount <= 0) return;
        untilOverloadRecover = cn.academy.Config.overloadRecoverCooldown;
        float max = getMaxOverload();
        overload = Math.min(max, overload + amount);
        if (overload >= max) {
            overloadFine = false;
        }
        addMaxOverload = Math.min(maxAddOverload(getHighestLevel()),
                addMaxOverload + Math.min(10f, amount * OVERLOAD_INCR_RATE));
    }

    /**
     * Fait avancer la recuperation du surcout d'un tick.
     *
     * Portage de la seconde moitie de {@code CPData.tick} : rien ne redescend avant
     * la fin du delai, puis la reserve se vide d'autant plus vite qu'elle est presque
     * vide — c'est le {@code getOverloadRecoverSpeed} de l'original.
     */
    public void tickOverload() {
        // Un maintien en cours epingle sa part de surcout : l'original la reposait a
        // chaque tick apres la recuperation, ce qui revient au meme.
        if (isHoldingOverload()) return;
        if (untilOverloadRecover > 0) {
            untilOverloadRecover--;
            return;
        }
        if (overload <= 0) return;
        overload = Math.max(0f, overload - getOverloadRecoverSpeed());
        if (overload <= 0f) {
            overload = 0f;
            overloadFine = true;
        }
    }

    /**
     * Vitesse de recuperation, portage de {@code CPData.getOverloadRecoverSpeed} :
     * {@code max(0,002 x reserve, 0,007 x reserve x lerp(1, 0,5, charge / reserve / 2))}.
     *
     * Sur une reserve de 100 points, cela fait 0,70 point par tick quand la barre est
     * vide et 0,53 quand elle est pleine : une surcharge complete tient donc une
     * dizaine de secondes.
     */
    public float getOverloadRecoverSpeed() {
        float max = getMaxOverload();
        if (max <= 0f) return 0f;
        float raw = Math.max(0.002f * max,
                0.007f * max * Skill.lerp(1f, 0.5f, overload / max / 2f));
        return (float) (cn.academy.Config.overloadRecoverSpeed * raw);
    }

    public void copyFrom(AbilityData other) {
        categoryLevels.clear();
        categoryLevels.putAll(other.categoryLevels);
        controlPoint = other.controlPoint;
        maxControlPoint = other.maxControlPoint;
        overload = other.overload;
        addMaxOverload = other.addMaxOverload;
        overloadFine = other.overloadFine;
        untilOverloadRecover = other.untilOverloadRecover;
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

    // ------------------------------------------------------------------
    // Charges
    // ------------------------------------------------------------------

    /**
     * Ticks accumules lors de la derniere charge de cette competence.
     *
     * Reste lisible apres le relachement : c'est la valeur que la competence consulte
     * pour doser son effet.
     */
    public int getChargeTicks(Skill skill) {
        return skill == null ? 0 : chargeTicks.getOrDefault(skill, 0);
    }

    /** Vrai si la touche de cette competence est actuellement enfoncee. */
    public boolean isCharging(Skill skill) {
        return skill != null && charging.contains(skill);
    }

    public boolean isChargingAnything() {
        return !charging.isEmpty();
    }

    /** Les competences dont la touche est enfoncee, en copie : l'appelant peut agir dessus. */
    public Set<Skill> getChargingSkills() {
        return Set.copyOf(charging);
    }

    // ------------------------------------------------------------------
    // Etat d'un maintien
    // ------------------------------------------------------------------

    private Hold holdOf(Skill skill) {
        return skill == null ? null : holds.computeIfAbsent(skill, s -> new Hold());
    }

    /**
     * Surcout que ce maintien ne laisse pas redescendre.
     *
     * L'original epinglait le surcout consomme a l'ouverture ({@code overloadKeep}) :
     * sans cela, tenir un bouclier rendrait la reserve au fur et a mesure, donc le
     * maintenir serait gratuit en surcout au bout de quelques secondes.
     */
    public void setHeldOverload(Skill skill, float overload) {
        Hold hold = holdOf(skill);
        if (hold != null) hold.overload = overload;
    }

    public float getHeldOverload(Skill skill) {
        Hold hold = holds.get(skill);
        return hold == null ? 0f : hold.overload;
    }

    /** Vrai si un maintien en cours epingle du surcout. */
    public boolean isHoldingOverload() {
        for (Hold hold : holds.values()) {
            if (hold.overload > 0f) return true;
        }
        return false;
    }

    /** Ticks tenus du dernier effet de ce maintien, ou -1 s'il n'y en a pas encore eu. */
    public int getHoldMark(Skill skill) {
        Hold hold = holds.get(skill);
        return hold == null ? -1 : hold.mark;
    }

    public void setHoldMark(Skill skill, int ticks) {
        Hold hold = holdOf(skill);
        if (hold != null) hold.mark = ticks;
    }

    /** Ancre fixe du maintien : le point que la competence vise, sans le suivre. */
    public void setHoldPoint(Skill skill, net.minecraft.world.phys.Vec3 point) {
        Hold hold = holdOf(skill);
        if (hold != null) hold.point = point;
    }

    @Nullable
    public net.minecraft.world.phys.Vec3 getHoldPoint(Skill skill) {
        Hold hold = holds.get(skill);
        return hold == null ? null : hold.point;
    }

    /**
     * Cible vivante du maintien.
     *
     * Seul l'identifiant est garde : une entite ne se range pas dans une donnee de
     * joueur, et l'original suivait lui aussi une entite qu'il relisait a chaque tick.
     * Zero signifie qu'il n'y en a pas.
     */
    public void setHoldTarget(Skill skill, int entityId) {
        Hold hold = holdOf(skill);
        if (hold != null) hold.targetId = entityId;
    }

    public int getHoldTargetId(Skill skill) {
        Hold hold = holds.get(skill);
        return hold == null ? 0 : hold.targetId;
    }

    /** Point de depart du maintien, pour l'experience gagnee en distance. */
    public void setHoldOrigin(Skill skill, net.minecraft.world.phys.Vec3 origin) {
        Hold hold = holdOf(skill);
        if (hold != null) hold.origin = origin;
    }

    @Nullable
    public net.minecraft.world.phys.Vec3 getHoldOrigin(Skill skill) {
        Hold hold = holds.get(skill);
        return hold == null ? null : hold.origin;
    }

    /**
     * Bloc vise par un maintien qui creuse, et ce qu'il reste a en user.
     *
     * L'original tenait ces deux nombres dans son contexte de rayon : le bloc vise, et
     * la durete qui lui restait. Le port n'a pas de contexte, donc ils vivent avec le
     * maintien — et sans eux, changer de cible a chaque tick remettrait le minage a zero.
     */
    public void setHoldBlock(Skill skill, net.minecraft.core.BlockPos block) {
        Hold hold = holdOf(skill);
        if (hold != null) hold.block = block;
    }

    @Nullable
    public net.minecraft.core.BlockPos getHoldBlock(Skill skill) {
        Hold hold = holds.get(skill);
        return hold == null ? null : hold.block;
    }

    public void setHoldProgress(Skill skill, float progress) {
        Hold hold = holdOf(skill);
        if (hold != null) hold.progress = progress;
    }

    public float getHoldProgress(Skill skill) {
        Hold hold = holds.get(skill);
        return hold == null ? 0f : hold.progress;
    }

    /** Commence une charge : le compteur repart de zero. */
    public void beginCharge(Skill skill) {
        if (skill == null) return;
        chargeTicks.put(skill, 0);
        charging.add(skill);
    }

    /** Termine une charge sans oublier son compteur : la competence va le lire. */
    public void endCharge(Skill skill) {
        if (skill == null) return;
        charging.remove(skill);
        // L'etat du maintien, lui, disparait : il n'a de sens que pendant.
        holds.remove(skill);
    }

    /** Annule une charge et oublie son compteur. */
    public void cancelCharge(Skill skill) {
        if (skill == null) return;
        charging.remove(skill);
        chargeTicks.remove(skill);
        holds.remove(skill);
    }

    /**
     * Fait avancer toutes les charges en cours d'un tick, sans depasser leur maximum.
     *
     * A appeler cote serveur a chaque tick du joueur. Le serveur tient son propre
     * compteur : le client n'annonce pas combien de temps il a tenu la touche, comme
     * dans l'original ou les deux cotes comptaient de leur cote.
     */
    public void tickCharges() {
        if (charging.isEmpty()) return;
        for (Skill skill : charging) {
            int max = skill.getMaxChargeTicks(this);
            int ticks = chargeTicks.getOrDefault(skill, 0) + 1;
            chargeTicks.put(skill, max > 0 ? Math.min(ticks, max) : ticks);
        }
    }

    /** Oublie toutes les charges. Appele a la mort du joueur. */
    public void clearCharges() {
        charging.clear();
        chargeTicks.clear();
        holds.clear();
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

        // Le surcout voyage avec le reste : l'original le sauvegardait aussi, donc se
        // reconnecter en pleine surcharge ne remet pas la reserve a zero.
        tag.putFloat("overload", overload);
        tag.putFloat("addOverload", addMaxOverload);
        tag.putBoolean("overloadFine", overloadFine);
        tag.putInt("untilOverloadRecover", untilOverloadRecover);
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

        overload = tag.getFloat("overload");
        addMaxOverload = tag.getFloat("addOverload");
        // Une sauvegarde sans le drapeau est une sauvegarde d'avant la surcharge :
        // le joueur doit repartir disponible, pas bloque par un booleen par defaut.
        overloadFine = !tag.contains("overloadFine") || tag.getBoolean("overloadFine");
        untilOverloadRecover = tag.getInt("untilOverloadRecover");
    }
}
