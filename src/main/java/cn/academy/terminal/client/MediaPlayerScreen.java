package cn.academy.terminal.client;

import cn.academy.AcademyCraft;
import cn.academy.client.gui.AcademyFonts;
import cn.academy.misc.media.Media;
import cn.academy.misc.media.MediaManager;
import cn.academy.misc.media.client.ClientMediaPlayer;
import cn.academy.misc.media.client.MediaLengths;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * L'ecran du lecteur media : la carte de l'original.
 *
 * <p>Portage de {@code MediaGui}, sur son document {@code guis/media_player.xml} : une page de
 * <b>650 sur 504</b> que le document dessine a <b>0,32</b> — donc 208 sur 161 a l'ecran. Le
 * port dessinait un panneau plein ecran de son invention ; tout ce qui suit vient du document,
 * en unites de page :
 *
 * <ul>
 *   <li>le bouton lecture (50 sur 42 a 51,85 ; 71,79) et le bouton arret a cote ;</li>
 *   <li>le titre du morceau en cours, boite de 275,58 calee a droite, en 36 ;</li>
 *   <li>la barre de volume de 128 sur 32 a 179,69 ; 77, avec son curseur qui glisse de 186 a
 *       298 ;</li>
 *   <li>la barre de progression de 554 sur 6 a 52 ; 120, et le temps ecoule a droite ;</li>
 *   <li>la liste : une fenetre de 552 sur 302 a 51 ; 169, des rangees de 554 sur 60 — la
 *       pochette de 50, le titre en 35, la description en 27 et la duree a droite — et la
 *       glissiere de 5 sur 55 qui les fait defiler.</li>
 * </ul>
 *
 * <p>L'original separait lecture et pause ; le moteur de son du jeu ne sait pas reprendre un
 * son a une position, donc le port <b>rejoue le morceau muet</b> depuis le debut et rend le
 * volume a la position de la pause : la lecture reprend exactement ou elle s'etait arretee
 * (voir {@code ClientMediaPlayer}). L'icone montre une pause pendant la lecture, comme chez
 * lui.
 *
 * <p>Comme l'original, fermer l'ecran <b>n'arrete pas</b> la musique, et le jeu ne se met pas
 * en pause : c'est le client qui joue le morceau.
 */
@OnlyIn(Dist.CLIENT)
public class MediaPlayerScreen extends Screen {

    // ------------------------------------------------------------------
    // Le document `guis/media_player.xml`, en unites de page.
    // ------------------------------------------------------------------

    private static final double PAGE_W = 650.0;
    private static final double PAGE_H = 504.0;
    /** L'echelle que le document donne a sa page : 650 sur 504 devient 208 sur 161. */
    private static final double PAGE_SCALE = 0.32;

    /** Les deux boutons, cote a cote sous le titre. */
    private static final double BTN_W = 50.0;
    private static final double BTN_H = 42.0;
    private static final double BTN_Y = 71.79296296296297;
    private static final double POP_X = 51.85185185185185;
    private static final double STOP_X = 114.81481481481481;

    /** Le titre du morceau en cours : largeur du document, cale a droite moins cinquante. */
    private static final double TITLE_W = 275.5787037037038;
    private static final double TITLE_X = PAGE_W - TITLE_W - 50.0;
    private static final double TITLE_Y = 77.81148148148147;
    private static final double TITLE_H = 30.0;

    /** Le volume : son fond, puis son curseur, qui glisse de son bord a l'autre. */
    private static final double VOLUME_X = 179.6875;
    private static final double VOLUME_Y = 77.0;
    private static final double VOLUME_W = 128.0;
    private static final double VOLUME_H = 32.0;
    private static final double KNOB_W = 9.375;
    private static final double KNOB_H = 30.0;
    private static final double KNOB_Y = 77.875;
    private static final double KNOB_LOWER = 186.0;
    private static final double KNOB_UPPER = 298.0;

    /** La barre du morceau, et le temps ecoule a sa droite. */
    private static final double PROGRESS_X = 52.0;
    private static final double PROGRESS_Y = 120.0;
    private static final double PROGRESS_W = 554.0;
    private static final double PROGRESS_H = 6.0;
    private static final double TIME_X = 403.7037037037038;
    private static final double TIME_Y = 126.76981481481482;
    private static final double TIME_W = 200.0;
    private static final double TIME_H = 30.0;

    /** La liste : sa fenetre, ses rangees, et la glissiere qui les fait defiler. */
    private static final double AREA_X = 51.0;
    private static final double AREA_Y = 169.0;
    private static final double AREA_H = 302.0;
    private static final double ROW_W = 554.0;
    private static final double ROW_H = 60.0;
    /** Cinq rangees tiennent dans les 302 de la fenetre, comme dans son `ElementList`. */
    private static final int VISIBLE_ROWS = 5;
    private static final double SCROLL_X = 604.0;
    private static final double SCROLL_W = 5.0;
    private static final double SCROLL_H = 55.0;
    private static final double SCROLL_LOWER = 169.0;
    private static final double SCROLL_UPPER = 415.0;

    /** Le contenu d'une rangee, en unites locales. */
    private static final double ROW_ICON = 50.0;
    private static final double ROW_ICON_X = 4.0;
    private static final double ROW_ICON_Y = 5.0;
    private static final double ROW_TITLE_X = 65.0;
    private static final double ROW_TITLE_Y = 1.0;
    private static final double ROW_TITLE_W = 300.0;
    private static final double ROW_TITLE_H = 30.0;
    private static final double ROW_DESC_X = 66.125;
    private static final double ROW_DESC_Y = 29.0;
    private static final double ROW_DESC_W = 300.0;
    private static final double ROW_DESC_H = 23.0;
    private static final double ROW_TIME_W = 70.0;
    private static final double ROW_TIME_X = ROW_W - ROW_TIME_W - 6.0;

    /** Les tailles de police du document, bornees par la gravure des glyphes du port. */
    private static final int CELL_TITLE = 33;   // 36
    private static final int CELL_ROW_TITLE = 33;   // 35
    private static final int CELL_ROW_TIME = 28;
    private static final int CELL_ROW_DESC = 27;
    private static final int CELL_TIME = 30;

    /** Les couleurs du document : du blanc, a l'opacite qu'il demande. */
    private static final int TITLE_COLOUR = 0x99FFFFFF;
    private static final int TIME_COLOUR = 0xDDFFFFFF;
    private static final int PROGRESS_COLOUR = 0xFFFFFFFF;
    private static final int ROW_TITLE_COLOUR = 0xBBFFFFFF;
    private static final int ROW_TIME_COLOUR = 0xB9FFFFFF;
    private static final int ROW_DESC_COLOUR = 0xEEFFFFFF;
    private static final int ROW_IDLE = 0x14FFFFFF;
    private static final int ROW_HOVER = 0x3CFFFFFF;
    private static final int KNOB_COLOUR = 0x99FFFFFF;
    private static final int KNOB_HOVER = 0xCCFFFFFF;
    private static final int SCROLL_COLOUR = 0x82FFFFFF;
    private static final int BUTTON_IDLE = 200;
    private static final int BUTTON_HOVER = 255;

    private static final ResourceLocation TEX_BACK = texture("guis/apps/media_player/back.png");
    private static final ResourceLocation TEX_PLAY = texture("guis/apps/media_player/play.png");
    private static final ResourceLocation TEX_PAUSE = texture("guis/apps/media_player/pause.png");
    private static final ResourceLocation TEX_STOP = texture("guis/apps/media_player/stop.png");
    private static final ResourceLocation TEX_VOLUME = texture("guis/icons/volume_overlay.png");
    private static final ResourceLocation TEX_NO_COVER = texture("guis/icons/icon_nomedia.png");

    private final List<Media> medias = MediaManager.internalMedias();

    /** Le morceau lance en dernier : c'est lui que le bouton lecture reprend. */
    private Media lastPlayed;

    /** La premiere rangee montree, quand la liste defile. */
    private int scroll;

    /** Ce que la souris tient : la glissiere de la liste, ou le curseur de volume. */
    private enum Drag { NONE, SCROLL, VOLUME }

    private Drag dragging = Drag.NONE;

    public MediaPlayerScreen() {
        super(Component.translatable("ac.app.media_player.name"));
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "textures/" + path);
    }

    // ------------------------------------------------------------------
    // Rendu
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Le fond des conteneurs du jeu : la carte de l'original flotte dessus, comme
        // l'ecran d'un coffre ou d'une machine.
        renderBackground(graphics);

        double pageX = pageX(mouseX);
        double pageY = pageY(mouseY);

        graphics.pose().pushPose();
        graphics.pose().translate((float) originX(), (float) originY(), 0f);
        graphics.pose().scale((float) PAGE_SCALE, (float) PAGE_SCALE, 1f);

        image(graphics, TEX_BACK, 0, 0, PAGE_W, PAGE_H, 650, 504);
        drawTransport(graphics, pageX, pageY);
        drawVolume(graphics, pageX, pageY);
        drawProgress(graphics);
        drawRows(graphics, pageX, pageY);

        graphics.pose().popPose();
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    /** Le titre du morceau, les deux boutons, et le temps ecoule. */
    private void drawTransport(GuiGraphics graphics, double pageX, double pageY) {
        Media media = ClientMediaPlayer.current();
        boolean playing = media != null;

        // Le titre, cale a droite dans sa boite.
        if (playing) {
            text(graphics, Component.translatable(media.titleKey()).getString(),
                    TITLE_X, TITLE_Y, TITLE_W, CELL_TITLE, TITLE_COLOUR, true);
        }

        // Les deux boutons : lecture ou pause puis arret. L'icone est une pause tant que le
        // morceau avance, une fleche des qu'il est en attente.
        boolean hoverPop = inside(pageX, pageY, POP_X, BTN_Y, BTN_W, BTN_H);
        boolean hoverStop = inside(pageX, pageY, STOP_X, BTN_Y, BTN_W, BTN_H);
        boolean advancing = playing && !ClientMediaPlayer.isPaused();
        imageAlpha(graphics, advancing ? TEX_PAUSE : TEX_PLAY, POP_X, BTN_Y, BTN_W, BTN_H,
                50, 42, hoverPop ? BUTTON_HOVER : BUTTON_IDLE);
        imageAlpha(graphics, TEX_STOP, STOP_X, BTN_Y, BTN_W, BTN_H, 50, 42,
                hoverStop ? BUTTON_HOVER : BUTTON_IDLE);

        // Le temps ecoule, a droite de la barre. Il s'arrete pendant une pause, comme chez
        // l'original, qui dessinait le `playedTime` de son lecteur.
        float elapsed = ClientMediaPlayer.elapsedSeconds();
        text(graphics, format(elapsed), TIME_X, TIME_Y, TIME_W, CELL_TIME, TIME_COLOUR, true);
    }

    /** Le fond du volume et son curseur, a la position du volume courant. */
    private void drawVolume(GuiGraphics graphics, double pageX, double pageY) {
        image(graphics, TEX_VOLUME, VOLUME_X, VOLUME_Y, VOLUME_W, VOLUME_H, 128, 32);

        double knobX = KNOB_LOWER
                + (KNOB_UPPER - KNOB_LOWER) * ClientMediaPlayer.getVolume();
        boolean hovered = inside(pageX, pageY, KNOB_LOWER, KNOB_Y,
                KNOB_UPPER - KNOB_LOWER + KNOB_W, KNOB_H);
        fill(graphics, knobX, KNOB_Y, knobX + KNOB_W, KNOB_Y + KNOB_H,
                hovered || dragging == Drag.VOLUME ? KNOB_HOVER : KNOB_COLOUR);
    }

    /** La part jouee du morceau, en blanc sur la rainure de la carte. */
    private void drawProgress(GuiGraphics graphics) {
        Media media = ClientMediaPlayer.current();
        if (media == null) return;

        float length = MediaLengths.of(media);
        if (length <= 0f) return;

        double part = Math.min(1.0, ClientMediaPlayer.elapsedSeconds() / length);
        fill(graphics, PROGRESS_X, PROGRESS_Y, PROGRESS_X + PROGRESS_W * part,
                PROGRESS_Y + PROGRESS_H, PROGRESS_COLOUR);
    }

    /** La liste des morceaux, puis sa glissiere. */
    private void drawRows(GuiGraphics graphics, double pageX, double pageY) {
        int last = Math.min(medias.size(), scroll + VISIBLE_ROWS);
        for (int index = scroll; index < last; index++) {
            Media media = medias.get(index);
            double top = AREA_Y + (index - scroll) * ROW_H;
            boolean hovered = inside(pageX, pageY, AREA_X, top, ROW_W, ROW_H);

            // La rangee s'eclaire sous la souris : le `Tint` du document, un voile blanc.
            fill(graphics, AREA_X, top, AREA_X + ROW_W, top + ROW_H,
                    hovered ? ROW_HOVER : ROW_IDLE);

            image(graphics, cover(media), AREA_X + ROW_ICON_X, top + ROW_ICON_Y,
                    ROW_ICON, ROW_ICON, 64, 64);

            String name = Component.translatable(media.titleKey()).getString();
            text(graphics, name, AREA_X + ROW_TITLE_X, top + ROW_TITLE_Y, ROW_TITLE_W,
                    CELL_ROW_TITLE, ROW_TITLE_COLOUR, false);

            String desc = Component.translatable(media.descKey()).getString();
            text(graphics, desc, AREA_X + ROW_DESC_X, top + ROW_DESC_Y, ROW_DESC_W,
                    CELL_ROW_DESC, ROW_DESC_COLOUR, false);

            text(graphics, format(MediaLengths.of(media)), AREA_X + ROW_TIME_X, top,
                    ROW_TIME_W, CELL_ROW_TIME, ROW_TIME_COLOUR, true);
        }

        // La glissiere : un curseur de 55 qui glisse de 169 a 415, s'il y a de quoi defiler.
        int max = maxScroll();
        if (max <= 0) return;

        double knobY = SCROLL_LOWER + (SCROLL_UPPER - SCROLL_LOWER) * ((double) scroll / max);
        fill(graphics, SCROLL_X, knobY, SCROLL_X + SCROLL_W, knobY + SCROLL_H, SCROLL_COLOUR);
    }

    /**
     * L'image d'une pochette, ou celle du morceau sans pochette.
     *
     * <p>L'original chargeait la sienne depuis le dossier du joueur ; le port n'a que les
     * trois morceaux du mod, dont les pochettes sont livrees avec lui.
     */
    private static ResourceLocation cover(Media media) {
        ResourceLocation cover = ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                "media/cover/" + media.id() + ".png");
        return exists(cover) ? cover : TEX_NO_COVER;
    }

    // ------------------------------------------------------------------
    // Interaction
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        double pageX = pageX(mouseX);
        double pageY = pageY(mouseY);

        // Le bouton de volume : on attrape le curseur ou l'on clique.
        if (inside(pageX, pageY, KNOB_LOWER, KNOB_Y,
                KNOB_UPPER - KNOB_LOWER + KNOB_W, KNOB_H)) {
            dragging = Drag.VOLUME;
            dragVolume(pageX);
            return true;
        }

        // La glissiere de la liste.
        if (maxScroll() > 0 && inside(pageX, pageY, SCROLL_X, SCROLL_LOWER,
                SCROLL_W, SCROLL_UPPER - SCROLL_LOWER + SCROLL_H)) {
            dragging = Drag.SCROLL;
            dragScroll(pageY);
            return true;
        }

        // Le bouton lecture : il reprend le dernier morceau, ou le premier de la liste.
        if (inside(pageX, pageY, POP_X, BTN_Y, BTN_W, BTN_H)) {
            togglePlay();
            return true;
        }

        // Le bouton arret.
        if (inside(pageX, pageY, STOP_X, BTN_Y, BTN_W, BTN_H)) {
            ClientMediaPlayer.stop();
            return true;
        }

        // Une rangee : elle lance son morceau.
        int index = rowAt(pageX, pageY);
        if (index >= 0) {
            play(medias.get(index));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX,
                                double dragY) {
        if (dragging == Drag.VOLUME) {
            dragVolume(pageX(mouseX));
            return true;
        }
        if (dragging == Drag.SCROLL) {
            dragScroll(pageY(mouseY));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = Drag.NONE;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** Le volume suit le curseur, de son bord gauche a son bord droit. */
    private void dragVolume(double pageX) {
        double part = (pageX - KNOB_LOWER) / (KNOB_UPPER - KNOB_LOWER);
        ClientMediaPlayer.setVolume((float) Math.max(0.0, Math.min(1.0, part)));
    }

    /** La liste suit la glissiere, d'une rangee a la fois. */
    private void dragScroll(double pageY) {
        double part = (pageY - SCROLL_LOWER) / (SCROLL_UPPER - SCROLL_LOWER);
        part = Math.max(0.0, Math.min(1.0, part));
        scroll = (int) Math.round(part * maxScroll());
    }

    /**
     * Le bouton lecture : il met le morceau en attente ou le reprend, et le lance s'il n'y en
     * a pas — c'est l'ordre de l'original, qui reprenait son dernier morceau joue.
     */
    private void togglePlay() {
        if (ClientMediaPlayer.current() != null) {
            ClientMediaPlayer.togglePause();
            return;
        }

        Media media = lastPlayed != null ? lastPlayed : medias.isEmpty() ? null : medias.get(0);
        play(media);
    }

    private void play(Media media) {
        if (media == null) return;
        lastPlayed = media;
        ClientMediaPlayer.play(media);
    }

    /** La rangee sous ce point, en tenant compte du defilement, ou -1. */
    private int rowAt(double pageX, double pageY) {
        if (pageX < AREA_X || pageX >= AREA_X + ROW_W) return -1;

        int index = scroll + (int) ((pageY - AREA_Y) / ROW_H);
        if (pageY < AREA_Y || index < 0 || index >= medias.size()) return -1;
        if (index >= scroll + VISIBLE_ROWS) return -1;
        return index;
    }

    /** Combien de rangees on peut faire defiler, comme le `getMaxProgress` de l'original. */
    private int maxScroll() {
        return Math.max(0, medias.size() - VISIBLE_ROWS);
    }

    @Override
    public boolean isPauseScreen() {
        // La musique continue quand on ferme l'ecran : le jeu ne doit donc pas se mettre en
        // pause, exactement comme l'original (`doesGuiPauseGame = false`).
        return false;
    }

    // ------------------------------------------------------------------
    // Outils de dessin et de mesure
    // ------------------------------------------------------------------

    /** Le bord gauche de la page a l'ecran, en pixels. */
    private double originX() {
        return (width - PAGE_W * PAGE_SCALE) / 2.0;
    }

    private double originY() {
        return (height - PAGE_H * PAGE_SCALE) / 2.0;
    }

    /** Un point de l'ecran, ramene dans les unites du document. */
    private double pageX(double mouseX) {
        return (mouseX - originX()) / PAGE_SCALE;
    }

    private double pageY(double mouseY) {
        return (mouseY - originY()) / PAGE_SCALE;
    }

    private static boolean inside(double x, double y, double left, double top, double width,
                                  double height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    /**
     * Un texte pose par le haut de sa cellule, coupe a la largeur de sa boite.
     *
     * <p>La coupe est celle du {@code TextBox} de l'original : la chaine s'arrete des que la
     * largeur est atteinte, sans points de suspension.
     */
    private void text(GuiGraphics graphics, String value, double boxX, double boxY,
                      double boxWidth, int cell, int colour, boolean rightAligned) {
        String shown = fit(value, boxWidth, cell);
        int x = (int) Math.round(rightAligned
                ? boxX + boxWidth - AcademyFonts.width(null, shown, cell)
                : boxX);
        AcademyFonts.drawTop(graphics, font, shown, x, (int) Math.round(boxY), colour, cell);
    }

    private static String fit(String text, double boxWidth, int cell) {
        double acc = 0;
        int i = 0;
        while (i < text.length() && acc < boxWidth) {
            int codePoint = text.codePointAt(i);
            acc += AcademyFonts.width(null, new String(Character.toChars(codePoint)), cell);
            i += Character.charCount(codePoint);
        }
        return text.substring(0, i);
    }

    /** Un rectangle plein, en unites de page. */
    private static void fill(GuiGraphics graphics, double x1, double y1, double x2, double y2,
                             int colour) {
        blend();
        graphics.fill((int) Math.round(x1), (int) Math.round(y1), (int) Math.round(x2),
                (int) Math.round(y2), colour);
    }

    /** Une image du document, a l'opacite pleine. */
    private static void image(GuiGraphics graphics, ResourceLocation texture, double x, double y,
                              double width, double height, int texWidth, int texHeight) {
        imageAlpha(graphics, texture, x, y, width, height, texWidth, texHeight, 255);
    }

    /** Une image, a une opacite donnee. */
    private static void imageAlpha(GuiGraphics graphics, ResourceLocation texture, double x,
                                   double y, double width, double height, int texWidth,
                                   int texHeight, int alpha) {
        blend();
        graphics.setColor(1f, 1f, 1f, alpha / 255f);
        graphics.blit(texture, (int) Math.round(x), (int) Math.round(y),
                (int) Math.round(width), (int) Math.round(height),
                0f, 0f, texWidth, texHeight, texWidth, texHeight);
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    private static void blend() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /** Le temps d'un morceau en {@code mm:ss}, comme le {@code getDisplayTime} de l'original. */
    private static String format(float seconds) {
        int total = Math.max(0, (int) seconds);
        return String.format("%02d:%02d", total / 60, total % 60);
    }

    /** Vrai si cette ressource existe : une pochette peut manquer, le reste non. */
    private static boolean exists(ResourceLocation location) {
        return MediaPlayerScreen.class.getResource(
                "/assets/" + location.getNamespace() + "/" + location.getPath()) != null;
    }
}
