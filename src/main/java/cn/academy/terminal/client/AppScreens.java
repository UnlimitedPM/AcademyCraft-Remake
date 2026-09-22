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
}
