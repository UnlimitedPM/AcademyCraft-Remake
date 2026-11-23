package cn.academy;

import cn.academy.client.gui.cgui.CGuiContainerScreen;
import cn.academy.client.gui.cgui.CGuiHist;
import cn.academy.client.gui.cgui.CGuiWireless;
import cn.academy.client.gui.cgui.CWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * Ecran du generateur solaire : sa propre page, le cadre anime de l'etat du ciel, et son panneau.
 *
 * <p>C'est le premier ecran qui apporte une <b>planche animee</b> : l'original decoupait
 * {@code effect_solar.png} en trois images l'une sous l'autre et montrait celle de l'etat du ciel —
 * plein soleil, soleil voile, ou rien. Le moteur sait maintenant demander une fenetre de texture, ce
 * qui servira aussi au noeud d'imagination et a ses dix images.
 */
public class SolarGenScreen extends CGuiContainerScreen<SolarGenMenu> {

    /** La planche des trois images : plein soleil, arrete, soleil voile. */
    private static final String EFFECT = "academy:textures/guis/effect/effect_solar.png";

    public SolarGenScreen(SolarGenMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /**
     * L'original : {@code InventoryPage(main)} sur le document {@code page_solar} — la page de la
     * machine EST celle de l'inventaire — puis un histogramme du tampon, un separateur, et la vitesse
     * de production.
     */
    @Override
    protected void build(CGuiContainerScreen<SolarGenMenu> screen) {
        page("inv", "rework/page_solar");

        histogram(CGuiHist.buffer(menu::getEnergyStored, menu.getMaxEnergyStored()));
        seplineInfo();
        property(Component.translatable("ac.gui.common.prop.gen_speed").getString(),
                String.format(Locale.ROOT, "%.2fIF/T", menu.generation()));

        // L'onglet sans fil de l'original, pose en dernier : le moteur ouvre la premiere page posee,
        // et une machine doit s'ouvrir sur sa page d'inventaire.
        new CGuiWireless(screen).install();
    }

    /**
     * L'image du cadre, une fois par image : le tiers haut pour un ciel degage, celui du milieu quand
     * rien ne produit, le dernier sous la pluie. C'est le {@code v} de l'original, au tiers pres.
     */
    @Override
    protected void frame(GuiGraphics graphics, double mouseX, double mouseY) {
        CWidget anim = widget("anim_frame");
        if (anim == null) return;

        // Le fichier de l'original ne lui donne aucune image : il dessinait son quad a la main, sans
        // passer par un composant. On lui en pose donc une ici si elle manque — sans quoi le widget
        // existe mais ne dessine rien, et le soleil ne se voit pas.
        if (anim.texture == null) {
            anim.texture = new CWidget.DrawTexture();
        }

        anim.texture.texture = EFFECT;
        anim.texture.v0 = switch (menu.sky()) {
            case STRONG -> 0.0;
            case STOPPED -> 1.0 / 3.0;
            case WEAK -> 2.0 / 3.0;
        };
        anim.texture.v1 = anim.texture.v0 + 1.0 / 3.0;
    }
}
