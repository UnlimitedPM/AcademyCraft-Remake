package cn.academy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Minimal procedural GUI (no custom texture yet): battery slot + energy bar. */
public class SolarGenScreen extends AbstractContainerScreen<SolarGenMenu> {

    private static final int BAR_X = 70, BAR_Y = 20, BAR_WIDTH = 12, BAR_HEIGHT = 50;

    public SolarGenScreen(SolarGenMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);
        graphics.fill(x + 7, y + 17, x + imageWidth - 7, y + imageHeight - 96, 0xFF8B8B8B);

        int barX = x + BAR_X, barY = y + BAR_Y;
        graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0xFF404040);
        int max = Math.max(1, menu.getMaxEnergyStored());
        int filled = (int) (BAR_HEIGHT * ((float) menu.getEnergyStored() / max));
        graphics.fill(barX, barY + BAR_HEIGHT - filled, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0xFFE0A030);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0x404040, false);
        graphics.drawString(font, menu.getEnergyStored() + " / " + menu.getMaxEnergyStored() + " FE",
                8, imageHeight - 96, 0x404040, false);
        graphics.drawString(font, menu.isLinkedToNode() ? "Noeud: raccorde" : "Noeud: aucun",
                8, imageHeight - 84, 0x404040, false);
    }
}
