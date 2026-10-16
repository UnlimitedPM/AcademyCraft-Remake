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

    /**
     * Un bord d'aplat se compte depuis le coin du CADRE, et pas depuis la racine de l'original.
     *
     * <p>C'est le demi-pixel de `placeSnapped`, vu de l'autre bout : la racine de l'original tombait
     * a {@code guiLeft - 0,5}, donc tout ce qu'il dessinait en aplat etait decale d'un demi-pixel
     * vers la gauche et vers le haut. Un bord entre deux pixels prenait le suivant ; un bord pile
     * sur un pixel prenait celui-la. Le cadre ayant ete ramene au pixel, c'est cette regle-la qui
     * rend aux aplats leur place relative — et c'est elle qui a rendu au joueur ses barres :
     * « tu as mis les barres des graph trop grosse et un peu trop en haut ».
     */
    @Test
    @DisplayName("un bord d'aplat se compte depuis le coin du cadre")
    void lesBordsDUnAplatSeComptentDepuisLeCadre() {
        assertEquals(23, CGuiLayout.framePixel(22.4), "entre deux pixels, il prend le suivant");
        assertEquals(22, CGuiLayout.framePixel(22.0), "pile sur un pixel, il prend celui-la");
        assertEquals(29, CGuiLayout.framePixel(28.8), "et la borne exclue suit la meme regle");
        assertEquals(0, CGuiLayout.framePixel(0.0), "le coin du cadre lui-meme");
        assertEquals(-3, CGuiLayout.framePixel(-3.6), "y compris au-dessus du cadre");
    }

    /**
     * Une barre d'histogramme occupe les SIX pixels que la meme barre occupe chez l'original.
     *
     * <p>Deux retours du joueur pour arriver la, et les deux fois c'est le meme demi-pixel qui
     * parlait. La barre fait seize unites dessinees a quatre dixiemes, soit 6,4 pixels : d'abord
     * posee a partir de son pixel arrondi, elle finissait un pixel trop tot a droite (« les barres
     * bleues et orange sont bien 1 pixel moins large sur la droite ») ; puis mesuree depuis l'absolu,
     * elle commencait un pixel trop tot de l'autre cote et se retrouvait un pixel trop haute —
     * « trop grosse et un peu trop en haut ».
     */
    @Test
    @DisplayName("une barre d'histogramme occupe six pixels, du premier au dernier")
    void laBarreOccupeLesBonPixels() {
        CWidget.ProgressBar bar = new CWidget.ProgressBar();
        bar.dir = CWidget.ProgressBar.Direction.UP;
        bar.progress = 1.0;

        // La barre d'un histogramme : 16 sur 120 unites dessinees a quatre dixiemes, posees a 22,4
        // de la boite. Sur l'ecran, elle fait donc 6,4 sur 48 pixels.
        CGuiLayout.Bar shape = CGuiLayout.bar(bar, 16 * 0.4, 120 * 0.4);
        CGuiLayout.Rect rect = CGuiLayout.pixels(shape, 100.4, 40.0);

        assertEquals(101, rect.x0(), "le premier pixel entierement couvert");
        assertEquals(107, rect.x1(), "et la borne exclue : six pixels, de 101 a 106");
        assertEquals(6, rect.x1() - rect.x0());
        assertEquals(40, rect.y0(), "la boite commence ou elle commence : rien a rogner");
        assertEquals(88, rect.y1(), "et la barre prend ses quarante-huit pixels de haut");

        // Un bord qui tombe pile sur un pixel ne le deborde pas : a 10,0 la barre commence au 10,
        // et un remplissage d'un quart (25 pixels) va donc jusqu'au 34 inclus.
        bar.dir = CWidget.ProgressBar.Direction.RIGHT;
        bar.progress = 0.25;
        CGuiLayout.Rect quart = CGuiLayout.pixels(CGuiLayout.bar(bar, 100, 20), 10.0, 5.0);
        assertEquals(10, quart.x0());
        assertEquals(35, quart.x1());
        assertEquals(25, quart.x1() - quart.x0(), "vingt-cinq pixels pleins");

        // Et vers le haut, la barre part du BAS : son premier pixel est celui du bas de la boite.
        bar.dir = CWidget.ProgressBar.Direction.UP;
        CGuiLayout.Rect haut = CGuiLayout.pixels(CGuiLayout.bar(bar, 100, 20), 10.0, 5.0);
        assertEquals(20, haut.y0(), "les trois quarts du bas, sur vingt pixels de haut");
        assertEquals(25, haut.y1(), "et son dernier pixel est le dernier de la boite");
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
