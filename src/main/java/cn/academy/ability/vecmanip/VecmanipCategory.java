package cn.academy.ability.vecmanip;

import cn.academy.ability.Category;

/** Vector Manipulation (Accelerator-type) ability category. First ported category, used as pilot. */
public class VecmanipCategory extends Category {

    public static final String NAME = "vecmanip";

    // Must be initialized before INSTANCE: the constructor below registers them.
    public static final DirectedShockSkill DIRECTED_SHOCK = new DirectedShockSkill();
    public static final VecReflectionSkill VEC_REFLECTION = new VecReflectionSkill();
    public static final VecAccelSkill VEC_ACCEL = new VecAccelSkill();

    public static final VecmanipCategory INSTANCE = new VecmanipCategory();

    private VecmanipCategory() {
        super(NAME);
        addSkill(DIRECTED_SHOCK);
        addSkill(VEC_REFLECTION);
        addSkill(VEC_ACCEL);

        // L'arbre de l'original part du choc dirige : c'est lui qui apprend a pousser, et
        // tout vecmanip en descend. L'acceleration de vecteur lui doit donc sa dependance,
        // et sans seuil d'experience — avoir appris le coup suffit.
        VEC_ACCEL.setParent(DIRECTED_SHOCK);
    }
}
