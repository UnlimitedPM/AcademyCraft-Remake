package cn.academy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ecran de la base de l'eolienne : un emplacement, l'etat de la colonne, et une
 * barre d'energie a droite.
 *
 * Rendu procedural, comme les autres ecrans du port, en attendant une texture.
 *
 * <h2>Placement</h2>
 *
 * La zone de la machine s'arrete a 70 de haut, le titre de l'inventaire du joueur
 * et les emplacements commencant juste apres. Le texte d'etat tient dans la
 * colonne de gauche, la barre d'energie est rejetee a droite, et les chiffres de
 * la barre sont dans une infobulle au survol : c'est la meme contrainte que sur
 * le fusor, et la meme reponse.
 */
public class WindgenBaseScreen extends AbstractContainerScreen<WindgenBaseMenu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int HOLE = 0xFF373737;
    private static final int TEXT = 0x404040;
    private static final int ENERGY = 0xFFE0A030;
    private static final int OK = 0x2E7D32;
    private static final int OFF = 0x9E2B25;

    private static final int BAR_X = 150;
    private static final int BAR_Y = 20;
    private static final int BAR_WIDTH = 12;
    private static final int BAR_HEIGHT = 50;

    public WindgenBaseScreen(WindgenBaseMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);

        int slotX = leftPos + WindgenBaseMenu.SLOT_X;
        int slotY = topPos + WindgenBaseMenu.SLOT_Y;
        graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, SLOT_BG);
        graphics.fill(slotX, slotY, slotX + 16, slotY + 16, HOLE);

        int barX = leftPos + BAR_X;
        int barY = topPos + BAR_Y;
        graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, HOLE);
        int max = Math.max(1, menu.getMaxEnergyStored());
        int filled = (int) (BAR_HEIGHT * ((float) menu.getEnergyStored() / max));
        graphics.fill(barX, barY + BAR_HEIGHT - filled, barX + BAR_WIDTH, barY + BAR_HEIGHT, ENERGY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (isHovering(BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.literal(
                    menu.getEnergyStored() + " / " + menu.getMaxEnergyStored() + " Imag"), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, TEXT, false);

        boolean producing = menu.getCompleteness() == WindgenBaseBlockEntity.Completeness.COMPLETE;
        graphics.drawString(font, statusText(menu.getCompleteness()), 8, 22, producing ? OK : OFF, false);
        graphics.drawString(font, menu.isLinkedToNode() ? "Noeud: raccorde" : "Noeud: aucun",
                8, 34, TEXT, false);

        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, TEXT, false);
    }

    /**
     * Traduit l'etat de la colonne. Le texte vient des cles de langue, pour que
     * le mod reste traduisible ; les quatre etats sont ceux de l'original.
     */
    private Component statusText(WindgenBaseBlockEntity.Completeness completeness) {
        return Component.translatable("academy.windgen_base.status."
                + completeness.name().toLowerCase(java.util.Locale.ROOT));
    }
}
