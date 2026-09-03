package cn.academy.ability.vecmanip;

import cn.academy.ability.Category;

/** Vector Manipulation (Accelerator-type) ability category. First ported category, used as pilot. */
public class VecmanipCategory extends Category {

    public static final String NAME = "vecmanip";

    // Must be initialized before INSTANCE: the constructor below registers them.
    public static final VecReflectionSkill VEC_REFLECTION = new VecReflectionSkill();
    public static final VecAccelSkill VEC_ACCEL = new VecAccelSkill();

    public static final VecmanipCategory INSTANCE = new VecmanipCategory();

    private VecmanipCategory() {
        super(NAME);
        addSkill(VEC_REFLECTION);
        addSkill(VEC_ACCEL);
    }
}
