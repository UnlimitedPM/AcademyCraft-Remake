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

        for (CWidget child : widget.children()) {
            place(child, widget.absX, widget.absY, t.width, t.height, scale);
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
