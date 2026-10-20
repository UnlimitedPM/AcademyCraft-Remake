package cn.academy.client.gui;

import cn.academy.AcademyCraft;
import cn.academy.ability.client.AbilityKeyBindings;
import cn.academy.terminal.client.TerminalKeyBindings;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * L'ecran de reglage du mod : la page de l'original.
 *
 * <p>Portage de {@code SettingsUI}, sur son document {@code guis/settings.xml} : une page de
 * <b>742 sur 923</b> que le document dessine a <b>0,2</b> — donc 148 sur 185 a l'ecran, centree.
 * Le port dessinait un panneau plein ecran de son invention ; tout ce qui suit vient du document,
 * en unites de page :
 *
 * <ul>
 *   <li>le fond {@code guis/settings.png}, qui porte le cadre et le titre ;</li>
 *   <li>la <b>liste</b> de 614 sur 720 a (60 ; 120), ou chaque ligne fait 611 sur 60 : un
 *       <i>titre de categorie</i> (son texte a 8, souligne d'un trait blanc de 4 unites en bas),
 *       une <i>case a cocher</i> (le libelle a 15 et la case de 35 a 550), une <i>touche</i> (le
 *       libelle, puis le nom de la touche dans 160 sur 40 a 440) ou un <i>bouton</i>;</li>
 *   <li>la <b>glissiere</b> de 9 sur 96 a (673 ; 119), {@code life_record/rollbar.png}, qui
 *       glisse jusqu'a 760.</li>
 * </ul>
 *
 * <p>Rien de tout cela n'apparait dans les options de Minecraft : les touches du mod se
 * choisissent ici, et nulle part ailleurs.
 *
 * <h2>Un ecart</h2>
 *
 * <p>Dans le document, le bouton d'une ligne d'action est dessine vide (son {@code TextBox} n'a
 * pas de contenu) — un rectangle gris de 160 sur 40 qui ne dit pas ce qu'il fait. Le port y ecrit
 * le mot de l'action : un bouton muet se lit comme un bug.
 */
@OnlyIn(Dist.CLIENT)
public class SettingsScreen extends Screen {

    // ------------------------------------------------------------------
    // Le document `guis/settings.xml`, en unites de page.
    // ------------------------------------------------------------------

    private static final double PAGE_W = 742.0;
    private static final double PAGE_H = 923.0;
    /** L'echelle que le document donne a sa page : 742 sur 923 devient 148 sur 185. */
    private static final double PAGE_SCALE = 0.2;

    private static final double AREA_X = 60.0;
    private static final double AREA_Y = 120.0;
    private static final double AREA_W = 614.0;
    private static final double AREA_H = 720.0;

    /** Une ligne de la liste, et son trait de categorie. */
    private static final double ROW_W = 611.0;
    private static final double ROW_H = 60.0;
    private static final double LINE_H = 4.0;
    private static final int HEAD_CELL = 42;
    private static final int CELL = 40;

    /** Le libelle, la case a cocher et le controle de droite. */
    private static final double LABEL_X = 15.0;
    private static final double LABEL_W = 300.0;
    private static final double BOX_X = 550.0;
    private static final double BOX_SIZE = 35.0;
    private static final double CTRL_X = 440.0;
    private static final double CTRL_W = 160.0;
    private static final double CTRL_H = 40.0;
    /** Les controles sont centres dans la hauteur de la ligne : (60 - 40) / 2. */
    private static final double CTRL_Y = (ROW_H - CTRL_H) / 2;
    /** Le titre de categorie se pose a 8 unites du bord de la ligne. */
    private static final double HEAD_X = 8.0;

    /** La glissiere : 9 sur 96, qui descend de 119 a 760 (`lower` / `upper` du document). */
    private static final double SCROLL_X = 673.0;
    private static final double SCROLL_W = 9.0;
    private static final double SCROLL_H = 96.0;
    private static final double SCROLL_LOWER = 119.0;
    private static final double SCROLL_UPPER = 760.0;

    /** Couleurs du document : libelle, titre de categorie, trait, bouton. */
    private static final int TEXT_COLOUR = 0xFFFFFFFF;
    private static final int HEAD_COLOUR = 0xAAFFFFFF;
    private static final int LINE_COLOUR = 0x96FFFFFF;
    private static final int BUTTON_COLOUR = 0xFF333333;
    private static final int BUTTON_HOVER = 0xFF555555;
    private static final int HOVER_VEIL = 0x33FFFFFF;
    /** La touche en cours de capture : l'orange de son {@code EditKey}. */
    private static final int EDIT_COLOUR = 0xFFFB8525;

    private static final ResourceLocation TEX_BACK = texture("guis/settings.png");
    private static final ResourceLocation TEX_ROLLBAR = texture("guis/life_record/rollbar.png");
    private static final ResourceLocation TEX_CHECK_ON = texture("guis/check_true.png");
    private static final ResourceLocation TEX_CHECK_OFF = texture("guis/check_false.png");

    /** Ce qu'une ligne fait quand on clique sur sa partie droite. */
    private enum Kind { HEADER, KEY, TOGGLE, ACTION }

    private record Row(String labelKey, Kind kind, String category,
                       KeyMapping key, java.util.function.BooleanSupplier getter, Runnable action) {

        static Row header(String category) {
            return new Row("ac.settings.cat." + category, Kind.HEADER, category, null, null, null);
        }

        static Row key(String category, KeyMapping mapping) {
            return new Row(mapping.getName(), Kind.KEY, category, mapping, null, null);
        }

        static Row toggle(String category, String labelKey,
                          java.util.function.BooleanSupplier getter, Runnable action) {
            return new Row(labelKey, Kind.TOGGLE, category, null, getter, action);
        }

        static Row action(String category, String labelKey, Runnable action) {
            return new Row(labelKey, Kind.ACTION, category, null, null, action);
        }
    }

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();

    /** Position de la glissiere, de 0 a 1. */
    private double progress;

    private boolean dragging;
    private double dragGrip;

    /** La ligne dont la touche est en cours de capture, ou -1. */
    private int capturing = -1;

    public SettingsScreen() {
        this(null);
    }

    public SettingsScreen(Screen parent) {
        super(Component.translatable("ac.app.settings.name"));
        this.parent = parent;
        buildRows();
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "textures/" + path);
    }

    /**
     * Le contenu de la page, dans l'ordre de l'original : un titre de categorie, puis ses
     * lignes. Le titre est une ligne a part entiere chez lui (le {@code t_cathead} de 611 sur 60,
     * avec son trait sous le texte), pas un simple libelle pose au-dessus.
     */
    private void buildRows() {
        if (!rows.isEmpty()) return;

        rows.add(Row.header("keys"));
        // Keys : ce que l'original listait, dans son ordre — les quatre touches d'aptitude
        // d'abord, puis l'allumage, les prereglages et le terminal.
        rows.add(Row.key("keys", AbilityKeyBindings.ABILITY_1));
        rows.add(Row.key("keys", AbilityKeyBindings.ABILITY_2));
        rows.add(Row.key("keys", AbilityKeyBindings.ABILITY_3));
        rows.add(Row.key("keys", AbilityKeyBindings.ABILITY_4));
        rows.add(Row.key("keys", AbilityKeyBindings.TOGGLE_ABILITY));
        rows.add(Row.key("keys", AbilityKeyBindings.PRESET_EDIT));
        rows.add(Row.key("keys", AbilityKeyBindings.PRESET_NEXT));
        rows.add(Row.key("keys", TerminalKeyBindings.OPEN_TERMINAL));

        rows.add(Row.header("generic"));
        // Generic : seule « Destroy blocks » a un comportement aujourd'hui.
        rows.add(Row.toggle("generic", "ac.settings.prop.destroyBlocks",
                () -> cn.academy.Config.destroyBlocks, () -> cn.academy.Config.toggleDestroyBlocks()));

        rows.add(Row.header("misc"));
        rows.add(Row.action("misc", "ac.settings.prop.edit_ui",
                () -> minecraft.setScreen(new CustomizeUiScreen(this))));
    }

    // ------------------------------------------------------------------
    // Geometrie
    // ------------------------------------------------------------------

    private double originX() {
        return (width - PAGE_W * PAGE_SCALE) / 2.0;
    }

    private double originY() {
        return (height - PAGE_H * PAGE_SCALE) / 2.0;
    }

    /** La hauteur de la liste entiere, titres de categorie compris, en unites de page. */
    private double contentHeight() {
        return rows.size() * ROW_H;
    }

    private boolean scrollable() {
        return contentHeight() > AREA_H;
    }

    /** Le defilement du moment, en unites de page. */
    private double shift() {
        if (!scrollable()) return 0;
        return progress * (contentHeight() - AREA_H);
    }

    /** Le haut de la glissiere, en coordonnees de page. */
    private double thumbY() {
        return SCROLL_LOWER + progress * (SCROLL_UPPER - SCROLL_LOWER);
    }

    private void scrollBy(double pageOffset) {
        if (!scrollable()) return;
        double max = contentHeight() - AREA_H;
        double now = shift() + pageOffset;
        progress = Math.max(0, Math.min(1, now / max));
    }

    // ------------------------------------------------------------------
    // Rendu
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        graphics.pose().pushPose();
        graphics.pose().translate((float) originX(), (float) originY(), 0f);
        graphics.pose().scale((float) PAGE_SCALE, (float) PAGE_SCALE, 1f);

        image(graphics, TEX_BACK, 0, 0, PAGE_W, PAGE_H, 742, 923);

        graphics.flush();
        graphics.enableScissor(
                (int) Math.floor(originX() + AREA_X * PAGE_SCALE),
                (int) Math.floor(originY() + AREA_Y * PAGE_SCALE),
                (int) Math.ceil(originX() + (AREA_X + AREA_W) * PAGE_SCALE),
                (int) Math.ceil(originY() + (AREA_Y + AREA_H) * PAGE_SCALE));
        drawRows(graphics, mouseX, mouseY);
        graphics.disableScissor();

        drawScrollBar(graphics);

        graphics.pose().popPose();
        graphics.flush();
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    private void drawRows(GuiGraphics graphics, int mouseX, int mouseY) {
        double pageX = (mouseX - originX()) / PAGE_SCALE;
        double pageY = (mouseY - originY()) / PAGE_SCALE;
        double shift = shift();

        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            double y = AREA_Y + i * ROW_H - shift;
            if (y > AREA_Y + AREA_H || y + ROW_H < AREA_Y) continue;

            boolean overRow = pageY >= y && pageY < y + ROW_H
                    && pageX >= AREA_X && pageX <= AREA_X + ROW_W;

            if (row.kind() == Kind.HEADER) {
                // Le titre de categorie : son texte a 8, en blanc a 170/255, et son trait de
                // 4 unites colle au bas de la ligne (`alignHeight>BOTTOM`).
                AcademyFonts.drawTop(graphics, font,
                        Component.translatable(row.labelKey()).getString(),
                        (int) Math.round(AREA_X + HEAD_X), (int) Math.round(y), HEAD_COLOUR,
                        HEAD_CELL);
                fill(graphics, AREA_X, y + ROW_H - LINE_H, ROW_W, LINE_H, LINE_COLOUR);
                continue;
            }

            // Le libelle de la ligne, et sa case ou son controle a droite.
            boolean overControl = overRow && pageX >= AREA_X + CTRL_X - 20;
            drawControl(graphics, row, i, y, overControl);
            drawLabel(graphics, row, y, overRow);
        }
    }

    private void drawLabel(GuiGraphics graphics, Row row, double y, boolean over) {
        double labelY = y + CTRL_Y;
        if (over && capturing < 0) {
            fill(graphics, AREA_X, y, ROW_W, ROW_H, HOVER_VEIL);
        }
        String label = fit(Component.translatable(row.labelKey()).getString(), LABEL_W, CELL);
        AcademyFonts.drawTop(graphics, font, label,
                (int) Math.round(AREA_X + LABEL_X), (int) Math.round(labelY), TEXT_COLOUR, CELL);
    }

    /**
     * Un texte coupe a la largeur de sa boite.
     *
     * <p>C'est la regle du {@code TextBox} de l'original : la chaine s'arrete des que la largeur
     * est atteinte, sans points de suspension. Sans elle, « Right Button » deborde de la page.
     */
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

    private void drawControl(GuiGraphics graphics, Row row, int index, double y, boolean over) {
        switch (row.kind()) {
            case TOGGLE -> {
                boolean on = row.getter() != null && row.getter().getAsBoolean();
                double boxX = AREA_X + BOX_X;
                double boxY = y + CTRL_Y;
                image(graphics, on ? TEX_CHECK_ON : TEX_CHECK_OFF, boxX, boxY, BOX_SIZE, BOX_SIZE,
                        48, 48);
            }
            case KEY -> {
                boolean editing = capturing == index;
                if (over || editing) {
                    // Le Tint de la touche : transparent, puis un voile blanc a 20 % au survol.
                    fill(graphics, AREA_X + CTRL_X, y + CTRL_Y, CTRL_W, CTRL_H, HOVER_VEIL);
                }
                Component text = editing
                        ? Component.translatable("ac.settings.press_key")
                        : row.key().getTranslatedKeyMessage();
                // Son TextBox est aligne a GAUCHE dans les 160 de large, pas centre, et coupe a
                // cette largeur.
                AcademyFonts.drawTop(graphics, font, fit(text.getString(), CTRL_W, CELL),
                        (int) Math.round(AREA_X + CTRL_X), (int) Math.round(y + CTRL_Y),
                        editing ? EDIT_COLOUR : TEXT_COLOUR, CELL);
            }
            case ACTION -> {
                fill(graphics, AREA_X + CTRL_X, y + CTRL_Y, CTRL_W, CTRL_H,
                        over ? BUTTON_HOVER : BUTTON_COLOUR);
                Component text = Component.translatable("ac.settings.ok");
                String shown = fit(text.getString(), CTRL_W, CELL);
                int x = (int) Math.round(AREA_X + CTRL_X + CTRL_W / 2.0
                        - AcademyFonts.width(null, shown, CELL) / 2.0);
                AcademyFonts.drawTop(graphics, font, shown, x,
                        (int) Math.round(y + CTRL_Y), TEXT_COLOUR, CELL);
            }
        }
    }

    private void drawScrollBar(GuiGraphics graphics) {
        if (!scrollable()) return;
        image(graphics, TEX_ROLLBAR, SCROLL_X, thumbY(), SCROLL_W, SCROLL_H, 9, 96);
    }

    // ------------------------------------------------------------------
    // La souris et le clavier
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        double pageX = (mouseX - originX()) / PAGE_SCALE;
        double pageY = (mouseY - originY()) / PAGE_SCALE;

        // La glissiere se prend par ou on la touche.
        if (scrollable() && pageX >= SCROLL_X && pageX <= SCROLL_X + SCROLL_W
                && pageY >= thumbY() - 4 && pageY <= thumbY() + SCROLL_H + 4) {
            dragging = true;
            dragGrip = pageY - thumbY();
            return true;
        }

        // Une ligne : seule sa partie droite repond, comme chez l'original, ou la case et la
        // touche sont des widgets a part.
        double shift = shift();
        for (int i = 0; i < rows.size(); i++) {
            double y = AREA_Y + i * ROW_H - shift;
            if (pageY < y || pageY >= y + ROW_H) continue;
            if (pageX < AREA_X + CTRL_X - 20 || pageX > AREA_X + ROW_W) continue;

            Row row = rows.get(i);
            switch (row.kind()) {
                case KEY -> capturing = i;
                case TOGGLE -> {
                    if (row.action() != null) row.action().run();
                }
                case ACTION -> {
                    if (row.action() != null) row.action().run();
                }
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX,
                                double dragY) {
        if (dragging) {
            double top = (mouseY - originY()) / PAGE_SCALE - dragGrip - SCROLL_LOWER;
            progress = Math.max(0, Math.min(1, top / (SCROLL_UPPER - SCROLL_LOWER)));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0) {
            scrollBy(-delta * ROW_H);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturing >= 0) {
            // Echap annule la capture, comme partout ailleurs.
            if (keyCode == 256) {
                capturing = -1;
                return true;
            }
            Row row = rows.get(capturing);
            if (row != null && row.key() != null) {
                row.key().setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM
                        .getOrCreate(keyCode));
                KeyMapping.resetMapping();
                if (minecraft != null) minecraft.options.save();
            }
            capturing = -1;
            return true;
        }

        if (keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            cn.academy.Config.save();
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------
    // Le dessin
    // ------------------------------------------------------------------

    private static void blend() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void fill(GuiGraphics graphics, double x, double y, double width,
                             double height, int colour) {
        blend();
        graphics.fill((int) Math.round(x), (int) Math.round(y),
                (int) Math.round(x + width), (int) Math.round(y + height), colour);
    }

    private static void image(GuiGraphics graphics, ResourceLocation texture, double x, double y,
                             double width, double height, int texWidth, int texHeight) {
        blend();
        graphics.setColor(1f, 1f, 1f, 1f);
        graphics.blit(texture, (int) Math.round(x), (int) Math.round(y),
                (int) Math.round(width), (int) Math.round(height),
                0f, 0f, texWidth, texHeight, texWidth, texHeight);
    }
}
