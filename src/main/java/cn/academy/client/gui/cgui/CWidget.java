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

        /**
         * La fenetre de la texture : de zero a un par defaut, c'est-a-dire l'image entiere.
         *
         * <p>C'est ce qui permet d'animer un cadre en decoupant une planche en plusieurs images :
         * {@code effect_solar.png} en contient trois l'une sous l'autre, et l'ecran du generateur
         * solaire n'en montre qu'une a la fois — celle de l'etat du ciel.
         */
        public double u0;
        public double v0;
        public double u1 = 1;
        public double v1 = 1;
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

        /**
         * La couleur, <b>en quatre cases</b> : rouge, vert, bleu, puis l'opacite.
         *
         * <p>C'est le contrat de tous les composants du moteur, et le dessin de la barre lit
         * l'opacite a la quatrieme case : une couleur qui n'en a que trois le fait planter en plein
         * rendu. {@link CGuiHist} en donne quatre.
         */
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

        /**
         * La hauteur de la cellule du texte, en pixels : c'est le {@code FontOption.fontSize} de
         * l'original, et dix est SA valeur par defaut (celle de LambdaLib2, qu'un document qui ne
         * dit rien heritait).
         *
         * <p>Le port y ecrivait quarante-trois, et ne s'en servait nulle part : le moteur dessinait
         * tout le texte a la taille de l'interface, ce qui effacait la hierarchie des pages — un
         * libelle a neuf, un renseignement a huit, un separateur a six. Le joueur l'a vu : « dans
         * les machines, certains textes sont ecrits avec une taille encore differente ».
         */
        public double fontSize = 10.0;

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

    /**
     * Un trait texture entre deux points : le lien d'un noeud vers celui dont il depend.
     *
     * <p>C'est le seul morceau du moteur qui se dessine <b>de travers</b>. Le trait du fichier
     * n'est pas horizontal : il part d'un noeud et vise un autre, et ces deux points ne sont
     * jamais alignes. Le quad est donc tourne autour de son premier bout, et sa texture est
     * posee dans le sens de la longueur.
     *
     * <p>Ses deux bouts sont donnes <b>dans les unites du widget</b>, comme le reste du moteur,
     * et le remplissage est anime : le trait pousse du premier bout vers le second, sa texture
     * etiree sur ce qu'il en reste. C'est ce que fait l'original, et c'est ce qui fait qu'un
     * arbre s'ouvre en se dessinant plutot que de s'afficher d'un coup.
     */
    public static final class Line {
        public String texture = "academy:textures/guis/developer/line.png";

        /** Les deux bouts, dans les unites du widget qui porte le trait. */
        public double x0;
        public double y0;
        public double x1;
        public double y1;

        /** L'epaisseur, perpendiculaire au trait. */
        public double thickness = 5.5;

        /** La part dessinee, de zero a un : la poussee de l'animation. */
        public double progress = 1.0;

        public int red = 255;
        public int green = 255;
        public int blue = 255;
        public int alpha = 255;
    }

    /**
     * Un noeud de l'arbre : un fond rond, un anneau, une icone, et un remplissage.
     *
     * <p>Quatre couches, dans cet ordre, et chacune a son role : le fond porte le disque plein
     * avec ses coins transparents, l'anneau sombre se pose un peu plus large pour cerner le
     * disque, l'icone dit la competence, et le remplissage mange l'anneau au fur et a mesure que
     * la competence progresse.
     *
     * <p>Les trois tailles viennent des images elles-memes, pas du fichier : le disque fait
     * vingt-trois unites alors que la boite du widget en fait seize, l'anneau en fait trente et
     * une, et l'icone quatorze. Les trois sont donc <b>centrees sur la boite</b>, et l'anneau
     * deborde de quatre unites de chaque cote. C'est ce qui faisait deja deborder le trait de
     * parente (voir {@link Line}), et ce qui a decide du seuil du test qui verifie qu'aucun
     * noeud n'en recouvre un autre.
     *
     * <p>{@code drawScale} est l'echelle du <b>dessin</b>, et elle est distincte de celle du
     * widget : le survol d'un noeud le fait grossir d'un cinquieme sans que sa place dans l'arbre
     * bouge d'un pixel. La confondre avec {@code transform.scale} ferait glisser le noeud — et,
     * avec lui, le trait qui le relie a son parent, qui se calcule a partir de la meme place.
     */
    public static final class Disc {
        public String back = "academy:textures/guis/developer/skill_back.png";
        public String ring = "academy:textures/guis/developer/skill_outline.png";

        public String icon;

        /**
         * Le masque du remplissage : il porte l'<b>angle</b>.
         *
         * <p>Sa composante rouge monte de zero a un sur un tour complet — mesure sur l'image : 52 a
         * zero degre, 203 a cent quatre-vingts — et c'est le programme de l'anneau qui le compare au
         * remplissage. C'est ce qui interdit de s'en passer : une valeur qui depend de l'angle ne se
         * decoupe pas au rectangle.
         */
        public String mask = "academy:textures/guis/developer/skill_radial_mask.png";

        /** La taille du disque dessine, en unites. */
        public double size = 23.0;

        /** Et celle de l'anneau, qui deborde donc du disque. */
        public double ringSize = 31.0;

        /** Et celle de l'icone, qui tient dedans. */
        public double iconSize = 14.0;

        /** Vrai quand la competence est apprise : c'est ce qui fait apparaitre l'anneau de remplissage. */
        public boolean drawFill;

        /** Et son remplissage, de zero a un. */
        public double fill;

        /** L'icone en gris, pour ce qui n'est pas encore appris. */
        public boolean grey;

        /** L'echelle du dessin seul : le survol grossit le noeud sans le deplacer. */
        public double drawScale = 1.0;

        public int backAlpha = 255;
        public int iconAlpha = 255;
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
    public Line line;
    public Disc disc;

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
        out.line = copy(line);
        out.disc = copy(disc);
        for (CWidget child : children) {
            out.add(child.copy());
        }
        return out;
    }

    private static Line copy(Line source) {
        if (source == null) return null;
        Line out = new Line();
        out.texture = source.texture;
        out.x0 = source.x0;
        out.y0 = source.y0;
        out.x1 = source.x1;
        out.y1 = source.y1;
        out.thickness = source.thickness;
        out.progress = source.progress;
        out.red = source.red;
        out.green = source.green;
        out.blue = source.blue;
        out.alpha = source.alpha;
        return out;
    }

    private static Disc copy(Disc source) {
        if (source == null) return null;
        Disc out = new Disc();
        out.back = source.back;
        out.ring = source.ring;
        out.mask = source.mask;
        out.icon = source.icon;
        out.size = source.size;
        out.ringSize = source.ringSize;
        out.iconSize = source.iconSize;
        out.drawFill = source.drawFill;
        out.fill = source.fill;
        out.grey = source.grey;
        out.drawScale = source.drawScale;
        out.backAlpha = source.backAlpha;
        out.iconAlpha = source.iconAlpha;
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
        // La fenetre suit la copie, sinon une planche decoupee perd son image : le gabarit d'une
        // ligne animee se recopie sans sa case de depart et se dessine entier, ce qui ne se voit
        // qu'a l'ecran et jamais a la lecture.
        out.u0 = source.u0;
        out.v0 = source.v0;
        out.u1 = source.u1;
        out.v1 = source.v1;
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
