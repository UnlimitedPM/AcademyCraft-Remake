package cn.academy.client.gui;

import cn.academy.AcademyCraft;
import cn.academy.ability.client.AbilityKeyBindings;
import cn.academy.terminal.client.TerminalKeyBindings;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * L'ecran de reglage du mod : ce que l'application « Settings » du terminal ouvre.
 *
 * <p>Portage de {@code SettingsUI}. L'original y rangeait tout ce qu'un joueur doit pouvoir
 * regler, parce que <b>rien de tout cela n'apparait dans les options de Minecraft</b> — les
 * touches du mod se choisissent ici, et nulle part ailleurs.
 *
 * <p>La disposition est celle de l'original : trois sections (<i>Keys</i>, <i>Generic</i>,
 * <i>Misc</i>), une ligne par reglage, et une barre de defilement quand il y en a trop.
 *
 * <h2>Ce qui n'est pas encore la</h2>
 *
 * <p>L'original listait aussi <i>Debug Console</i>, et trois options generales qui n'ont pas
 * encore de comportement dans le port (PvP entre joueurs, pile ou face, molette de distance
 * de teleportation). Elles ne sont <b>pas</b> affichees : une case qui ne fait rien vaut
 * moins qu'une case absente. Elles viendront avec ce qu'elles commandent.
 */
@OnlyIn(Dist.CLIENT)
public class SettingsScreen extends Screen {

    private static final int MARGIN = 16;
    private static final int ROW = 14;
    private static final int PANEL = 0xF0181824;
    private static final int EDGE = 0xFF6FA8DC;
    private static final int TEXT = 0xFFE0E0E0;
    private static final int DIM = 0xFF909090;
    private static final int CATEGORY = 0xFF9CC4E4;
    private static final int VALUE = 0xFFFFFFFF;
    private static final int HIGHLIGHT = 0xFF2A3A50;
    private static final int OK_BUTTON = 0xFF3A3A52;

    /** Ce qu'une ligne fait quand on clique sur sa partie droite. */
    private enum Kind { KEY, TOGGLE, ACTION }

    private record Row(String labelKey, Kind kind, String category,
                       KeyMapping key, java.util.function.BooleanSupplier getter, Runnable action) {

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

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int scroll;
    private int contentHeight;

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

    private void buildRows() {
        if (rows.size() > 0) return;

        // Keys : ce que l'original listait, dans son ordre. Les quatre touches d'aptitude
        // d'abord, puis l'allumage de l'aptitude, les prereglages et le terminal.
        rows.add(Row.key("keys", AbilityKeyBindings.ABILITY_1));
        rows.add(Row.key("keys", AbilityKeyBindings.ABILITY_2));
        rows.add(Row.key("keys", AbilityKeyBindings.ABILITY_3));
        rows.add(Row.key("keys", AbilityKeyBindings.ABILITY_4));
        rows.add(Row.key("keys", AbilityKeyBindings.TOGGLE_ABILITY));
        rows.add(Row.key("keys", AbilityKeyBindings.PRESET_EDIT));
        rows.add(Row.key("keys", AbilityKeyBindings.PRESET_NEXT));
        rows.add(Row.key("keys", TerminalKeyBindings.OPEN_TERMINAL));

        // Generic : seule « Destroy blocks » a un comportement aujourd'hui.
        rows.add(Row.toggle("generic", "ac.settings.prop.destroyBlocks",
                () -> cn.academy.Config.destroyBlocks, () -> cn.academy.Config.toggleDestroyBlocks()));

        // Misc.
        rows.add(Row.action("misc", "ac.settings.prop.edit_ui",
                () -> minecraft.setScreen(new CustomizeUiScreen(this))));
    }

    @Override
    protected void init() {
        panelLeft = MARGIN;
        panelTop = MARGIN;
        panelWidth = Math.min(width - 2 * MARGIN, 300);
        panelHeight = height - 2 * MARGIN;

        contentHeight = 0;
        String last = null;
        for (Row row : rows) {
            if (!row.category().equals(last)) {
                contentHeight += ROW;
                last = row.category();
            }
            contentHeight += ROW;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, PANEL);
        graphics.renderOutline(panelLeft, panelTop, panelWidth, panelHeight, EDGE);
        graphics.drawString(font, title, panelLeft + 8, panelTop + 6, TEXT, false);

        int y = panelTop + 6 + ROW + ROW - scroll;
        String last = null;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (!row.category().equals(last)) {
                if (isVisible(y)) {
                    graphics.drawString(font, Component.translatable("ac.settings.cat." + row.category()),
                            panelLeft + 8, y + 3, CATEGORY, false);
                }
                y += ROW;
                last = row.category();
            }

            if (isVisible(y)) {
                boolean over = inRow(mouseX, mouseY, y);
                if (over && !isCapturing(i)) {
                    graphics.fill(panelLeft + 4, y, panelLeft + panelWidth - 4, y + ROW, HIGHLIGHT);
                }
                graphics.drawString(font, Component.translatable(row.labelKey()),
                        panelLeft + 12, y + 3, TEXT, false);
                drawValue(graphics, row, i, y);
            }
            y += ROW;
        }

        drawScrollbar(graphics);
    }

    private boolean isVisible(int y) {
        return y + ROW > panelTop + ROW && y < panelTop + panelHeight;
    }

    private boolean inRow(double mouseX, double mouseY, int y) {
        return mouseX >= panelLeft + 4 && mouseX <= panelLeft + panelWidth - 4
                && mouseY >= y && mouseY < y + ROW;
    }

    private boolean isCapturing(int index) {
        return capturing == index;
    }

    private void drawValue(GuiGraphics graphics, Row row, int index, int y) {
        Component text = switch (row.kind()) {
            case KEY -> isCapturing(index)
                    ? Component.translatable("ac.settings.press_key")
                    : row.key().getTranslatedKeyMessage();
            case TOGGLE -> Component.literal(String.valueOf(row.getter().getAsBoolean()));
            case ACTION -> Component.translatable("ac.settings.ok");
        };

        int right = panelLeft + panelWidth - 12;
        int x = right - font.width(text);
        // La ou on clique : la partie droite de la ligne, pas la ligne entiere.
        graphics.fill(x - 3, y, right + 3, y + ROW, OK_BUTTON);
        graphics.drawString(font, text, x, y + 3, VALUE, false);
    }

    private void drawScrollbar(GuiGraphics graphics) {
        int viewport = panelHeight - 2 * ROW;
        if (contentHeight <= viewport) return;

        int trackTop = panelTop + ROW;
        int trackBottom = panelTop + panelHeight - ROW;
        int barHeight = Math.max(16, (int) ((float) viewport / contentHeight * (trackBottom - trackTop)));
        int maxScroll = contentHeight - viewport;
        int barTop = trackTop + (int) ((float) scroll / maxScroll * (trackBottom - trackTop - barHeight));

        int x = panelLeft + panelWidth - 6;
        graphics.fill(x, trackTop, x + 3, trackBottom, 0xFF000000);
        graphics.fill(x, barTop, x + 3, barTop + barHeight, EDGE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int y = panelTop + 6 + ROW + ROW - scroll;
        String last = null;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (!row.category().equals(last)) {
                y += ROW;
                last = row.category();
            }

            if (inRow(mouseX, mouseY, y)) {
                // Seule la partie droite repond : c'est la regle de l'original.
                Component text = switch (row.kind()) {
                    case KEY -> row.key().getTranslatedKeyMessage();
                    case TOGGLE -> Component.literal(String.valueOf(row.getter().getAsBoolean()));
                    case ACTION -> Component.translatable("ac.settings.ok");
                };
                int right = panelLeft + panelWidth - 12;
                if (mouseX >= right - font.width(text) - 3) {
                    click(row, i);
                    return true;
                }
                return true;
            }
            y += ROW;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void click(Row row, int index) {
        switch (row.kind()) {
            case KEY -> capturing = index;
            case TOGGLE -> row.action().run();
            case ACTION -> row.action().run();
        }
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
                row.key().setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int viewport = panelHeight - 2 * ROW;
        int maxScroll = Math.max(0, contentHeight - viewport);
        scroll = (int) Math.max(0, Math.min(maxScroll, scroll - delta * ROW));
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
