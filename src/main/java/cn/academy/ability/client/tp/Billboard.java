package cn.academy.ability.client.tp;

/**
 * Les quatre coins d'un carre tourne vers l'oeil.
 *
 * <p>Les particules de ce mod sont des carres poses dans le plan de l'ecran : leurs deux directions
 * sont la gauche et le haut de la camera, et ils se presentent donc toujours de face. Reste a savoir
 * dans quel sens va l'image, et c'est la seule chose qui se rate <b>en silence</b> : dans Minecraft,
 * <b>v vaut 0 en haut de l'image et 1 en bas</b>. Un carre qui prendrait v pour une hauteur d'ecran
 * se dessine a l'envers, et rien ne le dit — aucune des quatre portes ne regarde un rendu, et une
 * etincelle symetrique ne le montre jamais. Les fragments de formule, eux, sont des glyphes : c'est
 * le joueur qui l'a vu, et c'est ce tableau qui repare les trois rendus d'un coup.
 *
 * <p>L'ordre est celui de l'original ({@code Sprite.draw}), recopie tel quel : haut-gauche,
 * bas-gauche, bas-droit, haut-droit, chacun avec son coin d'image.
 */
public final class Billboard {

    /** Quatre coins : de combien reculer sur la gauche, de combien monter, et ou lire l'image. */
    private static final double[][] CORNERS = {
            { -1, 1, 0, 0 },
            { -1, -1, 0, 1 },
            { 1, -1, 1, 1 },
            { 1, 1, 1, 0 },
    };

    private Billboard() {
    }

    /** Combien il y a de coins. */
    public static int corners() {
        return CORNERS.length;
    }

    /** La part de gauche d'un coin, de -1 a 1. */
    public static double along(int corner) {
        return CORNERS[corner][0];
    }

    /** Sa part de hauteur, de -1 a 1. */
    public static double high(int corner) {
        return CORNERS[corner][1];
    }

    /** Ou l'on lit l'image, en travers : zero a gauche. */
    public static float u(int corner) {
        return (float) CORNERS[corner][2];
    }

    /** Et de haut en bas : zero est le HAUT de l'image, comme partout dans Minecraft. */
    public static float v(int corner) {
        return (float) CORNERS[corner][3];
    }
}
