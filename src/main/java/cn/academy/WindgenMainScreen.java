package cn.academy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ecran du rotor de l'eolienne : un emplacement pour l'helice et l'etat de la
 * zone balayee.
 *
 * Rendu procedural, comme les autres ecrans du port, en attendant une texture.
 */
public class WindgenMainScreen extends AbstractContainerScreen<WindgenMainMenu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int HOLE = 0xFF373737;
    private static final int TEXT = 0x404040;
    private static final int OK = 0x2E7D32;
    private static final int OFF = 0x9E2B25;

    public WindgenMainScreen(WindgenMainMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);

        int slotX = leftPos + WindgenMainMenu.SLOT_X;
        int slotY = topPos + WindgenMainMenu.SLOT_Y;
        graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, SLOT_BG);
        graphics.fill(slotX, slotY, slotX + 16, slotY + 16, HOLE);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, TEXT, false);

        boolean fan = menu.isFanInstalled();
        graphics.drawString(font, fan ? "Helice installee" : "Helice absente", 8, 22, fan ? OK : OFF,
                false);

        boolean clear = menu.isNoObstacle();
        graphics.drawString(font, clear ? "Pales degagees" : "Pales obstruees", 8, 34, clear ? OK : OFF,
                false);

        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, TEXT, false);
    }
}
