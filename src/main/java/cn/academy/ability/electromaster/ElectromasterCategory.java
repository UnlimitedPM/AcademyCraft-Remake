package cn.academy.ability.electromaster;

import cn.academy.ability.Category;

/** Electromaster (Railgun-type) ability category. */
public class ElectromasterCategory extends Category {

    public static final String NAME = "electromaster";

    public static final ArcGenSkill ARC_GEN = new ArcGenSkill();
    public static final RailgunSkill RAILGUN = new RailgunSkill();
    public static final BodyIntensifySkill BODY_INTENSIFY = new BodyIntensifySkill();
    public static final ThunderBoltSkill THUNDER_BOLT = new ThunderBoltSkill();

    public static final ElectromasterCategory INSTANCE = new ElectromasterCategory();

    private ElectromasterCategory() {
        super(NAME);
        addSkill(ARC_GEN);
        addSkill(RAILGUN);
        addSkill(BODY_INTENSIFY);
        addSkill(THUNDER_BOLT);

        // Les dependances de l'original dont les deux bouts sont portes. Les autres
        // chaines passent par des competences qui n'existent pas encore ici (mag_manip,
        // mag_movement, current_charging...), et une dependance vers une competence
        // absente rendrait la competence inapprenable pour toujours. Elles seront
        // reposees en meme temps que ces competences.
        BODY_INTENSIFY.setParent(ARC_GEN);
        THUNDER_BOLT.setParent(ARC_GEN);
        // L'original demandait 30 % d'experience dans le thunder bolt avant de
        // debloquer le railgun : l'avoir appris ne suffisait pas.
        RAILGUN.setParent(THUNDER_BOLT, 0.3f);
    }
}
