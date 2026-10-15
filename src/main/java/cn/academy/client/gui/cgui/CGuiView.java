package cn.academy.client.gui.cgui;

import com.mojang.blaze3d.systems.RenderSystem;
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
 * <p>{@link #place} pose l'arbre dans un parent, en pixels : l'ecran de competence lui donne l'ecran
 * entier, et le conteneur de machine son cadre — celui des slots, puisque c'est le seul repere que
 * les deux ont en commun. C'est tout le mystere du {@code +31 / +20} de l'original : une seule
 * trame pour les deux, et un demi-pixel qui tombe juste.
 */
@OnlyIn(Dist.CLIENT)
public final class CGuiView {

    /**
     * La taille des textures de <b>barre</b>, en pixels : une barre echantillonne une portion de son
     * image, donc il faut une base pour la compter. Les fonds de page, eux, ne decoupent rien.
     */
    private static final int TEXTURE = 48;

    /** Le cote d'un morceau du halo : la texture en fait trois, une par cote et une au centre. */
    private static final int QUAD = TEXTURE / 3;

    private final Screen screen;
    private final CWidget root;
    private final Map<CWidget, Runnable> clicks = new IdentityHashMap<>();

    private CWidget focused;

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

    /** Pose l'arbre en plein ecran : c'est ce que fait un ecran de competence. */
    public void place() {
        place(0, 0, screen.width, screen.height);
    }

    /**
     * Pose l'arbre dans un parent donne, en pixels : c'est ce que fait un ecran de machine, dont
     * le parent est son conteneur. A appeler avant de lire la moindre position.
     */
    public void place(double parentX, double parentY, double parentW, double parentH) {
        CGuiLayout.place(root, parentX, parentY, parentW, parentH, 1.0);
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
        blend();
        drawWidget(graphics, root, mouseX, mouseY);
    }

    /**
     * Le melange, a poser avant <b>chaque</b> texture.
     *
     * <p>L'original l'activait lui aussi — GL_BLEND, source sur un moins source — et il le FAUT : les
     * textures du mod sont BLANCHES et c'est leur canal alpha qui porte tout le dessin. Mesure sur
     * les fichiers : `line.png` plafonne a 71 sur 255, `parent_background.png` est un noir a moitie
     * (128), `blend_quad.png` un halo qui s'efface de 255 a 7. Sans melange, tout sort en blanc
     * opaque : un trait fin devient une barre pleine, et il ne reste plus aucune nuance.
     *
     * <p>Et il faut le reposer chaque fois, parce que `GuiGraphics` COUPE le melange a la fin de
     * chaque {@code blit} — verifie dans son code compile. L'activer une seule fois au debut du
     * dessin ne tient donc pas : la premiere texture de page le coupe, et tout ce qui suit — le halo
     * du panneau, ses deux bandes — se dessine en blanc opaque. C'est exactement ce que le joueur a
     * vu avant ce correctif.
     */
    private static void blend() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /** Un widget, puis ses enfants — l'ordre du fichier, qui est celui du dessin. */
    private void drawWidget(GuiGraphics graphics, CWidget widget, double mouseX, double mouseY) {
        if (!widget.transform.doesDraw || !widget.transform.enabled) return;

        int x = (int) Math.round(widget.absX);
        int y = (int) Math.round(widget.absY);
        int w = (int) Math.round(widget.absW);
        int h = (int) Math.round(widget.absH);

        if (w > 0 && h > 0) {
            if (widget.frame != null) {
                drawFrame(graphics, widget, x, y, w, h);
            }
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

    /**
     * Le cadre souple : le halo en huit morceaux, puis les deux bandes claires.
     *
     * <p>Le halo deborde de la boite — c'est lui qui arrondit les coins — et les bandes se dessinent
     * par-dessus, l'une sous le bord haut, l'autre vers le bas. Le tout est repris de
     * {@code TechUI.BlendQuad}, nombres compris.
     *
     * <p>Le carre du <b>centre</b> ne se dessine pas : chez l'original il fait le fond sombre du
     * panneau, mais ici ce fond est celui du widget — l'ajouter le foncerait deux fois.
     */
    private static void drawFrame(GuiGraphics graphics, CWidget widget, int x, int y, int w, int h) {
        CWidget.Frame frame = widget.frame;
        double margin = frame.margin;

        blend();
        ResourceLocation halo = ResourceLocation.tryParse(frame.blend);
        if (halo != null) {
            double[] xs = { x - margin, x, x + w, x + w + margin };
            double[] ys = { y - margin, y, y + h, y + h + margin };

            graphics.setColor(0f, 0f, 0f, frame.alpha / 255f);
            for (int col = 0; col < 3; col++) {
                for (int row = 0; row < 3; row++) {
                    if (col == 1 && row == 1) continue;

                    int x0 = (int) Math.round(xs[col]);
                    int y0 = (int) Math.round(ys[row]);
                    int x1 = (int) Math.round(xs[col + 1]);
                    int y1 = (int) Math.round(ys[row + 1]);
                    if (x1 <= x0 || y1 <= y0) continue;

                    graphics.blit(halo, x0, y0, x1 - x0, y1 - y0,
                            col * QUAD, row * QUAD, QUAD, QUAD, TEXTURE, TEXTURE);
                }
            }
            graphics.setColor(1f, 1f, 1f, 1f);
        }

        ResourceLocation line = ResourceLocation.tryParse(frame.line);
        if (line != null) {
            int lx = (int) Math.round(x - frame.lineMargin);
            int lw = (int) Math.round(w + frame.lineMargin * 2);

            blend();
            graphics.blit(line, lx, (int) Math.round(y + frame.topY), lw,
                    (int) Math.round(frame.topHeight), 0f, 0f, 1, 1, 1, 1);
            graphics.blit(line, lx, (int) Math.round(y + h - frame.bottomInset), lw,
                    (int) Math.round(frame.bottomHeight), 0f, 0f, 1, 1, 1, 1);
        }
    }

    private static void drawTexture(GuiGraphics graphics, String path, int x, int y, int w, int h,
                                    int colour) {
        ResourceLocation texture = ResourceLocation.tryParse(path);
        if (texture == null) return;

        graphics.setColor(((colour >> 16) & 0xFF) / 255f, ((colour >> 8) & 0xFF) / 255f,
                (colour & 0xFF) / 255f, ((colour >>> 24) & 0xFF) / 255f);
        // La texture entiere, etiree sur le rectangle : le fichier ne decoupe jamais ses images.
        // Passer 1 pour la taille de texture revient a demander ses coordonnees de zero a un, sans
        // avoir a connaitre ses dimensions — les pages des machines font 352 sur 374, les icones 48.
        blend();
        graphics.blit(texture, x, y, w, h, 0f, 0f, 1, 1, 1, 1);
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
