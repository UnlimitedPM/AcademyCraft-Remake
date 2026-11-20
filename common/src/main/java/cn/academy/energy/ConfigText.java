package cn.academy.energy;

/**
 * Ce qu'un nom ou un mot de passe du reseau sans fil peut faire de caracteres.
 *
 * <p>C'est une seule longueur pour tout le mod — le noeud, le Matrix, et le champ qui les ecrit a
 * l'ecran — et elle est rangee ici parce que trois copies de la meme regle finiraient par diverger.
 * Seize caracteres : c'est ce qui tient dans la colonne des valeurs du panneau d'infos, qui fait
 * quarante unites de large.
 */
public final class ConfigText {

    /** Le nombre de caracteres qu'un nom de noeud, un SSID ou un mot de passe peut porter. */
    public static final int MAX_LENGTH = 16;

    private ConfigText() {
    }

    /**
     * Nettoie ce qu'un joueur a tape : les espaces des deux bouts partent, et le reste est coupe a la
     * longueur maximale.
     *
     * <p>Le champ de l'ecran s'arrete de lui-meme a cette longueur, mais rien n'oblige un client
     * modifie a s'y tenir : c'est donc sur le serveur que la regle compte, et c'est ici qu'elle est
     * ecrite une fois pour toutes.
     *
     * <p>Un nom qui commence par une espace ne se lit pas dans un panneau aligne a droite : c'est la
     * raison du {@code trim}, et non un detail de confort.
     */
    public static String clean(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() <= MAX_LENGTH ? trimmed : trimmed.substring(0, MAX_LENGTH);
    }
}
