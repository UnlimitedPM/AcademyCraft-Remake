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
 *
 * <p>Et ce sont celles de l'original, <b>mot pour mot</b> : le port les avait traduites, avec des
 * colonnes calculees, et le joueur n'y a plus reconnu son menu F4. Les noms internes des
 * competences ({@code arc_gen}) en font partie — c'est un ecran de developpeur.
 */
class DebugConsoleLinesTest {

    @Test
    void sansCategorieLecranLeDit() {
        List<String> lines = DebugConsoleLines.info(new AbilityData(), null);

        assertEquals(2, lines.size());
        assertEquals("AcademyCraft developer info", lines.get(0));
        assertEquals("Ability not acquired", lines.get(1));
    }

    @Test
    void lesInformationsPortentLesNomsDeLoriginal() {
        // Les libelles sont ceux de l'original, espaces compris : "CP:" puis SEPT espaces, le
        // reste de la ligne etant le nombre. Une traduction ou une colonne calculee changeait
        // l'aspect de l'ecran, et c'est ce que le joueur a vu.
        Category category = new Category("electromaster");
        List<String> lines = DebugConsoleLines.info(new AbilityData(), category);

        assertEquals(10, lines.size());
        assertEquals("AcademyCraft developer info", lines.get(0));
        assertEquals("electromaster", lines.get(1), "le nom INTERNE de la categorie");
        assertEquals("Level 0", lines.get(2), "niveau de depart");
        assertTrue(lines.get(3).startsWith("CP:       "),
                "le libelle et ses sept espaces : [" + lines.get(3) + "]");
        assertTrue(lines.get(4).startsWith("Overload: "),
                "la surcharge, elle, n'a qu'un espace : [" + lines.get(4) + "]");
        assertEquals("CPData.canUseAbility: false", lines.get(5));
        assertEquals("CPData.activated: false", lines.get(6),
                "l'original montrait l'etat allume/eteint, et non la surcharge");
        assertTrue(lines.get(7).startsWith("CPData.addMaxCP: "), lines.get(7));
        assertEquals("CPData.interfering: false", lines.get(8));
        assertTrue(lines.get(9).startsWith(" AData.levelProgress: "),
                "la ligne de progression commence par une espace : [" + lines.get(9) + "]");
        assertTrue(lines.get(9).endsWith("%"), lines.get(9));
    }

    @Test
    void lesDeuxParenthusesNOntQuUneDecimale() {
        // Les lignes CP et Overload montrent une seule decimale, et le nombre brut d'une ligne
        // `CPData.addMax*` aucun arrondi. Le separateur suit la locale du systeme, comme chez
        // l'original : on compare donc les chiffres, pas la virgule.
        List<String> lines = DebugConsoleLines.info(new AbilityData(),
                ElectromasterCategory.INSTANCE);
        String cp = lines.get(3).replace(',', '.');
        String overload = lines.get(4).replace(',', '.');

        assertTrue(cp.matches("CP: +\\d+/\\d+\\(\\d+\\.\\d\\+\\d+\\.\\d\\)"), cp);
        assertTrue(overload.matches("Overload: +\\d+/\\d+\\(\\d+\\.\\d\\+\\d+\\.\\d\\)"),
                overload);
    }

    @Test
    void lEtatDesCompetencesAligneLesNoms() {
        // Une ligne par competence de la categorie, le nom garni jusqu'a 30 caracteres puis
        // l'experience, ou l'aveu qu'elle n'est pas apprise. La categorie se prend par son
        // instance : son constructeur est prive, et le registre de JUnit est vide.
        Category category = ElectromasterCategory.INSTANCE;
        List<String> lines = DebugConsoleLines.skills(new AbilityData(), List.of(category));

        assertEquals("AcademyCraft developer info", lines.get(0));
        assertEquals("Skill status", lines.get(1));
        assertEquals(category.getSkills().size() + 2, lines.size(),
                "deux lignes de titre, puis une par competence");
        assertFalse(category.getSkills().isEmpty(), "le lecteur a bien des competences");

        for (int i = 2; i < lines.size(); i++) {
            String row = lines.get(i);
            String name = category.getSkills().get(i - 2).getName();
            assertTrue(row.startsWith(name), row);
            assertTrue(row.length() >= DebugConsoleLines.SKILL_COLUMN + 1, row);
            assertTrue(row.substring(DebugConsoleLines.SKILL_COLUMN)
                    .equals("[not learned]")
                    || row.substring(DebugConsoleLines.SKILL_COLUMN).endsWith("%"), row);
        }

        // Le NOM INTERNE, pas celui de l'arbre : c'est ce qui en fait un ecran de developpeur.
        assertEquals("arc_gen", category.getSkills().get(0).getName());
        assertTrue(lines.get(2).startsWith("arc_gen "), lines.get(2));
    }

    @Test
    void plusieursCategoriesAnnoncentLaLeur() {
        // L'original n'avait qu'une categorie, donc une seule liste sans titre. Le port en
        // autorise plusieurs : sans titre, leurs competences se melangeraient et certaines
        // sembleraient manquer.
        Category electromaster = ElectromasterCategory.INSTANCE;
        Category autre = new Category("vecmanip");

        List<String> seule = DebugConsoleLines.skills(new AbilityData(), List.of(electromaster));
        assertEquals("Skill status", seule.get(1), "une seule categorie : pas de titre");
        assertEquals(electromaster.getSkills().size() + 2, seule.size());

        List<String> deux = DebugConsoleLines.skills(new AbilityData(),
                List.of(electromaster, autre));
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
