package cn.academy.compat.jei;

import com.mojang.blaze3d.systems.RenderSystem;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * La page d'une recette : la planche NEI de l'original, plus ce que la recette y ajoute.
 *
 * <h2>Pourquoi ce n'est pas dans {@code draw()}</h2>
 *
 * JEI 15 peint l'exterieur de la recette dans cet ordre : les widgets demandes par
 * {@code createRecipeExtras}, puis les emplacements (les objets), puis {@code draw}. Autrement dit
 * tout ce que dessine {@code draw} passe PAR-DESSUS les objets — on ne peut donc pas y poser la
 * planche (elle cacherait les objets), et une planche posee en widget cache, elle, ce qu'on
 * dessinerait dans {@code draw}. Tout va donc ensemble, dans le widget.
 *
 * <p>Et la planche se dessine a la main plutot que par {@code IGuiHelper.createDrawable} : celle-ci
 * passe par l'atlas de textures de JEI, ou nos images ne sont pas, et ne dessinait rien du tout
 * (verifie a l'ecran : ni planche, ni erreur).
 *
 * <h2>IL FAUT RALLUMER LE MELANGE</h2>
 *
 * C'est le piege qui a coute le plus cher ici. Les icones du mod sont des dessins BLANCS dont les
 * bords sont ANTICRENELES : leur transparence fait tout le dessin. Sans melange actif, chaque pixel
 * opaque sort blanc plein et la transparence disparait — l'icone devient une tache blanche, ce que
 * le joueur a decrit mot pour mot (« le blanc n'a pas de transparence »). On l'allume donc avant
 * chaque texture, comme le fait le moteur d'interface des machines (`CGuiView`).
 */
final class MachinePlaque implements IDrawable {

    /** Une image posee telle quelle, a une position de la page. */
    record Picture(ResourceLocation texture, int width, int height, int x, int y) {}

    /** L'icone d'un mode : sa case, et sa taille dans cette case. */
    record Icon(ResourceLocation texture, int x, int y, int size) {}

    /** Un texte, centre entre deux colonnes de la page. */
    record Label(String text, int left, int right, int y) {}

    /** La couleur du texte : du gris fonce, sur le gris clair de la planche. */
    private static final int TEXT_COLOUR = 0xFF303030;

    /** Le fond de la case d'icone : le sombre de l'ecran de la machine. */
    private static final int ICON_BACKDROP = 0xFF2A2A32;

    /** La taille de l'image source des icones de mode. */
    private static final int ICON_SOURCE = 48;

    private final int width;
    private final int height;
    private final Picture page;
    private final Icon icon;
    private final Label label;

    MachinePlaque(int width, int height, Picture page, Icon icon, Label label) {
        this.width = width;
        this.height = height;
        this.page = page;
        this.icon = icon;
        this.label = label;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        blend();
        graphics.blit(page.texture(), xOffset + page.x(), yOffset + page.y(),
                page.width(), page.height(), 0f, 0f,
                page.width(), page.height(), page.width(), page.height());

        if (icon != null) {
            // La case de la planche est claire, et les icones sont blanches : on leur remet le fond
            // sombre de l'ecran de la machine, sinon elles ne s'y voient pas du tout.
            int x = xOffset + icon.x();
            int y = yOffset + icon.y();
            graphics.fill(x, y, x + icon.size(), y + icon.size(), ICON_BACKDROP);
            blend();
            graphics.blit(icon.texture(), x, y, icon.size(), icon.size(), 0f, 0f,
                    ICON_SOURCE, ICON_SOURCE, ICON_SOURCE, ICON_SOURCE);
        }

        if (label != null) {
            var font = Minecraft.getInstance().font;
            int centred = label.left() + (label.right() - label.left() - font.width(label.text())) / 2;
            graphics.drawString(font, label.text(), xOffset + centred, yOffset + label.y(),
                    TEXT_COLOUR, false);
        }
    }

    /** Rallume le melange : un {@code fill} ou un {@code drawString} le coupe en se terminant. */
    private static void blend() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }
}
