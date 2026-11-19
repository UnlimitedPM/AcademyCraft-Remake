package cn.academy;

/**
 * Les chiffres de l'eolienne, sans Minecraft.
 *
 * <p>Portage de {@code WindGeneratorConsts} : la hauteur de colonne exigee, et la vitesse
 * des pales. Ils vivaient dans la base ({@link WindgenBaseBlockEntity}) alors que le
 * rotor en a besoin aussi : ils sont ici une fois pour les deux, et se relisent en JUnit —
 * une classe de block entity ne se charge pas dans un test unitaire.
 */
public final class WindgenStructure {

    /** Nombre de piliers exiges entre la base et le rotor. */
    public static final int MIN_PILLARS = 8;

    /** Nombre de piliers au-dela duquel la colonne est refusee. */
    public static final int MAX_PILLARS = 40;

    /**
     * Vitesse des pales, en degres par seconde.
     *
     * <p>{@code TileWindGenMain.getSpinSpeed} : soixante degres par seconde, soit un tour
     * en six secondes — et seulement quand la colonne est complete. Une helice installee
     * sur une colonne trop courte ne tourne pas du tout.
     */
    public static final float SPIN_PER_SECOND = 60.0f;

    private WindgenStructure() {}

    /**
     * La colonne tient-elle debout ?
     *
     * <p>Il faut assez de piliers, et la base au pied : une colonne qui s'arrete sur de la
     * pierre n'en est pas une. Le nombre maximal est verifie par celui qui compte les
     * piliers, parce que lui seul sait s'arreter.
     */
    public static boolean isComplete(int pillars, boolean baseAtBottom) {
        return baseAtBottom && pillars >= MIN_PILLARS;
    }

    /** La vitesse des pales a cet instant, en degres par seconde. */
    public static float spinSpeed(boolean complete) {
        return complete ? SPIN_PER_SECOND : 0.0f;
    }

    /**
     * L'angle des pales apres {@code dt} secondes.
     *
     * <p>L'original accumulait l'angle sur le block entity, cote client : c'est ce qui
     * permet a une eolienne qui s'arrete de s'arreter *ou* elle est, au lieu de revenir
     * a la verticale a chaque revision de la structure.
     */
    /**
     * L'angle des pales a un instant donne, mesure depuis une ancre.
     *
     * <p>C'est une <b>fonction du temps</b>, et non une accumulation d'une image a l'autre. Le port
     * accumulait : chaque image ajoutait {@code vitesse x duree de l'image}, si bien qu'une image
     * plus longue ou plus courte faisait avancer les pales d'un cran de plus ou de moins. A l'arret,
     * cela ne se voyait pas ; en marchant ou en tournant la tete, le decor glisse parfaitement lisse
     * et les pales, non — le joueur a decrit cette « sensation bizarre a l'ecran » le 22/10.
     *
     * <p>Une fonction du temps ne peut pas deriver : deux rendus pour le meme instant donnent le
     * meme angle, et un temps qui recule (une passe de rendu en retard) ne fait pas tourner les
     * pales a l'envers.
     *
     * <p>Fonction pure, donc verifiable.
     *
     * @param seconds       l'instant de l'image, en secondes
     * @param anchorSeconds l'instant ou l'ancre a ete posee
     * @param anchorAngle   l'angle a cet instant-la
     * @param spinning      vrai si les pales tournent
     */
    public static float angleAt(double seconds, double anchorSeconds, float anchorAngle,
                                boolean spinning) {
        if (!spinning) return ((anchorAngle % 360.0f) + 360.0f) % 360.0f;
        double dt = Math.max(0.0d, seconds - anchorSeconds);
        return (float) ((anchorAngle + SPIN_PER_SECOND * dt) % 360.0d);
    }
}
