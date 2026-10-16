package cn.academy.client.gui.cgui;

import cn.academy.client.gui.AcademyFonts;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

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

    /**
     * La base des coordonnees de texture, pour compter les fractions d'une image.
     *
     * <p>Le moteur demande ses fenetres en fractions — zero a un pour l'image entiere, un tiers
     * pour une planche de trois — et {@code blit} les veut en entiers : cette base est le denominateur
     * commun. Mille deux cents se divise par trois, quatre, cinq, six, huit et dix, ce qui couvre les
     * planches du mod ({@code effect_solar} en trois morceaux, le noeud d'imagination en dix).
     */
    private static final int UV_BASE = 1200;

    /**
     * Les deux couleurs d'un champ modifiable, celles de l'original : blanc au repos, bleu pendant
     * qu'on le change. Le bleu dit que ce qui est ecrit n'est pas encore parti au serveur.
     */
    private static final int idleRed = 255;
    private static final int idleGreen = 255;
    private static final int idleBlue = 255;
    private static final int editRed = 0x21;
    private static final int editGreen = 0x80;
    private static final int editBlue = 0xD8;

    private final Screen screen;
    private final CWidget root;
    private final Map<CWidget, Runnable> clicks = new IdentityHashMap<>();
    private final Map<CWidget, Consumer<String>> confirms = new IdentityHashMap<>();

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

    /** Le widget sous ce point, ou {@code null}. */
    public CWidget hitTest(double mouseX, double mouseY) {
        return hitTest(root, mouseX, mouseY);
    }

    /**
     * Le widget sous ce point : le DERNIER de l'arbre qui le contient.
     *
     * <p>C'est le {@code gtnTraverse} de LambdaLib2, et ses deux details comptent :
     *
     * <ul>
     *   <li>un widget est juge sur <b>sa</b> boite, jamais sur celle de ses parents. Le bouton LEARN
     *       du developpeur fait cent quatre-vingt-six unites de large dans un panneau qui en fait
     *       cent quatre : il deborde, et le port — qui exigeait que chaque parent contienne aussi le
     *       point — le rendait donc incliquable. Le joueur l'a dit : « appuyer sur le bouton ne fait
     *       rien » ;</li>
     *   <li>le dernier gagne, et non le premier : celui du dessus, comme a l'ecran, la ou le port
     *       donnait le clic a celui du fond.</li>
     * </ul>
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
    public void onClick(CWidget widget, Runnable action) {
        clicks.put(widget, action);
    }

    /** Le champ qui prend le clavier, ou rien pour le rendre. */
    public void focus(CWidget box) {
        if (focused != null && focused.text != null) focused.text.content = "";
        focused = box;
        if (box != null && box.text != null) box.text.content = "";
    }

    /** Ce que ce widget fait quand on valide son contenu : l'{@code ConfirmInputEvent} de l'original. */
    public void onConfirm(CWidget box, Consumer<String> action) {
        confirms.put(box, action);
    }

    public void confirm(CWidget box) {
        if (box == null) return;

        // Le champ redevient blanc en se validant, comme chez l'original : c'est le signe que ce
        // qu'il contient est parti au serveur et n'est plus une saisie en cours.
        if (box.text != null) box.text.color(idleRed, idleGreen, idleBlue);

        Consumer<String> action = confirms.get(box);
        if (action != null) action.accept(box.text == null ? "" : box.text.content);

        // Et il rend le clavier : sans cela, ce qui se tape ensuite s'ajouterait a la valeur
        // qu'on vient d'envoyer. Le champ se vide en perdant le focus, et la machine le reprend
        // a son compte a l'image suivante : c'est ainsi qu'on voit arriver la valeur acceptee
        // par le serveur — nettoyee de ses espaces, ou refusee, auquel cas elle revient telle
        // quelle.
        focus(null);
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

        // Un bouton s'allume sous le curseur. C'est le seul retour qu'il donne — il n'a pas de fond —
        // et c'est ce que l'original faisait dans son FrameEvent : quatre cinquiemes d'intensite au
        // repos, pleine lumiere sous la souris. Un widget qui se clique et qui porte du texte est un
        // bouton ; les onglets, eux, s'allument par leur image et ne sont pas touches.
        if (widget.text != null && clicks.containsKey(widget)) {
            int lum = widget.isPointWithin(mouseX, mouseY) ? 255 : 204;
            widget.text.color(lum, lum, lum);
        }

        int x = (int) Math.round(widget.absX);
        int y = (int) Math.round(widget.absY);
        int w = (int) Math.round(widget.absW);
        int h = (int) Math.round(widget.absH);

        if (w > 0 && h > 0) {
            if (widget.frame != null) {
                drawFrame(graphics, widget, x, y, w, h);
            }

            // La teinte d'un widget, et ses deux facons — c'est le fichier qui choisit :
            //
            //   - `affectTexture` : elle REMPLACE la couleur du dessin. Une image dont l'alpha porte
            //     tout le motif s'allume ainsi sous le curseur sans qu'un pixel de plus soit peint,
            //     et c'est ce que l'original faisait en changeant la couleur de sa DrawTexture ;
            //   - sinon : elle se pose par-dessus, en aplat.
            //
            // Le port peignait l'aplat dans les deux cas. Les boutons de la page du brouilleur se
            // retrouvaient donc sous un carre blanc de 178 d'opacite, et leurs fleches ne se voyaient
            // plus : le drapeau etait lu, et jamais ecoute.
            int[] tint = null;
            if (widget.tint != null) {
                tint = widget.isPointWithin(mouseX, mouseY) ? widget.tint.hover : widget.tint.idle;
            }

            if (widget.texture != null) {
                int colour = tint != null && widget.tint.affectTexture
                        ? argb(tint[3], tint[0], tint[1], tint[2])
                        : argb(widget.texture.alpha, widget.texture.red, widget.texture.green,
                                widget.texture.blue);
                if (widget.texture.texture == null) {
                    graphics.fill(x, y, x + w, y + h, colour);
                } else {
                    drawTexture(graphics, widget.texture, x, y, w, h, colour);
                }
            }
            if (widget.tint != null && !widget.tint.affectTexture && tint[3] > 0) {
                graphics.fill(x, y, x + w, y + h, argb(tint[3], tint[0], tint[1], tint[2]));
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

        // Le trait passe avant le noeud qu'il relie : l'original le dessinait APRES, mais avec un
        // test de profondeur qui l'ecartait la ou le noeud se trouve — donc, a l'arrivee, la meme
        // image. Les deux bouts du trait s'arretent d'ailleurs a douze unites et demie du centre de
        // chaque noeud, c'est-a-dire en dehors du disque qui en fait onze et demie : ils ne se
        // croisent jamais, et l'ordre ne se voit pas.
        if (widget.line != null) {
            drawLine(graphics, widget, widget.absX, widget.absY, widget.absScale);
        }
        if (widget.disc != null) {
            drawDisc(graphics, widget, widget.absX, widget.absY, w, h);
        }

        for (CWidget child : widget.children()) {
            drawWidget(graphics, child, mouseX, mouseY);
        }
    }

    /**
     * Un trait de l'arbre : un quad texture, pose de travers.
     *
     * <p>Le premier bout du trait est l'origine du dessin, qui est simplement translate la ; le quad
     * est ensuite tourne de l'angle qui va vers le second bout. La texture est posee dans le sens de
     * la <b>longueur</b> (son {@code u} suit le trait, son {@code v} le traverse), et elle couvre
     * exactement la part dessinee : le trait qui pousse etire donc son motif au lieu de le decouper,
     * ce que fait l'original et ce qui se voit — le trait part du noeud et file vers son parent.
     */
    private static void drawLine(GuiGraphics graphics, CWidget widget, double originX,
                                 double originY, double scale) {
        CWidget.Line line = widget.line;
        ResourceLocation texture = ResourceLocation.tryParse(line.texture);
        if (texture == null) return;

        double x0 = originX + line.x0 * scale;
        double y0 = originY + line.y0 * scale;
        double x1 = x0 + (line.x1 - line.x0) * scale * line.progress;
        double y1 = y0 + (line.y1 - line.y0) * scale * line.progress;

        double dx = x1 - x0;
        double dy = y1 - y0;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 1) return;

        // L'epaisseur aussi a sa partie fractionnaire : arrondie, elle decalait le trait d'un
        // demi-pixel et le bord du quad se voyait en marches d'escalier sur les diagonales, ce que le
        // joueur a vu. Le quad se dessine sur une epaisseur entiere, et la pose le ramene a la bonne.
        double width = Math.max(1.0d, line.thickness * scale);
        int wholeWidth = Math.max(1, (int) Math.floor(width));
        float squash = (float) (width / wholeWidth);
        // La longueur a sa partie fractionnaire : le trait qui pousse avancait donc d'un pixel entier a
        // la fois, et le joueur l'a vu — « parfois les lignes blanches augmentent d'un pixel en trop ».
        // Le quad se dessine sur sa longueur entiere, et l'echelle de la pose etire ce qu'il reste.
        int whole = (int) Math.floor(length);
        float stretch = (float) (length / whole);

        graphics.pose().pushPose();
        graphics.pose().translate(x0, y0, 0);
        // L'axe des ordonnees du jeu descend, celui du repere tourne descend aussi : l'angle se
        // donne donc tel quel, sans le retourner.
        graphics.pose().mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
        // Le demi-pixel d'epaisseur, dans le repere tourne : c'est le CENTRE du trait qui doit tomber
        // sur l'axe qui joint les deux noeuds, et non son bord. Arrondi, ce decalage posait le trait a
        // cheval sur deux pixels, ce qui se voyait en marches d'escalier sur les diagonales.
        graphics.pose().translate(0.0d, -width / 2.0d, 0.0d);
        graphics.pose().scale(stretch, squash, 1f);

        graphics.setColor(line.red / 255f, line.green / 255f, line.blue / 255f, line.alpha / 255f);
        blend();
        graphics.blit(texture, 0, 0, whole, wholeWidth, 0f, 0f, 1, 1, 1, 1);
        graphics.setColor(1f, 1f, 1f, 1f);
        graphics.pose().popPose();
    }

    /**
     * Un noeud de l'arbre : le disque, son cerne, et l'icone de la competence.
     *
     * <p>Les trois sont <b>centres sur la boite</b> du widget, et non poses a son coin : la boite en
     * fait seize et le disque vingt-trois, donc le disque deborde de trois unites et demie de chaque
     * cote. C'est la regle de cet arbre — un noeud se dessine autour de sa place, pas a partir d'elle
     * — et c'est elle qui decide aussi de l'ecart minimum entre deux noeuds.
     *
     * <p>L'echelle du dessin est celle du widget multipliee par {@code drawScale} : le survol d'un
     * noeud le grossit ainsi d'un cinquieme <b>sans le deplacer</b>, parce que le centrage se fait
     * apres l'echelle.
     */
    private static void drawDisc(GuiGraphics graphics, CWidget widget, double x, double y, int w, int h) {
        CWidget.Disc disc = widget.disc;
        double scale = widget.absScale * disc.drawScale;

        // LE DEMI-PIXEL. Les positions du moteur sont entieres, et c'est ce qui faisait avancer l'arbre
        // par sauts : un deplacement continu se pose sur des pixels entiers. Le remede est celui du
        // texte : on translate d'abord au point exact, puis on dessine autour de lui. Tout ce qui suit
        // se lit donc par rapport au CENTRE du noeud, et l'echelle du survol se pose autour de lui —
        // c'est ce qui fait qu'il grossit sur place au lieu de partir vers le bas et la droite.
        graphics.pose().pushPose();
        graphics.pose().translate(x + w / 2.0, y + h / 2.0, 0);
        graphics.pose().scale((float) scale, (float) scale, 1f);

        centred(graphics, disc.back, disc.size, 255, 255, 255, disc.backAlpha);

        // Le cerne : un cinquieme de blanc et six dixiemes d'opacite, soit un gris fonce qui se fond
        // dans le panneau. C'est ce qui detache le disque du fond de l'arbre.
        centred(graphics, disc.ring, disc.ringSize, 51, 51, 51,
                (int) Math.round(disc.backAlpha * 0.6));

        // Le remplissage : la part d'experience acquise, dessinee par le programme de l'original — le
        // seul dessin de l'interface qui ne s'obtienne pas avec des rectangles textures. Il se tait si
        // le programme n'est pas la : voir CGuiShaders.
        if (disc.drawFill) {
            CGuiShaders.ring(graphics, disc.ring, disc.mask, disc.ringSize, disc.fill, disc.backAlpha);
        }

        if (disc.icon != null) {
            boolean greyed = disc.grey
                    && CGuiShaders.greyIcon(graphics, disc.icon, disc.iconSize, disc.iconAlpha);
            if (!greyed) {
                centred(graphics, disc.icon, disc.iconSize, 255, 255, 255, disc.iconAlpha);
            }
        }
        graphics.pose().popPose();
    }

    /** Une texture posee au milieu du repere courant. */
    private static void centred(GuiGraphics graphics, String path, double size, int red, int green,
                                int blue, int alpha) {
        blit(graphics, path, -size / 2, -size / 2, size, size, red, green, blue, alpha);
    }

    /**
     * Une texture, posee a une place et dans une couleur donnees.
     *
     * <p>Le melange se repose avant chaque appel : {@code GuiGraphics} le coupe a la fin de chaque
     * {@code blit}, et c'est lui qui porte tout le dessin des images du mod.
     */
    private static void blit(GuiGraphics graphics, String path, double x, double y, double w,
                             double h, int red, int green, int blue, int alpha) {
        if (alpha <= 0 || w <= 0 || h <= 0) return;
        ResourceLocation texture = ResourceLocation.tryParse(path);
        if (texture == null) return;

        graphics.setColor(red / 255f, green / 255f, blue / 255f, alpha / 255f);
        blend();
        graphics.blit(texture, (int) Math.round(x), (int) Math.round(y),
                (int) Math.round(w), (int) Math.round(h), 0f, 0f, 1, 1, 1, 1);
        graphics.setColor(1f, 1f, 1f, 1f);
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

    private static void drawTexture(GuiGraphics graphics, CWidget.DrawTexture window, int x, int y,
                                    int w, int h, int colour) {
        ResourceLocation texture = ResourceLocation.tryParse(window.texture);
        if (texture == null) return;

        graphics.setColor(((colour >> 16) & 0xFF) / 255f, ((colour >> 8) & 0xFF) / 255f,
                (colour & 0xFF) / 255f, ((colour >>> 24) & 0xFF) / 255f);
        // La fenetre demandee, en fractions converties sur une base entiere : une image entiere fait
        // zero a un, une planche de trois en prend un tiers. Le tout sans jamais avoir a connaitre les
        // dimensions du fichier — les pages des machines font 352 sur 374, les icones 48.
        blend();
        graphics.blit(texture, x, y, w, h,
                (float) (window.u0 * UV_BASE), (float) (window.v0 * UV_BASE),
                Math.max(1, (int) Math.round((window.u1 - window.u0) * UV_BASE)),
                Math.max(1, (int) Math.round((window.v1 - window.v0) * UV_BASE)),
                UV_BASE, UV_BASE);
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
        var font = screen.getMinecraft().font;
        int textWidth = AcademyFonts.width(font, content);
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

        AcademyFonts.draw(graphics, font, content, textX, textY, colour, false);

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

        CWidget hit = pick(mouseX, mouseY);
        for (CWidget widget = hit; widget != null; widget = parentOf(widget)) {
            Runnable action = clicks.get(widget);
            if (action != null) {
                action.run();
                return true;
            }
        }
        // Un clic sur un champ editable lui donne le clavier : c'est ainsi que le noeud demande son
        // nom et son mot de passe. L'original le faisait par le GainFocusEvent de son champ.
        if (hit != null && hit.text != null && hit.text.allowEdit) {
            if (hit != focused) focus(hit);
            return true;
        }
        if (hit != focused) focus(null);

        // Faux quand rien n'a ete fait, meme si un widget a ete touche : le fond d'une page couvre
        // tout, et un clic sur un emplacement de machine doit rester a l'inventaire. Un ecran de
        // conteneur s'appuie sur ce faux pour savoir s'il doit passer le clic a vanilla.
        return false;
    }

    /**
     * Le widget du dessus qui PEUT prendre ce clic : celui qui a une action, ou un champ qui s'ecrit.
     *
     * <p>Ce n'est pas la meme chose que {@link #hitTest} : un widget qui n'ecoute rien ne <b>prend</b>
     * pas le clic, il le laisse passer a ce qu'il recouvre. C'est la regle de LambdaLib2 — un
     * composant sans ecouteur ne recoit aucun evenement — et sans elle les <b>crochets</b> d'un champ
     * du panneau d'infos avalaient le clic : ils se posent six unites par-dessus le champ qu'ils
     * decorent, donc un clic juste a droite du libelle tombait sur le crochet, le champ ne prenait pas
     * le clavier, et le joueur ne pouvait plus rien ecrire. Un champ vide est le cas ou cela se voit
     * tout de suite : c'est la que le regard vise, et c'est aussi la que le champ commence.
     */
    private CWidget pick(double mouseX, double mouseY) {
        return pick(root, mouseX, mouseY);
    }

    private CWidget pick(CWidget widget, double mouseX, double mouseY) {
        boolean interactive = clicks.containsKey(widget)
                || widget.text != null && widget.text.allowEdit;
        CWidget found = interactive && widget.transform.doesDraw && widget.isPointWithin(mouseX, mouseY)
                ? widget : null;

        for (CWidget child : widget.children()) {
            CWidget deeper = pick(child, mouseX, mouseY);
            if (deeper != null) found = deeper;
        }
        return found;
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
            // Le champ passe en bleu des qu'on le change : c'est la deuxieme couleur de l'original,
            // et elle dit d'un coup d'oeil que cette valeur n'est pas encore validee.
            focused.text.color(editRed, editGreen, editBlue);
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
