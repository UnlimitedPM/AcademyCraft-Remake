package cn.academy;

import cn.academy.client.gui.cgui.CGuiContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ecran du rotor de l'eolienne : le fond de la machine, l'emplacement de l'helice, et l'altitude.
 *
 * <p>C'est la premiere machine a passer sur le moteur CGui, celui des ecrans de l'original. Le
 * cadre, les onglets, la page et le panneau d'infos viennent de {@link CGuiContainerScreen} ; tout
 * ce qui reste ici tient en deux lignes, et c'est exactement ce que faisait l'original.
 */
public class WindgenMainScreen extends CGuiContainerScreen<WindgenMainMenu> {

    public WindgenMainScreen(WindgenMainMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /**
     * Le rotor n'a qu'une chose a dire : l'altitude a laquelle il tourne, qui est toute sa
     * production. L'original tenait en une ligne — {@code ret.infoPage.property("altitude",
     * tile.getPos.getY)} — et il n'y a rien a y ajouter : pas d'inventaire de machine au-dela de
     * l'helice, pas de reseau, pas de barre. La page est celle de l'inventaire, avec le fond de la
     * machine et son unique onglet.
     */
    @Override
    protected void build(CGuiContainerScreen<WindgenMainMenu> screen) {
        inventoryPage("windmain");
        property(Component.translatable("ac.gui.prop.altitude").getString(),
                Integer.toString(menu.altitude()));
    }
}
