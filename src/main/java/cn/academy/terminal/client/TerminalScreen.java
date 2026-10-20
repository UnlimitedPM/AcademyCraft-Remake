package cn.academy.terminal.client;

import cn.academy.AcademyCraft;
import cn.academy.ModSounds;
import cn.academy.client.gui.AcademyFonts;
import cn.academy.sound.AcademySounds;
import cn.academy.terminal.App;
import cn.academy.terminal.AppRegistry;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.time.LocalTime;
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

    /**
     * La grille : des cases de 151 au pas de 180, trois par rangee.
     *
     * <p>Le depart est a 65, pas a 30 : c'est le {@code START_X} de {@code TerminalUI}, et c'est
     * ce qui CENTRE les trois colonnes (65 a gauche, 64 a droite sur une page de 640). Le joueur
     * l'a vu tout de suite — « les applications ne sont plus du tout bien centrees ».
     */
    private static final double GRID_X = 65.0;
    private static final double GRID_Y = 155.0;
    private static final double CELL = 151.0;
    private static final double STEP = 180.0;
    private static final int COLUMNS = 3;
    private static final int ROWS = 3;

    /** La grille de selection de l'original, plus petite que la page. */
    private static final double SELECT_W = 605.0;
    private static final double SELECT_H = 740.0;

    /** L'icone et le nom, dans une case : l'icone est grande — 110 sur 151, comme le document. */
    private static final double ICON_X = 9.0;
    private static final double ICON_Y = 32.0;
    private static final double ICON_SIZE = 110.0;
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
    private static final int CURSOR_ALPHA = 255;

    /** Le decalage d'apparition des cases, et sa duree. */
    private static final double APPEAR_STEP = 0.1;
    private static final double APPEAR_TIME = 0.4;

    /**
     * En combien de morceaux par cote se coupent la plaque d'une case et son icone.
     *
     * <p>Un morceau garde des cotes paralleles : plus il y en a, plus le trapeze de la
     * perspective est fin. Quatre par cote ramene l'erreur sous le pixel pour une case de 151.
     */
    private static final int CELL_STEPS = 4;
    private static final int ICON_STEPS = 4;

    /**
     * De combien la plaque d'une case est ECLAIREE avant d'etre posee.
     *
     * <p>Calibrage sur la reference du joueur (captures cote a cote) : sa plaque vaut
     * (90 ; 94 ; 82) sur sa page, et le dessin en deux passages donne (88 ; 92 ; 80) — deux
     * unites de moins. La consigne est revenue deux fois : « le fond gris est trop transparent »
     * puis, une fois la plaque eclaircie a 1,3 (105 ; 108 ; 96), « notre gris est trop clair ».
     * Le dessin des deux passages est donc JUSTE, c'est ce +3 % qui tombe exactement sur sa
     * valeur. Ne pas retoucher sans une nouvelle capture de reference.
     */
    private static final float PLATE_TINT = 1.03f;

    // ------------------------------------------------------------------
    // La camera de l'original : sa place devant l'oeil, ses angles, son champ.
    // ------------------------------------------------------------------

    /** Le champ de vision de l'original, en degres : son {@code gluPerspective(50, ...)}. */
    private static final double CAM_FOV = 50.0;

    /** Son echelle : la page de 640 sur 785 tient dans 1/310 de sa taille, en unites du monde. */
    private static final double CAM_UNIT = 1.0 / 310.0;

    /** Ses angles de repos, et le balayage que la souris y ajoute (les nombres de l'original). */
    private static final double BASE_YAW = -18.0;
    private static final double YAW_SWING = 4.0;
    private static final double BASE_PITCH = 7.0;
    private static final double PITCH_SWING = 4.0;
    private static final double BASE_ROLL = -1.6;

    /**
     * POURQUOI LA PROJECTION EST CALCULEE A LA MAIN.
     *
     * <p>Le joueur, apres quatre essais : « j'ai l'impression que le point de depart pour faire
     * tourner le terminal est mal pose, comme s'il tournait autour du point en haut a droite au
     * lieu du centre », puis « normalement le terminal est penche vers notre direction, plus ou
     * moins par rapport au centre du jeu la ou il y a le curseur ».
     *
     * <p>Les deux reproches viennent de la MEME chose : une pose affine ne sait pas montrer la
     * perspective. Chez l'original la page est posee quatre unites devant l'oeil, a droite ; son
     * cote droit est donc plus loin, plus court, et elle parait tournee VERS LE JOUEUR. Ici les
     * quatre coins sont projetes par cette camera, et chaque element est dessine dans la petite
     * transformation locale de son propre voisinage : le fond, les icones, les textes et le
     * curseur suivent donc tous la perspective, comme dans son mod.
     */
    private static final int CAM_GRID_X = 10;
    private static final int CAM_GRID_Y = 12;

    /** Combien de fois le contenu de la page est pose (l'original le pose deux fois). */
    private static final int CONTENT_PASSES = 2;

    /** La camera de l'image, refaite a chaque rendu. */
    private static final Matrix4f IDENTITY = new Matrix4f();
    private Matrix4f frameCamera = IDENTITY;

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

    /** La saisie du chat, tant qu'elle est ouverte : la page la prend elle-meme. */
    private boolean chatMode;
    private String chatText = "";

    /** Le premier caractere apres l'ouverture est avale (la touche du chat le produit aussi). */
    private boolean swallowChar;

    /** Instant de l'ouverture, pour ignorer une repetition de la meme touche. */
    private long chatOpenedAt;

    /** Position du pointeur a l'ouverture du chat, rendue telle quelle a la sortie. */
    private double chatPointerX;
    private double chatPointerY;

    public TerminalScreen() {
        super(Component.translatable("ac.terminal.title"));
        this.apps = installedApps();

        // L'icone de MisakaCloud est tiree au hasard A CHAQUE OUVERTURE du terminal (le joueur :
        // « elle ne change pas aleatoirement a chaque fois que je ferme et que je reouvre »).
        // L'original tirait a chaque rendu, donc elle scintillait ; ici le tirage est par ouverture.
        for (App app : this.apps) {
            if (app instanceof cn.academy.terminal.app.AppTutorial tutorial) {
                tutorial.rerollIcon();
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        // Le curseur du jeu disparait : seul le halo de la page reste visible.
        GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(),
                GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);

        // La page revient la ou elle etait : son halo ET le pointeur du systeme. Sans ce
        // replacement, revenir du chat posait le pointeur hors de la page, et `confineCursor`
        // le rabattait sur le bord gauche — « ca remet mon curseur au milieu a gauche ».
        frameCamera = buildCamera(yawNow(), pitchNow());
        warpCursorToPage();
    }

    /** Pose le pointeur du systeme sur le curseur de la page (en pixels de fenetre). */
    private void warpCursorToPage() {
        float[] point = project(frameCamera, buffX, buffY);
        var window = Minecraft.getInstance().getWindow();
        double sx = window.getWidth() / (double) Math.max(1, width);
        double sy = window.getHeight() / (double) Math.max(1, height);
        GLFW.glfwSetCursorPos(window.getWindow(), point[0] * sx, point[1] * sy);
    }

    /**
     * Garde la VRAIE souris DANS LE TERMINAL.
     *
     * <p>Le halo de la page est borne (0..605, 0..740), mais cela n'empechait pas le pointeur
     * du systeme de sortir : en multi-ecran il passait sur le voisin et le clic changeait de
     * fenetre. Le joueur a demande la meme limite que le curseur dessine, donc on borne le
     * pointeur au RECTANGLE DE LA PAGE (sa boite a l'ecran), et non a la fenetre du jeu.
     *
     * <p>Un ecran recoit des coordonnees absolues : on ne peut pas detourner le pointeur comme
     * le faisait le {@code MouseHelper} de l'original, on le rapporte donc au bord des qu'il
     * le depasse (une fois par image, ce qui suffit a le retenir).
     */
    private void confineCursor() {
        Matrix4f camera = frameCamera;
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        double[][] corners = { { 0, 0 }, { PAGE_W, 0 }, { 0, PAGE_H }, { PAGE_W, PAGE_H } };
        for (double[] corner : corners) {
            float[] point = project(camera, corner[0], corner[1]);
            minX = Math.min(minX, point[0]);
            maxX = Math.max(maxX, point[0]);
            minY = Math.min(minY, point[1]);
            maxY = Math.max(maxY, point[1]);
        }

        var window = Minecraft.getInstance().getWindow();
        double sx = window.getWidth() / (double) Math.max(1, width);
        double sy = window.getHeight() / (double) Math.max(1, height);
        double loX = Math.max(1.0, minX * sx);
        double hiX = Math.min(window.getWidth() - 2.0, maxX * sx);
        double loY = Math.max(1.0, minY * sy);
        double hiY = Math.min(window.getHeight() - 2.0, maxY * sy);

        long handle = window.getWindow();
        double[] x = new double[1], y = new double[1];
        GLFW.glfwGetCursorPos(handle, x, y);
        double cx = Math.max(loX, Math.min(hiX, x[0]));
        double cy = Math.max(loY, Math.min(hiY, y[0]));
        if (cx != x[0] || cy != y[0]) {
            GLFW.glfwSetCursorPos(handle, cx, cy);
        }
    }

    /**
     * Une touche du jeu, dans la page : le terminal se referme, et le CHAT s'ouvre.
     *
     * <p>Un {@code KeyMapping} ne rend ses clics que hors d'un ecran (son contexte est
     * {@code IN_GAME}) : ouvert, c'est donc a la page de reconnaitre SA touche. C'est ce que
     * faisait l'original, dont la meme touche allumait et eteignait l'overlay.
     *
     * <p>LE CHAT SE TAPE ICI, sans changer d'ecran. Passer par un {@code ChatScreen} obligeait a
     * fermer la page : Minecraft reprenait alors le pointeur (retour au centre de la fenetre) et
     * la page revenait une image plus tard — le joueur voyait la page clignoter et sa souris
     * sauter, « moi si possible je voudrais que ca ne le fasse juste pas du tout ». La page prend
     * donc la saisie elle-meme : Entree envoie, Echap annule, Retour arriere efface.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (chatMode) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                // Entree gardee enfoncee repete l'evenement : ouvrir puis envoyer tout de suite
                // donnait l'impression d'un double appui (« comme si j'appuyais 2 fois »).
                if (System.currentTimeMillis() - chatOpenedAt < 150L) return true;
                sendChat();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                setChatMode(false);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !chatText.isEmpty()) {
                chatText = chatText.substring(0, chatText.length() - 1);
                return true;
            }
            return true;
        }

        if (keyCode == TerminalKeyBindings.OPEN_TERMINAL.getKey().getValue()) {
            onClose();
            return true;
        }
        if (minecraft != null && keyCode == minecraft.options.keyChat.getKey().getValue()) {
            // La touche qui ouvre le chat produit AUSSI son caractere (T ecrivait « t »). On avale
            // le premier caractere, et on note l'instant pour qu'une repetition de la meme touche
            // (Entree gardee enfoncee) ne referme pas la ligne tout de suite.
            swallowChar = true;
            chatOpenedAt = System.currentTimeMillis();
            setChatMode(true);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Entre ou sort de la saisie du chat.
     *
     * <p>Pendant la saisie, le POINTEUR DU SYSTEME redevient visible et libre : le joueur veut
     * « quand meme voir la souris bouger quand j'ecris dans le chat ». Le halo de la page, lui,
     * reste fige la ou il etait.
     *
     * <p>A la sortie, on remet le pointeur DU SYSTEME sur le halo : sinon il se retrouve hors de
     * la page, `confineCursor` le rabattrait sur le bord et le halo sauterait avec lui — « elle
     * revient encore au milieu sur la gauche ». Il ne peut donc plus rien se deplacer.
     */
    private void setChatMode(boolean on) {
        chatMode = on;
        long handle = Minecraft.getInstance().getWindow().getWindow();

        if (on) {
            // On RETIENT la position du pointeur, pour la rendre telle quelle en sortant : relire
            // une projection puis la reposer laissait une erreur d'echelle (pixels de fenetre
            // contre pixels d'interface) et le halo finissait « un peu trop haut et un peu trop a
            // gauche ». Les coordonnees de GLFW se rendent dans leur propre unite, donc au pixel.
            GLFW.glfwSetInputMode(handle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            double[] x = new double[1], y = new double[1];
            GLFW.glfwGetCursorPos(handle, x, y);
            chatPointerX = x[0];
            chatPointerY = y[0];
            return;
        }

        chatText = "";
        GLFW.glfwSetInputMode(handle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        GLFW.glfwSetCursorPos(handle, chatPointerX, chatPointerY);
        frameCamera = buildCamera(yawNow(), pitchNow());
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!chatMode) return super.charTyped(codePoint, modifiers);
        if (swallowChar) {
            swallowChar = false;
            return true;
        }
        if (chatText.length() < 250 && !Character.isISOControl(codePoint)) {
            chatText += codePoint;
        }
        return true;
    }

    /** Envoie ce qui est tape : une commande si la ligne commence par « / ». */
    private void sendChat() {
        String text = chatText.trim();
        setChatMode(false);
        if (text.isEmpty() || minecraft == null || minecraft.player == null) return;
        if (text.startsWith("/")) {
            minecraft.player.connection.sendCommand(text.substring(1));
        } else {
            minecraft.player.connection.sendChat(text);
        }
    }

    /** La ligne de saisie, posee en bas de l'ecran comme celle du jeu. */
    private void drawChatInput(GuiGraphics graphics) {
        int top = height - 14;
        graphics.fill(2, top, width - 2, height - 2, 0x80000000);
        String caret = (System.currentTimeMillis() / 500L) % 2L == 0L ? "_" : "";
        // LA POLICE DU JEU, pas celle du mod : la ligne s'ecrit comme le chat du jeu (le joueur :
        // « quand j'ecris dans le chat c'est ecrit avec la police d'ecriture de windows »).
        graphics.drawString(font, chatText + caret, 4, top + 3, 0xFFFFFFFF);
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
                .sorted(java.util.Comparator.comparingInt(app -> orderOf(app.getName())))
                .toList();
    }

    /**
     * L'ordre des applications, celui de l'original.
     *
     * <p>Il ne venait pas du registre mais de ses annotations {@code @RegApp(priority = ...)} : le
     * tutoriel d'abord, puis le reglage (-1), puis l'ecran « a propos » (-2), et le lecteur media
     * en dernier. Le joueur l'a vu : chez lui la premiere case est MisakaCloud, pas About.
     */
    private static final List<String> APP_ORDER =
            List.of("tutorial", "skill_tree", "freq_transmitter", "settings", "about", "media_player");

    private static int orderOf(String name) {
        int index = APP_ORDER.indexOf(name);
        return index < 0 ? APP_ORDER.size() : index;
    }

    // ------------------------------------------------------------------
    // Rendu
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // PENDANT LA SAISIE DU CHAT, LA SOURIS NE BOUGE PLUS : ni le halo, ni la page. Le joueur :
        // « je ne veux pas non plus pouvoir la bouger pendant que le chat est ouvert ». On ne lit
        // donc plus la souris du tout tant que la ligne est ouverte.
        if (!chatMode) {
            confineCursor();
            double[] target = pageAt(mouseX, mouseY);
            double step = BALANCE_SPEED * (minecraft == null ? 0.016 : minecraft.getDeltaFrameTime());
            buffX = balance(buffX, target[0], step);
            buffY = balance(buffY, target[1], step);
        }

        frameCamera = buildCamera(yawNow(), pitchNow());

        renderPage(graphics);

        graphics.setColor(1f, 1f, 1f, 1f);
    }

    /**
     * La page elle-meme, sans toucher a la souris.
     *
     * <p>Appelee par {@link #render} quand le terminal a la main, et par l'overlay du chat
     * quand il ne l'a plus : c'est ce qui le laisse AFFICHE pendant qu'on ecrit.
     */
    public void renderPage(GuiGraphics graphics) {
        if (frameCamera == null || frameCamera == IDENTITY) {
            frameCamera = buildCamera(yawNow(), pitchNow());
        }

        backdrop(graphics);

        // Tout le contenu se dessine DEUX FOIS, comme le fond. Le joueur : « le fond est encore
        // un peu trop sombre, le texte et les icones aussi ». C'est bien ce que fait l'original :
        // mesure sur sa capture, le corps du panneau vaut 0,36 d'opacite pour un PNG qui n'en
        // porte que 0,20 — soit {@code 1 - 0,8^2}, deux passages — et le meme doublage éclaircit
        // ses plaques (0,36 -> 0,59), ses icones (0,6 -> 0,84) et ses noms (0,2 -> 0,36).
        for (int pass = 0; pass < CONTENT_PASSES; pass++) {
            drawHeader(graphics);
            drawApps(graphics, buffX, buffY);
            drawArrows(graphics);
        }

        drawCursor(graphics, buffX, buffY);

        if (chatMode) {
            drawChatInput(graphics);
        }

        graphics.setColor(1f, 1f, 1f, 1f);
    }

    /**
     * Les deux angles du moment : ceux de l'original, plus ce que la souris y ajoute.
     *
     * <p>ECART ASSUME : l'original ajoutait {@code + 1 * sin(t)} au lacet, ce qui faisait
     * lentement respirer la page. Le joueur le voit comme un defaut — « les app bougent un peu
     * meme quand on ne deplace pas la souris du tout » — et il a raison de le voir ainsi : ce
     * balancement deplace aussi le point sous le curseur, donc le halo derivait tout seul.
     * Retire : la page ne bouge plus que quand la souris bouge.
     */
    private double yawNow() {
        return BASE_YAW - YAW_SWING * (buffX / SELECT_W - 0.5);
    }

    private double pitchNow() {
        return BASE_PITCH + PITCH_SWING * (buffY / SELECT_H - 0.5);
    }

    /**
     * Le fond de la page, en damier.
     *
     * <p>Un seul quadrilatere ne peut pas suivre une projection : la perspective courbe ses
     * bords. La page est donc coupee en cases, chacune avec sa propre transformation locale —
     * c'est ce qui donne la silhouette en trapeze de l'original. Le fond se pose DEUX FOIS,
     * comme chez lui (mesure sur sa capture : 0,36 d'opacite pour un PNG qui n'en porte que
     * 0,20, soit {@code 1 - 0,8^2}).
     */
    private void backdrop(GuiGraphics graphics) {
        // Le fond ne passe PAS par une image par case : deux cases voisines arrondies au pixel
        // laissent un liseré clair (le joueur le voyait en damier). Les sommets sont donc
        // calcules un par un et le maillage est soude : les bords mitoyens sont exactement les
        // memes, il n'y a plus de couture.
        //
        // ET SURTOUT PAS `RenderType.text` : ce type passe par le shader du TEXTE, qui JETTE les
        // fragments d'alpha inferieur a 0,1 (le `discard` de `rendertype_text`). Or les bords de
        // back.png sont a 12/255 — le joueur a vu disparaitre « les couleurs blanches semi
        // transparentes sur les bordures ». On prend donc le meme chemin qu'un blit : position
        // + uv, et le shader `position_tex`.
        graphics.flush();

        RenderSystem.setShaderTexture(0, TEX_BACK);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        for (int pass = 0; pass < 2; pass++) {
            for (int col = 0; col < CAM_GRID_X; col++) {
                for (int row = 0; row < CAM_GRID_Y; row++) {
                    double x0 = PAGE_W * col / CAM_GRID_X;
                    double x1 = PAGE_W * (col + 1) / CAM_GRID_X;
                    double y0 = PAGE_H * row / CAM_GRID_Y;
                    double y1 = PAGE_H * (row + 1) / CAM_GRID_Y;
                    float u0 = (float) (x0 / PAGE_W);
                    float u1 = (float) (x1 / PAGE_W);
                    float v0 = (float) (y0 / PAGE_H);
                    float v1 = (float) (y1 / PAGE_H);

                    pageVertex(builder, x0, y0, u0, v0);
                    pageVertex(builder, x0, y1, u0, v1);
                    pageVertex(builder, x1, y1, u1, v1);
                    pageVertex(builder, x1, y0, u1, v0);
                }
            }
        }

        BufferUploader.drawWithShader(builder.end());
    }

    /** Un sommet de la page, deja projete a l'ecran. */
    private void pageVertex(BufferBuilder builder, double pageX, double pageY, float u, float v) {
        float[] point = project(frameCamera, pageX, pageY);
        builder.vertex(point[0], point[1], 0.0f).uv(u, v).endVertex();
    }

    /**
     * Une image de la page, EN PERSPECTIVE.
     *
     * <p>Un element pose par la transformation locale de {@link #pushLocal} garde ses cotes
     * paralleles : c'est un parallelogramme. Or la page, elle, se resserre de 17 pour cent vers
     * son cote eloigne. Les plaques et les icones des applications paraissaient donc posees a
     * plat sur une page inclinee — le joueur : « j'ai l'impression que les app doivent elles
     * aussi etre decalees plus ou moins comme le terminal en lui meme ».
     *
     * <p>La case est donc coupee en petits morceaux, chacun avec ses QUATRE coins projetes par
     * la camera : la fuite entre ainsi dans l'element, comme dans le fond. Quatre morceaux par
     * cote suffisent — l'ecart de projection sur 151 unites vaut quelques pixels, divise par
     * quatre il passe sous le pixel.
     *
     * <p>Passer par un {@link BufferBuilder} oblige a vider d'abord ce que la page a en attente :
     * les sommets ecrits ici partent tout de suite, et l'ordre du dessin doit rester celui des
     * appels.
     */
    private void imageProjected(GuiGraphics graphics, ResourceLocation texture, double x,
                                double y, double width, double height, int alpha, float tint,
                                int steps) {
        if (alpha <= 0) return;

        graphics.flush();
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(tint, tint, tint, alpha / 255f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        for (int col = 0; col < steps; col++) {
            for (int row = 0; row < steps; row++) {
                double x0 = x + width * col / steps;
                double x1 = x + width * (col + 1) / steps;
                double y0 = y + height * row / steps;
                double y1 = y + height * (row + 1) / steps;
                float u0 = (float) (col / (double) steps);
                float u1 = (float) ((col + 1) / (double) steps);
                float v0 = (float) (row / (double) steps);
                float v1 = (float) ((row + 1) / (double) steps);

                pageVertex(builder, x0, y0, u0, v0);
                pageVertex(builder, x0, y1, u0, v1);
                pageVertex(builder, x1, y1, u1, v1);
                pageVertex(builder, x1, y0, u1, v0);
            }
        }

        BufferUploader.drawWithShader(builder.end());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    /** Le logo, les deux mots du terminal, le joueur, et le compte suivi de l'heure. */
    private void drawHeader(GuiGraphics graphics) {
        // Le logo profite du meme anticrenelage que les icones (il est reduit lui aussi).
        smooth(TEX_LOGO);
        image(graphics, TEX_LOGO, LOGO_X, LOGO_Y, LOGO_SIZE, LOGO_SIZE, 50, 50);

        text(graphics, "DATA", STATIC_X, STATIC_0_Y, 100, STATIC_CELL, STATIC_COLOUR, false);
        text(graphics, "TERMINAL", STATIC_X, STATIC_1_Y, 160, STATIC_CELL, STATIC_COLOUR, false);

        String user = minecraft == null || minecraft.player == null
                ? "" : minecraft.player.getGameProfile().getName();
        text(graphics, user, USER_X, USER_Y, USER_W, USER_CELL, USER_COLOUR, true);

        text(graphics, countText(), COUNT_X, COUNT_Y, COUNT_W, COUNT_CELL, COUNT_COLOUR, true);
    }

    /**
     * « 3 Apps, 14:36 » : le compte des applications, puis l'heure REELLE de l'ordinateur.
     *
     * <p>L'original affichait l'heure du MONDE (fuseau de Minecraft). Le joueur a demande
     * l'heure de son PC : « a cote du nombre d'app installees, que ca ecrive l'heure reelle sur
     * notre ordinateur ». C'est donc {@link LocalTime#now()}, au format 24 heures.
     */
    private String countText() {
        String count = Component.translatable("ac.gui.terminal.appcount", apps.size()).getString();
        return count + ", " + CLOCK.format(LocalTime.now());
    }

    /** Le format de l'horloge : 24 heures, deux chiffres partout, comme l'original. */
    private static final java.time.format.DateTimeFormatter CLOCK =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm");

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

            // La plaque de la case passe par le maillage projete, pas par la pose affine : elle
            // doit se resserrer avec la page (voir `imageProjected`). Elle est LISSEE, comme les
            // icones : a 0,83 d'echelle son liseré sortait en escalier.
            smooth(hovered ? TEX_APP_HIGHLIGHT : TEX_APP);
            imageProjected(graphics, hovered ? TEX_APP_HIGHLIGHT : TEX_APP, x, y, CELL, CELL,
                    (int) Math.round(255 * appear), PLATE_TINT, CELL_STEPS);

            App app = apps.get(index);
            int iconAlpha = (int) Math.round(255 * (hovered ? 0.8 : 0.6) * appear);
            // Les icones se dessinent LISSEES : le joueur voyait « des escaliers en trop » et
            // les trouve anticrenelees chez l'original. Le reste de la page garde le plus proche
            // voisin (c'est ce que fait son interface, et les traits fins y restent nets).
            smooth(app.getIcon());
            imageProjected(graphics, app.getIcon(), x + ICON_X, y + ICON_Y, ICON_SIZE,
                    ICON_SIZE, iconAlpha, 1.0f, ICON_STEPS);

            double textPart = hovered ? 0.1 + 0.72 * appear : 0.1 + 0.1 * appear;
            // Le document centre le nom dans sa case de 151 (son TextBox dit `align=CENTER`) ;
            // aligne a gauche, il debordait vers la droite et la case ne se lisait plus
            // centree — le joueur l'a vu des la premiere image.
            textCentered(graphics, app.getDisplayName().getString(), x, y + LABEL_Y, CELL,
                    LABEL_CELL, ((int) Math.round(255 * textPart) << 24) | 0x00FFFFFF);
        }
    }

    /** Les deux fleches, effacees quand il n'y a rien a faire defiler. */
    private void drawArrows(GuiGraphics graphics) {
        if (scroll > 0) {
            image(graphics, TEX_ARROW_UP, (PAGE_W - ARROW_W) / 2, ARROW_UP_Y, ARROW_W, ARROW_H,
                    64, 17);
        }
        if (scroll < maxScroll()) {
            image(graphics, TEX_ARROW_DOWN, (PAGE_W - ARROW_W) / 2, ARROW_DOWN_Y, ARROW_W,
                    ARROW_H, 64, 17);
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
                32, 32, CURSOR_ALPHA);
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
        if (chatMode) {
            // La page ne se manipule pas pendant la saisie : la souris y est figee.
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        double[] point = pageAt(mouseX, mouseY);
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
            App app = apps.get(index);
            if (minecraft != null && AppScreens.isAvailable(app)) {
                // Le seul son de l'original qui se declare dans la categorie `master`, et le
                // plus discret de tous : 0,2, un clic de selection.
                if (minecraft.player != null) {
                    AcademySounds.playFor(minecraft.player, ModSounds.TERMINAL_SELECT,
                            SoundSource.MASTER, 0.2f, 1.0f);
                }
                // La page s'ouvre PAR-DESSUS le terminal, qui revient a sa fermeture.
                AppScreens.open(app, this);
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

    /**
     * Echap ne ferme PAS la page.
     *
     * <p>Le joueur : « dans le vrai mod, pour le refermer il n'y a que la meme touche que celle
     * qui l'a ouvert ». Echap servait au chat ; une fois celui-ci ferme, il ne doit pas emporter
     * le terminal avec lui.
     */
    @Override
    public boolean shouldCloseOnEsc() {
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
    private Matrix4f buildCamera(double yaw, double pitch) {
        double aspect = width / (double) Math.max(1, height);

        Matrix4f camera = new Matrix4f();
        camera.translate((float) (0.35 * aspect + 1.0), (float) -0.6, -4.0f);
        camera.rotateZ((float) Math.toRadians(BASE_ROLL));
        camera.rotateY((float) Math.toRadians(yaw));
        camera.rotateX((float) Math.toRadians(pitch));
        camera.translate(-1.0f, 1.8f, 0.0f);
        camera.scale((float) CAM_UNIT, (float) -CAM_UNIT, (float) CAM_UNIT);
        return camera;
    }

    /** Un point de la page, projete a l'ecran (en unites d'interface). */
    private float[] project(Matrix4f camera, double pageX, double pageY) {
        Vector4f point = new Vector4f((float) pageX, (float) pageY, 0.0f, 1.0f);
        camera.transform(point);

        double depth = -point.z;
        double tanHalf = Math.tan(Math.toRadians(CAM_FOV / 2.0));
        double aspect = width / (double) Math.max(1, height);
        double ndcX = point.x / (depth * tanHalf * aspect);
        double ndcY = point.y / (depth * tanHalf);

        return new float[] {
                (float) ((ndcX * 0.5 + 0.5) * width),
                (float) ((1.0 - (ndcY * 0.5 + 0.5)) * height)
        };
    }

    /**
     * La petite transformation locale d'une case de la page.
     *
     * <p>Elle s'appuie sur les trois coins projetes de la case : chaque element (image, texte,
     * case d'application) est donc dessine DANS la perspective, avec son orientation et son
     * echelle du moment. C'est ce qui remplace la pose affine et rend l'inclinaison de
     * l'original — bord droit plus court, page tournee vers le joueur.
     */
    private void pushLocal(GuiGraphics graphics, double x, double y, double w, double h) {
        double safeW = Math.max(w, 1.0e-3);
        double safeH = Math.max(h, 1.0e-3);

        float[] origin = project(frameCamera, x, y);
        float[] alongX = project(frameCamera, x + safeW, y);
        float[] alongY = project(frameCamera, x, y + safeH);

        double ux = (alongX[0] - origin[0]) / safeW;
        double uy = (alongX[1] - origin[1]) / safeW;
        double vx = (alongY[0] - origin[0]) / safeH;
        double vy = (alongY[1] - origin[1]) / safeH;

        // Le constructeur de JOML range ses arguments par COLONNE (son m01 est la colonne 0,
        // ligne 1) : les deux bases vont donc par groupes de quatre, puis la translation.
        Matrix4f local = new Matrix4f(
                (float) ux, (float) uy, 0.0f, 0.0f,
                (float) vx, (float) vy, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                (float) (origin[0] - ux * x - vx * y), (float) (origin[1] - uy * x - vy * y),
                0.0f, 1.0f);

        graphics.pose().pushPose();
        graphics.pose().mulPoseMatrix(local);
    }

    private static void popLocal(GuiGraphics graphics) {
        graphics.pose().popPose();
    }

    /**
     * Ou tombe cette souris, en unites de page.
     *
     * <p>La projection n'a pas d'inverse direct simple : on la remonte par Newton, en partant du
     * curseur de l'image precedente (il bouge de quelques unites par image, deux ou trois pas
     * suffisent donc a tomber au pixel pres).
     */
    private double[] pageAt(double mouseX, double mouseY) {
        Matrix4f camera = buildCamera(yawNow(), pitchNow());
        double x = buffX;
        double y = buffY;

        for (int step = 0; step < 8; step++) {
            float[] here = project(camera, x, y);
            double errorX = here[0] - mouseX;
            double errorY = here[1] - mouseY;
            if (Math.abs(errorX) < 0.05 && Math.abs(errorY) < 0.05) break;

            float[] alongX = project(camera, x + 1.0, y);
            float[] alongY = project(camera, x, y + 1.0);
            double j11 = alongX[0] - here[0];
            double j21 = alongX[1] - here[1];
            double j12 = alongY[0] - here[0];
            double j22 = alongY[1] - here[1];

            double det = j11 * j22 - j12 * j21;
            if (Math.abs(det) < 1.0e-9) break;

            x -= (j22 * errorX - j12 * errorY) / det;
            y -= (-j21 * errorX + j11 * errorY) / det;
        }

        return new double[] { clamp(x, 0.0, SELECT_W), clamp(y, 0.0, SELECT_H) };
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
        pushLocal(graphics, boxX, boxY, Math.max(boxWidth, 1.0), Math.max(cell, 1));
        AcademyFonts.drawTop(graphics, font, shown, x, (int) Math.round(boxY), colour, cell);
        popLocal(graphics);
    }

    /** Un texte centre dans sa boite, comme le {@code align=CENTER} du document. */
    private void textCentered(GuiGraphics graphics, String value, double boxX, double boxY,
                              double boxWidth, int cell, int colour) {
        String shown = fit(value, boxWidth, cell);
        int x = (int) Math.round(boxX + (boxWidth - AcademyFonts.width(null, shown, cell)) / 2.0);
        pushLocal(graphics, boxX, boxY, Math.max(boxWidth, 1.0), Math.max(cell, 1));
        AcademyFonts.drawTop(graphics, font, shown, x, (int) Math.round(boxY), colour, cell);
        popLocal(graphics);
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

    private void image(GuiGraphics graphics, ResourceLocation texture, double x, double y,
                       double width, double height, int texWidth, int texHeight) {
        imageAlpha(graphics, texture, x, y, width, height, texWidth, texHeight, 255);
    }

    /**
     * Passe une texture en filtrage LINEAIRE (voir {@link cn.academy.client.gui.GuiTextures}).
     *
     * <p>Le joueur : « les icones des app dans le vrai mod ont un anti aliasing dessus, la je vois
     * des escaliers en trop ». Un blit de GUI echantillonne au plus proche voisin, donc une icone
     * de 110 pixels dessinee plus petite sort en escalier.
     *
     * <p>LINEAIRE SIMPLE, sans mipmaps. Les mipmaps avaient ete essayes pour « enlever les
     * escaliers », et c'est eux qui les ont crees : une icone de 110 px dessinee sur 94 lit
     * 1,18 texel par pixel, donc le filtre trilineaire melange deja le niveau deux fois plus
     * petit, et le dessin sort par gros blocs — « les icones sont pas belles comme dans le vrai
     * mod, comme si les bords etaient pixelises ». Le vrai mod, lui, dessine ses icones a
     * l'echelle 1 (page de 640 unites sur 640 pixels) : il n'a besoin d'aucun mipmap.
     */
    private static void smooth(ResourceLocation texture) {
        cn.academy.client.gui.GuiTextures.smooth(texture);
    }

    private void imageAlpha(GuiGraphics graphics, ResourceLocation texture, double x,
                            double y, double width, double height, int texWidth,
                            int texHeight, int alpha) {
        if (alpha <= 0) return;

        pushLocal(graphics, x, y, width, height);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1f, 1f, 1f, alpha / 255f);
        graphics.blit(texture, (int) Math.round(x), (int) Math.round(y),
                (int) Math.round(width), (int) Math.round(height),
                0f, 0f, texWidth, texHeight, texWidth, texHeight);
        graphics.setColor(1f, 1f, 1f, 1f);
        popLocal(graphics);
    }
}
