package cn.academy;

import cn.academy.client.gui.cgui.CGuiContainerScreen;
import cn.academy.client.gui.cgui.CGuiHist;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ecran du generateur de phase : la page de la machine, et son histogramme.
 *
 * <p>Deux barres a droite des slots, comme l'original : le tampon d'energie, et la phase liquide
 * qu'il brule. La jauge que le port dessinait a la main — et le mot d'etat a cote du titre — sont
 * partis avec : la barre de phase dit exactement ce que la cuve contient, et c'est ce que le mod
 * de 1.12.2 affichait.
 */
public class PhaseGeneratorScreen extends CGuiContainerScreen<PhaseGeneratorMenu> {

    public PhaseGeneratorScreen(PhaseGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /**
     * L'original, en deux appels : {@code histEnergy(() => tile.getEnergy, tile.bufferSize)}, puis un
     * element ecrit a la main pour la phase liquide — couleur {@code 0xffb983fb}, valeur sur la
     * taille de la cuve, et « %d mB ».
     *
     * <p>Son nom est « IF », en dur dans l'original : l'auteur avait oublie de mettre la cle de la
     * phase liquide. On le garde tel quel — c'est ce que le mod affiche — et la cle inconnue ressort
     * simplement sous cette forme.
     */
    @Override
    protected void build(CGuiContainerScreen<PhaseGeneratorMenu> screen) {
        inventoryPage("phasegen");

        histogram(
                CGuiHist.energy(menu::getEnergyStored, menu.getMaxEnergyStored()),
                CGuiHist.custom("IF", 0xFFB983FB,
                        () -> (double) menu.getLiquidAmount() / menu.getTankSize(),
                        () -> menu.getLiquidAmount() + " mB"));
    }
}
