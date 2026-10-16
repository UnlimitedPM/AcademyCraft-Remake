package cn.academy.client.gui.cgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * L'ecran d'une machine : le {@code ContainerUI} de l'original, pose sur un conteneur vanilla.
 *
 * <p>C'est la piece qui manquait pour que les neuf machines se ressemblent enfin a l'original. Elle
 * tient trois choses, et rien d'autre :
 *
 * <ol>
 *   <li>le <b>cadre</b> : une racine de 172 sur 187 calee sur les slots, et les documents de l'ecran
 *       poses dedans ;</li>
 *   <li>les <b>pages</b> : une par onglet, une seule visible a la fois ;</li>
 *   <li>le <b>panneau d'infos</b> : la ou la machine dit ou elle en est.</li>
 * </ol>
 *
 * <h2>Le +31 et le +20</h2>
 *
 * <p>Le conteneur vanilla fait 176 sur 166 ; celui-ci fait <b>207 sur 186</b>, et ce n'est pas un
 * ornement. Les slots se comptent depuis {@code guiLeft / guiTop}, le document depuis le coin de sa
 * racine : le 31 et le 20 sont exactement ce qu'il faut pour que les deux coins <b>coincident</b>,
 * a un demi-pixel pres. C'est le calcul de l'original, et sans lui les slots tombent a cote de leur
 * fond.
 *
 * <h2>Ce qui n'est pas encore la</h2>
 *
 * <p>Le panneau d'infos n'a pour l'instant que ses lignes de propriete : les histogrammes, les
 * valeurs modifiables et les boutons viendront avec la premiere machine qui en a besoin. La racine
 * fait deja 172 et les onglets sont deja poses a −20 et tous les 22 pixels, comme chez l'original,
 * donc ce qui manque est <b>dedans</b> et non autour.
 */
@OnlyIn(Dist.CLIENT)
public abstract class CGuiContainerScreen<T extends AbstractContainerMenu>
        extends AbstractContainerScreen<T> {

    /** Une page d'ecran : son identifiant, et son arbre. */
    protected record Page(String id, CWidget window) {}

    private final CWidget root = new CWidget("root");
    private final CWidget info = new CWidget("info");
    private final List<Page> pages = new ArrayList<>();

    /** Les fonds qui respirent : les widgets dont le nom commence par {@code ui_}, comme chez
     * l'original, qui les faisait osciller entre 0,675 et 0,85 d'opacite. */
    private final List<CWidget.DrawTexture> breathing = new ArrayList<>();

    /** Les histogrammes du panneau d'infos : leur barre, leur texte, et d'ou ils lisent. */
    private final List<Reading> readings = new ArrayList<>();

    private record Reading(CWidget.ProgressBar bar, CWidget.TextBox text, CGuiHist source) {}

    private CGuiView view;
    private int current;

    /** Le bas du panneau d'infos, ou la prochaine ligne se pose. */
    private double infoY = 10;

    protected CGuiContainerScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = CGuiContainerLayout.WIDTH;
        this.imageHeight = CGuiContainerLayout.HEIGHT;
    }

    /**
     * Ce que la machine construit a l'ouverture : ses pages, et ses lignes d'infos.
     *
     * <p>Appelee a chaque {@code init}, comme l'original qui rebatissait ses pages a chaque
     * redimensionnement — c'est ce qui remet les onglets en place quand la fenetre change de taille.
     */
    protected abstract void build(CGuiContainerScreen<T> screen);

    /**
     * Ouvre une page a partir d'un document de l'original, par exemple {@code rework/page_solar}.
     *
     * @return la fenetre de la page, pour la machine qui a quelque chose a y poser elle-meme — le
     *     noeud d'imagination lui ajoute son animation, que son document ne porte pas
     */
    protected CWidget page(String id, String document) {
        CWidget window = CGuiContainerLayout.page(CGuiDocument.read(document).copy());
        pages.add(new Page(id, window));
        return window;
    }

    /** La page d'inventaire d'une machine sans page propre : {@code page_inv} et son fond a elle. */
    protected CWidget inventoryPage(String machine) {
        CWidget window = page("inv", "rework/page_inv");
        CWidget block = window.child("ui_block");
        if (block != null && block.texture != null) {
            block.texture.texture = "academy:textures/guis/ui/ui_" + machine + ".png";
        }
        return window;
    }

    /**
     * La fenetre d'une page deja ouverte, ou {@code null}.
     *
     * <p>C'est pour les machines qui ont quelque chose a poser <b>sur</b> leur page — des boutons, une
     * liste. Leur {@code build} repasse par la a chaque reconstruction du panneau, et recreer la page
     * en laisserait une deuxieme, jamais posee dans l'arbre et pourtant bien presente : la liste
     * qu'on y aurait accrochee ne se verrait pas.
     */
    protected CWidget pageWindow(String id) {
        for (Page page : pages) {
            if (page.id().equals(id)) return page.window();
        }
        return null;
    }

    /** Ce qu'un widget de la page fait quand on clique dessus : l'{@code LeftClickEvent} de l'original. */
    protected void onClick(CWidget widget, Runnable action) {
        if (view != null) view.onClick(widget, action);
    }

    /** Et ce qu'il fait quand on valide ce qu'il contient. */
    protected void onConfirm(CWidget widget, Consumer<String> action) {
        if (view != null) view.onConfirm(widget, action);
    }

    /** Le champ qui prend le clavier, ou {@code null} pour le rendre. */
    protected void focus(CWidget widget) {
        if (view != null) view.focus(widget);
    }

    /** La page courante, celle que les slots concernent. */
    protected Page current() {
        return pages.isEmpty() ? null : pages.get(current);
    }

    /** Les slots ne se dessinent et ne s'ouvrent que sur la page d'inventaire. */
    protected boolean inventoryShown() {
        Page page = current();
        return page != null && page.id().equals("inv");
    }

    /** L'arbre du panneau d'infos, pour y ajouter ce qu'on veut. */
    protected CWidget info() {
        return info;
    }

    /**
     * Une ligne d'information : un nom a gauche, une valeur a droite, comme l'original.
     *
     * @return la <b>ligne</b> posee, et non son champ : c'est elle qu'on cache ou qu'on deplace,
     *     et son champ se retrouve par {@code child("value")}
     */
    protected CWidget property(String key, String value) {
        CWidget line = new CWidget("line" + (int) infoY);
        line.transform.x = 6;
        line.transform.width = 90;
        line.transform.height = 8;

        CWidget name = new CWidget("key");
        name.transform.width = 40;
        name.transform.height = 8;
        name.text = new CWidget.TextBox();
        name.text.content = key;
        name.text.align = "CENTER";
        name.text.heightAlign = "CENTER";
        name.text.color(255, 255, 255);
        line.add(name);

        CWidget amount = new CWidget("value");
        amount.transform.x = 40;
        amount.transform.width = 40;
        amount.transform.height = 8;
        amount.text = new CWidget.TextBox();
        amount.text.content = value;
        amount.text.align = "CENTER";
        amount.text.heightAlign = "CENTER";
        amount.text.color(255, 255, 255);
        line.add(amount);

        element(line);
        return line;
    }

    /** Un peu de blanc entre deux renseignements. */
    protected void blank(double height) {
        infoY += height;
    }

    /**
     * Une ligne d'information que le joueur peut changer : sa valeur s'ecrit, entre deux crochets.
     *
     * <p>C'est le {@code property(key, value, editCallback)} de l'original, crochets compris : ils
     * disent d'un coup d'oeil que ce champ se clique. Tant qu'il n'a pas le clavier il montre la
     * valeur de la machine ; des qu'il l'a, il se vide et attend une saisie — et c'est la touche
     * Entree qui valide, ce qui appelle {@code onEdit}.
     *
     * <p>La valeur envoyee est celle du champ, telle quelle : a la machine de decider si elle
     * l'accepte. Un nom de noeud, par exemple, n'est ecrit que si celui qui le demande l'a pose.
     *
     * @return la ligne posee, comme {@link #property(String, String)} ; son champ est son
     *     {@code child("value")}, et ses crochets sont ses enfants nommes {@code bracket}
     */
    protected CWidget property(String key, String value, Consumer<String> onEdit) {
        return property(key, value, onEdit, false);
    }

    /** Le meme, masque : c'est ainsi que se saisit un mot de passe. */
    protected CWidget property(String key, String value, Consumer<String> onEdit,
                               boolean password) {
        CWidget line = property(key, value);
        CWidget field = line.child("value");
        if (onEdit == null || field == null || field.text == null) return line;

        field.text.allowEdit = true;
        field.text.doesEcho = password;

        // Les crochets se posent dans la ligne, de part et d'autre du champ : trois unites a gauche
        // du champ et deux a droite, comme l'original — ils debordent donc de la valeur, et c'est
        // voulu, c'est ce qui les fait se voir.
        line.add(bracket("[", field.transform.x - 4));
        line.add(bracket("]", field.transform.x + field.transform.width + 2));

        if (view != null) view.onConfirm(field, onEdit);
        return line;
    }

    /** Un crochet, de part et d'autre du champ : dix unites de large, comme chez l'original. */
    private static CWidget bracket(String text, double x) {
        CWidget out = new CWidget("bracket");
        out.transform.x = x;
        out.transform.width = 10;
        out.transform.height = 8;
        out.text = new CWidget.TextBox();
        out.text.content = text;
        out.text.heightAlign = "CENTER";
        out.text.color(255, 255, 255);
        return out;
    }

    /** Vrai si ce champ a le clavier : un champ qu'on est en train d'ecrire ne se reecrit pas. */
    protected boolean isFocused(CWidget widget) {
        return view != null && view.focused() == widget;
    }

    /**
     * Un bouton : du texte au milieu du panneau, qui s'allume sous le curseur et fait ce qu'on lui dit.
     *
     * <p>C'est le {@code button} de l'original, et il n'a <b>pas de fond</b> : sa largeur est celle de
     * son texte, cinquante unites au minimum, et son seul retour visuel est sa luminosite — quatre
     * cinquiemes au repos, pleine sous le curseur. C'est peu, et c'est ce que l'original faisait ;
     * l'allumage, lui, se fait au dessin, dans {@link CGuiView}.
     */
    protected void button(String text, Runnable action) {
        CWidget widget = new CWidget("button");
        widget.transform.alignWidth = "CENTER";
        widget.transform.width = Math.max(50,
            cn.academy.client.gui.AcademyFonts.width(font, text) + 5);
        widget.transform.height = 8;

        widget.text = new CWidget.TextBox();
        widget.text.content = text;
        widget.text.align = "CENTER";
        widget.text.heightAlign = "CENTER";

        element(widget);
        if (view != null) view.onClick(widget, action);
    }

    /**
     * Rebatit le panneau d'infos, et lui seul.
     *
     * <p>C'est ce que l'original appelait {@code infoPage.reset()} : certaines machines
     * <b>changent de forme</b> — le Matrix ne montre ses deux champs de reseau qu'une fois le reseau
     * cree — et un panneau dont les lignes dependent de l'etat ne peut pas etre bati une fois pour
     * toutes a l'ouverture. Les pages, elles, ne bougent pas : elles ne sont pas touchees.
     *
     * <p>Le placement est refait dans la foulee : les widgets qui viennent de naitre n'ont pas encore
     * de position a l'ecran, et les dessiner avant de les poser les poserait tous au coin.
     */
    protected void rebuild() {
        info.children().clear();
        readings.clear();
        infoY = 10;

        // Ce que la machine rebatit, c'est son PANNEAU ; ses pages, elle les recree au passage parce
        // que c'est ainsi que son build est ecrit. Celles-la ne sont jamais posees dans l'arbre, donc
        // personne ne les voit — mais elles s'accumuleraient dans la liste a chaque reconstruction.
        int before = pages.size();

        build(this);

        while (pages.size() > before) {
            pages.remove(pages.size() - 1);
        }

        CGuiContainerLayout.info(info, infoY);

        if (view != null) {
            view.place(leftPos, topPos, imageWidth, imageHeight);
        }
    }

    /**
     * Repose l'arbre dans le cadre, sans rien reconstruire.
     *
     * <p>A appeler quand un ecran a bouge des widgets <b>hors</b> de son {@code build} : les widgets
     * qui viennent de naitre n'ont pas encore de place a l'ecran, et les dessiner avant de les poser
     * les poserait tous au coin. {@link #rebuild()} le fait pour le panneau d'infos ; celui-ci le fait
     * pour ce qu'un ecran ajoute a sa page, comme l'arbre du developpeur qui gagne un noeud des qu'une
     * competence s'ouvre.
     */
    protected void reflow() {
        if (view != null) {
            view.place(leftPos, topPos, imageWidth, imageHeight);
        }
    }

    /**
     * Le fond translucide de l'ecran.
     *
     * <p>Depuis 1.17 vanilla ne le dessine plus derriere un conteneur : son {@code render} appelle
     * {@code renderBg} directement, donc c'est a l'ecran de le poser. L'original tenait en une ligne —
     * {@code drawDefaultBackground} — et c'est exactement ce gris-la : deux noirs transparents
     * superposes, l'un en haut de l'ecran, l'autre en bas.
     *
     * <p>Et une couche de plus, par-dessus, que <b>l'original n'avait pas</b>. Le joueur a compare :
     * « le grand fond noir n'est pas assez sombre », et pour toutes les machines. La cause est
     * probablement le flou de menu de cette version — le monde derriere n'est plus net comme en
     * 1.12.2, et ce que les deux noirs laissent passer ne se lit donc pas de la meme facon. Que ce
     * soit cela ou non, le remede est le meme, et il est volontaire : un quart de noir en plus. Il se
     * retire en mettant {@code BACKDROP_EXTRA} a zero.
     */
    private static final int BACKDROP_EXTRA = 0x40000000;

    /** Un widget de l'arbre, par son nom : c'est ainsi qu'une page retrouve son cadre anime. */
    protected CWidget widget(String name) {
        if (view != null) {
            CWidget found = view.find(name);
            if (found != null) return found;
        }

        // Pendant `build`, la vue ne connait pas encore les pages : elles ne sont posees dans la
        // racine qu'APRES lui. Tout ce qui se cable la — boutons, barres, textes — se cablait donc
        // dans le vide, et les deux fleches du formeur de metal ne faisaient rien du tout. La vue
        // reste cherchee d'abord, parce que c'est elle qui porte les onglets et le panneau d'infos.
        for (Page page : pages) {
            CWidget found = find(page.window(), name);
            if (found != null) return found;
        }
        return null;
    }

    /** Le widget de ce nom, cherche dans tout cet arbre. */
    private static CWidget find(CWidget widget, String name) {
        if (widget.name().equals(name)) return widget;
        for (CWidget child : widget.children()) {
            CWidget found = find(child, name);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Un separateur : le titre d'une section, en petit et a demi efface.
     *
     * <p>C'est le {@code sepline} de l'original : une ligne a trois unites du bord, large de tout le
     * panneau moins trois, trois unites de blanc avant elle, et le texte de la cle
     * {@code ac.gui.common.sep.<id>} en blanc a soixante pour cent.
     */
    protected void seplineInfo() {
        sepline("info");
    }

    protected void sepline(String id) {
        CWidget line = new CWidget("sep_" + id);
        line.transform.x = 3;
        line.transform.width = CGuiContainerLayout.INFO_WIDTH - 3;
        line.transform.height = 8;

        CWidget title = new CWidget("title");
        title.transform.width = CGuiContainerLayout.INFO_WIDTH - 3;
        title.transform.height = 8;
        title.text = new CWidget.TextBox();
        title.text.content = Component.translatable("ac.gui.common.sep." + id).getString();
        title.text.alpha = 153;
        line.add(title);

        blank(3);
        element(line);
    }

    /**
     * Pose un element du panneau, puis avance le curseur : c'est l'{@code element()} de l'original.
     *
     * <p>Le y du widget, c'est le curseur ; ce qu'il fait avancer, c'est sa hauteur <b>une fois mise
     * a l'echelle</b> — un histogramme de 210 unites dessine a quatre dixiemes n'en prend que 84. Le
     * poser soi-meme sans passer par ici, c'est se tromper de vingt pixels : c'est ce qui est arrive
     * au premier jet, ou la boite tombait sur la legende qui la suit.
     */
    private CWidget element(CWidget widget) {
        widget.transform.y = infoY;
        infoY += widget.transform.height * widget.transform.scale;
        info.add(widget);
        return widget;
    }

    /**
     * Un histogramme : la boite de l'original, une barre par element, et leur legende.
     *
     * <p>Repris de {@code InfoArea.histogram}, nombres compris. La boite fait 210 unites dessinees a
     * quatre dixiemes — soit 84 pixels — les barres seize de large et cent vingt de haut, posees a
     * partir de 56 et tous les quarante. Un histogramme prend donc moins de place qu'il n'en
     * dessine : c'est ce que fait son {@code blank(-30)}, et c'est ce qui colle la boite aux lignes
     * qui la suivent au lieu de laisser un trou.
     */
    protected void histogram(CGuiHist... elems) {
        CWidget box = new CWidget("histogram");
        box.transform.width = CGuiHist.BOX;
        box.transform.height = CGuiHist.BOX;
        box.transform.scale = CGuiHist.BOX_SCALE;
        box.texture = new CWidget.DrawTexture();
        box.texture.texture = CGuiHist.TEXTURE;

        for (int i = 0; i < elems.length; i++) {
            CWidget row = new CWidget("bar" + i);
            row.transform.width = CGuiHist.BAR_WIDTH;
            row.transform.height = CGuiHist.BAR_HEIGHT;
            row.transform.x = CGuiHist.BAR_X + i * CGuiHist.BAR_STEP;
            row.transform.y = CGuiHist.BAR_Y;
            row.bar = new CWidget.ProgressBar();
            row.bar.dir = CWidget.ProgressBar.Direction.UP;
            row.bar.color = elems[i].color();
            box.add(row);
        }

        blank(-CGuiHist.BOX_LIFT);
        element(box);

        for (int i = 0; i < elems.length; i++) {
            CWidget.TextBox value = legend(elems[i]);
            readings.add(new Reading(box.children().get(i).bar, value, elems[i]));
        }
    }

    /**
     * La ligne de legende d'un element : le carre de sa couleur, son nom, et sa valeur.
     *
     * @return le champ de la valeur, que l'ecran reecrit a chaque image
     */
    private CWidget.TextBox legend(CGuiHist elem) {
        CWidget line = new CWidget("hist" + (int) infoY);
        line.transform.x = 6;
        line.transform.width = 90;
        line.transform.height = 8;

        // Le carre, six unites de cote, pose trois unites a GAUCHE de la ligne : c'est le pos(-3)
        // de l'original, qui le fait deborder du panneau.
        CWidget icon = new CWidget("icon");
        icon.transform.x = -3;
        icon.transform.y = 1;
        icon.transform.width = 6;
        icon.transform.height = 6;
        icon.texture = new CWidget.DrawTexture();
        icon.texture.red = elem.color()[0];
        icon.texture.green = elem.color()[1];
        icon.texture.blue = elem.color()[2];
        line.add(icon);

        CWidget name = new CWidget("key");
        name.transform.x = 4;
        name.transform.width = 32;
        name.transform.height = 8;
        name.text = new CWidget.TextBox();
        name.text.content = Component.translatable(elem.id()).getString();
        name.text.align = "CENTER";
        name.text.heightAlign = "CENTER";
        line.add(name);

        CWidget value = new CWidget("value");
        value.transform.x = 40;
        value.transform.width = 40;
        value.transform.height = 8;
        value.text = new CWidget.TextBox();
        value.text.content = elem.desc().get();
        value.text.align = "CENTER";
        value.text.heightAlign = "CENTER";
        line.add(value);

        element(line);
        return value.text;
    }

    @Override
    protected void init() {
        super.init();

        view = new CGuiView(this, root);
        layoutRoot(root);

        pages.clear();
        breathing.clear();
        readings.clear();
        info.children().clear();
        infoY = 10;
        build(this);

        if (showsInfo()) CGuiContainerLayout.info(info, infoY);

        // La racine porte les onglets puis les pages, dans cet ordre : l'original ajoutait le
        // bouton AVANT sa fenetre, donc la fenetre passe au-dessus du bouton.
        root.children().clear();
        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            if (showsTabs()) root.add(tab(page, i));
            root.add(page.window());
            collectBreathing(page.window());
        }
        if (showsInfo()) root.add(info);

        show(0);
    }

    /** Les fonds de page qui respirent : ceux que l'original nommait {@code ui_quelquechose}. */
    private void collectBreathing(CWidget widget) {
        if (widget.texture != null && widget.name().startsWith("ui_")) {
            breathing.add(widget.texture);
        }
        for (CWidget child : widget.children()) {
            collectBreathing(child);
        }
    }

    /** L'onglet d'une page : une copie du bouton de l'original, avec l'icone de son identifiant. */
    private CWidget tab(Page page, int index) {
        CWidget button = CGuiContainerLayout.tab(CGuiDocument.read("rework/pageselect").copy(),
                index, page.id());
        view.onClick(button, () -> show(index));
        return button;
    }

    /** Ce que fait un onglet : toutes les fenetres se cachent, la sienne se montre. */
    private void show(int index) {
        current = index;
        for (Page page : pages) {
            page.window().transform.doesDraw = false;
        }
        if (!pages.isEmpty()) pages.get(index).window().transform.doesDraw = true;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (view == null) return;

        // Le fond translucide de l'ecran, d'abord. Depuis 1.17 vanilla ne le dessine plus derriere
        // un conteneur : son render appelle renderBg directement, donc c'est a l'ecran de le poser.
        // L'original tenait en une ligne — drawDefaultBackground — et c'est exactement ce gris-la :
        // deux noirs transparents superposes, l'un en haut de l'ecran, l'autre en bas.
        super.renderBackground(graphics);
        if (BACKDROP_EXTRA != 0) {
            graphics.fill(0, 0, width, height, BACKDROP_EXTRA);
        }

        // Puis le document, pose sur le conteneur et pas sur l'ecran : c'est ce qui met le fond de la
        // page et les slots dans le meme repere. Les deux trames se rejoignent a un demi-pixel.
        view.place(leftPos, topPos, imageWidth, imageHeight);

        // Ce que la machine a a dire ce tick-la, puis le dessin de tout l'arbre.
        frame(graphics, mouseX, mouseY);
        breathe();
        readHistograms();
        view.draw(graphics, mouseX, mouseY);
    }

    /** Relit les histogrammes : chaque barre se remplit, chaque valeur se reecrit. */
    private void readHistograms() {
        for (Reading reading : readings) {
            // L'original ne descend jamais jusqu'a zero : une barre vide garde un cheveu de
            // remplissage, et c'est ce qui la rend lisible.
            reading.bar().progress = Math.max(0.03,
                    Math.min(1.0, reading.source().value().getAsDouble()));
            reading.text().content = reading.source().desc().get();
        }
    }

    /** Le souffle des fonds de page : une seule opacite pour tous, comme chez l'original. */
    private void breathe() {
        if (breathing.isEmpty()) return;

        int alpha = breatheAlpha();
        for (CWidget.DrawTexture texture : breathing) {
            texture.alpha = alpha;
        }
    }

    /**
     * La geometrie de la racine : le cadre du document, accroche au conteneur.
     *
     * <p>C'est ce qui met le document sur les slots, et c'est donc la seule chose que les machines
     * n'ont pas a changer. La Developer, elle, n'a pas de slots : sa page n'est pas un cadre de
     * 207 sur 186 pose au milieu de l'ecran mais l'ecran entier, et sa racine se pose donc autrement.
     */
    protected void layoutRoot(CWidget root) {
        CGuiContainerLayout.root(root);
    }

    /**
     * Vrai si l'ecran montre les onglets de ses pages.
     *
     * <p>Chaque page a son onglet, colle au bord gauche du cadre, et c'est ainsi qu'une machine
     * passe de son panneau a son inventaire. Un ecran qui n'a qu'une page et pas d'inventaire n'a
     * rien a proposer a gauche de son cadre : la Developer, encore elle, ou cet onglet n'aurait meme
     * pas d'icone a montrer.
     */
    protected boolean showsTabs() {
        return true;
    }

    /**
     * Vrai si l'ecran montre le panneau d'infos, a droite du cadre.
     *
     * <p>C'est le panneau noir ou se posent les lignes de {@code property} et les histogrammes. Une
     * page qui porte deja tout ce qu'elle a a dire n'en veut pas : le panneau se poserait par-dessus.
     */
    protected boolean showsInfo() {
        return true;
    }

    /**
     * L'opacite du souffle a cet instant, entre 172 et 216 sur 255.
     *
     * <p>Les fonds de page en respirent d'eux-memes, mais l'original en faisait aussi respirer
     * l'animation du noeud, qui la multiplie a ses images. Une machine qui en a besoin la demande ici
     * plutot que de refaire le calcul.
     */
    protected int breatheAlpha() {
        return CGuiContainerLayout.breatheAlpha(net.minecraft.Util.getMillis() / 1000.0);
    }

    /**
     * Les slots ne se dessinent, ne se survolent et ne s'ouvrent que sur la page qui les concerne.
     *
     * <p>Vanilla dessine ses slots dans son propre {@code render}, et le port ne peut pas l'en
     * empecher slot par slot — la methode qui les dessine est privee. Quand la page courante n'est
     * pas celle de l'inventaire, on ne l'appelle donc pas du tout : il reste le fond, le document
     * de l'original, et rien d'autre. Sans cela, une page qui ne montre pas l'inventaire laisserait
     * quand meme ses 36 emplacements cliquables sous le curseur, et un clic dans le vide viderait
     * l'inventaire du joueur.
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (inventoryShown()) {
            super.render(graphics, mouseX, mouseY, partialTick);

            // L'infobulle de l'objet survole : vanilla ne la dessine PAS pour un conteneur. Ce n'est
            // pas une omission de notre part, c'est le fonctionnement de cette version — l'inventaire
            // du jeu, lui, l'appelle a la main dans son propre render, et le port faisait de meme
            // dans ses anciens ecrans. Sans cette ligne, on survole un objet sans jamais voir son nom,
            // ni dans la machine, ni dans son propre inventaire.
            //
            // Elle doit venir APRES super.render : c'est ce dernier qui trouve l'emplacement survole
            // et qui dessine les objets, donc c'est lui qui sait quoi montrer.
            renderTooltip(graphics, mouseX, mouseY);
            return;
        }
        // Page sans inventaire : le document de l'original et rien d'autre — ni slots, ni survol.
        // Le fond translucide vient de renderBg, comme dans l'autre branche.
        renderBg(graphics, partialTick, mouseX, mouseY);
    }

    /**
     * Rien : les titres du conteneur sont deja dans les images du fichier de l'original, qui a
     * l'inventaire du joueur et le nom de la machine. Vanilla, lui, les ecrirait une deuxieme fois.
     */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX,
                                double dragY) {
        if (!inventoryShown()) return false;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /** Ce que la machine met a jour avant chaque dessin : ses barres, ses valeurs. */
    protected void frame(GuiGraphics graphics, double mouseX, double mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // La vue d'abord, et pour une raison qui n'a rien d'evident : l'ecran d'une machine EST un
        // conteneur d'inventaire, et `AbstractContainerScreen.mouseClicked` rend VRAI des qu'un clic
        // tombe dans son cadre — verifie au pont, sur l'ecran du formeur. Le document de l'original
        // ne voyait donc jamais le clic de ses propres boutons : ses fleches de mode ne faisaient
        // rien, alors que le bouton LEARN du developpeur, lui, marchait (sa page n'est pas un
        // inventaire, donc vanilla n'etait pas appele du tout).
        //
        // Et la vue ne prend la main que si un widget a VRAIMENT quelque chose a faire : le fond de
        // la page couvre tout, et un clic sur un emplacement doit rester a l'inventaire.
        if (view != null && view.click(mouseX, mouseY, button)) return true;

        boolean slots = inventoryShown();
        if (slots && super.mouseClicked(mouseX, mouseY, button)) return true;
        return slots;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (view != null && view.charTyped(codePoint)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Tant qu'un champ editable a le clavier, le jeu n'y touche pas : c'est ce que faisait
        // l'original, et sans cela taper un nom fermerait l'ecran.
        if (view != null && view.keyPressed(keyCode)) return true;
        if (view != null && view.editing()) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
