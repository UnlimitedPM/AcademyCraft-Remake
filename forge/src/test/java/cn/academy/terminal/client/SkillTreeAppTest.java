package cn.academy.terminal.client;

import cn.academy.client.gui.cgui.CGuiDocument;
import cn.academy.client.gui.cgui.CWidget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * La page de l'application Arbre de competences, relue dans le vrai document.
 *
 * <h2>Pourquoi cet ecran n'a pas de page a lui</h2>
 *
 * <p>L'original ne lui en donnait pas : {@code SkillTreeAppUI} lisait la page de la machine du
 * developpeur ({@code rework/page_developer}) et l'affichait sans machine. Le port fait pareil, et
 * c'est ce qui rend cet ecran juste — les deux versions montrent le meme arbre, au meme endroit, avec
 * les memes chiffres, parce que c'est le meme fichier.
 *
 * <p>Ce test est la memoire de ce choix. Il ne juge pas la beaute de la page : il exige qu'elle
 * porte tout ce que l'ecran touche. Un widget renomme dans le document ne casserait rien au
 * chargement — les deux ecrans se contenteraient de ne plus rien y mettre, sans un mot.
 */
class SkillTreeAppTest {

    /** La page de l'original, en unites de conception : la taille de l'ecran du developpeur. */
    private static final double PAGE_WIDTH = 400.0;
    private static final double PAGE_HEIGHT = 187.0;

    private static CWidget page() {
        return CGuiDocument.read("rework/page_developer");
    }

    private static CWidget child(CWidget parent, String name) {
        assertNotNull(parent, "la page doit exister");
        CWidget found = parent.child(name);
        assertNotNull(found, "la page doit porter « " + name + " »");
        return found;
    }

    @Test
    @DisplayName("l'application lit la page de la machine du developpeur, a sa taille")
    void laPageEstCelleDuDeveloppeur() {
        CWidget main = page();
        assertEquals("main", main.name(), "la racine du document s'appelle « main »");
        assertEquals(PAGE_WIDTH, main.transform.width, 1.0e-9);
        assertEquals(PAGE_HEIGHT, main.transform.height, 1.0e-9);
    }

    @Test
    @DisplayName("la page porte l'arbre, et les deux panneaux de gauche que l'ecran remplit")
    void laPagePorteSesMorceaux() {
        CWidget main = page();

        // L'arbre : la zone, et la bande qui la decore.
        child(child(main, "parent_right"), "area");
        child(child(main, "parent_right"), "ui_right");

        CWidget left = child(main, "parent_left");
        child(left, "ui_left");

        // Le panneau de l'aptitude : l'icone, le nom, le niveau, l'experience, la barre — et le
        // bouton que l'application cache, parce qu'elle n'apprend rien.
        CWidget ability = child(left, "panel_ability");
        for (String name : new String[] { "logo_ability", "text_abilityname", "text_level",
                "text_exp", "logo_progress", "btn_upgrade" }) {
            child(ability, name);
        }

        // Et le panneau de la machine, que l'application cache entierement.
        child(left, "panel_machine");
    }
}
