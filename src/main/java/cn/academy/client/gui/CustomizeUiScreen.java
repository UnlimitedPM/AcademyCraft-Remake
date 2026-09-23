package cn.academy.client.gui;

import cn.academy.client.hud.HudConfig;
import cn.academy.client.hud.HudElement;
import cn.academy.client.hud.HudLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * L'ecran « Customize UI » : ou se posent les elements du HUD.
 *
 * <p>Portage de {@code CustomizeUI}. La disposition est celle de l'original : un panneau
 * <b>Elements</b> en haut a gauche qui liste les quatre elements, un <b>apercu</b> de chacun
 * dessine a sa place reelle, et — pour l'element choisi — deux champs <b>X</b> et <b>Y</b>
 * poses a droite de sa ligne.
 *
 * <p>Les valeurs sont celles de l'original, bornes comprises : de -512 a 512. Hors bornes,
 * la saisie est refusee et le champ passe au rouge, exactement comme avant. Un element
 * choisi porte un contour blanc, pour qu'on voie lequel on deplace.
 *
 * <p>Les apercus viennent des textures de l'original ({@code guis/edit_preview/}) la ou elles
 * existent. Le lecteur media n'en a pas : il se dessine pour l'instant comme un panneau
 * portant son nom, en attendant son propre rendu.
 */
@OnlyIn(Dist.CLIENT)
public class CustomizeUiScreen extends Screen {

    private static final int MARGIN = 16;
    private static final int ROW = 12;
    private static final int PANEL = 0xF0181824;
    private static final int EDGE = 0xFF6FA8DC;
    private static final int TEXT = 0xFFE0E0E0;
    private static final int DIM = 0xFF909090;
    private static final int SELECTED = 0xFF2A3A50;
    private static final int FIELD = 0xFF101018;
    private static final int FIELD_TEXT = 0xFFFFFFFF;
    private static final int FIELD_BAD = 0xFFBB3333;
    private static final int OUTLINE = 0xFFFFFFFF;

    /** L'ecran precedent, pour y revenir sur Echap. */
    private final Screen parent;

    private final HudLayout layout;

    private final List<HudElement> elements = List.of(HudElement.values());

    private HudElement selected;
    private boolean editingX = true;
    private boolean editing = false;
    private String buffer = "";
    private boolean bufferBad = false;

    private int listLeft;
    private int listTop;
    private int listWidth;

    public CustomizeUiScreen(Screen parent) {
        super(Component.translatable("ac.gui.uiedit.elements"));
        this.parent = parent;
        this.layout = HudConfig.read();
        this.selected = elements.get(0);
    }

    @Override
    protected void init() {
        listLeft = MARGIN;
        listTop = MARGIN;
        listWidth = 0;
        for (HudElement element : elements) {
            listWidth = Math.max(listWidth, font.width(Component.translatable(element.getLabelKey())));
        }
        listWidth += 24;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        // Les apercus, a leur place reelle.
        for (HudElement element : elements) {
            drawPreview(graphics, element);
        }

        // Le panneau de la liste.
        int height = ROW * (elements.size() + 1) + 6;
        graphics.fill(listLeft, listTop, listLeft + listWidth, listTop + height, PANEL);
        graphics.renderOutline(listLeft, listTop, listWidth, height, EDGE);

        graphics.drawString(font, title, listLeft + 6, listTop + 4, TEXT, false);
        int y = listTop + 4 + ROW;
        for (HudElement element : elements) {
            boolean over = mouseX >= listLeft && mouseX <= listLeft + listWidth
                    && mouseY >= y - 1 && mouseY < y + ROW - 1;
            if (element == selected) {
                graphics.fill(listLeft + 2, y - 1, listLeft + listWidth - 2, y + ROW - 1, SELECTED);
            }
            graphics.drawString(font, Component.translatable(element.getLabelKey()),
                    listLeft + 6, y + 2, element == selected ? TEXT : over ? TEXT : DIM, false);
            y += ROW;
        }

        // Les deux champs de l'element choisi, a droite de sa ligne.
        if (selected != null) {
            int row = listTop + 4 + ROW * (elements.indexOf(selected) + 1);
            drawField(graphics, fieldX(), row, "X", HudConfigLabel.X, mouseX, mouseY);
            drawField(graphics, fieldX() + FIELD_WIDTH + 3, row, "Y", HudConfigLabel.Y, mouseX, mouseY);
        }

        graphics.drawString(font, Component.translatable("ac.gui.uiedit.hint"),
                MARGIN, height + MARGIN + 12, DIM, false);
    }

    private static final int FIELD_WIDTH = 54;
    private static final int FIELD_HEIGHT = 11;

    /** Le libelle des deux champs, pour ne pas se tromper de signe. */
    private enum HudConfigLabel { X, Y }

    private int fieldX() {
        return listLeft + listWidth + 6;
    }

    private void drawField(GuiGraphics graphics, int x, int y, String letter,
                           HudConfigLabel axis, int mouseX, int mouseY) {
        boolean focused = editing && selected != null
                && (axis == HudConfigLabel.X) == editingX;
        int color = bufferBad && focused ? FIELD_BAD : FIELD;

        graphics.fill(x, y - 1, x + FIELD_WIDTH, y + FIELD_HEIGHT, color);
        graphics.renderOutline(x, y - 1, FIELD_WIDTH, FIELD_HEIGHT + 1, EDGE);

        String value;
        if (focused) {
            value = buffer;
        } else {
            double number = axis == HudConfigLabel.X ? layout.getX(selected) : layout.getY(selected);
            value = number == Math.floor(number) ? String.valueOf((int) number) : String.valueOf(number);
        }
        graphics.drawString(font, letter + " " + value, x + 3, y + 1, FIELD_TEXT, false);
    }

    private void drawPreview(GuiGraphics graphics, HudElement element) {
        int w = element.getWidth();
        int h = element.getHeight();
        int x = layout.placeX(element, width, w);
        int y = layout.placeY(element, height, h);

        ResourceLocation texture = previewTexture(element);
        if (texture != null) {
            graphics.blit(texture, x, y, 0, 0, w, h, w, h);
        } else {
            graphics.fill(x, y, x + w, y + h, PANEL);
            graphics.drawString(font, Component.translatable(element.getLabelKey()),
                    x + 4, y + h / 2 - 4, DIM, false);
        }

        if (element == selected) {
            graphics.renderOutline(x - 1, y - 1, w + 2, h + 2, OUTLINE);
        }
    }

    /** L'apercu d'un element, quand l'original en livre un. */
    private static ResourceLocation previewTexture(HudElement element) {
        String name = switch (element) {
            case CP_BAR -> "cpbar";
            case KEY_HINT -> "key_hint";
            case NOTIFICATION -> "notify_logo";
            case MEDIA -> null;
        };
        if (name == null) return null;
        return ResourceLocation.fromNamespaceAndPath("academy", "textures/guis/edit_preview/" + name + ".png");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // Les lignes de la liste.
        int y = listTop + 4 + ROW;
        for (HudElement element : elements) {
            if (mouseX >= listLeft && mouseX <= listLeft + listWidth
                    && mouseY >= y - 1 && mouseY < y + ROW - 1) {
                selected = element;
                editing = false;
                return true;
            }
            y += ROW;
        }

        // Les deux champs de l'element choisi.
        if (selected != null) {
            int row = listTop + 4 + ROW * (elements.indexOf(selected) + 1);
            if (inField(mouseX, mouseY, fieldX(), row)) {
                beginEdit(true);
                return true;
            }
            if (inField(mouseX, mouseY, fieldX() + FIELD_WIDTH + 3, row)) {
                beginEdit(false);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean inField(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + FIELD_WIDTH
                && mouseY >= y - 1 && mouseY <= y + FIELD_HEIGHT;
    }

    /** Ouvre un champ : on tape dedans, et Entree valide. */
    private void beginEdit(boolean x) {
        editing = true;
        editingX = x;
        bufferBad = false;
        double value = x ? layout.getX(selected) : layout.getY(selected);
        buffer = value == Math.floor(value) ? String.valueOf((int) value) : String.valueOf(value);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!editing) return super.charTyped(codePoint, modifiers);
        if ((codePoint >= '0' && codePoint <= '9') || codePoint == '-' || codePoint == '.') {
            buffer += codePoint;
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editing) {
            if (keyCode == 259 && !buffer.isEmpty()) { // retour arriere
                buffer = buffer.substring(0, buffer.length() - 1);
                bufferBad = false;
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Entree
                confirmEdit();
                return true;
            }
            if (keyCode == 256) { // Echap ferme le champ avant l'ecran
                editing = false;
                bufferBad = false;
                return true;
            }
            return true;
        }

        if (keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void confirmEdit() {
        double value;
        try {
            value = Double.parseDouble(buffer.trim());
        } catch (NumberFormatException e) {
            bufferBad = true;
            return;
        }

        if (!HudLayout.isValid(value)) {
            bufferBad = true;
            return;
        }

        double x = editingX ? value : layout.getX(selected);
        double y = editingX ? layout.getY(selected) : value;
        if (HudConfig.write(selected, x, y)) {
            layout.set(selected, x, y);
            editing = false;
            bufferBad = false;
        } else {
            bufferBad = true;
        }
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
