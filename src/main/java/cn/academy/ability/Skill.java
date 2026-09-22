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
        if (dependency == this) {
            throw new IllegalArgumentException("A skill cannot depend on itself: " + name);
        }
        dependencies.add(dependency);
        conditions.add(new ConditionDependency(dependency));
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
