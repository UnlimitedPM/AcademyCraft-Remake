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
    public static final VecDeviationSkill VEC_DEVIATION = new VecDeviationSkill();
    public static final StormWingSkill STORM_WING = new StormWingSkill();
    public static final PlasmaCannonSkill PLASMA_CANNON = new PlasmaCannonSkill();

    public static final VecmanipCategory INSTANCE = new VecmanipCategory();

    private VecmanipCategory() {
        super(NAME);
        // La teinte de la categorie, telle que l'original la posait sur CatVecManip : trois
        // canaux et aucune opacite, donc aucun voile. C'est voulu chez lui, pas un oubli.
        setColorStyle(0, 0, 0);
        // L'ORDRE EST CELUI DE L'ORIGINAL, et il se voit : le menu F4 liste les competences dans
        // cet ordre-la, niveau par niveau. C'est celui de CatVecManip ; le port les rangeait
        // dans l'ordre ou elles avaient ete codees, ce qui melangeait les niveaux.
        addSkill(DIRECTED_SHOCK);
        addSkill(GROUNDSHOCK);
        addSkill(VEC_ACCEL);
        addSkill(VEC_DEVIATION);
        addSkill(DIRECTED_BLASTWAVE);
        addSkill(STORM_WING);
        addSkill(BLOOD_RETROGRADE);
        addSkill(VEC_REFLECTION);
        addSkill(PLASMA_CANNON);

        // Les trois cursus generiques ferment la categorie, comme dans l'original.
        cn.academy.ability.generic.GenericSkills.addTo(this);

        // L'arbre de l'original part du choc dirige : c'est lui qui apprend a pousser, et
        // tout vecmanip en descend. L'acceleration de vecteur et l'onde de choc lui doivent
        // donc leur dependance, et sans seuil d'experience — avoir appris le coup suffit.
        // L'onde dirigee, elle, descend de l'onde au sol : on n'apprend pas a viser avant
        // d'avoir appris a frapper le sol.
        VEC_ACCEL.setParent(DIRECTED_SHOCK);
        GROUNDSHOCK.setParent(DIRECTED_SHOCK);
        DIRECTED_BLASTWAVE.setParent(GROUNDSHOCK);
        BLOOD_RETROGRADE.setParent(DIRECTED_BLASTWAVE);
        // La deviation descend de l'acceleration : on n'apprend pas a arreter ce qui vole
        // avant d'avoir appris a se propulser soi-meme.
        VEC_DEVIATION.setParent(VEC_ACCEL);
        // Et la reflexion descend de la deviation : arreter ce qui vole s'apprend avant de
        // le retourner, exactement comme dans l'arbre de l'original.
        VEC_REFLECTION.setParent(VEC_DEVIATION);
        // Les ailes de tempete descendent de l'acceleration, comme la deviation : on
        // n'apprend pas a voler avant d'avoir appris a se propulser.
        STORM_WING.setParent(VEC_ACCEL);
        // Et le canon a plasma descend des ailes : c'est la derniere competence de la
        // categorie, et la plus chere.
        PLASMA_CANNON.setParent(STORM_WING);
    }
}
