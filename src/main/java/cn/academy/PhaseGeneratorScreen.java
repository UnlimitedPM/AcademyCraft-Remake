package cn.academy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ecran du generateur de phase : trois emplacements, une jauge d'energie a
 * gauche, une jauge de phase a droite.
 *
 * Rendu procedural, comme les autres ecrans du port, en attendant une texture.
 *
 * <h2>Placement</h2>
 *
 * La zone de la machine s'arrete a 70 de haut et les trois emplacements
 * l'occupent presque entierement : il ne reste aucune colonne assez large pour
 * un message d'etat. Les valeurs sont donc dans les infobulles des jauges, et
 * l'etat tient en deux mots a cote du titre.
 */
public class PhaseGeneratorScreen extends AbstractContainerScreen<PhaseGeneratorMenu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int HOLE = 0xFF373737;
    private static final int TEXT = 0x404040;
    private static final int ENERGY = 0xFFE0A030;
    private static final int PHASE = 0xFF7A4FBF;
    private static final int OK = 0x2E7D32;
    private static final int OFF = 0x9E2B25;

    private static final int GAUGE_Y = 20;
    private static final int GAUGE_WIDTH = 10;
    private static final int GAUGE_HEIGHT = 50;

    private static final int ENERGY_X = 8;
    private static final int TANK_X = 150;

    public PhaseGeneratorScreen(PhaseGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);

        slotBackground(graphics, leftPos + PhaseGeneratorMenu.LIQUID_IN_X, topPos + PhaseGeneratorMenu.TOP_SLOT_Y);
        slotBackground(graphics, leftPos + PhaseGeneratorMenu.LIQUID_OUT_X, topPos + PhaseGeneratorMenu.TOP_SLOT_Y);
        slotBackground(graphics, leftPos + PhaseGeneratorMenu.OUTPUT_X, topPos + PhaseGeneratorMenu.OUTPUT_SLOT_Y);

        gauge(graphics, leftPos + ENERGY_X, topPos + GAUGE_Y, menu.getEnergyStored(),
                menu.getMaxEnergyStored(), ENERGY);
        gauge(graphics, leftPos + TANK_X, topPos + GAUGE_Y, menu.getLiquidAmount(), menu.getTankSize(), PHASE);
    }

    /** Jauge verticale remplie du bas vers le haut. */
    private void gauge(GuiGraphics graphics, int x, int y, int amount, int max, int colour) {
        graphics.fill(x, y, x + GAUGE_WIDTH, y + GAUGE_HEIGHT, HOLE);
        if (max <= 0) return;
        int filled = (int) (GAUGE_HEIGHT * Math.min(1.0f, (float) amount / max));
        graphics.fill(x, y + GAUGE_HEIGHT - filled, x + GAUGE_WIDTH, y + GAUGE_HEIGHT, colour);
    }

    private void slotBackground(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_BG);
        graphics.fill(x, y, x + 16, y + 16, HOLE);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (isHovering(ENERGY_X, GAUGE_Y, GAUGE_WIDTH, GAUGE_HEIGHT, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.literal(
                    menu.getEnergyStored() + " / " + menu.getMaxEnergyStored() + " Imag"), mouseX, mouseY);
        } else if (isHovering(TANK_X, GAUGE_Y, GAUGE_WIDTH, GAUGE_HEIGHT, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.literal(
                    menu.getLiquidAmount() + " / " + menu.getTankSize() + " mB"), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, TEXT, false);

        // Etat en deux mots, a droite du titre : la seule place libre de la zone
        // haute qui ne recouvre ni un emplacement ni une jauge.
        Component state = Component.translatable(menu.isProducing()
                ? "academy.phase_generator.state.on"
                : "academy.phase_generator.state.off");
        graphics.drawString(font, state, imageWidth - 8 - font.width(state), 6,
                menu.isProducing() ? OK : OFF, false);

        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, TEXT, false);
    }
}
