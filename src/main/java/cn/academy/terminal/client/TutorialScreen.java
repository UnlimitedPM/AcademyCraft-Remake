package cn.academy.terminal.client;

import cn.academy.ability.client.AbilityKeyBindings;
import cn.academy.client.gui.AcademyFonts;
import cn.academy.client.gui.cgui.CGuiScreen;
import cn.academy.client.gui.cgui.CWidget;
import cn.academy.terminal.tutorial.TutorialLibrary;
import cn.academy.terminal.tutorial.TutorialMarkdown;
import cn.academy.terminal.tutorial.TutorialText;
import cn.academy.terminal.tutorial.client.ClientTutorialData;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * L'ecran de « MisakaCloud », sur le VRAI document de l'original.
 *
 * <p>La mise en page est celle de {@code assets/academy/guis/tutorial.xml}, copie tel quel :
 * une fenetre de 427 x 240, le panneau de la liste a gauche (85 x 220,5), et a droite la
 * colonne du texte (172 de large) avec, au-dessus, la fenetre d'apercu et, en dessous, celle
 * du titre et du resume. Tout est mis a l'echelle de la largeur comme chez l'original
 * ({@code width / 480}).
 *
 * <h2>Ce que le port n'a pas encore</h2>
 *
 * <p>L'APERCU 3D ({@code showWindow}) : l'original y dessinait un modele de machine, avec des
 * boutons pour tourner autour, et chaque tutoriel portait son propre groupe de vues. La
 * fenetre est donc laissee fermee — sans quoi elle montrerait un cadre vide. Le reste de
 * l'ecran (liste, titre, resume, texte) est celui de l'original.
 *
 * <h2>Le texte</h2>
 *
 * <p>Le contenu est du markdown : {@link TutorialMarkdown} le coupe en lignes et en images,
 * et c'est ici qu'il est mis en page, avec la police du mod et le defilement de la fenetre.
 * Les captures ({@code ![titre](academy:textures/tutorial/...)}) etaient l'un des deux
 * reproches du joueur — « on ne voit pas les images » : elles se dessinent a la largeur de
 * la colonne, proportions gardees par {@link TutorialImages}.
 */
@OnlyIn(Dist.CLIENT)
public class TutorialScreen extends CGuiScreen {

    /** La largeur de reference du document : l'original mettait l'ecran a cette echelle. */
    private static final double REF_WIDTH = 480.0;

    /** La largeur du texte dans la colonne (l'original : {@code widthLimit_$eq(150)}). */
    private static final int CONTENT_WIDTH = 150;

    /** La cellule du texte : huit, comme le {@code FontOption(8)} de l'original. */
    private static final int CONTENT_CELL = 8;

    /** La teinte de la ligne choisie dans la liste (l'original : blanc a 30 %). */
    private static final int[] SELECTED = { 255, 255, 255, 76 };
    private static final int[] UNSELECTED = { 255, 255, 255, 0 };

    private final List<TutorialText> texts = new ArrayList<>();
    private final List<Boolean> opened = new ArrayList<>();
    private final List<CWidget> rows = new ArrayList<>();

    /** Le tutoriel choisi, ou {@code -1} tant qu'on n'a rien clique. */
    private int selected = -1;

    /** Defilement du texte, en pixels. */
    private int scroll;
    private int contentHeight;

    private List<TutorialMarkdown.Block> blocks = List.of();

    /** Les apercus du tutoriel choisi, et celui qui est montre. */
    private List<TutorialPreviews.View> previews = List.of();
    private int previewIndex;

    public TutorialScreen() {
        super(Component.translatable("ac.app.tutorial.name"), "tutorial");
    }

    @Override
    protected void layout() {
        CWidget frame = find("frame");
        if (frame != null) frame.transform.scale = width / REF_WIDTH;
        super.layout();
    }

    @Override
    protected void init() {
        super.init();

        // Tout se lit une fois : les treize textes et ce que le serveur a ouvert. Les relire a
        // chaque rendu ferait treize lectures de fichier par image.
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        String name = Minecraft.getInstance().getUser().getName();
        texts.clear();
        opened.clear();
        for (TutorialLibrary.Entry entry : TutorialLibrary.entries()) {
            TutorialText raw = TutorialLibrary.load(entry.id(), language);
            texts.add(new TutorialText(raw.title(), raw.brief(), raw.contentFor(name)));
            opened.add(entry.alwaysOpen() || ClientTutorialData.get().isUnlocked(entry.id()));
        }

        // L'introduction en logo de l'original (logo0..3) : ce sont les images blanches floues
        // qui entourent la liste, et c'est leur degrade que le joueur voyait dans le vrai mod.
        // Elles restent affichees tant qu'aucun tutoriel n'est choisi, puis s'effacent — c'est
        // exactement ce que faisait l'original au premier clic. L'apercu 3D, lui, reste ferme.
        hide("showWindow");

        // Le cote droit attend un clic : c'est l'etat de repos de l'original.
        hide("rightWindow");
        hide("centerPart");

        buildList();
        orderScrollBars();

        CWidget left = find("showWindow");
        if (left != null) {
            onClick(left.child("btn_left"), () -> cyclePreview(-1));
            onClick(left.child("btn_right"), () -> cyclePreview(1));
        }
    }

    /** Les fleches de l'apercu : elles tournent entre les vues du tutoriel. */
    private void cyclePreview(int delta) {
        if (previews.isEmpty()) return;
        previewIndex = Math.floorMod(previewIndex + delta, previews.size());
    }

    /**
     * La barre de defilement se dessine DANS CET ORDRE : la glissiere, puis le curseur.
     *
     * <p>Le document les pose dans l'autre sens, et le moteur dessine dans l'ordre du fichier :
     * la glissiere passait donc par-dessus le curseur, qui n'avait plus l'air d'exister. C'est
     * le reproche du joueur — « il manque la barre de scroll qui n'est juste pas visible ».
     */
    private void orderScrollBars() {
        CWidget centerPart = find("centerPart");
        if (centerPart == null) return;

        CWidget track = centerPart.child("scroll_1");
        CWidget thumb = centerPart.child("scroll_2");
        if (track == null || thumb == null) return;

        centerPart.remove(track);
        centerPart.remove(thumb);
        centerPart.add(track);
        centerPart.add(thumb);
    }

    private void hide(String name) {
        CWidget widget = find(name);
        if (widget != null) widget.transform.doesDraw = false;
    }

    /** La liste : une ligne par tutoriel, ouverte en clair et fermee en gris. */
    private void buildList() {
        CWidget list = find("list");
        if (list == null) return;

        for (CWidget child : new ArrayList<>(list.children())) list.remove(child);
        rows.clear();
        list.list = new CWidget.ElementList();

        for (int i = 0; i < texts.size(); i++) {
            int index = i;

            CWidget row = new CWidget("row_" + i);
            row.transform.width = 72;
            row.transform.height = 12;
            row.tint = new CWidget.Tint();
            row.tint.idle = UNSELECTED.clone();
            row.tint.hover = SELECTED.clone();

            CWidget label = new CWidget("label");
            label.transform.width = 69;
            label.transform.height = 12;
            label.transform.x = 3;
            label.text = new CWidget.TextBox();
            label.text.content = ellipsize(texts.get(i).title());
            label.text.fontSize = 10;
            label.text.align = "LEFT";
            label.text.heightAlign = "CENTER";
            int grey = opened.get(i) ? 255 : 153;
            label.text.red = grey;
            label.text.green = grey;
            label.text.blue = grey;

            row.add(label);
            onClick(row, () -> select(index));
            rows.add(row);
            list.add(row);
        }
    }

    /** Un titre trop long est coupe : la colonne ne doit pas deborder sur le texte. */
    private String ellipsize(String value) {
        String out = value;
        while (AcademyFonts.width(font, out) > 65 && out.length() > 4) {
            out = out.substring(0, out.length() - 2);
        }
        return out.equals(value) ? out : out + ".";
    }

    private void select(int index) {
        // Premier choix : les logos s'effacent, comme chez l'original.
        if (selected < 0) {
            hide("logo0");
            hide("logo1");
            hide("logo2");
            hide("logo3");
        }

        selected = index;
        scroll = 0;
        blocks = TutorialMarkdown.parse(texts.get(index).content(), TutorialScreen::keyName);
        contentHeight = measure();

        previews = TutorialPreviews.views(TutorialLibrary.entries().get(index).id());
        previewIndex = 0;
        CWidget showWindow = find("showWindow");
        if (showWindow != null) showWindow.transform.doesDraw = !previews.isEmpty();

        CWidget rightWindow = find("rightWindow");
        CWidget centerPart = find("centerPart");
        if (rightWindow != null) rightWindow.transform.doesDraw = true;
        // Comme l'original : le texte ne s'ouvre que pour un tutoriel APPRIS. Un tutoriel
        // ferme reste lisible dans la liste, en gris, et son titre s'affiche a droite.
        if (centerPart != null) centerPart.transform.doesDraw = opened.get(index);

        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).tint.idle = (i == index ? SELECTED : UNSELECTED).clone();
        }
    }

    /** Le nom d'une touche tel que le jeu l'affiche, ou {@code null} si l'identifiant est inconnu. */
    private static String keyName(String id) {
        KeyMapping key = switch (id) {
            case "ability_activation" -> AbilityKeyBindings.TOGGLE_ABILITY;
            case "edit_preset" -> AbilityKeyBindings.PRESET_EDIT;
            case "switch_preset" -> AbilityKeyBindings.PRESET_NEXT;
            case "open_data_terminal" -> TerminalKeyBindings.OPEN_TERMINAL;
            default -> null;
        };
        return key == null ? null : key.getTranslatedKeyMessage().getString();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (selected < 0) return;
        drawRightWindow(graphics);
        if (opened.get(selected)) drawContent(graphics);
        if (!previews.isEmpty()) {
            drawPreview(graphics);
            drawTags(graphics);
        }
    }

    /** L'apercu courant : un bloc ou un objet en volume, une image, ou une grille de recette. */
    private void drawPreview(GuiGraphics graphics) {
        CWidget window = find("showWindow");
        CWidget area = window == null ? null : window.child("area");
        if (area == null || previews.isEmpty()) return;

        double cx = area.absX + area.absW / 2;
        double cy = area.absY + area.absH / 2;
        double size = Math.min(area.absW, area.absH);
        TutorialPreviews.View view = previews.get(previewIndex);

        switch (view.kind()) {
            case BLOCK, ITEM -> centreItem(graphics, TutorialPreviews.stack(view.id()), cx, cy, size);
            case ICON -> {
                ResourceLocation icon = ResourceLocation.tryParse(view.id());
                int[] iconSize = TutorialImages.size(view.id());
                if (icon != null && iconSize != null) {
                    int w = (int) Math.round(size * 0.7);
                    int h = Math.max(1, (int) Math.round(iconSize[1] * (double) w / iconSize[0]));
                    graphics.blit(icon, (int) Math.round(cx - w / 2.0),
                            (int) Math.round(cy - h / 2.0), w, h,
                            0f, 0f, iconSize[0], iconSize[1], iconSize[0], iconSize[1]);
                }
            }
            case RECIPE -> drawRecipe(graphics, view.id(), cx, cy, size);
        }
    }

    /** Une pile bien centree, mise a l'echelle de la case. */
    private static void centreItem(GuiGraphics graphics, ItemStack stack, double cx, double cy,
                                   double size) {
        if (stack.isEmpty()) return;

        graphics.pose().pushPose();
        graphics.pose().translate((float) cx, (float) cy, 0f);
        float scale = (float) (size / 16.0 * 0.8);
        graphics.pose().scale(scale, scale, 1f);
        graphics.renderItem(stack, -8, -8);
        graphics.pose().popPose();
    }

    /** Une grille de trois sur trois, la fleche, et le resultat. */
    private static void drawRecipe(GuiGraphics graphics, String id, double cx, double cy,
                                   double size) {
        List<ItemStack> ingredients = TutorialPreviews.ingredients(id);
        ItemStack result = TutorialPreviews.stack(id);

        double cell = Math.min(size / 4.8, size / 3.0);
        double left = cx - (4.8 * cell) / 2;
        double top = cy - (3 * cell) / 2;

        for (int i = 0; i < 9; i++) {
            double x = left + (i % 3) * cell;
            double y = top + (i / 3) * cell;
            slot(graphics, x, y, cell);
            if (i < ingredients.size()) itemIn(graphics, ingredients.get(i), x, y, cell);
        }

        // La fleche, entre la grille et le resultat.
        int arrowY = (int) Math.round(cy);
        graphics.fill((int) Math.round(left + 3.1 * cell), arrowY - 1,
                (int) Math.round(left + 3.5 * cell), arrowY + 1, 0xFFC8C8C8);

        double resultX = left + 3.6 * cell;
        slot(graphics, resultX, top + cell, cell);
        itemIn(graphics, result, resultX, top + cell, cell);
    }

    /** Le fond d'une case de recette. */
    private static void slot(GuiGraphics graphics, double x, double y, double cell) {
        int x0 = (int) Math.round(x);
        int y0 = (int) Math.round(y);
        int x1 = (int) Math.round(x + cell);
        int y1 = (int) Math.round(y + cell);
        graphics.fill(x0, y0, x1, y1, 0x50000000);
        graphics.fill(x0, y0, x1, y0 + 1, 0x60C8C8C8);
        graphics.fill(x0, y1 - 1, x1, y1, 0x60C8C8C8);
        graphics.fill(x0, y0, x0 + 1, y1, 0x60C8C8C8);
        graphics.fill(x1 - 1, y0, x1, y1, 0x60C8C8C8);
    }

    /** Une pile dans une case de recette. */
    private static void itemIn(GuiGraphics graphics, ItemStack stack, double x, double y,
                               double cell) {
        if (stack.isEmpty()) return;

        graphics.pose().pushPose();
        graphics.pose().translate((float) x, (float) y, 0f);
        float scale = (float) (cell * 0.8 / 16.0);
        graphics.pose().scale(scale, scale, 1f);
        graphics.renderItem(stack, 1, 1);
        graphics.pose().popPose();
    }

    /** La rangee d'etiquettes, une par apercu : CRAFT pour une recette, VIEW pour le reste. */
    private void drawTags(GuiGraphics graphics) {
        CWidget window = find("showWindow");
        CWidget area = window == null ? null : window.child("tag_area");
        if (area == null) return;

        double size = area.absH;
        double step = size - area.absScale;

        for (int i = 0; i < previews.size(); i++) {
            ResourceLocation icon = TutorialPreviews.tagIcon(previews.get(i));
            int x = (int) Math.round(area.absX + i * step);
            int y = (int) Math.round(area.absY);
            int side = Math.max(1, (int) Math.round(size));

            int tint = i == previewIndex ? 0xFFFFFFFF : 0xB2FFFFFF;
            graphics.setColor(((tint >> 16) & 0xFF) / 255f, ((tint >> 8) & 0xFF) / 255f,
                    (tint & 0xFF) / 255f, 1f);
            graphics.blit(icon, x, y, side, side, 0f, 0f, 48, 48, 48, 48);
            graphics.setColor(1f, 1f, 1f, 1f);
        }
    }

    /** L'etiquette sous ce point, ou {@code -1}. */
    private int tagAt(double mouseX, double mouseY) {
        CWidget window = find("showWindow");
        CWidget area = window == null ? null : window.child("tag_area");
        if (area == null) return -1;

        double size = area.absH;
        double step = size - area.absScale;
        for (int i = 0; i < previews.size(); i++) {
            double x = area.absX + i * step;
            if (mouseX >= x && mouseX <= x + size && mouseY >= area.absY
                    && mouseY <= area.absY + size) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && selected >= 0 && !previews.isEmpty()) {
            int tag = tagAt(mouseX, mouseY);
            if (tag >= 0) {
                previewIndex = tag;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Les deux traits lumineux de l'introduction, tant que les logos sont la.
     *
     * <p>L'original les dessinait dans un ecouteur de {@code logo1} : deux segments blancs
     * flanques de leur halo, a 500 unites du centre (dont 300 deja pleines), quinze unites
     * au-dessus du milieu. C'est le « blanc en transparence » que le joueur regrettait.
     */
    @Override
    protected void frame(GuiGraphics graphics, double mouseX, double mouseY) {
        CWidget logo1 = find("logo1");
        if (logo1 == null || !logo1.transform.doesDraw) return;

        double scale = logo1.absScale;
        double cx = logo1.absX + logo1.absW / 2;
        double cy = logo1.absY + logo1.absH / 2 + 15 * scale;
        double ln = 500 * scale;
        double ln2 = 300 * scale;
        double ht = Math.max(1.0, 5 * scale);

        lineGlow(graphics, cx + (ln - ln2), cx + ln, cy, ht);
        lineGlow(graphics, cx - ln, cx - (ln - ln2), cy, ht);

        moveScrollThumb();
    }

    /** Un trait blanc avec son halo : le {@code lineglow} de l'original. */
    private static void lineGlow(GuiGraphics graphics, double x0, double x1, double y,
                                 double height) {
        ResourceLocation glow = ResourceLocation.tryParse(
                "academy:textures/effects/glow_line.png");
        int left = (int) Math.round(Math.min(x0, x1));
        int right = (int) Math.round(Math.max(x0, x1));

        if (glow != null && right > left) {
            graphics.blit(glow, left, (int) Math.round(y - height * 2), right - left,
                    Math.max(1, (int) Math.round(height * 5)), 0f, 0f, 32, 32, 32, 32);
        }
        graphics.fill(left, (int) Math.round(y), right,
                (int) Math.round(y + height), 0xFFFFFFFF);
    }

    /** Le curseur de la barre suit le defilement, dans la glissiere du document. */
    private void moveScrollThumb() {
        CWidget centerPart = find("centerPart");
        if (centerPart == null) return;
        CWidget track = centerPart.child("scroll_1");
        CWidget thumb = centerPart.child("scroll_2");
        if (track == null || thumb == null) return;

        int max = Math.max(0, contentHeight - contentViewHeight());
        double span = Math.max(0.0, track.transform.height - thumb.transform.height - 4);
        thumb.transform.y = 2 + (max <= 0 ? 0 : (double) scroll / max * span);
    }

    /** La hauteur du contenu mis en page, pour la barre de defilement. */
    private int measure() {
        int height = 0;

        for (TutorialMarkdown.Block block : blocks) {
            if (block.isImage()) {
                int[] size = TutorialImages.size(block.image());
                height += (size == null ? 0
                        : Math.max(1, (int) Math.round(size[1] * (double) CONTENT_WIDTH / size[0])))
                        + 4;
                continue;
            }
            int cell = cellOf(block);
            height += (block.text().isEmpty() ? 1
                    : AcademyFonts.split(font, block.text(), CONTENT_WIDTH, cell).size())
                    * AcademyFonts.lineHeight(cell);
        }
        return height;
    }

    /** La cellule d'un morceau : 12 pour un titre, 10 pour un sous-titre, 8 pour le corps. */
    private static int cellOf(TutorialMarkdown.Block block) {
        return switch (block.heading()) {
            case 1 -> 12;
            case 2 -> 10;
            default -> CONTENT_CELL;
        };
    }

    /** Un texte du contenu ; un titre se dessine en GRAS, comme l'original (double passe). */
    private void drawLine(GuiGraphics graphics, String line, int x, int y, int colour, int cell,
                          boolean bold) {
        AcademyFonts.drawTop(graphics, font, line, x, y, colour, cell);
        if (bold) AcademyFonts.drawTop(graphics, font, line, x + 1, y, colour, cell);
    }

    /** Le titre et le resume, dans la fenetre du bas : 3 unites du bord, comme l'original. */
    private void drawRightWindow(GuiGraphics graphics) {
        CWidget window = find("rightWindow");
        CWidget area = window == null ? null : window.child("text");
        if (area == null) return;

        int left = (int) Math.round(area.absX) + 3;
        int top = (int) Math.round(area.absY) + 3;

        AcademyFonts.drawTop(graphics, font, texts.get(selected).title(), left, top,
                0xFFFFFFFF, AcademyFonts.GUI_SIZE);
        top += AcademyFonts.lineHeight(AcademyFonts.GUI_SIZE) + 2;

        String brief = TutorialMarkdown.inline(texts.get(selected).brief(), TutorialScreen::keyName);
        if (brief.isEmpty()) return;
        for (String line : AcademyFonts.split(font, brief, (int) area.absW - 8, CONTENT_CELL)) {
            if (!line.isEmpty()) {
                AcademyFonts.drawTop(graphics, font, line, left, top, 0xFFC8C8C8, CONTENT_CELL);
            }
            top += AcademyFonts.lineHeight(CONTENT_CELL);
        }
    }

    /** Le contenu, dans la colonne de gauche de la partie droite, avec son defilement. */
    private void drawContent(GuiGraphics graphics) {
        CWidget centerPart = find("centerPart");
        CWidget area = centerPart == null ? null : centerPart.child("text");
        if (area == null) return;

        int left = (int) Math.round(area.absX) + 4;
        int top = (int) Math.round(area.absY) + 3;
        int bottom = (int) Math.round(area.absY + area.absH) - 2;

        graphics.enableScissor((int) Math.round(area.absX), (int) Math.round(area.absY),
                (int) Math.round(area.absX + area.absW), (int) Math.round(area.absY + area.absH));

        int y = top - scroll;

        for (TutorialMarkdown.Block block : blocks) {
            if (block.isImage()) {
                y = drawImage(graphics, block.image(), left, y, top, bottom) + 4;
                continue;
            }

            int blockCell = cellOf(block);
            List<String> lines = block.text().isEmpty()
                    ? List.of("")
                    : AcademyFonts.split(font, block.text(), CONTENT_WIDTH, blockCell);
            for (String line : lines) {
                if (!line.isEmpty() && y + blockCell >= top && y <= bottom) {
                    drawLine(graphics, line, left, y, 0xFFE0E0E0, blockCell, block.heading() > 0);
                }
                y += AcademyFonts.lineHeight(blockCell);
            }
        }

        graphics.disableScissor();
    }

    /**
     * Une capture du tutoriel, a la largeur de la colonne et proportions gardees.
     *
     * @return le bas de l'image, ou {@code y} quand le fichier est absent
     */
    private int drawImage(GuiGraphics graphics, String path, int x, int y, int top, int bottom) {
        int[] size = TutorialImages.size(path);
        ResourceLocation texture = path == null ? null : ResourceLocation.tryParse(path);
        if (size == null || texture == null) return y;

        int w = CONTENT_WIDTH;
        int h = Math.max(1, (int) Math.round(size[1] * (double) w / size[0]));
        if (y + h >= top && y <= bottom) {
            graphics.blit(texture, x, y, w, h, 0f, 0f, size[0], size[1], size[0], size[1]);
        }
        return y + h;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (selected >= 0 && delta != 0) {
            int viewHeight = contentViewHeight();
            int max = Math.max(0, contentHeight - viewHeight);
            scroll = Math.max(0, Math.min(max,
                    scroll + (int) (-delta * AcademyFonts.lineHeight(CONTENT_CELL) * 2)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private int contentViewHeight() {
        CWidget centerPart = find("centerPart");
        CWidget area = centerPart == null ? null : centerPart.child("text");
        return area == null ? 0 : Math.max(0, (int) Math.round(area.absH) - 5);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
