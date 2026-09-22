package cn.academy.ability.teleporter;

import cn.academy.ability.Category;

/** Teleporter ability category. */
public class TeleporterCategory extends Category {

    public static final String NAME = "teleporter";

    public static final ShiftTeleportSkill SHIFT_TELEPORT = new ShiftTeleportSkill();
    public static final PenetrateTeleportSkill PENETRATE_TELEPORT = new PenetrateTeleportSkill();
    public static final DimFoldingTheoremSkill DIM_FOLDING_THEOREM = new DimFoldingTheoremSkill();
    public static final ThreateningTeleportSkill THREATENING_TELEPORT = new ThreateningTeleportSkill();
    public static final MarkTeleportSkill MARK_TELEPORT = new MarkTeleportSkill();

    public static final TeleporterCategory INSTANCE = new TeleporterCategory();

    private TeleporterCategory() {
        super(NAME);
        addSkill(SHIFT_TELEPORT);
        addSkill(PENETRATE_TELEPORT);
        addSkill(DIM_FOLDING_THEOREM);
        addSkill(THREATENING_TELEPORT);
        addSkill(MARK_TELEPORT);

        // L'arbre de l'original part du lancer d'objet : c'est lui qui apprend a
        // viser, et tout le reste en descend. Les deux competences concernees sont
        // portees, donc la dependance est reposee ; celles qui manquent encore
        // (mark_teleport, location_teleport, flashing, flesh_ripping) attendent leur
        // tour, une dependance vers une competence absente rendant la competence
        // inapprenable pour toujours.
        DIM_FOLDING_THEOREM.setParent(THREATENING_TELEPORT, 0.2f);
        PENETRATE_TELEPORT.setParent(THREATENING_TELEPORT, 0.5f);
        MARK_TELEPORT.setParent(THREATENING_TELEPORT, 0.4f);
    }
}
