package cn.academy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ecran du brouilleur d'aptitudes : l'emplacement d'energie, l'etat, le rayon a
 * regler et le cout que cela represente.
 *
 * Rendu procedural, comme les autres ecrans du port, en attendant une texture.
 *
 * <h2>Pourquoi le cout est affiche en clair ici</h2>
 *
 * Le rayon se paie au carre : passer de 10 a 20 blocs quadruple la facture. Un
 * joueur qui ne verrait que le rayon ne comprendrait pas pourquoi sa machine
 * s'eteint toute seule. Le cout par tick est donc ecrit a cote, en clair.
 */
public class AbilityInterfererScreen extends AbstractContainerScreen<AbilityInterfererMenu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int HOLE = 0xFF373737;
    private static final int TEXT = 0x404040;
    private static final int ENERGY = 0xFFE0A030;
    private static final int OK = 0x2E7D32;
    private static final int OFF = 0x9E2B25;

    private static final int BAR_X = 8;
    private static final int BAR_Y = 20;
    private static final int BAR_WIDTH = 10;
    private static final int BAR_HEIGHT = 50;

    private static final int ROW_1 = 22;
    private static final int ROW_2 = 34;
    private static final int ROW_3 = 46;

    private static final int BUTTON_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 16;
    private static final int BUTTON_Y = 64;

    public AbilityInterfererScreen(AbilityInterfererMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(Button.builder(Component.literal("-"), b -> press(AbilityInterfererMenu.BUTTON_RANGE_DOWN))
                .bounds(leftPos + 96, topPos + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.literal("-" + AbilityInterfererBlockEntity.RANGE_STEP + " blocs")))
                .build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> press(AbilityInterfererMenu.BUTTON_RANGE_UP))
                .bounds(leftPos + 120, topPos + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.literal("+" + AbilityInterfererBlockEntity.RANGE_STEP + " blocs")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("academy.interferer.action.toggle"),
                        b -> press(AbilityInterfererMenu.BUTTON_TOGGLE))
                .bounds(leftPos + 8, topPos + BUTTON_Y, 80, BUTTON_HEIGHT)
                .build());
    }

    /**
     * Libelle fixe pour le bouton d'allumage.
     *
     * Un libelle qui dependrait de l'etat ne se mettrait pas a jour : les widgets
     * sont construits une fois, a l'ouverture de l'ecran. C'est le texte d'etat, lui
     * redessine a chaque image, qui dit ce qu'il en est.
     */
    private Component toggleLabel() {
        return Component.translatable("academy.interferer.action.toggle");
    }

    /** Les boutons de conteneur passent par le chemin vanilla : aucun paquet maison. */
    private void press(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);

        int slotX = leftPos + AbilityInterfererMenu.SLOT_X;
        int slotY = topPos + AbilityInterfererMenu.SLOT_Y;
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

        // L'etat passe avant tout : un brouilleur eteint, c'est justement ce que le
        // joueur doit voir quand il se demande pourquoi plus rien ne repond.
        graphics.drawString(font, Component.translatable(menu.isEnabled()
                        ? "academy.interferer.state.on" : "academy.interferer.state.off"),
                24, ROW_1, menu.isEnabled() ? OK : OFF, false);

        graphics.drawString(font, menu.getRange() + " blocs", 24, ROW_2, TEXT, false);
        graphics.drawString(font, (long) menu.getCostPerTick() + " / tick", 24, ROW_3,
                menu.canPay() ? TEXT : OFF, false);

        graphics.drawString(font, menu.getAffectedCount() + " joueurs", 24, 58, TEXT, false);
        graphics.drawString(font, menu.isLinkedToNode() ? "Noeud: raccorde" : "Noeud: aucun",
                100, ROW_1, TEXT, false);

        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, TEXT, false);
    }
}
