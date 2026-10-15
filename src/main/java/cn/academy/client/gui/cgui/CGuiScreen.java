package cn.academy.client.gui.cgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * L'ecran qui dessine un document CGui : le squelette de {@code CGuiScreen} de l'original.
 *
 * <p>Il pose l'arbre une fois par image — c'est {@link CGuiLayout} qui calcule ou chaque widget
 * tombe — puis le dessine dans l'ordre du fichier, et route la souris et le clavier. Les composants
 * se dessinent dans l'ordre ou CGui les empilait : le fond ou la texture, puis la teinte, puis le
 * cerne, puis le texte.
 *
 * <h2>Ce qui est repris, et ce qui ne l'est pas</h2>
 *
 * <p>Repris : le placement (ancrage, pivot, echelle), la teinte qui s'allume au survol, le texte
 * avec ses alignements, le champ de saisie avec son focus, et la touche Entree qui valide.
 * Laisse : le glissement des widgets, leurs animations internes, et la reflexion par nom de classe
 * — un document qui demanderait autre chose que les cinq composants lus par {@link CGuiDocument}
 * verrait simplement ces composants-la manquer.
 *
 * <h2>Les textures</h2>
 *
 * <p>Celles du mod font quarante-huit pixels de cote — icones, coche, croix — et le fichier les
 * pose dans des boites de trente-trois unites, soit huit pixels. C'est donc la texture ENTIERE qui
 * est etiree dans le widget, et non son coin : le port dessinait son coin haut-gauche, ce qui
 * donnait a ses icones un air de tache.
 */
@OnlyIn(Dist.CLIENT)
public abstract class CGuiScreen extends Screen {

    /** La taille des textures d'interface du mod, en pixels : voir le commentaire de la classe. */
    private static final int TEXTURE = 48;

    private final CWidget root;
    private final Map<CWidget, Runnable> clicks = new IdentityHashMap<>();

    /** Le champ qui prend le clavier, ou {@code null}. */
    private CWidget focused;

    protected CGuiScreen(Component title, String document) {
        super(title);
        this.root = CGuiDocument.read(document).copy();
    }

    /** L'arbre du document, copie : chaque ecran modifie le sien. */
    protected CWidget root() {
        return root;
    }

    /** Pose l'arbre sur l'ecran. A appeler avant de lire la moindre position. */
    protected void layout() {
        CGuiLayout.place(root, 0, 0, width, height, 1.0);
    }

    /** Le widget de ce nom, cherche dans tout l'arbre. */
    protected CWidget find(String name) {
        return find(root, name);
    }

    private static CWidget find(CWidget widget, String name) {
        if (widget.name().equals(name)) return widget;
        for (CWidget child : widget.children()) {
            CWidget found = find(child, name);
            if (found != null) return found;
        }
        return null;
    }

    /** Le widget le plus profond sous ce point, ou {@code null}. */
    protected CWidget hitTest(double mouseX, double mouseY) {
        return hitTest(root, mouseX, mouseY);
    }

    private static CWidget hitTest(CWidget widget, double mouseX, double mouseY) {
        if (!widget.transform.doesDraw) return null;
        if (!widget.isPointWithin(mouseX, mouseY)) return null;

        for (CWidget child : widget.children()) {
            CWidget deeper = hitTest(child, mouseX, mouseY);
            if (deeper != null) return deeper;
        }
        return widget;
    }

    /** Ce que ce widget fait quand on clique dessus. */
    protected void onClick(CWidget widget, Runnable action) {
        clicks.put(widget, action);
    }

    /** Le champ qui prend le clavier, ou rien pour le rendre. */
    protected void focus(CWidget box) {
        if (focused != null && focused.text != null) focused.text.content = "";
        focused = box;
        if (box != null && box.text != null) box.text.content = "";
    }

    protected CWidget focused() {
        return focused;
    }

    /** Ce que la touche Entree fait dans le champ : c'est l'ecran qui decide. */
    protected void confirm(CWidget box) {
    }

    /** Les couleurs du moment : appele avant chaque dessin, souris en main. */
    protected void frame(GuiGraphics graphics, double mouseX, double mouseY) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        layout();
        frame(graphics, mouseX, mouseY);
        drawWidget(graphics, root, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** Un widget, puis ses enfants — l'ordre du fichier, qui est celui du dessin. */
    protected void drawWidget(GuiGraphics graphics, CWidget widget, double mouseX, double mouseY) {
        if (!widget.transform.doesDraw || !widget.transform.enabled) return;

        int x = (int) Math.round(widget.absX);
        int y = (int) Math.round(widget.absY);
        int w = (int) Math.round(widget.absW);
        int h = (int) Math.round(widget.absH);

        if (w > 0 && h > 0) {
            if (widget.texture != null) {
                int colour = argb(widget.texture.alpha, widget.texture.red, widget.texture.green,
                        widget.texture.blue);
                if (widget.texture.texture == null) {
                    graphics.fill(x, y, x + w, y + h, colour);
                } else {
                    drawTexture(graphics, widget.texture.texture, x, y, w, h, colour);
                }
            }
            if (widget.tint != null) {
                int[] colour = widget.isPointWithin(mouseX, mouseY) ? widget.tint.hover
                        : widget.tint.idle;
                if (colour[3] > 0) {
                    graphics.fill(x, y, x + w, y + h, argb(colour[3], colour[0], colour[1],
                            colour[2]));
                }
            }
            if (widget.outline != null) {
                int colour = argb(widget.outline.alpha, widget.outline.red, widget.outline.green,
                        widget.outline.blue);
                int line = Math.max(1, (int) Math.round(widget.outline.lineWidth
                        * widget.absScale));
                outline(graphics, x, y, w, h, line, colour);
            }
            if (widget.text != null) {
                drawText(graphics, widget, x, y, w, h);
            }
        }

        for (CWidget child : widget.children()) {
            drawWidget(graphics, child, mouseX, mouseY);
        }
    }

    private static void drawTexture(GuiGraphics graphics, String path, int x, int y, int w, int h,
                                    int colour) {
        ResourceLocation texture = ResourceLocation.tryParse(path);
        if (texture == null) return;

        graphics.setColor(((colour >> 16) & 0xFF) / 255f, ((colour >> 8) & 0xFF) / 255f,
                (colour & 0xFF) / 255f, ((colour >>> 24) & 0xFF) / 255f);
        graphics.blit(texture, x, y, w, h, 0f, 0f, TEXTURE, TEXTURE, TEXTURE, TEXTURE);
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    /** Un carre creux : le cerne du fichier, qui se dessine sur les quatre bords. */
    private static void outline(GuiGraphics graphics, int x, int y, int w, int h, int line,
                                int colour) {
        graphics.fill(x, y, x + w, y + line, colour);
        graphics.fill(x, y + h - line, x + w, y + h, colour);
        graphics.fill(x, y + line, x + line, y + h - line, colour);
        graphics.fill(x + w - line, y + line, x + w, y + h - line, colour);
    }

    /** Le texte d'un widget, ou son indice quand il est vide, avec ses alignements. */
    private void drawText(GuiGraphics graphics, CWidget widget, int x, int y, int w, int h) {
        CWidget.TextBox box = widget.text;
        boolean empty = box.content == null || box.content.isEmpty();
        String content = empty && box.hint != null ? box.hint : box.content;
        if (content == null || content.isEmpty()) return;

        int colour = argb(box.alpha, box.red, box.green, box.blue);
        int textWidth = font.width(content);
        int textX = switch (box.align.toUpperCase(java.util.Locale.ROOT)) {
            case "CENTER" -> x + (w - textWidth) / 2;
            case "RIGHT" -> x + w - textWidth;
            default -> x;
        };
        int textY = switch (box.heightAlign.toUpperCase(java.util.Locale.ROOT)) {
            case "CENTER" -> y + (h - 8) / 2;
            case "BOTTOM" -> y + h - 9;
            default -> y;
        };

        graphics.drawString(font, content, textX, textY, colour, false);

        // Le curseur du champ : un trait apres la derniere lettre, tant qu'il a le clavier.
        if (focused == widget && box.allowEdit && (System.currentTimeMillis() / 500) % 2 == 0) {
            graphics.fill(textX + textWidth + 1, textY, textX + textWidth + 2, textY + 9, colour);
        }
    }

    private static int argb(int alpha, int red, int green, int blue) {
        return (clamp(alpha) << 24) | (clamp(red) << 16) | (clamp(green) << 8) | clamp(blue);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;

        CWidget hit = hitTest(mouseX, mouseY);
        for (CWidget widget = hit; widget != null; widget = parentOf(widget)) {
            Runnable action = clicks.get(widget);
            if (action != null) {
                action.run();
                return true;
            }
        }
        // Un clic qui ne touche aucun geste rend le clavier : c'est ce que faisait l'original en
        // retirant le focus des qu'on cliquait ailleurs.
        if (hit != focused) focus(null);
        return hit != null;
    }

    private CWidget parentOf(CWidget widget) {
        return parentOf(root, widget);
    }

    private static CWidget parentOf(CWidget current, CWidget target) {
        for (CWidget child : current.children()) {
            if (child == target) return current;
            CWidget found = parentOf(child, target);
            if (found != null) return found;
        }
        return null;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (focused != null && focused.text != null && focused.text.allowEdit
                && focused.text.content.length() < 16 && codePoint >= ' ') {
            focused.text.content += codePoint;
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focused != null && focused.text != null && focused.text.allowEdit) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE
                    && !focused.text.content.isEmpty()) {
                focused.text.content = focused.text.content.substring(0,
                        focused.text.content.length() - 1);
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
                confirm(focused);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Le gestionnaire de textures, pour les ecrans qui veulent savoir si leur image est prete. */
    protected static Minecraft minecraft() {
        return Minecraft.getInstance();
    }
}
