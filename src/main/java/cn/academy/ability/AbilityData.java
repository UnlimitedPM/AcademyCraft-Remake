package cn.academy.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-player ability state: learning progress per category and the Control Point resource
 * consumed by active skills. Port of original DevelopData/AbilityData, simplified.
 */
public class AbilityData implements INBTSerializable<CompoundTag> {

    private final Map<String, Integer> categoryLevels = new HashMap<>();

    private float controlPoint = 100f;
    private float maxControlPoint = 100f;

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
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        CompoundTag levels = new CompoundTag();
        categoryLevels.forEach(levels::putInt);
        tag.put("levels", levels);
        tag.putFloat("cp", controlPoint);
        tag.putFloat("maxCp", maxControlPoint);
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
        maxControlPoint = tag.contains("maxCp") ? tag.getFloat("maxCp") : 100f;
    }
}
