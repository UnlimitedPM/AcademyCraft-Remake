package cn.academy.ability.teleporter;

import cn.academy.ability.Category;

/** Teleporter ability category. */
public class TeleporterCategory extends Category {

    public static final String NAME = "teleporter";

    public static final ShiftTeleportSkill SHIFT_TELEPORT = new ShiftTeleportSkill();
    public static final SpaceFluctuationSkill SPACE_FLUCTUATION = new SpaceFluctuationSkill();
    public static final PenetrateTeleportSkill PENETRATE_TELEPORT = new PenetrateTeleportSkill();
    public static final DimFoldingTheoremSkill DIM_FOLDING_THEOREM = new DimFoldingTheoremSkill();
    public static final ThreateningTeleportSkill THREATENING_TELEPORT = new ThreateningTeleportSkill();
    public static final MarkTeleportSkill MARK_TELEPORT = new MarkTeleportSkill();
    public static final FleshRippingSkill FLESH_RIPPING = new FleshRippingSkill();
    public static final FlashingSkill FLASHING = new FlashingSkill();
    public static final LocationTeleportSkill LOCATION_TELEPORT = new LocationTeleportSkill();

    public static final TeleporterCategory INSTANCE = new TeleporterCategory();

    private TeleporterCategory() {
        super(NAME);
        // La teinte de la categorie, celle que l'original posait sur CatTeleporter : un gris a
        // 145 d'opacite, le voile le plus dense des quatre.
        setColorStyle(164, 164, 164, 145);
        // L'ORDRE EST CELUI DE L'ORIGINAL, et il se voit : le menu F4 liste les competences dans
        // cet ordre-la, niveau par niveau. C'est celui de CatTeleporter ; le port les rangeait
        // dans l'ordre ou elles avaient ete codees, ce qui melangeait les niveaux.
        // LA FIGURE DE L'ARBRE : les coordonnees de CatTeleporter de l'original, au chiffre pres.
        // La menace est la racine ; les marques et les deplacements s'etagent vers la droite.
        THREATENING_TELEPORT.setPosition(14, 42);
        DIM_FOLDING_THEOREM.setPosition(50, 75);
        PENETRATE_TELEPORT.setPosition(60, 46);
        MARK_TELEPORT.setPosition(70, 16);
        FLESH_RIPPING.setPosition(130, 12);
        LOCATION_TELEPORT.setPosition(118, 50);
        SHIFT_TELEPORT.setPosition(175, 47);
        SPACE_FLUCTUATION.setPosition(160, 80);
        FLASHING.setPosition(220, 20);

        addSkill(THREATENING_TELEPORT);
        addSkill(DIM_FOLDING_THEOREM);
        addSkill(PENETRATE_TELEPORT);
        addSkill(MARK_TELEPORT);
        addSkill(FLESH_RIPPING);
        addSkill(LOCATION_TELEPORT);
        addSkill(SHIFT_TELEPORT);
        addSkill(SPACE_FLUCTUATION);
        addSkill(FLASHING);

        // Les trois cursus generiques ferment la categorie, comme dans l'original.
        cn.academy.ability.generic.GenericSkills.addTo(this);

        // L'arbre de l'original part du lancer d'objet : c'est lui qui apprend a
        // viser, et tout le reste en descend. Les seuils sont ceux de l'original, au mot
        // pres, et chaque lien est pose.
        DIM_FOLDING_THEOREM.setParent(THREATENING_TELEPORT, 0.2f);
        PENETRATE_TELEPORT.setParent(THREATENING_TELEPORT, 0.5f);
        MARK_TELEPORT.setParent(THREATENING_TELEPORT, 0.4f);
        // L'original demandait les deux : savoir marquer, et savoir traverser.
        FLESH_RIPPING.setParent(MARK_TELEPORT, 0.5f);
        FLESH_RIPPING.addDependency(PENETRATE_TELEPORT, 0.5f);
        // Le scintillement descend du saut court, avec la meme exigence que l'original :
        // presque toute l'experience du saut, donc l'avoir beaucoup pratique.
        FLASHING.setParent(SHIFT_TELEPORT, 0.8f);
        // Et la teleportation a la marque, comme dans l'original, demande les deux : savoir
        // traverser un mur, et savoir marquer un endroit. C'est la troisieme competence du
        // port a deux parentes.
        LOCATION_TELEPORT.setParent(PENETRATE_TELEPORT, 0.8f);
        LOCATION_TELEPORT.addDependency(MARK_TELEPORT, 0.8f);

        // ET SHIFT PART DE LOCATION : c'est l'original qui le dit — `shiftTP.setParent(locTP, 0.5f)`
        // dans CatTeleporter — et le port l'avait OUBLIE. La faute ne se voyait pas dans le code :
        // shift_teleport se retrouvait racine de l'arbre, donc dessine sans trait vers son parent, et
        // le joueur l'a vu avant le test : « il n'y a pas de trait qui relie le location teleport au
        // shift teleport ». Un test fige maintenant la parente de chaque competence.
        SHIFT_TELEPORT.setParent(LOCATION_TELEPORT, 0.5f);
        // La fluctuation d'espace descend du saut, et l'original la liait SANS seuil : l'avoir
        // appris suffisait. Elle ne fait rien toute seule — elle augmente les coups critiques de
        // toutes les autres teleportations (voir TeleportCrits).
        SPACE_FLUCTUATION.setParent(SHIFT_TELEPORT);
    }
}
