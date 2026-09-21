package cn.academy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ecran du Matrix sans fil : les quatre emplacements a gauche, l'etat du reseau
 * a droite.
 *
 * Rendu procedural, comme {@link SolarGenScreen}, en attendant une texture.
 */
public class MatrixScreen extends AbstractContainerScreen<MatrixMenu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int TEXT = 0x404040;
    private static final int OK = 0x2E7D32;
    private static final int OFF = 0x9E2B25;

    public MatrixScreen(MatrixMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);

        // Fond des quatre emplacements du Matrix.
        for (int slot = 0; slot < 3; slot++) {
            slotBackground(graphics, x + 53 + slot * 18, y + 35);
        }
        slotBackground(graphics, x + 125, y + 35);
    }

    private void slotBackground(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_BG);
        graphics.fill(x, y, x + 16, y + 16, 0xFF373737);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, TEXT, false);

        boolean working = menu.isWorking();
        graphics.drawString(font, working ? "Actif" : "Inactif", 8, 20,
                working ? OK : OFF, false);

        if (working) {
            graphics.drawString(font, "Coeur " + menu.getCoreLevel() + " / 3", 8, 44, TEXT, false);
            graphics.drawString(font, "Portee " + menu.getRange() + " blocs", 8, 55, TEXT, false);
            graphics.drawString(font, menu.getCapacity() + " noeuds max", 8, 66, TEXT, false);
            graphics.drawString(font, menu.getBandwidth() + " / tick", 8, 77, TEXT, false);
        } else {
            graphics.drawString(font, "Coeur + 3 plaques", 8, 44, TEXT, false);
            graphics.drawString(font, "requis", 8, 55, TEXT, false);
        }

        graphics.drawString(font, "Tampon " + menu.getBuffer(), 96, 55, TEXT, false);

        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, TEXT, false);
    }
}
