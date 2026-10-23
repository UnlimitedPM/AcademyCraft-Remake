package cn.academy.terminal.client;

import cn.academy.AcademyCraft;
import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.ability.client.ClientAbilityData;
import cn.academy.ability.develop.condition.LearningCondition;
import cn.academy.client.gui.cgui.CGuiDocument;
import cn.academy.client.gui.cgui.CGuiShaders;
import cn.academy.client.gui.cgui.CGuiView;
import cn.academy.client.gui.cgui.CWidget;
import cn.academy.client.gui.cgui.DeveloperConsole;
import cn.academy.client.gui.cgui.GuiPageHost;
import cn.academy.client.gui.cgui.GuiSurface;
import cn.academy.client.gui.cgui.SkillTreeView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * L'application Arbre de competences : la page du developpeur, sans la machine.
 *
 * <p>C'est l'{@code SkillTreeAppUI} de l'original, et il ne s'en inventait pas une : il lisait la
 * <b>meme</b> page — {@code rework/page_developer}, quatre cents unites sur cent quatre-vingt-sept —
 * et appelait le meme {@code Common.initialize}, seulement sans developpeur. Tout ce qui reste ici
 * vient de la, dans cet ordre :
 *
 * <ul>
 *   <li>l'arbre a droite : chaque competence a sa place, reliee par un trait a celle dont elle
 *       depend, son icone au milieu d'un disque et son experience qui remplit l'anneau — c'est
 *       {@link SkillTreeView}, le meme que sur la machine ;</li>
 *   <li>le panneau de gauche : l'aptitude du joueur, son niveau et son experience ;</li>
 *   <li>et aucune machine : l'original retirait son panneau et changeait la bande de gauche pour
 *       {@code ui_developerleft_skilltree}, parce qu'une application ne tire rien d'un reseau.</li>
 * </ul>
 *
 * <h2>Ce que l'application ne fait pas</h2>
 *
 * <p>Elle n'apprend rien. L'original n'avait de bouton d'apprentissage que dans la machine — c'est
 * elle qui a l'energie, les stimulations et le droit d'enseigner. Une competence se consulte ici :
 * son nom, son experience, sa description, et ce qui manque encore pour l'apprendre.
 *
 * <p>Comme l'original, la page <b>ne met pas le jeu en pause</b> ({@code doesGuiPauseGame = false}).
 */
@OnlyIn(Dist.CLIENT)
public class SkillTreeScreen extends Screen {

    /** La page de l'original, en unites de conception. */
    private static final double PAGE_WIDTH = 400;
    private static final double PAGE_HEIGHT = 187;

    /** Le document, celui de la machine du developpeur : c'est le meme. */
    private static final String DOCUMENT = "rework/page_developer";

    /**
     * La bande de gauche de l'original quand il n'y a pas de machine.
     *
     * <p>Elle ne montre ni charge ni reseau : l'image le dit, et c'est pour cela qu'elle existe.
     */
    private static final String LEFT_IMAGE =
            "academy:textures/guis/ui/ui_developerleft_skilltree.png";

    /** La fiche d'une competence, aux tailles de l'original. */
    private static final String SURFACE_BACK = "academy:textures/guis/developer/skill_back.png";
    private static final double SURFACE_BACK_SIZE = 50.0;
    private static final double SURFACE_ICON_SIZE = 27.0;
    private static final double TITLE_FONT = 12.0;
    private static final double EXP_FONT = 8.0;
    private static final double TEXT_FONT = 10.0;
    private static final double DESC_FONT = 9.0;
    private static final double TEXT_WIDTH = 240.0;
    private static final double TEXT_HEIGHT = 12.0;
    private static final double TITLE_Y = 29.0;
    private static final double SUBTITLE_Y = 39.0;
    private static final double DESCRIPTION_Y = 49.0;
    private static final int DESCRIPTION_WIDTH = 200;

    /** La console : la largeur de ses lignes, leur hauteur, et ou la pile se pose. */
    private static final double CONSOLE_X = 5.0;
    private static final double CONSOLE_Y = 5.0;
    private static final double CONSOLE_LINE = 10.0;
    private static final double CONSOLE_FONT = 8.0;

    /** La console du joueur sans aptitude, et la boite qui la porte. */
    @Nullable
    private DeveloperConsole console;

    @Nullable
    private CWidget consoleBox;

    /**
     * La page, sa racine, et le moteur qui la dessine.
     *
     * <p>C'est un {@link CGuiView} et pas le rendu simple des documents : les noeuds de l'arbre et
     * leurs traits ne sont dessines que par lui (disque, cerne, anneau de progression, icone
     * eclipsee, trait qui pousse). Le rendu simple ne connait que les images, les textes et les
     * barres — l'arbre y serait invisible.
     */
    private final CWidget root = new CWidget("root");
    private final CWidget page = CGuiDocument.read(DOCUMENT).copy();

    @Nullable
    private CGuiView view;

    private int leftPos;
    private int topPos;

    /** Ce que l'arbre et la fiche demandent a cet ecran. */
    private final GuiPageHost host = new GuiPageHost() {
        @Override
        public CWidget page() {
            return page;
        }

        @Override
        public void onClick(CWidget widget, Runnable action) {
            if (view != null) view.onClick(widget, action);
        }

        @Override
        public void reflow() {
            place();
        }
    };

    private final SkillTreeView tree = new SkillTreeView(host);
    private final GuiSurface veil = new GuiSurface(host, PAGE_WIDTH, PAGE_HEIGHT);

    public SkillTreeScreen() {
        super(Component.translatable("ac.app.skill_tree.name"));
    }

    /**
     * La page prend toute la place qu'elle demande : c'est elle qui decide de la taille de l'ecran.
     *
     * <p>C'est exactement ce que fait la machine du developpeur, dont la page n'est pas un cadre de
     * conteneur mais l'ecran entier. Le document la pose deja centree — c'est son alignement — donc
     * il n'y a qu'a lui donner sa taille et son coin.
     */
    @Override
    protected void init() {
        super.init();

        root.transform.width = PAGE_WIDTH;
        root.transform.height = PAGE_HEIGHT;
        root.transform.x = 0;
        root.transform.y = 0;
        root.transform.scale = 1.0;
        root.transform.alignWidth = "LEFT";
        root.transform.alignHeight = "TOP";
        root.transform.doesDraw = true;

        root.children().clear();
        root.add(page);
        view = new CGuiView(this, root);

        leftPos = (int) Math.round((width - PAGE_WIDTH) / 2.0);
        topPos = (int) Math.round((height - PAGE_HEIGHT) / 2.0);

        build();
        place();
    }

    /** Pose la page au milieu de l'ecran, et tout ce qu'elle porte avec elle. */
    private void place() {
        if (view != null) view.place(leftPos, topPos, PAGE_WIDTH, PAGE_HEIGHT);
    }

    /**
     * Le dessin : le fond du jeu, la page, et rien d'autre.
     *
     * <p>Pas d'inventaire, pas d'infobulle : une application ne montre que ce que la page dit —
     * comme l'ecran du developpeur, qui n'affiche pas non plus l'inventaire du joueur.
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        place();
        frame(graphics, mouseX, mouseY);
        if (view != null) view.draw(graphics, mouseX, mouseY);
        drawConsole(graphics);
    }

    /**
     * Ce que l'ecran a a construire : le panneau de gauche, la bande qui remplace la machine, et
     * l'arbre de droite.
     */
    private void build() {
        CWidget page = host.page();
        if (page == null) return;

        buildAbilityPanel(page);
        buildMachinePanel(page);
        buildTree(page);
    }

    /**
     * Le panneau de gauche : l'aptitude du joueur, son niveau, son experience.
     *
     * <p>Il n'y a pas de bouton de montee de niveau : l'original ne le montrait qu'avec une machine
     * en main, et l'application n'en a pas. Le reste est celui de la machine — meme icone, meme nom,
     * memes chiffres — parce que c'est la meme page.
     */
    private void buildAbilityPanel(CWidget page) {
        CWidget panel = page.child("parent_left");
        CWidget ability = panel == null ? null : panel.child("panel_ability");
        if (ability == null) return;

        Category category = category();
        AbilityData data = ClientAbilityData.get();

        CWidget logo = ability.child("logo_ability");
        if (logo != null) {
            // L'original ne cachait pas l'icone : sans aptitude il montrait `icon_nocategory`.
            if (logo.texture == null) logo.texture = new CWidget.DrawTexture();
            logo.transform.doesDraw = true;
            logo.texture.texture = category == null
                    ? "academy:textures/guis/icons/icon_nocategory.png"
                    : categoryIcon(category);
        }

        CWidget name = ability.child("text_abilityname");
        if (name != null && name.text != null) {
            name.text.content = category == null
                    ? "N/A"
                    : Component.translatable(category.getDisplayKey()).getString();
        }

        CWidget level = ability.child("text_level");
        if (level != null && level.text != null) {
            level.text.content = Component.translatable("academy.ability.level"
                    + (category == null ? 0 : data.getCategoryLevel(category))).getString();
        }

        CWidget exp = ability.child("text_exp");
        if (exp != null && exp.text != null) {
            exp.text.content = "EXP " + (category == null ? 100
                    : (int) (data.getLevelProgress(category) * 100)) + "%";
        }

        CWidget progress = ability.child("logo_progress");
        if (progress != null && progress.bar != null) {
            progress.bar.progress = category == null ? 0.0
                    : Math.max(0.02, data.getLevelProgress(category));
        }

        // Le document porte le bouton : l'application ne l'a pas, et un bouton qu'on ne peut pas
        // cliquer serait un mensonge de plus.
        CWidget upgrade = ability.child("btn_upgrade");
        if (upgrade != null) upgrade.transform.doesDraw = false;
    }

    /**
     * La bande de gauche sans machine : l'image de l'original, et pas de panneau d'energie.
     *
     * <p>C'est exactement ce que l'original faisait avec un developpeur portable — et une
     * application EST un developpeur portable, sans machine du tout.
     */
    private void buildMachinePanel(CWidget page) {
        CWidget panel = page.child("parent_left");
        if (panel == null) return;

        CWidget machine = panel.child("panel_machine");
        if (machine != null) machine.transform.doesDraw = false;

        CWidget left = panel.child("ui_left");
        if (left != null && left.texture != null) left.texture.texture = LEFT_IMAGE;
    }

    /** La zone de droite : l'arbre, ou la console quand le joueur n'a encore aucune aptitude. */
    private void buildTree(CWidget page) {
        CWidget right = page.child("parent_right");
        CWidget area = right == null ? null : right.child("area");
        if (area == null) return;

        Category category = category();
        AbilityData data = ClientAbilityData.get();
        if (category == null) {
            buildConsole(area);
            tree.note(null, data);
            return;
        }

        console = null;
        consoleBox = null;
        tree.build(category, data, this::openSkill);
    }

    /**
     * La console : ce que voit un joueur qui n'a encore aucune aptitude.
     *
     * <p>Le meme texte que sur la machine, mot pour mot, et sans commande : c'est la machine qui
     * donne l'aptitude, pas l'application. La derniere ligne dit la commande, et elle marche ici
     * aussi — mais c'est {@code /learn} dans le chat, pas une frappe dans la console.
     *
     * <p>L'animation, elle, est la meme : c'est la console de l'original, qui ne savait pas sur quel
     * ecran elle se trouvait.
     */
    private void buildConsole(CWidget area) {
        area.children().clear();

        if (console == null) {
            String name = minecraft == null || minecraft.player == null
                    ? "" : minecraft.player.getGameProfile().getName();
            console = new DeveloperConsole(consoleTexts(), name, false, false,
                    new java.util.Random());
        }

        CWidget box = new CWidget("console");
        box.transform.width = area.transform.width;
        box.transform.height = area.transform.height;
        box.transform.x = 0;
        box.transform.y = 0;
        box.transform.alignWidth = "LEFT";
        box.transform.alignHeight = "TOP";
        area.add(box);
        consoleBox = box;
    }

    /** Les textes de la console, ceux de la machine. */
    private static DeveloperConsole.Texts consoleTexts() {
        return new DeveloperConsole.Texts(
                key("academy.skill_tree.console.init"),
                key("academy.skill_tree.console.boot_failed"),
                key("academy.skill_tree.console.invalid_cat"),
                key("academy.skill_tree.console.learn_hint"),
                key("academy.skill_tree.console.override"),
                key("academy.skill_tree.console.invalid_command"),
                percent -> Component.translatable("academy.skill_tree.console.progress",
                        DeveloperConsole.percent(percent)).getString(),
                key("academy.skill_tree.console.dev_begin"),
                key("academy.skill_tree.console.dev_succ"),
                key("academy.skill_tree.console.dev_fail"),
                key("academy.skill_tree.console.reset_begin"),
                key("academy.skill_tree.console.reset_succ"),
                key("academy.skill_tree.console.reset_fail"),
                key("academy.skill_tree.console.reset_fail_dev"),
                key("academy.skill_tree.console.reset_fail_other"));
    }

    private static String key(String translationKey) {
        return Component.translatable(translationKey).getString();
    }

    /**
     * Dessine la console, ligne par ligne, a la place de la zone de l'arbre.
     *
     * <p>Apres la page, comme sur la machine : {@code frame} passe avant le document, donc un texte
     * ecrit la serait recouvert par le fond.
     */
    private void drawConsole(GuiGraphics graphics) {
        if (console == null || consoleBox == null) return;

        console.tick(net.minecraft.Util.getMillis() / 1000.0);

        List<String> lines = console.lines();
        int x = (int) Math.round(consoleBox.absX + CONSOLE_X * consoleBox.absScale);
        int y = (int) Math.round(consoleBox.absY + CONSOLE_Y * consoleBox.absScale);
        int cell = cn.academy.client.gui.AcademyFonts.cell(CONSOLE_FONT * consoleBox.absScale);
        int step = (int) Math.round(CONSOLE_LINE * consoleBox.absScale);

        for (int i = 0; i < lines.size(); i++) {
            cn.academy.client.gui.AcademyFonts.drawTop(graphics, font, lines.get(i),
                    x, y + i * step, 0xFFFFFFFF, cell);
        }
    }

    // ------------------------------------------------------------------
    // Ce qui bouge, et ce qui se lit
    // ------------------------------------------------------------------

    /**
     * Ce qui change avant chaque dessin : la page rebatie si l'aptitude a bouge, la fiche, et
     * l'arbre.
     */
    private void frame(GuiGraphics graphics, double mouseX, double mouseY) {
        Category category = category();
        AbilityData data = ClientAbilityData.get();
        if (!tree.unchanged(category, data)) {
            build();
            place();
        }

        veil.animate();
        tree.frame(ClientAbilityData.get(), mouseX, mouseY, width, height, veil.opened());
    }

    /** La fiche d'une competence : son icone, son nom, son experience ou ce qui lui manque. */
    private void openSkill(Skill skill) {
        AbilityData data = ClientAbilityData.get();
        boolean learned = data.isSkillLearned(skill);

        CWidget content = new CWidget("skill");
        content.add(surfaceIcon(skill));

        content.add(text(skillName(skill), TITLE_Y, 255, 255, 255, TITLE_FONT));
        if (learned) {
            content.add(text(Component.translatable("academy.developer.skill_exp",
                    (int) (data.getSkillExp(skill) * 100)).getString(), SUBTITLE_Y, 161, 225, 255,
                    EXP_FONT));
        } else {
            content.add(text(Component.translatable("academy.developer.not_learned").getString(),
                    SUBTITLE_Y + 1, 255, 85, 85, TEXT_FONT));
        }

        double y = DESCRIPTION_Y;
        for (String line : wrap(skill.getDescription().getString(), DESCRIPTION_WIDTH)) {
            content.add(text(line, y, 255, 255, 255, DESC_FONT));
            y += 10;
        }

        if (!learned) {
            // Ce qui manque pour l'apprendre, dit par la condition elle-meme : c'est ce qui envoie
            // le joueur vers la machine avec une raison plutot qu'avec une envie.
            y += 6;
            for (LearningCondition condition : skill.getConditions()) {
                if (condition.accepts(data, skill, null)) continue;
                content.add(text(condition.describe(skill).getString(), y, 255, 88, 88, DESC_FONT));
                y += 11;
            }
        }

        veil.open(content);
    }

    /**
     * Un clic que rien n'a pris referme la fiche ouverte.
     *
     * <p>C'est la regle de l'original : son voile couvrait tout l'ecran et se refermait au premier
     * clic, ou qu'il tombe.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (view != null && view.click(mouseX, mouseY, button)) return true;
        if (veil.opened() && button == 0) {
            veil.beginClose();
            return true;
        }
        return false;
    }

    /** Une competence se consulte dans l'application ; elle s'apprend sur la machine. */
    private static String skillName(Skill skill) {
        return Component.translatable(skill.getDisplayKey()).getString()
                + " (LV " + skill.getLevel() + ")";
    }

    /** L'aptitude du joueur : la plus avancee, comme sur la machine. */
    @Nullable
    private Category category() {
        return ClientAbilityData.get().getHighestCategory();
    }

    /** L'icone d'une categorie : celle de ses pages, sous {@code guis/icons}. */
    private static String categoryIcon(Category category) {
        return AcademyCraft.MOD_ID + ":textures/guis/icons/icon_" + category.getName() + ".png";
    }

    // ------------------------------------------------------------------
    // Les morceaux de la fiche
    // ------------------------------------------------------------------

    /** Le disque d'une fiche : le fond, et l'icone de la competence par-dessus. */
    private static CWidget surfaceIcon(Skill skill) {
        CWidget icon = centred("icon", SURFACE_BACK_SIZE, SURFACE_BACK_SIZE);
        icon.texture = new CWidget.DrawTexture();
        icon.texture.texture = SURFACE_BACK;

        CWidget glyph = centred("glyph", SURFACE_ICON_SIZE, SURFACE_ICON_SIZE);
        glyph.texture = new CWidget.DrawTexture();
        glyph.texture.texture = skill.getHintIcon().toString();
        // L'icone d'une competence fait trente-deux pixels pour un dessin de vingt-sept unites :
        // lissee a l'echelle de l'ecran, sinon elle sort en gros pixels.
        CGuiShaders.linear(skill.getHintIcon());
        icon.add(glyph);
        return icon;
    }

    /**
     * Une ligne de la fiche, centree sur la page.
     *
     * <p>Sa boite n'est <b>pas</b> vide : le moteur n'ecrit un texte que dans une boite qui a une
     * taille, et une boite de zero sur zero ne dessine rien du tout.
     */
    private static CWidget text(String content, double y, int red, int green, int blue,
                                double size) {
        CWidget box = new CWidget("text");
        box.transform.width = TEXT_WIDTH;
        box.transform.height = Math.max(TEXT_HEIGHT, size + 2);
        box.transform.y = y;
        box.transform.alignWidth = "CENTER";
        box.transform.alignHeight = "CENTER";
        box.text = new CWidget.TextBox();
        box.text.content = content;
        box.text.fontSize = size;
        box.text.align = "CENTER";
        box.text.heightAlign = "CENTER";
        box.text.color(red, green, blue);
        return box;
    }

    private static CWidget centred(String name, double width, double height) {
        CWidget widget = new CWidget(name);
        widget.transform.width = width;
        widget.transform.height = height;
        widget.transform.alignWidth = "CENTER";
        widget.transform.alignHeight = "CENTER";
        return widget;
    }

    /** Coupe une phrase en lignes qui tiennent dans la largeur donnee, sur les espaces. */
    private List<String> wrap(String content, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : content.split(" ")) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (current.length() > 0
                    && cn.academy.client.gui.AcademyFonts.width(font, candidate, (int) DESC_FONT)
                            > maxWidth) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }

    /** Le clavier va d'abord a la page : c'est elle qui connait ses champs. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (view != null && view.keyPressed(keyCode)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * La page ne met pas le jeu en pause.
     *
     * <p>C'est le {@code doesGuiPauseGame = false} de l'original, pose sur les deux ecrans qui
     * montrent cet arbre : on regarde ses competences sans arreter la partie.
     */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Fermer l'application rend le terminal, qui n'a jamais ete ferme. */
    @Override
    public void onClose() {
        AppScreens.close();
    }
}
