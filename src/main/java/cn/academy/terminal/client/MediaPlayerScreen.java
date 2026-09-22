package cn.academy.terminal.client;

import cn.academy.misc.media.Media;
import cn.academy.misc.media.MediaManager;
import cn.academy.misc.media.client.ClientMediaData;
import cn.academy.misc.media.client.ClientMediaPlayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * L'ecran du lecteur media : la liste des morceaux, et ce qui tourne.
 *
 * <p>Portage de {@code MediaGui}, reduit a ce que le port sait faire. L'original affichait
 * une pochette, une barre de progression et un temps ecoule, qu'il tenait d'un decodeur OGG
 * — JOrbis, embarque pour cela. Le port n'embarque aucun decodeur : il joue le fichier par
 * le moteur de son du jeu, et ne sait donc pas combien de temps il dure. Il reste l'essentiel
 * — la liste, ce qui est possede, ce qui tourne, et le clic qui bascule.
 *
 * <p>Comme l'original, fermer l'ecran <b>n'arrete pas</b> la musique : c'est le client qui la
 * joue, et elle continue tant qu'on ne la coupe pas.
 */
@OnlyIn(Dist.CLIENT)
public class MediaPlayerScreen extends Screen {

    private static final int MARGIN = 16;
    private static final int HEADER = 24;
    private static final int ROW = 20;

    private static final int PANEL = 0xF0181824;
    private static final int PANEL_EDGE = 0xFF6FA8DC;
    private static final int TEXT = 0xFFE0E0E0;
    private static final int DIM = 0xFF909090;
    private static final int LOCKED = 0xFF606060;
    private static final int PLAYING = 0xFF7FD37F;
    private static final int SELECTED = 0xFF2A3A50;

    private final List<Media> medias = MediaManager.internalMedias();

    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;

    /** Le rang survole, ou -1. */
    private int hovered = -1;

    public MediaPlayerScreen() {
        super(Component.translatable("ac.app.media_player.name"));
    }

    @Override
    protected void init() {
        panelWidth = width - 2 * MARGIN;
        panelHeight = height - 2 * MARGIN;
        leftPos = MARGIN;
        topPos = MARGIN;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        graphics.fill(leftPos, topPos, leftPos + panelWidth, topPos + panelHeight, PANEL);
        graphics.renderOutline(leftPos, topPos, panelWidth, panelHeight, PANEL_EDGE);

        graphics.drawString(font, title, leftPos + 8, topPos + 6, TEXT, false);
        graphics.drawString(font, Component.translatable("ac.media.player.hint"),
                leftPos + 8, topPos + HEADER - 8, DIM, false);

        hovered = -1;
        int y = topPos + HEADER + 8;
        for (Media media : medias) {
            boolean owned = ClientMediaData.isInstalled(media);
            boolean playing = ClientMediaPlayer.isPlaying(media);
            boolean over = mouseX >= leftPos + 6 && mouseX <= leftPos + panelWidth - 6
                    && mouseY >= y && mouseY < y + ROW;

            if (over && owned) {
                hovered = medias.indexOf(media);
                graphics.fill(leftPos + 6, y, leftPos + panelWidth - 6, y + ROW, SELECTED);
            }

            int color = !owned ? LOCKED : playing ? PLAYING : TEXT;
            graphics.drawString(font, Component.translatable(media.titleKey()),
                    leftPos + 12, y + 6, color, false);

            Component state = !owned
                    ? Component.translatable("ac.media.player.locked")
                    : playing
                            ? Component.translatable("ac.media.player.playing")
                            : Component.translatable("ac.media.player.owned");
            graphics.drawString(font, state,
                    leftPos + panelWidth - 12 - font.width(state), y + 6, color, false);

            y += ROW;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hovered >= 0) {
            ClientMediaPlayer.toggle(medias.get(hovered));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        // La musique continue quand on ferme le terminal : le jeu ne doit donc pas se
        // mettre en pause pour autant, exactement comme l'original.
        return false;
    }
}
