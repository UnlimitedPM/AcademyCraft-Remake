package cn.academy.compat.jei;

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
 * <h2>L'icone du mode a l'echelle 1</h2>
 *
 * Les quatre icones de mode sont des dessins de 48 de cote (une scie circulaire pour INCISE, un
 * stylet pour ETCH...) et l'ecran de la machine les reduit a 24 : a cette taille il n'en reste
 * qu'une cloche blanche, verifie a l'ecran dans la machine comme dans JEI. La page leur donne donc
 * une case de 48, a l'echelle 1, ou le dessin se lit enfin.
 */
final class MachinePlaque implements IDrawable {

    /** Une image posee telle quelle, a une position de la page. */
    record Picture(ResourceLocation texture, int width, int height, int x, int y) {}

    /** L'icone d'un mode, dessinee a sa taille native sur un fond sombre. */
    record Icon(ResourceLocation texture, int x, int y) {}

    /** Un texte, centre entre deux colonnes de la page. */
    record Label(String text, int left, int right, int y) {}

    /** La couleur du texte : du gris fonce, sur le gris clair de la planche. */
    private static final int TEXT_COLOUR = 0xFF303030;

    /** Le fond sombre de la case d'icone : c'est celui de l'ecran de la machine. */
    private static final int ICON_BACKDROP = 0xFF2A2A32;
    private static final int ICON_BORDER = 0xFF9AA0AA;

    /** La taille native des icones de mode. */
    static final int ICON_SOURCE = 48;

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
        graphics.blit(page.texture(), xOffset + page.x(), yOffset + page.y(),
                page.width(), page.height(), 0f, 0f,
                page.width(), page.height(), page.width(), page.height());

        if (icon != null) {
            // Au plus proche voisin, comme la machine : ce sont des dessins fins, un filtrage les
            // moyenne en une tache blanche.
            int x = xOffset + icon.x();
            int y = yOffset + icon.y();
            graphics.fill(x - 1, y - 1, x + ICON_SOURCE + 1, y + ICON_SOURCE + 1, ICON_BORDER);
            graphics.fill(x, y, x + ICON_SOURCE, y + ICON_SOURCE, ICON_BACKDROP);
            graphics.blit(icon.texture(), x, y, ICON_SOURCE, ICON_SOURCE, 0f, 0f,
                    ICON_SOURCE, ICON_SOURCE, ICON_SOURCE, ICON_SOURCE);
        }

        if (label != null) {
            var font = Minecraft.getInstance().font;
            int centred = label.left() + (label.right() - label.left() - font.width(label.text())) / 2;
            graphics.drawString(font, label.text(), xOffset + centred, yOffset + label.y(),
                    TEXT_COLOUR, false);
        }
    }
}
