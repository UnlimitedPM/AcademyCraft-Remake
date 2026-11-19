package cn.academy.terminal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Etat du terminal de donnees d'un joueur : le terminal est-il installe, et
 * quelles applications le sont.
 *
 * Portage de {@code TerminalData} (qui etait un {@code DataPart} de LambdaLib).
 *
 * <h2>Une deviation assumee : des noms, pas des hachages</h2>
 *
 * L'original retenait {@code app.getName().hashCode()} — un entier par
 * application. Deux noms peuvent partager un hachage, et le joueur se
 * retrouverait alors avec une application qu'il n'a jamais installee. Le nom
 * lui-meme n'est pas plus gros a sauvegarder et ne pose pas la question. C'est le
 * seul ecart de comportement de cette classe, et il ne se voit pas en jeu.
 */
public class TerminalData implements INBTSerializable<CompoundTag> {

    private static final String TAG_INSTALLED = "installed";
    private static final String TAG_APPS = "apps";

    private boolean installed;
    private final Set<String> installedApps = new LinkedHashSet<>();

    public boolean isTerminalInstalled() {
        return installed;
    }

    public boolean isInstalled(App app) {
        return app.isPreInstalled() || installedApps.contains(app.getName());
    }

    /**
     * Les applications installees, dans l'ordre du registre.
     *
     * Le registre est passe en parametre au lieu d'etre lu du singleton : la donnee
     * du joueur reste ainsi une donnee, testable sans avoir a peupler le registre du
     * jeu.
     */
    public List<App> getInstalledApps(AppRegistry registry) {
        List<App> out = new ArrayList<>();
        for (App app : registry.all()) {
            if (isInstalled(app)) out.add(app);
        }
        return out;
    }

    /** Le nombre d'applications installees par un objet, hors applications d'origine. */
    public int getInstalledCount() {
        return installedApps.size();
    }

    /** Installe le terminal. Retourne vrai si cela a change quelque chose. */
    public boolean install() {
        if (installed) return false;
        installed = true;
        return true;
    }

    /** Installe une application. Retourne vrai si cela a change quelque chose. */
    public boolean installApp(App app) {
        if (isInstalled(app)) return false;
        return installedApps.add(app.getName());
    }

    /**
     * Oublie tout.
     *
     * Sert au port : sans cela, il fallait effacer le NBT du joueur a la main
     * pour retester l'installation d'un terminal.
     */
    public void reset() {
        installed = false;
        installedApps.clear();
    }

    public void copyFrom(TerminalData other) {
        installed = other.installed;
        installedApps.clear();
        installedApps.addAll(other.installedApps);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG_INSTALLED, installed);
        ListTag list = new ListTag();
        for (String name : installedApps) {
            list.add(StringTag.valueOf(name));
        }
        tag.put(TAG_APPS, list);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        installed = tag.getBoolean(TAG_INSTALLED);
        installedApps.clear();
        ListTag list = tag.getList(TAG_APPS, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            installedApps.add(list.getString(i));
        }
    }
}
