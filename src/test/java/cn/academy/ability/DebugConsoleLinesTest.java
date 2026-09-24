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
        assertTrue(lines.get(3).contains("(") && lines.get(3).endsWith(")"),
                "les CP montrent leur plafond separe, comme la surcharge : " + lines.get(3));
        assertTrue(lines.get(4).startsWith("Overload: "), lines.get(4));
        assertTrue(lines.get(4).contains("(") && lines.get(4).endsWith(")"),
                "la surcharge montre son maximum separe : " + lines.get(4));
        assertEquals("CPData.canUseAbility: false", lines.get(5),
                "sans categorie l'aptitude ne peut pas etre allumee, donc rien ne part");
        assertEquals("CPData.activated: false", lines.get(6),
                "l'original ne montrait pas la surcharge ici, mais l'etat allume/eteint");
        assertTrue(lines.get(7).startsWith("CPData.addMaxCP: "), lines.get(7));
        assertEquals("CPData.interfering: false", lines.get(8));
        assertTrue(lines.get(9).startsWith(" AData.levelProgress: "),
                "l'espace de tete est celui de l'original : " + lines.get(9));
        assertTrue(lines.get(9).endsWith("%"), lines.get(9));
        assertEquals(10, lines.size(),
                "l'original ne liste pas le maximum ajoute de surcharge, seulement celui des CP");
    }

    @Test
    void lesAjoutsSontEcritsAvecCinqDecimalesAuPlus() {
        // Le maximum ajoute grandit par petites touches, jusqu'a sept decimales : a ce compte
        // la ligne ne dit plus rien. Cinq suffisent, et les zeros inutiles s'en vont.
        assertEquals("0", DebugConsoleLines.decimal(0.0f));
        assertEquals("0.71624", DebugConsoleLines.decimal(0.7162399f));
        assertEquals("33.19888", DebugConsoleLines.decimal(33.198875f));
        assertEquals("7", DebugConsoleLines.decimal(7.0f));
        assertEquals("70.7", DebugConsoleLines.decimal(70.699999f));
    }

    @Test
    void lesDeuxParenthesesNOntQuUneDecimale() {
        // Les lignes CP et Overload, elles, montrent une seule decimale, comme l'original :
        // c'est le detail du texte qu'on lit en jouant, pas la donnee brute des lignes
        // CPData.addMax* qui suivent.
        List<String> lines = DebugConsoleLines.info(new AbilityData(),
                ElectromasterCategory.INSTANCE);
        String cp = lines.get(3);
        String overload = lines.get(4);

        assertTrue(cp.matches("CP:       \\d+/\\d+\\(\\d+\\.\\d\\+\\d+\\.\\d\\)"), cp);
        assertTrue(overload.matches("Overload: \\d+/\\d+\\(\\d+\\.\\d\\+\\d+\\.\\d\\)"),
                overload);
    }

    @Test
    void lEtatDesCompetencesAligneLesNoms() {
        // Une ligne par competence de la categorie, le nom garni jusqu'a 30 caracteres puis
        // l'experience, ou l'aveu qu'elle n'est pas apprise. La categorie se prend par son
        // instance : son constructeur est prive, et le registre de JUnit est vide.
        Category category = ElectromasterCategory.INSTANCE;
        List<String> lines = DebugConsoleLines.skills(new AbilityData(), List.of(category));

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
    void plusieursCategoriesAnnoncentLaLeur() {
        // L'original n'avait qu'une categorie, donc une seule liste sans titre. Le port en
        // autorise plusieurs : sans titre, leurs competences se melangeraient et certaines
        // sembleraient manquer.
        Category electromaster = ElectromasterCategory.INSTANCE;
        Category autre = new Category("vecmanip");

        List<String> seule = DebugConsoleLines.skills(new AbilityData(), List.of(electromaster));
        assertEquals(DebugConsoleLines.SKILL_STATUS, seule.get(1),
                "une seule categorie : pas de titre, comme avant");
        assertEquals(electromaster.getSkills().size() + 2, seule.size());

        List<String> deux = DebugConsoleLines.skills(new AbilityData(),
                List.of(electromaster, autre));
        // Deux lignes de titre, puis le nom de chaque categorie suivi de ses competences.
        assertEquals(2 + (1 + electromaster.getSkills().size()) + 1, deux.size(),
                "la seconde categorie est vide dans ce test, mais elle s'annonce");
        assertEquals("electromaster", deux.get(2));
        assertEquals("vecmanip", deux.get(deux.size() - 1));
    }

    @Test
    void unJoueurSansRienPeutSeServirDeSesCompetences() {
        // La regle que le paquet applique a l'appui : aptitude eteinte, surcharge pleine ou
        // brouillage, et rien ne part. L'etat sature ne se fabrique pas a la main —
        // `getCategoryLevels()` rend une vue non modifiable et le plafond de surcharge vient du
        // niveau — et l'allumage demande une categorie, donc ce test ne couvre que le cas de
        // depart, celui que l'ecran montre a l'ouverture.
        AbilityData data = new AbilityData();
        assertFalse(data.isActivated(), "l'aptitude est eteinte au depart, comme chez l'original");
        assertFalse(DebugConsoleLines.canUseAbility(data), "donc aucune competence ne part");

        data.setActivated(true);
        assertFalse(data.isActivated(),
                "allumer le drapeau ne suffit pas : il faut aussi une categorie");
        assertTrue(data.isActivatedRaw(), "le drapeau brut, lui, a bien change");

        assertEquals("CPData.canUseAbility: false",
                DebugConsoleLines.info(data, ElectromasterCategory.INSTANCE).get(5));
    }
}
