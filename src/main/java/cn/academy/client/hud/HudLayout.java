package cn.academy.client.hud;

import java.util.EnumMap;
import java.util.Map;

/**
 * La position de chaque element du HUD.
 *
 * <p>Portage de {@code ACHud.Node} : l'original rangeait ces couples dans la config du mod
 * (categorie {@code gui}, cle = nom de l'element), et reecrivait le widget a chaque image.
 * La classe qui suit ne fait que tenir les nombres et les placer sur l'ecran ; c'est
 * {@link HudConfig} qui les sauvegarde.
 *
 * <p>La borne de l'original est reprise : de {@code -512} a {@code 512}. Hors bornes, la
 * valeur est <b>refusee</b> — dans l'ecran de reglage, le champ passe au rouge et la saisie
 * ne s'applique pas.
 */
public class HudLayout {

    /** La borne de l'original, dans les deux sens. */
    public static final double LIMIT = 512.0;

    private final Map<HudElement, double[]> positions = new EnumMap<>(HudElement.class);

    public HudLayout() {
        resetAll();
    }

    /** Remet tous les elements a leur place d'origine. */
    public void resetAll() {
        for (HudElement element : HudElement.values()) {
            reset(element);
        }
    }

    /** Remet un element a sa place d'origine. */
    public void reset(HudElement element) {
        positions.put(element, new double[] {element.getDefaultX(), element.getDefaultY()});
    }

    public double getX(HudElement element) {
        return pair(element)[0];
    }

    public double getY(HudElement element) {
        return pair(element)[1];
    }

    /** Une valeur tient-elle dans les bornes de l'original ? */
    public static boolean isValid(double value) {
        return !Double.isNaN(value) && value >= -LIMIT && value <= LIMIT;
    }

    /**
     * Deplace un element. Rend faux — et ne change rien — si une des deux valeurs sort des
     * bornes : c'est ce refus qui fait passer le champ au rouge dans l'ecran de reglage.
     */
    public boolean set(HudElement element, double x, double y) {
        if (!isValid(x) || !isValid(y)) return false;
        positions.put(element, new double[] {x, y});
        return true;
    }

    /** Deplace un element sans verifier : pour relire une config deja validee. */
    public void setRaw(HudElement element, double x, double y) {
        if (isValid(x) && isValid(y)) {
            set(element, x, y);
        } else {
            reset(element);
        }
    }

    private double[] pair(HudElement element) {
        double[] pair = positions.get(element);
        return pair == null ? new double[] {element.getDefaultX(), element.getDefaultY()} : pair;
    }

    /**
     * Le bord gauche de l'element, en pixels, pour un ecran de cette largeur.
     *
     * <p>Ancre a gauche, X est la distance depuis le bord gauche ; ancre a droite, X est un
     * ecart depuis le bord droit, donc <b>negatif</b> — c'est le signe de l'original.
     */
    public int placeX(HudElement element, int screenWidth, int elementWidth) {
        double x = getX(element);
        return switch (element.getSide()) {
            case LEFT -> (int) Math.round(x);
            case RIGHT -> (int) Math.round(screenWidth - elementWidth + x);
        };
    }

    /**
     * Le bord haut de l'element, en pixels, pour un ecran de cette hauteur.
     *
     * <p>L'ecart s'ajoute toujours dans le sens de l'ecran : ancre en haut, Y descend depuis
     * le haut ; ancre au milieu, Y descend depuis le milieu ; ancre en bas, Y monte depuis le
     * bas (donc negatif dans les defauts). C'est la regle qui pose le rappel des touches a
     * {@code 0/30} trente pixels sous le milieu, et non au-dessus.
     */
    public int placeY(HudElement element, int screenHeight, int elementHeight) {
        double y = getY(element);
        return switch (element.getSlide()) {
            case TOP -> (int) Math.round(y);
            case MIDDLE -> (int) Math.round(screenHeight / 2.0 - elementHeight / 2.0 + y);
            case BOTTOM -> (int) Math.round(screenHeight - elementHeight + y);
        };
    }
}
