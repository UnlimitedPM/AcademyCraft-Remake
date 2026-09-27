package cn.academy.ability.meltdowner;

import cn.academy.ability.Category;

/** Meltdowner (plasma/radiation-type) ability category. */
public class MeltdownerCategory extends Category {

    public static final String NAME = "meltdowner";

    public static final MeltdownerSkill MELTDOWNER = new MeltdownerSkill();
    public static final ElectronBombSkill ELECTRON_BOMB = new ElectronBombSkill();
    public static final RadiationIntensifySkill RADIATION_INTENSIFY = new RadiationIntensifySkill();
    public static final LightShieldSkill LIGHT_SHIELD = new LightShieldSkill();
    public static final ScatterBombSkill SCATTER_BOMB = new ScatterBombSkill();
    public static final JetEngineSkill JET_ENGINE = new JetEngineSkill();
    public static final RayBarrageSkill RAY_BARRAGE = new RayBarrageSkill();
    public static final MineRayBasicSkill MINE_RAY_BASIC = new MineRayBasicSkill();
    public static final MineRayExpertSkill MINE_RAY_EXPERT = new MineRayExpertSkill();
    public static final MineRayLuckSkill MINE_RAY_LUCK = new MineRayLuckSkill();

    public static final MeltdownerCategory INSTANCE = new MeltdownerCategory();

    private MeltdownerCategory() {
        super(NAME);
        // L'ORDRE EST CELUI DE L'ORIGINAL, et il se voit : le menu F4 liste les competences dans
        // cet ordre-la, niveau par niveau. C'est celui de CatMeltdowner.
        // (electron_missile, absent du port, se rangera apres le rayon chanceux.)
        addSkill(ELECTRON_BOMB);
        addSkill(RADIATION_INTENSIFY);
        addSkill(SCATTER_BOMB);
        addSkill(LIGHT_SHIELD);
        addSkill(MELTDOWNER);
        addSkill(MINE_RAY_BASIC);
        addSkill(RAY_BARRAGE);
        addSkill(JET_ENGINE);
        addSkill(MINE_RAY_EXPERT);
        addSkill(MINE_RAY_LUCK);

        // Les trois cursus generiques ferment la categorie, comme dans l'original.
        cn.academy.ability.generic.GenericSkills.addTo(this);

        // Dependances de l'original dont les deux bouts sont portes. Il ne reste que
        // l'ecran de la bombe a electrons, qui attend une application du terminal.
        // L'original demandait la moitie de l'experience de la bombe a electrons : le passif
        // vient de la meme source que le reste du plasma.
        RADIATION_INTENSIFY.setParent(ELECTRON_BOMB, 0.5f);
        LIGHT_SHIELD.setParent(ELECTRON_BOMB, 1f);
        SCATTER_BOMB.setParent(ELECTRON_BOMB, 0.8f);
        // Le meltdowner demande les deux : la bombe pour la maitrise du plasma, le
        // bouclier pour l'avoir tenu. C'est la deuxieme competence du port a deux
        // parentes, apres la dechirure.
        MELTDOWNER.setParent(SCATTER_BOMB, 0.8f);
        MELTDOWNER.addDependency(LIGHT_SHIELD, 0.8f);
        // Le reacteur demande une experience pleine dans le meltdowner : l'original
        // n'ouvrait ce vol qu'a qui avait epuise le reste du plasma.
        JET_ENGINE.setParent(MELTDOWNER, 1f);
        // La salve demande une demi-experience dans le meltdowner, comme l'original.
        RAY_BARRAGE.setParent(MELTDOWNER, 0.5f);
        // Les rayons miniers se suivent : l'expert demande le premier, le chanceux
        // demande l'expert, comme dans l'original.
        MINE_RAY_BASIC.setParent(MELTDOWNER, 0.3f);
        MINE_RAY_EXPERT.setParent(MINE_RAY_BASIC, 0.8f);
        MINE_RAY_LUCK.setParent(MINE_RAY_EXPERT, 1f);
    }
}
