package cn.academy.terminal.app;

import cn.academy.AcademyCraft;
import cn.academy.terminal.App;
import net.minecraft.resources.ResourceLocation;

/**
 * L'application « Lecteur media » : la musique du mod.
 *
 * <p>Portage de {@code MediaApp}, l'objet Scala de l'original — qui n'avait d'application
 * que ce qu'il en fallait : un nom, une icone, et un ecran ouvert au demarrage. Le port
 * garde exactement cela.
 *
 * <p>Elle s'installe avec un objet, comme les autres : l'original faisait de
 * {@code app_media_player} un objet a fabriquer, et un joueur qui n'a pas encore de disque
 * n'a rien a faire de la liste.
 */
public final class AppMediaPlayer extends App {

    public static final AppMediaPlayer INSTANCE = new AppMediaPlayer();

    private AppMediaPlayer() {
        super("media_player");
    }

    @Override
    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                "textures/guis/apps/media_player/icon.png");
    }

    @Override
    public int getIconSize() {
        return 110;
    }
}
