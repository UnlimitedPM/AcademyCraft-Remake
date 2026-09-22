package cn.academy.ability.vecmanip;

import cn.academy.ability.Category;

/** Vector Manipulation (Accelerator-type) ability category. First ported category, used as pilot. */
public class VecmanipCategory extends Category {

    public static final String NAME = "vecmanip";

    // Must be initialized before INSTANCE: the constructor below registers them.
    public static final DirectedShockSkill DIRECTED_SHOCK = new DirectedShockSkill();
    public static final GroundshockSkill GROUNDSHOCK = new GroundshockSkill();
    public static final DirectedBlastwaveSkill DIRECTED_BLASTWAVE = new DirectedBlastwaveSkill();
    public static final BloodRetrogradeSkill BLOOD_RETROGRADE = new BloodRetrogradeSkill();
    public static final VecReflectionSkill VEC_REFLECTION = new VecReflectionSkill();
    public static final VecAccelSkill VEC_ACCEL = new VecAccelSkill();

    public static final VecmanipCategory INSTANCE = new VecmanipCategory();

    private VecmanipCategory() {
        super(NAME);
        addSkill(DIRECTED_SHOCK);
        addSkill(GROUNDSHOCK);
        addSkill(DIRECTED_BLASTWAVE);
        addSkill(BLOOD_RETROGRADE);
        addSkill(VEC_REFLECTION);
        addSkill(VEC_ACCEL);

        // L'arbre de l'original part du choc dirige : c'est lui qui apprend a pousser, et
        // tout vecmanip en descend. L'acceleration de vecteur et l'onde de choc lui doivent
        // donc leur dependance, et sans seuil d'experience — avoir appris le coup suffit.
        // L'onde dirigee, elle, descend de l'onde au sol : on n'apprend pas a viser avant
        // d'avoir appris a frapper le sol.
        VEC_ACCEL.setParent(DIRECTED_SHOCK);
        GROUNDSHOCK.setParent(DIRECTED_SHOCK);
        DIRECTED_BLASTWAVE.setParent(GROUNDSHOCK);
        BLOOD_RETROGRADE.setParent(DIRECTED_BLASTWAVE);
    }
}
