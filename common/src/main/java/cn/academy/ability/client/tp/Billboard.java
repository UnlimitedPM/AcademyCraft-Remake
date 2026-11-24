package cn.academy.ability.client.tp;

/**
 * Les quatre coins d'un carre tourne vers l'oeil.
 *
 * <p>Les particules de ce mod sont des carres poses dans le plan de l'ecran : leurs deux directions
 * sont la gauche et le haut de la camera, et ils se presentent donc toujours de face. Reste a savoir
 * dans quel sens va l'image, et c'est ce qui se rate <b>en silence</b> — deux fois de suite.
 *
 * <p><b>En hauteur</b> : dans Minecraft, `v` vaut 0 en haut de l'image et 1 en bas. Le prendre pour
 * une hauteur d'ecran retourne le carre de haut en bas.
 *
 * <p><b>En largeur</b> : `along` deplace le coin VERS LA GAUCHE DE LA CAMERA, donc `along` positif est
 * le bord <b>gauche de l'ecran</b> — et c'est lui qui doit lire `u = 0`. Le croire a l'envers est le
 * piege symetrique, et il ne se voit pas davantage.
 *
 * <p>Aucune des quatre portes ne regarde un rendu : une etincelle symetrique ne montre ni l'un ni
 * l'autre, et c'est le joueur qui a vu les glyphes de formule a l'envers, puis a l'envers dans l'autre
 * sens. Les deux conventions sont donc figees par {@code BillboardTest}.
 *
 * <p>L'ordre des coins est celui de l'original ({@code Sprite.draw}) : haut-gauche, bas-gauche,
 * bas-droit, haut-droit, chacun avec son coin d'image.
 */
public final class Billboard {

    /** Quatre coins : de combien aller vers la gauche de la camera, de combien monter, et l'image. */
    private static final double[][] CORNERS = {
            { 1, 1, 0, 0 },
            { 1, -1, 0, 1 },
            { -1, -1, 1, 1 },
            { -1, 1, 1, 0 },
    };

    private Billboard() {
    }

    /** Combien il y a de coins. */
    public static int corners() {
        return CORNERS.length;
    }

    /**
     * De combien ce coin va vers la gauche de la camera, de -1 a 1.
     *
     * <p>Positif, il est a gauche de l'ecran : c'est le bord qui lit `u = 0`.
     */
    public static double along(int corner) {
        return CORNERS[corner][0];
    }

    /** Sa part de hauteur, de -1 a 1 : positif en haut de l'ecran. */
    public static double high(int corner) {
        return CORNERS[corner][1];
    }

    /** Ou l'on lit l'image, en travers : zero est le bord gauche de l'ECRAN. */
    public static float u(int corner) {
        return (float) CORNERS[corner][2];
    }

    /** Et de haut en bas : zero est le HAUT de l'image, comme partout dans Minecraft. */
    public static float v(int corner) {
        return (float) CORNERS[corner][3];
    }
}
