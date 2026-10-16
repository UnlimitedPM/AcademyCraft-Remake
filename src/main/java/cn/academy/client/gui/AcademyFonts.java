package cn.academy.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** AWT system-font renderer, matching LambdaLib2's oversampled glyph atlas. */
@OnlyIn(Dist.CLIENT)
public final class AcademyFonts {
    private static final String[] FONT_FAMILIES = {
            "Microsoft YaHei", "SimHei", "Adobe Heiti Std R", "Arial"
    };

    /**
     * La hauteur du texte de l'interface, en pixels — LE seul chiffre a tourner pour le grossir.
     *
     * <p>C'est la hauteur d'un caractere, son <b>cadratin</b>, et non la cellule du videau : l'avance
     * et le dessin se calculent tous les deux a partir d'elle, donc les lettres grandissent ensemble
     * et l'espacement suit. La cellule du videau, elle, fait le quart plus que le cadratin (trente-
     * trois unites pour vingt-quatre, la hauteur de ligne de la police) : s'en servir comme echelle
     * rapetissait tout d'un quart, et c'est ce que le joueur a vu.
     *
     * <p>A neuf, mesure sur Microsoft YaHei : la hauteur de capitale vaut 6,8 px et un « H » avance
     * de 7,1 px. La police du jeu, elle, fait 7 et 6 — c'est donc la taille du texte du jeu, et
     * c'est ce que les ecrans attendent, eux qui posent leurs lignes tous les dix a vingt pixels.
     */
    public static final int GUI_SIZE = 9;

    private static final int CONSOLE_SIZE = 10;
    private static final int SOURCE_FONT_SIZE = 24;
    private static final int ATLAS_SIZE = 2048;
    private static final int CELL_SIZE = (int) (SOURCE_FONT_SIZE * 1.4f);
    private static final int CELLS_PER_ROW = ATLAS_SIZE / CELL_SIZE;
    private static final int MAX_GLYPHS = CELLS_PER_ROW * CELLS_PER_ROW;

    /**
     * La ligne de base, mesuree depuis le haut du texte : sept pixels.
     *
     * <p>C'est celle de la police du jeu, qui pose ses capitales a un pixel du haut et son pied a
     * sept. La cellule du videau est donc posee pour que la ligne de base tombe la, <b>a n'importe
     * quelle taille</b> : le texte se place exactement ou {@code drawString} le placerait, et
     * changer la taille ne le fait pas descendre dans son panneau.
     */
    private static final int BASELINE = 7;

    private static final Map<Integer, java.awt.Font> FONTS = new HashMap<>();
    private static final Map<Integer, java.awt.FontMetrics> METRICS = new HashMap<>();
    private static final Map<GlyphKey, Glyph> GLYPHS = new HashMap<>();

    private static NativeImage atlasPixels;
    private static DynamicTexture atlasTexture;
    private static ResourceLocation atlasLocation;
    private static int nextGlyphSlot;
    private static boolean atlasDirty;

    private record GlyphKey(int codePoint, int size) {}
    private record Glyph(int advance, int x, int y, int size) {}

    private AcademyFonts() {
    }

    public static int width(Font ignored, String text) {
        return width(text, GUI_SIZE);
    }

    public static int width(Font ignored, Component text) {
        return width(text.getString(), GUI_SIZE);
    }

    /**
     * L'avance total d'un texte, en pixels : la somme des avances de ses caracteres.
     *
     * <p>Le dessin se sert de la MEME fonction caractere par caractere : la largeur mesuree et la
     * largeur dessinee ne peuvent donc pas diverger, ce qui compte pour tout ce qui se centre ou
     * s'aligne a droite.
     */
    static int width(String text, int size) {
        int total = 0;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            total += advance(codePoint, size);
            offset += Character.charCount(codePoint);
        }
        return total;
    }

    /**
     * L'avance d'un caractere : ce que la police en donne, ramene du cadratin source a la taille.
     *
     * <p>Arrondie caractere par caractere, et non sur la somme : c'est l'avance qui est posee au
     * dessin, donc c'est elle qu'il faut mesurer.
     */
    private static int advance(int codePoint, int size) {
        double source = metrics(SOURCE_FONT_SIZE)
                .stringWidth(new String(Character.toChars(codePoint)));
        return (int) Math.round(source * size / (double) SOURCE_FONT_SIZE);
    }

    /** La hauteur d'un caractere dessine : le cadratin, en pixels. */
    private static int box(int size) {
        return (int) Math.round(CELL_SIZE * size / (double) SOURCE_FONT_SIZE);
    }

    /**
     * La hauteur des CAPITALES, en pixels, pour une taille donnee.
     *
     * <p>C'est la mesure qui dit si le texte est lisible a cote de celui du jeu, et c'est elle qu'un
     * test fige : elle vaut 6,8 px a la taille de l'interface, contre 7 pour la police de Minecraft.
     * Exposee pour ce test, et parce que c'est la seule chose qu'un reglage de la taille fait varier
     * qui se voie.
     */
    static int capitalHeight(int size) {
        java.awt.Font font = awtFont(SOURCE_FONT_SIZE);
        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scratch.createGraphics();
        configure(graphics, font);
        double source = font.createGlyphVector(graphics.getFontRenderContext(), "H")
                .getVisualBounds().getHeight();
        graphics.dispose();
        return (int) Math.round(source * size / (double) SOURCE_FONT_SIZE);
    }

    public static int lineHeight(int size) {
        return size;
    }

    /** La hauteur d'une ligne du texte de l'interface : c'est la taille, comme dans le jeu. */
    public static int lineHeight() {
        return lineHeight(GUI_SIZE);
    }

    public static List<String> split(Font ignored, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\n", -1)) {
            if (paragraph.trim().isEmpty()) {
                lines.add("");
                continue;
            }

            StringBuilder line = new StringBuilder();
            for (String word : paragraph.trim().split("\\s+")) {
                if (line.length() == 0) {
                    appendWrappedWord(word, line, lines, maxWidth);
                    continue;
                }

                String candidate = line + " " + word;
                if (width(candidate, GUI_SIZE) <= maxWidth) {
                    line.append(' ').append(word);
                } else {
                    lines.add(line.toString());
                    line.setLength(0);
                    appendWrappedWord(word, line, lines, maxWidth);
                }
            }
            if (line.length() > 0) lines.add(line.toString());
        }
        return lines;
    }

    private static void appendWrappedWord(String word, StringBuilder line,
                                          List<String> lines, int maxWidth) {
        for (int offset = 0; offset < word.length();) {
            int codePoint = word.codePointAt(offset);
            String next = new String(Character.toChars(codePoint));
            if (line.length() > 0 && width(line + next, GUI_SIZE) > maxWidth) {
                lines.add(line.toString());
                line.setLength(0);
            }
            line.append(next);
            offset += Character.charCount(codePoint);
        }
    }

    public static int draw(GuiGraphics graphics, Font ignored, String text, int x, int y,
                           int color, boolean shadow) {
        return drawText(graphics, text, x, y, color, shadow, GUI_SIZE);
    }

    public static int draw(GuiGraphics graphics, Font ignored, Component text, int x, int y,
                           int color, boolean shadow) {
        return drawText(graphics, text.getString(), x, y, color, shadow, GUI_SIZE);
    }

    public static int drawConsole(GuiGraphics graphics, String text, int x, int y, int color) {
        return drawText(graphics, text, x, y, color, false, CONSOLE_SIZE);
    }

    private static int drawText(GuiGraphics graphics, String text, int x, int y,
                                int color, boolean shadow, int size) {
        if (text.isEmpty() || (color >>> 24) == 0) return 0;
        ensureGlyphs(text, size);
        if (atlasDirty) uploadAtlas();

        if (shadow) drawLine(graphics, text, x + 1, y + 1, shadowColor(color), size);
        drawLine(graphics, text, x, y, color, size);
        return width(text, size);
    }

    private static void ensureGlyphs(String text, int size) {
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            glyph(codePoint, size);
            offset += Character.charCount(codePoint);
        }
    }

    private static void drawLine(GuiGraphics graphics, String text, int x, int y,
                                 int color, int size) {
        int cursor = x;
        int side = box(size);
        // La cellule est posee pour que la ligne de base tombe sur celle de la police du jeu, quelle
        // que soit la taille : c'est ce qui fait qu'un texte grossi grandit sur place au lieu de
        // descendre dans son panneau.
        int top = y + BASELINE - (int) Math.round(baselineInCell() * size
                / (double) SOURCE_FONT_SIZE);

        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            Glyph glyph = glyph(codePoint, size);
            if (glyph.size() > 0) {
                graphics.setColor(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f,
                        (color & 0xFF) / 255f, ((color >>> 24) & 0xFF) / 255f);
                graphics.blit(atlasLocation, cursor, top, side, side,
                        glyph.x(), glyph.y(), CELL_SIZE, CELL_SIZE, ATLAS_SIZE, ATLAS_SIZE);
                cursor += glyph.advance();
            }
            offset += Character.charCount(codePoint);
        }
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    /** Ou la ligne de base tombe dans la cellule, en unites du videau : c'est ou le glyphe est peint. */
    private static int baselineInCell() {
        return 1 + metrics(SOURCE_FONT_SIZE).getAscent();
    }

    private static Glyph glyph(int codePoint, int size) {
        GlyphKey key = new GlyphKey(codePoint, size);
        Glyph cached = GLYPHS.get(key);
        if (cached != null) return cached;

        ensureAtlas();
        java.awt.Font font = awtFont(SOURCE_FONT_SIZE);
        java.awt.FontMetrics metrics = metrics(SOURCE_FONT_SIZE);
        String text = new String(Character.toChars(codePoint));
        int advance = advance(codePoint, size);
        if (advance == 0) {
            Glyph blank = new Glyph(0, 0, 0, 0);
            GLYPHS.put(key, blank);
            return blank;
        }

        if (nextGlyphSlot >= MAX_GLYPHS) {
            if (codePoint != '?') return glyph('?', size);
            return new Glyph(advance, 0, 0, 0);
        }

        int x = (nextGlyphSlot % CELLS_PER_ROW) * CELL_SIZE;
        int y = (nextGlyphSlot / CELLS_PER_ROW) * CELL_SIZE;
        nextGlyphSlot++;

        BufferedImage image = new BufferedImage(CELL_SIZE, CELL_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D draw = image.createGraphics();
        configure(draw, font);
        draw.setColor(java.awt.Color.WHITE);
        draw.drawString(text, 3, 1 + metrics.getAscent());
        draw.dispose();

        NativeImage glyphPixels = nativeImage(image);
        glyphPixels.copyRect(atlasPixels, 0, 0, x, y, CELL_SIZE, CELL_SIZE, false, false);
        glyphPixels.close();
        atlasDirty = true;

        Glyph result = new Glyph(advance, x, y, size);
        GLYPHS.put(key, result);
        return result;
    }

    private static void ensureAtlas() {
        if (atlasTexture != null) return;
        atlasPixels = new NativeImage(ATLAS_SIZE, ATLAS_SIZE, true);
        atlasTexture = new DynamicTexture(atlasPixels);
        atlasLocation = Minecraft.getInstance().getTextureManager()
                .register("academy_awt_glyphs", atlasTexture);
    }

    private static void uploadAtlas() {
        atlasTexture.upload();
        atlasTexture.bind();
        atlasTexture.setFilter(true, true);
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, -0.65f);
        atlasDirty = false;
    }

    private static NativeImage nativeImage(BufferedImage image) {
        try {
            ByteArrayOutputStream encoded = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", encoded)) {
                throw new IOException("No PNG writer available");
            }
            return NativeImage.read(encoded.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not rasterize AcademyCraft glyph", exception);
        }
    }

    private static java.awt.Font awtFont(int size) {
        return FONTS.computeIfAbsent(SOURCE_FONT_SIZE, requestedSize -> {
            java.awt.Font[] installed = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
            for (String family : FONT_FAMILIES) {
                for (java.awt.Font candidate : installed) {
                    if (candidate.getName().equalsIgnoreCase(family)) {
                        return new java.awt.Font(candidate.getName(), java.awt.Font.PLAIN,
                                SOURCE_FONT_SIZE);
                    }
                }
            }
            return new java.awt.Font(java.awt.Font.DIALOG, java.awt.Font.PLAIN, SOURCE_FONT_SIZE);
        });
    }

    private static java.awt.FontMetrics metrics(int size) {
        return METRICS.computeIfAbsent(SOURCE_FONT_SIZE, requestedSize -> {
            BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = scratch.createGraphics();
            configure(graphics, awtFont(SOURCE_FONT_SIZE));
            java.awt.FontMetrics result = graphics.getFontMetrics();
            graphics.dispose();
            return result;
        });
    }

    private static void configure(Graphics2D graphics, java.awt.Font font) {
        graphics.setFont(font);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
    }

    private static int shadowColor(int color) {
        return (color & 0xFF000000)
                | (((color >> 16) & 0xFF) / 4 << 16)
                | (((color >> 8) & 0xFF) / 4 << 8)
                | ((color & 0xFF) / 4);
    }
}