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

    /** Ouvre une page a partir d'un document de l'original, par exemple {@code rework/page_solar}. */
    protected void page(String id, String document) {
        pages.add(new Page(id, CGuiContainerLayout.page(CGuiDocument.read(document).copy())));
    }

    /** La page d'inventaire d'une machine sans page propre : {@code page_inv} et son fond a elle. */
    protected void inventoryPage(String machine) {
        page("inv", "rework/page_inv");
        CWidget block = pages.get(pages.size() - 1).window().child("ui_block");
        if (block != null && block.texture != null) {
            block.texture.texture = "academy:textures/guis/ui/ui_" + machine + ".png";
        }
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

    /** Une ligne d'information : un nom a gauche, une valeur a droite, comme l'original. */
    protected CWidget property(String key, String value) {
        CWidget line = new CWidget("line" + (int) infoY);
        line.transform.x = 6;
        line.transform.y = infoY;
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

        info.add(line);
        infoY += 8;
        return amount;
    }

    /** Un peu de blanc entre deux renseignements. */
    protected void blank(double height) {
        infoY += height;
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

        blank(-30);
        info.add(box);
        blank(CGuiHist.ADVANCE);

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
        line.transform.y = infoY;
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

        info.add(line);
        infoY += 8;
        return value.text;
    }

    @Override
    protected void init() {
        super.init();

        view = new CGuiView(this, root);
        CGuiContainerLayout.root(root);

        pages.clear();
        breathing.clear();
        readings.clear();
        info.children().clear();
        infoY = 10;
        build(this);

        CGuiContainerLayout.info(info, infoY);

        // La racine porte les onglets puis les pages, dans cet ordre : l'original ajoutait le
        // bouton AVANT sa fenetre, donc la fenetre passe au-dessus du bouton.
        root.children().clear();
        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            root.add(tab(page, i));
            root.add(page.window());
            collectBreathing(page.window());
        }
        root.add(info);

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

        // Puis le document, pose sur le conteneur et pas sur l'ecran : c'est ce qui met le fond de la
        // page et les slots dans le meme repere. Les deux trames se rejoignent a un demi-pixel.
        view.place(leftPos, topPos, CGuiContainerLayout.WIDTH, CGuiContainerLayout.HEIGHT);

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

        int alpha = CGuiContainerLayout.breatheAlpha(net.minecraft.Util.getMillis() / 1000.0);
        for (CWidget.DrawTexture texture : breathing) {
            texture.alpha = alpha;
        }
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
        boolean slots = inventoryShown();
        if (slots && super.mouseClicked(mouseX, mouseY, button)) return true;
        if (view != null && view.click(mouseX, mouseY, button)) return true;
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
