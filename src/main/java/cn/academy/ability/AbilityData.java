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
    private static final String TAG_COOLDOWN_TOTALS = "cooldownTotals";
    private static final String TAG_MARKS = "marks";

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

    /**
     * Le joueur a qui appartiennent ces donnees, sur le serveur uniquement.
     *
     * <p>Trois succes du mod se declenchent au fond des donnees elles-memes — apprendre
     * une competence, la saturer d'experience, tomber en surcharge — et ces endroits-la
     * n'ont aucun joueur sous la main. Le proprietaire est donc pose a l'attachement de
     * la capacite, et reste <b>nul</b> partout ailleurs : dans les tests unitaires, qui
     * construisent des donnees sans joueur, rien ne se declenche.
     */
    private net.minecraft.server.level.ServerPlayer owner;

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
     * La duree TOTALE de chaque recharge en cours, telle qu'elle a ete posee.
     *
     * <p>Sans elle on saurait qu'une competence est indisponible, mais pas a quel point :
     * c'est elle qui donne son echelle au gris de l'icone. L'original gardait les deux
     * nombres dans son {@code SkillCooldown} ({@code getTickLeft} et {@code getMaxTick}).
     */
    private final Map<String, Integer> cooldownTotals = new HashMap<>();

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

        /**
         * Reperes poses par le maintien, dans l'ordre.
         *
         * La bombe a fragmentation y range ses billes : elle en pose une toutes les dix
         * ticks, et elles ne partent qu'a la fin du maintien. L'original les gardait dans
         * son contexte d'activation ; le port les range avec le maintien, et elles
         * disparaissent avec lui.
         */
        private final List<net.minecraft.world.phys.Vec3> points = new ArrayList<>();

        /**
         * Le vol libre du joueur avant le maintien.
         *
         * Les ailes de tempete l'ouvrent pour la duree de leur vol et le rendent a la fin :
         * sans ce souvenir, un joueur de mode survie garderait le vol apres sa competence.
         */
        private boolean flying;

        /**
         * Billes accumulees par le maintien.
         *
         * Le missile a electrons en pose une toutes les dix ticks — jusqu'a cinq — et chaque
         * tir en consomme une. L'original en faisait des entites visibles (des spheres dessinees
         * par un shader) ; le port, qui n'a pas d'effets de ce genre, les compte, et le compte
         * disparait avec le maintien comme le reste.
         */
        private int balls;
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

    /**
     * Les endroits marques par le joueur, portage de {@code LocTeleportData}.
     *
     * <p>La teleportation a la marque et son ecran vivent de cette liste : l'ecran la lit
     * pour l'afficher, et c'est elle qui se sauvegarde et qui voyage jusqu'au client avec
     * le reste de la donnee du joueur — un seul chemin suffit, donc pas de seconde
     * capacite ni de second paquet pour une liste de coordonnees.
     */
    private final List<cn.academy.ability.teleporter.LocationMark> marks = new ArrayList<>();

    /** Les marques, dans l'ordre ou elles ont ete posees. */
    public List<cn.academy.ability.teleporter.LocationMark> getMarks() {
        return List.copyOf(marks);
    }

    /**
     * Pose une marque, a la suite.
     *
     * <p>Reprend {@code add} de l'original : le rang dans la liste <b>est</b> l'identifiant.
     */
    public void addMark(String name, String dimension, double x, double y, double z) {
        marks.add(new cn.academy.ability.teleporter.LocationMark(
                cn.academy.ability.teleporter.LocationMark.cleanName(name, marks.size()),
                dimension, x, y, z));
    }

    /**
     * Retire une marque, et renumerote celles qui suivent.
     *
     * <p>Reprend {@code remove} de l'original : sans renumerotation, l'ecran enverrait
     * l'identifiant d'une marque pour en designer une autre.
     */
    public void removeMark(int id) {
        if (id < 0 || id >= marks.size()) return;
        marks.remove(id);
    }

    /** La marque de ce rang, ou {@code null}. */
    @Nullable
    public cn.academy.ability.teleporter.LocationMark getMark(int id) {
        return id < 0 || id >= marks.size() ? null : marks.get(id);
    }

    /**
     * L'aptitude est-elle allumee ?
     *
     * <p>Portage de l'etat du meme nom chez l'original : elle est <b>eteinte au depart</b>, et
     * le joueur l'allume avec sa touche (V chez lui). Tant qu'elle est eteinte, aucune
     * competence ne part — c'est aussi ce qui fait disparaitre les elements du HUD, la barre de
     * CP comprise.
     */
    private boolean activated;

    /**
     * Le joueur peut-il se servir de son aptitude, s'il en a une ?
     *
     * <p>L'original demandait DEUX choses : avoir une categorie, et l'avoir allumee.
     */
    public boolean isActivated() {
        return getHighestLevel() > 0 && activated;
    }

    /** Le drapeau brut, sans la condition de categorie : ce que la sauvegarde retient. */
    public boolean isActivatedRaw() {
        return activated;
    }

    public void setActivated(boolean value) {
        activated = value;
        markDirty();
    }

    private float controlPoint = cn.academy.Config.startingControlPoint();

    /** Compteur de synchronisation, et « quelque chose a change » : voir {@link #syncInterval()}. */
    private int tickSync;
    private boolean syncDirty;

    /**
     * Ce que l'usage des competences a ajoute au plafond de la reserve.
     *
     * <p>C'est le {@code addMaxCP} de l'original, et son reglage n'est pas un ornement : chez lui
     * la reserve grandit a mesure qu'on s'en sert, et c'est ce que montrent les deux nombres
     * entre parentheses du menu F4 ({@code CP: 4071/4071(4000.0+70.7)}).
     */
    private float addMaxControlPoint;

    /**
     * Plafond de base de la reserve par niveau, repris de {@code init_cp} de l'original.
     *
     * <p>C'est la meme echelle que dans la 1.12.2, a l'unite pres : 1800 points des le niveau 1,
     * 8000 au niveau 5. Le port y avait substitue un plafond de 100 avec un facteur 28 pour
     * retomber sur les couts des competences ; ce detour rendait surtout la recuperation
     * invisible — 0,03 point par tick au lieu de 0,54. La reserve est maintenant celle de
     * l'original, donc les couts, qui l'etaient deja, s'y lisent sans conversion.
     */
    private static final float[] BASE_MAX_CONTROL_POINT = {1800f, 1800f, 2800f, 4000f, 5800f, 8000f};

    /**
     * Ce que l'usage des competences peut ajouter a la reserve, repris de {@code add_cp}.
     *
     * <p>Comme pour le surcout, le plafond grandit a mesure qu'on se sert de ses pouvoirs, et
     * c'est cette table qui dit jusqu'ou.
     */
    private static final float[] MAX_ADD_CONTROL_POINT = {0f, 900f, 1000f, 1500f, 1700f, 12000f};

    /** Part du cout qui devient du plafond, reprise de {@code maxcp_incr_rate}. */
    private static final float CP_INCR_RATE = 0.0025f;

    /**
     * La part du plafond que la reserve regagne par tick, chez l'original ({@code 0,0003}).
     *
     * <p>Elle est <b>proportionnelle</b> au plafond : un joueur deux fois plus experimente
     * regagne deux fois plus de points, et met donc le meme temps a remplir sa barre.
     */
    private static final float CP_RECOVER_FRACTION = 0.0003f;

    /** Plafond de base de la reserve pour ce niveau. */
    public static float baseMaxControlPoint(int level) {
        return BASE_MAX_CONTROL_POINT[clampLevel(level)];
    }

    /** Gain de plafond maximal pour ce niveau. */
    public static float maxAddControlPoint(int level) {
        return MAX_ADD_CONTROL_POINT[clampLevel(level)];
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
     *
     * <p>Un vrai changement de niveau est aussi celui de la reserve : c'est la le
     * {@code LevelChangeEvent} de l'original, qui donnait la reserve du nouveau niveau,
     * pleine, en repartant de zero sur les ajouts.
     */
    public void setCategoryLevel(Category category, int level) {
        int previous = getCategoryLevel(category);
        categoryLevels.put(category.getName(), level);
        levelProgress.remove(category.getName());
        if (previous != level) {
            onLevelChanged();
        }
    }

    public boolean hasLearned(Category category) {
        return getCategoryLevel(category) > 0;
    }

    /**
     * Les categories que ce joueur connait, par leur nom.
     *
     * <p>Ce que la donnee retient vraiment : les niveaux sont ranges par <b>nom</b>, pour
     * qu'une categorie disparue ne fasse pas tomber une sauvegarde. C'est ce qui permet de
     * decider sans registre — et donc de relire la decision en test unitaire.
     */
    public Map<String, Integer> getCategoryLevels() {
        return Map.copyOf(categoryLevels);
    }

    public float getControlPoint() {
        return controlPoint;
    }

    public float getMaxControlPoint() {
        return getRawMaxControlPoint() + addMaxControlPoint + getPassiveMaxControlPoint();
    }

    /** Le plafond de base : celui du niveau, sans ce que l'usage a ajoute. */
    public float getRawMaxControlPoint() {
        return baseMaxControlPoint(getHighestLevel());
    }

    /** Ce que l'usage a ajoute au plafond. */
    public float getAddMaxControlPoint() {
        return addMaxControlPoint;
    }

    /**
     * Le plafond que les competences passives apprises ajoutent.
     *
     * <p>Portage des evenements {@code CalcEvent.MaxCP} / {@code MaxOverload} /
     * {@code CPRecoverSpeed} de l'original : la, chaque competence passive s'inscrivait aupres
     * du bus de Forge et ajoutait sa part au moment du calcul. Ici, la competence dit seulement
     * ce qu'elle donne ({@code Skill.getMaxControlPointBonus}) et la somme se fait au moment du
     * calcul, comme chez lui.
     *
     * <p>En test unitaire le registre des categories est vide, donc ces trois methodes rendent
     * 0 et 1 : un calcul de reserve n'y depend jamais de ce que le joueur a appris.
     */
    public float getPassiveMaxControlPoint() {
        float bonus = 0f;
        for (Category category : CategoryManager.INSTANCE.getCategories()) {
            for (Skill skill : getLearnedSkills(category)) {
                bonus += skill.getMaxControlPointBonus(this);
            }
        }
        return bonus;
    }

    /** Le plafond de surcout que les competences passives apprises ajoutent. */
    public float getPassiveMaxOverload() {
        float bonus = 0f;
        for (Category category : CategoryManager.INSTANCE.getCategories()) {
            for (Skill skill : getLearnedSkills(category)) {
                bonus += skill.getMaxOverloadBonus(this);
            }
        }
        return bonus;
    }

    /** Le facteur de recuperation des competences passives apprises, multiplie entre elles. */
    public float getPassiveRecoverScale() {
        float scale = 1f;
        for (Category category : CategoryManager.INSTANCE.getCategories()) {
            for (Skill skill : getLearnedSkills(category)) {
                scale *= skill.getControlPointRecoverScale(this);
            }
        }
        return scale;
    }

    /** Pose l'ajout, borne a ce que le niveau autorise et jamais negatif. */
    public void setAddMaxControlPoint(float value) {
        addMaxControlPoint = Math.min(maxAddControlPoint(getHighestLevel()), Math.max(0f, value));
    }

    /**
     * Le plafond suit le niveau, la reserve se remplit et l'ajout repart a zero.
     *
     * <p>Portage de {@code CPData.recalcMaxValue} et des evenements qui l'appelaient :
     * {@code changedLevel} remettait les deux ajouts a zero avant de recalculer, donc monter
     * d'un niveau donne la reserve pleine de ce niveau, et le compteur d'ajout repart de zero.
     * C'est le prix d'une reserve plus grande, et l'original le facturait ainsi.
     */
    private void onLevelChanged() {
        addMaxControlPoint = 0f;
        addMaxOverload = 0f;
        overload = 0f;
        overloadFine = true;
        untilOverloadRecover = 0;
        untilRecover = 0;
        controlPoint = getMaxControlPoint();
        markDirty();
    }

    public boolean consumeControlPoint(float amount) {
        return perform(amount, 0f);
    }

    public void tickRegen() {
        if (untilRecover > 0) {
            untilRecover--;
            return;
        }
        // Portage de CPData.getCPRecoverSpeed : une part du PLAFOND par tick, et non un
        // montant fixe, et cette part double quand la reserve est pleine. Sur 1800 points,
        // cela fait 0,54 point par tick a vide et 1,08 a ras bord : de quoi voir le
        // compteur avancer a chaque tick, comme chez l'original.
        float base = getRawMaxControlPoint();
        if (base <= 0f) return;
        float speed = (float) (cn.academy.Config.controlPointRegenSpeed * CP_RECOVER_FRACTION
                * base * (1f + Math.min(1f, controlPoint / base)));
        // L'entrainement mental (Mind Course) accelere la recuperation : c'est le
        // `CalcEvent.CPRecoverSpeed` de l'original, ou la competence multipliait la vitesse.
        speed *= getPassiveRecoverScale();
        controlPoint = Math.min(getMaxControlPoint(), controlPoint + speed);
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
     * Faux quand le surcout a atteint son maximum, et jusqu'a ce que la reserve soit vide.
     *
     * <p>C'est le verrou : tant qu'il est faux, plus aucune competence ne part. Il se releve
     * seulement quand la reserve est entierement redescendue, donc le joueur ne relance rien
     * pendant que sa surcharge reflue.
     */
    private boolean overloadFine = true;

    /** Ticks restants avant que le surcout ne redescende. */
    private int untilOverloadRecover;

    /**
     * Ticks restants avant que la reserve ne se remette a remonter.
     *
     * <p>Portage de {@code CPData.untilRecover} : chaque paiement arme ce delai
     * ({@code cp_recover_cooldown}, 15 ticks), pour qu'une salve de competences ne
     * s'autofinance pas tick par tick.
     */
    private int untilRecover;

    public float getOverload() {
        return overload;
    }

    public float getMaxOverload() {
        return baseMaxOverload(getHighestLevel()) + addMaxOverload + getPassiveMaxOverload();
    }

    /**
     * Force la reserve courante, bornee au plafond du niveau.
     *
     * Portage de {@code CPData.setCP} : la commande de debogage s'en sert pour rendre la
     * reserve pleine (`/aim fullcp`), comme l'original qui remplissait alors sa barre.
     */
    public void setControlPoint(float value) {
        controlPoint = Math.max(0f, Math.min(getMaxControlPoint(), value));
        markDirty();
    }

    /**
     * Force le surcout courant, borne a la reserve.
     *
     * Portage de {@code CPData.setOverload} : l'original s'en servait pour remettre le
     * surcout epingle d'un maintien apres que la recuperation l'a fait baisser.
     */
    public void setOverload(float value) {
        overload = Math.max(0f, Math.min(getMaxOverload(), value));
        markDirty();
    }

    /**
     * Quelque chose a change : le client doit le savoir au plus vite.
     *
     * <p>Portage de {@code CPData.markDirty} : c'est ce drapeau qui resserre la cadence de
     * synchronisation a 4 ticks (voir {@link #syncInterval()}). Il n'est PAS pose par la
     * recuperation, qui avance a chaque tick et se contente de la cadence de croisiere —
     * exactement comme l'original, ou seul {@code curCP += recover} ne marquait rien.
     */
    private void markDirty() {
        syncDirty = true;
    }

    /**
     * Ticks avant la prochaine envoi de l'etat au client, portage de {@code CPData.tick}.
     *
     * <p>L'original ne renvoyait pas son etat a cadence fixe :
     * {@code (aptitude allumee ? 1 : 3) x (quelque chose a change ? 4 : 10)}. Un paiement
     * partait donc vers le client en 4 ticks au pire, alors que la reserve attend son
     * delai de 15 ticks avant de remonter : le joueur voyait vraiment ses points partir.
     *
     * <p>Le port envoyait toutes les 20 ticks, donc TOUJOURS apres le debut de la reprise :
     * un coup de 70 points semblait en couter 60, et c'est ce qui a mis le joueur sur la
     * piste. C'est la cadence de l'original qui est revenue ici.
     */
    public int syncInterval() {
        // Le drapeau BRUT, comme chez lui : une aptitude eteinte n'a rien a envoyer vite.
        return (activated ? 1 : 3) * (syncDirty ? 4 : 10);
    }

    /**
     * Un tick du compteur de synchronisation ; vrai quand il faut envoyer l'etat.
     *
     * <p>Le compteur et le drapeau se remettent a zero ensemble : c'est l'envoi qui les
     * efface, comme le {@code dataDirty = false; tickSync = 0;} de l'original.
     */
    public boolean tickSync() {
        if (++tickSync < syncInterval()) return false;
        tickSync = 0;
        syncDirty = false;
        return true;
    }

    /** Part du plafond acquise en utilisant ses competences. */
    public float getAddMaxOverload() {
        return addMaxOverload;
    }

    /**
     * Vrai tant que la reserve de surcout tient le joueur : c'est le verrou des competences.
     *
     * <p>Portage de {@code CPData.canUseAbility}, qui ne regardait que ce drapeau.
     */
    public boolean isOverloadRecovering() {
        return !overloadFine;
    }

    /**
     * Vrai pendant la surcharge <b>montree</b> : le delai avant que la reserve ne redescende.
     *
     * <p>Portage de {@code CPData.isOverloaded}, qui exigeait les deux : le verrou ET le delai
     * en cours. Le temoin ne montre donc l'etat de surcharge que le temps de ce delai
     * ({@code overloadRecoverCooldown}, 32 ticks) ; passe ce delai la barre revient au fond
     * normal et sa bande de surcharge <b>reflue</b> vers la gauche, au lieu de rester plantee a
     * fond jusqu'a la derniere goutte.
     */
    public boolean isOverloaded() {
        return !overloadFine && untilOverloadRecover > 0;
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
        growMaxControlPoint(cp);
        addOverload(overloadCost);
        if (cp > 0f) untilRecover = cn.academy.Config.controlPointRecoverCooldown;
        markDirty();
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
        growMaxControlPoint(cp);
        addOverload(overloadCost);
        if (cp > 0f) untilRecover = cn.academy.Config.controlPointRecoverCooldown;
        markDirty();
    }

    /**
     * Agrandit la reserve quand une competence est payee.
     *
     * <p>Portage de {@code CPData.addMaxCP}, appele chez lui des que le paiement a reussi —
     * force ou non, et avec le montant DEMANDE, pas celui qui restait. C'est ce qui fait qu'un
     * joueur qui se sert de ses competences finit avec une reserve un peu plus grande, sans
     * jamais depasser ce que son niveau autorise.
     */
    private void growMaxControlPoint(float consumed) {
        if (consumed <= 0) return;
        setAddMaxControlPoint(addMaxControlPoint + consumed * CP_INCR_RATE);
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
            // Le franchissement du maximum, et non l'etat : le succes ne tombe qu'une fois.
            if (overloadFine) {
                cn.academy.advancements.AcademyAdvancements.award(owner,
                        cn.academy.advancements.AcademyAdvancements.AC_OVERLOAD);
            }
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

    /** Ticks restants de chute annulee, ou 0. */
    private int gravitySuspension;

    /**
     * Annule presque la chute pendant quelques ticks.
     *
     * <p>Portage du {@code GravityCancellor} de l'original, qu'il fallait relancer apres
     * chaque saut du scintillement : sans lui, un saut en l'air ne ferait que tomber un peu
     * plus loin. L'effet appartient a l'original au client ; le port le tient cote serveur,
     * ou la position fait autorite.
     */
    public void suspendGravity(int ticks) {
        gravitySuspension = Math.max(gravitySuspension, ticks);
    }

    public int getGravitySuspension() {
        return gravitySuspension;
    }

    /** Fait avancer la suspension de chute d'un tick. */
    public void tickGravitySuspension() {
        if (gravitySuspension > 0) gravitySuspension--;
    }

    /**
     * Retient a qui appartiennent ces donnees, pour les succes qui se declenchent ici.
     *
     * <p>Appele par le fournisseur de capacite, au moment de l'attachement. Le client
     * n'a pas de {@code ServerPlayer} : ses donnees gardent donc un proprietaire nul, ce
     * qui est exactement ce qu'on veut — un succes ne s'accorde que sur le serveur.
     */
    public void setOwner(net.minecraft.world.entity.player.Player player) {
        owner = player instanceof net.minecraft.server.level.ServerPlayer serverPlayer ? serverPlayer : null;
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
        addMaxControlPoint = other.addMaxControlPoint;
        activated = other.activated;
        overload = other.overload;
        addMaxOverload = other.addMaxOverload;
        overloadFine = other.overloadFine;
        untilOverloadRecover = other.untilOverloadRecover;
        untilRecover = other.untilRecover;
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
        boolean changed = learnedSkills.add(skillKey(skill));
        if (changed) {
            cn.academy.advancements.AcademyAdvancements.award(owner,
                    cn.academy.advancements.AcademyAdvancements.AC_LEARNING_SKILL);
        }
        return changed;
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
        // Une competence dont l'experience se calcule (voir Skill.hasComputedExp) n'en apprend
        // aucune : c'est l'etat du joueur qui parle, et il peut changer tout seul.
        if (skill.hasComputedExp()) return skill.computeExp(this);
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
        float reached = Math.min(1f, current + effective);
        skillExps.put(key, reached);

        // « Combien de fois as-tu fait ca ? » — le succes tombe quand une competence
        // atteint son maximum d'experience, pas a chaque emploi.
        if (current < 1f && reached >= 1f) {
            cn.academy.advancements.AcademyAdvancements.award(owner,
                    cn.academy.advancements.AcademyAdvancements.AC_EXP_FULL);
        }

        addLevelProgress(skill.getCategory(), effective);
    }

    /**
     * Force l'experience d'une competence, sans toucher au niveau en cours.
     *
     * <p>La voie normale est {@link #addSkillExp}, qui verse aussi de l'avancement : c'est le
     * jeu. La commande de debogage, elle, doit poser une valeur et rien d'autre — c'est la
     * contrepartie de `/aim exp <competence> <valeur>` de l'original, ou `setSkillExp`
     * ecrivait directement dans sa table.
     */
    public void setSkillExp(Skill skill, float exp) {
        if (skill == null || skill.getCategory() == null) return;
        skillExps.put(skillKey(skill), Math.max(0f, Math.min(1f, exp)));
        markDirty();
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
     * La duree totale de la recharge en cours, telle qu'elle a ete posee — c'est elle qui
     * donne son echelle au gris : sans elle on saurait qu'une competence est indisponible,
     * mais pas a quel point.
     *
     * <p>Le port relisait la COURBE ({@code skill.getCooldownTicks(data)}) pour ce nombre, ce
     * qui ne marche que pour les competences qui en declarent une. Les trois dont la recharge
     * est posee par l'effet — {@code dir_shock}, {@code location_teleport}, {@code flesh_ripping}
     * — annoncaient donc une duree nulle, et leur icone ne s'estompait jamais : c'est ce que le
     * joueur a vu (« seul le premier slot fait le gris »).
     */
    public int getCooldownTotal(Skill skill) {
        if (skill == null || skill.getCategory() == null) return 0;
        return cooldownTotals.getOrDefault(skillKey(skill), 0);
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
        cooldownTotals.merge(key, ticks, Math::max);
        // Une recharge qui commence se dit TOUT DE SUITE. Sans ce marquage, le client
        // n'apprenait qu'elle courait qu'a la prochaine synchronisation ordinaire (jusqu'a
        // trente ticks plus tard) : le rappel des touches ne s'estompait qu'apres coup, et le
        // joueur lisait cette latence comme un affichage faux.
        markDirty();
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
            String key = entry.getKey();
            int left = entry.getValue() - 1;
            if (left <= 0) {
                iterator.remove();
                cooldownTotals.remove(key);
            } else {
                entry.setValue(left);
            }
        }
    }

    /** Oublie toutes les recharges. Appele a la mort du joueur, comme l'original. */
    public void clearCooldowns() {
        cooldowns.clear();
        cooldownTotals.clear();
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

    /**
     * Pose un repere de plus, dans l'ordre.
     *
     * C'est ce qui permet a un maintien d'accumuler quelque chose tick apres tick sans
     * que le paquet d'activation ait a s'en meler : la bombe a fragmentation pose une
     * bille a chaque dizaine de ticks, et les relit toutes a la fin.
     */
    public void addHoldPoint(Skill skill, net.minecraft.world.phys.Vec3 point) {
        Hold hold = holdOf(skill);
        if (hold != null) hold.points.add(point);
    }

    /** Les reperes poses par ce maintien, en copie : l'appelant peut les parcourir. */
    public List<net.minecraft.world.phys.Vec3> getHoldPoints(Skill skill) {
        Hold hold = holds.get(skill);
        return hold == null ? List.of() : List.copyOf(hold.points);
    }

    /** Les billes accumulees par ce maintien ; 0 s'il n'en a pas. */
    public int getHoldBalls(Skill skill) {
        Hold hold = holds.get(skill);
        return hold == null ? 0 : hold.balls;
    }

    /** Pose le nombre de billes du maintien. */
    public void setHoldBalls(Skill skill, int balls) {
        Hold hold = holdOf(skill);
        if (hold != null) hold.balls = balls;
    }

    /**
     * Le vol libre du joueur avant ce maintien, pour le rendre a la fin.
     *
     * C'est le {@code prevAllowFlying} du contexte de l'original : sans lui, une competence
     * qui ouvre le vol le laisserait ouvert pour toujours.
     */
    public void setHoldFlying(Skill skill, boolean flying) {
        Hold hold = holdOf(skill);
        if (hold != null) hold.flying = flying;
    }

    public boolean getHoldFlying(Skill skill) {
        Hold hold = holds.get(skill);
        return hold != null && hold.flying;
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
        tag.putFloat("addCp", addMaxControlPoint);
        tag.putInt("untilRecover", untilRecover);
        // L'etat allume/eteint voyage avec le reste : se reconnecter ne doit pas eteindre
        // l'aptitude du joueur, et le HUD du client en depend.
        tag.putBoolean("activated", activated);
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

        // Les durees voyagent avec les ticks restants : c'est ce qui donne son echelle au gris
        // de l'icone chez le client, qui n'a pas de courbe a relire.
        CompoundTag cooldownTotalTags = new CompoundTag();
        cooldownTotals.forEach(cooldownTotalTags::putInt);
        tag.put(TAG_COOLDOWN_TOTALS, cooldownTotalTags);

        // Le surcout voyage avec le reste : l'original le sauvegardait aussi, donc se
        // reconnecter en pleine surcharge ne remet pas la reserve a zero.
        tag.putFloat("overload", overload);
        tag.putFloat("addOverload", addMaxOverload);
        tag.putBoolean("overloadFine", overloadFine);
        tag.putInt("untilOverloadRecover", untilOverloadRecover);

        // Les marques voyagent avec le reste : l'ecran de la teleportation les affiche, et
        // le client ne peut pas les deviner.
        ListTag markTags = new ListTag();
        for (cn.academy.ability.teleporter.LocationMark mark : marks) {
            markTags.add(mark.save());
        }
        tag.put(TAG_MARKS, markTags);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        categoryLevels.clear();
        CompoundTag levels = tag.getCompound("levels");
        for (String key : levels.getAllKeys()) {
            categoryLevels.put(key, levels.getInt(key));
        }
        setAddMaxControlPoint(tag.getFloat("addCp"));
        // Une sauvegarde d'avant les tables de niveau peut porter plus de points que le
        // plafond du joueur : ils sont rognes plutot que d'etre perdus plus tard sans mot.
        controlPoint = Math.max(0f, Math.min(getMaxControlPoint(), tag.getFloat("cp")));
        untilRecover = tag.getInt("untilRecover");
        activated = tag.getBoolean("activated");
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
        cooldownTotals.clear();
        CompoundTag cds = tag.getCompound(TAG_COOLDOWNS);
        for (String key : cds.getAllKeys()) {
            int left = cds.getInt(key);
            cooldowns.put(key, left);
            // Une sauvegarde d'avant les durees memoirees n'a que les ticks restants : on prend
            // ce qu'on a, sinon le gris n'aurait aucune echelle au lieu d'une approximative.
            cooldownTotals.put(key, left);
        }
        CompoundTag totalTags = tag.getCompound(TAG_COOLDOWN_TOTALS);
        for (String key : totalTags.getAllKeys()) {
            cooldownTotals.merge(key, totalTags.getInt(key), Math::max);
        }

        overload = tag.getFloat("overload");
        addMaxOverload = tag.getFloat("addOverload");
        // Une sauvegarde sans le drapeau est une sauvegarde d'avant la surcharge :
        // le joueur doit repartir disponible, pas bloque par un booleen par defaut.
        overloadFine = !tag.contains("overloadFine") || tag.getBoolean("overloadFine");
        untilOverloadRecover = tag.getInt("untilOverloadRecover");

        marks.clear();
        ListTag markTags = tag.getList(TAG_MARKS, Tag.TAG_COMPOUND);
        for (int i = 0; i < markTags.size(); i++) {
            cn.academy.ability.teleporter.LocationMark mark =
                    cn.academy.ability.teleporter.LocationMark.load(markTags.getCompound(i));
            // Une dimension disparue laisse un trou, et non une exception : les autres
            // marques doivent survivre a celle-la.
            if (mark != null) marks.add(mark);
        }
    }
}
