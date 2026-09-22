package cn.academy.ability.teleporter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Un endroit ou l'on a marque son passage : le portage de {@code Location} de l'original.
 *
 * <p>Un nom, une dimension, et trois coordonnees. La dimension est gardee sous forme de
 * <b>texte</b> — {@code minecraft:overworld} — et non comme une cle de registre : c'est ce qui
 * permet de sauvegarder, de relire et de comparer une marque sans avoir le moindre registre
 * sous la main, donc aussi en test unitaire. La comparaison avec le monde du joueur se fait
 * sur le meme texte, ce qui revient au meme sans rien demander au jeu.
 *
 * <p>Le rang dans la liste <b>est</b> l'identifiant, comme dans l'original : oublier une
 * marque renumerote celles qui suivent, ce qui evite de retenir des numeros pour toujours.
 */
public record LocationMark(String name, String dimension, double x, double y, double z) {

    /** Longueur maximale d'un nom, pour qu'il tienne sur une ligne d'ecran. */
    public static final int MAX_NAME = 24;

    /** La dimension d'un monde, sous la forme qui se sauvegarde. */
    public static String of(Level level) {
        return level.dimension().location().toString();
    }

    /** Nom donne a une marque quand le joueur n'en a pas saisi. */
    public static String defaultName(int index) {
        return "Point " + (index + 1);
    }

    /**
     * Un nom propre : debarrasse des espaces qui l'entourent, borne, et jamais vide.
     *
     * Le nom vient du client, donc il n'y a aucune raison de lui faire confiance : un nom de
     * mille caracteres casserait l'affichage, et un nom vide rendrait la marque introuvable
     * dans sa propre liste.
     */
    public static String cleanName(String raw, int index) {
        if (raw == null) return defaultName(index);
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return defaultName(index);
        return trimmed.length() <= MAX_NAME ? trimmed : trimmed.substring(0, MAX_NAME);
    }

    /** La dimension sous sa forme courte, pour l'affichage : {@code the_nether}. */
    public String dimensionName() {
        ResourceLocation id = ResourceLocation.tryParse(dimension);
        if (id == null) return dimension;
        return id.getNamespace().equals("minecraft") ? id.getPath() : id.toString();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        tag.putString("dim", dimension);
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        return tag;
    }

    /**
     * Relit une marque sauvegardee, ou rend {@code null} si son texte de dimension n'est
     * meme pas un identifiant.
     *
     * <p>Une dimension qu'un mod a emportee n'est pas detectable ici — il faudrait le registre
     * du serveur — donc la marque est conservee : c'est le saut qui refusera de l'atteindre,
     * en le disant.
     */
    public static LocationMark load(CompoundTag tag) {
        String dimension = tag.getString("dim");
        if (ResourceLocation.tryParse(dimension) == null) return null;
        return new LocationMark(cleanName(tag.getString("name"), 0), dimension,
                tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"));
    }
}
