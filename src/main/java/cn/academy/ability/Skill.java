package cn.academy.ability;

import cn.academy.ability.develop.condition.ConditionDependency;
import cn.academy.ability.develop.condition.ConditionLevel;
import cn.academy.ability.develop.condition.LearningCondition;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Base unit of an ability category (port of original Skill+Controllable merged).
 * A skill is either "active" (triggered by a key, costs Control Points) or "passive"
 * (always in effect while learned, hooks into gameplay events like onDamaged).
 *
 * <h2>Apprentissage</h2>
 *
 * Une competence se situe a un <b>niveau</b> de sa categorie et porte les
 * conditions a remplir pour l'apprendre. C'est le portage de {@code Skill} de la
 * 1.12.2, qui melangeait deja les deux cotes de la competence — ce qu'elle fait et
 * comment on l'obtient.
 */
public abstract class Skill {

    private final String name;
    private final int level;
    private final List<LearningCondition> conditions = new ArrayList<>();
    private final List<Skill> dependencies = new ArrayList<>();
    private Category category;
    private int id = -1;

    @Nullable
    private Skill parent;

    /**
     * Competence sans niveau : elle n'exige que le niveau 0 de sa categorie.
     *
     * Reserve aux competences qui n'ont pas de place dans une progression — les
     * doublures des tests. Une vraie competence declare son niveau : un test verifie
     * qu'aucune competence enregistree ne reste au niveau 0, sinon une omission se
     * verrait seulement en jeu, une competence offerte d'office.
     */
    protected Skill(String name) {
        this(name, 0);
    }

    protected Skill(String name, int level) {
        this.name = name;
        this.level = level;
        // Comme dans l'original : toute competence exige au moins le niveau ou elle
        // se trouve. Pour une competence de niveau 0, la condition est toujours vraie.
        this.conditions.add(ConditionLevel.INSTANCE);
    }

    final void bind(Category category, int id) {
        this.category = category;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public int getId() {
        return id;
    }

    /** Niveau de la categorie a partir duquel cette competence peut s'apprendre. */
    public int getLevel() {
        return level;
    }

    /**
     * Nombre de stimulations au developpeur pour l'apprendre.
     *
     * Formule reprise telle quelle de l'original : trois stimulations de base, et un
     * demi-carre du niveau en plus. Apprendre une competence de niveau 5 coute donc
     * cinq fois plus cher que celle de niveau 1.
     */
    public int getLearningStims() {
        return (int) (3 + level * level * 0.5f);
    }

    /** Cle de langue du nom affiche, comme {@code ac.ability.<categorie>.<nom>.name}. */
    public String getDisplayKey() {
        String categoryName = category == null ? "" : category.getName();
        return "ac.ability." + categoryName + "." + name + ".name";
    }

    public Component getDisplayName() {
        return Component.translatable(getDisplayKey());
    }

    // ------------------------------------------------------------------
    // Apprentissage : conditions et dependances
    // ------------------------------------------------------------------

    public void addCondition(LearningCondition condition) {
        conditions.add(condition);
    }

    public List<LearningCondition> getConditions() {
        return List.copyOf(conditions);
    }

    /**
     * Declare qu'une autre competence doit etre apprise avant celle-ci.
     *
     * Reprend {@code addSkillDep} de l'original. Une competence dependante de
     * elle-meme est refusee a la construction : c'est une boucle impossible dont le
     * seul effet serait de rendre la competence inapprenable sans rien dire.
     */
    public void addDependency(Skill dependency) {
        addDependency(dependency, 0f);
    }

    /**
     * Declare une dependance qui exige en plus un seuil d'experience dans la parente.
     *
     * Reprend {@code addSkillDep(skill, exp)} de l'original. Une dependance declaree
     * sans seuil vaut 0, donc « apprise » suffit.
     */
    public void addDependency(Skill dependency, float requiredExp) {
        if (dependency == this) {
            throw new IllegalArgumentException("A skill cannot depend on itself: " + name);
        }
        dependencies.add(dependency);
        conditions.add(new ConditionDependency(dependency, requiredExp));
    }

    public List<Skill> getDependencies() {
        return List.copyOf(dependencies);
    }

    /**
     * La competence dont celle-ci decoule dans l'arbre.
     *
     * Reprend {@code setParent}, qui posait du meme coup une condition : dans
     * l'original, la competence parente devait etre apprise. La parente sert donc a
     * deux choses ici — decider ce qui merite d'etre montre, et bloquer
     * l'apprentissage tant qu'elle n'est pas apprise.
     */
    public void setParent(Skill parent) {
        if (this.parent != null) {
            throw new IllegalStateException("Parent already set on " + name);
        }
        if (parent == this) {
            throw new IllegalArgumentException("A skill cannot be its own parent: " + name);
        }
        this.parent = parent;
        addDependency(parent);
    }

    @Nullable
    public Skill getParent() {
        return parent;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public boolean isPassive() {
        return false;
    }

    // ------------------------------------------------------------------
    // Experience d'usage
    // ------------------------------------------------------------------

    /**
     * Experience gagnee en utilisant cette competence.
     *
     * Chaque competence de l'original appelait {@code ctx.addSkillExp(...)} avec son
     * propre montant, au moment ou son effet aboutissait : un coup porte, un bouclier
     * qui encaisse, une distance de teleportation. Le paquet d'activation ne peut pas
     * savoir tout cela, donc la valeur est declaree ici et versee a l'activation (voir
     * {@code ActivateSkillPacket}), au montant de base de l'original. Les competences
     * passives, qui ne s'activent pas, ont le leur verse depuis leur propre crochet.
     *
     * Zero signifie « pas encore porte » : un test verifie qu'aucune competence livree
     * n'est restee a zero, sinon elle rapporterait silencieusement une progression nulle
     * et le niveau serait inatteignable.
     */
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    /**
     * Multiplicateur d'experience, porte par la competence.
     *
     * L'original en faisait un reglage par competence ({@code exp_incr_speed}, defaut
     * 1.0). La config du port est plate et n'a pas cette arborescence, donc elle est
     * ici sans reglage : le crochet existe pour le jour ou elle l'aura.
     */
    public float getExpIncrSpeed() {
        return 1f;
    }

    /**
     * Duree de la recharge de cette competence, en ticks. 0 = aucune.
     *
     * L'original appelait {@code ctx.setCooldown(...)} apres l'effet, avec une courbe
     * qui dependait de l'experience (15 a 5 ticks pour un arc, 300 a 160 pour le
     * railgun). Le port la declare ici et la pose a l'activation, pour que toutes les
     * competences suivent la meme regle : une competence qui part consomme sa recharge,
     * meme si sa cible lui echappe.
     *
     * Deux competences n'ont pas la leur : celles dont la recharge de l'original
     * dependait du temps de charge du tir (le bouclier et le meltdowner) attendent que
     * ce temps de charge soit porte.
     */
    public int getCooldownTicks(AbilityData data) {
        return 0;
    }

    /**
     * Surcout demande par une activation.
     *
     * Portage de la seconde ressource de l'original ({@code ctx.consume(overload, cp)}) :
     * en plus des CP, chaque competence charge une reserve qui se remplit et met le
     * joueur en surcharge quand elle est pleine. Contrairement aux couts en CP de
     * l'original, ceux du surcout tiennent dans la meme echelle que le port, donc ce
     * sont les vrais chiffres de la 1.12.2.
     */
    public float getOverloadCost(AbilityData data) {
        return 0f;
    }

    // ------------------------------------------------------------------
    // Temps de charge
    // ------------------------------------------------------------------

    /**
     * Cette competence demande-t-elle de garder la touche enfoncee ?
     *
     * L'original avait deux familles : celles qui partent a l'appui (l'arc, le
     * teleport), et celles qui se chargent (le meltdowner, l'acceleration de vecteur).
     * Une competence qui se charge est executee au relachement, avec le temps qu'elle a
     * accumule, que ses degats comme sa recharge peuvent lire via
     * {@code data.getChargeTicks(this)}.
     */
    public boolean isChargeable() {
        return false;
    }

    /** Nombre de ticks de charge au-dela duquel la competence ne gagne plus rien. */
    public int getMaxChargeTicks(AbilityData data) {
        return 0;
    }

    /**
     * Nombre de ticks de charge en dessous duquel la competence ne part pas.
     *
     * Reprend {@code TICKS_MIN} du meltdowner de l'original : relacher trop tot ne
     * declenche rien du tout, et ne coute donc rien.
     */
    public int getMinChargeTicks(AbilityData data) {
        return 0;
    }

    /**
     * Execution apres une charge.
     *
     * Par defaut, l'activation ordinaire : une competence qui ne se charge pas n'a pas
     * a connaitre cette methode.
     */
    public void onActivateCharged(Player player, AbilityData data, int chargeTicks) {
        onActivate(player, data);
    }

    /**
     * Interpolation lineaire entre deux valeurs, {@code t} ramene entre 0 et 1.
     *
     * Portage de {@code MathUtils.lerpf} : l'original s'en servait partout pour faire
     * grandir la puissance d'une competence avec son experience. Le port ne s'en sert
     * pour l'instant que pour l'experience elle-meme ; les courbes de puissance par
     * competence viendront s'y brancher.
     */
    protected static float lerp(float from, float to, float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        return from + (to - from) * clamped;
    }

    /** Control Points consumed each time this skill is activated. */
    public float getCpCost() {
        return 0f;
    }

    /** Called server-side when the player triggers this skill's key. */
    public void onActivate(Player player, AbilityData data) {}

    /**
     * Called server-side for every passive skill of every learned category when the
     * owning player takes damage. Return the (possibly modified) damage amount.
     */
    public float onDamaged(Player player, AbilityData data, LivingHurtEvent event) {
        return event.getAmount();
    }

    /**
     * Applique le multiplicateur global de degats ({@code general.damageScale}) a
     * une valeur de degats de base. Les competences actives doivent passer par ici
     * plutot que d'appeler {@code hurt()} avec leur constante brute, afin que le
     * reglage de config reste effectif partout.
     */
    protected static float scaled(float baseDamage) {
        return baseDamage * (float) cn.academy.Config.damageScale;
    }
}
