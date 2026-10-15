package cn.academy.client.gui.cgui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Les deux regles de dessin du moteur qui ne dependent d'aucun ecran : la barre de progression et
 * la liste.
 *
 * <p>Ce sont les deux composants que les pages de machines reclamaient, et tous les deux ont une
 * partie pure — le morceau de barre a dessiner, et les lignes d'une liste qui tiennent dans sa
 * boite. Le reste (la texture, le focus, le clic) a besoin du jeu ; ceci, non, et c'est donc ici
 * qu'on fige les nombres, ceux de l'original.
 */
class CGuiLayoutTest {

    private static final double EPS = 1e-9;

    /**
     * Un texte sans boite s'accroche au centre de son parent : il faut donc que le parent AIT une
     * boite.
     *
     * <p>C'est ce qui a cache la surface du developeur au joueur. Son contenu est un widget neuf,
     * donc large et haut de ZERO, et ses textes se posaient alors sur son coin : ceux qui sont
     * au-dessus du centre — le nom, le niveau et la description — tombaient au-dessus du bord de la
     * page, et le joueur n'a rien vu. Il l'a dit ainsi : « quand je click sur un pouvoir, je n'ai pas
     * la description ». Le parent doit donc prendre la boite de la page, et c'est ce que ceci fige :
     * la meme chaine de widgets ne tombe pas au meme endroit selon que le parent a une boite ou non.
     */
    @Test
    @DisplayName("un parent sans boite pose ses enfants sur son coin, pas sur son centre")
    void unParentSansBoitePerdSesEnfants() {
        CWidget page = new CWidget("page");
        page.transform.width = 400;
        page.transform.height = 187;

        CWidget boxed = new CWidget("boxed");
        boxed.transform.width = 400;
        boxed.transform.height = 187;
        boxed.add(texteSansBoite(-58));

        CWidget empty = new CWidget("empty");
        empty.add(texteSansBoite(-58));

        page.add(boxed);
        page.add(empty);
        CGuiLayout.place(page, 0, 0, 400, 187, 1.0);

        CWidget dansLaBoite = boxed.children().get(0);
        assertEquals(200, dansLaBoite.absX, EPS, "le centre en largeur de la page");
        assertEquals(93.5 - 58, dansLaBoite.absY, EPS, "le centre en hauteur, moins les 58 unites");

        CWidget dansLeVide = empty.children().get(0);
        assertEquals(0, dansLeVide.absX, EPS, "sans boite, le texte reste sur le bord");
        assertEquals(-58, dansLeVide.absY, EPS, "et passe donc au-dessus du haut de la page");
    }

    /** Une ligne de texte centree : c'est la boite vide qui la fait s'accrocher au centre. */
    private static CWidget texteSansBoite(double y) {
        CWidget text = new CWidget("text");
        text.transform.width = 0;
        text.transform.height = 0;
        text.transform.y = y;
        text.transform.alignWidth = "CENTER";
        text.transform.alignHeight = "CENTER";
        return text;
    }

    @Test
    @DisplayName("une barre qui monte prend le BAS de son image, et celle qui descend le HAUT")
    void laBarrePrendLeBonMorceau() {
        CWidget.ProgressBar bar = new CWidget.ProgressBar();
        bar.progress = 0.25;

        // Vers la droite : le remplissage part du bord gauche, et prend le debut de la texture.
        bar.dir = CWidget.ProgressBar.Direction.RIGHT;
        CGuiLayout.Bar right = CGuiLayout.bar(bar, 100, 20);
        assertEquals(0, right.x(), EPS);
        assertEquals(25, right.w(), EPS);
        assertEquals(0, right.u(), EPS);
        assertEquals(0.25, right.uw(), EPS);

        // Vers la gauche : c'est l'autre bout de la texture.
        bar.dir = CWidget.ProgressBar.Direction.LEFT;
        CGuiLayout.Bar left = CGuiLayout.bar(bar, 100, 20);
        assertEquals(75, left.x(), EPS, "le remplissage colle au bord droit");
        assertEquals(0.75, left.u(), EPS, "et prend la FIN de l'image");
        assertEquals(0.25, left.uw(), EPS);

        // Vers le haut : la barre monte depuis le bas, donc son rectangle est en bas, et la
        // portion de texture est celle du bas elle aussi.
        bar.dir = CWidget.ProgressBar.Direction.UP;
        CGuiLayout.Bar up = CGuiLayout.bar(bar, 100, 20);
        assertEquals(15, up.y(), EPS);
        assertEquals(5, up.h(), EPS);
        assertEquals(0.75, up.v(), EPS);
        assertEquals(0.25, up.vh(), EPS);

        // Vers le bas : elle pend depuis le haut.
        bar.dir = CWidget.ProgressBar.Direction.DOWN;
        CGuiLayout.Bar down = CGuiLayout.bar(bar, 100, 20);
        assertEquals(0, down.y(), EPS);
        assertEquals(0, down.v(), EPS);
        assertEquals(0.25, down.vh(), EPS);

        // Et le remplissage est borne : une machine ne remplit pas plus que plein, ni moins que
        // vide, meme si ses nombres partent de travers.
        bar.dir = CWidget.ProgressBar.Direction.RIGHT;
        bar.progress = 1.5;
        assertEquals(100, CGuiLayout.bar(bar, 100, 20).w(), EPS);
        bar.progress = -3;
        assertEquals(0, CGuiLayout.bar(bar, 100, 20).w(), EPS, "et une barre vide ne se remplit pas");
    }

    @Test
    @DisplayName("une ligne qui depasse ne se dessine pas du tout")
    void uneLigneQuiDepasseNeSeDessinePas() {
        double[] heights = { 80, 80, 80 };

        // Trois lignes de quatre-vingts, et deux entre elles : la troisieme commence a 164 et
        // finirait a 244, donc elle ne tient pas dans 200 et ne se dessine pas. C'est la regle de
        // l'original, et c'est ce qui fait qu'on ne voit JAMAIS une ligne a moitie.
        assertEquals(2, CGuiLayout.visibleRows(heights, 2.0, 200, 0));
        assertEquals(3, CGuiLayout.visibleRows(heights, 2.0, 244, 0), "244 la fait tenir pile");
        assertEquals(2, CGuiLayout.visibleRows(heights, 2.0, 243, 0), "243 non");

        // Le defilement decale le compte : a partir du deuxieme rang, il en reste deux.
        assertEquals(2, CGuiLayout.visibleRows(heights, 2.0, 200, 1));
        assertEquals(1, CGuiLayout.visibleRows(heights, 2.0, 200, 2));

        // Un defilement qui depasse la fin ne montre plus rien plutot que de se tromper.
        assertEquals(0, CGuiLayout.visibleRows(heights, 2.0, 200, 3));

        // Et une boite plus grande que tout garde toutes les lignes.
        assertEquals(3, CGuiLayout.visibleRows(heights, 2.0, 1000, 0));
    }
}
