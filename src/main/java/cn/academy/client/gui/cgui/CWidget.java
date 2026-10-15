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

    private final String name;
    private final List<CWidget> children = new ArrayList<>();

    public Transform transform = new Transform();
    public DrawTexture texture;
    public Outline outline;
    public Tint tint;
    public TextBox text;

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
        for (CWidget child : children) {
            out.add(child.copy());
        }
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
        out.hint = source.hint;
        return out;
    }
}
