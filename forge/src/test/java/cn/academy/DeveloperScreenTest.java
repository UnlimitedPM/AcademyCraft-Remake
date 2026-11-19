package cn.academy;

import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.condition.ConditionAnySkillOfLevel;
import cn.academy.ability.develop.condition.ConditionDependency;
import cn.academy.ability.develop.condition.ConditionDeveloperType;
import cn.academy.ability.develop.condition.ConditionLevel;
import cn.academy.client.gui.cgui.SkillTreeView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L'opacite d'un trait de l'arbre des competences, relue sans Minecraft.
 *
 * <p>L'arbre se lit d'un coup d'oeil a la lumiere de ses traits : ce qui est appris brille, ce qui
 * reste a prendre se devine. C'est une regle de l'original, ecrite la-bas en une ligne —
 * {@code mAlpha * (if (learned) 1.0 else 0.4)} — et le port l'avait perdue. Le joueur a pose les
 * deux versions cote a cote : « la luminosite des elements n'est pas la meme pour les 2 ».
 */
class DeveloperScreenTest {

    /** L'opacite d'un noeud, de zero a un : pleine si appris, sept dixiemes, ou un quart. */
    private static final double LEARNED = 1.0;
    private static final double IN_REACH = 0.7;
    private static final double OUT_OF_REACH = 0.25;

    @Test
    @DisplayName("le trait d'une competence apprise garde toute son opacite")
    void leTraitApprisRestePlein() {
        assertEquals(LEARNED, SkillTreeView.linkDim(LEARNED, true), 1.0e-9);
    }

    @Test
    @DisplayName("le trait d'une competence a prendre est attenue a quatre dixiemes")
    void leTraitNonApprisEstAttenue() {
        assertEquals(0.28, SkillTreeView.linkDim(IN_REACH, false), 1.0e-9,
                "sept dixiemes de noeud, et les quatre dixiemes du trait");
        assertEquals(0.10, SkillTreeView.linkDim(OUT_OF_REACH, false), 1.0e-9,
                "un quart de noeud, et les quatre dixiemes du trait");
        assertEquals(SkillTreeView.LINK_UNLEARNED, SkillTreeView.linkDim(1.0, false), 1.0e-9);
    }

    /**
     * L'ordre du fondu et de la teinte, qui n'est pas interchangeable.
     *
     * <p>L'original ecrit {@code mAlpha * clampd(0, 1, dt * 10.0)} : le fondu monte jusqu'a un, puis
     * la teinte du noeud le reduit — et une fois le fondu fini, il ne reste QUE la teinte. Le port
     * multipliait d'abord et bornait ensuite, donc tout saturait a un des que le produit depassait
     * ce chiffre : au bout de deux dixiemes de seconde, une competence lointaine (0,25) brillait
     * autant qu'une competence apprise. Le joueur l'a vu deux fois — « toutes les competences sont
     * reliees avec la ligne blanche » et les noeuds hors de portee « pas assez sombres ».
     */
    @Test
    @DisplayName("le fondu d'un noeud finit sur sa teinte, jamais sur un")
    void leFonduFinitSurLaTeinte() {
        // Une image plus tard : la teinte pleine, rien de plus.
        assertEquals(LEARNED, SkillTreeView.appearAlpha(LEARNED, 10.0), 1.0e-9);
        assertEquals(IN_REACH, SkillTreeView.appearAlpha(IN_REACH, 10.0), 1.0e-9,
                "une competence a portee reste a sept dixiemes, meme une fois son fondu fini");
        assertEquals(OUT_OF_REACH, SkillTreeView.appearAlpha(OUT_OF_REACH, 10.0), 1.0e-9,
                "et une competence hors de portee reste a un quart — c'est elle qu'on voit trop clair");

        // Au debut : rien. Le fondu lui-meme est lineaire, a dix par seconde.
        assertEquals(0.0, SkillTreeView.appearAlpha(LEARNED, 0.0), 1.0e-9);
        assertEquals(0.5 * IN_REACH, SkillTreeView.appearAlpha(IN_REACH, 0.05), 1.0e-9,
                "a mi-fondu, la moitie de la teinte");
        assertEquals(0.0, SkillTreeView.appearAlpha(OUT_OF_REACH, -1.0), 1.0e-9,
                "un temps negatif ne descend pas sous zero");
    }

    /**
     * Les conditions de la fiche : les icones, et celles qui ne se montrent pas.
     *
     * <p>C'est le contrat du {@code IDevCondition} de l'original, qu'un ecran lit pour dessiner sa
     * rangee : une icone, un texte de survol, et un drapeau « afficher ». Le port n'avait que la
     * phrase — le joueur a vu des lignes rouges la ou le vrai mod pose des icones grisees.
     */
    @Test
    @DisplayName("chaque condition a l'icone de l'original, et le niveau ne se montre pas")
    void lesConditionsOntLeurIcone() {
        assertFalse(ConditionLevel.INSTANCE.shouldDisplay(),
                "le niveau du joueur se lit deja sur le panneau de gauche");
        assertNull(ConditionLevel.INSTANCE.getIcon());

        DeveloperType advanced = DeveloperType.ADVANCED;
        ConditionDeveloperType machine = new ConditionDeveloperType(advanced);
        assertNotNull(machine.getIcon(), "la machine exigee a l'image de la machine");
        // Hors jeu, une cle de langue n'est pas resolue : c'est elle qu'on lit ici, et c'est ce
        // qui garantit que le survol dit bien le nom de la MACHINE EXIGEE et pas une phrase fixe.
        assertTrue(machine.getHint().getString().contains(advanced.nameKey()),
                "le survol nomme la machine exigee");
        assertTrue(machine.needsDeveloper(), "elle seule depend de la machine");

        // La dependance : l'icone de la competence parente, et son seuil dans le survol.
        Skill parent = new Skill("parent", 1) {};
        Category category = new Category("test");
        category.addSkill(parent);
        ConditionDependency dependency = new ConditionDependency(parent, 0.3f);
        assertEquals(parent.getHintIcon(), dependency.getIcon());
        assertNotNull(dependency.getHint(), "le survol dit la dependance et son seuil");
        assertFalse(dependency.needsDeveloper(), "l'application peut la poser");

        // Le cursus : le disque chiffre du niveau demande.
        ConditionAnySkillOfLevel any = new ConditionAnySkillOfLevel(3);
        assertEquals("academy:textures/abilities/condition/any3.png", any.getIcon().toString());
        assertNotNull(any.getHint());
    }

    @Test
    @DisplayName("la Developer ne fait pas respirer ses fonds de page")
    void laDeveloperNeRespirePas() {
        // Les ecrans de machine font osciller l'opacite de leurs couches ui_ entre 172 et 216, comme
        // l'original ; celui du developpement, non — la 1.12.2 y dessine ses blancs a 255, et le port
        // les sortait a 182. Un ecran ne se construit pas sans Minecraft : c'est donc la surcharge
        // elle-meme qu'on verifie ici, et elle est la seule chose a ne pas perdre.
        assertDoesNotThrow(() -> DeveloperScreen.class.getDeclaredMethod("breathes"),
                "DeveloperScreen doit surcharger breathes() pour eteindre le souffle");
    }
}
