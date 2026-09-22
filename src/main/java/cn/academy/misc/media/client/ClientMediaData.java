package cn.academy.misc.media.client;

import cn.academy.misc.media.Media;
import cn.academy.misc.media.MediaAcquireData;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Ce que le client sait des morceaux du joueur.
 *
 * <p>Le serveur decide, le client affiche : cette classe ne fait que garder la derniere
 * liste recue, et la relire pour l'ecran du lecteur. Elle passe par la <b>meme</b> classe de
 * donnee que le serveur — {@link MediaAcquireData} — pour que la lecture d'un sac de NBT
 * n'existe qu'une fois dans le mod.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientMediaData {

    private static final MediaAcquireData DATA = new MediaAcquireData();

    private ClientMediaData() {}

    /** Remplace la liste par celle que le serveur vient d'envoyer. */
    public static void update(CompoundTag tag) {
        DATA.deserializeNBT(tag);
    }

    public static MediaAcquireData get() {
        return DATA;
    }

    /** Les morceaux que le joueur possede, dans l'ordre de la liste des morceaux livres. */
    public static List<Media> installed() {
        return DATA.installed();
    }

    public static boolean isInstalled(Media media) {
        return DATA.isInstalled(media);
    }
}
