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
    public static final FleshRippingSkill FLESH_RIPPING = new FleshRippingSkill();
    public static final FlashingSkill FLASHING = new FlashingSkill();

    public static final TeleporterCategory INSTANCE = new TeleporterCategory();

    private TeleporterCategory() {
        super(NAME);
        addSkill(SHIFT_TELEPORT);
        addSkill(PENETRATE_TELEPORT);
        addSkill(DIM_FOLDING_THEOREM);
        addSkill(THREATENING_TELEPORT);
        addSkill(MARK_TELEPORT);
        addSkill(FLESH_RIPPING);
        addSkill(FLASHING);

        // L'arbre de l'original part du lancer d'objet : c'est lui qui apprend a
        // viser, et tout le reste en descend. Les competences dont les deux bouts sont
        // portes reposent leur dependance ; celles qui manquent encore
        // (location_teleport, et la fluctuation d'espace qui descend d'elle) attendent
        // leur tour, une dependance vers une competence absente rendant la competence
        // inapprenable pour toujours.
        DIM_FOLDING_THEOREM.setParent(THREATENING_TELEPORT, 0.2f);
        PENETRATE_TELEPORT.setParent(THREATENING_TELEPORT, 0.5f);
        MARK_TELEPORT.setParent(THREATENING_TELEPORT, 0.4f);
        // L'original demandait les deux : savoir marquer, et savoir traverser.
        FLESH_RIPPING.setParent(MARK_TELEPORT, 0.5f);
        FLESH_RIPPING.addDependency(PENETRATE_TELEPORT, 0.5f);
        // Le scintillement descend du saut court, avec la meme exigence que l'original :
        // presque toute l'experience du saut, donc l'avoir beaucoup pratique.
        FLASHING.setParent(SHIFT_TELEPORT, 0.8f);
    }
}
