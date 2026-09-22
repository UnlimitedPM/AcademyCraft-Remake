package cn.academy.terminal.client;

import cn.academy.terminal.tutorial.TutorialLibrary;
import cn.academy.terminal.tutorial.TutorialText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * L'ecran de « MisakaCloud » : la liste des tutoriels a gauche, leur texte a droite.
 *
 * <p>C'est la disposition de l'original — le contenu au milieu, la liste a gauche, et
 * l'apercu des recettes a droite. L'apercu n'est pas porte : il demanderait un affichage
 * de recettes complet, pour des figures qui ne sont pas l'essentiel.
 *
 * <h2>Ce qui ouvre un tutoriel</h2>
 *
 * L'original retenait ce que le joueur avait <b>obtenu</b> (fabrique, ramasse ou cuit)
 * et le conservait d'une session a l'autre ; le port regarde ce qu'il <b>porte</b>, au
 * moment ou l'ecran s'ouvre. La difference se voit si l'objet a ete range dans un
 * coffre : le tutoriel se referme. C'est une simplification assumee, en attendant le
 * suivi persistant — et elle est permissive, un objet montre ouvrant ce qu'il aurait
 * fallu fabriquer.
 *
 * <p>Un tutoriel non ouvert reste <b>visible</b>, en gris, et son texte s'affiche quand
 * meme : c'est ce que faisait l'original, ou la liste entiere restait a l'ecran.
 */
@OnlyIn(Dist.CLIENT)
public class TutorialScreen extends Screen {

    private static final int MARGIN = 16;
    private static final int HEADER = 18;
    private static final int ROW = 12;
    private static final int LIST_WIDTH = 96;

    private static final int PANEL = 0xF0181824;
    private static final int PANEL_EDGE = 0xFF6FA8DC;
    private static final int TEXT = 0xFFE0E0E0;
    private static final int DIM = 0xFF909090;
    private static final int LOCKED = 0xFF606060;
    private static final int SELECTED = 0xFF2A3A50;

    private final List<TutorialText> texts = new ArrayList<>();
    private final List<Boolean> opened = new ArrayList<>();

    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;

    /** Le tutoriel choisi, par son rang dans la liste. */
    private int selected;

    /** Defilement du texte, en pixels. */
    private int scroll;
    private int contentHeight;

    /** Le contenu courant, une fois mis en page. */
    private List<FormattedCharSequence> wrapped = List.of();

    public TutorialScreen() {
        super(Component.translatable("ac.app.tutorial.name"));
    }

    @Override
    protected void init() {
        panelWidth = width - 2 * MARGIN;
        panelHeight = height - 2 * MARGIN;
        leftPos = MARGIN;
        topPos = MARGIN;

        // Tout se lit une fois : les textes des treize tutoriels et ce qui est ouvert.
        // Les relire a chaque rendu ferait treize lectures de fichier par image, et
        // autant de parcours d'inventaire.
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        String name = Minecraft.getInstance().getUser().getName();
        texts.clear();
        opened.clear();
        for (TutorialLibrary.Entry entry : TutorialLibrary.entries()) {
            TutorialText raw = TutorialLibrary.load(entry.id(), language);
            texts.add(new TutorialText(raw.title(), raw.brief(), raw.contentFor(name)));
            opened.add(isOpen(entry));
        }

        layout();
    }

    private TutorialLibrary.Entry entry() {
        return TutorialLibrary.entries().get(selected);
    }

    private TutorialText text() {
        return texts.get(selected);
    }

    /** Vrai si le joueur porte de quoi ouvrir ce tutoriel. */
    private static boolean isOpen(TutorialLibrary.Entry entry) {
        if (entry.alwaysOpen()) return true;

        Player player = Minecraft.getInstance().player;
        if (player == null) return false;

        for (String id : entry.requiredItems()) {
            ResourceLocation key = ResourceLocation.tryParse(id);
            if (key == null) continue;
            Item item = BuiltInRegistries.ITEM.get(key);
            if (item != null && player.getInventory().contains(new ItemStack(item))) return true;
        }
        return false;
    }

    private int listRight() {
        return leftPos + LIST_WIDTH;
    }

    private int textLeft() {
        return listRight() + 6;
    }

    private int viewTop() {
        return topPos + HEADER;
    }

    private int viewBottom() {
        return topPos + panelHeight - 4;
    }

    private int viewHeight() {
        return viewBottom() - viewTop();
    }

    private int viewWidth() {
        return leftPos + panelWidth - textLeft() - 6;
    }

    /** Met le texte courant en page, et ramene le defilement dans ses bornes. */
    private void layout() {
        TutorialText text = text();
        wrapped = font.split(Component.literal(text.content()), viewWidth());
        int brief = text.brief().isEmpty() ? 0
                : font.split(Component.literal(text.brief()), viewWidth()).size() * font.lineHeight
                        + font.lineHeight;
        contentHeight = font.lineHeight + 2 + brief + wrapped.size() * font.lineHeight;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - viewHeight())));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        graphics.fill(leftPos - 1, topPos - 1, leftPos + panelWidth + 1, topPos + panelHeight + 1,
                PANEL_EDGE);
        graphics.fill(leftPos, topPos, leftPos + panelWidth, topPos + panelHeight, PANEL);
        graphics.fill(listRight(), viewTop(), listRight() + 1, viewBottom(), PANEL_EDGE);
        graphics.drawString(font, title, leftPos + 8, topPos + 5, TEXT, false);

        drawList(graphics, mouseX, mouseY);
        drawText(graphics);
        drawScrollBar(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** La liste des tutoriels : l'ouvert en clair, les autres en gris. */
    private void drawList(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int i = 0; i < texts.size(); i++) {
            int y = viewTop() + i * ROW;
            boolean hovered = mouseY >= y && mouseY < y + ROW
                    && mouseX >= leftPos + 2 && mouseX < listRight() - 2;

            if (i == selected) {
                graphics.fill(leftPos + 2, y, listRight() - 2, y + ROW, SELECTED);
            }

            int colour = opened.get(i) ? (hovered || i == selected ? TEXT : DIM) : LOCKED;
            graphics.drawString(font, ellipsize(texts.get(i).title()), leftPos + 6, y + 2, colour,
                    false);
        }
    }

    /** Un titre trop long est coupe : la colonne ne doit pas deborder sur le texte. */
    private String ellipsize(String value) {
        String out = value;
        while (font.width(out) > LIST_WIDTH - 12 && out.length() > 4) {
            out = out.substring(0, out.length() - 2);
        }
        return out.equals(value) ? out : out + ".";
    }

    private void drawText(GuiGraphics graphics) {
        TutorialText text = text();
        int x = textLeft();
        int y = viewTop() - scroll;

        if (text.isEmpty()) {
            graphics.drawString(font, Component.translatable("ac.tutorial.missing"), x, viewTop(),
                    DIM, false);
            return;
        }

        graphics.drawString(font, text.title(), x, y, TEXT, false);
        y += font.lineHeight + 2;

        if (!text.brief().isEmpty()) {
            for (FormattedCharSequence line : font.split(Component.literal(text.brief()), viewWidth())) {
                graphics.drawString(font, line, x, y, DIM, false);
                y += font.lineHeight;
            }
            y += font.lineHeight;
        }

        for (FormattedCharSequence line : wrapped) {
            if (y + font.lineHeight >= viewTop() && y <= viewBottom()) {
                graphics.drawString(font, line, x, y, TEXT, false);
            }
            y += font.lineHeight;
        }
    }

    private void drawScrollBar(GuiGraphics graphics) {
        int max = Math.max(0, contentHeight - viewHeight());
        if (max <= 0) return;

        int barX = leftPos + panelWidth - 4;
        graphics.fill(barX, viewTop(), barX + 2, viewBottom(), 0xFF373737);
        int barHeight = Math.max(8, viewHeight() * viewHeight() / contentHeight);
        int barTop = viewTop() + (viewHeight() - barHeight) * scroll / max;
        graphics.fill(barX, barTop, barX + 2, barTop + barHeight, PANEL_EDGE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < texts.size(); i++) {
            int y = viewTop() + i * ROW;
            if (mouseX >= leftPos + 2 && mouseX < listRight() - 2
                    && mouseY >= y && mouseY < y + ROW) {
                selected = i;
                scroll = 0;
                layout();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0) {
            int max = Math.max(0, contentHeight - viewHeight());
            scroll = Math.max(0, Math.min(max, scroll + (int) (-delta * font.lineHeight * 2)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
