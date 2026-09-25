package cn.academy.misc.media.client;

import cn.academy.AcademyCraft;
import cn.academy.misc.media.Media;
import cn.academy.misc.media.OggDuration;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * La duree des morceaux, lue une fois chacune.
 *
 * <p>Le fichier vit dans les ressources du mod ({@code assets/academy/sounds/media/<id>.ogg}) :
 * c'est le meme que celui que le jeu joue, donc la duree affichee est celle du morceau entendu,
 * sans table a tenir a jour. La lecture se fait une seule fois par morceau, et son echec rend
 * <b>0</b> plutot qu'une exception : un morceau sans duree s'affiche sans barre.
 */
@OnlyIn(Dist.CLIENT)
public final class MediaLengths {

    private static final Map<String, Float> CACHE = new HashMap<>();

    private MediaLengths() {}

    /** La duree du morceau, en secondes, ou 0 si elle est introuvable. */
    public static float of(Media media) {
        if (media == null) return 0f;
        return CACHE.computeIfAbsent(media.id(), id -> read(id));
    }

    private static float read(String id) {
        // Le meme chemin que les autres ressources du port (voir AboutDocument) : le fichier
        // se lit par le classpath, ce qui marche aussi bien dans le jeu qu'en test.
        String path = "/assets/" + AcademyCraft.MOD_ID + "/sounds/media/" + id + ".ogg";
        try (InputStream stream = MediaLengths.class.getResourceAsStream(path)) {
            if (stream == null) return 0f;
            return OggDuration.seconds(stream.readAllBytes());
        } catch (Exception e) {
            // Une ressource absente ou illisible ne doit pas empecher le HUD de s'afficher.
            return 0f;
        }
    }
}
