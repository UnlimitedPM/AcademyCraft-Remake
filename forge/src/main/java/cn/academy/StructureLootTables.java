package cn.academy;

import java.util.List;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;

/**
 * Les tables de butin de structure ou le mod pose quelque chose, et quoi.
 *
 * <p>Portage de {@code ACItemAdditionalRegistry}, dont la liste {@code DEFAULT_APPEARANCE} nommait
 * cinq tables : la mine abandonnee, la pyramide du desert, le temple de la jungle, la bibliotheque
 * du fort et le donjon a generateur. Le port s'y tenait, et le joueur a demande deux choses de plus
 * (22/10) :
 *
 * <ul>
 *   <li>les <b>facteurs d'induction</b> doivent aussi tomber dans les coffres de la <b>cite de
 *       l'End</b>, des <b>bastions</b> et de la <b>forteresse du Nether</b> ;</li>
 *   <li>les <b>morceaux de musique</b>, eux, doivent tomber dans <b>chaque structure</b> — pas
 *       seulement les cinq de l'original.</li>
 * </ul>
 *
 * <p>Deux listes, donc, et une seule regle chacune : un facteur est un objet rare qui ouvre une
 * categorie de pouvoir, il reste dans les coffres qui valent le detour ; un morceau est un
 * collectionnable, il peut se trouver partout.
 *
 * <p>PUR : aucune classe du jeu au-dela de {@code ResourceLocation}, donc relisible en JUnit.
 */
public final class StructureLootTables {

    /**
     * Les coffres ou tombe un facteur d'induction.
     *
     * <p>Les cinq de l'original, plus les trois familles demandees : la cite de l'End, les quatre
     * coffres des bastions (le pont, l'etable a hoglin, le bastion commun et son tresor) et la
     * forteresse du Nether.
     */
    public static final Set<ResourceLocation> FACTOR_TABLES = Set.of(
            chest("abandoned_mineshaft"),
            chest("desert_pyramid"),
            chest("jungle_temple"),
            chest("stronghold_library"),
            chest("simple_dungeon"),
            chest("end_city_treasure"),
            chest("bastion_bridge"),
            chest("bastion_hoglin_stable"),
            chest("bastion_other"),
            chest("bastion_treasure"),
            chest("nether_bridge"));

    /**
     * Les coffres ou tombe un morceau de musique : <b>toutes</b> les structures du jeu.
     *
     * <p>C'est la liste des tables {@code minecraft:chests/*} de la 1.20.1, moins deux qui ne sont
     * pas des coffres de structure : {@code spawn_bonus_chest} (le coffre de depart du joueur, qui
     * n'appartient a aucune structure) et {@code jungle_temple_dispenser} (un distributeur, pas un
     * coffre). Les coffres de village y sont : un village est une structure.
     */
    public static final List<ResourceLocation> MEDIA_TABLES = List.of(
            chest("abandoned_mineshaft"),
            chest("ancient_city"),
            chest("ancient_city_ice_box"),
            chest("bastion_bridge"),
            chest("bastion_hoglin_stable"),
            chest("bastion_other"),
            chest("bastion_treasure"),
            chest("buried_treasure"),
            chest("desert_pyramid"),
            chest("end_city_treasure"),
            chest("igloo_chest"),
            chest("jungle_temple"),
            chest("nether_bridge"),
            chest("pillager_outpost"),
            chest("ruined_portal"),
            chest("shipwreck_map"),
            chest("shipwreck_supply"),
            chest("shipwreck_treasure"),
            chest("simple_dungeon"),
            chest("stronghold_corridor"),
            chest("stronghold_crossing"),
            chest("stronghold_library"),
            chest("underwater_ruin_big"),
            chest("underwater_ruin_small"),
            chest("village/village_armorer"),
            chest("village/village_butcher"),
            chest("village/village_cartographer"),
            chest("village/village_desert_house"),
            chest("village/village_fisher"),
            chest("village/village_fletcher"),
            chest("village/village_mason"),
            chest("village/village_plains_house"),
            chest("village/village_savanna_house"),
            chest("village/village_shepherd"),
            chest("village/village_snowy_house"),
            chest("village/village_taiga_house"),
            chest("village/village_tannery"),
            chest("village/village_temple"),
            chest("village/village_toolsmith"),
            chest("village/village_weaponsmith"),
            chest("woodland_mansion"));

    private StructureLootTables() {}

    /** Un facteur tombe-t-il dans cette table ? */
    public static boolean hasFactor(ResourceLocation table) {
        return table != null && FACTOR_TABLES.contains(table);
    }

    /** Un morceau tombe-t-il dans cette table ? */
    public static boolean hasMedia(ResourceLocation table) {
        return table != null && MEDIA_TABLES.contains(table);
    }

    private static ResourceLocation chest(String name) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "chests/" + name);
    }
}
