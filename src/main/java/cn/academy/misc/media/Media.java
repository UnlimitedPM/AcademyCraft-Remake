package cn.academy.misc.media;

import cn.academy.AcademyCraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Un morceau du lecteur media.
 *
 * <p>L'original en faisait un objet Scala, avec son titre, sa description, sa pochette et
 * son URL — parce qu'il savait lire des fichiers que le joueur deposait dans un dossier.
 * Le port ne garde que ce qui se lit sans decoder un OGG : un <b>identifiant</b>, qui est
 * aussi le nom du fichier, et le fait qu'il soit livre avec le mod ou non.
 *
 * <p>Le titre et la description restent des cles de traduction : c'est la langue du joueur
 * qui les ecrit, comme dans l'original.
 */
public record Media(String id, boolean internal) {

    public Media(String id) {
        this(id, true);
    }

    /** Le nom du morceau, traduit dans la langue du joueur. */
    public String titleKey() {
        return "ac.media." + id + ".name";
    }

    /** Ce que le morceau est, en une ligne. */
    public String descKey() {
        return "ac.media." + id + ".desc";
    }

    /**
     * Le son du morceau, tel qu'il est range dans les ressources.
     *
     * <p>Le fichier vit dans {@code assets/academy/sounds/media/<id>.ogg} : la 1.20.1
     * resout un chemin de son sans declaration des lors qu'il suit celui du fichier, donc
     * aucun fichier de description n'est necessaire.
     */
    public ResourceLocation sound() {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "media/" + id);
    }

    /** L'objet qui donne ce morceau, tel qu'il est enregistre. */
    public String itemName() {
        return "media_" + id;
    }
}
