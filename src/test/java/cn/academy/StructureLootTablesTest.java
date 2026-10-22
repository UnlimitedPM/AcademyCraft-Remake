package cn.academy;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les tables de butin de structure : ou tombe quoi.
 *
 * <p>Ces listes sont des noms de tables vanilla, donc rien ne les verifie a la compilation : une
 * faute de frappe ne se verrait qu'en jeu, dans un coffre qui ne donne jamais rien. Le test fige
 * donc les deux regles demandees par le joueur le 22/10 — les facteurs dans les cinq tables de
 * l'original plus l'End, les bastions et le Nether ; les morceaux dans toutes les structures.
 */
class StructureLootTablesTest {

    private static ResourceLocation chest(String name) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "chests/" + name);
    }

    @Test
    void lesFacteursRestentDansLesCinqTablesDeLoriginal() {
        for (String name : new String[]{"abandoned_mineshaft", "desert_pyramid", "jungle_temple",
                "stronghold_library", "simple_dungeon"}) {
            assertTrue(StructureLootTables.hasFactor(chest(name)), name);
        }
    }

    @Test
    void lesFacteursDescendentAussiDansLEndLesBastionsEtLeNether() {
        // La demande du 22/10, mot pour mot : « les end city et les bastions ainsi que les
        // forteresse du nether ».
        assertTrue(StructureLootTables.hasFactor(chest("end_city_treasure")), "cite de l'End");
        assertTrue(StructureLootTables.hasFactor(chest("bastion_bridge")), "pont du bastion");
        assertTrue(StructureLootTables.hasFactor(chest("bastion_hoglin_stable")), "etable");
        assertTrue(StructureLootTables.hasFactor(chest("bastion_other")), "bastion commun");
        assertTrue(StructureLootTables.hasFactor(chest("bastion_treasure")), "tresor du bastion");
        assertTrue(StructureLootTables.hasFactor(chest("nether_bridge")), "forteresse du Nether");
    }

    @Test
    void lesFacteursNeDescendentPasPartout() {
        // Un facteur ouvre une categorie de pouvoir : il reste une trouvaille. Les coffres de
        // village et les epaves n'en donnent pas.
        assertFalse(StructureLootTables.hasFactor(chest("village/village_plains_house")));
        assertFalse(StructureLootTables.hasFactor(chest("shipwreck_treasure")));
        assertFalse(StructureLootTables.hasFactor(chest("buried_treasure")));
        assertFalse(StructureLootTables.hasFactor(null));
    }

    @Test
    void lesMorceauxTombentDansChaqueStructure() {
        // « pour les musiques, contrairement au ability, fait en sorte que ce soit dans chaque
        // structure » : les trois familles demandees, plus les cinq de l'original, plus les
        // structures de la 1.20.
        for (String name : new String[]{"end_city_treasure", "bastion_treasure", "nether_bridge",
                "abandoned_mineshaft", "desert_pyramid", "jungle_temple", "stronghold_library",
                "simple_dungeon", "ancient_city", "woodland_mansion", "pillager_outpost",
                "underwater_ruin_big", "shipwreck_treasure", "buried_treasure", "igloo_chest",
                "ruined_portal", "stronghold_corridor", "stronghold_crossing",
                "village/village_plains_house", "village/village_weaponsmith", "village/village_temple"}) {
            assertTrue(StructureLootTables.hasMedia(chest(name)), name);
        }
    }

    @Test
    void lesMorceauxNeTombentPasDansLesDeuxTablesQuiNeSontPasDesStructures() {
        // Le coffre de depart du joueur n'appartient a aucune structure, et le distributeur du
        // temple de la jungle n'est pas un coffre.
        assertFalse(StructureLootTables.hasMedia(chest("spawn_bonus_chest")));
        assertFalse(StructureLootTables.hasMedia(chest("jungle_temple_dispenser")));
        assertFalse(StructureLootTables.hasMedia(null));
    }

    @Test
    void lesDeuxListesSontCelllesDeLa1201() {
        // Les noms viennent de `data/minecraft/loot_tables/chests/` du jar vanilla : 43 tables,
        // dont 41 retenues. Un nom invente ici ne casserait rien a la compilation, d'ou ce compte.
        assertEquals(43, StructureLootTables.MEDIA_TABLES.size() + 2,
                "toutes les tables de coffre de la 1.20.1, moins les deux exclues");
        assertEquals(11, StructureLootTables.FACTOR_TABLES.size(),
                "cinq de l'original, plus l'End, les quatre bastions et le Nether");
    }
}
