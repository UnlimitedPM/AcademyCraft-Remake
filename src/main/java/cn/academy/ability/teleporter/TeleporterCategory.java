package cn.academy.ability.teleporter;

import cn.academy.ability.Category;

/** Teleporter ability category. */
public class TeleporterCategory extends Category {

    public static final String NAME = "teleporter";

    public static final ShiftTeleportSkill SHIFT_TELEPORT = new ShiftTeleportSkill();
    public static final PenetrateTeleportSkill PENETRATE_TELEPORT = new PenetrateTeleportSkill();
    public static final DimFoldingTheoremSkill DIM_FOLDING_THEOREM = new DimFoldingTheoremSkill();

    public static final TeleporterCategory INSTANCE = new TeleporterCategory();

    private TeleporterCategory() {
        super(NAME);
        addSkill(SHIFT_TELEPORT);
        addSkill(PENETRATE_TELEPORT);
        addSkill(DIM_FOLDING_THEOREM);
    }
}
