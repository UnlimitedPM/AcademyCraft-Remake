package cn.academy.ability;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Base unit of an ability category (port of original Skill+Controllable merged).
 * A skill is either "active" (triggered by a key, costs Control Points) or "passive"
 * (always in effect while learned, hooks into gameplay events like onDamaged).
 */
public abstract class Skill {

    private final String name;
    private Category category;
    private int id = -1;

    protected Skill(String name) {
        this.name = name;
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
