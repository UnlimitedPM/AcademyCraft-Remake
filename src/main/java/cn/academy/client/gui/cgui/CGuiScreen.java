package cn.academy.client.gui.cgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.mojang.blaze3d.systems.RenderSystem;

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

    /** Le widget sous ce point, ou {@code null}. Voir {@code CGuiView.hitTest} pour la regle. */
    protected CWidget hitTest(double mouseX, double mouseY) {
        return hitTest(root, mouseX, mouseY);
    }

    /**
     * Le widget sous ce point : le DERNIER de l'arbre qui le contient, et lui seul.
     *
     * <p>Le {@code gtnTraverse} de LambdaLib2 : un widget est juge sur sa propre boite — pas sur
     * celle de ses parents, qui peut etre plus petite que lui — et c'est celui du dessus qui prend
     * le clic.
     */
    private static CWidget hitTest(CWidget widget, double mouseX, double mouseY) {
        CWidget found = widget.transform.doesDraw && widget.isPointWithin(mouseX, mouseY)
                ? widget : null;

        for (CWidget child : widget.children()) {
            CWidget deeper = hitTest(child, mouseX, mouseY);
            if (deeper != null) found = deeper;
        }
        return found;
    }

    /** Ce que ce widget fait quand on clique dessus. */
    protected void onClick(CWidget widget, Runnable action) {
        clicks.put(widget, action);
    }

    /**
     * Le champ qui prend le clavier, ou rien pour le rendre.
     *
     * <p>Le contenu n'est efface ni chez le champ qu'on quitte, ni chez celui qu'on prend : l'original
     * ne vide un champ que par sa touche Suppr, et cliquer d'un champ a l'autre doit garder les deux
     * saisies. L'autre moteur ({@link CGuiView}) suit la meme regle.
     */
    protected void focus(CWidget box) {
        focused = box;
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
        // `enabled` n'est PAS « visible ». Chez l'original c'est le drapeau des EVENEMENTS de
        // chaque composant ({@code Component.enabled} : un gestionnaire desactive ne repond
        // pas), et un widget dont le Transform le porte ne se cache pas pour autant. Le
        // document du tutoriel s'en sert — le panneau du texte, sa fenetre de titre et le
        // champ du resume sont desactives pour ne pas prendre les clics — et le port les
        // cachait donc en entier : le texte s'affichait par chance (il est dessine par
        // l'ecran) mais la barre de defilement, elle, n'apparaissait jamais.
        if (!widget.transform.doesDraw) return;

        // Meme chose que CGuiView : la position exacte du widget vit dans la pose, pour que ses
        // elements se posent ou l'original les posait, et non au pixel arrondi.
        double exactX = widget.absX;
        double exactY = widget.absY;
        int x = (int) Math.floor(exactX);
        int y = (int) Math.floor(exactY);
        int w = (int) Math.round(widget.absW);
        int h = (int) Math.round(widget.absH);

        if (w > 0 && h > 0) {
            graphics.pose().pushPose();
            graphics.pose().translate((float) (exactX - x), (float) (exactY - y), 0f);

            // La teinte d'un widget, et ses deux facons — c'est le fichier qui choisit :
            //
            //   - `affectTexture` : elle REMPLACE la couleur du dessin. Une image dont l'alpha
            //     porte tout le motif s'allume ainsi sans qu'un pixel de plus soit peint ;
            //   - sinon : elle se pose par-dessus, en aplat.
            //
            // Ce moteur peignait l'aplat dans les deux cas. Le curseur de la barre de
            // defilement du tutoriel s'en trouvait RECOUVERT d'un carre blanc a 80 %
            // (`Tint` a `affectTexture` dans le document) : ses bords doux disparaissaient, et
            // on ne voyait plus qu'un bloc a bord dur — le joueur l'a signale trois fois.
            // C'est la meme regle que {@code CGuiView}, ou le drapeau, lui, etait deja lu.
            int[] tint = null;
            if (widget.tint != null && widget.tint.enabled) {
                tint = widget.isPointWithin(mouseX, mouseY) ? widget.tint.hover
                        : widget.tint.idle;
            }
            boolean tintTexture = tint != null && widget.tint.affectTexture;

            if (widget.texture != null) {
                int colour = tintTexture
                        ? argb(tint[3], tint[0], tint[1], tint[2])
                        : argb(widget.texture.alpha, widget.texture.red, widget.texture.green,
                                widget.texture.blue);
                if (widget.texture.texture == null) {
                    fillExact(graphics, widget, exactX, exactY, widget.absW, widget.absH, colour);
                } else {
                    drawTexture(graphics, widget, widget.texture.texture, exactX, exactY, colour);
                }
            }
            if (tint != null && !tintTexture && tint[3] > 0) {
                fillExact(graphics, widget, exactX, exactY, widget.absW, widget.absH,
                        argb(tint[3], tint[0], tint[1], tint[2]));
            }
            if (widget.bar != null) {
                drawBar(graphics, widget, exactX, exactY, widget.absW, widget.absH);
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

            graphics.pose().popPose();
        }

        for (CWidget child : widget.children()) {
            drawWidget(graphics, child, mouseX, mouseY);
        }
    }

    /**
     * Une pose posee sur la position exacte d'un rectangle, dont l'etendue entiere sera etiree pour
     * couvrir la vraie : voir {@code CGuiView.exactQuad}, dont ce moteur est la copie.
     */
    private static void exactQuad(GuiGraphics graphics, CWidget widget, double x, double y,
                                  double scaleX, double scaleY) {
        graphics.pose().pushPose();
        graphics.pose().translate((float) -(widget.absX - Math.floor(widget.absX)),
                (float) -(widget.absY - Math.floor(widget.absY)), 0f);
        graphics.pose().translate((float) x, (float) y, 0f);
        graphics.pose().scale((float) scaleX, (float) scaleY, 1f);
    }

    /** Un aplat, a la taille exacte du widget. */
    private static void fillExact(GuiGraphics graphics, CWidget widget, double x, double y,
                                  double w, double h, int colour) {
        if (w <= 0 || h <= 0) return;
        int iw = Math.max(1, (int) Math.round(w));
        int ih = Math.max(1, (int) Math.round(h));

        exactQuad(graphics, widget, x, y, w / iw, h / ih);
        graphics.fill(0, 0, iw, ih, colour);
        graphics.pose().popPose();
    }

    /**
     * Une barre de progression : la portion de sa texture qui correspond au remplissage, ou un
     * aplat de sa couleur quand elle n'en a pas.
     *
     * <p>Le morceau a dessiner vient de {@link CGuiLayout#bar}, ou le calcul est pur et relu par un
     * test : c'est le {@code switch} du composant de l'original, et il decide a la fois du
     * rectangle et de la portion de texture — une barre qui monte prend le BAS de son image. Et sa
     * taille est celle du document, fraction comprise : voir exactQuad.
     */
    private static void drawBar(GuiGraphics graphics, CWidget widget, double x, double y,
                                double w, double h) {
        if (w <= 0 || h <= 0) return;

        CGuiLayout.Bar bar = CGuiLayout.bar(widget.bar, w, h);
        if (bar.w() <= 0 || bar.h() <= 0) return;

        int[] color = widget.bar.color;
        ResourceLocation texture = widget.bar.texture == null ? null
                : ResourceLocation.tryParse(widget.bar.texture);
        int rw = Math.max(1, (int) Math.round(bar.w()));
        int rh = Math.max(1, (int) Math.round(bar.h()));

        exactQuad(graphics, widget, x + bar.x(), y + bar.y(), bar.w() / rw, bar.h() / rh);

        if (texture == null) {
            graphics.fill(0, 0, rw, rh, argb(color[3], color[0], color[1], color[2]));
        } else {
            graphics.setColor(color[0] / 255f, color[1] / 255f, color[2] / 255f, color[3] / 255f);
            graphics.blit(texture, 0, 0, rw, rh,
                    (float) (bar.u() * TEXTURE), (float) (bar.v() * TEXTURE),
                    (int) Math.round(bar.uw() * TEXTURE), (int) Math.round(bar.vh() * TEXTURE),
                    TEXTURE, TEXTURE);
            graphics.setColor(1f, 1f, 1f, 1f);
        }

        graphics.pose().popPose();
    }

    private static void drawTexture(GuiGraphics graphics, CWidget widget, String path, double x,
                                    double y, int colour) {
        ResourceLocation texture = ResourceLocation.tryParse(path);
        if (texture == null) return;

        // Sans le melange, l'alpha de l'image est IGNORE : les coins ronds d'une fenetre se
        // dessinaient en blanc plein, et son fond noir a moitie devenait un aplat noir. Une
        // icone d'etiquette, qui tient tout entiere dans son alpha, n'etait plus qu'un carre
        // blanc — c'est ce que le joueur voyait sous les apercus du tutoriel.
        blend();
        graphics.setColor(((colour >> 16) & 0xFF) / 255f, ((colour >> 8) & 0xFF) / 255f,
                (colour & 0xFF) / 255f, ((colour >>> 24) & 0xFF) / 255f);
        int iw = Math.max(1, (int) Math.round(widget.absW));
        int ih = Math.max(1, (int) Math.round(widget.absH));
        exactQuad(graphics, widget, x, y, widget.absW / iw, widget.absH / ih);
        graphics.blit(texture, 0, 0, iw, ih, 0f, 0f, TEXTURE, TEXTURE, TEXTURE, TEXTURE);
        graphics.pose().popPose();
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    /** Le melange alpha, celui que tout dessin de texture demande. */
    protected static void blend() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
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
        boolean editable = focused == widget && box.allowEdit;
        String content = empty && box.hint != null ? box.hint : box.content;

        // Un champ vide qui a le clavier se dessine quand meme, pour son curseur : voir CGuiView.
        if ((content == null || content.isEmpty()) && !editable) return;
        if (content == null) content = "";

        // Un champ de mot de passe se dessine masque : le vrai texte ne sort jamais de l'ecran.
        if (box.doesEcho && !empty) content = String.valueOf(box.echoChar).repeat(box.content.length());

        // La taille vient du widget, comme chez l'original : voir CGuiView, dont ce dessin est la
        // copie d'origine (ce moteur-la attend sa fusion).
        int cell = cn.academy.client.gui.AcademyFonts.cell(box.fontSize * widget.absScale);

        int colour = argb(box.alpha, box.red, box.green, box.blue);
        int textWidth = cn.academy.client.gui.AcademyFonts.width(font, content, cell);
        int textX = switch (box.align.toUpperCase(java.util.Locale.ROOT)) {
            case "CENTER" -> x + (w - textWidth) / 2;
            case "RIGHT" -> x + w - textWidth;
            default -> x;
        };
        int textY = switch (box.heightAlign.toUpperCase(java.util.Locale.ROOT)) {
            case "CENTER" -> y + Math.max(0, h - cell) / 2;
            case "BOTTOM" -> y + Math.max(0, h - cell);
            default -> y;
        };

        // Pose par le haut de sa cellule, comme le `TextBox` de l'original : voir CGuiView.
        if (!content.isEmpty()) {
            cn.academy.client.gui.AcademyFonts.drawTop(graphics, font, content, textX, textY, colour,
                    cell);
        }

        // Le curseur du champ : un trait apres la derniere lettre, tant qu'il a le clavier.
        if (editable && (System.currentTimeMillis() / 500) % 2 == 0) {
            graphics.fill(textX + textWidth + 1, textY, textX + textWidth + 2, textY + cell, colour);
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

        // Faux quand rien n'a ete fait : voir {@code CGuiView.click} pour la raison.
        return false;
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
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                    || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
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
