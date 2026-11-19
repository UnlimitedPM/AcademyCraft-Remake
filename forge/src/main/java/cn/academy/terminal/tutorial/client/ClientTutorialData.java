package cn.academy.terminal.tutorial.client;

import cn.academy.terminal.tutorial.TutorialData;
import cn.academy.terminal.tutorial.TutorialLibrary;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Cache client de {@link TutorialData}, tenu a jour par paquets.
 *
 * <p>L'ecran des tutoriels a besoin de savoir lesquels sont ouverts pour les colorer ; il
 * n'a pas la capacite, et seule la copie envoyee par le serveur fait foi. C'est ce qui
 * permet de n'ouvrir qu'une fois : le client ne devine rien, il lit.
 */
public final class ClientTutorialData {

    private static final TutorialData DATA = new TutorialData();

    private ClientTutorialData() {}

    public static void update(CompoundTag tag) {
        // Ce que le client connaissait avant la mise a jour : ce qui s'ajoute est nouveau, et
        // c'est exactement ce que l'original annoncait par une notification
        // (`TutorialActivatedEvent` -> NotifyUI). Une seule notification a la fois, comme chez
        // lui : la derniere arrivee remplace celle qui passait.
        Set<String> before = DATA.getUnlocked();
        DATA.deserializeNBT(tag);
        for (String id : DATA.getUnlocked()) {
            if (!before.contains(id)) notify(id);
        }
    }

    /** Annonce un tutoriel qui vient de s'ouvrir, avec son titre. */
    private static void notify(String id) {
        String title = TutorialLibrary.load(id, language()).title();
        cn.academy.client.hud.NotificationHud.show(
                ResourceLocation.fromNamespaceAndPath(cn.academy.AcademyCraft.MOD_ID,
                        "textures/tutorial/update_notify.png"),
                Component.translatable("ac.tutorial.update"),
                Component.literal(title));
    }

    /** La langue demandee au jeu, comme l'ecran des tutoriels. */
    private static String language() {
        return net.minecraft.client.Minecraft.getInstance().getLanguageManager().getSelected();
    }

    public static TutorialData get() {
        return DATA;
    }
}
