package cn.academy.misc.media;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Les morceaux qu'un joueur possede.
 *
 * <p>Portage de {@code MediaAcquireData}. L'original tenait un {@code BitSet} range par
 * <b>index</b> dans la liste des morceaux : un morceau insere au milieu aurait decale tous
 * les suivants, et un morceau retire aurait donne a un joueur celui de son voisin. Le port
 * range des <b>noms</b>, comme il le fait partout ailleurs — c'est la meme raison, et elle
 * n'a pas change.
 *
 * <p>Rien ici ne connait Minecraft : la classe se relit en JUnit, ou aucun registre
 * n'existe.
 */
public class MediaAcquireData {

    private static final String TAG_ACQUIRED = "acquired";

    private final Set<String> acquired = new LinkedHashSet<>();

    /** Installe un morceau. Rend vrai si cela a change quelque chose. */
    public boolean install(Media media) {
        return media != null && acquired.add(media.id());
    }

    /** Installe un morceau par son identifiant, s'il existe. */
    public boolean install(String id) {
        return install(MediaManager.get(id));
    }

    /** Si le joueur possede ce morceau. */
    public boolean isInstalled(Media media) {
        return media != null && acquired.contains(media.id());
    }

    public boolean isInstalled(String id) {
        return id != null && acquired.contains(id);
    }

    /** Les morceaux possedes, dans l'ordre de la liste des morceaux livres. */
    public List<Media> installed() {
        List<Media> out = new ArrayList<>();
        for (Media media : MediaManager.internalMedias()) {
            if (acquired.contains(media.id())) out.add(media);
        }
        return out;
    }

    /** Combien de morceaux sont possedes. */
    public int count() {
        return acquired.size();
    }

    public void clear() {
        acquired.clear();
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (String id : acquired) {
            list.add(StringTag.valueOf(id));
        }
        tag.put(TAG_ACQUIRED, list);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        acquired.clear();
        if (tag == null || !tag.contains(TAG_ACQUIRED, Tag.TAG_LIST)) return;

        ListTag list = tag.getList(TAG_ACQUIRED, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            String id = list.getString(i);
            // Un morceau qui n'existe plus — une version anterieure, un contenu retire —
            // est simplement ignore : mieux vaut un morceau en moins qu'une donnee qui
            // empeche le reste de se lire.
            if (MediaManager.get(id) != null) acquired.add(id);
        }
    }
}
