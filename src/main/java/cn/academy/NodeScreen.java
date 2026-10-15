package cn.academy;

import cn.academy.client.gui.cgui.CGuiContainerScreen;
import cn.academy.client.gui.cgui.CGuiHist;
import cn.academy.client.gui.cgui.CWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ecran du noeud sans fil : sa page, son animation, et ce qu'il transporte.
 *
 * <p>La page de l'inventaire, comme les autres machines — l'original appelait
 * {@code InventoryPage("node")}, soit {@code page_inv} avec le fond du noeud. Ce qui est propre au
 * noeud, c'est l'<b>animation</b> posee au milieu : dix images dans {@code effect_node.png}, huit pour
 * un noeud raccorde a un reseau et deux pour un noeud seul.
 *
 * <h2>Ce que l'animation dit</h2>
 *
 * <p>Les deux etats ne different pas seulement par leurs images mais par leur <b>rythme</b> : le noeud
 * raccorde bat toutes les 800 ms, le noeud seul clignote toutes les 3 secondes. Et l'ensemble respire,
 * de la meme opacite que les fonds de page, ce que l'original obtenait en donnant {@code breatheAlpha}
 * au dessin de ses images.
 *
 * <p>L'etat est lu sur le block entity, qui le recoit du serveur : le noeud cherche son Matrix tout
 * seul, donc l'ecran n'a aucune question a poser, contrairement a l'original qui interrogeait le
 * serveur toutes les deux secondes.
 */
public class NodeScreen extends CGuiContainerScreen<NodeMenu> {

    /** La planche des dix images, l'une sous l'autre. */
    private static final String EFFECT = "academy:textures/guis/effect/effect_node.png";

    /** Combien d'images elle porte : la planche fait 186 sur 750, donc dix fois 186 sur 75. */
    private static final int BOARD_FRAMES = 10;

    /** Le noeud raccorde a un reseau : les huit premieres images, toutes les 800 ms. */
    private static final int LINKED_FRAMES = 8;
    private static final int LINKED_HOLD_MS = 800;

    /** Le noeud seul : les deux dernieres, toutes les 3 secondes — il clignote plus lentement. */
    private static final int UNLINKED_FRAMES = 2;
    private static final int UNLINKED_HOLD_MS = 3000;

    /** L'etat que l'ecran est en train d'animer, et ou il en est. */
    private boolean animatingLinked;
    private int frame;
    private double frameAt = Double.NaN;

    public NodeScreen(NodeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /**
     * L'original : {@code InventoryPage("node")}, une animation posee par-dessus, puis un histogramme
     * de l'energie et de la charge, un separateur, et la portee du signal.
     */
    @Override
    protected void build(CGuiContainerScreen<NodeMenu> screen) {
        CWidget page = inventoryPage("node");
        page.add(animation());

        histogram(
                CGuiHist.energy(menu::energy, menu.maxEnergy()),
                CGuiHist.capacity(menu::load, menu.capacity()));

        seplineInfo();
        property(Component.translatable("ac.gui.common.prop.range").getString(),
                Integer.toString(menu.range()));
    }

    /**
     * L'animation, aux mesures de l'original : 186 sur 75 posee a 42 et 35,5, et dessinee a moitie
     * taille. Le document de la page ne la porte pas — l'original la construisait a la main et
     * l'accrochait a sa fenetre — donc elle nait ici, et la fenetre est celle que rend la page.
     */
    private CWidget animation() {
        CWidget anim = new CWidget("anim_node");
        anim.transform.x = 42;
        anim.transform.y = 35.5;
        anim.transform.width = 186;
        anim.transform.height = 75;
        anim.transform.scale = 0.5;

        anim.texture = new CWidget.DrawTexture();
        anim.texture.texture = EFFECT;
        // La premiere image tout de suite : sans fenetre, le moteur dessinerait la planche entiere,
        // soit les dix images ecrasees l'une sur l'autre.
        anim.texture.v0 = 0.0;
        anim.texture.v1 = 1.0 / BOARD_FRAMES;

        return anim;
    }

    /**
     * Une image de l'animation par image de jeu, tant que son temps est ecoule : c'est le
     * {@code StateContext} de l'original, qui remettait le compteur a zero quand le noeud changeait
     * d'etat.
     */
    @Override
    protected void frame(GuiGraphics graphics, double mouseX, double mouseY) {
        CWidget anim = widget("anim_node");
        if (anim == null || anim.texture == null) return;

        double now = net.minecraft.Util.getMillis() / 1000.0;
        boolean linked = menu.isLinked();

        if (Double.isNaN(frameAt) || linked != animatingLinked) {
            // Premier passage, ou le noeud vient d'etre raccorde — ou de perdre son Matrix : on
            // repart de la premiere image de l'etat et on lui laisse son temps plein.
            animatingLinked = linked;
            frame = 0;
            frameAt = now;
        } else if ((now - frameAt) * 1000.0 >= holdMillis(linked)) {
            frameAt = now;
            frame = (frame + 1) % frameCount(linked);
        }

        // La fenetre de texture : la planche est coupee en dix, et l'etat choisit ou commencent ses
        // images — 0 pour le noeud raccorde, 8 pour le noeud seul.
        int index = begin(linked) + frame;
        anim.texture.v0 = (double) index / BOARD_FRAMES;
        anim.texture.v1 = (double) (index + 1) / BOARD_FRAMES;

        // Et elle respire comme les fonds de page, d'une seule opacite pour toute l'image.
        anim.texture.alpha = breatheAlpha();
    }

    /** Ou commencent les images de l'etat. */
    private static int begin(boolean linked) {
        return linked ? 0 : LINKED_FRAMES;
    }

    /** Combien d'images il parcourt. */
    private static int frameCount(boolean linked) {
        return linked ? LINKED_FRAMES : UNLINKED_FRAMES;
    }

    /** Et combien de temps chacune reste a l'ecran. */
    private static int holdMillis(boolean linked) {
        return linked ? LINKED_HOLD_MS : UNLINKED_HOLD_MS;
    }
}
