package cn.academy.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Per-player ability state: learning progress per category and the Control Point resource
 * consumed by active skills. Port of original DevelopData/AbilityData, simplified.
 */
public class AbilityData implements INBTSerializable<CompoundTag> {

    private final Map<String, Integer> categoryLevels = new HashMap<>();

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

    public void setCategoryLevel(Category category, int level) {
        categoryLevels.put(category.getName(), level);
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
    }
}
