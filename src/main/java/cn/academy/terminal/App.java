package cn.academy.terminal;

import cn.academy.AcademyCraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Une application du terminal de donnees.
 *
 * Portage de {@code cn.academy.terminal.App}. Une application porte un nom
 * technique — c'est lui qui nomme ses ressources et ses cles de langue — et un
 * identifiant attribue par {@link AppRegistry} au moment de l'enregistrement.
 * L'identifiant sert au reseau : la donnee du joueur ne retient que des noms.
 *
 * <h2>Pourquoi il n'y a pas d'ecran ici</h2>
 *
 * L'original exposait {@code createEnvironment()} et laissait chaque application
 * empiler elle-meme ses fenetres. Reprendre cela obligerait cette classe — chargee
 * des deux cotes, un serveur dedie compris — a nommer un type client dans sa
 * signature. L'ecran est donc range a part, dans
 * {@code terminal.client.AppScreens}, ou il n'existe que du cote client. Une
 * application dont l'ecran n'est pas enregistre n'est pas cliquable dans le
 * terminal, ce qui est exactement le cas de celles qui n'ont pas encore de contenu.
 *
 * L'icone reste une texture sous {@code guis/apps/<nom>/}. L'original la
 * dimensionnait dans son fichier XML ; ici {@link #getIconSize()} donne sa taille
 * en pixels, parce que les fichiers recopies n'ont pas tous la meme (110, 128...).
 */
public abstract class App {

    private final String name;
    private int appId = -1;
    private boolean preInstalled;

    protected App(String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    public final int getAppId() {
        return appId;
    }

    final void setAppId(int id) {
        this.appId = id;
    }

    /** Cle de langue du nom affiche, comme {@code ac.app.<nom>.name} dans l'original. */
    public final String getDisplayKey() {
        return "ac.app." + name + ".name";
    }

    public final Component getDisplayName() {
        return Component.translatable(getDisplayKey());
    }

    /**
     * Marque l'application comme installee d'office.
     *
     * L'original en prevoyait trois (A propos, MisakaCloud, Reglages) : elles ne
     * servent a rien tant qu'on n'a pas le terminal, mais elles n'ont pas a etre
     * installees avec un objet d'installation qui n'existe pas.
     */
    public final App setPreInstalled() {
        preInstalled = true;
        return this;
    }

    public final boolean isPreInstalled() {
        return preInstalled;
    }

    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "textures/guis/apps/" + name + "/icon.png");
    }

    /** Taille en pixels du fichier d'icone, qui varie d'une application a l'autre. */
    public int getIconSize() {
        return 110;
    }
}
