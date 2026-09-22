package cn.academy.ability.meltdowner;

import cn.academy.ability.Category;

/** Meltdowner (plasma/radiation-type) ability category. */
public class MeltdownerCategory extends Category {

    public static final String NAME = "meltdowner";

    public static final MeltdownerSkill MELTDOWNER = new MeltdownerSkill();
    public static final ElectronBombSkill ELECTRON_BOMB = new ElectronBombSkill();
    public static final LightShieldSkill LIGHT_SHIELD = new LightShieldSkill();

    public static final MeltdownerCategory INSTANCE = new MeltdownerCategory();

    private MeltdownerCategory() {
        super(NAME);
        addSkill(MELTDOWNER);
        addSkill(ELECTRON_BOMB);
        addSkill(LIGHT_SHIELD);

        // Dependances de l'original dont les deux bouts sont portes. Les autres
        // (scatter_bomb, jet_engine, mine_ray_*...) attendent leurs competences.
        LIGHT_SHIELD.setParent(ELECTRON_BOMB);
        MELTDOWNER.addDependency(LIGHT_SHIELD);
    }
}
