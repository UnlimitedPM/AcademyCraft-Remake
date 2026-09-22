package cn.academy.ability.meltdowner;

import cn.academy.ability.Category;

/** Meltdowner (plasma/radiation-type) ability category. */
public class MeltdownerCategory extends Category {

    public static final String NAME = "meltdowner";

    public static final MeltdownerSkill MELTDOWNER = new MeltdownerSkill();
    public static final ElectronBombSkill ELECTRON_BOMB = new ElectronBombSkill();
    public static final LightShieldSkill LIGHT_SHIELD = new LightShieldSkill();
    public static final MineRayBasicSkill MINE_RAY_BASIC = new MineRayBasicSkill();
    public static final MineRayExpertSkill MINE_RAY_EXPERT = new MineRayExpertSkill();
    public static final MineRayLuckSkill MINE_RAY_LUCK = new MineRayLuckSkill();

    public static final MeltdownerCategory INSTANCE = new MeltdownerCategory();

    private MeltdownerCategory() {
        super(NAME);
        addSkill(MELTDOWNER);
        addSkill(ELECTRON_BOMB);
        addSkill(LIGHT_SHIELD);
        addSkill(MINE_RAY_BASIC);
        addSkill(MINE_RAY_EXPERT);
        addSkill(MINE_RAY_LUCK);

        // Dependances de l'original dont les deux bouts sont portes. Les autres
        // (scatter_bomb, jet_engine, et l'ecran de la bombe a electrons) attendent leurs
        // competences.
        LIGHT_SHIELD.setParent(ELECTRON_BOMB);
        MELTDOWNER.addDependency(LIGHT_SHIELD);
        // Les rayons miniers se suivent : l'expert demande le premier, le chanceux
        // demande l'expert, comme dans l'original.
        MINE_RAY_BASIC.setParent(MELTDOWNER, 0.3f);
        MINE_RAY_EXPERT.setParent(MINE_RAY_BASIC, 0.8f);
        MINE_RAY_LUCK.setParent(MINE_RAY_EXPERT, 1f);
    }
}
