package cn.academy;

import cn.academy.ability.Category;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ecran du developeur d'aptitudes.
 *
 * Une ligne par categorie, avec son niveau et un bouton pour lancer la prochaine
 * etape. En dessous, la progression de l'apprentissage en cours et son etat.
 *
 * Rendu procedural, comme les autres ecrans du port, en attendant une texture.
 *
 * <h2>Pourquoi les couts sont dans des infobulles</h2>
 *
 * Une ligne de categorie ne fait que 126 pixels de large, et il faut y faire tenir
 * le nom de la categorie et son niveau. Le prix et le nombre de stimulations sont
 * donc sur le bouton, au survol. C'est la meme contrainte que sur le fusor, et la
 * meme reponse.
 */
public class DeveloperScreen extends AbstractContainerScreen<DeveloperMenu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int HOLE = 0xFF373737;
    private static final int TEXT = 0x404040;
    private static final int ENERGY = 0xFFE0A030;
    private static final int PROGRESS = 0xFF4CAF50;
    private static final int OK = 0x2E7D32;
    private static final int OFF = 0x9E2B25;

    private static final int ROW_Y = 18;
    private static final int ROW_HEIGHT = 16;
    private static final int LABEL_X = 24;
    private static final int BUTTON_X = 154;
    private static final int BUTTON_WIDTH = 16;
    private static final int BUTTON_HEIGHT = 14;

    private static final int ENERGY_X = 8;
    private static final int ENERGY_WIDTH = 10;

    private static final int BAR_X = 24;
    private static final int BAR_Y = 86;
    private static final int BAR_WIDTH = 136;
    private static final int BAR_HEIGHT = 8;

    public DeveloperScreen(DeveloperMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 202;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        for (int i = 0; i < menu.getCategoryCount(); i++) {
            final int categoryId = i;
            Category category = menu.getCategory(i);
            String name = category == null ? "?" : category.getName();

            Button button = Button.builder(Component.literal("+"), b -> develop(categoryId))
                    .bounds(leftPos + BUTTON_X, topPos + ROW_Y + i * ROW_HEIGHT - 3,
                            BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build();

            // Au niveau maximal il n'y a plus rien a developper : griser plutot que
            // de laisser un bouton qui ne ferait rien. Tout est grise aussi pendant
            // un apprentissage : la machine n'en mene qu'un a la fois.
            button.active = menu.getCategoryLevel(i) < menu.getMaxLevel() && !menu.isDeveloping();
            button.setTooltip(Tooltip.create(Component.literal(name + " : "
                    + menu.getStimulationsFor(i) + " stimulations, "
                    + (long) menu.getCostFor(i) + " Imag")));
            addRenderableWidget(button);
        }
    }

    /**
     * Les boutons de conteneur passent par le chemin vanilla : aucun paquet
     * maison, le clic est route vers {@code DeveloperMenu.clickMenuButton}.
     */
    private void develop(int categoryId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, categoryId);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);

        int rowsHeight = menu.getCategoryCount() * ROW_HEIGHT;
        int barX = leftPos + ENERGY_X;
        int barY = topPos + ROW_Y - 3;
        graphics.fill(barX, barY, barX + ENERGY_WIDTH, barY + rowsHeight, HOLE);
        int max = Math.max(1, menu.getMaxEnergyStored());
        int filled = (int) (rowsHeight * Math.min(1.0f, (float) menu.getEnergyStored() / max));
        graphics.fill(barX, barY + rowsHeight - filled, barX + ENERGY_WIDTH, barY + rowsHeight, ENERGY);

        int progressX = leftPos + BAR_X;
        int progressY = topPos + BAR_Y;
        graphics.fill(progressX, progressY, progressX + BAR_WIDTH, progressY + BAR_HEIGHT, HOLE);
        int done = (int) (BAR_WIDTH * menu.getProgress());
        graphics.fill(progressX, progressY, progressX + done, progressY + BAR_HEIGHT, PROGRESS);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, TEXT, false);

        for (int i = 0; i < menu.getCategoryCount(); i++) {
            Category category = menu.getCategory(i);
            String name = category == null ? "?" : category.getName();
            int level = menu.getCategoryLevel(i);

            // La categorie en cours d'apprentissage est mise en avant.
            boolean active = menu.isDeveloping() && menu.getDevelopingCategory() == i;
            int colour = active ? OK : TEXT;

            graphics.drawString(font, name + "  " + level + " / " + menu.getMaxLevel(),
                    LABEL_X, ROW_Y + i * ROW_HEIGHT, colour, false);
        }

        Component status = statusText();
        graphics.drawString(font, status, BAR_X, BAR_Y + BAR_HEIGHT + 4,
                menu.getState() == DeveloperBlockEntity.DevState.FAILED ? OFF : TEXT, false);

        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, TEXT, false);
    }

    /** Message d'etat, dans la langue du joueur. */
    private Component statusText() {
        DeveloperBlockEntity.DevState state = menu.getState();
        if (state == DeveloperBlockEntity.DevState.DEVELOPING) {
            return Component.translatable("academy.developer.state.developing")
                    .append(" : " + (long) (menu.getProgress() * 100) + " %");
        }
        return Component.translatable("academy.developer.state."
                + state.name().toLowerCase(java.util.Locale.ROOT));
    }
}
