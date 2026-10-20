package cn.academy.terminal.client;

import cn.academy.AcademyCraft;
import cn.academy.ModSounds;
import cn.academy.client.gui.AcademyFonts;
import cn.academy.sound.AcademySounds;
import cn.academy.terminal.App;
import cn.academy.terminal.AppRegistry;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * L'ecran du terminal de donnees : la page de l'original.
 *
 * <p>Portage de {@code TerminalUI}, sur son document {@code guis/terminal.xml} : une page de
 * <b>640 sur 785</b> — le fond {@code back.png}, le logo, les deux mots « DATA » et
 * « TERMINAL », le nom du joueur, le compte d'applications suivi de l'heure du monde — et une
 * <b>grille de trois par trois</b> de cases de 151 unites au pas de 180, a partir de (30 ;
 * 155). Chaque case porte {@code app_back.png}, ou {@code app_back_highlight.png} quand elle
 * est visee, l'icone de l'application et son nom.
 *
 * <p>Ce qui fait vivre la page, comme chez l'original :
 * <ul>
 *   <li>les cases <b>apparaissent en decalage</b>, une par une : chacune s'allume sur
 *       {@code (age - (rang + 1) * 0,1) / 0,4} de seconde ;</li>
 *   <li>la case visee s'eclaire : son fond passe en surbrillance, son icone a 0,8 et son nom
 *       a 0,82, les autres restant a 0,6 et 0,2 ;</li>
 *   <li>changer de case <b>claque</b> le son de selection du terminal ;</li>
 *   <li>un halo suit la souris ({@code cursor.png}, en melange additif) ;</li>
 *   <li>les fleches haut et bas font defiler la grille d'une rangee, et s'effacent quand il
 *       n'y a plus rien a faire defiler.</li>
 * </ul>
 *
 * <h2>La perspective, comme chez lui</h2>
 *
 * <p>L'original ne posait pas la page a plat : il montait une vraie scene 3D, vue en
 * {@code gluPerspective(50)}, posee devant et a droite — « comme si on la tenait » — et
 * penchee de {@code -18} degres en lacet et {@code 7} en tangage, le tout a l'echelle
 * {@code 1/310}.
 *
 * <p>Cette scene est reprise ici pour sa <b>pose</b> : la meme chaine de rotations, posee sur
 * la pose de dessin du jeu, la meme echelle, et la meme place — trois quarts a droite, un peu
 * au-dessus du milieu. Une chose manque : la <b>fuite</b> de la perspective. Le jeu ecrit ses
 * sommets avec une transformation affine (il jette la quatrieme composante), donc une
 * projection en perspective posee sur la pose s'ecrase au lieu de fuir ; la page est donc vue
 * de biais, mais sans raccourci. Le cadre, les cases et le son sont ceux de l'original.
 *
 * <p>Comme la page n'est plus face a l'ecran, la souris ne tombe pas droit dessus : elle est
 * <b>desempilee</b> par l'inverse de la meme matrice, ce qui donne les coordonnees exactes
 * sous le curseur. Le halo est <b>retenu dans la page</b> (0 a 605, 0 a 740, comme chez lui)
 * et le curseur du jeu est <b>cache</b> : seul le point blanc reste.
 *
 * <p>Un ecart : le curseur suit la souris de l'ecran au lieu d'etre pris en main par le jeu —
 * un {@code Screen} ne peut pas detourner la souris comme le faisait son {@code MouseHelper}.
 */
public class TerminalScreen extends Screen {

    // ------------------------------------------------------------------
    // Le document `guis/terminal.xml`, en unites de page.
    // ------------------------------------------------------------------

    private static final double PAGE_W = 640.0;
    private static final double PAGE_H = 785.0;

    /** La grille : des cases de 151 au pas de 180, trois par rangee. */
    private static final double GRID_X = 30.0;
    private static final double GRID_Y = 155.0;
    private static final double CELL = 151.0;
    private static final double STEP = 180.0;
    private static final int COLUMNS = 3;
    private static final int ROWS = 3;

    /** La grille de selection de l'original, plus petite que la page. */
    private static final double SELECT_W = 605.0;
    private static final double SELECT_H = 740.0;

    /** L'icone et le nom, dans une case. */
    private static final double ICON_X = 40.0;
    private static final double ICON_Y = 50.0;
    private static final double ICON_SIZE = 50.0;
    private static final double LABEL_Y = 148.0;
    private static final int LABEL_CELL = 32;

    /** Le cartouche : le logo, les deux mots, le joueur et le compte. */
    private static final double LOGO_X = 40.0;
    private static final double LOGO_Y = 50.0;
    private static final double LOGO_SIZE = 50.0;
    private static final double STATIC_X = 98.0;
    private static final double STATIC_0_Y = 42.0;
    private static final double STATIC_1_Y = 74.0;
    private static final int STATIC_CELL = 40;
    private static final double USER_X = 400.0;
    private static final double USER_Y = 41.66666666666667;
    private static final double USER_W = 200.0;
    private static final double COUNT_X = 300.0;
    private static final double COUNT_Y = 84.0;
    private static final double COUNT_W = 300.0;
    private static final int USER_CELL = 40;
    private static final int COUNT_CELL = 30;

    private static final int STATIC_COLOUR = 0xAAFFFFFF;
    private static final int USER_COLOUR = 0xCCFFFFFF;
    private static final int COUNT_COLOUR = 0x99FFFFFF;

    /** Les fleches de defilement : 100 sur 25, a 0,8, en haut et en bas de la page. */
    private static final double ARROW_W = 80.0;
    private static final double ARROW_H = 20.0;
    private static final double ARROW_UP_Y = 133.0;
    private static final double ARROW_DOWN_Y = PAGE_H - 40.0 - ARROW_H;

    /** Le halo de la souris : 20 a 24 de cote, un peu plus quand une case est visee. */
    private static final double CURSOR_BASE = 20.0;
    private static final int CURSOR_ALPHA = 180;

    /** Le decalage d'apparition des cases, et sa duree. */
    private static final double APPEAR_STEP = 0.1;
    private static final double APPEAR_TIME = 0.4;

    // ------------------------------------------------------------------
    // La scene de l'original : sa place, ses angles, sa vitesse de curseur.
    // ------------------------------------------------------------------

    /**
     * La part de la hauteur d'ecran que prend la page.
     *
     * <p>L'original la posait en unites du monde, devant un regard a 50 degres : sa taille a
     * l'ecran suivait donc celle de la fenetre. Ici la meme regle, en clair : deux tiers de la
     * hauteur, ce qui laisse le panneau sur la droite sans manger tout l'ecran.
     */
    private static final double VIEW_HEIGHT_PART = 0.66;

    /** Sa place : trois quarts a droite, un peu au-dessus du milieu — la ou son regard la posait. */
    private static final double VIEW_X = 0.74;
    private static final double VIEW_Y = 0.52;

    /** Les angles de l'original, et ce que la souris y ajoute. */
    private static final double BASE_YAW = -18.0;
    private static final double YAW_SWING = 4.0;
    private static final double BASE_PITCH = 7.0;
    private static final double PITCH_SWING = 4.0;
    private static final double BASE_ROLL = -1.6;

    /** Le curseur glisse vers sa cible a cette vitesse, en unites par seconde. */
    private static final double BALANCE_SPEED = 3000.0;

    private static final ResourceLocation TEX_BACK = texture("back.png");
    private static final ResourceLocation TEX_APP = texture("app_back.png");
    private static final ResourceLocation TEX_APP_HIGHLIGHT = texture("app_back_highlight.png");
    private static final ResourceLocation TEX_ARROW_UP = texture("arrow_up.png");
    private static final ResourceLocation TEX_ARROW_DOWN = texture("arrow_down.png");
    private static final ResourceLocation TEX_CURSOR = texture("cursor.png");
    private static final ResourceLocation TEX_LOGO = texture("logo.png");

    private final List<App> apps;

    /** La premiere rangee montree, quand la grille defile. */
    private int scroll;

    /** L'instant d'ouverture : c'est lui qui donne l'age de la page, donc l'apparition. */
    private final long openedNano = System.nanoTime();

    /** La case visee a l'image precedente : c'est le changement qui fait claquer le son. */
    private int lastSelected = -1;

    /** Le curseur dans la page, retenu dans ses bords : c'est lui qui incline la scene. */
    private double buffX = SELECT_W / 2;
    private double buffY = SELECT_H / 2;

    public TerminalScreen() {
        super(Component.translatable("ac.terminal.title"));
        this.apps = installedApps();
    }

    @Override
    protected void init() {
        super.init();
        // Le curseur du jeu disparait : seul le halo de la page reste visible.
        GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(),
                GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
    }

    @Override
    public void removed() {
        super.removed();
        GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(),
                GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                "textures/guis/data_terminal/" + name);
    }

    /**
     * Les applications proposees par le terminal.
     *
     * Deux filtres, et pas un de plus : il faut que l'application soit installee, et qu'un
     * ecran lui soit associe cote client. Une application preinstallee de l'original mais non
     * portee n'a pas d'ecran, donc elle ne s'affiche pas : dessiner une icone qui n'ouvre rien
     * serait pire que de ne rien dessiner.
     */
    private static List<App> installedApps() {
        return ClientTerminalData.get().getInstalledApps(AppRegistry.INSTANCE).stream()
                .filter(AppScreens::isAvailable)
                .toList();
    }

    // ------------------------------------------------------------------
    // Rendu
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Le terminal de l'original flottait sur le jeu sans l'assombrir : il ne prend donc
        // pas le fond des contenants, et le jeu ne se met pas en pause.
        //
        // La souris est d'abord ramenee dans la page avec la scene de l'image precedente,
        // puis le curseur glisse vers elle : c'est lui qui incline la scene, donc il faut
        // le connaitre avant de poser la matrice.
        double[] target = pageAt(mouseX, mouseY, buffX, buffY);
        double step = BALANCE_SPEED * (minecraft == null ? 0.016 : minecraft.getDeltaFrameTime());
        buffX = balance(buffX, target[0], step);
        buffY = balance(buffY, target[1], step);

        graphics.pose().pushPose();
        graphics.pose().mulPoseMatrix(sceneMatrix(buffX, buffY));

        image(graphics, TEX_BACK, 0, 0, PAGE_W, PAGE_H, 640, 785);
        drawHeader(graphics);
        drawApps(graphics, buffX, buffY);
        drawArrows(graphics);
        drawCursor(graphics, buffX, buffY);

        graphics.pose().popPose();
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    /** Le logo, les deux mots du terminal, le joueur, et le compte suivi de l'heure. */
    private void drawHeader(GuiGraphics graphics) {
        image(graphics, TEX_LOGO, LOGO_X, LOGO_Y, LOGO_SIZE, LOGO_SIZE, 50, 50);

        text(graphics, "DATA", STATIC_X, STATIC_0_Y, 100, STATIC_CELL, STATIC_COLOUR, false);
        text(graphics, "TERMINAL", STATIC_X, STATIC_1_Y, 160, STATIC_CELL, STATIC_COLOUR, false);

        String user = minecraft == null || minecraft.player == null
                ? "" : minecraft.player.getGameProfile().getName();
        text(graphics, user, USER_X, USER_Y, USER_W, USER_CELL, USER_COLOUR, true);

        text(graphics, countText(), COUNT_X, COUNT_Y, COUNT_W, COUNT_CELL, COUNT_COLOUR, true);
    }

    /** « 3 Apps, 14:36 » : le compte des applications, puis l'heure du monde. */
    private String countText() {
        String count = Component.translatable("ac.gui.terminal.appcount", apps.size()).getString();

        if (minecraft == null || minecraft.level == null) return count;

        int time = (int) (minecraft.level.getDayTime() % 24000);
        return count + ", " + two(time / 1000) + ":" + two((time % 1000) * 60 / 1000);
    }

    private static String two(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    /**
     * La grille des applications, avec l'apparition en decalage et l'eclairage de la case
     * visee.
     */
    private void drawApps(GuiGraphics graphics, double pageX, double pageY) {
        double age = (System.nanoTime() - openedNano) / 1_000_000_000.0;
        int selected = cellAt(pageX, pageY);

        // Le son de selection se joue quand la case visee CHANGE, comme chez l'original.
        if (selected >= 0 && selected != lastSelected && minecraft != null
                && minecraft.player != null) {
            AcademySounds.playFor(minecraft.player, ModSounds.TERMINAL_SELECT,
                    SoundSource.MASTER, 0.2f, 1.0f);
        }
        lastSelected = selected;

        int first = scroll * COLUMNS;
        int last = Math.min(apps.size(), first + COLUMNS * ROWS);
        for (int index = first; index < last; index++) {
            int order = index - first;
            double x = GRID_X + STEP * (order % COLUMNS);
            double y = GRID_Y + STEP * (order / COLUMNS);

            // L'apparition : chaque case s'allume un dixieme de seconde apres la precedente.
            double appear = clamp((age - (index + 1) * APPEAR_STEP) / APPEAR_TIME, 0.0, 1.0);
            boolean hovered = index == selected;

            imageAlpha(graphics, hovered ? TEX_APP_HIGHLIGHT : TEX_APP, x, y, CELL, CELL,
                    151, 151, (int) Math.round(255 * appear));

            App app = apps.get(index);
            int iconAlpha = (int) Math.round(255 * (hovered ? 0.8 : 0.6) * appear);
            imageAlpha(graphics, app.getIcon(), x + ICON_X, y + ICON_Y, ICON_SIZE, ICON_SIZE,
                    app.getIconSize(), app.getIconSize(), iconAlpha);

            double textPart = hovered ? 0.1 + 0.72 * appear : 0.1 + 0.1 * appear;
            text(graphics, app.getDisplayName().getString(), x, y + LABEL_Y, CELL, LABEL_CELL,
                    ((int) Math.round(255 * textPart) << 24) | 0x00FFFFFF, false);
        }
    }

    /** Les deux fleches, effacees quand il n'y a rien a faire defiler. */
    private void drawArrows(GuiGraphics graphics) {
        if (scroll > 0) {
            image(graphics, TEX_ARROW_UP, (PAGE_W - ARROW_W) / 2, ARROW_UP_Y, ARROW_W, ARROW_H,
                    100, 25);
        }
        if (scroll < maxScroll()) {
            image(graphics, TEX_ARROW_DOWN, (PAGE_W - ARROW_W) / 2, ARROW_DOWN_Y, ARROW_W,
                    ARROW_H, 100, 25);
        }
    }

    /**
     * Le halo de la souris, en melange additif.
     *
     * <p>Il se pose sur le curseur de la page, qui est retenu dans ses bords ; l'opacite est
     * celle de l'original (0,4 en additif), remontee un peu pour ressortir sur le fond sombre.
     */
    private void drawCursor(GuiGraphics graphics, double pageX, double pageY) {
        double seconds = (System.nanoTime() - openedNano) / 1_000_000_000.0;
        double beat = CURSOR_BASE + Math.sin(seconds * 3.333) * 2;
        double size = (cellAt(pageX, pageY) >= 0 ? 1.3 : 1.0) * beat;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        imageAlpha(graphics, TEX_CURSOR, pageX - size / 2, pageY - size / 2, size, size,
                20, 20, CURSOR_ALPHA);
        RenderSystem.defaultBlendFunc();
    }

    /**
     * La case sous un point de la page, ou -1.
     *
     * <p>Ce sont les <b>vraies</b> cases qui comptent, pas la grille de selection de
     * l'original (605 sur 740 coupee en trois par trois) : sa grille debordait des cases, et
     * le joueur cliquait une application en visant sa voisine.
     */
    private int cellAt(double pageX, double pageY) {
        int first = scroll * COLUMNS;
        int last = Math.min(apps.size(), first + COLUMNS * ROWS);

        for (int index = first; index < last; index++) {
            int order = index - first;
            double x = GRID_X + STEP * (order % COLUMNS);
            double y = GRID_Y + STEP * (order / COLUMNS);
            if (pageX >= x && pageX < x + CELL && pageY >= y && pageY < y + CELL) return index;
        }
        return -1;
    }

    /** Combien de rangees on peut faire defiler, comme le {@code getMaxScroll} de l'original. */
    private int maxScroll() {
        int rows = (apps.size() + COLUMNS - 1) / COLUMNS;
        return Math.max(0, rows - ROWS);
    }

    private void scrollBy(int rows) {
        int before = scroll;
        scroll = Math.max(0, Math.min(maxScroll(), scroll + rows));
        if (scroll != before) lastSelected = -1;
    }

    // ------------------------------------------------------------------
    // Interaction
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        double[] point = pageAt(mouseX, mouseY, buffX, buffY);
        double pageX = point[0];
        double pageY = point[1];

        // Les fleches de defilement.
        double arrowX = (PAGE_W - ARROW_W) / 2;
        if (inside(pageX, pageY, arrowX, ARROW_UP_Y, ARROW_W, ARROW_H)) {
            scrollBy(-1);
            return true;
        }
        if (inside(pageX, pageY, arrowX, ARROW_DOWN_Y, ARROW_W, ARROW_H)) {
            scrollBy(1);
            return true;
        }

        // Une case : elle ouvre son application.
        int index = cellAt(pageX, pageY);
        if (index >= 0) {
            Screen page = AppScreens.create(apps.get(index));
            if (minecraft != null && page != null) {
                // Le seul son de l'original qui se declare dans la categorie `master`, et le
                // plus discret de tous : 0,2, un clic de selection.
                if (minecraft.player != null) {
                    AcademySounds.playFor(minecraft.player, ModSounds.TERMINAL_SELECT,
                            SoundSource.MASTER, 0.2f, 1.0f);
                }
                minecraft.setScreen(page);
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // La molette fait la meme chose que les fleches : l'original n'avait que le bord de
        // l'ecran, faute de pouvoir lire la molette dans son overlay.
        if (delta != 0) {
            scrollBy(delta > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        // L'overlay de l'original ne mettait pas le jeu en pause.
        return false;
    }

    // ------------------------------------------------------------------
    // Outils de dessin et de mesure
    // ------------------------------------------------------------------

    /**
     * La pose de l'original : sa place, ses trois angles, et son echelle.
     *
     * <p>L'ordre est celui de ses appels GL, du dernier au premier pour un sommet :
     * on ramene le point au centre de la page, on le met a l'echelle (l'axe Y retourne, comme
     * son {@code scale(1/310, -1/310)}), on le fait tourner de ses trois angles, puis on le
     * pose a sa place sur l'ecran.
     *
     * <p>La fuite en perspective manque : le jeu ecrit ses sommets avec une transformation
     * affine — il jette la quatrieme composante — donc une projection en perspective posée
     * sur la pose s'ecrase. La page est donc vue de biais, sans raccourci.
     */
    private Matrix4f sceneMatrix(double cursorX, double cursorY) {
        double time = (System.nanoTime() - openedNano) / 1_000_000_000.0;

        double yaw = BASE_YAW - YAW_SWING * (cursorX / SELECT_W - 0.5) + Math.sin(time);
        double pitch = BASE_PITCH + PITCH_SWING * (cursorY / SELECT_H - 0.5);

        Matrix4f scene = new Matrix4f();
        scene.translate((float) (width * VIEW_X), (float) (height * VIEW_Y), 0.0f);
        scene.rotateZ((float) Math.toRadians(BASE_ROLL));
        scene.rotateY((float) Math.toRadians(yaw));
        scene.rotateX((float) Math.toRadians(pitch));
        scene.scale((float) viewScale(), (float) viewScale(), 1.0f);
        scene.translate((float) (-PAGE_W / 2.0), (float) (-PAGE_H / 2.0), 0.0f);
        return scene;
    }

    /**
     * Ou tombe cette souris, en unites de page.
     *
     * <p>La page n'est pas face a l'ecran : un point de l'ecran ne lui correspond pas
     * directement. C'est l'inverse de sa pose qui le dit, ce qui rend les coordonnees exactes
     * sous le curseur — et donc des cases qu'on vise vraiment.
     */
    private double[] pageAt(double mouseX, double mouseY, double cursorX, double cursorY) {
        Matrix4f inverse = new Matrix4f(sceneMatrix(cursorX, cursorY));
        inverse.invert();

        Vector4f point = new Vector4f((float) mouseX, (float) mouseY, 0.0f, 1.0f);
        inverse.transform(point);

        if (Math.abs(point.w) < 1.0e-6) return new double[] { cursorX, cursorY };

        return new double[] {
                clamp(point.x / point.w, 0.0, SELECT_W),
                clamp(point.y / point.w, 0.0, SELECT_H)
        };
    }

    /** L'echelle de la page : deux tiers de la hauteur de l'ecran. */
    private double viewScale() {
        return height * VIEW_HEIGHT_PART / PAGE_H;
    }

    /** Le curseur glisse vers sa cible, a la vitesse de l'original. */
    private static double balance(double from, double to, double step) {
        double distance = to - from;
        if (Math.abs(distance) <= step) return to;
        return from + Math.signum(distance) * step;
    }

    private static boolean inside(double x, double y, double left, double top, double width,
                                  double height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Un texte pose par le haut de sa cellule, coupe a la largeur de sa boite.
     *
     * <p>C'est la regle du {@code TextBox} de l'original : pas de debordement, pas de points de
     * suspension.
     */
    private void text(GuiGraphics graphics, String value, double boxX, double boxY,
                      double boxWidth, int cell, int colour, boolean rightAligned) {
        String shown = fit(value, boxWidth, cell);
        int x = (int) Math.round(rightAligned
                ? boxX + boxWidth - AcademyFonts.width(null, shown, cell)
                : boxX);
        AcademyFonts.drawTop(graphics, font, shown, x, (int) Math.round(boxY), colour, cell);
    }

    private static String fit(String text, double boxWidth, int cell) {
        double acc = 0;
        int i = 0;
        while (i < text.length() && acc < boxWidth) {
            int codePoint = text.codePointAt(i);
            acc += AcademyFonts.width(null, new String(Character.toChars(codePoint)), cell);
            i += Character.charCount(codePoint);
        }
        return text.substring(0, i);
    }

    private static void image(GuiGraphics graphics, ResourceLocation texture, double x, double y,
                              double width, double height, int texWidth, int texHeight) {
        imageAlpha(graphics, texture, x, y, width, height, texWidth, texHeight, 255);
    }

    private static void imageAlpha(GuiGraphics graphics, ResourceLocation texture, double x,
                                   double y, double width, double height, int texWidth,
                                   int texHeight, int alpha) {
        if (alpha <= 0) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1f, 1f, 1f, alpha / 255f);
        graphics.blit(texture, (int) Math.round(x), (int) Math.round(y),
                (int) Math.round(width), (int) Math.round(height),
                0f, 0f, texWidth, texHeight, texWidth, texHeight);
        graphics.setColor(1f, 1f, 1f, 1f);
    }
}
