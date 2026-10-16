package cn.academy.client.gui.cgui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La regle du clic : un widget est juge sur SA boite, et le dernier gagne.
 *
 * <p>Les deux ensemble sont le {@code gtnTraverse} de LambdaLib2, et c'est ce qui manquait au port.
 * Il exigeait que <b>chaque parent</b> contienne aussi le point, et rendait le clic au widget
 * <b>du fond</b>. Le bouton LEARN du developpeur tombe donc dans le trou : il fait cent
 * quatre-vingt-six unites de large et quarante de haut la ou son panneau n'en fait que cent quatre
 * sur trente-deux, et il deborde — vers la droite et vers le bas, exactement la ou le joueur clique.
 * Il l'a dit en trois mots : « appuyer sur le bouton ne fait rien ».
 *
 * <p>Rien ici n'a besoin du jeu : la vue se construit sur un arbre pose a la main, et le clic se
 * demande en pixels.
 */
class CGuiClickTest {

    /** Un widget pose, en pixels : la boite du clic, et celle du dessin. */
    private static CWidget box(String name, double x, double y, double width, double height) {
        CWidget widget = new CWidget(name);
        widget.transform.doesDraw = true;
        widget.transform.width = width;
        widget.transform.height = height;
        widget.absX = x;
        widget.absY = y;
        widget.absW = width;
        widget.absH = height;
        return widget;
    }

    @Test
    @DisplayName("un bouton qui deborde de son parent se clique quand meme")
    void unBoutonQuiDebordeSeClique() {
        CWidget root = box("root", 0, 0, 400, 187);
        CWidget panel = box("panel_ability", 0, 0, 104, 32);
        CWidget button = box("btn_upgrade", 60, 14, 93, 30);
        panel.add(button);
        root.add(panel);

        boolean[] clicked = { false };
        CGuiView view = new CGuiView(null, root);
        view.onClick(button, () -> clicked[0] = true);

        // Le centre du bouton tombe HORS du panneau, et c'est la tout le probleme.
        assertTrue(view.click(106, 29, 0), "le clic doit trouver le bouton");
        assertTrue(clicked[0], "et le bouton doit le recevoir");
    }

    @Test
    @DisplayName("le widget du dessus prend le clic, pas celui du fond")
    void leDessusGagne() {
        CWidget root = box("root", 0, 0, 400, 187);
        CWidget under = box("under", 0, 0, 100, 50);
        CWidget over = box("over", 0, 0, 100, 50);
        root.add(under);
        root.add(over);

        boolean[] underClicked = { false };
        boolean[] overClicked = { false };
        CGuiView view = new CGuiView(null, root);
        view.onClick(under, () -> underClicked[0] = true);
        view.onClick(over, () -> overClicked[0] = true);

        assertTrue(view.click(10, 10, 0), "le clic doit toucher quelque chose");
        assertTrue(overClicked[0], "c'est celui du dessus");
        assertTrue(!underClicked[0], "et pas celui du fond");
    }

    @Test
    @DisplayName("un clic sans action rend FAUX, pour laisser l'inventaire le prendre")
    void unClicSansActionRendFaux() {
        CWidget root = box("root", 0, 0, 400, 187);
        CWidget page = box("ui_inv", 0, 0, 176, 187);
        CWidget button = box("btn_left", 60, 14, 16, 16);
        page.add(button);
        root.add(page);

        CGuiView view = new CGuiView(null, root);

        // Le fond de la page couvre tout : sans cette regle, le clic sur un emplacement d'inventaire
        // serait pris par la page, et l'ecran de la machine n'aurait plus d'inventaire du tout.
        assertTrue(!view.click(10, 10, 0), "le fond n'a aucune action a faire");

        // Et le bouton, lui, dit oui — c'est ce que l'ecran de conteneur lit pour savoir s'il doit
        // passer le clic a vanilla. Vanilla, lui, rend VRAI des qu'un clic tombe dans son cadre :
        // les fleches du formeur de metal ne voyaient donc jamais le clic.
        boolean[] clicked = { false };
        view.onClick(button, () -> clicked[0] = true);
        assertTrue(view.click(68, 22, 0), "le bouton, si");
        assertTrue(clicked[0], "et il a fait ce qu'il avait a faire");
    }

    /** Un champ du panneau d'infos, avec son libelle et ses deux crochets. */
    private static CWidget line(String name, CWidget[] valueOut) {
        CWidget line = box(name, 0, 0, 90, 8);

        CWidget label = box("key", 0, 0, 40, 8);
        label.text = new CWidget.TextBox();
        label.text.content = "SSID";
        line.add(label);

        CWidget value = box("value", 46, 0, 40, 8);
        value.text = new CWidget.TextBox();
        value.text.allowEdit = true;
        line.add(value);
        valueOut[0] = value;

        // Les crochets se posent six unites PAR-DESSUS le champ : c'est l'original, et c'est la
        // tout le probleme — sur un champ vide, le regard vise justement ce bord-la.
        line.add(box("bracket", 42, 0, 10, 8));
        line.add(box("bracket", 88, 0, 10, 8));
        return line;
    }

    @Test
    @DisplayName("un champ prend le clic meme sous un crochet qui le recouvre")
    void unChampSousUnCrochetPrendLeClic() {
        CWidget root = box("root", 0, 0, 400, 187);
        CWidget[] value = new CWidget[1];
        root.add(line("line1", value));

        CGuiView view = new CGuiView(null, root);

        // Le crochet n'ecoute rien : il doit laisser passer le clic au champ qu'il decore. Sans
        // cette regle le champ ne prenait pas le clavier, et le joueur ne pouvait plus rien ecrire.
        assertTrue(view.click(48, 4, 0), "le clic doit etre pris par le champ");
        assertSame(value[0], view.focused(), "et c'est le champ qui a le clavier");
        assertTrue(view.charTyped('a'), "on doit pouvoir ecrire");
        assertEquals("a", value[0].text.content, "et la lettre arrive dans le champ");
    }

    @Test
    @DisplayName("un clic sur le libelle, lui, rend le clavier")
    void unClicSurLeLibelleRendLeClavier() {
        CWidget root = box("root", 0, 0, 400, 187);
        CWidget[] value = new CWidget[1];
        root.add(line("line1", value));

        CGuiView view = new CGuiView(null, root);
        assertTrue(view.click(48, 4, 0), "le champ prend le clavier");

        // Le libelle n'est pas un champ : cliquer dessus ne doit rien ecrire, et doit rendre le
        // clavier — c'est ce que fait l'original, dont le libelle n'ecoutait rien du tout.
        assertTrue(!view.click(20, 4, 0), "un libelle n'a rien a faire");
        assertTrue(view.focused() == null, "et il rend le clavier");
    }
}
