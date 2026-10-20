package cn.academy.compat.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Le fond d'une page de recette : un rectangle sombre et, autour de chaque emplacement, son cadre.
 *
 * <p>Pourquoi pas le fond blanc que JEI pose par defaut : les objets clairs du mod — l'aiguille, le
 * silbarn, les cristaux — sont presque blancs et disparaissaient dessus. Un fond sombre les rend
 * tous lisibles, et rappelle celui de nos machines.
 *
 * <p>Les emplacements sont donnes en coordonnees relatives a la page : un objet de 16 pixels centre
 * sur son cadre, comme partout dans le jeu.
 */
final class JeiBackground implements IDrawable {

    private static final int SLOT = 18;

    private static final int BORDER = 0xFF20242B;
    private static final int INNER = 0xFF6E7783;

    private final int width;
    private final int height;
    private final int[] slots;

    private JeiBackground(int width, int height, int[] slots) {
        this.width = width;
        this.height = height;
        this.slots = slots;
    }

    /**
     * @param slots couples {@code x, y} des coins hauts gauches des objets a encadrer
     */
    static JeiBackground of(int width, int height, int... slots) {
        return new JeiBackground(width, height, slots);
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
        graphics.fill(xOffset, yOffset, xOffset + width, yOffset + height, 0xFF10131A);
        graphics.fill(xOffset + 1, yOffset + 1, xOffset + width - 1, yOffset + height - 1, 0xFF191D26);

        for (int i = 0; i + 1 < slots.length; i += 2) {
            int x = xOffset + slots[i];
            int y = yOffset + slots[i + 1];
            graphics.fill(x - 1, y - 1, x - 1 + SLOT, y - 1 + SLOT, BORDER);
            graphics.fill(x, y, x + SLOT - 2, y + SLOT - 2, INNER);
            graphics.fill(x, y, x + SLOT - 2, y + SLOT - 3, 0xFF88919D);
        }
    }
}
