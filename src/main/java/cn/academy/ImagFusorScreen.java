package cn.academy;

import cn.academy.client.gui.cgui.CGuiContainerScreen;
import cn.academy.client.gui.cgui.CGuiHist;
import cn.academy.client.gui.cgui.CWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ecran du fusor d'Imag : sa page, sa barre de fusion, et ce que la recette reclame.
 *
 * <p>La page fait tout le travail : elle porte le dessin de la machine, la barre de fusion et le
 * nombre de phase a fournir. L'ecran ne fait que leur donner leur valeur, une fois par image, en les
 * prenant par leur nom dans le document — exactement comme l'original, qui les cherchait par
 * {@code getWidget("progress")} et {@code getWidget("text_imagneeded")}.
 */
public class ImagFusorScreen extends CGuiContainerScreen<ImagFusorMenu> {

    public ImagFusorScreen(ImagFusorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /**
     * L'original : {@code InventoryPage(template.copy())} sur {@code page_imagfusor}, puis un
     * histogramme a deux barres — l'energie et la phase liquide — sur {@code histEnergy} et
     * {@code histPhaseLiquid}.
     */
    @Override
    protected void build(CGuiContainerScreen<ImagFusorMenu> screen) {
        page("inv", "rework/page_imagfusor");

        histogram(
                CGuiHist.energy(menu::getEnergyStored, menu.getMaxEnergyStored()),
                CGuiHist.liquid(menu::getLiquidAmount, menu.getTankSize()));
    }

    /**
     * Deux choses de la page changent d'une image a l'autre : la barre de fusion, et le nombre
     * d'unites de phase que la recette reclame — « IDLE » tant qu'aucune n'est engagee.
     */
    @Override
    protected void frame(GuiGraphics graphics, double mouseX, double mouseY) {
        CWidget progress = widget("progress");
        if (progress != null && progress.bar != null) {
            progress.bar.progress = menu.getProgress();
        }

        CWidget needed = widget("text_imagneeded");
        if (needed != null && needed.text != null) {
            needed.text.content = menu.hasRecipe() ? Integer.toString(menu.imagNeeded()) : "IDLE";
        }
    }
}
