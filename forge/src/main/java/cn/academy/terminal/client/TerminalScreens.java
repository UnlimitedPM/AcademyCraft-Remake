package cn.academy.terminal.client;

import cn.academy.ability.preset.client.PresetEditScreen;
import cn.academy.terminal.app.AppAbout;
import cn.academy.terminal.app.AppSettings;
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
        // Les reglages n'ont pas d'ecran a eux : leur matiere, ce sont les prereglages de
        // touches, et l'ecran qui les regle est le meme que celui de la touche dediee.
        AppScreens.register(AppSettings.INSTANCE, cn.academy.client.gui.SettingsScreen::new);
        AppScreens.register(cn.academy.terminal.app.AppMediaPlayer.INSTANCE, MediaPlayerScreen::new);
        // L'Emetteur de frequence n'a pas de page : il rend le jeu et arme son mode.
        AppScreens.registerAction(cn.academy.terminal.app.AppFreqTransmitter.INSTANCE,
                FreqTransmitterClient::start);

        // Le seul chemin neutre vers un ecran, pour ce qui n'est pas le terminal :
        // l'objet MisakaCloud ouvre le tutoriel sans passer par la grille.
        cn.academy.terminal.App.setOpener(AppScreens::open);
    }
}
