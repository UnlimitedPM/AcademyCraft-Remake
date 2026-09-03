package cn.academy.ability.electromaster;

import cn.academy.ability.Category;

/** Electromaster (Railgun-type) ability category. */
public class ElectromasterCategory extends Category {

    public static final String NAME = "electromaster";

    public static final ArcGenSkill ARC_GEN = new ArcGenSkill();
    public static final RailgunSkill RAILGUN = new RailgunSkill();
    public static final BodyIntensifySkill BODY_INTENSIFY = new BodyIntensifySkill();

    public static final ElectromasterCategory INSTANCE = new ElectromasterCategory();

    private ElectromasterCategory() {
        super(NAME);
        addSkill(ARC_GEN);
        addSkill(RAILGUN);
        addSkill(BODY_INTENSIFY);
    }
}
