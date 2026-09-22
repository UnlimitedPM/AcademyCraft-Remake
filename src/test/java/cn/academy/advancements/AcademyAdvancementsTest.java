package cn.academy.advancements;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les succes du mod : tout ce qui se decide sans joueur.
 *
 * <p>Un succes est un fichier de donnees, et un fichier de donnees ne casse rien quand il
 * est faux : il ne fait simplement rien. Un declencheur mal orthographie ne leve aucune
 * erreur au chargement — le succes reste impossible a obtenir, et personne ne s'en apercoit
 * avant d'avoir joue une partie entiere.
 *
 * <p>Ces tests tiennent donc les deux bouts de la chaine, ceux qui se verifient sans lancer
 * le jeu : les noms que le code utilise, et les fichiers qui les citent. Le reste — la
 * remise du succes a un vrai joueur — se voit en partie, et Forge refuse de la simuler.
 */
class AcademyAdvancementsTest {

    @Test
    void lesQuinzeNomsSontUniquesEtBienFormes() {
        assertEquals(15, AcademyAdvancements.NAMES.length, "l'original a quinze declencheurs");

        Set<String> seen = new HashSet<>();
        for (String name : AcademyAdvancements.NAMES) {
            assertTrue(seen.add(name), "deux succes ne peuvent pas porter le meme nom : " + name);
            assertTrue(name.matches("[a-z0-9_]+"),
                    "un nom de succes s'ecrit en minuscules et sans accent : " + name);
            assertEquals("academy", AcademyAdvancements.id(name).getNamespace(),
                    "un succes du mod vit dans son propre namespace");
            assertEquals(name, AcademyAdvancements.id(name).getPath(),
                    "l'identifiant reprend le nom tel quel");
        }
    }

    @Test
    void seulsTroisPaliersDonnentUnSucces() {
        // Le premier niveau, puis les troisieme et cinquieme : c'est ce que l'original
        // recompensait, et rien d'autre. Les niveaux 2 et 4 se traversent en silence.
        assertEquals(java.util.List.of(AcademyAdvancements.DEV_CATEGORY),
                AcademyAdvancements.levelNames(1));
        assertEquals(java.util.List.of(AcademyAdvancements.AC_LEVEL_3),
                AcademyAdvancements.levelNames(3));
        assertEquals(java.util.List.of(AcademyAdvancements.AC_LEVEL_5),
                AcademyAdvancements.levelNames(5));

        for (int level : new int[] {0, 2, 4, 6, 99}) {
            assertTrue(AcademyAdvancements.levelNames(level).isEmpty(),
                    "le niveau " + level + " ne donne aucun succes");
        }
    }

    @Test
    void laTableDeLaFabriqueEstCelleDeLoriginal() {
        assertEquals(4, AcademyAdvancementDispatcher.CRAFTED.size(),
                "l'original recompensait quatre fabrications");
        assertEquals(AcademyAdvancements.PHASE_GENERATOR,
                AcademyAdvancementDispatcher.CRAFTED.get("phase_generator"));
        assertEquals(AcademyAdvancements.AC_NODE,
                AcademyAdvancementDispatcher.CRAFTED.get("node_basic"));
        assertEquals(AcademyAdvancements.AC_MATRIX,
                AcademyAdvancementDispatcher.CRAFTED.get("matrix"));
        assertEquals(AcademyAdvancements.AC_DEVELOPER,
                AcademyAdvancementDispatcher.CRAFTED.get("developer_portable"));
    }

    @Test
    void laTableDuRamassageDonneUnSeulSuccesParObjet() {
        // Quatre facteurs, un seul succes : l'original n'avait qu'un objet, le port en a
        // quatre, mais ramasser l'un ou l'autre mene au meme succes.
        Set<String> names = new HashSet<>(AcademyAdvancementDispatcher.PICKED_UP.values());
        assertEquals(2, names.size(), "ramasser un facteur, ou de la phase : deux succes");
        assertTrue(names.contains(AcademyAdvancements.GETTING_FACTOR));
        assertTrue(names.contains(AcademyAdvancements.GETTING_PHASE));

        for (String item : new String[] {"factor_electromaster", "factor_meltdowner",
                "factor_teleporter", "factor_vecmanip"}) {
            assertEquals(AcademyAdvancements.GETTING_FACTOR,
                    AcademyAdvancementDispatcher.PICKED_UP.get(item),
                    "les quatre facteurs menent au meme succes : " + item);
        }
        assertEquals(AcademyAdvancements.GETTING_PHASE,
                AcademyAdvancementDispatcher.PICKED_UP.get("matter_unit_phase_liquid"));
    }

    @Test
    void lesTablesNeCitentQueDesSuccesQuiExistent() {
        Set<String> known = Set.of(AcademyAdvancements.NAMES);
        for (var entry : AcademyAdvancementDispatcher.CRAFTED.entrySet()) {
            assertTrue(known.contains(entry.getValue()),
                    "la fabrique cite un succes inconnu : " + entry.getValue());
        }
        for (var entry : AcademyAdvancementDispatcher.PICKED_UP.entrySet()) {
            assertTrue(known.contains(entry.getValue()),
                    "le ramassage cite un succes inconnu : " + entry.getValue());
        }
    }

    @Test
    void chaqueSuccesAnnonceSonPropreDeclencheurDansSonFichier() {
        // C'est le vrai contrat de tout ce module : le code enregistre quinze noms, et
        // quinze fichiers les citent. Une faute d'orthographe d'un cote ou de l'autre ne
        // se verrait qu'apres une partie entiere.
        for (String name : AcademyAdvancements.NAMES) {
            String json = read(name + ".json");
            assertNotNull(json, "le fichier du succes doit etre dans les ressources : " + name);
            assertTrue(json.contains("\"trigger\": \"" + "academy:" + name + "\""),
                    "le succes " + name + " doit citer son propre declencheur");
        }

        String root = read("root.json");
        assertNotNull(root, "la racine doit etre dans les ressources");
        assertTrue(root.contains("\"background\""),
                "la racine porte le fond de l'onglet des succes");
        assertFalse(root.contains("\"parent\""), "la racine n'a pas de parent");
    }

    @Test
    void chaqueFichierCiteUnParentQuiExiste() {
        for (String name : AcademyAdvancements.NAMES) {
            String json = read(name + ".json");
            assertNotNull(json, "le fichier doit exister : " + name);

            String parent = value(json, "parent");
            assertNotNull(parent, "un succes autre que la racine a un parent : " + name);
            assertTrue(parent.startsWith("academy:"),
                    "le parent d'un succes du mod vit dans son namespace : " + parent);

            String path = parent.substring("academy:".length());
            assertTrue(path.equals("root") || Set.of(AcademyAdvancements.NAMES).contains(path),
                    "le parent de " + name + " doit exister : " + path);
        }
    }

    /** La valeur d'une cle de premier niveau dans un fichier de succes. */
    private static String value(String json, String key) {
        int at = json.indexOf('"' + key + '"');
        if (at < 0) return null;
        int open = json.indexOf('"', json.indexOf(':', at) + 1);
        if (open < 0) return null;
        int close = json.indexOf('"', open + 1);
        return close < 0 ? null : json.substring(open + 1, close);
    }

    /** Lit un fichier de succes depuis les ressources du mod. */
    private static String read(String file) {
        try (InputStream in = AcademyAdvancementsTest.class.getClassLoader()
                .getResourceAsStream("data/academy/advancements/" + file)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
