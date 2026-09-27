package cn.academy.ability.electromaster;

import cn.academy.ability.Category;

/** Electromaster (Railgun-type) ability category. */
public class ElectromasterCategory extends Category {

    public static final String NAME = "electromaster";

    public static final ArcGenSkill ARC_GEN = new ArcGenSkill();
    public static final RailgunSkill RAILGUN = new RailgunSkill();
    public static final BodyIntensifySkill BODY_INTENSIFY = new BodyIntensifySkill();
    public static final ThunderBoltSkill THUNDER_BOLT = new ThunderBoltSkill();
    public static final ThunderClapSkill THUNDER_CLAP = new ThunderClapSkill();
    public static final ChargingSkill CHARGING = new ChargingSkill();
    public static final MagMovementSkill MAG_MOVEMENT = new MagMovementSkill();
    public static final MineDetectSkill MINE_DETECT = new MineDetectSkill();
    public static final MagManipSkill MAG_MANIP = new MagManipSkill();

    public static final ElectromasterCategory INSTANCE = new ElectromasterCategory();

    private ElectromasterCategory() {
        super(NAME);
        // L'ORDRE EST CELUI DE L'ORIGINAL, et il se voit : le menu F4 liste les competences
        // dans cet ordre-la. C'est celui de CatElectromaster, ou `ironSand` est commente par
        // son auteur — donc absent chez lui aussi.
        addSkill(ARC_GEN);
        addSkill(CHARGING);
        addSkill(MAG_MOVEMENT);
        addSkill(MAG_MANIP);
        addSkill(MINE_DETECT);
        addSkill(BODY_INTENSIFY);
        addSkill(THUNDER_BOLT);
        addSkill(RAILGUN);
        addSkill(THUNDER_CLAP);

        // Les trois cursus generiques ferment la categorie, comme dans l'original : ils occupent
        // les niveaux 3, 4 et 5 pour TOUS les pouvoirs, et ne se rangent pas sur une touche.
        cn.academy.ability.generic.GenericSkills.addTo(this);

        // L'arbre de l'electromaster, enfin complet : tant que mag_manip manquait, trois
        // de ses liens ne pouvaient pas etre posees (une dependance vers une competence
        // absente rendrait la competence inapprenable pour toujours). Les seuils sont
        // ceux de l'original, au mot pres.
        BODY_INTENSIFY.setParent(ARC_GEN, 1f);
        // Le corps demande toute l'experience de l'arc, et la meme chose du branchement :
        // il faut avoir appris a charger une machine avant d'intensifier son propre corps.
        BODY_INTENSIFY.addDependency(CHARGING, 1f);
        // Le thunder bolt demandait l'arc, sans seuil, et 70 % du branchement.
        THUNDER_BOLT.setParent(ARC_GEN);
        THUNDER_BOLT.addDependency(CHARGING, 0.7f);
        // L'original demandait 30 % d'experience dans le thunder bolt avant de
        // debloquer le railgun : l'avoir appris ne suffisait pas. Et toute l'experience
        // de la manipulation d'un bloc, parce que le railgun se charge de la meme facon.
        RAILGUN.setParent(THUNDER_BOLT, 0.3f);
        RAILGUN.addDependency(MAG_MANIP, 1f);
        // L'original demandait cette fois l'experience pleine dans le thunder bolt.
        THUNDER_CLAP.setParent(THUNDER_BOLT, 1f);
        // Le tout premier degre d'electromaster : brancher sa reserve sur une machine
        // demandait un peu d'arc, comme dans l'original.
        CHARGING.setParent(ARC_GEN, 0.3f);
        // La traction : l'original la demandait sans seuil d'experience dans l'arc, donc
        // l'arc appris suffisait, plus 70 % du branchement.
        MAG_MOVEMENT.setParent(ARC_GEN);
        MAG_MOVEMENT.addDependency(CHARGING, 0.7f);
        // La manipulation d'un bloc descend de la traction, et l'original demandait la
        // moitie de son experience : on n'arrache pas un bloc avant d'avoir appris a s'y
        // accrocher.
        MAG_MANIP.setParent(MAG_MOVEMENT, 0.5f);
        // Et la detection de minerais ferme la chaine : toute l'experience de la
        // manipulation, puisqu'elle en est la suite directe.
        MINE_DETECT.setParent(MAG_MANIP, 1f);
    }
}
