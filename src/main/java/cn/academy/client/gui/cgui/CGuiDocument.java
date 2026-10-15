package cn.academy.client.gui.cgui;

import cn.academy.AcademyCraft;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Le lecteur de documents CGui : le fichier de l'original, relu tel quel.
 *
 * <p>C'est ce qui manquait au port pour avoir <b>le</b> bon ecran plutot qu'un ecran qui y
 * ressemble. L'original ne dessinait pas sa liste de marques a la main : il lisait
 * {@code assets/academy/guis/loctele_new.xml}, un arbre de widgets poses et colores, et c'est ce
 * fichier qui reste la reference. Le port ne l'avait pas : il redessinait tout a la main, et le
 * joueur l'a vu — « le menu n'a pas l'air de correspondre au vrai ».
 *
 * <p>Il ne lit que ce que son format porte vraiment : des {@code Widget} nommes, leurs enfants, et
 * cinq composants — transformation, texture, cerne, teinte, texte. Le reste (listes defilantes,
 * boutons, champs a curseur) etait ecrit en CODE chez l'original, et l'est encore ici : c'est le
 * meme partage, et c'est ce qui fait que le fichier se relit ligne a ligne a cote de l'ecran.
 *
 * <p>{@link #parse} est pure : elle prend le texte du fichier et rend l'arbre, sans toucher ni au
 * disque ni au jeu — c'est ce qui permet de la figer par un test.
 */
public final class CGuiDocument {

    private CGuiDocument() {}

    /** Lit le document {@code assets/academy/guis/<nom>.xml}. */
    public static CWidget read(String name) {
        String path = "assets/" + AcademyCraft.MOD_ID + "/guis/" + name + ".xml";
        try (InputStream in = CGuiDocument.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Document CGui introuvable : " + path);
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Document CGui illisible : " + path, e);
        }
    }

    /** L'arbre d'un document, a partir de son texte. */
    public static CWidget parse(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        var document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

        Element first = firstWidget(document.getDocumentElement());
        if (first == null) throw new IllegalStateException("Document CGui sans widget racine");
        return build(first);
    }

    private static Element firstWidget(Element parent) {
        for (Element child : elements(parent)) {
            if (child.getTagName().equals("Widget")) return child;
            Element deeper = firstWidget(child);
            if (deeper != null) return deeper;
        }
        return null;
    }

    private static CWidget build(Element element) {
        CWidget widget = new CWidget(attribute(element, "name"));

        for (Element child : elements(element)) {
            if (child.getTagName().equals("Widget")) {
                widget.add(build(child));
            } else if (child.getTagName().equals("Component")) {
                component(widget, child);
            }
        }
        return widget;
    }

    /** Un composant, range par son nom de classe — celui du fichier de l'original. */
    private static void component(CWidget widget, Element element) {
        String name = attribute(element, "class");
        if (name == null) return;

        switch (name.substring(name.lastIndexOf('.') + 1)) {
            case "Transform" -> transform(widget, element);
            case "DrawTexture" -> widget.texture = texture(element);
            case "Outline" -> widget.outline = outline(element);
            case "Tint" -> widget.tint = tint(element);
            case "ProgressBar" -> widget.bar = progressBar(element);
            case "TextBox" -> widget.text = textBox(element);
            default -> {
                // Un composant que le port n'a pas repris : celui des listes defilantes, des
                // boutons ou des barres. Les fichiers qui en portent en auront besoin un jour ;
                // celui-ci n'en a pas.
            }
        }
    }

    private static void transform(CWidget widget, Element element) {
        CWidget.Transform t = widget.transform;
        t.width = number(element, "width", 0);
        t.height = number(element, "height", 0);
        t.x = number(element, "x", 0);
        t.y = number(element, "y", 0);
        t.pivotX = number(element, "pivotX", 0);
        t.pivotY = number(element, "pivotY", 0);
        t.scale = number(element, "scale", 1);
        t.doesDraw = flag(element, "doesDraw", true);
        t.enabled = flag(element, "enabled", true);

        String alignWidth = text(element, "alignWidth");
        if (alignWidth != null) t.alignWidth = alignWidth.toUpperCase(Locale.ROOT);
        String alignHeight = text(element, "alignHeight");
        if (alignHeight != null) t.alignHeight = alignHeight.toUpperCase(Locale.ROOT);
    }

    private static CWidget.DrawTexture texture(Element element) {
        CWidget.DrawTexture out = new CWidget.DrawTexture();
        Element texture = first(element, "texture");
        if (texture != null && !flag(texture, "isNull", false)) {
            String value = texture.getTextContent().trim();
            if (!value.isEmpty()) out.texture = value;
        }
        read(out, color(element, "color"));
        out.zLevel = number(element, "zLevel", 0);
        return out;
    }

    private static CWidget.Outline outline(Element element) {
        CWidget.Outline out = new CWidget.Outline();
        read(out, color(element, "color"));
        out.lineWidth = number(element, "lineWidth", 1);
        return out;
    }

    /**
     * Une barre de progression : sa texture, sa direction, et son remplissage.
     *
     * <p>Le fichier de la 1.12.2 la decrit ainsi — {@code illustrating}, {@code texture},
     * {@code dir} — et le reste vient du code, comme le remplissage lui-meme : c'est la machine
     * qui sait ou elle en est, pas la page.
     */
    private static CWidget.ProgressBar progressBar(Element element) {
        CWidget.ProgressBar out = new CWidget.ProgressBar();
        String texture = text(element, "texture");
        if (texture != null && !texture.isEmpty()) out.texture = texture;
        out.illustrating = flag(element, "illustrating", false);
        out.progress = number(element, "progress", 0);

        String dir = text(element, "dir");
        if (dir != null) {
            try {
                out.dir = CWidget.ProgressBar.Direction.valueOf(dir.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                out.dir = CWidget.ProgressBar.Direction.RIGHT;
            }
        }

        int[] color = color(element, "color");
        if (color != null) out.color = color;
        return out;
    }

    private static CWidget.Tint tint(Element element) {
        CWidget.Tint out = new CWidget.Tint();
        int[] idle = color(element, "idleColor");
        int[] hover = color(element, "hoverColor");
        if (idle != null) out.idle = idle;
        if (hover != null) out.hover = hover;
        out.affectTexture = flag(element, "affectTexture", false);
        return out;
    }

    private static CWidget.TextBox textBox(Element element) {
        CWidget.TextBox out = new CWidget.TextBox();
        out.content = text(element, "content") == null ? "" : text(element, "content");
        out.allowEdit = flag(element, "allowEdit", false);
        out.doesEcho = flag(element, "doesEcho", false);
        String echo = text(element, "echoChar");
        if (echo != null && !echo.isEmpty()) out.echoChar = echo.charAt(0);

        String align = text(element, "heightAlign");
        if (align != null) out.heightAlign = align.toUpperCase(Locale.ROOT);

        Element option = first(element, "option");
        if (option != null) {
            out.fontSize = number(option, "fontSize", out.fontSize);
            String horizontal = text(option, "align");
            if (horizontal != null) out.align = horizontal.toUpperCase(Locale.ROOT);
            read(out, color(option, "color"));
        }
        return out;
    }

    private static void read(CWidget.DrawTexture out, int[] rgba) {
        if (rgba == null) return;
        out.red = rgba[0];
        out.green = rgba[1];
        out.blue = rgba[2];
        out.alpha = rgba[3];
    }

    private static void read(CWidget.Outline out, int[] rgba) {
        if (rgba == null) return;
        out.red = rgba[0];
        out.green = rgba[1];
        out.blue = rgba[2];
        out.alpha = rgba[3];
    }

    private static void read(CWidget.TextBox out, int[] rgba) {
        if (rgba == null) return;
        out.red = rgba[0];
        out.green = rgba[1];
        out.blue = rgba[2];
        out.alpha = rgba[3];
    }

    private static int[] color(Element parent, String tag) {
        Element element = first(parent, tag);
        if (element == null) return null;
        return new int[] { channel(element, "red", 255), channel(element, "green", 255),
                channel(element, "blue", 255), channel(element, "alpha", 255) };
    }

    private static int channel(Element element, String tag, int fallback) {
        Element found = first(element, tag);
        if (found == null) return fallback;
        try {
            return (int) Double.parseDouble(found.getTextContent().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String text(Element parent, String tag) {
        Element element = first(parent, tag);
        return element == null ? null : element.getTextContent().trim();
    }

    private static double number(Element parent, String tag, double fallback) {
        String value = text(parent, tag);
        if (value == null || value.isEmpty()) return fallback;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean flag(Element parent, String tag, boolean fallback) {
        String value = text(parent, tag);
        return value == null || value.isEmpty() ? fallback : Boolean.parseBoolean(value);
    }

    private static String attribute(Element element, String name) {
        String value = element.getAttribute(name);
        return value == null || value.isEmpty() ? null : value;
    }

    private static Element first(Element parent, String tag) {
        for (Element child : elements(parent)) {
            if (child.getTagName().equals(tag)) return child;
        }
        return null;
    }

    /** Les enfants elements d'un noeud, dans l'ordre du fichier — donc dans l'ordre du dessin. */
    private static java.util.List<Element> elements(Element parent) {
        java.util.List<Element> out = new java.util.ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element) out.add(element);
        }
        return out;
    }
}
