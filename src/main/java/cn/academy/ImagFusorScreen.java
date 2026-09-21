package cn.academy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ecran du fusor d'Imag : cinq emplacements, une jauge d'energie a gauche, une
 * jauge de phase liquide a droite et la progression de la fusion au milieu.
 *
 * Rendu procedural, comme les autres ecrans du port, en attendant une texture.
 *
 * <h2>Pourquoi les chiffres sont dans des infobulles</h2>
 *
 * La zone de la machine fait 176 de large et s'arrete a 70 de haut, et elle est
 * presque entierement occupee par les emplacements et les deux jauges. Il ne
 * reste aucune colonne assez large pour ecrire "8000 / 8000 mB" sans passer sous
 * les emplacements de l'inventaire. Les valeurs sont donc affichees au survol des
 * jauges, ce qui est aussi la convention des autres interfaces du jeu.
 */
public class ImagFusorScreen extends AbstractContainerScreen<ImagFusorMenu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int HOLE = 0xFF373737;
    private static final int TEXT = 0x404040;
    private static final int ENERGY = 0xFFE0A030;
    private static final int PHASE = 0xFF7A4FBF;
    private static final int PROGRESS = 0xFF4CAF50;

    private static final int GAUGE_Y = 20;
    private static final int GAUGE_WIDTH = 10;
    private static final int GAUGE_HEIGHT = 46;

    private static final int ENERGY_X = 8;
    private static final int TANK_X = 134;

    private static final int BAR_X = 44;
    private static final int BAR_Y = 44;
    private static final int BAR_WIDTH = 68;
    private static final int BAR_HEIGHT = 6;

    public ImagFusorScreen(ImagFusorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);

        int[][] slots = {
                {ImagFusorMenu.INPUT_X, ImagFusorMenu.TOP_SLOT_Y},
                {ImagFusorMenu.OUTPUT_X, ImagFusorMenu.TOP_SLOT_Y},
                {ImagFusorMenu.IMAG_INPUT_X, ImagFusorMenu.BOTTOM_SLOT_Y},
                {ImagFusorMenu.ENERGY_INPUT_X, ImagFusorMenu.BOTTOM_SLOT_Y},
                {ImagFusorMenu.IMAG_OUTPUT_X, ImagFusorMenu.BOTTOM_SLOT_Y}};
        for (int[] slot : slots) {
            slotBackground(graphics, leftPos + slot[0], topPos + slot[1]);
        }

        gauge(graphics, leftPos + ENERGY_X, topPos + GAUGE_Y, menu.getEnergyStored(), menu.getMaxEnergyStored(),
                ENERGY);
        gauge(graphics, leftPos + TANK_X, topPos + GAUGE_Y, menu.getLiquidAmount(), menu.getTankSize(), PHASE);

        // Barre de progression, entre les deux lignes d'emplacements.
        int barX = leftPos + BAR_X;
        int barY = topPos + BAR_Y;
        graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, HOLE);
        int done = (int) (BAR_WIDTH * menu.getProgress());
        graphics.fill(barX, barY, barX + done, barY + BAR_HEIGHT, PROGRESS);
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

        // Etat du raccordement, a droite du titre : la seule place libre de la
        // zone haute qui ne recouvre ni un emplacement ni une jauge.
        Component link = Component.literal(menu.isLinkedToNode() ? "Noeud: raccorde" : "Noeud: aucun");
        graphics.drawString(font, link, imageWidth - 8 - font.width(link), 6, TEXT, false);

        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, TEXT, false);
    }
}
