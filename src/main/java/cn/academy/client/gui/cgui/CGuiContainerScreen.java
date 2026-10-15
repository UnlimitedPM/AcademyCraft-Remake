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

    /** Le conteneur : 176 + 31 et 166 + 20, pour que slots et documents partagent un coin. */
    private static final int CONTAINER_WIDTH = 207;
    private static final int CONTAINER_HEIGHT = 186;

    /** La racine de l'original : 172 sur 187, posee a dix-huit unites a gauche des slots. */
    private static final double ROOT_WIDTH = 172;
    private static final double ROOT_HEIGHT = 187;
    private static final double ROOT_X = -18;

    /** Et la page, 176 sur 187 : centree dans la racine, elle tombe donc DEUX unites a gauche. */
    private static final double PAGE_WIDTH = 176;
    private static final double PAGE_HEIGHT = 187;

    /** Les onglets : vingt-quatre unites, a l'echelle 0,7, a vingt unites a gauche, tous les 22. */
    private static final double TAB_SCALE = 0.7;
    private static final double TAB_STEP = 22;
    private static final double TAB_X = -20;

    /** Le fond du panneau d'infos, et le gris de ses lignes. */
    private static final int INFO_FILL = 0x80000000;
    private static final int INFO_TEXT = 0xFFFFFFFF;

    /** Une page d'ecran : son identifiant, et son arbre. */
    protected record Page(String id, CWidget window) {}

    private final CWidget root = new CWidget("root");
    private final CWidget info = new CWidget("info");
    private final List<Page> pages = new ArrayList<>();

    private CGuiView view;
    private int current;

    /** Le bas du panneau d'infos, ou la prochaine ligne se pose. */
    private double infoY = 10;

    protected CGuiContainerScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = CONTAINER_WIDTH;
        this.imageHeight = CONTAINER_HEIGHT;
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
        CWidget window = CGuiDocument.read(document).copy();
        window.transform.width = PAGE_WIDTH;
        window.transform.height = PAGE_HEIGHT;
        window.transform.alignWidth = "CENTER";
        window.transform.alignHeight = "CENTER";
        window.transform.doesDraw = false;
        pages.add(new Page(id, window));
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

    @Override
    protected void init() {
        super.init();

        view = new CGuiView(this, root);
        root.transform.width = ROOT_WIDTH;
        root.transform.height = ROOT_HEIGHT;
        root.transform.x = ROOT_X;
        root.transform.doesDraw = true;

        pages.clear();
        info.children().clear();
        infoY = 10;
        build(this);

        // Le panneau d'infos, a sept unites du bord droit de la racine, comme chez l'original.
        info.transform.x = ROOT_WIDTH + 7;
        info.transform.y = 5;
        info.transform.width = 100;
        info.transform.height = Math.max(50, infoY + 8);
        info.texture = new CWidget.DrawTexture();
        info.texture.red = 0;
        info.texture.green = 0;
        info.texture.blue = 0;
        info.texture.alpha = 128;

        // La racine porte les onglets puis les pages, dans cet ordre : l'original ajoutait le
        // bouton AVANT sa fenetre, donc la fenetre passe au-dessus du bouton.
        root.children().clear();
        for (int i = 0; i < pages.size(); i++) {
            root.add(tab(pages.get(i), i));
            root.add(pages.get(i).window());
        }
        root.add(info);

        show(0);
    }

    /** L'onglet d'une page : une copie du bouton de l'original, avec l'icone de son identifiant. */
    private CWidget tab(Page page, int index) {
        CWidget button = CGuiDocument.read("rework/pageselect").copy();
        button.transform.width = 24;
        button.transform.height = 24;
        button.transform.alignWidth = "LEFT";
        button.transform.alignHeight = "TOP";
        button.transform.scale = TAB_SCALE;
        button.transform.x = TAB_X;
        button.transform.y = index * TAB_STEP;
        if (button.texture != null) {
            button.texture.texture = "academy:textures/guis/icons/icon_" + page.id() + ".png";
        }
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

        // L'ancre de la racine : le coin des slots, plus le demi-pixel du calcul de l'original.
        view.origin(leftPos - 0.5, topPos + 0.5);

        // Ce que la machine a a dire ce tick-la, puis le dessin de tout l'arbre.
        view.place();
        frame(graphics, mouseX, mouseY);
        view.draw(graphics, mouseX, mouseY);
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

    /** Le gris des lignes d'infos, pour les ecrans qui s'en servent. */
    protected static int infoText() {
        return INFO_TEXT;
    }

    /** Le fond du panneau : noir a moitie, comme le {@code BlendQuad} de l'original. */
    protected static int infoFill() {
        return INFO_FILL;
    }
}
