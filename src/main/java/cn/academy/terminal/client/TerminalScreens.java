package cn.academy.terminal.client;

import cn.academy.terminal.app.AppAbout;
import cn.academy.terminal.app.AppSkillTree;
import cn.academy.terminal.app.AppTutorial;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Enregistrement des ecrans d'applications.
 *
 * Appele depuis {@code AcademyCraft.ClientModEvents.onClientSetup}. Une application
 * portee s'ajoute ici, une ligne par application.
 */
@OnlyIn(Dist.CLIENT)
public final class TerminalScreens {

    private TerminalScreens() {}

    public static void init() {
        AppScreens.register(AppAbout.INSTANCE, AboutScreen::new);
        AppScreens.register(AppSkillTree.INSTANCE, SkillTreeScreen::new);
        AppScreens.register(AppTutorial.INSTANCE, TutorialScreen::new);

        // Le seul chemin neutre vers un ecran, pour ce qui n'est pas le terminal :
        // l'objet MisakaCloud ouvre le tutoriel sans passer par la grille.
        cn.academy.terminal.App.setOpener(AppScreens::open);
    }
}
