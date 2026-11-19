package cn.academy.client.gui;

import cn.academy.client.hud.HudConfig;
import cn.academy.client.hud.HudElement;
import cn.academy.client.hud.HudLayout;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * L'ecran de reglage du HUD : ou se posent les elements.
 *
 * <p>Portage de {@code CustomizeUI}, dont la disposition est reprise telle quelle — elle est
 * dans {@link CustomizeUiLayout} : un panneau <b>Elements</b> a sa place exacte, qui passe
 * donc sous l'apercu des notifications ; les <b>apercus</b> de chacun, dessines a leur place
 * reelle ; et, pour l'element choisi, les deux champs <b>X</b> et <b>Y</b> a droite de sa
 * ligne, jamais a une place fixe.
 *
 * <p>Les apercus ne sont pas des vignettes inventees : ce sont les morceaux de l'original,
 * avec ses nombres. La notification se compose — comme chez lui — de son fond, de son icone
 * et de deux lignes d'exemple ; le lecteur media n'a pas d'image mais du texte et une barre
 * de progression ; le rappel des touches et la barre de CP livrent leur image.
 *
 * <p>A l'ouverture, <b>rien n'est choisi</b> : c'est ce que faisait l'original, et cela evite
 * d'entourer de blanc un element qu'on n'a pas designe.
 *
 * <p>Les valeurs sont celles de l'original, bornes comprises : de -512 a 512. Hors bornes,
 * la saisie est refusee et le champ passe au rouge, exactement comme avant.
 */
@OnlyIn(Dist.CLIENT)
public class CustomizeUiScreen extends Screen {

    private static final int OUTLINE = 0xFFFFFFFF;

    /** Les images de l'original, telles quelles : aucune n'est redessinee. */
    private static final ResourceLocation PANEL_TEXTURE = texture("window_ui_resize");
    private static final ResourceLocation CPBAR_TEXTURE = texture("edit_preview/cpbar");
    private static final ResourceLocation KEY_HINT_TEXTURE = texture("edit_preview/key_hint");
    private static final ResourceLocation NOTIFY_LOGO_TEXTURE = texture("edit_preview/notify_logo");
    private static final ResourceLocation NOTIFY_BACK_TEXTURE = texture("notification/back");
    private static final int CPBAR_W = 512;
    private static final int CPBAR_H = 78;
    private static final int NOTIFY_LOGO = 64;

    /** Le rappel des touches : son image, a l'echelle de l'apercu de l'original. */
    private static final float KEY_HINT_SCALE = 0.46f;
    private static final int KEY_HINT_IMAGE_W = 128;
    private static final int KEY_HINT_IMAGE_H = 193;

    /**
     * Ce que l'apercu montre : les deux lignes d'exemple de l'original.
     *
     * <p>Le reste de ses mesures vit dans {@link CustomizeUiLayout}, avec le HUD : l'apercu et le
     * HUD ne peuvent donc pas diverger. Les deux TITRES, eux, viennent de la langue —
     * {@code ac.gui.uiedit.demo.notification} et {@code ac.media.only_my_railgun.name} —, parce
     * qu'un apercu doit se lire dans la langue du joueur, comme le HUD qu'il imite.
     */
    private static final String NOTIFY_DEMO_TEXT = "blablabla";
    private static final String MEDIA_DEMO_TIME = "04:30";

    /** L'ecran precedent, pour y revenir sur Echap. */
    private final Screen parent;

    private final HudLayout layout;

    private final List<HudElement> elements = List.of(HudElement.values());

    private HudElement selected;
    private boolean editingX = true;
    private boolean editing = false;
    private String buffer = "";
    private boolean bufferBad = false;

    public CustomizeUiScreen(Screen parent) {
        super(Component.translatable("ac.gui.uiedit.elements"));
        this.parent = parent;
        this.layout = HudConfig.read();
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath("academy", "textures/guis/" + path + ".png");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        // Le fond de l'ecran vient d'etre dessine, et il coupe le melange en partant. Chaque
        // dessin le rallume donc lui-meme : sans cela, tout ce qui a de la transparence sort
        // en aplat opaque et les textes ne se melangent pas.
        blend();

        // Les apercus, a leur place reelle. Le panneau passe apres eux, comme avant.
        for (HudElement element : elements) {
            drawPreview(graphics, element);
        }

        drawPanel(graphics, mouseX, mouseY);

        if (selected != null) {
            drawEditBox(graphics, selected.ordinal());
        }

        RenderSystem.disableBlend();
    }

    /**
     * Rallume le melange avant un dessin.
     *
     * <p>Indispensable : {@code graphics.fill} termine par {@code disableBlend()}. Comme
     * l'ordre de dessin passe par le lecteur media (qui n'est fait que de {@code fill}) avant
     * la notification, le panneau et le temoin de CP, tout ce qui suit sortait en aplats
     * opaques : les fondus des textures et la transparence de l'icone etaient perdus.
     */
    private static void blend() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /**
     * Un aplat, le melange rallume juste avant.
     *
     * <p>Tous les aplats de ce fichier passent par ici : la teinte d'une ligne, le fond de la
     * caisse, la barre du lecteur. {@code graphics.fill} a son propre type de rendu, mais il part
     * de l'etat laisse par le dessin precedent — et ce qui precede un aplat translucide n'est pas
     * toujours un aplat. Une seule fois sans melange, et les pixels qui devaient laisser voir le
     * monde sortent en plein.
     */
    private static void fill(GuiGraphics graphics, int x1, int y1, int x2, int y2, int colour) {
        blend();
        graphics.fill(x1, y1, x2, y2, colour);
    }

    /**
     * Un carre creux dont le trait se compte en pixels <b>d'ecran</b>, comme l'original.
     *
     * <p>Son {@code Outline} dessine un {@code GL_LINE_LOOP} de {@code lineWidth} 2, et cette
     * largeur-la est en pixels de la FENETRE : un cheveu, quel que soit l'agrandissement de
     * l'interface. Notre ecran se compte en pixels GUI, qui en valent deux ou trois — un
     * {@code renderOutline} d'un pixel GUI, et pire deux cernes colles comme ce fichier les
     * dessinait, faisaient donc un cadre jusqu'a trois fois trop epais.
     *
     * <p>La pose est ramenee au pixel d'ecran le temps du trait : le carre garde l'epaisseur de
     * l'original a toutes les echelles d'interface, et rien ne depend plus de celle du joueur.
     */
    private void frame(GuiGraphics graphics, int x, int y, int w, int h, int colour) {
        int scale = Math.max(1, (int) minecraft.getWindow().getGuiScale());

        // Le MELANGE, toujours : la teinte d'une ligne, le fond de la caisse et la barre du
        // lecteur portent un alpha, et un aplat parti sans melange sort en plein.
        blend();

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.scale(1.0f / scale, 1.0f / scale, 1.0f);

        int left = x * scale;
        int top = y * scale;
        int right = (x + w) * scale;
        int bottom = (y + h) * scale;
        int line = LINE;

        graphics.fill(left, top, right, top + line, colour);
        graphics.fill(left, bottom - line, right, bottom, colour);
        graphics.fill(left, top + line, left + line, bottom - line, colour);
        graphics.fill(right - line, top + line, right, bottom - line, colour);

        pose.popPose();
    }

    /** L'epaisseur du trait des deux carres : les deux pixels d'ecran de l'original. */
    private static final int LINE = 2;

    /** Le haut d'un texte dont on connait le bas : l'original cale ses boites par le bas. */
    private int textTopForBottom(float bottom) {
        return Math.round(bottom - font.lineHeight);
    }

    /** Le panneau des elements : sa texture d'origine, a moitie, et ses lignes. */
    private void drawPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        blend();

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(CustomizeUiLayout.PANEL_X, CustomizeUiLayout.PANEL_Y, 0);
        pose.scale((float) CustomizeUiLayout.SCALE, (float) CustomizeUiLayout.SCALE, 1.0f);

        graphics.blit(PANEL_TEXTURE, 0, 0,
                CustomizeUiLayout.PANEL_W, CustomizeUiLayout.PANEL_H,
                0.0f, 0.0f,
                CustomizeUiLayout.PANEL_W, CustomizeUiLayout.PANEL_H,
                CustomizeUiLayout.PANEL_W, CustomizeUiLayout.PANEL_H);

        for (int i = 0; i < elements.size(); ++i) {
            fill(graphics, CustomizeUiLayout.BODY_X,
                    CustomizeUiLayout.BODY_Y + i * CustomizeUiLayout.ROW_H,
                    CustomizeUiLayout.BODY_X + CustomizeUiLayout.ROW_W,
                    CustomizeUiLayout.BODY_Y + (i + 1) * CustomizeUiLayout.ROW_H,
                    isOverRow(i, mouseX, mouseY)
                            ? CustomizeUiLayout.ROW_TINT_HOVER : CustomizeUiLayout.ROW_TINT);
        }

        pose.popPose();

        float headerScale = CustomizeUiLayout.fontScale(CustomizeUiLayout.HEADER_FONT);
        drawText(graphics, title, CustomizeUiLayout.headerTextLeft(),
                CustomizeUiLayout.textTop(CustomizeUiLayout.headerTextCenterY(),
                        Math.round(CustomizeUiLayout.LINE * headerScale)),
                headerScale, 1.0f, CustomizeUiLayout.HEADER_TEXT);

        float rowScale = CustomizeUiLayout.fontScale(CustomizeUiLayout.ROW_FONT);
        for (int i = 0; i < elements.size(); ++i) {
            drawText(graphics, Component.translatable(elements.get(i).getLabelKey()),
                    CustomizeUiLayout.rowTextLeft(),
                    CustomizeUiLayout.textTop(CustomizeUiLayout.rowTextCenterY(i),
                            Math.round(CustomizeUiLayout.LINE * rowScale)),
                    rowScale, 1.0f, CustomizeUiLayout.ROW_TEXT);
        }
    }

    /** L'apercu d'un element, a sa place reelle et a la taille de l'original. */
    private void drawPreview(GuiGraphics graphics, HudElement element) {
        blend();

        int w = element.getPreviewWidth();
        int h = element.getPreviewHeight();
        int x = layout.placeX(element, width, w);
        int y = layout.placeY(element, height, h);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);

        switch (element) {
            case CP_BAR -> {
                blend();
                graphics.blit(CPBAR_TEXTURE, 0, 0, w, h, 0.0f, 0.0f,
                        CPBAR_W, CPBAR_H, CPBAR_W, CPBAR_H);
            }
            case KEY_HINT -> {
                // Son image, a l'echelle ou l'original la montre dans cet ecran.
                pose.pushPose();
                pose.scale(KEY_HINT_SCALE, KEY_HINT_SCALE, 1.0f);
                blend();
                graphics.blit(KEY_HINT_TEXTURE, 0, 0,
                        KEY_HINT_IMAGE_W, KEY_HINT_IMAGE_H,
                        0.0f, 0.0f,
                        KEY_HINT_IMAGE_W, KEY_HINT_IMAGE_H,
                        KEY_HINT_IMAGE_W, KEY_HINT_IMAGE_H);
                pose.popPose();
            }
            case NOTIFICATION -> drawNotificationPreview(graphics);
            case MEDIA -> drawMediaPreview(graphics);
        }

        pose.popPose();

        // Le temoin de l'element choisi : un carre creux autour de son apercu, comme le `Outline`
        // que l'original posait sur le widget d'apercu. Il suit la BOITE de l'apercu, pas son
        // image (chez lui le widget fait 140x210 pour une image de 128x193), et son trait garde
        // l'epaisseur de l'original : voir {@link #frame}.
        if (element == selected) {
            frame(graphics, x - 1, y - 1, w + 2, h + 2, OUTLINE);
        }
    }

    /** La notification : son fond, son icone, et les deux lignes d'exemple de l'original. */
    private void drawNotificationPreview(GuiGraphics graphics) {
        blend();

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.scale(CustomizeUiLayout.NOTIFY_SCALE, CustomizeUiLayout.NOTIFY_SCALE, 1.0f);

        graphics.blit(NOTIFY_BACK_TEXTURE, 0, 0, CustomizeUiLayout.NOTIFY_W,
                CustomizeUiLayout.NOTIFY_H, 0.0f, 0.0f, CustomizeUiLayout.NOTIFY_W,
                CustomizeUiLayout.NOTIFY_H, CustomizeUiLayout.NOTIFY_W, CustomizeUiLayout.NOTIFY_H);
        // Le MELANGE avant CHAQUE image, jamais une fois pour deux : une icone tient tout entiere
        // dans son alpha, et sans lui elle ne laisse qu'un carre blanc. C'est le meme piege que
        // sous les apercus du tutoriel : voir {@code CGuiScreen.drawTexture}.
        blend();
        graphics.blit(NOTIFY_LOGO_TEXTURE, CustomizeUiLayout.NOTIFY_ICON_X, CustomizeUiLayout.NOTIFY_ICON_Y,
                CustomizeUiLayout.NOTIFY_ICON, CustomizeUiLayout.NOTIFY_ICON, 0.0f, 0.0f,
                NOTIFY_LOGO, NOTIFY_LOGO, NOTIFY_LOGO, NOTIFY_LOGO);

        blend();
        drawText(graphics, Component.translatable("ac.gui.uiedit.demo.notification"),
                CustomizeUiLayout.NOTIFY_TITLE_X,
                CustomizeUiLayout.NOTIFY_TITLE_Y,
                CustomizeUiLayout.plainFontScale(CustomizeUiLayout.NOTIFY_TITLE_FONT),
                CustomizeUiLayout.NOTIFY_SCALE, CustomizeUiLayout.NOTIFY_TEXT_COLOR);
        drawText(graphics, Component.literal(NOTIFY_DEMO_TEXT), CustomizeUiLayout.NOTIFY_TEXT_X,
                CustomizeUiLayout.NOTIFY_TEXT_Y,
                CustomizeUiLayout.plainFontScale(CustomizeUiLayout.NOTIFY_TEXT_FONT),
                CustomizeUiLayout.NOTIFY_SCALE, CustomizeUiLayout.NOTIFY_TEXT_COLOR);

        pose.popPose();
    }

    /** Le lecteur media : pas d'image, son titre, sa duree et sa barre de progression. */
    private void drawMediaPreview(GuiGraphics graphics) {
        blend();

        // La barre, dessinee au dixieme de pixel : le fond gris fait 1,3 de haut et la
        // progression blanche 2,2, donc du blanc juste au-dessus et juste en dessous.
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.scale(CustomizeUiLayout.MEDIA_BAR_STEP, CustomizeUiLayout.MEDIA_BAR_STEP, 1.0f);

        int unit = Math.round(1.0f / CustomizeUiLayout.MEDIA_BAR_STEP);
        int barX = CustomizeUiLayout.MEDIA_BAR_X * unit;
        int barY = CustomizeUiLayout.MEDIA_BAR_Y * unit;
        int barW = CustomizeUiLayout.MEDIA_BAR_W * unit;
        int grey = CustomizeUiLayout.MEDIA_BAR_GREY_TENTHS;
        int white = CustomizeUiLayout.MEDIA_BAR_WHITE_TENTHS;
        int inset = (white - grey) / 2;

        fill(graphics, barX, barY - inset,
                barX + Math.round(barW * CustomizeUiLayout.MEDIA_BAR_PROGRESS), barY - inset + white,
                CustomizeUiLayout.MEDIA_BAR_FILL);
        fill(graphics, barX, barY, barX + barW, barY + grey, CustomizeUiLayout.MEDIA_BAR_BACK);
        pose.popPose();

        // Le titre se cale par le bas de sa boite (juste au-dessus de la barre), et la duree
        // par le bas de la sienne, dix pixels plus bas : c'est ce qui les met a deux hauteurs.
        // Le bas se retire avec la hauteur de ligne de la POLICE, et non celle du xml : c'est
        // celle-la qui est dessinee, et l'ecart se voit (le titre tombait sur la barre).
        float titleScale = CustomizeUiLayout.plainFontScale(CustomizeUiLayout.MEDIA_TITLE_FONT);
        float timeScale = CustomizeUiLayout.plainFontScale(CustomizeUiLayout.MEDIA_TIME_FONT);
        blend();
        drawText(graphics, Component.translatable("ac.media.only_my_railgun.name"),
                CustomizeUiLayout.MEDIA_TITLE_X,
                textTopForBottom(CustomizeUiLayout.MEDIA_TITLE_BOTTOM),
                titleScale, 1.0f, CustomizeUiLayout.NOTIFY_TEXT_COLOR);
        blend();
        drawText(graphics, Component.literal(MEDIA_DEMO_TIME), CustomizeUiLayout.MEDIA_TIME_X,
                textTopForBottom(CustomizeUiLayout.MEDIA_TIME_BOTTOM),
                timeScale, 1.0f, CustomizeUiLayout.NOTIFY_TEXT_COLOR);
    }

    /** Le cadre des deux champs, a droite de la ligne de l'element choisi. */
    private void drawEditBox(GuiGraphics graphics, int index) {
        blend();

        int x = CustomizeUiLayout.editLeft();
        int y = CustomizeUiLayout.editTop(index);

        fill(graphics, x, y, x + CustomizeUiLayout.EDIT_W, y + CustomizeUiLayout.EDIT_H,
                CustomizeUiLayout.EDIT_BACK);
        // Son contour : le trait de deux pixels d'ecran de son `Outline`, pas deux cernes colles.
        frame(graphics, x, y, CustomizeUiLayout.EDIT_W, CustomizeUiLayout.EDIT_H,
                CustomizeUiLayout.EDIT_EDGE);

        int top = CustomizeUiLayout.fieldTop(index);
        drawText(graphics, Component.literal("X"), x + CustomizeUiLayout.LABEL_X, top,
                1.0f, 1.0f, CustomizeUiLayout.FIELD_TEXT);
        drawText(graphics, Component.literal("Y"), x + CustomizeUiLayout.LABEL_Y, top,
                1.0f, 1.0f, CustomizeUiLayout.FIELD_TEXT);

        drawField(graphics, index, true);
        drawField(graphics, index, false);
    }

    /** Un des deux champs, avec sa valeur ; il passe au rouge si la saisie ne passe pas. */
    private void drawField(GuiGraphics graphics, int index, boolean x) {
        blend();

        int left = CustomizeUiLayout.fieldLeft(x);
        int top = CustomizeUiLayout.fieldTop(index);
        boolean focused = editing && x == editingX;

        fill(graphics, left, top, left + CustomizeUiLayout.FIELD_W,
                top + CustomizeUiLayout.FIELD_H,
                focused && bufferBad ? CustomizeUiLayout.FIELD_BAD : CustomizeUiLayout.FIELD_BACK);

        String value = focused ? buffer
                : String.valueOf(x ? layout.getX(selected) : layout.getY(selected));
        drawText(graphics, Component.literal(value), left + 1, top, 1.0f, 1.0f,
                CustomizeUiLayout.FIELD_TEXT);
    }

    /**
     * Ecrit un texte dans la police du jeu.
     *
     * <p>L'original dessinait avec une police <b>du systeme</b> (Microsoft YaHei, lue par AWT),
     * et Minecraft ne sait pas la rendre proprement : il la rasterise sans hinting puis la
     * reduit. Toutes les facons de contourner ont ete essayees et mesurees (tailles de raster,
     * sur-echantillonnage, filtrage lisse de l'atlas, gravage des glyphes d'avance) : le port
     * dessine donc avec la police du jeu, quitte a s'eloigner un peu de la police d'origine.
     *
     * <p>La taille dessinee vaut {@code scale * poseScale} — c'est le sens de {@code scale}, qui
     * n'est PAS encore reduit par la pose. Sans pose reduite (le panneau, le lecteur media, les
     * champs) elle est arrondie au <b>multiple entier</b> de la police : toute taille
     * fractionnaire est reechantillonnee, donc floue ; le minimum y est donc la taille de la
     * police du jeu. Les apercus, eux, sont dessines deux fois plus petits par leur pose (0,25) :
     * leur texte garde l'echelle demandee, sinon il sortirait plus gros que l'image qu'il
     * accompagne.
     *
     * <p>{@code poseScale} est l'echelle de la pose en place : l'echelle appliquee ici la
     * compense, pour que le texte tombe sur la taille voulue a l'ecran.
     */
    private void drawText(GuiGraphics graphics, Component text, int left, int top,
                          float scale, float poseScale, int color) {
        blend();

        float wanted = scale * poseScale;
        float step = poseScale == 1.0f ? Math.max(1.0f, Math.round(scale)) : wanted;
        float inner = step / poseScale;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(left, top, 0);
        pose.scale(inner, inner, 1.0f);
        AcademyFonts.draw(graphics, font, text, 0, 0, color, false);
        pose.popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // Les lignes de la liste : cliquer en choisit une, et les champs la suivent.
        for (int i = 0; i < elements.size(); ++i) {
            if (isOverRow(i, mouseX, mouseY)) {
                if (elements.get(i) != selected) {
                    selected = elements.get(i);
                    editing = false;
                    bufferBad = false;
                }
                return true;
            }
        }

        // Les deux champs de l'element choisi.
        if (selected != null) {
            int index = selected.ordinal();
            if (isOverField(mouseX, mouseY, index, true)) {
                beginEdit(true);
                return true;
            }
            if (isOverField(mouseX, mouseY, index, false)) {
                beginEdit(false);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean isOverRow(int index, double mouseX, double mouseY) {
        return mouseX >= CustomizeUiLayout.rowLeft()
                && mouseX < CustomizeUiLayout.rowLeft() + CustomizeUiLayout.rowWidth()
                && mouseY >= CustomizeUiLayout.rowTop(index)
                && mouseY < CustomizeUiLayout.rowTop(index) + CustomizeUiLayout.rowHeight();
    }

    private static boolean isOverField(double mouseX, double mouseY, int index, boolean x) {
        int left = CustomizeUiLayout.fieldLeft(x);
        int top = CustomizeUiLayout.fieldTop(index);
        return mouseX >= left && mouseX < left + CustomizeUiLayout.FIELD_W
                && mouseY >= top && mouseY < top + CustomizeUiLayout.FIELD_H;
    }

    /** Ouvre un champ : on tape dedans, et Entree valide. */
    private void beginEdit(boolean x) {
        editing = true;
        editingX = x;
        bufferBad = false;
        // Le champ affiche la valeur comme l'original : telle qu'un nombre s'ecrit.
        buffer = String.valueOf(x ? layout.getX(selected) : layout.getY(selected));
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!editing) return super.charTyped(codePoint, modifiers);
        if ((codePoint >= '0' && codePoint <= '9') || codePoint == '-' || codePoint == '.') {
            buffer += codePoint;
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editing) {
            if (keyCode == 259 && !buffer.isEmpty()) { // retour arriere
                buffer = buffer.substring(0, buffer.length() - 1);
                bufferBad = false;
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Entree
                confirmEdit();
                return true;
            }
            if (keyCode == 256) { // Echap ferme le champ avant l'ecran
                editing = false;
                bufferBad = false;
                return true;
            }
            return true;
        }

        if (keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void confirmEdit() {
        double value;
        try {
            value = Double.parseDouble(buffer.trim());
        } catch (NumberFormatException e) {
            bufferBad = true;
            return;
        }

        if (!HudLayout.isValid(value)) {
            bufferBad = true;
            return;
        }

        double x = editingX ? value : layout.getX(selected);
        double y = editingX ? layout.getY(selected) : value;
        // L'original ne referme pas le champ apres avoir valide : on peut corriger sans le
        // rouvrir, et c'est la couleur du fond qui dit si la valeur est passee.
        if (HudConfig.write(selected, x, y)) {
            layout.set(selected, x, y);
            bufferBad = false;
        } else {
            bufferBad = true;
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
