package cn.academy.terminal.client;

import cn.academy.client.gui.AcademyFonts;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * L'installation du terminal de donnees : la barre de progression de l'original.
 *
 * <p>Portage de {@code TerminalInstallEffect}, sur son document
 * {@code guis/terminal_installing.xml} : une barre de <b>150 sur 9</b>, un liseré blanc de
 * 147 sur 6, un creux de 146 sur 5, la part remplie de 145 sur 4, et au-dessus le mot
 * « Installing... » sur son propre fond gris.
 *
 * <p>Ses temps sont ceux de l'original, au centieme :
 * <ul>
 *   <li><b>4 secondes</b> de remplissage ({@code ANIM_LENGTH}) ;</li>
 *   <li>puis <b>0,7 seconde</b> d'attente ({@code WAIT}) avant d'ouvrir le terminal ;</li>
 *   <li>apparition en 0,2 s, disparition en 0,2 s apres les 4 secondes ;</li>
 *   <li>le texte garde un dixieme d'opacite meme efface, comme dans son {@code initBlender}.</li>
 * </ul>
 *
 * <p>A la fin, elle ouvre le terminal et dit dans le chat quelle touche l'ouvre — avec le nom
 * <b>reel</b> de la touche du joueur, ce que l'original faisait aussi (il lisait son
 * gestionnaire de touches).
 */
@OnlyIn(Dist.CLIENT)
public class TerminalInstallScreen extends Screen {

    /** Les temps de l'original, en secondes. */
    private static final double LENGTH = 4.0;
    private static final double WAIT = 0.7;
    private static final double BLEND_IN = 0.2;
    private static final double BLEND_OUT = 0.2;

    /** Le document : la barre, son liseré, son creux, sa part remplie, et son etiquette. */
    private static final double BAR_W = 150.0;
    private static final double BAR_H = 9.0;
    private static final double OUTLINE_W = 147.0;
    private static final double OUTLINE_H = 6.0;
    private static final double COVER_W = 146.0;
    private static final double COVER_H = 5.0;
    private static final double PROGRESS_W = 145.0;
    private static final double PROGRESS_H = 4.0;
    private static final double TAG_H = 8.0;
    private static final int TAG_CELL = 10;

    /** Les couleurs du document, alpha compris. */
    private static final int BACK = 0x783C3C3C;
    private static final int OUTLINE = 0x96FFFFFF;
    private static final int COVER = 0xC81E1E1E;
    private static final int PROGRESS = 0xC8FFFFFF;

    /** L'endroit de la barre dans la fenetre : le document la posait aux quatre cinquiemes. */
    private static final double BAR_Y_PART = 0.846;

    private final long startNano = System.nanoTime();

    /** Vrai une fois le terminal ouvert : la barre ne doit pas le rouvrir. */
    private boolean finished;

    public TerminalInstallScreen() {
        super(Component.empty());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        double time = elapsed();
        double fade = fade(time);

        double left = (width - BAR_W) / 2.0;
        double top = height * BAR_Y_PART;
        double middleX = left + BAR_W / 2.0;
        double middleY = top + BAR_H / 2.0;

        // Le fond de la barre.
        fill(graphics, left, top, BAR_W, BAR_H, scaleAlpha(BACK, fade));
        fill(graphics, middleX - OUTLINE_W / 2, middleY - OUTLINE_H / 2, OUTLINE_W, OUTLINE_H,
                scaleAlpha(OUTLINE, fade));
        fill(graphics, middleX - COVER_W / 2, middleY - COVER_H / 2, COVER_W, COVER_H,
                scaleAlpha(COVER, fade));

        // La part remplie : le temps ecoule sur les quatre secondes.
        double progress = Math.min(1.0, time / LENGTH);
        fill(graphics, middleX - PROGRESS_W / 2, middleY - PROGRESS_H / 2,
                PROGRESS_W * progress, PROGRESS_H, scaleAlpha(PROGRESS, fade));

        // L'etiquette, sur son propre fond, juste au-dessus de la barre.
        fill(graphics, left, top - TAG_H, BAR_W, TAG_H, scaleAlpha(BACK, fade));
        String tag = Component.translatable("ac.gui.terminal.installing").getString();
        int textAlpha = (int) Math.round(255 * (0.1 + 0.9 * fade));
        AcademyFonts.drawTop(graphics, font, fit(tag, BAR_W - 2, TAG_CELL), (int) Math.round(left),
                (int) Math.round(top - TAG_H), (textAlpha << 24) | 0x00FFFFFF, TAG_CELL);
    }

    @Override
    public void tick() {
        if (!finished && elapsed() >= LENGTH + WAIT) finish();
    }

    /**
     * Ouvre le terminal, et dit quelle touche l'ouvre.
     *
     * <p>C'est l'ordre de l'original : {@code TerminalUI.keyHandler.onKeyUp()} puis le message
     * dans le chat.
     */
    private void finish() {
        finished = true;

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new TerminalScreen());

        if (minecraft.player != null) {
            String key = TerminalKeyBindings.OPEN_TERMINAL.getTranslatedKeyMessage().getString();
            minecraft.player.displayClientMessage(
                    Component.translatable("ac.terminal.key_hint", key), false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        // L'effet de l'original etait un overlay : le jeu continuait de tourner.
        return false;
    }

    // ------------------------------------------------------------------
    // Outils
    // ------------------------------------------------------------------

    private double elapsed() {
        return (System.nanoTime() - startNano) / 1_000_000_000.0;
    }

    /** L'opacite de l'effet : apparition, plein, puis disparition — celles de l'original. */
    private static double fade(double time) {
        if (time < BLEND_IN) return time / BLEND_IN;
        if (time > LENGTH) return Math.max(0.0, 1.0 - (time - LENGTH) / BLEND_OUT);
        return 1.0;
    }

    /** La meme couleur, a l'opacite de l'effet. */
    private static int scaleAlpha(int colour, double fade) {
        int alpha = (int) Math.round(((colour >>> 24) & 0xFF) * fade);
        return (alpha << 24) | (colour & 0x00FFFFFF);
    }

    private static void fill(GuiGraphics graphics, double x, double y, double width,
                             double height, int colour) {
        if (width <= 0 || height <= 0 || ((colour >>> 24) & 0xFF) <= 0) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.fill((int) Math.round(x), (int) Math.round(y),
                (int) Math.round(x + width), (int) Math.round(y + height), colour);
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
}
