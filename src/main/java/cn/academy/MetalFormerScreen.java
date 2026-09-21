package cn.academy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ecran du formeur de metal : trois emplacements, une barre de progression, une
 * barre d'energie et les deux boutons de mode.
 *
 * Rendu procedural, comme les autres ecrans du port, en attendant une texture.
 * L'original avait une vraie texture et une icone par mode
 * ({@code guis/icons/icon_former_<mode>.png}) ; le nom du mode est ecrit en
 * toutes lettres ici.
 *
 * <h2>Placement</h2>
 *
 * La zone de la machine est etroite : 176 de large, dont les emplacements et le
 * titre du joueur qui commence a 72. Tout ce qui est dessine ici tient donc entre
 * 20 et 70 de haut, et le reste part a droite des emplacements.
 */
public class MetalFormerScreen extends AbstractContainerScreen<MetalFormerMenu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int HOLE = 0xFF373737;
    private static final int TEXT = 0x404040;
    private static final int ENERGY = 0xFFE0A030;
    private static final int PROGRESS = 0xFF4CAF50;

    private static final int ENERGY_X = 8;
    private static final int ENERGY_Y = 20;
    private static final int ENERGY_WIDTH = 10;
    private static final int ENERGY_HEIGHT = 50;

    private static final int PROGRESS_X = 66;
    private static final int PROGRESS_Y = 34;
    private static final int PROGRESS_WIDTH = 30;
    private static final int PROGRESS_HEIGHT = 12;

    private static final int BUTTON_Y = 40;
    private static final int BUTTON_LEFT_X = 100;
    private static final int BUTTON_RIGHT_X = 152;
    private static final int BUTTON_SIZE = 16;

    public MetalFormerScreen(MetalFormerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(Button.builder(Component.literal("<"),
                        button -> sendMode(MetalFormerMenu.BUTTON_MODE_PREVIOUS))
                .bounds(leftPos + BUTTON_LEFT_X, topPos + BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE)
                .build());
        addRenderableWidget(Button.builder(Component.literal(">"),
                        button -> sendMode(MetalFormerMenu.BUTTON_MODE_NEXT))
                .bounds(leftPos + BUTTON_RIGHT_X, topPos + BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE)
                .build());
    }

    /**
     * Les boutons de conteneur passent par le chemin vanilla : aucun paquet
     * maison, le clic est route vers {@code MetalFormerMenu.clickMenuButton}.
     */
    private void sendMode(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);

        slotBackground(graphics, leftPos + MetalFormerMenu.IN_SLOT_X, topPos + MetalFormerMenu.SLOT_Y);
        slotBackground(graphics, leftPos + MetalFormerMenu.OUT_SLOT_X, topPos + MetalFormerMenu.SLOT_Y);
        slotBackground(graphics, leftPos + MetalFormerMenu.IN_SLOT_X, topPos + MetalFormerMenu.BATTERY_SLOT_Y);

        // Barre d'energie, remplie du bas vers le haut, comme les autres ecrans.
        int barX = leftPos + ENERGY_X;
        int barY = topPos + ENERGY_Y;
        graphics.fill(barX, barY, barX + ENERGY_WIDTH, barY + ENERGY_HEIGHT, HOLE);
        int max = Math.max(1, menu.getMaxEnergyStored());
        int filled = (int) (ENERGY_HEIGHT * ((float) menu.getEnergyStored() / max));
        graphics.fill(barX, barY + ENERGY_HEIGHT - filled, barX + ENERGY_WIDTH, barY + ENERGY_HEIGHT, ENERGY);

        // Barre de progression.
        int progressX = leftPos + PROGRESS_X;
        int progressY = topPos + PROGRESS_Y;
        graphics.fill(progressX, progressY, progressX + PROGRESS_WIDTH, progressY + PROGRESS_HEIGHT, HOLE);
        int done = (int) (PROGRESS_WIDTH * menu.getProgress());
        graphics.fill(progressX, progressY, progressX + done, progressY + PROGRESS_HEIGHT, PROGRESS);
    }

    private void slotBackground(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_BG);
        graphics.fill(x, y, x + 16, y + 16, HOLE);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // Le nom du mode est centre entre les deux boutons.
        Component mode = Component.translatable("academy.metal_former.mode." + menu.getMode().getId());
        int middle = (BUTTON_LEFT_X + BUTTON_SIZE + BUTTON_RIGHT_X) / 2;
        graphics.drawString(font, mode, leftPos + middle - font.width(mode) / 2, topPos + 44, TEXT, false);

        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, TEXT, false);
        graphics.drawString(font, menu.getEnergyStored() + " / " + menu.getMaxEnergyStored(), 24, 62, TEXT, false);
        graphics.drawString(font, menu.isLinkedToNode() ? "Noeud: raccorde" : "Noeud: aucun", 24, 20, TEXT, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, TEXT, false);
    }
}
