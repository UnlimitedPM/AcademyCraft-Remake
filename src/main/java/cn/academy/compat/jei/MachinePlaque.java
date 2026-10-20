package cn.academy.compat.jei;

import cn.academy.client.gui.GuiTextures;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * La planche d'une machine, telle que l'original l'avait dessinee pour NEI, plus ce que la recette
 * y ajoute.
 *
 * <h2>Pourquoi ce n'est pas dans {@code draw()}</h2>
 *
 * JEI 15 peint l'exterieur de la recette dans cet ordre : les widgets demandes par
 * {@code createRecipeExtras}, puis les emplacements (les objets), puis {@code draw}. Autrement dit
 * <b>tout ce que dessine {@code draw} passe PAR-DESSUS les objets</b> — on ne peut donc pas y poser
 * la planche (elle cacherait les objets), et une planche posee en widget cache, elle, l'icone et le
 * libelle ecrits dans {@code draw}. Les trois vont donc ensemble, dans le widget.
 *
 * <p>Et la planche se dessine a la main plutot que par {@code IGuiHelper.createDrawable} : celle-ci
 * passe par l'atlas de textures de JEI, ou nos images ne sont pas, et ne dessinait rien du tout
 * (verifie a l'ecran : ni planche, ni erreur).
 */
final class MachinePlaque implements IDrawable {

    /** L'icone d'un mode, posee dans une case de la planche (son image source fait 48 de cote). */
    record Icon(ResourceLocation texture, int x, int y, int size) {}

    /** Un texte, centre entre deux colonnes de la planche. */
    record Label(String text, int left, int right, int y) {}

    /** La couleur du texte : du gris fonce, sur le gris clair de la planche. */
    private static final int TEXT_COLOUR = 0xFF303030;

    /** Le fond sombre de la case d'icone : c'est celui de l'ecran de la machine. */
    private static final int ICON_BACKDROP = 0xFF2A2A32;
    private static final int ICON_BORDER = 0xFF9AA0AA;

    private final ResourceLocation page;
    private final int width;
    private final int height;
    private final Icon icon;
    private final Label label;

    MachinePlaque(ResourceLocation page, int width, int height, Icon icon, Label label) {
        this.page = page;
        this.width = width;
        this.height = height;
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
        graphics.blit(page, xOffset, yOffset, width, height, 0f, 0f, width, height, width, height);

        if (icon != null) {
            // Les icones sont dessinees en BLANC dans le mod, sur le fond sombre de l'ecran de la
            // machine : posees telles quelles sur le gris clair de la planche, elles faisaient un
            // carre blanc illisible. On leur redonne le fond de la machine, et on les dessine a sa
            // taille (24 chez elle, 22 ici, la place que la planche laisse entre ses deux fleches).
            int x = xOffset + icon.x();
            int y = yOffset + icon.y();
            int size = icon.size();
            graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, ICON_BORDER);
            graphics.fill(x, y, x + size, y + size, ICON_BACKDROP);
            GuiTextures.smoothMipmapped(icon.texture());
            graphics.blit(icon.texture(), x, y, size, size, 0f, 0f, 48, 48, 48, 48);
        }

        if (label != null) {
            var font = Minecraft.getInstance().font;
            int centred = label.left() + (label.right() - label.left() - font.width(label.text())) / 2;
            graphics.drawString(font, label.text(), xOffset + centred, yOffset + label.y(),
                    TEXT_COLOUR, false);
        }
    }
}
