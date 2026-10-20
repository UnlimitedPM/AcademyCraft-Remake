package cn.academy.terminal.client;

import cn.academy.AcademyCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * La page du terminal reste affichee pendant qu'on ecrit dans le chat.
 *
 * <p>Le joueur : « quand on ouvre le chat pour parler, ca ne doit pas refermer le terminal ».
 * Un ecran de Minecraft remplace le precedent, donc le chat recouvrait la page et il fallait la
 * rouvrir. La page est donc tenue en vie : elle est redessinee ici, en calque, tant que le chat
 * est ouvert, et {@link TerminalKeyBindings} la remet en main a sa fermeture (defilement et
 * curseur compris, puisque c'est la MEME instance).
 *
 * <p>C'est exactement ce que faisait l'original, dont le terminal etait un {@code AuxGui} pose
 * sur le jeu : ouvrir le chat ne l'eteignait pas.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class TerminalGhostOverlay {

    private TerminalGhostOverlay() {
    }

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;

        TerminalScreen page = TerminalScreen.ghost();
        if (page == null) return;

        // La page ne sert que sous le chat : le terminal ouvert la dessine lui-meme, et tout
        // autre ecran doit rester seul.
        if (!(Minecraft.getInstance().screen instanceof ChatScreen)) return;

        page.renderPage(event.getGuiGraphics());
    }
}
