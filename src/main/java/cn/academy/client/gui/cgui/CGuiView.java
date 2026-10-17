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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
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

    /**
     * Les champs ou le joueur a pose le clavier, et dont la saisie n'a pas encore ete validee.
     *
     * <p>Deux regles tiennent ensemble. Un champ ne se vide plus en prenant le clavier : l'original ne
     * le fait pas (seule sa touche Suppr vide), et cliquer d'un champ a l'autre effacait donc le
     * premier — « si je commence a ecrire dans le SSID puis que je clique sur Password, il supprime ce
     * que j'avais ecrit ». Et un champ ou l'on vient d'ecrire ne se relit plus a chaque image : sans
     * cela la machine reecrit sa valeur par-dessus la saisie des que le clavier passe a l'autre champ,
     * ce qui l'efface par un second chemin, invisible celui-la.
     */
    private final Set<CWidget> edited = Collections.newSetFromMap(new IdentityHashMap<>());

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
     *
     * <p>La racine — le CADRE du conteneur — se pose au pixel entier ({@link
     * CGuiLayout#placeSnapped}) pour tomber juste sur les trous des emplacements vanilla ; tout ce
     * qu'elle contient garde la position exacte que le document lui donne.
     */
    public void place(double parentX, double parentY, double parentW, double parentH) {
        CGuiLayout.placeSnapped(root, parentX, parentY, parentW, parentH, 1.0);
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
        // Un sous-arbre qui ne se dessine pas ne se touche pas non plus. Une page cachee reste dans
        // l'arbre — c'est ainsi que le moteur la garde prete — et ses champs, eux, repondaient : le
        // joueur ne pouvait plus prendre l'objet du dernier emplacement de sa barre rapide, sans
        // rien voir sous son curseur.
        if (!widget.transform.doesDraw || !widget.transform.enabled) return null;

        CWidget found = widget.isPointWithin(mouseX, mouseY) ? widget : null;

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

    /**
     * Le champ qui prend le clavier, ou rien pour le rendre.
     *
     * <p>Le contenu n'est efface ni chez le champ qu'on quitte, ni chez celui qu'on prend : c'est le
     * comportement de l'original, ou seul le clic pose le clavier, ou la touche Suppr vide a la
     * demande, et ou le champ se remplit donc comme un formulaire — deux champs d'un meme formulaire
     * (un nom de reseau et son mot de passe) gardent chacun sa saisie.
     */
    public void focus(CWidget box) {
        focused = box;
        if (box != null) edited.add(box);
    }

    /** Vrai si le joueur a pose le clavier sur ce champ sans encore le valider. */
    public boolean isEdited(CWidget box) {
        return box != null && edited.contains(box);
    }

    /**
     * Rend le clavier sans rien valider : la saisie en cours est abandonnee, et la machine reprend la
     * main a l'image suivante.
     *
     * <p>C'est ce que fait Echappe. Le texte tape reste affiche le temps de l'appui — l'ecran le relit
     * ensuite depuis la machine — et rien n'est envoye au serveur.
     */
    private void release(CWidget box) {
        if (box == null) return;
        edited.remove(box);
        if (box.text != null) box.text.color(idleRed, idleGreen, idleBlue);
        focus(null);
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

        // Et il rend le clavier : sans cela, ce qui se tape ensuite s'ajouterait a la valeur qu'on
        // vient d'envoyer. Le champ cesse aussi d'etre « en cours d'ecriture », donc l'ecran le relit
        // a l'image suivante et c'est la reponse du serveur qu'on voit : la valeur nettoyee, ou celle
        // d'avant si elle a ete refusee.
        edited.remove(box);
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

        // Les positions d'un widget sont FRACTIONNAIRES chez l'original : il dessine ses quads a la
        // position exacte que sa pose lui donne (`x = tx + ... + transform.x * parentScale`), et la
        // carte graphique etale alors le dessin sur les deux pixels voisins. Le port arrondissait au
        // pixel le plus proche, ce qui fait glisser chaque element d'un demi-pixel dans un sens ou
        // dans l'autre : le joueur l'a vu sur les carres d'une legende (« sureleves d'un a deux
        // pixels a peine ») et sur les barres d'un histogramme (« legerement decalees vers la
        // gauche »). La position exacte vit donc dans la POSE, et les nombres restent entiers
        // en dessous — meme remede que pour les glyphes de la police.
        double exactX = widget.absX;
        double exactY = widget.absY;
        int x = (int) Math.floor(exactX);
        int y = (int) Math.floor(exactY);
        int w = (int) Math.round(widget.absW);
        int h = (int) Math.round(widget.absH);

        if (w > 0 && h > 0) {
            graphics.pose().pushPose();
            graphics.pose().translate((float) (exactX - x), (float) (exactY - y), 0f);

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
            if (widget.tint != null && widget.tint.enabled) {
                tint = widget.isPointWithin(mouseX, mouseY) ? widget.tint.hover : widget.tint.idle;
            }

            if (widget.texture != null) {
                int colour = tint != null && widget.tint.affectTexture
                        ? argb(tint[3], tint[0], tint[1], tint[2])
                        : argb(widget.texture.alpha, widget.texture.red, widget.texture.green,
                                widget.texture.blue);
                if (widget.texture.texture == null) {
                    fillExact(graphics, widget, exactX, exactY, widget.absW, widget.absH, colour);
                } else {
                    drawTexture(graphics, widget, widget.texture, exactX, exactY, colour);
                }
            }
            if (tint != null && !widget.tint.affectTexture && tint[3] > 0) {
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

        double x0 = originX + line.x0 * scale;
        double y0 = originY + line.y0 * scale;
        double x1 = x0 + (line.x1 - line.x0) * scale * line.progress;
        double y1 = y0 + (line.y1 - line.y0) * scale * line.progress;

        double dx = x1 - x0;
        double dy = y1 - y0;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 1) return;

        double width = Math.max(1.0d, line.thickness * scale);

        graphics.pose().pushPose();
        graphics.pose().translate(x0, y0, 0);
        // L'axe des ordonnees du jeu descend, celui du repere tourne descend aussi : l'angle se
        // donne donc tel quel, sans le retourner.
        graphics.pose().mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
        // Le demi-pixel d'epaisseur, dans le repere tourne : c'est le CENTRE du trait qui doit tomber
        // sur l'axe qui joint les deux noeuds, et non son bord.
        graphics.pose().translate(0.0d, -width / 2.0d, 0.0d);

        // Le trait passe par le type LISSE, et garde ses dimensions fractionnaires : c'est ce qui
        // efface les escaliers de ses diagonales. Le port arrondissait sa longueur et son epaisseur
        // pour les dessiner en {@code blit}, au plus proche voisin — deux raisons pour que les bords
        // mordent la grille de pixels, et le joueur les voyait toujours : « les lignes blanches en
        // diagonales ont toujours les escaliers visibles, il faudrait donc faire de l'anti aliasing ».
        CGuiShaders.strip(graphics, line.texture, length, width,
                line.red, line.green, line.blue, line.alpha);
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

        // La position EXACTE dans la pose, et le dessin LISSE : les trois morceaux d'un noeud se
        // centrent ainsi sur le meme point, et leurs bords se fondent au lieu de sauter d'un pixel a
        // l'autre. Arrondie au plus proche voisin, une image de cent vingt-huit pixels reduite a
        // vingt-trois faisait un cerne casse, que le joueur lisait comme un decalage avec l'anneau
        // de progression : « j'ai l'impression qu'il y a un petit decalage du cercle en lui meme avec
        // le cercle qui donne le pourcentage ».
        double px = Math.floor(x);
        double py = Math.floor(y);

        graphics.pose().pushPose();
        graphics.pose().translate((float) (x - px), (float) (y - py), 0f);
        CGuiShaders.textured(graphics, path, w, h, red, green, blue, alpha);
        graphics.pose().popPose();
    }

    /**
     * Une pose posee sur la position <b>exacte</b> d'un rectangle, dont l'etendue entiere sera
     * ensuite etiree pour couvrir la vraie.
     *
     * <p>C'est la piece qui manquait depuis le debut, et elle tient en une phrase : <b>la taille du
     * document va a la carte graphique, elle ne s'arrondit pas.</b> Un element qui mesure 6,4 pixels
     * se dessine donc sur 6 pixels que la pose etire a 6,4, et le resultat est le quad fractionnaire
     * de l'original — la carte le rasterise exactement comme elle rasterisait le sien, un pixel
     * s'allume quand son centre tombe dedans.
     *
     * <p>Le port arrondissait la taille AVANT de dessiner, et il y a perdu trois fois : une barre de
     * 6,4 pixels en peignait 6 (« les barres sont bien 1 pixel moins large sur la droite »), puis,
     * mesuree depuis l'absolu, elle en peignait 7 et montait d'un pixel (« trop grosse et un peu
     * trop en haut »), et une icone d'onglet de 16,8 pixels etait etiree pour remplir les 17 qu'elle
     * occupe (« certains endroits qui paraissent plus gros »). Le rectangle de l'original, lui, ne
     * dependait d'aucune de ces regles : il etait simplement exact.
     *
     * <p>La fraction de la pose du widget est retiree au passage : les coordonnees donnees ici sont
     * absolues, comme celles de l'original, et c'est la POSITION qui reste sur la grille — pour que
     * l'echantillonnage d'une texture ne glisse pas d'un texel.
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
            // Les deux bandes se posent a la taille exacte du document — 3,2 unites de debord, 8,6
            // au-dessus du bord haut, 12 de haut — comme le rectangle de l'original : voir exactQuad.
            blend();
            band(graphics, widget, line, widget.absX - frame.lineMargin, widget.absY + frame.topY,
                    widget.absW + frame.lineMargin * 2, frame.topHeight);
            band(graphics, widget, line, widget.absX - frame.lineMargin,
                    widget.absY + widget.absH - frame.bottomInset,
                    widget.absW + frame.lineMargin * 2, frame.bottomHeight);
        }
    }

    /** Une des deux bandes claires d'un cadre : `line.png` etire sur toute la largeur du panneau. */
    private static void band(GuiGraphics graphics, CWidget widget, ResourceLocation line,
                             double x, double y, double w, double h) {
        int iw = Math.max(1, (int) Math.round(w));
        int ih = Math.max(1, (int) Math.round(h));

        exactQuad(graphics, widget, x, y, w / iw, h / ih);
        graphics.blit(line, 0, 0, iw, ih, 0f, 0f, 1, 1, 1, 1);
        graphics.pose().popPose();
    }

    private static void drawTexture(GuiGraphics graphics, CWidget widget,
                                    CWidget.DrawTexture window, double x, double y, int colour) {
        ResourceLocation texture = ResourceLocation.tryParse(window.texture);
        if (texture == null) return;

        graphics.setColor(((colour >> 16) & 0xFF) / 255f, ((colour >> 8) & 0xFF) / 255f,
                (colour & 0xFF) / 255f, ((colour >>> 24) & 0xFF) / 255f);
        // La fenetre demandee, en fractions converties sur une base entiere : une image entiere fait
        // zero a un, une planche de trois en prend un tiers. Le tout sans jamais avoir a connaitre les
        // dimensions du fichier — les pages des machines font 352 sur 374, les icones 48.
        //
        // Et la taille est celle du DOCUMENT, fraction comprise : une icone d'onglet mesure 16,8
        // pixels, elle se pose donc sur un quad de 16,8 pixels — la pose etire pour cela les seize
        // pixels entiers que la version sait dessiner. C'est ce qui rend a ses bords doux les pixels
        // qu'ils avaient chez l'original : « l'icone du coffre et du reseau wifi doivent avoir un
        // effet de transparence pour certains de leurs pixels car la ils ont certains endroits qui
        // paraissent plus gros ».
        blend();
        int iw = Math.max(1, (int) Math.round(widget.absW));
        int ih = Math.max(1, (int) Math.round(widget.absH));
        exactQuad(graphics, widget, x, y, widget.absW / iw, widget.absH / ih);
        graphics.blit(texture, 0, 0, iw, ih,
                (float) (window.u0 * UV_BASE), (float) (window.v0 * UV_BASE),
                Math.max(1, (int) Math.round((window.u1 - window.u0) * UV_BASE)),
                Math.max(1, (int) Math.round((window.v1 - window.v0) * UV_BASE)),
                UV_BASE, UV_BASE);
        graphics.pose().popPose();
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    /**
     * Une barre de progression : la portion de sa texture qui correspond au remplissage, ou un
     * aplat de sa couleur quand elle n'en a pas. Le morceau vient de {@link CGuiLayout#bar}.
     *
     * <p>Et sa taille est celle du document, fraction comprise : 6,4 pixels se dessinent sur un quad
     * de 6,4 pixels, que la carte rasterise comme elle rasterisait celui de l'original. Voir
     * {@link #exactQuad} pour les trois formes que l'arrondi a prises cette semaine.
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
            blend();
            // La portion de texture se compte sur UV_BASE et non sur TEXTURE : a 48 pas par image,
            // la fraction sautait de 1/48 d'un coup (deux pixels et demi de la texture du fusor)
            // alors que le quad, lui, grandissait en continu. Le dessin a l'interieur de la barre
            // reculait donc d'un pixel a chaque saut : « on la voit faire des gauche droite de 1
            // pixel environ durant l'animation ». A 1200 pas, le saut est de un dixieme de pixel.
            graphics.blit(texture, 0, 0, rw, rh,
                    (float) (bar.u() * UV_BASE), (float) (bar.v() * UV_BASE),
                    (int) Math.round(bar.uw() * UV_BASE), (int) Math.round(bar.vh() * UV_BASE),
                    UV_BASE, UV_BASE);
            graphics.setColor(1f, 1f, 1f, 1f);
        }

        graphics.pose().popPose();
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

        // Un champ VIDE qui a le clavier se dessine quand meme : c'est son curseur qui dit qu'on
        // peut ecrire. Le port sortait avant de le poser, donc celui du mot de passe ne s'allumait
        // qu'a la premiere lettre — « quand on clique entre les [ ], on ne voit pas la barre bleue
        // qui nous dit qu'on peut ecrire tant qu'on n'a pas deja commence a ecrire ». L'original,
        // lui, posait son trait des que le champ avait le focus, contenu ou pas.
        if ((content == null || content.isEmpty()) && !editable) return;
        if (content == null) content = "";

        // Un champ de mot de passe se dessine masque : le vrai texte ne sort jamais de l'ecran.
        if (box.doesEcho && !empty) {
            content = String.valueOf(box.echoChar).repeat(box.content.length());
        }

        // LA TAILLE VIENT DU WIDGET, comme chez l'original : son `FontOption` porte une cellule de
        // n pixels, que l'echelle heritee des parents multiplie. Le port dessinait tout a la taille
        // de l'interface, ce qui effacait la hierarchie des pages — « dans les machines, certains
        // textes sont ecrits avec une taille encore differente » — et les quads de glyphes de
        // l'original font EXACTEMENT `fontSize` de cote, donc la correspondance est directe.
        int cell = AcademyFonts.cell(box.fontSize * widget.absScale);

        int colour = argb(box.alpha, box.red, box.green, box.blue);
        var font = screen.getMinecraft().font;
        int textWidth = AcademyFonts.width(font, content, cell);
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

        // Pose par le haut de sa cellule, comme le `TextBox` de l'original :
        // `(height - fontSize) x heightAlign.factor`. Avec la ligne de base du jeu, tout le texte
        // des pages tombait un pixel trop bas, et le joueur l'a vu.
        if (!content.isEmpty()) {
            AcademyFonts.drawTop(graphics, font, content, textX, textY, colour, cell);
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
        // Meme regle que le dessin, et meme raison que dans {@link #hitTest} : ce qui ne se voit pas
        // ne prend pas le clic.
        if (!widget.transform.doesDraw || !widget.transform.enabled) return null;

        boolean interactive = clicks.containsKey(widget)
                || widget.text != null && widget.text.allowEdit;
        CWidget found = interactive && widget.isPointWithin(mouseX, mouseY)
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
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            // Echappe rend le clavier SANS fermer l'ecran : c'est le geste qui sort d'un champ. Il
            // abandonne la saisie en cours (le champ reprend la valeur de la machine a l'image
            // suivante), et un SECOND appui ferme l'ecran, comme partout ailleurs.
            //
            // Sans cette branche, l'ecran avalait Echappe des qu'un champ avait le clavier — il le
            // fait pour que la touche n'aille pas dans l'inventaire — donc la touche ne faisait plus
            // rien du tout. Le joueur : « quand on ecrit le nom du reseau et/ou son mot de passe,
            // faire echap pour sortir du bloc ne fonctionne plus ».
            release(focused);
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE
                && !focused.text.content.isEmpty()) {
            focused.text.content = focused.text.content.substring(0,
                    focused.text.content.length() - 1);
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE) {
            // La touche Suppr vide le champ entierement : c'est le seul geste de l'original qui
            // efface un champ, et c'est celui dont on a besoin pour remplacer une valeur au lieu de
            // l'effacer lettre par lettre.
            focused.text.content = "";
            focused.text.color(editRed, editGreen, editBlue);
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            confirm(focused);
            return true;
        }
        return false;
    }
}
