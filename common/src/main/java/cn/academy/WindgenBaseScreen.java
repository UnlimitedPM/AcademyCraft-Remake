package cn.academy;

import cn.academy.client.gui.cgui.CGuiContainerScreen;
import cn.academy.client.gui.cgui.CGuiHist;
import cn.academy.client.gui.cgui.CGuiWireless;
import cn.academy.client.gui.cgui.CWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ecran de la base de l'eolienne : sa page, et ses trois icones qu'on allume.
 *
 * <p>La colonne se lit d'un coup d'oeil : l'icone de la base est toujours allumee, celle du milieu
 * s'allume des qu'il y a des piliers, celle du haut quand le rotor est en place. C'est l'original qui
 * pilotait leur opacite — deux dixiemes pour ce qui manque, six pour une colonne finie qui ne tourne
 * pas, pleine pour ce qui est en place — et l'ecran du port fait exactement la meme chose.
 */
public class WindgenBaseScreen extends CGuiContainerScreen<WindgenBaseMenu> {

    public WindgenBaseScreen(WindgenBaseMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /**
     * L'original : {@code InventoryPage(main)} sur {@code page_windbase}, un histogramme du tampon,
     * un separateur, et l'altitude.
     */
    @Override
    protected void build(CGuiContainerScreen<WindgenBaseMenu> screen) {
        page("inv", "rework/page_windbase");

        histogram(CGuiHist.buffer(menu::getEnergyStored, menu.getMaxEnergyStored()));
        seplineInfo();
        property(Component.translatable("ac.gui.common.prop.altitude").getString(),
                Integer.toString(menu.altitude()));

        // L'onglet sans fil de l'original, pose en dernier : le moteur ouvre la premiere page posee,
        // et une machine doit s'ouvrir sur sa page d'inventaire.
        new CGuiWireless(screen).install();
    }

    /**
     * Ce que la colonne a de monte, une fois par image. Les trois nombres sont ceux de l'original :
     * une piece manquante reste a deux dixiemes, un rotor en place mais a l'arret monte a six
     * dixiemes, et tout ce qui est en place est a pleine opacite.
     */
    @Override
    protected void frame(GuiGraphics graphics, double mouseX, double mouseY) {
        float[] alpha = switch (menu.getCompleteness()) {
            case COMPLETE_NOT_WORKING -> new float[] { 0.6f, 1.0f, 1.0f };
            case COMPLETE -> new float[] { 1.0f, 1.0f, 1.0f };
            case NO_TOP -> new float[] { 0.2f, 1.0f, 1.0f };
            case BASE_ONLY -> new float[] { 0.2f, 0.2f, 1.0f };
        };

        light("icon_main", alpha[0]);
        light("icon_middle", alpha[1]);
        light("icon_base", alpha[2]);
    }

    /** Une icone de la colonne : celles du fichier sont blanches, on ne regle que leur opacite. */
    private void light(String name, float alpha) {
        CWidget icon = widget(name);
        if (icon == null) return;

        // Comme le cadre du generateur solaire : l'original dessinait dans un ecouteur, donc le
        // widget du fichier peut etre nu. On lui pose une image si elle manque.
        if (icon.texture == null) {
            icon.texture = new CWidget.DrawTexture();
        }
        icon.texture.alpha = Math.round(alpha * 255f);
    }
}
