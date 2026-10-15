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
 * disent de quel bord part son {@code x} et son {@code y} — puis son <b>pivot</b> dit ou ce point
 * tombe dans le widget lui-meme. Le pivot vaut {@code 0, 0} partout dans ce fichier, donc le point
 * d'accroche est le coin haut-gauche ; mais la racine, elle, s'accroche au CENTRE de l'ecran,
 * ce qui est toute l'astuce du fichier : ses enfants se comptent depuis le milieu.
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
        CWidget.Transform t = widget.transform;
        double scale = parentScale * t.scale;

        double anchorX = parentX + anchor(t.x, parentW, t.alignWidth) * parentScale;
        double anchorY = parentY + anchor(t.y, parentH, t.alignHeight) * parentScale;

        widget.absW = t.width * scale;
        widget.absH = t.height * scale;
        widget.absX = anchorX - t.pivotX * widget.absW;
        widget.absY = anchorY - t.pivotY * widget.absH;
        widget.absScale = scale;

        if (widget.list != null) {
            list(widget, scale);
            return;
        }

        for (CWidget child : widget.children()) {
            place(child, widget.absX, widget.absY, t.width, t.height, scale);
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

    /** L'origine d'un cote : le bord demande, plus le decalage du fichier. */
    private static double anchor(double value, double size, String align) {
        if (align == null) return value;
        return switch (align.toUpperCase(java.util.Locale.ROOT)) {
            case "CENTER" -> size * 0.5 + value;
            case "RIGHT", "BOTTOM" -> size + value;
            default -> value;
        };
    }
}
