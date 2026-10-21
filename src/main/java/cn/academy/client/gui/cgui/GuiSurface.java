package cn.academy.client.gui.cgui;

/**
 * La fiche qui s'ouvre sur une page : un voile noir, ouvert et referme en un cinquieme de seconde.
 *
 * <p>C'est la {@code Cover} de l'original : la page entiere sous un noir a sept dixiemes — donc
 * encore lisible derriere — et par-dessus ce que la fiche a a dire. Le joueur y tenait : « quand on
 * lit ce que fait l'attaque, on doit quand meme voir l'interface de base derriere ».
 *
 * <p>Elle s'ouvre en <b>dernier dans la page</b>, donc au-dessus de tout le reste, et se referme au
 * premier clic. Le contenu, lui, appartient a l'ecran : la machine du developpeur y met de quoi
 * apprendre, l'application du terminal s'y contente de lire.
 */
public final class GuiSurface {

    /** L'opacite du voile : sept dixiemes de noir. */
    private static final int VEIL_ALPHA = 178;

    /** Le fondu, dans les deux sens. */
    private static final double VEIL_FADE = 0.2;

    private final GuiPageHost host;
    private final double pageWidth;
    private final double pageHeight;

    private CWidget widget;
    private double since;
    private boolean closing;

    public GuiSurface(GuiPageHost host, double pageWidth, double pageHeight) {
        this.host = host;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
    }

    /** Vrai quand une fiche est ouverte, fondu de fermeture compris. */
    public boolean opened() {
        return widget != null;
    }

    /**
     * Ouvre une fiche sur la page.
     *
     * <p>Le contenu prend la <b>boite de la page</b>. Sans cela il n'a ni largeur ni hauteur — c'est
     * le defaut d'un widget neuf — et ses textes, qui s'accrochent au CENTRE de leur parent, se
     * posaient alors sur le coin haut-gauche de la page : ceux dont le y est negatif, c'est-a-dire le
     * nom, le niveau et la DESCRIPTION, tombaient au-dessus du bord et ne se voyaient pas. Le joueur
     * l'a dit ainsi : « quand je click sur un pouvoir, je n'ai pas la description ».
     */
    public void open(CWidget content) {
        CWidget page = host.page();
        if (page == null) return;

        close();

        widget = new CWidget("surface");
        widget.transform.width = pageWidth;
        widget.transform.height = pageHeight;
        widget.transform.x = 0;
        widget.transform.y = 0;
        widget.transform.alignWidth = "LEFT";
        widget.transform.alignHeight = "TOP";
        widget.texture = new CWidget.DrawTexture();
        widget.texture.red = 0;
        widget.texture.green = 0;
        widget.texture.blue = 0;
        widget.texture.alpha = 0;

        content.transform.width = pageWidth;
        content.transform.height = pageHeight;
        content.transform.alignWidth = "LEFT";
        content.transform.alignHeight = "TOP";
        widget.add(content);

        page.add(widget);
        host.onClick(widget, this::beginClose);
        host.reflow();

        since = now();
        closing = false;
    }

    /** Referme la fiche, en la faisant d'abord s'effacer. */
    public void beginClose() {
        if (widget == null || closing) return;
        closing = true;
        since = now();
    }

    /** Retire la fiche d'un coup, sans fondu : c'est ce qu'il faut avant d'en ouvrir une autre. */
    public void close() {
        CWidget page = host.page();
        if (widget != null && page != null) page.remove(widget);
        widget = null;
        closing = false;
    }

    /** Le fondu de la fiche : elle monte a sept dixiemes, et redescend quand on la referme. */
    public void animate() {
        if (widget == null || widget.texture == null) return;

        double progress = clamp01((now() - since) / VEIL_FADE);
        double alpha = closing ? 1 - progress : progress;
        widget.texture.alpha = (int) Math.round(alpha * VEIL_ALPHA);

        if (closing && progress >= 1) close();
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private static double now() {
        return net.minecraft.Util.getMillis() / 1000.0;
    }
}
