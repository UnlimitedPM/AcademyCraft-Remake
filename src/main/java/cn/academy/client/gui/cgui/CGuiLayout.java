package cn.academy.client.gui.cgui;

/**
 * Ou chaque widget tombe sur l'ecran : le calcul de CGui, en nombres nus.
 *
 * <p>C'est le coeur de ce qu'il fallait reprendre de l'ancien moteur, et c'est aussi ce qui se
 * relit sans lancer un jeu : les nombres du fichier sont en unites de conception, l'echelle du
 * noeud racine vaut {@code 0,24}, et tout ce qui suit en decoule.
 *
 * <h2>Les trois regles de CGui</h2>
 *
 * <p>Un widget s'accroche a un point de son parent — {@code alignWidth} et {@code alignHeight}
 * disent de quel bord part son {@code x} et son {@code y} : rien a gauche, tout a droite, et la
 * <b>moitie de l'espace libre</b> au centre — puis le {@code x} du fichier s'ajoute par-dessus. Le
 * pivot du fichier ne joue pas ici : l'original ne s'en sert pas pour poser ses widgets, et les
 * documents du mod le laissent a zero partout.
 *
 * <p>L'echelle, enfin, se <b>multiplie</b> le long de l'arbre : la racine divise tout par quatre
 * environ, et un enfant qui voudrait rapetisser encore le ferait depuis la.
 */
public final class CGuiLayout {

    private CGuiLayout() {}

    /**
     * Pose un widget et toute sa descendance.
     *
     * @param parentX     le coin gauche du parent, en pixels
     * @param parentY     son coin haut, en pixels
     * @param parentW     sa largeur en unites de conception — l'espace ou ses enfants se comptent
     * @param parentH     idem en hauteur
     * @param parentScale l'echelle qui arrive du parent
     */
    public static void place(CWidget widget, double parentX, double parentY,
                             double parentW, double parentH, double parentScale) {
        place(widget, parentX, parentY, parentW, parentH, parentScale, false);
    }

    /**
     * Le meme, mais la <b>racine</b> se pose au pixel entier : c'est le cadre d'un conteneur.
     *
     * <p>La formule du document lui donne un demi-pixel — {@code (207 - 172) / 2 - 18 = -0,5} — et
     * l'original le dessinait ainsi ; mais le cadre doit tomber juste sur les trous des emplacements
     * du jeu, que vanilla dessine a des positions <b>entieres</b>. Le joueur l'a vu des que les
     * elements ont pris leur position exacte : « le contenu a l'air d'etre bon, mais le conteneur
     * n'est lui meme plus bon, il faudrait decaler le GUI entier un peu plus en bas a droite ». La
     * racine est donc ramenee au pixel, et <b>tout ce qu'elle contient garde sa position exacte</b>.
     */
    public static void placeSnapped(CWidget root, double parentX, double parentY,
                                    double parentW, double parentH, double parentScale) {
        place(root, parentX, parentY, parentW, parentH, parentScale, true);
    }

    private static void place(CWidget widget, double parentX, double parentY,
                              double parentW, double parentH, double parentScale, boolean snap) {
        CWidget.Transform t = widget.transform;
        double scale = parentScale * t.scale;

        widget.absW = t.width * scale;
        widget.absH = t.height * scale;
        widget.absScale = scale;

        // L'espace que le parent laisse libre, puis la part qu'en prend l'alignement : rien a
        // gauche, la moitie au centre, tout a droite. C'est la formule de l'original, et c'est
        // elle qui fait coincider le document et les slots quand le parent est le conteneur.
        double spanX = parentW * parentScale - widget.absW;
        double spanY = parentH * parentScale - widget.absH;

        widget.absX = parentX + spanX * factor(t.alignWidth) + t.x * parentScale;
        widget.absY = parentY + spanY * factor(t.alignHeight) + t.y * parentScale;

        if (snap) {
            widget.absX = Math.round(widget.absX);
            widget.absY = Math.round(widget.absY);
        }

        if (widget.list != null) {
            list(widget, scale);
            return;
        }

        for (CWidget child : widget.children()) {
            place(child, widget.absX, widget.absY, t.width, t.height, scale, false);
        }
    }

    /**
     * Le morceau de barre qu'une barre de progression remplit : son rectangle, puis la portion de
     * texture qui va avec.
     *
     * <p>Le rectangle et les coordonnees de texture se calculent ensemble, et c'est tout l'esprit
     * du composant : une barre qui monte ne dessine pas toute sa texture en la coupant, elle en
     * dessine une <b>part</b>, du bon cote. Voir le {@code switch} de l'original, repris tel quel.
     */
    public record Bar(double x, double y, double w, double h, double u, double v,
                      double uw, double vh) {}

    /** Le morceau a dessiner pour cette barre, dans la boite du widget. */
    public static Bar bar(CWidget.ProgressBar bar, double width, double height) {
        double disp = Math.max(0.0, Math.min(1.0, bar.progress));

        return switch (bar.dir) {
            case RIGHT -> new Bar(0, 0, width * disp, height, 0, 0, disp, 1);
            case LEFT -> new Bar(width * (1 - disp), 0, width * disp, height, 1 - disp, 0, disp, 1);
            case UP -> new Bar(0, height * (1 - disp), width, height * disp, 0, 1 - disp, 1, disp);
            case DOWN -> new Bar(0, 0, width, height * disp, 0, 0, 1, disp);
        };
    }

    /** Le rectangle d'une barre, en <b>pixels d'ecran</b> : du premier au dernier pixel allume. */
    public record Rect(int x0, int y0, int x1, int y1) {}

    /**
     * Ce rectangle, ramene aux pixels que la barre occupe.
     *
     * <p>L'original ne dessinait pas des pixels : il posait un quad a des coordonnees
     * <b>fractionnaires</b>. La barre d'un histogramme fait seize unites a l'echelle 0,4, soit 6,4
     * pixels, et c'est {@link #framePixel} qui dit lesquels elle occupe.
     *
     * @param x le coin gauche du widget, en pixels d'ecran — fraction comprise
     * @param y idem en hauteur
     */
    public static Rect pixels(Bar bar, double x, double y) {
        return new Rect(framePixel(x + bar.x()), framePixel(y + bar.y()),
                framePixel(x + bar.x() + bar.w()), framePixel(y + bar.y() + bar.h()));
    }

    /**
     * Le premier pixel qu'un bord occupe, compte <b>depuis le coin du cadre</b>.
     *
     * <p>C'est la regle des aplats — une barre de progression, les deux bandes claires d'un panneau —
     * et elle tient tout entiere dans le demi-pixel de la racine. L'original posait la sienne a
     * {@code guiLeft - 0,5} : tout ce qu'il dessinait etait donc decale d'un demi-pixel vers la
     * gauche et vers le haut, et un bord qui tombait <b>entre</b> deux pixels allumait le suivant,
     * tandis qu'un bord qui tombait pile sur un pixel allumait celui-la. C'est exactement {@code ceil},
     * mesure depuis le coin du cadre.
     *
     * <p>Et c'est bien le CADRE qui sert de repere, parce que le port a ramene la racine au pixel
     * (voir {@link #placeSnapped} : les trous des emplacements du jeu sont a des positions entieres).
     * Un aplat qui garderait son ancien pixel ne suivrait donc pas ce demi-pixel, et flotterait un
     * pixel a gauche et en haut de tout le reste — ce que le joueur a vu tout de suite, sur toutes
     * les machines a histogramme : « tu as mis les barres des graph trop grosse et un peu trop en
     * haut », et sur le panneau : « celle du dessous est plus petite, et celle du haut est un peu
     * trop en haut a droite ».
     *
     * <p>La mesure n'est la mesure du cadre que si l'origine de l'arbre est entiere : les ecrans de
     * machine passent par {@code placeSnapped}, donc c'est bien le cas la ou cette regle sert.
     */
    static int framePixel(double edge) {
        return (int) Math.ceil(edge);
    }

    /**
     * Combien de lignes de cette liste tiennent entierement dans sa boite, a partir de son rang.
     *
     * <p>C'est la boucle de {@code ElementList.updateList} : une ligne qui depasse, meme d'un
     * pixel, ne se dessine pas du tout — le contraire d'un ecran qui la couperait. Le joueur ne
     * voit donc jamais une ligne a moitie, et c'est ce qui donne son air a la page.
     */
    public static int visibleRows(double[] heights, double spacing, double boxHeight, int progress) {
        double sum = 0;
        for (int i = Math.max(0, progress); i < heights.length; i++) {
            if (sum + heights[i] > boxHeight) return i - Math.max(0, progress);
            sum += heights[i] + spacing;
        }
        return heights.length - Math.max(0, progress);
    }

    /** Le rangement d'une liste : ses lignes visibles, l'une sous l'autre. */
    private static void list(CWidget widget, double scale) {
        int count = widget.children().size();
        double[] heights = new double[count];
        for (int i = 0; i < count; i++) {
            heights[i] = widget.children().get(i).transform.height;
        }

        int progress = Math.max(0, widget.list.progress);
        int visible = visibleRows(heights, widget.list.spacing, widget.transform.height, progress);
        double sum = 0;

        for (int i = 0; i < count; i++) {
            CWidget child = widget.children().get(i);
            if (i < progress || i >= progress + visible) {
                child.transform.doesDraw = false;
            } else {
                child.transform.doesDraw = true;
                child.transform.x = 0;
                child.transform.y = sum;
                sum += heights[i] + widget.list.spacing;
            }
            place(child, widget.absX, widget.absY, widget.transform.width,
                    widget.transform.height, scale);
        }
    }

    /** Le cote de l'ancre, en fraction de l'espace libre : zero a gauche, la moitie au centre. */
    static double factor(String align) {
        if (align == null) return 0.0;
        return switch (align.toUpperCase(java.util.Locale.ROOT)) {
            case "CENTER" -> 0.5;
            case "RIGHT", "BOTTOM" -> 1.0;
            default -> 0.0;
        };
    }
}
