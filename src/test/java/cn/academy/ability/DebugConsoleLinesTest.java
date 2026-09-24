package cn.academy.ability;

import cn.academy.ability.electromaster.ElectromasterCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le menu F4 : ses lignes, et ce qu'elles disent.
 *
 * <p>C'est la seule fenetre ou les points de controle et la surcharge se lisent en jeu ; il vaut
 * donc mieux que ses lignes soient justes Avant d'etre jolies.
 */
class DebugConsoleLinesTest {

    @Test
    void sansCategorieLecranLeDit() {
        List<String> lines = DebugConsoleLines.info(new AbilityData(), null);

        assertEquals(2, lines.size());
        assertEquals(DebugConsoleLines.TITLE, lines.get(0));
        assertEquals(DebugConsoleLines.NO_CATEGORY, lines.get(1));
    }

    @Test
    void lesInformationsPortentLesNomsDeLoriginal() {
        // Les libelles sont ceux de l'original, mot pour mot : c'est un ecran de developpeur, il
        // se lit dans ses captures a lui et pas dans une traduction.
        Category category = new Category("electromaster");
        List<String> lines = DebugConsoleLines.info(new AbilityData(), category);

        assertEquals(DebugConsoleLines.TITLE, lines.get(0));
        assertEquals("electromaster", lines.get(1), "le nom interne de la categorie");
        assertEquals("Level 0", lines.get(2), "niveau de depart");
        assertTrue(lines.get(3).startsWith("CP:       "),
                "les deux points et six espaces de l'original : " + lines.get(3));
        assertTrue(lines.get(4).startsWith("Overload: "), lines.get(4));
        assertTrue(lines.get(4).contains("(") && lines.get(4).endsWith(")"),
                "la surcharge montre son maximum separe : " + lines.get(4));
        assertEquals("CPData.canUseAbility: true", lines.get(5),
                "sans surcharge ni brouillage, le joueur peut s'en servir");
        assertEquals("CPData.overloaded: false", lines.get(6));
        assertTrue(lines.get(7).startsWith("CPData.addMaxOverload: "), lines.get(7));
        assertEquals("CPData.interfering: false", lines.get(8));
        assertTrue(lines.get(9).startsWith(" AData.levelProgress: "),
                "l'espace de tete est celui de l'original : " + lines.get(9));
        assertTrue(lines.get(9).endsWith("%"), lines.get(9));
    }

    @Test
    void lEtatDesCompetencesAligneLesNoms() {
        // Une ligne par competence de la categorie, le nom garni jusqu'a 30 caracteres puis
        // l'experience, ou l'aveu qu'elle n'est pas apprise. La categorie se prend par son
        // instance : son constructeur est prive, et le registre de JUnit est vide.
        Category category = ElectromasterCategory.INSTANCE;
        List<String> lines = DebugConsoleLines.skills(new AbilityData(), category);

        assertEquals(DebugConsoleLines.TITLE, lines.get(0));
        assertEquals(DebugConsoleLines.SKILL_STATUS, lines.get(1));
        assertEquals(category.getSkills().size() + 2, lines.size(),
                "deux lignes de titre, puis une par competence");
        assertFalse(category.getSkills().isEmpty(), "le lecteur a bien des competences");

        for (int i = 2; i < lines.size(); i++) {
            String row = lines.get(i);
            String name = category.getSkills().get(i - 2).getName();
            assertTrue(row.startsWith(name), row);
            assertTrue(row.length() >= DebugConsoleLines.SKILL_COLUMN + 1, row);
            assertTrue(row.substring(DebugConsoleLines.SKILL_COLUMN)
                    .equals(DebugConsoleLines.NOT_LEARNED)
                    || row.substring(DebugConsoleLines.SKILL_COLUMN).endsWith("%"), row);
        }
    }

    @Test
    void unJoueurSansRienPeutSeServirDeSesCompetences() {
        // La regle que le paquet applique a l'appui : surcharge pleine ou brouillage, et rien
        // ne part. L'etat sature ne se fabrique pas a la main — `getCategoryLevels()` rend une
        // vue non modifiable et le plafond de surcharge vient du niveau — donc ce test ne
        // couvre que le cas de depart, celui que l'ecran montre a l'ouverture.
        assertTrue(DebugConsoleLines.canUseAbility(new AbilityData()));
        assertEquals("CPData.canUseAbility: true",
                DebugConsoleLines.info(new AbilityData(), ElectromasterCategory.INSTANCE).get(5));
    }
}
