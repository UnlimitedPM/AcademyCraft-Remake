package cn.academy.terminal.client;

import cn.academy.AcademyCraft;
import cn.academy.client.gui.AcademyFonts;
import cn.academy.terminal.about.AboutDocument;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * L'ecran de l'application « A propos » : la page de l'original.
 *
 * <p>Portage de {@code AppAbout.AboutUI}, sur son document {@code guis/about.xml} : une page de
 * <b>742 sur 923</b> que le document dessine a <b>0,25</b> — donc 185 sur 231 a l'ecran, centree.
 * Le port dessinait un panneau de son invention ; tout ce qui suit vient du document, en unites
 * de page :
 *
 * <ul>
 *   <li>le fond {@code about/bg.png}, qui porte deja le titre, le logo et la devise ;</li>
 *   <li>la <b>zone</b> de 630 sur 605 a (53 ; 266), et ses deux onglets de 315 sur 58 cote a
 *       cote, chacun avec sa lueur {@code button_glow.png} de 332 sur 80 centree sur lui ;</li>
 *   <li>la <b>fenetre du texte</b> de 620 sur 540 sous les onglets, et sa <b>glissiere</b> de 10
 *       sur 70 qui glisse de 472 unites vers le bas.</li>
 * </ul>
 *
 * <p>Les onglets se comportent comme chez lui : celui qui est ouvert prend un fond blanc a 50 %,
 * sa lueur et un texte sombre ; l'autre reste a 20 % de blanc, sans lueur, en texte blanc.
 *
 * <h2>Les textes</h2>
 *
 * <p>Ils viennent de {@link AboutDocument}, qui lit {@code config/about.json} — le portage du
 * {@code about.conf} HOCON de l'original, le port n'ayant pas de bibliotheque HOCON. La mise en
 * page est celle d'{@code initTexts()} : l'en-tete centre, chaque role a droite a trente unites
 * du milieu avec ses noms a gauche, les donateurs en trois colonnes, puis le remerciement.
 *
 * <p>L'onglet des dons n'a pas de barre de defilement chez l'original — son texte tient dans la
 * fenetre — et ses liens ouvrent le navigateur du bureau : c'est le seul geste du jeu qui en
 * sorte, et il est explicite (un clic sur une ligne bleue).
 */
@OnlyIn(Dist.CLIENT)
public class AboutScreen extends Screen {

    // ------------------------------------------------------------------
    // Le document `guis/about.xml`, en unites de page.
    // ------------------------------------------------------------------

    private static final double PAGE_W = 742.0;
    private static final double PAGE_H = 923.0;
    /** L'echelle que le document donne a sa page : 742 sur 923 devient 185 sur 231. */
    private static final double PAGE_SCALE = 0.25;

    private static final double AREA_X = 53.0;
    private static final double AREA_Y = 266.0;
    private static final double AREA_W = 630.0;

    /** Les deux onglets, et la lueur qui se pose derriere celui qui est ouvert. */
    private static final double TAB_W = 315.0;
    private static final double TAB_H = 58.0;
    private static final double GLOW_W = 332.0;
    private static final double GLOW_H = 80.0;
    private static final int TAB_CELL = 42;

    /** La fenetre du texte : sous les onglets. */
    private static final double VIEW_X = AREA_X;
    private static final double VIEW_Y = AREA_Y + 58.0;
    private static final double VIEW_W = 620.0;
    private static final double VIEW_H = 540.0;

    /** La glissiere : 10 sur 70, qui descend de 472 unites (`lower` 58, `upper` 530). */
    private static final double DRAG_W = 10.0;
    private static final double DRAG_H = 70.0;
    private static final double DRAG_X = AREA_X + AREA_W - DRAG_W;
    private static final double DRAG_Y = AREA_Y + 58.0;
    private static final double DRAG_TRAVEL = 472.0;

    /** Le corps du texte de l'original, en unites de page. */
    private static final double FONT = 30.0;
    /** Ecart entre le milieu de la fenetre et les colonnes « role / noms ». */
    private static final double ROLE_GAP = 30.0;
    /** Largeur d'une case de la liste des donateurs, et sa marge. */
    private static final double DONATOR_W = 150.0;
    private static final double DONATOR_MARGIN = 30.0;

    /** Les couleurs de l'original : texte, lien, lien survole, onglet ouvert. */
    private static final int TEXT_COLOUR = 0xFFFFFFFF;
    private static final int LINK_COLOUR = 0x5BB4FFFF;
    private static final int LINK_HOVER = 0x8ECBFFFF;
    private static final int TAB_TEXT_ON = 0xFF3D3F4B;
    private static final int TAB_TEXT_OFF = 0xFFFFFFFF;
    private static final int TAB_FILL_ON = 0x80FFFFFF;
    private static final int TAB_FILL_OFF = 0x33FFFFFF;

    /** La teinte de chaque lueur, telle que l'ecran de l'original la posait. */
    private static final int GLOW_CREDITS = 0x2984F1;
    private static final int GLOW_DONATE = 0xE79CFF;

    /** Le voile noir a 21/255 que le document pose sur la fenetre du texte. */
    private static final int VIEW_MASK = 0x15000000;

    /** Ce que la molette ajoute au defilement par cran : son 0,001 x 0,2 x 120. */
    private static final double WHEEL_STEP = 0.024;

    private static final ResourceLocation TEX_BG = texture("about/bg.png");
    private static final ResourceLocation TEX_GLOW = texture("about/button_glow.png");

    private enum Tab { CREDITS, DONATE }

    private static final int LEFT = 0;
    private static final int CENTER = 1;
    private static final int RIGHT = 2;

    /**
     * Une ligne du document, deja placee.
     *
     * @param x     bord gauche pour un alignement a gauche, milieu pour un centrage, bord droit
     *              pour un alignement a droite
     * @param cell  taille de la cellule, en unites de page
     * @param url   l'adresse a ouvrir, sur les liens de l'onglet des dons
     */
    private record Line(String text, double x, double y, int cell, int align, String url) {}

    private final AboutDocument document;

    private final List<Line> credits = new ArrayList<>();
    private final List<Line> donation = new ArrayList<>();

    private float creditsMaxY;

    private Tab tab = Tab.CREDITS;

    /** Position de la glissiere, de 0 a 1. */
    private double progress;

    /** Vrai tant que la glissiere est tenue. */
    private boolean dragging;

    /**
     * L'ecart entre le pointeur et le haut de la glissiere au moment ou on l'a prise : sans lui,
     * la glissiere saute sous la souris des qu'on la touche.
     */
    private double dragGrip;

    /** L'adresse sous la souris, ou null. */
    private String hoveredUrl;

    public AboutScreen() {
        super(Component.translatable("ac.app.about.name"));
        this.document = AboutDocument.load();
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "textures/guis/" + path);
    }

    private List<Line> lines() {
        return tab == Tab.CREDITS ? credits : donation;
    }

    // ------------------------------------------------------------------
    // Mise en page
    // ------------------------------------------------------------------

    @Override
    protected void init() {
        super.init();
        credits.clear();
        donation.clear();
        buildCredits(Component.translatable("ac.about.donators_info").getString());
        buildDonation();
    }

    /** L'onglet des credits : la disposition d'{@code initTexts()}, unite par unite. */
    private void buildCredits(String donatorsHint) {
        double centre = VIEW_W / 2;
        double y = 2 * FONT;

        for (String text : document.getHeader()) {
            credits.add(new Line(text, centre, y, (int) FONT, CENTER, null));
            y += FONT;
        }

        y += 2 * FONT;
        for (AboutDocument.Staff entry : document.getStaff()) {
            credits.add(new Line(entry.role(), centre - ROLE_GAP, y, (int) FONT, RIGHT, null));
            for (String name : entry.names()) {
                credits.add(new Line(name, centre + ROLE_GAP, y, (int) FONT, LEFT, null));
                y += FONT;
            }
            y += 0.5 * FONT;
        }

        y += FONT;
        credits.add(new Line("Donators", centre, y, (int) FONT, CENTER, null));
        y += 1.1 * FONT;

        // La cle de langue de l'original se decoupe sur ses « \n » litteraux ; on accepte aussi
        // de vrais retours a la ligne.
        for (String hint : donatorsHint.split("\\\\n|\\n")) {
            credits.add(new Line(hint, centre, y, (int) Math.round(FONT * 0.7), CENTER, null));
            y += FONT * 0.7;
        }

        y += 1.5 * FONT;

        List<String> donators = document.getDonators();
        for (int i = 0; i < donators.size(); i++) {
            double x = DONATOR_MARGIN + (i % AboutDocument.DONATORS_PER_ROW)
                    * (VIEW_W - 2 * DONATOR_MARGIN - DONATOR_W) / 2;
            credits.add(new Line(donators.get(i), x, y,
                    (int) Math.round(FONT * 0.8), LEFT, null));
            if (i % AboutDocument.DONATORS_PER_ROW == AboutDocument.DONATORS_PER_ROW - 1) {
                y += FONT * 0.8;
            }
        }

        y += 2 * FONT;
        credits.add(new Line("Thank you for playing!", centre, y, (int) FONT, CENTER, null));
        y += FONT;

        creditsMaxY = (float) (y + 30);
    }

    /**
     * L'onglet des dons.
     *
     * <p>La grammaire est celle du fichier de l'original : une ligne {@code ]...} s'aligne a
     * droite, et une ligne {@code !!texte|url} est un lien ecrit plus gros (cellule 40 au lieu de
     * 30), avec dix unites de marge au-dessus et vingt en dessous.
     */
    private void buildDonation() {
        String lang = minecraft == null ? "en_us" : minecraft.getLanguageManager().getSelected();
        double centre = VIEW_W / 2;
        double left = centre - 280;
        double right = centre + 280;
        double y = 100;

        for (String raw : document.donation(lang)) {
            String text = raw == null ? "" : raw;
            if (text.startsWith("!!")) {
                int bar = text.indexOf('|');
                if (bar > 2) {
                    y += 10;
                    donation.add(new Line(text.substring(2, bar), left, y, 40, LEFT,
                            text.substring(bar + 1)));
                    y += 50;
                    continue;
                }
            }
            if (text.startsWith("]")) {
                donation.add(new Line(text.substring(1), right, y, (int) FONT, RIGHT, null));
            } else {
                donation.add(new Line(text, left, y, (int) FONT, LEFT, null));
            }
            y += 30;
        }
    }

    // ------------------------------------------------------------------
    // Rendu
    // ------------------------------------------------------------------

    private double originX() {
        return (width - PAGE_W * PAGE_SCALE) / 2.0;
    }

    private double originY() {
        return (height - PAGE_H * PAGE_SCALE) / 2.0;
    }

    /** Defilement du moment, en unites de page. */
    private double offset() {
        if (tab != Tab.CREDITS) return 0;
        return progress * Math.max(0, creditsMaxY - VIEW_H + 50);
    }

    /** La glissiere, en coordonnees de page. */
    private double thumbY() {
        return DRAG_Y + progress * DRAG_TRAVEL;
    }

    private boolean scrollable() {
        return tab == Tab.CREDITS && creditsMaxY > VIEW_H;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        hoveredUrl = null;

        graphics.pose().pushPose();
        graphics.pose().translate((float) originX(), (float) originY(), 0f);
        graphics.pose().scale((float) PAGE_SCALE, (float) PAGE_SCALE, 1f);

        image(graphics, TEX_BG, 0, 0, PAGE_W, PAGE_H, 742, 923);
        drawTabs(graphics);

        // Le voile du document, puis le texte, coupe a la fenetre.
        fill(graphics, VIEW_X, VIEW_Y, VIEW_W, VIEW_H, VIEW_MASK);

        graphics.flush();
        graphics.enableScissor(
                (int) Math.floor(originX() + VIEW_X * PAGE_SCALE),
                (int) Math.floor(originY() + VIEW_Y * PAGE_SCALE),
                (int) Math.ceil(originX() + (VIEW_X + VIEW_W) * PAGE_SCALE),
                (int) Math.ceil(originY() + (VIEW_Y + VIEW_H) * PAGE_SCALE));
        drawLines(graphics, mouseX, mouseY);
        graphics.disableScissor();

        drawDragBar(graphics);

        graphics.pose().popPose();
        graphics.flush();
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    /** Les deux onglets, dans l'ordre du document. */
    private void drawTabs(GuiGraphics graphics) {
        tabButton(graphics, Tab.CREDITS, AREA_X, GLOW_CREDITS, "Credits");
        tabButton(graphics, Tab.DONATE, AREA_X + TAB_W, GLOW_DONATE, "Donate");
    }

    private void tabButton(GuiGraphics graphics, Tab which, double x, int glow, String label) {
        boolean open = tab == which;
        double y = AREA_Y;

        fill(graphics, x, y, TAB_W, TAB_H, open ? TAB_FILL_ON : TAB_FILL_OFF);

        if (open) {
            // La lueur est CENTREE sur le bouton : elle le depasse de 8,5 de chaque cote et de 11
            // en haut et en bas, ce que dit son alignement dans le document.
            imageTinted(graphics, TEX_GLOW, x + (TAB_W - GLOW_W) / 2, y + (TAB_H - GLOW_H) / 2,
                    GLOW_W, GLOW_H, 332, 80, glow);
        }

        // Le libelle est centre dans les 315 de large et les 58 de haut (`align=CENTER`,
        // `heightAlign=CENTER`), a la cellule de 42 du document.
        int textY = (int) Math.round(y + (TAB_H - TAB_CELL) / 2);
        int textX = (int) Math.round(x + TAB_W / 2.0
                - AcademyFonts.width(null, label, TAB_CELL) / 2.0);
        AcademyFonts.drawTop(graphics, font, label, textX, textY,
                open ? TAB_TEXT_ON : TAB_TEXT_OFF, TAB_CELL);
    }

    /** Les lignes, decalees par la glissiere, et le survol des liens. */
    private void drawLines(GuiGraphics graphics, int mouseX, int mouseY) {
        double pageX = (mouseX - originX()) / PAGE_SCALE;
        double pageY = (mouseY - originY()) / PAGE_SCALE;
        double shift = offset();

        for (Line line : lines()) {
            if (line.text().isEmpty()) continue;

            double y = line.y() - shift;
            if (y > VIEW_H || y + line.cell() < 0) continue;

            int textW = AcademyFonts.width(null, line.text(), line.cell());
            double textX = switch (line.align()) {
                case CENTER -> line.x() - textW / 2.0;
                case RIGHT -> line.x() - textW;
                default -> line.x();
            };

            boolean over = false;
            if (line.url() != null) {
                over = pageX >= textX && pageX <= textX + textW
                        && pageY >= VIEW_Y + y && pageY <= VIEW_Y + y + line.cell();
                if (over) {
                    hoveredUrl = line.url();
                }
            }

            int colour = line.url() == null ? TEXT_COLOUR : (over ? LINK_HOVER : LINK_COLOUR);
            AcademyFonts.drawTop(graphics, font, line.text(),
                    (int) Math.round(VIEW_X + textX), (int) Math.round(VIEW_Y + y), colour,
                    line.cell());
        }
    }

    /** La glissiere : un rectangle blanc a un peu plus de la moitie, comme son document. */
    private void drawDragBar(GuiGraphics graphics) {
        if (!scrollable()) return;
        fill(graphics, DRAG_X, thumbY(), DRAG_W, DRAG_H, 0x85FFFFFF);
    }

    // ------------------------------------------------------------------
    // La souris
    // ------------------------------------------------------------------

    private double pageX(double mouseX) {
        return (mouseX - originX()) / PAGE_SCALE;
    }

    private double pageY(double mouseY) {
        return (mouseY - originY()) / PAGE_SCALE;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        double x = pageX(mouseX);
        double y = pageY(mouseY);

        if (y >= AREA_Y && y <= AREA_Y + TAB_H) {
            if (x >= AREA_X && x <= AREA_X + TAB_W) {
                setTab(Tab.CREDITS);
                return true;
            }
            if (x >= AREA_X + TAB_W && x <= AREA_X + 2 * TAB_W) {
                setTab(Tab.DONATE);
                return true;
            }
        }

        // La glissiere se prend par ou on la touche.
        if (scrollable() && x >= DRAG_X && x <= DRAG_X + DRAG_W
                && y >= thumbY() - 4 && y <= thumbY() + DRAG_H + 4) {
            dragging = true;
            dragGrip = y - thumbY();
            return true;
        }

        // Un lien ouvre le navigateur du bureau : le seul geste du jeu qui en sorte.
        if (hoveredUrl != null) {
            openUrl(hoveredUrl);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void setTab(Tab which) {
        if (tab == which) return;
        tab = which;
        // L'original remet sa glissiere en haut a chaque changement d'onglet.
        progress = 0;
        dragging = false;
    }

    private static void openUrl(String url) {
        try {
            Util.getPlatform().openUri(url);
        } catch (Exception e) {
            // Un systeme sans navigateur ne doit pas faire tomber le jeu.
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX,
                                double dragY) {
        if (dragging) {
            double top = pageY(mouseY) - dragGrip - DRAG_Y;
            progress = Math.max(0, Math.min(1, top / DRAG_TRAVEL));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0 && tab == Tab.CREDITS) {
            progress = Math.max(0, Math.min(1, progress - delta * WHEEL_STEP));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    // ------------------------------------------------------------------
    // Le dessin
    // ------------------------------------------------------------------

    private static void blend() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /** Une image teintee : la lueur d'un onglet est blanche dans son PNG et prend sa couleur ici. */
    private static void imageTinted(GuiGraphics graphics, ResourceLocation texture, double x,
                                    double y, double width, double height, int texWidth,
                                    int texHeight, int rgb) {
        blend();
        graphics.setColor((rgb >> 16 & 0xFF) / 255f, (rgb >> 8 & 0xFF) / 255f,
                (rgb & 0xFF) / 255f, 1f);
        graphics.blit(texture, (int) Math.round(x), (int) Math.round(y),
                (int) Math.round(width), (int) Math.round(height),
                0f, 0f, texWidth, texHeight, texWidth, texHeight);
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    private static void fill(GuiGraphics graphics, double x, double y, double width,
                             double height, int colour) {
        blend();
        graphics.fill((int) Math.round(x), (int) Math.round(y),
                (int) Math.round(x + width), (int) Math.round(y + height), colour);
    }

    private static void image(GuiGraphics graphics, ResourceLocation texture, double x, double y,
                             double width, double height, int texWidth, int texHeight) {
        blend();
        graphics.setColor(1f, 1f, 1f, 1f);
        graphics.blit(texture, (int) Math.round(x), (int) Math.round(y),
                (int) Math.round(width), (int) Math.round(height),
                0f, 0f, texWidth, texHeight, texWidth, texHeight);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
