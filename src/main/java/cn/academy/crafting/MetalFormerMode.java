package cn.academy.crafting;

/**
 * Les quatre usages du formeur de metal.
 *
 * Portage de {@code TileMetalFormer.Mode}. Une recette appartient a un seul mode :
 * la meme entree transformee en INCISE et en ETCH donne deux resultats differents,
 * c'est le mode qui tranche.
 *
 * L'original associait aussi a chaque mode une texture d'icone
 * ({@code guis/icons/icon_former_<mode>.png}). Le port n'a pas ces icones : l'ecran
 * affiche le nom du mode en toutes lettres.
 */
public enum MetalFormerMode {

    PLATE("plate"),
    INCISE("incise"),
    ETCH("etch"),
    REFINE("refine");

    private final String id;

    MetalFormerMode(String id) {
        this.id = id;
    }

    /** Identifiant en minuscules, utilise pour la cle de langue. */
    public String getId() {
        return id;
    }

    /**
     * Mode suivant ou precedent, en boucle.
     *
     * @param delta +1 pour avancer, -1 pour reculer
     */
    public MetalFormerMode cycle(int delta) {
        MetalFormerMode[] values = values();
        int next = (ordinal() + delta) % values.length;
        if (next < 0) next += values.length;
        return values[next];
    }

    /** Retrouve un mode depuis son rang sauvegarde, en se protegeant d'un NBT abime. */
    public static MetalFormerMode byOrdinal(int ordinal) {
        MetalFormerMode[] values = values();
        if (ordinal < 0 || ordinal >= values.length) return PLATE;
        return values[ordinal];
    }
}
