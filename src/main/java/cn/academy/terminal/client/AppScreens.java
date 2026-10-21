package cn.academy.terminal.client;

import cn.academy.terminal.App;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Les ecrans des applications, cote client.
 *
 * Cette table existe pour une raison precise : {@link App} est chargee des deux
 * cotes, un serveur dedie compris, et ne doit donc pas nommer un type client. Le
 * lien entre une application et sa page vit ici, ou il n'est construit que sur le
 * client (voir {@code AcademyCraft.ClientModEvents}).
 *
 * Une application sans entree ici existe et s'installe, mais n'est pas cliquable
 * dans le terminal : c'est le cas de celles dont le contenu n'est pas encore porte.
 */
@OnlyIn(Dist.CLIENT)
public final class AppScreens {

    private static final Map<Integer, Supplier<Screen>> FACTORIES = new HashMap<>();

    private AppScreens() {}

    public static void register(App app, Supplier<Screen> factory) {
        FACTORIES.put(app.getAppId(), factory);
    }

    public static boolean isAvailable(App app) {
        return FACTORIES.containsKey(app.getAppId());
    }

    /** L'ecran de l'application, ou null si elle n'en a pas. */
    public static Screen create(App app) {
        Supplier<Screen> factory = FACTORIES.get(app.getAppId());
        return factory == null ? null : factory.get();
    }

    /**
     * Ouvre l'ecran d'une application, s'il existe.
     *
     * Les objets qui ouvrent leur page sans passer par le terminal — l'original avait
     * un objet MisakaCloud qui affichait directement les tutoriels — y arrivent par
     * {@code App.requestOpen}, dont cette methode est l'aboutissement cote client.
     */
    public static void open(App app) {
        Screen page = create(app);
        if (page != null) {
            net.minecraft.client.Minecraft.getInstance().setScreen(page);
        }
    }
    /**
     * L'ecran qui a ouvert l'application : le terminal.
     *
     * <p>Un ecran de Minecraft REMPLACE le precedent, il n'y a donc pas de « dessous » : on garde
     * l'instance pour la REPOSER a la fermeture. Ouvrir une application ne fermait pas le
     * terminal, mais le rendre au jeu a la sortie le faisait disparaitre — le joueur : « quand on
     * ouvre une app, ce n'est pas cense fermer le data terminal derriere, il doit rester ouvert ».
     */
    private static Screen origin;

    /** Ouvre une application depuis un ecran, qui revient a sa fermeture. */
    public static void open(App app, Screen from) {
        origin = from;
        open(app);
    }

    /**
     * Ferme une application : le terminal reprend la main, s'il en venait.
     *
     * <p>Et la pause du lecteur se repose <b>tout de suite</b> apres : rendre le jeu ou le terminal
     * remet en marche tous les canaux du moteur de son, celui du mod compris. Le tick le rattrape,
     * mais avec un trou de quelques dizaines de millisecondes, et le joueur l'entend — « le son
     * reprend pendant une micro seconde ».
     */
    public static void close() {
        Screen target = origin;
        origin = null;
        net.minecraft.client.Minecraft.getInstance().setScreen(target);
        cn.academy.misc.media.client.ClientMediaPlayer.reassertPause();
    }}
