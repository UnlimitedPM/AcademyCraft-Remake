package cn.academy.client.gui.cgui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
