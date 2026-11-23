package cn.academy;

import cn.academy.client.gui.AcademyFonts;
import cn.academy.client.gui.cgui.CGuiContainerScreen;
import cn.academy.client.gui.cgui.CGuiHist;
import cn.academy.client.gui.cgui.CGuiWireless;
import cn.academy.client.gui.cgui.CWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ecran du formeur de metal : sa page, l'icone de son mode avec ses deux fleches, et la barre de travail.
 *
 * <p>C'etait le dernier ecran du port a tout dessiner a la main : des rectangles gris, le nom du mode
 * ecrit en toutes lettres, et l'aveu dans son propre commentaire — « en attendant une texture ». Le
 * document de l'original, lui, etait deja dans le port, identique au bit pres : trois emplacements, une
 * barre de progression, une icone de mode et deux fleches.
 *
 * <h2>Le changement de mode</h2>
 *
 * <p>L'original passait par un message reseau a lui ({@code MFNetDelegate}, canal {@code alt}) et
 * attendait la reponse du serveur pour changer son icone. Le port n'en a pas besoin : le conteneur a
 * deja ses identifiants de bouton, et un clic part dans le paquet de bouton de vanilla, que le serveur
 * route vers {@code MetalFormerMenu.clickMenuButton}. Moins de code, et surtout l'icone vient du
 * SERVEUR — elle ne peut donc pas dire autre chose que ce que la machine fait vraiment.
 */
public class MetalFormerScreen extends CGuiContainerScreen<MetalFormerMenu> {

    /**
     * Une icone par mode : l'original avait les quatre fichiers, et le port les avait deja aussi —
     * mais son ecran procedural ne s'en servait pas, et ecrivait le nom du mode a la place.
     */
    private static final String MODE_ICON = "academy:textures/guis/icons/icon_former_%s.png";

    public MetalFormerScreen(MetalFormerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /**
     * L'original : {@code InventoryPage(main)} sur {@code page_metalformer} — la page de la machine EST
     * celle de l'inventaire — puis l'histogramme de son TAMPON, comme le generateur solaire.
     *
     * <p>Sauf que le tampon du formeur n'est pas celui du solaire : l'original demande
     * {@code histEnergy}, le bleu du tampon ordinaire, et le port demandait {@code histBuffer} — sa
     * copie du generateur solaire. La barre etait donc d'un bleu plus clair, et sa legende annoncait
     * « buffer » au lieu d'« energy » : « pour certaines machines elles n'ont meme pas la meme couleur
     * et le texte qui dit a quoi correspond la barre n'est pas le bon non plus ».
     */
    @Override
    protected void build(CGuiContainerScreen<MetalFormerMenu> screen) {
        page("inv", "rework/page_metalformer");

        histogram(CGuiHist.energy(menu::getEnergyStored, menu.getMaxEnergyStored()));

        // Les deux fleches : le clic part par le chemin vanilla des boutons de conteneur.
        onClick(widget("btn_left"), () -> clickButton(MetalFormerMenu.BUTTON_MODE_PREVIOUS));
        onClick(widget("btn_right"), () -> clickButton(MetalFormerMenu.BUTTON_MODE_NEXT));

        // L'onglet sans fil de l'original, pose en dernier : le moteur ouvre la premiere page posee,
        // et une machine doit s'ouvrir sur sa page d'inventaire.
        new CGuiWireless(screen).install();
    }

    /**
     * Ce qui change d'une image a l'autre : la barre de travail et l'icone du mode.
     *
     * <p>Le mode n'est pas a suivre ici : il vient du serveur par le conteneur, donc l'icone suit ce
     * que la machine fait, et non ce que le client croit.
     */
    @Override
    protected void frame(GuiGraphics graphics, double mouseX, double mouseY) {
        CWidget progress = widget("progress");
        if (progress != null && progress.bar != null) {
            // Par paliers, comme l'original : il ne synchronisait son compteur que tous les dix ticks,
            // donc six morceaux. Voir MetalFormerBlockEntity.PROGRESS_STEPS.
            progress.bar.progress = MetalFormerBlockEntity.displayProgress(menu.getProgress());
        }

        CWidget icon = widget("icon_mode");
        if (icon == null) return;
        // Le fichier de l'original lui donne une image par defaut, mais elle est posee par le composant
        // du noeud voisin et pas par celui-ci : on lui en pose donc une ici si elle manque, sans quoi
        // l'icone ne se dessinerait pas du tout.
        if (icon.texture == null) icon.texture = new CWidget.DrawTexture();
        icon.texture.texture = MODE_ICON.formatted(menu.getMode().getId());
    }

    /**
     * Le nom du mode, quand la souris passe sur son icone.
     *
     * <p>C'est la boite de l'original ({@link AcademyFonts#drawBox}) : un noir a moitie, la police
     * du mod, et le nom ecrit dedans. Le port se servait de l'infobulle du jeu en se disant qu'elle
     * faisait le meme office — elle ne le fait pas, ni pour la police ni pour l'habillage.
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        CWidget icon = widget("icon_mode");
        if (icon == null) return;
        if (mouseX < icon.absX || mouseX >= icon.absX + icon.absW) return;
        if (mouseY < icon.absY || mouseY >= icon.absY + icon.absH) return;

        // L'original ne passait pas par l'infobulle du jeu : son `icon_mode` dessinait, dans son
        // FrameEvent, un `TechUI.drawTextBox` — le nom du mode sur un noir a moitie, avec la police
        // du mod (`FontOption(10)`) et non celle du jeu, a six unites de l'icone et dix au-dessus.
        // Le joueur a releve les deux : « la police d'ecriture n'a pas ete changee pour ce texte
        // la », puis « le texte s'affiche avec l'affichage basique alors que normalement il y a un
        // affichage particulier pour ca ».
        AcademyFonts.drawBox(graphics, font,
                Component.translatable("academy.metal_former.mode." + menu.getMode().getId()).getString(),
                icon.absX + 6, icon.absY - 10, AcademyFonts.GUI_SIZE);
    }

    /**
     * Le clic d'un bouton de conteneur, par le chemin vanilla : le serveur le route vers
     * {@code MetalFormerMenu.clickMenuButton}, qui change le mode. Aucun paquet maison a ecrire.
     */
    private void clickButton(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }
}
