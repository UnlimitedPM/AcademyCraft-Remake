package cn.academy.client.hud;

import cn.academy.client.gui.CustomizeUiLayout;
import cn.academy.misc.media.Media;
import cn.academy.misc.media.client.ClientMediaPlayer;
import cn.academy.misc.media.client.MediaLengths;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Le morceau en cours : le <i>Media Player</i> de l'original.
 *
 * <p>Portage de {@code MediaAuxGui} : sa mise en page vient de son
 * {@code media_player_aux.xml}, celui-la meme qui a servi a l'apercu de l'ecran de reglage —
 * donc les mesures sont partagees avec {@link CustomizeUiLayout} et le HUD ne peut pas
 * diverger de ce que le joueur a sous les yeux dans <i>Customize UI</i>.
 *
 * <p>Ce qu'il montre : le titre du morceau, la position ecoulee en {@code mm:ss}, et la barre
 * du morceau — son fond, puis la part jouee. Rien quand aucun morceau ne tourne, comme chez
 * l'original, dont l'element ne s'affichait que si {@code currentPlaying} avait une valeur.
 *
 * <p>La duree du morceau se lit dans le fichier Ogg lui-meme (voir
 * {@code cn.academy.misc.media.OggDuration}) : c'est ce que l'original faisait avec un decodeur
 * embarque, et ce que le port fait sans decodeur. Quand elle reste introuvable, la barre reste
 * vide et la position continue de defiler : un morceau s'entend meme si on ignore sa longueur.
 */
public final class MediaHud {

    /** Le blanc des deux lignes, comme le xml du lecteur. */
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private MediaHud() {}

    /** Dessine le lecteur, ou l'ecran et la config le posent, si un morceau tourne. */
    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        Media media = ClientMediaPlayer.current();
        if (media == null) return;

        float length = MediaLengths.of(media);
        float elapsed = ClientMediaPlayer.elapsedSeconds();

        // Le morceau est fini : le jeu a lache son son, et le HUD ne doit pas rester la. C'est
        // aussi ce que faisait l'original, en lisant l'etat de son lecteur a chaque image.
        if (length > 0f && elapsed > length) {
            ClientMediaPlayer.stop();
            return;
        }

        HudElement element = HudElement.MEDIA;
        HudLayout layout = HudConfig.read();
        int left = layout.placeX(element, screenWidth, element.getWidth());
        int top = layout.placeY(element, screenHeight, element.getHeight());

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(left, top, 0);

        drawBar(graphics, MediaHudVisuals.progress(elapsed, length));
        drawText(graphics, media, elapsed);

        pose.popPose();
    }

    /**
     * La barre du morceau.
     *
     * <p>Son xml la mesure au <b>dixieme de pixel</b> : le fond gris fait 1,3 de haut et la part
     * jouee 2,2, donc du blanc juste au-dessus et juste en dessous. Le dessin se fait donc dans
     * une pose dix fois plus grande, comme dans l'apercu de l'ecran de reglage.
     */
    private static void drawBar(GuiGraphics graphics, float progress) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.scale(CustomizeUiLayout.MEDIA_BAR_STEP, CustomizeUiLayout.MEDIA_BAR_STEP, 1.0f);

        int unit = Math.round(1.0f / CustomizeUiLayout.MEDIA_BAR_STEP);
        int barX = CustomizeUiLayout.MEDIA_BAR_X * unit;
        int barY = CustomizeUiLayout.MEDIA_BAR_Y * unit;
        int barW = CustomizeUiLayout.MEDIA_BAR_W * unit;
        int grey = CustomizeUiLayout.MEDIA_BAR_GREY_TENTHS;
        int white = CustomizeUiLayout.MEDIA_BAR_WHITE_TENTHS;
        int inset = (white - grey) / 2;

        if (progress > 0f) {
            graphics.fill(barX, barY - inset,
                    barX + Math.round(barW * progress), barY - inset + white,
                    CustomizeUiLayout.MEDIA_BAR_FILL);
        }
        graphics.fill(barX, barY, barX + barW, barY + grey, CustomizeUiLayout.MEDIA_BAR_BACK);
        pose.popPose();
    }

    /** Le titre du morceau, et sa position : deux lignes, chacune calee par son bas. */
    private static void drawText(GuiGraphics graphics, Media media, float elapsed) {
        // Le titre du morceau est une cle de traduction : c'est la langue du joueur qui
        // l'ecrit, comme dans l'original.
        drawLine(graphics,
                net.minecraft.network.chat.Component.translatable(media.titleKey()).getString(),
                CustomizeUiLayout.MEDIA_TITLE_X, CustomizeUiLayout.MEDIA_TITLE_BOTTOM,
                CustomizeUiLayout.plainFontScale(CustomizeUiLayout.MEDIA_TITLE_FONT));
        drawLine(graphics, MediaHudVisuals.formatTime(elapsed),
                CustomizeUiLayout.MEDIA_TIME_X, CustomizeUiLayout.MEDIA_TIME_BOTTOM,
                CustomizeUiLayout.plainFontScale(CustomizeUiLayout.MEDIA_TIME_FONT));
    }

    /**
     * Une ligne du lecteur.
     *
     * <p>Le bas de la boite est retire avec la hauteur de ligne de la <b>police</b>, et non
     * celle du xml : c'est celle-la qui est dessinee, et l'ecart se voit (le titre tombait sur
     * la barre).
     */
    private static void drawLine(GuiGraphics graphics, String text, int x, float bottom, float scale) {
        var font = Minecraft.getInstance().font;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, Math.round(bottom - font.lineHeight), 0);
        pose.scale(scale, scale, 1.0f);
        graphics.drawString(font, text, 0, 0, TEXT_COLOR, false);
        pose.popPose();
    }
}
