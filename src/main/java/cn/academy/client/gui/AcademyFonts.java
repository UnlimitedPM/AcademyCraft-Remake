package cn.academy.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
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
     * La hauteur de la CELLULE du texte de l'interface, en pixels — LE seul chiffre a tourner.
     *
     * <p>C'est la hauteur que la lettre occupe a l'ecran, et c'est l'unite de l'original : son
     * {@code FontOption(n)} dessine des quads de n pixels de cote, avance ses caracteres de
     * {@code charWidth x n / charSize} et fait avancer ses lignes de n. La gravure du videau, elle,
     * est faite a trente-trois : c'est le rapport des deux (un et quatre dixiemes) qui donne a la
     * fois l'echelle des avances et l'epaisseur du trait.
     *
     * <p>Dix, pour deux raisons qui se rejoignent : c'est le defaut de LambdaLib2 —
     * {@code new FontOption()} vaut dix, donc ce qu'herite tout texte dont le document ne dit rien —
     * et c'est la cellule que l'original donne a sa console de debogage, son
     * {@code DebugConsole.Text(String)} construisant un {@code FontOption(10)}. Mesure sur Microsoft
     * YaHei : une capitale de 5,5 pixels, et un « H » qui avance de 5,8. Le joueur est arrive la en
     * deux temps : « le texte est encore legerement plus gros, comme si c'etait ecrit en gras »,
     * puis son propre repere — onze et demi doit devenir onze.
     *
     * <p>CE CHIFFRE EST LA CELLULE, et non le cadratin de la police, et c'est ce qui fait que la
     * gravure, l'avance et le dessin parlent tous les trois du meme pixel. Calculee sur le cadratin
     * — ce que le port faisait —, l'avance sortait de quatre pour cent plus COURTE que la lettre
     * dessinee, qui est arrondie a la cellule : les lettres se tassaient l'une contre l'autre, et le
     * joueur l'a vu du premier coup d'oeil — « dans le vrai mod, le texte a l'air d'etre un peu plus
     * etire vers la droite, avec ces lettres un peu plus larges ».
     */
    public static final int GUI_SIZE = 10;

    /**
     * La taille a laquelle les glyphes sont graves dans le videau, et la cellule qui les porte.
     *
     * <p>Vingt-quatre et trente-trois : les nombres que le mod se donne a lui-meme, dans son
     * {@code ClientResources} — {@code TrueTypeFont.withFallback(Font.PLAIN, 24, "Microsoft YaHei",
     * ...)} puis une cellule d'une fois et quatre dixiemes sa taille, comme le fait le constructeur
     * du {@code TrueTypeFont} de LambdaLib2. (Son {@code defaultFont}, lui, grave a trente-deux :
     * c'est un autre chemin, et l'AcademyCraft n'y passe pas.)
     *
     * <p>C'est le RAPPORT des deux qui compte, et il vaut un et quatre dixiemes dans les deux cas :
     * une lettre dessinee sur une cellule de dix pixels nait d'une image de trente-trois, celle des
     * nombres du mod.
     */
    private static final int SOURCE_FONT_SIZE = 24;
    private static final int ATLAS_SIZE = 2048;
    private static final int CELL_SIZE = (int) (SOURCE_FONT_SIZE * 1.4f);
    private static final int CELLS_PER_ROW = ATLAS_SIZE / CELL_SIZE;
    private static final int MAX_GLYPHS = CELLS_PER_ROW * CELLS_PER_ROW;

    /** Les bornes d'une cellule : sous cinq pixels la lettre n'existe plus, au-dessus de trente-trois elle depasse la gravure. */
    private static final int MIN_CELL = 5;
    private static final int MAX_CELL = 33;

    /**
     * L'epaisseur ajoutee a chaque lettre, en unites de la gravure : zero n'ajoute rien.
     *
     * <p>Le trait de la lettre est trace une seconde fois, en contour, ce qui l'epaissit de sa
     * largeur de chaque cote sans toucher a sa forme — c'est l'embellissement synthetique, et il
     * tombe juste parce que la gravure est RAPETISSEE : la cellule fait quarante-quatre unites et
     * l'ecran en demande huit a treize, donc un demi-pixel de contour ici ne fait qu'un huitieme de
     * pixel la-bas. Mesure sur « Hamburgefonstiv », une fois la cellule ramenee a sa taille de
     * dessin : +18 % d'encre a 0,5, +29 % a 0,8, +36 % a 1. Le joueur trouvait le texte du vrai mod
     * plus epais que le notre (« il est pas assez epais ») ; c'est le seul chiffre a tourner si le
     * trait parait encore maigre, ou s'il devient pateux.
     *
     * <p>Elle se compte en unites de GRAVURE, donc elle suit la taille de la gravure : a quarante-
     * quatre la valeur d'origine etait 0,6, la meme epaisseur a l'ecran vaut 0,6 x 24 / 32 = 0,45
     * avec une gravure de vingt-quatre. La regler sans regarder ce rapport change l'epaisseur deux
     * fois.
     */
    private static final double GLYPH_WEIGHT = 0.45;

    /**
     * La ligne de base, mesuree depuis le haut du texte : huit pixels.
     *
     * <p>C'est ce qui pose la CELLULE a l'aplomb de la ligne convenue, comme le fait l'original :
     * son {@code TextBox} met le haut de sa cellule au y du widget (pour un alignement en haut), et
     * sa console avance de la hauteur de cette cellule. Le port posait la ligne de base un pixel
     * plus haut — sept —, ce que le joueur a vu tout de suite : « le texte est environ un pixel trop
     * haut (pour le F4 par exemple) ». A dix pixels de cellule, huit fait tomber l'encre exactement
     * ou l'original la mettait ; l'ecart maximal, sur une autre taille, vaut un demi-pixel.
     */
    private static final int BASELINE = 8;

    private static final Map<Integer, java.awt.Font> FONTS = new HashMap<>();
    private static final Map<Integer, java.awt.FontMetrics> METRICS = new HashMap<>();
    private static final Map<GlyphKey, Glyph> GLYPHS = new HashMap<>();

    private static NativeImage atlasPixels;
    private static DynamicTexture atlasTexture;
    private static ResourceLocation atlasLocation;
    private static int nextGlyphSlot;
    private static boolean atlasDirty;

    private record GlyphKey(int codePoint, int size) {}
    private record Glyph(double advance, int x, int y, int size) {}

    private AcademyFonts() {
    }

    public static int width(Font ignored, String text) {
        return width(text, GUI_SIZE);
    }

    public static int width(Font ignored, Component text) {
        return width(text.getString(), GUI_SIZE);
    }

    /** La largeur d'un texte dessine a une taille donnee, en pixels. */
    public static int width(Font ignored, String text, int size) {
        return width(text, size);
    }

    public static int width(Font ignored, Component text, int size) {
        return width(text.getString(), size);
    }

    /**
     * La taille a demander pour dessiner une cellule de tel nombre de pixels : c'est deja la bonne
     * unite, il n'y a donc rien a traduire — seulement de quoi borner un document abime.
     *
     * <p>Les pages du mod demandent 6, 8, 9, 10, 12 et 13 : le meme langage que l'original, dont le
     * {@code FontOption(n)} dessine une cellule de n pixels. Sous cinq la lettre disparait, et
     * au-dela de trente-trois on demanderait plus grand que la gravure elle-meme.
     */
    public static int cell(double cellPixels) {
        return Math.max(MIN_CELL, Math.min(MAX_CELL, (int) Math.round(cellPixels)));
    }

    /**
     * L'avance total d'un texte, en pixels : la somme EXACTE de ses avances, arrondie une fois.
     *
     * <p>C'est ce que le dessin pose, caractere par caractere, et la somme n'est arrondie qu'au
     * bout : la largeur mesuree et la largeur dessinee ne peuvent donc pas diverger, ce qui compte
     * pour tout ce qui se centre ou s'aligne a droite.
     */
    static int width(String text, int size) {
        return (int) Math.round(exactWidth(text, size));
    }

    /**
     * La somme exacte des avances d'un texte, sans arrondi.
     *
     * <p>L'original avancait de la valeur EXACTE de chaque avance — {@code x += info.width * scale},
     * en flottants — et ne l'arrondissait nulle part. Le port arrondissait chaque avance a un pixel
     * entier, ce qui perdait jusqu'a un demi-pixel PAR LETTRE : une ligne de trente caracteres
     * sortait une dizaine de pixels plus courte que celle du vrai mod, et le joueur l'a vu tout de
     * suite — « le texte a l'air d'etre un peu plus etire vers la droite, avec des lettres un peu
     * plus larges ». Chaque caractere se pose donc a sa position exacte, arrondie au pixel le plus
     * proche AU DESSIN.
     */
    static double exactWidth(String text, int size) {
        double total = 0;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            total += advance(codePoint, size);
            offset += Character.charCount(codePoint);
        }
        return total;
    }

    /**
     * L'avance d'un caractere, exacte : ce que la police en donne au cadratin de la gravure, ramene
     * a la cellule demandee.
     *
     * <p>{@code charWidth} et non la largeur d'un texte d'un caractere : c'est la mesure que
     * l'original lisait ({@code metrics.charWidth(ch)}), et les deux different d'un pixel sur les
     * lettres qui depassent leur avance.
     */
    static double advance(int codePoint, int cell) {
        double source = metrics(SOURCE_FONT_SIZE).charWidth(codePoint);
        return source * cell / (double) CELL_SIZE;
    }

    /**
     * La hauteur des CAPITALES, en pixels, pour une cellule donnee.
     *
     * <p>C'est la mesure qui dit si le texte est lisible a cote de celui du jeu, et c'est elle qu'un
     * test fige : elle vaut 5,5 px a la cellule de l'interface, contre 7 pour la police de
     * Minecraft.
     */
    static int capitalHeight(int cell) {
        java.awt.Font font = awtFont(SOURCE_FONT_SIZE);
        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scratch.createGraphics();
        configure(graphics, font);
        double source = font.createGlyphVector(graphics.getFontRenderContext(), "H")
                .getVisualBounds().getHeight();
        graphics.dispose();
        return (int) Math.round(source * cell / (double) CELL_SIZE);
    }

    /** La hauteur d'une ligne pour une cellule donnee : c'est la cellule, comme chez l'original. */
    public static int lineHeight(int cell) {
        return cell;
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

    /** Un texte a une taille donnee — celle d'un widget, qui vient de son document. */
    public static int draw(GuiGraphics graphics, Font ignored, String text, int x, int y,
                           int color, boolean shadow, int size) {
        return drawText(graphics, text, x, y, color, shadow, size);
    }

    public static int draw(GuiGraphics graphics, Font ignored, Component text, int x, int y,
                           int color, boolean shadow, int size) {
        return drawText(graphics, text.getString(), x, y, color, shadow, size);
    }

    /**
     * La console de debogage (F4) : le meme texte que l'interface, et c'est mesure deux fois. Sur
     * les deux captures du joueur, a la meme echelle d'interface, la meme ligne fait 298 pixels chez
     * nous a la taille dix et 231 chez l'original 1.12.2, dont les barres rapides font exactement la
     * meme taille (le rapport vaut 0,775) ; et la source de l'original le dit aussi, son
     * {@code DebugConsole.Text(String)} construisant un {@code FontOption(10)}. Ce sont ses LIGNES
     * qui sont serrees — dix pixels, la hauteur de sa cellule.
     */
    public static int drawConsole(GuiGraphics graphics, String text, int x, int y, int color) {
        return drawText(graphics, text, x, y, color, false, GUI_SIZE);
    }

    private static int drawText(GuiGraphics graphics, String text, int x, int y,
                                int color, boolean shadow, int size) {
        if (text.isEmpty() || (color >>> 24) == 0) return 0;
        ensureGlyphs(text, size);
        if (atlasDirty) uploadAtlas();

        // Le melange, pose par la police elle-meme, et c'est tout l'objet de ces trois lignes : la
        // police du jeu le posait par le type de rendu de son texte, et le port l'a perdu en passant
        // a AWT. Sans elles le texte herite de ce que l'ecran a laisse derriere lui — un effet du mod
        // qui AJOUTE sa lumiere laisse le melange additif, et le texte devient gras et lumineux ;
        // n'importe quel aplat le laisse eteint, et le texte s'ecrit sans fondu. C'est ce que le
        // joueur a vu, et il l'a dit deux fois : « avec la notification le texte est le bon, une fois
        // qu'elle part il devient moins clean », puis « quand je me mets avec le mod pour utiliser les
        // pouvoirs, le texte devient plus fluide ». Le texte changeait donc selon ce qui avait ete
        // dessine AVANT lui dans l'image. Un renderer de HUD ne suppose jamais l'etat qu'il n'a pas
        // pose.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

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
        // La teinte du moment : un appelant qui assombrit son texte — une notification qui s'efface,
        // l'apercu d'un ecran de reglage — pose une couleur AVANT d'appeler, et la police doit la
        // respecter. C'est ce que faisait celle du jeu, dont le type de rendu ne posait aucune couleur
        // a lui : il multipliait simplement celle du moment. Sans cela, la notification de l'original
        // s'effacait avec son panneau et son texte restait en plein, ce qui se voit.
        float[] tint = RenderSystem.getShaderColor();
        float red = tint[0];
        float green = tint[1];
        float blue = tint[2];
        float alpha = tint[3];

        int side = size;
        // La cellule est posee pour que la ligne de base tombe sur celle de la police du jeu, quelle
        // que soit la taille : c'est ce qui fait qu'un texte grossi grandit sur place au lieu de
        // descendre dans son panneau. Le decalage se lit dans la gravure, donc il suit la cellule.
        int top = y + BASELINE - (int) Math.round(baselineInCell() * size
                / (double) CELL_SIZE);

        // Le curseur est EXACT et c'est le dessin qui arrondit : la position d'un caractere ne perd
        // rien, et une ligne se termine exactement a la largeur annoncee. Arrondir l'avance de
        // chaque lettre mangeait un demi-pixel par lettre, et le joueur l'a vu.
        double exactCursor = x;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            Glyph glyph = glyph(codePoint, size);
            if (glyph.size() > 0) {
                graphics.setColor(((color >> 16) & 0xFF) / 255f * red,
                        ((color >> 8) & 0xFF) / 255f * green,
                        (color & 0xFF) / 255f * blue,
                        ((color >>> 24) & 0xFF) / 255f * alpha);
                graphics.blit(atlasLocation, (int) Math.round(exactCursor), top, side, side,
                        glyph.x(), glyph.y(), CELL_SIZE, CELL_SIZE, ATLAS_SIZE, ATLAS_SIZE);
                exactCursor += glyph.advance();
            }
            offset += Character.charCount(codePoint);
        }

        // La teinte est rendue telle qu'elle etait : ce qui se dessine apres nous y compte.
        graphics.setColor(red, green, blue, alpha);
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
        double advance = advance(codePoint, size);
        if (advance <= 0) {
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
        if (GLYPH_WEIGHT > 0) {
            java.awt.font.GlyphVector vector =
                    font.createGlyphVector(draw.getFontRenderContext(), text);
            draw.setStroke(new java.awt.BasicStroke((float) GLYPH_WEIGHT,
                    java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            draw.draw(vector.getOutline(3, 1 + metrics.getAscent()));
        }
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