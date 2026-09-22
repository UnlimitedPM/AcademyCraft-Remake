package cn.academy.terminal.tutorial.client;

import cn.academy.terminal.tutorial.TutorialData;
import net.minecraft.nbt.CompoundTag;

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
        DATA.deserializeNBT(tag);
    }

    public static TutorialData get() {
        return DATA;
    }
}
