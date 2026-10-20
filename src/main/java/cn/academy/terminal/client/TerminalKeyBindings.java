package cn.academy.terminal.client;

import cn.academy.AcademyCraft;
import cn.academy.terminal.TerminalEvents;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Ouverture du terminal par une touche.
 *
 * L'original utilisait Alt gauche ({@code open_data_terminal}) et affichait un
 * overlay permanent. Ici la meme touche ouvre une page, et le terminal ne s'ouvre
 * que s'il est installe : c'est le seul controle d'acces, comme dans l'original ou
 * l'overlay refusait de s'afficher sans la donnee.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public class TerminalKeyBindings {

    public static final KeyMapping OPEN_TERMINAL = new KeyMapping(
            "key.academy.open_terminal", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_LALT, "key.categories.academy");

    @Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Registration {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(OPEN_TERMINAL);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // La page revient une fois le chat totalement ferme (voir TerminalScreen.requestResume).
        var pending = TerminalScreen.takeResume();
        if (pending != null) {
            var client = net.minecraft.client.Minecraft.getInstance();
            if (client.screen == null) client.setScreen(pending);
        }

        if (!OPEN_TERMINAL.consumeClick()) return;

        var minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        if (!ClientTerminalData.get().isTerminalInstalled()) return;

        minecraft.setScreen(new TerminalScreen());
    }

    /**
     * Le terminal reprend la main quand le chat qu'il a laisse s'ouvrir se referme.
     *
     * <p>La page n'est pas fermee : elle est seulement recouverte ({@link TerminalGhostOverlay}
     * la dessine pendant la saisie). C'est la MEME instance qui revient, donc le defilement et le
     * curseur restent ou ils etaient.
     */
    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (!(event.getScreen() instanceof net.minecraft.client.gui.screens.ChatScreen)) return;
        if (TerminalScreen.ghost() == null) return;

        TerminalScreen.requestResume();
    }
}
