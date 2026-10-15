package cn.academy.client.gui.cgui;

import java.util.ArrayList;
import java.util.List;

/**
 * Un widget du moteur CGui : un nom, des composants, et des enfants.
 *
 * <p>C'est la piece de base de {@code loctele_new.xml}, et elle reprend ce que {@code Widget} de
 * LambdaLib2 porte vraiment : un arbre de noms, des composants accroches par nom de classe, et des
 * operations qu'un ecran utilise pour le modifier — {@code child}, {@code copy}, {@code remove}.
 *
 * <h2>Ce que le port n'a pas repris</h2>
 *
 * <p>L'original acceptait n'importe quel composant, dans n'importe quel ordre, par reflexion. Ici
 * les cinq composants dont l'ecran a besoin sont des <b>champs</b> — transformation, texture,
 * cerne, teinte, texte — ce qui suffit a lire le fichier et se relit sans reflexion. Un composant
 * qui manquerait se lirait comme absent, sans rien casser.
 *
 * <h2>Les coordonnees</h2>
 *
 * <p>Elles sont celles du fichier, en unites de conception : le fichier est pose a l'echelle
 * {@code 0,24}, donc ses nombres se lisent tels quels et c'est {@link CGuiLayout} qui les met a
 * l'echelle de l'ecran.
 */
public class CWidget {

    /** Ou un widget se pose dans son parent, et comment il s'y accroche. */
    public static final class Transform {
        public double width;
        public double height;
        public double x;
        public double y;
        public double pivotX;
        public double pivotY;
        public double scale = 1.0;

        /** {@code LEFT}, {@code CENTER} ou {@code RIGHT} : de quel bord {@code x} se compte. */
        public String alignWidth = "LEFT";

        /** {@code TOP}, {@code CENTER} ou {@code BOTTOM} : de quel bord {@code y} se compte. */
        public String alignHeight = "TOP";

        public boolean doesDraw = true;
        public boolean enabled = true;
    }

    /** Un fond, ou une image : la texture du fichier, ou sa couleur quand il n'y en a pas. */
    public static final class DrawTexture {
        public String texture;
        public int red = 255;
        public int green = 255;
        public int blue = 255;
        public int alpha = 255;

        /** L'ordre de dessin, que le fichier porte mais que rien n'utilise ici. */
        public double zLevel;
    }

    /** Un cerne, dessine sur les quatre bords du widget. */
    public static final class Outline {
        public int red = 255;
        public int green = 255;
        public int blue = 255;
        public int alpha = 255;
        public double lineWidth = 1.0;
    }

    /** Une teinte posee sur le widget, qui s'allume quand la souris le survole. */
    public static final class Tint {
        public int[] idle = { 255, 255, 255, 0 };
        public int[] hover = { 255, 255, 255, 0 };

        /** Le fichier porte ce drapeau ; la teinte du port se pose toujours par-dessus. */
        public boolean affectTexture;
    }

    /**
     * Une barre de progression : la fenetre d'une texture, remplie d'un cote.
     *
     * <p>C'est le {@code ProgressBar} de LambdaLib2, au champ pres : sa texture, sa direction, son
     * remplissage de zero a un, et sa couleur. Quand elle n'a pas de texture, c'est un aplat de
     * cette couleur — le developpeur s'en sert ainsi.
     *
     * <p>{@code illustrating} est son mode de demonstration : la barre fait alors l'aller-retour
     * toute seule, ce qui sert a regler une page sans avoir de machine sous la main.
     */
    public static final class ProgressBar {
        public enum Direction { RIGHT, LEFT, UP, DOWN }

        public String texture;
        public Direction dir = Direction.RIGHT;

        /** Le remplissage, de zero a un. */
        public double progress;

        public int[] color = { 255, 255, 255, 255 };
        public boolean illustrating;
    }

    /**
     * Une liste qui defile, par lignes entieres : le {@code ElementList} de LambdaLib2.
     *
     * <p>Elle ne se dessine pas elle-meme : elle <b>range</b> les enfants de son widget. Seuls
     * ceux qui tiennent entierement dans sa boite se dessinent, a partir du rang
     * {@link #progress}, et chacun part ou le precedent s'est arrete. C'est ce qui fait que les
     * pages du mod — celle des machines sans fil, celle du brouilleur — n'ont qu'a empiler des
     * elements pour obtenir une liste qui defile.
     */
    public static final class ElementList {
        public double spacing;

        /** Le rang de la premiere ligne visible. */
        public int progress;
    }

    /** Un bout de texte, que l'ecran peut changer, et qui peut devenir un champ de saisie. */
    public static final class TextBox {
        public String content = "";
        public double fontSize = 43.0;

        /** {@code LEFT}, {@code CENTER} ou {@code RIGHT}, dans la boite du widget. */
        public String align = "LEFT";

        public int red = 255;
        public int green = 255;
        public int blue = 255;
        public int alpha = 255;

        /** {@code TOP}, {@code CENTER} ou {@code BOTTOM} : ou le texte se pose en hauteur. */
        public String heightAlign = "TOP";

        /** Vrai quand le joueur peut y ecrire : c'est l'ecran qui l'allume et l'eteint. */
        public boolean allowEdit;

        /**
         * Le masque d'un champ de mot de passe : le vrai texte est garde tel quel, et c'est le
         * <b>dessin</b> qui le remplace par ce caractere, autant de fois qu'il y a de lettres.
         *
         * <p>C'est l'{@code echoChar} de l'original, et l'onglet « sans fil » s'en sert pour son
         * mot de passe : la valeur envoyee reste la bonne, mais personne ne la lit par-dessus
         * l'epaule du joueur.
         */
        public char echoChar;
        public boolean doesEcho;

        /** Et l'indice grise qui s'affiche tant que le champ est vide. */
        public String hint;

        public void color(int r, int g, int b) {
            red = r;
            green = g;
            blue = b;
        }

        public void alpha(int a) {
            alpha = a;
        }
    }

    /**
     * Le cadre souple de l'original : le {@code BlendQuad} de {@code TechUI}.
     *
     * <p>Il se dessine <b>autour</b> de la boite du widget, avec un debord, et fait deux choses :
     *
     * <ul>
     *   <li>huit morceaux d'un halo ({@code blend_quad}) teintes en noir a moitie : les bords et les
     *       coins, qui arrondissent la boite et debordent de quatre unites ;</li>
     *   <li>deux bandes claires ({@code line}) par-dessus, l'une posee <b>au-dessus</b> de la boite
     *       et l'autre vers son bas : ce sont les deux traits blancs du panneau.</li>
     * </ul>
     *
     * <p>Le carre du centre n'est pas dessine : chez l'original il fait le fond sombre du panneau,
     * mais le fond d'un widget est justement ce qu'il porte deja.
     *
     * <p>C'est le seul endroit du mod qui s'en sert, et c'est le panneau d'infos des machines. Les
     * nombres sont ceux de l'original : quatre unites de debord, et les deux bandes a 8,6 unites
     * au-dessus du bord haut et 2 au-dessus du bas.
     */
    public static final class Frame {
        public String blend = "academy:textures/guis/blend_quad.png";
        public double margin = 4;

        /** Le noir a moitie : {@code Colors.monoBlend(0.0f, 0.5f)} chez l'original. */
        public int alpha = 128;

        public String line = "academy:textures/guis/line.png";

        /** Le debord lateral des bandes, et leur place en hauteur. */
        public double lineMargin = 3.2;
        public double topY = -8.6;
        public double topHeight = 12;
        public double bottomInset = 2;
        public double bottomHeight = 8;
    }

    private final String name;
    private final List<CWidget> children = new ArrayList<>();

    public Transform transform = new Transform();
    public DrawTexture texture;
    public Outline outline;
    public Tint tint;
    public TextBox text;
    public ProgressBar bar;
    public ElementList list;
    public Frame frame;

    // --- Ou le widget tombe sur l'ecran, en pixels : pose par CGuiLayout, lu par le rendu. ---

    public double absX;
    public double absY;
    public double absW;
    public double absH;

    /** L'echelle reellement appliquee a ce widget, celle du fichier et de ses parents multipliees. */
    public double absScale = 1.0;

    public CWidget(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    /** Vrai si ce point, en pixels, tombe dans le widget. */
    public boolean isPointWithin(double mouseX, double mouseY) {
        return mouseX >= absX && mouseX <= absX + absW
                && mouseY >= absY && mouseY <= absY + absH;
    }

    /** Les enfants, dans l'ordre du fichier — qui est aussi l'ordre du dessin. */
    public List<CWidget> children() {
        return children;
    }

    public void add(CWidget child) {
        children.add(child);
    }

    /** L'enfant de ce nom, ou {@code null} : le {@code child} de l'original. */
    public CWidget child(String childName) {
        for (CWidget child : children) {
            if (child.name.equals(childName)) return child;
        }
        return null;
    }

    /** L'enfant de ce nom, en le creant s'il n'existe pas. */
    public CWidget ensure(String childName) {
        CWidget found = child(childName);
        if (found != null) return found;
        CWidget created = new CWidget(childName);
        children.add(created);
        return created;
    }

    public void remove(CWidget child) {
        children.remove(child);
    }

    /** Enleve l'enfant de ce nom, s'il y en a un. */
    public void remove(String childName) {
        children.removeIf(child -> child.name.equals(childName));
    }

    /**
     * Une copie <b>profonde</b> : c'est le {@code copy} de l'original, et il en a besoin a chaque
     * ligne de la liste — le fichier ne porte qu'un modele de ligne, et chaque marque en prend une
     * copie pour y mettre son nom.
     */
    public CWidget copy() {
        CWidget out = new CWidget(name);
        out.transform = copy(transform);
        out.texture = copy(texture);
        out.outline = copy(outline);
        out.tint = copy(tint);
        out.text = copy(text);
        out.bar = copy(bar);
        out.list = copy(list);
        for (CWidget child : children) {
            out.add(child.copy());
        }
        return out;
    }

    private static ProgressBar copy(ProgressBar source) {
        if (source == null) return null;
        ProgressBar out = new ProgressBar();
        out.texture = source.texture;
        out.dir = source.dir;
        out.progress = source.progress;
        out.color = source.color.clone();
        out.illustrating = source.illustrating;
        return out;
    }

    private static ElementList copy(ElementList source) {
        if (source == null) return null;
        ElementList out = new ElementList();
        out.spacing = source.spacing;
        out.progress = source.progress;
        return out;
    }

    private static Transform copy(Transform source) {
        if (source == null) return null;
        Transform out = new Transform();
        out.width = source.width;
        out.height = source.height;
        out.x = source.x;
        out.y = source.y;
        out.pivotX = source.pivotX;
        out.pivotY = source.pivotY;
        out.scale = source.scale;
        out.alignWidth = source.alignWidth;
        out.alignHeight = source.alignHeight;
        out.doesDraw = source.doesDraw;
        out.enabled = source.enabled;
        return out;
    }

    private static DrawTexture copy(DrawTexture source) {
        if (source == null) return null;
        DrawTexture out = new DrawTexture();
        out.texture = source.texture;
        out.red = source.red;
        out.green = source.green;
        out.blue = source.blue;
        out.alpha = source.alpha;
        out.zLevel = source.zLevel;
        return out;
    }

    private static Outline copy(Outline source) {
        if (source == null) return null;
        Outline out = new Outline();
        out.red = source.red;
        out.green = source.green;
        out.blue = source.blue;
        out.alpha = source.alpha;
        out.lineWidth = source.lineWidth;
        return out;
    }

    private static Tint copy(Tint source) {
        if (source == null) return null;
        Tint out = new Tint();
        out.idle = source.idle.clone();
        out.hover = source.hover.clone();
        out.affectTexture = source.affectTexture;
        return out;
    }

    private static TextBox copy(TextBox source) {
        if (source == null) return null;
        TextBox out = new TextBox();
        out.content = source.content;
        out.fontSize = source.fontSize;
        out.align = source.align;
        out.red = source.red;
        out.green = source.green;
        out.blue = source.blue;
        out.alpha = source.alpha;
        out.heightAlign = source.heightAlign;
        out.allowEdit = source.allowEdit;
        out.echoChar = source.echoChar;
        out.doesEcho = source.doesEcho;
        out.hint = source.hint;
        return out;
    }
}
