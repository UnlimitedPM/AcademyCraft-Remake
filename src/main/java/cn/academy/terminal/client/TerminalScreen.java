package cn.academy.terminal.client;

import cn.academy.terminal.App;
import cn.academy.terminal.TerminalData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * L'ecran du terminal de donnees : une grille d'applications installees.
 *
 * <h2>Ce qui change par rapport a l'original</h2>
 *
 * Le terminal de l'original etait un overlay en 3D dessine par le moteur CGui de
 * LambdaLib : la grille tournait avec la souris, on cliquait l'icone face a soi, et
 * la fenetre de l'application s'empilait par-dessus. Rien de tout cela n'existe
 * ici : c'est une page plate, sans rotation ni animation, et un clic sur une icone
 * remplace la page par l'ecran de l'application.
 *
 * Ce qui est conserve : le terminal s'ouvre par une touche (Alt gauche, comme
 * {@code open_data_terminal}), il n'affiche que ce qui est installe, et une
 * application sans contenu porte n'apparait pas du tout — c'est le registre qui
 * decide, pas l'ecran.
 */
public class TerminalScreen extends Screen {

    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 146;

    private static final int PANEL = 0xF0202020;
    private static final int PANEL_EDGE = 0xFF6FA8DC;
    private static final int CELL = 0x40FFFFFF;
    private static final int CELL_HOVER = 0x80A8D8FF;
    private static final int TEXT = 0xFFE0E0E0;
    private static final int DIM = 0xFF909090;

    private static final int CELL_SIZE = 44;
    private static final int CELLS_PER_ROW = 4;

    private int leftPos;
    private int topPos;

    /** L'application sous la souris, ou null. Recalculee a chaque image. */
    private App hovered;

    public TerminalScreen() {
        super(Component.translatable("ac.terminal.title"));
    }

    @Override
    protected void init() {
        this.leftPos = (width - PANEL_WIDTH) / 2;
        this.topPos = (height - PANEL_HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        graphics.fill(leftPos - 1, topPos - 1, leftPos + PANEL_WIDTH + 1, topPos + PANEL_HEIGHT + 1, PANEL_EDGE);
        graphics.fill(leftPos, topPos, leftPos + PANEL_WIDTH, topPos + PANEL_HEIGHT, PANEL);

        graphics.drawString(font, title, leftPos + 8, topPos + 7, TEXT, false);

        List<App> apps = installedWidgets();
        if (apps.isEmpty()) {
            graphics.drawString(font, Component.translatable("ac.terminal.no_apps"),
                    leftPos + 8, topPos + 24, DIM, false);
        }

        hovered = null;
        for (int i = 0; i < apps.size(); i++) {
            App app = apps.get(i);
            int x = cellX(i);
            int y = cellY(i);
            boolean over = mouseX >= x && mouseX < x + CELL_SIZE && mouseY >= y && mouseY < y + CELL_SIZE;
            if (over) hovered = app;

            graphics.fill(x, y, x + CELL_SIZE, y + CELL_SIZE, over ? CELL_HOVER : CELL);
            drawIcon(graphics, app, x, y);
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        if (hovered != null) {
            graphics.renderTooltip(font, hovered.getDisplayName(), mouseX, mouseY);
        }
    }

    /** Dessine l'icone de l'application au centre de sa case. */
    private void drawIcon(GuiGraphics graphics, App app, int x, int y) {
        int size = 28;
        int ox = x + (CELL_SIZE - size) / 2;
        int oy = y + (CELL_SIZE - size) / 2;
        int tex = app.getIconSize();
        ResourceLocation icon = app.getIcon();
        // On reduit l'icone d'origine (110 ou 128 pixels) a la taille d'une case :
        // les fichiers recopies n'ont pas tous la meme dimension.
        graphics.blit(icon, ox, oy, size, size, 0f, 0f, tex, tex, tex, tex);
    }

    private int cellX(int index) {
        return leftPos + 10 + (index % CELLS_PER_ROW) * (CELL_SIZE + 2);
    }

    private int cellY(int index) {
        return topPos + 24 + (index / CELLS_PER_ROW) * (CELL_SIZE + 2);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            List<App> apps = installedWidgets();
            for (int i = 0; i < apps.size(); i++) {
                int x = cellX(i);
                int y = cellY(i);
                if (mouseX >= x && mouseX < x + CELL_SIZE && mouseY >= y && mouseY < y + CELL_SIZE) {
                    Screen page = AppScreens.create(apps.get(i));
                    if (minecraft != null && page != null) {
                        minecraft.setScreen(page);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Les applications proposees par le terminal.
     *
     * Deux filtres, et pas un de plus : il faut que l'application soit installee,
     * et qu'un ecran lui soit associe cote client. Une application preinstallee de
     * l'original mais non portee n'a pas d'ecran, donc elle ne s'affiche pas :
     * dessiner une icone qui n'ouvre rien serait pire que de ne rien dessiner.
     */
    private List<App> installedWidgets() {
        TerminalData data = ClientTerminalData.get();
        return data.getInstalledApps(cn.academy.terminal.AppRegistry.INSTANCE).stream()
                .filter(AppScreens::isAvailable)
                .toList();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
