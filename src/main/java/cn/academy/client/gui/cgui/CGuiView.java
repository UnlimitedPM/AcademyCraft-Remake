package cn.academy.client.gui.cgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Le coeur du moteur CGui : un document, ses widgets, et les gestes qui vont avec.
 *
 * <p>C'est ce qui etait dans {@code CGuiScreen}, sorti de la pour servir aussi aux ecrans de
 * <b>machines</b>. La raison est la meme que chez l'original, dont le {@code CGuiScreenContainer}
 * derivoir de {@code GuiContainer} et non de {@code CGuiScreen} : un ecran de machine doit rester un
 * {@code AbstractContainerScreen}, parce que c'est le conteneur qui porte ses <b>slots</b> et son
 * menu. Les deux familles partagent donc le moteur, pas l'ecran.
 *
 * <h2>Le repere</h2>
 *
 * <p>{@link #place} pose l'arbre a partir d'une origine, en pixels : l'ecran de competence la donne
 * en haut a gauche de l'ecran, et le conteneur la cale sur {@code guiLeft / guiTop} pour que le
 * document et les slots tombent dans le meme repere. C'est tout le mystere du {@code +31 / +20} de
 * l'original : les deux trames n'en font qu'une.
 */
@OnlyIn(Dist.CLIENT)
public final class CGuiView {

    /** La taille des textures d'interface du mod, en pixels : elles font 48 de cote. */
    private static final int TEXTURE = 48;

    private final Screen screen;
    private final CWidget root;
    private final Map<CWidget, Runnable> clicks = new IdentityHashMap<>();

    private CWidget focused;

    /** L'origine du repere, en pixels : le parent du widget racine. */
    private double originX;
    private double originY;

    public CGuiView(Screen screen, String document) {
        this.screen = screen;
        this.root = CGuiDocument.read(document).copy();
    }

    /**
     * Le meme, sur un arbre deja construit : c'est ce que fait un ecran de machine, qui pose ses
     * onglets, ses pages et son panneau d'infos lui-meme dans une racine a lui.
     */
    public CGuiView(Screen screen, CWidget root) {
        this.screen = screen;
        this.root = root;
    }

    /** L'arbre du document, copie : chaque ecran modifie le sien. */
    public CWidget root() {
        return root;
    }

    public CWidget focused() {
        return focused;
    }

    /**
     * L'ancre du widget racine, en pixels : l'ecran de competence la pose au milieu de l'ecran, le
     * conteneur de machine sur le coin de ses slots.
     */
    public void origin(double x, double y) {
        this.originX = x;
        this.originY = y;
    }

    /** Pose l'arbre. A appeler avant de lire la moindre position. */
    public void place() {
        CGuiLayout.place(root, originX, originY, 0, 0, 1.0);
    }

    /** Le widget de ce nom, cherche dans tout l'arbre. */
    public CWidget find(String name) {
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
    public CWidget hitTest(double mouseX, double mouseY) {
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
    public void onClick(CWidget widget, Runnable action) {
        clicks.put(widget, action);
    }

    /** Le champ qui prend le clavier, ou rien pour le rendre. */
    public void focus(CWidget box) {
        if (focused != null && focused.text != null) focused.text.content = "";
        focused = box;
        if (box != null && box.text != null) box.text.content = "";
    }

    /** Ce que la touche Entree fait dans le champ : c'est l'ecran qui decide. */
    public void confirm(CWidget box) {
    }

    public void draw(GuiGraphics graphics, double mouseX, double mouseY) {
        drawWidget(graphics, root, mouseX, mouseY);
    }

    /** Un widget, puis ses enfants — l'ordre du fichier, qui est celui du dessin. */
    private void drawWidget(GuiGraphics graphics, CWidget widget, double mouseX, double mouseY) {
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
            if (widget.bar != null) {
                drawBar(graphics, widget, x, y, w, h);
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

    /**
     * Une barre de progression : la portion de sa texture qui correspond au remplissage, ou un
     * aplat de sa couleur quand elle n'en a pas. Le morceau vient de {@link CGuiLayout#bar}.
     */
    private static void drawBar(GuiGraphics graphics, CWidget widget, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;

        CGuiLayout.Bar bar = CGuiLayout.bar(widget.bar, w, h);
        if (bar.w() <= 0 || bar.h() <= 0) return;

        int[] color = widget.bar.color;
        ResourceLocation texture = widget.bar.texture == null ? null
                : ResourceLocation.tryParse(widget.bar.texture);
        if (texture == null) {
            graphics.fill(x + (int) Math.round(bar.x()), y + (int) Math.round(bar.y()),
                    x + (int) Math.round(bar.x() + bar.w()),
                    y + (int) Math.round(bar.y() + bar.h()),
                    argb(color[3], color[0], color[1], color[2]));
            return;
        }

        graphics.setColor(color[0] / 255f, color[1] / 255f, color[2] / 255f, color[3] / 255f);
        graphics.blit(texture, x + (int) Math.round(bar.x()), y + (int) Math.round(bar.y()),
                (int) Math.round(bar.w()), (int) Math.round(bar.h()),
                (float) (bar.u() * TEXTURE), (float) (bar.v() * TEXTURE),
                (int) Math.round(bar.uw() * TEXTURE), (int) Math.round(bar.vh() * TEXTURE),
                TEXTURE, TEXTURE);
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

        // Un champ de mot de passe se dessine masque : le vrai texte ne sort jamais de l'ecran.
        if (box.doesEcho && !empty) {
            content = String.valueOf(box.echoChar).repeat(box.content.length());
        }

        int colour = argb(box.alpha, box.red, box.green, box.blue);
        int textWidth = screen.getMinecraft().font.width(content);
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

        graphics.drawString(screen.getMinecraft().font, content, textX, textY, colour, false);

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

    /** Un clic, routé au widget qui l'ecoute — le plus proche d'abord, puis ses parents. */
    public boolean click(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        CWidget hit = hitTest(mouseX, mouseY);
        for (CWidget widget = hit; widget != null; widget = parentOf(widget)) {
            Runnable action = clicks.get(widget);
            if (action != null) {
                action.run();
                return true;
            }
        }
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

    public boolean charTyped(char codePoint) {
        if (focused != null && focused.text != null && focused.text.allowEdit
                && focused.text.content.length() < 16 && codePoint >= ' ') {
            focused.text.content += codePoint;
            return true;
        }
        return false;
    }

    /** Vrai quand un champ editable a le clavier : l'ecran bloque alors le clavier du jeu. */
    public boolean editing() {
        return focused != null && focused.text != null && focused.text.allowEdit;
    }

    public boolean keyPressed(int keyCode) {
        if (!editing()) return false;

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
        return false;
    }
}
