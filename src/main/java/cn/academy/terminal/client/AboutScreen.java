package cn.academy.terminal.client;

import cn.academy.terminal.about.AboutDocument;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * L'ecran de l'application « A propos » : un document qui defile.
 *
 * <h2>Ce qui change par rapport a l'original</h2>
 *
 * L'original dessinait ce texte dans un widget 3D avec une barre de defilement a
 * tirer, un masque de profondeur et des liens cliquables qui ouvraient le
 * navigateur du bureau. Ici la molette suffit, et il n'y a pas de liens : le seul
 * texte du document qui ressemble a une adresse est l'en-tete du projet, et ouvrir
 * un navigateur depuis un jeu n'apporte rien.
 *
 * Le document lui-meme vient de {@link AboutDocument}, qui ne connait pas
 * Minecraft. Cet ecran ne fait que le mettre en page et le faire defiler.
 */
@OnlyIn(Dist.CLIENT)
public class AboutScreen extends Screen {

    private static final int PANEL_MARGIN = 20;
    private static final int HEADER = 20;
    private static final int MAX_PANEL_WIDTH = 280;

    private static final int PANEL = 0xF0181824;
    private static final int PANEL_EDGE = 0xFF6FA8DC;
    private static final int TEXT = 0xFFE0E0E0;
    private static final int DIM = 0xFF909090;

    /** Espace entre le centre et les colonnes « role / noms ». */
    private static final int ROLE_GAP = 20;

    private static final int LINE = 10;
    private static final int DONATOR_COLUMN = 84;

    private final AboutDocument document;

    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;

    /** Defilement courant, en pixels. */
    private int scroll;

    private int contentHeight;

    public AboutScreen() {
        super(Component.translatable("ac.app.about.name"));
        this.document = AboutDocument.load();
    }

    @Override
    protected void init() {
        panelWidth = Math.min(MAX_PANEL_WIDTH, width - 2 * PANEL_MARGIN);
        panelHeight = height - 2 * PANEL_MARGIN;
        leftPos = (width - panelWidth) / 2;
        topPos = (height - panelHeight) / 2;
        contentHeight = measure();
        scroll = 0;
    }

    private int viewTop() {
        return topPos + HEADER;
    }

    private int viewHeight() {
        return panelHeight - HEADER - 4;
    }

    /** Hauteur totale du document, une fois mis en page. */
    private int measure() {
        int height = 0;
        for (AboutDocument.Line line : lines()) {
            height += lineHeight(line);
        }
        height += document.donatorRows().size() * LINE;
        return height;
    }

    private static int lineHeight(AboutDocument.Line line) {
        return Math.round(LINE * line.scale());
    }

    private List<AboutDocument.Line> lines() {
        return document.toLines(Component.translatable("ac.about.donators_info").getString());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        graphics.fill(leftPos - 1, topPos - 1, leftPos + panelWidth + 1, topPos + panelHeight + 1, PANEL_EDGE);
        graphics.fill(leftPos, topPos, leftPos + panelWidth, topPos + panelHeight, PANEL);
        graphics.drawString(font, title, leftPos + 8, topPos + 7, TEXT, false);

        if (document.isEmpty()) {
            graphics.drawString(font, Component.translatable("ac.about.empty"),
                    leftPos + 8, topPos + HEADER, DIM, false);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        int centre = leftPos + panelWidth / 2;
        int y = viewTop() - scroll;
        int bottom = viewTop() + viewHeight();

        for (AboutDocument.Line line : lines()) {
            int height = lineHeight(line);
            if (y + height >= viewTop() && y <= bottom) {
                draw(graphics, line, centre, y);
            }
            y += height;
        }

        drawDonators(graphics, centre, y, bottom);
        drawScrollBar(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void draw(GuiGraphics graphics, AboutDocument.Line line, int centre, int y) {
        if (line.text().isEmpty()) return;
        graphics.pose().pushPose();
        graphics.pose().scale(line.scale(), line.scale(), 1.0f);

        // Les coordonnees sont divisees par l'echelle : le texte est dessine dans un
        // repere reduit, donc tout ce qui n'est pas mis a l'echelle doit l'etre.
        float scaledCentre = centre / line.scale();
        float scaledY = y / line.scale();

        int textWidth = font.width(line.text());
        float x = switch (line.align()) {
            case CENTER -> scaledCentre - textWidth / 2.0f;
            case RIGHT -> scaledCentre - ROLE_GAP - textWidth;
            case LEFT -> scaledCentre + ROLE_GAP;
        };

        graphics.drawString(font, line.text(), Math.round(x), Math.round(scaledY), TEXT, false);
        graphics.pose().popPose();
    }

    /** Les donateurs, en colonnes, comme le faisait l'original. */
    private void drawDonators(GuiGraphics graphics, int centre, int y, int bottom) {
        List<List<String>> rows = document.donatorRows();
        if (rows.isEmpty()) return;

        int first = centre - (AboutDocument.DONATORS_PER_ROW - 1) * DONATOR_COLUMN / 2;
        for (List<String> row : rows) {
            if (y + LINE >= viewTop() && y <= bottom) {
                for (int i = 0; i < row.size(); i++) {
                    graphics.drawString(font, row.get(i), first + i * DONATOR_COLUMN, y, DIM, false);
                }
            }
            y += LINE;
        }
    }

    private void drawScrollBar(GuiGraphics graphics) {
        int max = Math.max(0, contentHeight - viewHeight());
        if (max <= 0) return;

        int barX = leftPos + panelWidth - 4;
        int top = viewTop();
        int height = viewHeight();
        graphics.fill(barX, top, barX + 2, top + height, 0xFF373737);
        int barHeight = Math.max(8, height * height / contentHeight);
        int barTop = top + (height - barHeight) * scroll / max;
        graphics.fill(barX, barTop, barX + 2, barTop + barHeight, PANEL_EDGE);
    }

    private void scrollBy(int amount) {
        int max = Math.max(0, contentHeight - viewHeight());
        scroll = Math.max(0, Math.min(max, scroll + amount));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0) {
            scrollBy((int) (-delta * LINE * 2));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
