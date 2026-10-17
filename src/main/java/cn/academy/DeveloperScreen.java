package cn.academy;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.ability.client.ClientAbilityData;
import cn.academy.ability.develop.DevelopActionLevel;
import cn.academy.ability.develop.LearningHelper;
import cn.academy.ability.develop.condition.LearningCondition;
import cn.academy.client.gui.cgui.CGuiContainerScreen;
import cn.academy.client.gui.cgui.CGuiShaders;
import cn.academy.client.gui.cgui.CWidget;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * L'ecran du developpeur d'aptitudes : l'arbre des competences de l'original.
 *
 * <h2>Ce qui a change, et pourquoi</h2>
 *
 * Le port avait un ecran a lui : la liste plate des categories, une ligne par competence, un bouton
 * pour apprendre. Il apprenait, il marchait, et il ne ressemblait a rien de ce que l'original montre
 * — la ou l'original dessine un <b>arbre</b> : chaque competence a sa place, reliee par un trait a
 * celle dont elle depend, son icone au milieu d'un disque, et son experience qui remplira un anneau
 * autour d'elle.
 *
 * <p>Cet ecran-ci lit la page de l'original telle quelle — {@code page_developer}, quatre cents
 * unites de large sur cent quatre-vingt-sept — et n'invente donc plus sa mise en page.
 *
 * <h2>Pas d'inventaire, et c'est voulu</h2>
 *
 * L'original ne montre pas l'inventaire du joueur sur cet ecran, et le port fait pareil : la page
 * occupe toute la place, on n'y range rien, on n'y depose rien. Le conteneur reste pourtant un
 * conteneur — c'est lui qui porte l'energie, l'etat de l'apprentissage et les boutons, par le chemin
 * vanilla des boutons de menu, sans un paquet a nous — mais l'ecran ne s'en sert que pour lire.
 *
 * <h2>L'arbre se dessine au fil du temps</h2>
 *
 * Chaque noeud apparait en fondu, decale de son rang, et le trait qui le relie a son parent pousse du
 * noeud vers lui. La souris ecarte tout l'arbre d'un dixieme de la distance qui la separe du centre,
 * ce qui donne au fond un relief de quelques unites. Rien n'est fige a l'ouverture : tout se
 * recalcule a chaque image.
 *
 * <h2>Ce qui reste a faire</h2>
 *
 * L'anneau d'experience d'une competence apprise, et l'icone grisee de celles qui ne le sont pas : les
 * deux sont l'oeuvre de deux petits programmes d'origine, dont le port n'a pas encore l'emploi. En
 * attendant, un noeud montre son disque, son cerne et son icone — et la progression se lit sur les
 * barres des deux panneaux de gauche.
 */
public class DeveloperScreen extends CGuiContainerScreen<DeveloperMenu> {

    /** La page de l'original, en unites de conception. */
    private static final double PAGE_WIDTH = 400;
    private static final double PAGE_HEIGHT = 187;

    /** La page, et les trois morceaux du document que l'ecran modifie. */
    private static final String PAGE = "dev";
    private static final String AREA = "parent_right/area";
    private static final String ABILITY_PANEL = "parent_left/panel_ability";
    private static final String MACHINE_PANEL = "parent_left/panel_machine";

    /**
     * La boite d'un noeud, et le trait qui le relie a son parent.
     *
     * <p>Le trait s'arrete a {@code 12,2} du centre de chaque noeud alors que le disque en fait
     * {@code 11,5} : il ne mord donc jamais dessus, et c'est pour cela que l'ordre de leur dessin ne
     * se voit pas.
     */
    private static final double NODE_BOX = 16.0;
    private static final double LINK_INSET = 12.2;
    private static final double LINK_WIDTH = 5.5;
    private static final double LINK_FADE = 5.0;

    /**
     * Ce qui reste a l'opacite d'un trait quand la competence n'est pas apprise : quatre dixiemes.
     *
     * <p>C'est le facteur de l'original — {@code mAlpha * (if (learned) 1.0 else 0.4)} — et il
     * manquait au port : un trait se dessinait a l'opacite de son noeud, deux fois et demie trop
     * clair des que la competence n'etait pas apprise. Le joueur a compare les deux versions cote a
     * cote : « la luminosite des elements n'est pas la meme pour les 2 ».
     */
    static final double LINK_UNLEARNED = 0.4;

    /** L'apparition : un fondu par noeud, decale de son rang de huit centiemes de seconde. */
    private static final double APPEAR_STEP = 0.08;
    private static final double APPEAR_SPEED = 10.0;

    /** Le survol : un cinquieme de plus, atteint en un dixieme de seconde. */
    private static final double HOVER_SCALE = 1.2;
    private static final double HOVER_TRANSIT = 0.1;

    /** Le relief : tout l'arbre se decale de dix unites au plus, en sens inverse de la souris. */
    private static final double PARALLAX = 10.0;

    /**
     * Le fond de la zone de l'arbre.
     *
     * <p>L'image se dessine un centieme plus grande que la zone, et c'est <b>ce centieme</b> qui la fait
     * glisser : le relief de l'arbre, c'est le rapport entre ces quelques dixiemes d'unite et les dix
     * unites que parcourent les noeuds.
     */
    private static final String AREA_BACKDROP =
            "academy:textures/guis/effect/effect_developer_background.png";
    private static final double BACKDROP_SLIDE = 0.01;
    private static final double BACKDROP_WINDOW = 1.0 / 1.01;

    /** La surface : un voile noir, ouvert et referme en un cinquieme de seconde. */
    private static final int VEIL_ALPHA = 178;
    private static final double VEIL_FADE = 0.2;

    /** Le bouton de l'original : son image, sa taille dessinee, et ses deux teintes. */
    private static final String BUTTON_TEXTURE = "academy:textures/guis/developer/button.png";
    private static final double BUTTON_WIDTH = 64.0;
    private static final double BUTTON_HEIGHT = 32.0;
    private static final double BUTTON_SCALE = 0.5;
    private static final double BUTTON_Y = 30.0;

    /** La largeur dans laquelle la description d'une competence se coupe : celle de l'original. */
    private static final int DESCRIPTION_WIDTH = 200;

    /**
     * La boite d'une ligne de texte libre.
     *
     * <p>Elle n'est pas vide, et c'est tout son interet : le moteur n'ecrit un texte que dans une
     * boite qui a une taille. Toutes les lignes des surfaces — le nom d'une competence, son
     * experience, ses conditions, son cout — etaient posees dans des boites de zero sur zero, et
     * n'etaient donc <b>jamais dessinees</b>.
     */
    private static final double TEXT_WIDTH = 240.0;
    private static final double TEXT_HEIGHT = 12.0;

    /**
     * Les tailles de la fiche d'une competence, celles de l'original.
     *
     * <p>Il les avait fixees une par une — {@code foSkillTitle} a douze, {@code foSkillProg} a huit,
     * {@code foSkillDesc} a neuf, {@code foSkillUnlearned} a dix — et le port les ecrivait toutes a
     * dix, la taille par defaut du moteur. Le joueur : « le texte n'est pas bien centre, de la bonne
     * couleur et de la bonne taille ».
     */
    private static final double TEXT_FONT = 10.0;
    private static final double TITLE_FONT = 12.0;
    private static final double EXP_FONT = 8.0;
    private static final double DESC_FONT = 9.0;

    /**
     * Ou se posent les lignes de la fiche, en unites sous l'icone.
     *
     * <p>L'original posait son bloc de texte vingt-cinq unites sous l'icone, puis chaque ligne a
     * trois, quinze et vingt-quatre de ce bloc — soit vingt-huit, quarante et quarante-neuf. Le port
     * les posait AU-DESSUS de l'icone, a moins quarante-six : le joueur voyait le nom et la
     * description au-dessus du dessin, alors que l'original les met dessous.
     */
    private static final double TITLE_Y = 29.0;
    private static final double SUBTITLE_Y = 39.0;
    private static final double DESCRIPTION_Y = 49.0;

    /** La console : la largeur de ses lignes, leur hauteur, et ou la pile se pose. */
    private static final double CONSOLE_WIDTH = 240.0;
    private static final double CONSOLE_LINE = 10.0;
    private static final double CONSOLE_DY = -22.0;

    /** L'icone d'une surface : le meme disque, mais plus grand. */
    private static final String SURFACE_BACK = "academy:textures/guis/developer/skill_back.png";
    private static final double SURFACE_BACK_SIZE = 50.0;
    private static final double SURFACE_ICON_SIZE = 27.0;

    /**
     * Un noeud de l'arbre, et de quoi l'animer.
     *
     * <p>{@code dim} est l'opacite que l'original donne a une competence selon son etat : pleine si
     * elle est apprise, sept dixiemes si celle dont elle depend l'est — elle est alors a portee — et
     * un quart sinon, c'est-a-dire a peine visible. Toute la progression se lit la, sans un mot.
     */
    private static final class Node {
        final Skill skill;
        final CWidget widget;
        final boolean learned;
        final double dim;
        final double born;
        boolean hovered;
        double since;

        Node(Skill skill, CWidget widget, boolean learned, double dim, double born) {
            this.skill = skill;
            this.widget = widget;
            this.learned = learned;
            this.dim = dim;
            this.born = born;
            this.since = born;
        }
    }

    private final List<Node> nodes = new ArrayList<>();

    /** Ce que l'arbre montre, sous forme de phrase comparable : voir {@link #signature()}. */
    private String signature = "";

    /** L'instant d'ouverture, en secondes : c'est l'origine de tous les fondus. */
    private double opened;

    /** La surface ouverte, s'il y en a une, et son propre fondu. */
    @Nullable
    private CWidget surface;
    private double surfaceSince;
    private boolean surfaceClosing;

    public DeveloperScreen(DeveloperMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        // La page de l'original n'est pas un cadre de machine : elle fait quatre cents unites de large,
        // et c'est elle qui decide de la taille de l'ecran.
        this.imageWidth = (int) PAGE_WIDTH;
        this.imageHeight = (int) PAGE_HEIGHT;
    }

    /** La page occupe toute la place : elle ne se pose pas au milieu d'un cadre de machine. */
    @Override
    protected void layoutRoot(CWidget root) {
        root.transform.width = PAGE_WIDTH;
        root.transform.height = PAGE_HEIGHT;
        root.transform.x = 0;
        root.transform.y = 0;
        root.transform.scale = 1.0;
        root.transform.alignWidth = "LEFT";
        root.transform.alignHeight = "TOP";
        root.transform.doesDraw = true;
    }

    /**
     * Les fonds de cette page ne respirent pas.
     *
     * <p>L'original faisait osciller l'opacite des couches nommees {@code ui_} sur ses ecrans de
     * machine ; son ecran de developpement, lui, n'installait pas ce souffle et dessinait donc sa page
     * en plein. Le port le lui appliquait quand meme, et tout ce qui est blanc sur cette page sortait
     * a 182 sur 255 — le grand carre du logo, le cadre du panneau, le logo « Academy tech ». Mesure
     * sur les deux captures du joueur : 182 chez nous, 255 dans la 1.12.2, pour exactement le meme
     * nombre de pixels. Le joueur : « l'image blanche dans la machine en haut a gauche par exemple qui
     * est tres sombre par rapport au vrai ».
     */
    @Override
    protected boolean breathes() {
        return false;
    }

    /** Un onglet de page se poserait a gauche du cadre, hors de la page : il n'y en a pas. */
    @Override
    protected boolean showsTabs() {
        return false;
    }

    /** Et le panneau d'infos se poserait dessus : la page dit deja tout ce qu'elle a a dire. */
    @Override
    protected boolean showsInfo() {
        return false;
    }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    /**
     * La page, ses deux panneaux de gauche, et l'arbre.
     *
     * <p>Elle est construite ici, dans le {@code build} du conteneur, et c'est le seul endroit ou elle
     * peut l'etre : c'est la que les pages naissent, avant que l'ecran ne montre la premiere. La
     * Developer ne rebatit jamais son panneau — elle n'en a pas — donc la reconstruction qui
     * retirerait les pages nees apres coup ne la concerne pas.
     */
    @Override
    protected void build(CGuiContainerScreen<DeveloperMenu> screen) {
        CWidget page = page(PAGE, "rework/page_developer");
        page.transform.width = PAGE_WIDTH;
        page.transform.height = PAGE_HEIGHT;
        page.transform.x = 0;
        page.transform.y = 0;
        page.transform.alignWidth = "LEFT";
        page.transform.alignHeight = "TOP";

        opened = now();
        buildAbilityPanel(page);
        buildMachinePanel(page);
        buildTree(page);

        // La bobine magnetique change l'ecran : l'original ne montrait plus l'arbre du tout et laissait
        // la place a la console du changement d'aptitude. Le port pose la meme question, sous forme de
        // surface, et c'est ce que le joueur tient en main qui decide — comme chez l'original.
        if (holdsCoil()) openReset();
    }

    /** La bobine magnetique en main : c'est elle qui demande a changer d'aptitude. */
    private boolean holdsCoil() {
        return minecraft != null && minecraft.player != null
                && minecraft.player.getMainHandItem().is(ModItems.MAGNETIC_COIL.get());
    }

    /**
     * Le panneau de gauche : l'aptitude du joueur, son niveau, son experience.
     *
     * <p>La barre dit l'avancement du niveau en cours — celle qui ne monte qu'en se servant de ses
     * aptitudes — et le bouton de droite ne se montre que quand le palier est plein et que la machine
     * sait enseigner la suite.
     */
    private void buildAbilityPanel(CWidget page) {
        CWidget panel = page.child("parent_left");
        CWidget ability = panel == null ? null : panel.child("panel_ability");
        if (ability == null) return;

        Category category = category();
        AbilityData data = ClientAbilityData.get();

        CWidget logo = ability.child("logo_ability");
        if (logo != null) {
            // Sans aptitude, il n'y a pas d'icone a montrer — et une texture absente se dessine en
            // rectangle BLANC. Le joueur l'a vu et l'a dit : « l'image qui montre quel pouvoir on a
            // est un carre blanc quand on n'a pas de pouvoir ».
            logo.transform.doesDraw = category != null;
            if (logo.texture == null) logo.texture = new CWidget.DrawTexture();
            logo.texture.texture = category == null ? null : categoryIcon(category);
        }

        CWidget name = ability.child("text_abilityname");
        if (name != null && name.text != null) {
            // « N/A » ne disait rien a personne, et le joueur a reclame le bon texte : sans
            // aptitude, on le dit, au lieu d'ecrire une abreviation d'anglais de bureau.
            name.text.content = category == null
                    ? Component.translatable("academy.developer.no_ability").getString()
                    : Component.translatable(category.getDisplayKey()).getString();
        }

        CWidget level = ability.child("text_level");
        if (level != null && level.text != null) {
            level.text.content = category == null ? ""
                    : Component.translatable("academy.developer.level",
                            data.getCategoryLevel(category)).getString();
        }

        // Le pourcentage d'experience. Le document porte « EXP 30% » : un texte d'exemple, que l'ecran
        // doit remplacer — sinon il reste la, et c'est exactement ce que le joueur a vu : « la machine
        // montre comme quoi je suis a 30% de mon niveau alors que dans le menu F4 je ne suis qu'a 3% ».
        // Le chiffre est celui de la barre juste au-dessus, donc de l'avancement du niveau en cours.
        CWidget exp = ability.child("text_exp");
        if (exp != null && exp.text != null) {
            exp.text.content = category == null ? ""
                    : "EXP " + (int) (data.getLevelProgress(category) * 100) + "%";
        }

        CWidget progress = ability.child("logo_progress");
        if (progress != null && progress.bar != null) {
            // L'original ne descend jamais jusqu'a zero : une barre vide garde un cheveu de
            // remplissage, et c'est ce qui la rend lisible. C'est son minimum de deux centiemes.
            progress.bar.progress = category == null ? 0.0
                    : Math.max(0.02, data.getLevelProgress(category));
        }

        CWidget upgrade = ability.child("btn_upgrade");
        if (upgrade != null) {
            // Le bouton ne se montre QUE quand la prochaine marche est a portee : pas au niveau
            // maximal — il n'y a plus rien a monter — et pas non plus tant que le palier n'est pas
            // plein. C'est ce que le joueur a decrit : « quand je suis niveau 5, je ne devrais pas voir
            // le bouton LEARN [...] et pour les niveaux inferieurs, je dois le voir uniquement quand je
            // suis a 100 % ». Le palier plein est demande a {@code AbilityData}, qui porte la meme regle
            // que celle que le serveur appliquera au clic.
            // Sans aptitude, ce meme bouton est celui qui en donne une : c'est l'action de montee de
            // niveau de l'original, qui choisissait la categorie quand il n'y en avait pas encore.
            boolean first = category == null;
            boolean open = first || (data.getCategoryLevel(category) < DevelopActionLevel.MAX_LEVEL
                    && data.canLevelUp(category));
            upgrade.transform.doesDraw = open;
            if (open) {
                onClick(upgrade, first
                        ? () -> click(DeveloperMenu.FIRST_CATEGORY_BUTTON)
                        : this::openLevel);
            }
        }
    }

    /**
     * Le panneau de la machine : sa charge, et la part du reseau qu'elle accepte.
     *
     * <p>Un developpeur portable n'en a pas : il ne tire rien d'un reseau, donc son panneau se cache et
     * la bande de gauche change d'image — c'est ce que fait l'original, et l'image
     * {@code ui_developerleft_skilltree} n'existe que pour cela.
     *
     * <p>La ligne du noeud sans fil reste cachee : le nom du noeud auquel la machine est reliee vit sur
     * le serveur, et rien ne le fait encore descendre jusqu'ici. Mieux vaut ne rien montrer qu'un nom
     * faux.
     */
    private void buildMachinePanel(CWidget page) {
        CWidget panel = page.child("parent_left");
        CWidget machine = panel == null ? null : panel.child("panel_machine");
        if (machine == null) return;

        // « Current Node: » : la ligne du noeud, celle que l'original dessinait sous la barre
        // d'energie. Le port la cachait entierement, si bien que le joueur ne savait pas a quoi sa
        // machine etait raccordee â€” il l'a reclamee deux fois. Le libelle est dans la page ; c'est
        // le nom du noeud, connu du seul serveur, qui manquait.
        CWidget wireless = machine.child("button_wireless");
        CWidget node = wireless == null ? null : wireless.child("text_nodename");
        if (node != null && node.text != null) {
            String name = menu.getNodeName();
            node.text.content = name.isEmpty()
                    ? Component.translatable("academy.developer.not_connected").getString()
                    : name;
        }

        if (menu.isPortable()) {
            machine.transform.doesDraw = false;
            CWidget left = panel.child("ui_left");
            if (left != null && left.texture != null) {
                left.texture.texture = "academy:textures/guis/ui/ui_developerleft_skilltree.png";
            }
            return;
        }

        CWidget power = machine.child("progress_power");
        if (power != null && power.bar != null) {
            int max = Math.max(1, menu.getMaxEnergyStored());
            power.bar.progress = Math.max(0.0,
                    Math.min(1.0, (double) menu.getEnergyStored() / max));
        }
        CWidget rate = machine.child("progress_syncrate");
        if (rate != null && rate.bar != null) {
            rate.bar.progress = menu.getDeveloperType().getSyncRate();
        }
    }

    /**
     * La console : ce que voit un joueur qui n'a encore aucune aptitude.
     *
     * <p>L'original ne montrait pas un arbre vide a ce moment-la : il posait une petite console a la
     * place de l'arbre — « Welcome to Academy OS », l'echec du demarrage, le verdict, et la commande
     * a taper. Le port garde son texte mot pour mot et remplace la frappe par un bouton, parce qu'un
     * bouton se clique — et la commande existe aussi pour de vrai, c'est {@code /learn}.
     */
    private void buildConsole(CWidget area) {
        area.children().clear();
        // La console dit aussi ce qu'elle montre : sans cela, `refresh` la rebatirait a chaque image.
        signature = signature();

        List<String> lines = consoleText();
        double top = CONSOLE_DY - (lines.size() - 1) * CONSOLE_LINE / 2.0;
        for (int i = 0; i < lines.size(); i++) {
            area.add(consoleLine(lines.get(i), top + i * CONSOLE_LINE));
        }

        double below = top + lines.size() * CONSOLE_LINE + 6;
        if (menu.isDeveloping()) {
            area.add(text(Component.translatable("academy.developer.state.developing").getString(),
                    below, 255, 216, 0));
            return;
        }
        area.add(consoleButton(() -> click(DeveloperMenu.FIRST_CATEGORY_BUTTON), below));
    }

    /**
     * Le texte de la console, concatene comme l'original le concatene.
     *
     * <p>Quatre cles, celles de l'original, et dans son ordre : le demarrage, l'echec qui se colle a
     * la fin de la meme ligne, le verdict, et la commande a taper. L'invite {@code OS >} ferme la
     * console — c'est la que l'original attendait sa frappe.
     */
    private List<String> consoleText() {
        String name = minecraft == null || minecraft.player == null
                ? "" : minecraft.player.getGameProfile().getName();
        String all = Component.translatable("academy.skill_tree.console.init", name).getString()
                + Component.translatable("academy.skill_tree.console.boot_failed").getString()
                + Component.translatable("academy.skill_tree.console.invalid_cat").getString()
                + Component.translatable("academy.skill_tree.console.learn_hint").getString()
                + "OS > ";
        return List.of(all.split("\n", -1));
    }

    /** Une ligne de la console, posee a gauche dans sa boite. */
    private static CWidget consoleLine(String content, double y) {
        CWidget line = new CWidget("console_line");
        line.transform.width = CONSOLE_WIDTH;
        line.transform.height = CONSOLE_LINE;
        line.transform.y = y;
        line.transform.alignWidth = "CENTER";
        line.transform.alignHeight = "CENTER";
        line.text = new CWidget.TextBox();
        line.text.content = content;
        line.text.align = "LEFT";
        line.text.heightAlign = "CENTER";
        return line;
    }

    /**
     * Le bouton de la console : la meme image que celui des surfaces, son etiquette, et rien a
     * fermer — il n'y a aucune surface ouverte sous lui.
     */
    private CWidget consoleButton(Runnable action, double y) {
        CWidget widget = button(action, y, false);
        widget.add(text("LEARN", 0, 255, 255, 255));
        return widget;
    }

    /**
     * L'arbre : un noeud par competence, et un trait par parente.
     *
     * <p>Toutes les competences de la categorie ne s'y trouvent pas. Une competence n'apparait que si
     * elle vaut la peine d'etre montree — si le joueur a le niveau pour elle, s'il l'a deja apprise, ou
     * si celle dont elle depend est apprise. C'est {@link LearningHelper} qui en decide, et l'original
     * s'en servait exactement de meme : l'arbre s'ouvre au fur et a mesure.
     */
    private void buildTree(CWidget page) {
        // La zone se prend par la PAGE, et non par `widget("area")` : celui-ci passe par la vue, qui
        // n'existe pas encore quand la page se construit. Le joueur voyait donc, a la place de la
        // console, le vieux message « ask a developer for one » — la console ne s'affichait jamais.
        CWidget right = page.child("parent_right");
        CWidget area = right == null ? null : right.child("area");
        if (area == null) return;

        // Sans aucune aptitude, il n'y a pas d'arbre a montrer : la console prend la place, et
        // c'est elle qui dit au joueur comment entrer dans le mod. Le passage se fait tout seul
        // une fois la premiere aptitude apprise, puisque l'ecran rebatit des que l'aptitude change.
        if (category() == null) {
            buildConsole(area);
            return;
        }
        nodes.clear();

        area.children().clear();

        // Le fond de la zone, sur la zone elle-meme : voir {@link #drawAreaBackdrop} pour ce qu'il a
        // fallu comprendre avant de le mettre la.
        if (area.texture == null) area.texture = new CWidget.DrawTexture();
        area.texture.texture = AREA_BACKDROP;
        area.texture.u0 = slide(0);
        area.texture.v0 = slide(0);
        area.texture.u1 = area.texture.u0 + BACKDROP_WINDOW;
        area.texture.v1 = area.texture.v0 + BACKDROP_WINDOW;

        Category category = category();
        if (category == null) {
            area.add(message("academy.developer.no_category"));
            signature = signature();
            return;
        }

        AbilityData data = ClientAbilityData.get();
        int index = 0;
        for (Skill skill : category.getSkills()) {
            if (!LearningHelper.canBePotentiallyLearned(data, skill)) continue;

            boolean learned = data.isSkillLearned(skill);
            Node node = new Node(skill, node(skill.getName()), learned,
                    dim(data, skill, learned), APPEAR_STEP * index);
            area.add(node.widget);
            onClick(node.widget, () -> openSkill(skill));

            // Le trait vers le parent : il part du noeud et vise le centre de l'autre, en s'arretant a
            // douze unites et demie de chacun des deux.
            Skill parent = skill.getParent();
            if (parent != null) node.widget.line = link(skill, parent);

            nodes.add(node);
            index++;
        }

        // Le trait connait le noeud d'ou il part : son survol ecarte le premier bout, comme celui du
        // noeud qui le porte ecarte le second. Sans cela, le disque grossi passait par-dessus le bout
        // et le joueur voyait le trait au travers du cerne de progression.
        Map<Skill, CWidget> bySkill = new HashMap<>();
        for (Node node : nodes) bySkill.put(node.skill, node.widget);
        for (Node node : nodes) {
            if (node.widget.line == null) continue;
            Skill parent = node.skill.getParent();
            node.widget.line.source = parent == null ? null : bySkill.get(parent);
        }

        signature = signature();
    }

    /** L'opacite d'un noeud : voir {@link Node}. */
    private static double dim(AbilityData data, Skill skill, boolean learned) {
        if (learned) return 1.0;
        Skill parent = skill.getParent();
        return parent == null || data.isSkillLearned(parent) ? 0.7 : 0.25;
    }

    private static CWidget node(String name) {
        CWidget node = new CWidget("skill_" + name);
        node.transform.width = NODE_BOX;
        node.transform.height = NODE_BOX;
        node.transform.alignWidth = "LEFT";
        node.transform.alignHeight = "TOP";
        node.transform.scale = 1.0;
        node.disc = new CWidget.Disc();
        return node;
    }

    /**
     * Le trait qui relie une competence a celle dont elle depend, dans les unites de son noeud.
     *
     * <p>Le repere est celui de la boite du noeud : son centre est donc a huit unites, et le vecteur
     * vers le parent se lit directement dans les coordonnees de l'arbre — celles que les classes de
     * categorie ont reprises de l'original. Les deux bouts reculent de {@code 12,2} le long de ce
     * vecteur, et c'est ce qui laisse les deux disques degages.
     */
    private static CWidget.Line link(Skill skill, Skill parent) {
        double dx = parent.getGuiX() - skill.getGuiX();
        double dy = parent.getGuiY() - skill.getGuiY();
        double norm = Math.sqrt(dx * dx + dy * dy);
        double half = NODE_BOX / 2;

        CWidget.Line line = new CWidget.Line();
        line.thickness = LINK_WIDTH;
        line.progress = 0;
        line.x0 = half;
        line.y0 = half;
        line.x1 = half;
        line.y1 = half;
        if (norm <= 0) return line;

        double ux = dx / norm * LINK_INSET;
        double uy = dy / norm * LINK_INSET;
        line.x0 = dx + half - ux;
        line.y0 = dy + half - uy;
        line.x1 = half + ux;
        line.y1 = half + uy;
        return line;
    }

    /** Le peu qui se dit quand le joueur n'a encore aucune aptitude. */
    private static CWidget message(String key) {
        CWidget box = new CWidget("message");
        box.transform.width = 200;
        box.transform.height = 12;
        box.transform.alignWidth = "CENTER";
        box.transform.alignHeight = "CENTER";
        box.text = new CWidget.TextBox();
        box.text.content = Component.translatable(key).getString();
        box.text.align = "CENTER";
        box.text.heightAlign = "CENTER";
        return box;
    }

    /** L'icone d'une categorie : celle de ses pages, sous {@code guis/icons}. */
    private static String categoryIcon(Category category) {
        return AcademyCraft.MOD_ID + ":textures/guis/icons/icon_" + category.getName() + ".png";
    }

    /**
     * L'aptitude du joueur.
     *
     * <p>L'original n'en connaissait qu'une : le joueur choisissait son pouvoir, et l'arbre etait le
     * sien. Le port laisse un joueur connaitre plusieurs categories, donc il faut bien en designer une
     * — c'est la plus avancee, celle ou il a le plus monte, et c'est celle que l'ecran montre. Un
     * joueur qui n'en a aucune voit la phrase qui le dit.
     */
    @Nullable
    private Category category() {
        return ClientAbilityData.get().getHighestCategory();
    }

    // ------------------------------------------------------------------
    // Animation
    // ------------------------------------------------------------------

    /**
     * Ce qui bouge a chaque image : le relief, le survol, les fondus, et le trait qui pousse.
     *
     * <p>Les positions se posent ici, apres que le placement les a lues — les noeuds sont donc dessines
     * avec la place de l'image precedente, en retard d'une image. Cela ne se voit pas : un soixantieme
     * de seconde, sur un deplacement de dix unites au total. L'echelle du survol, elle, se lit au
     * dessin et s'applique donc tout de suite.
     */
    @Override
    protected void frame(GuiGraphics graphics, double mouseX, double mouseY) {
        refresh();
        animateSurface();
        if (nodes.isEmpty()) return;

        double time = now();
        // Le relief se lit sur l'ecran entier, pas sur la zone de l'arbre : c'est ce que faisait
        // l'original, et c'est ce qui fait que le fond bouge meme quand la souris est sur le panneau.
        double dx = clamp01(mouseX / Math.max(1, width)) - 0.5;
        double dy = clamp01(mouseY / Math.max(1, height)) - 0.5;

        drawAreaBackdrop(dx, dy);
        if (nodes.isEmpty()) return;

        // Le survol ne compte pas tant qu'une surface est ouverte : elle est modale, et un noeud ne doit
        // ni s'allumer ni repondre a travers son voile.
        boolean blocked = surface != null;
        AbilityData data = ClientAbilityData.get();

        for (Node node : nodes) {
            node.widget.transform.x = node.skill.getGuiX() - dx * PARALLAX;
            node.widget.transform.y = node.skill.getGuiY() - dy * PARALLAX;

            boolean hovered = !blocked && node.widget.isPointWithin(mouseX, mouseY);
            if (hovered != node.hovered) {
                node.hovered = hovered;
                node.since = time;
            }

            double transit = clamp01((time - node.since) / HOVER_TRANSIT);
            double scale = node.hovered
                    ? lerp(1.0, HOVER_SCALE, transit)
                    : lerp(HOVER_SCALE, 1.0, transit);

            double dt = Math.max(0, time - opened - node.born);
            CWidget.Disc disc = node.widget.disc;
            if (disc != null) {
                disc.icon = node.skill.getHintIcon().toString();
                disc.drawScale = scale;
                // L'anneau dit l'experience acquise, et l'icone grisee dit que la competence n'est pas
                // encore apprise. Les deux tiennent dans le meme noeud, et c'est ce qui fait qu'on lit
                // l'arbre d'un coup d'oeil : ce qui brille appartient au joueur, ce qui grisonne reste a
                // prendre.
                disc.drawFill = node.learned;
                disc.fill = data.getSkillExp(node.skill);
                disc.grey = !node.learned;
                disc.backAlpha = alpha(node.dim * APPEAR_SPEED * dt);
                disc.iconAlpha = alpha(node.dim * APPEAR_SPEED * (dt - APPEAR_STEP));
            }
            if (node.widget.line != null) {
                node.widget.line.progress = clamp01(LINK_FADE * dt);
                node.widget.line.alpha = alpha(LINK_FADE * dt * linkDim(node.dim, node.learned));
            }
        }
    }

    /**
     * L'opacite d'un trait : celle de son noeud, et le facteur des competences non apprises.
     *
     * <p>Voir {@link #LINK_UNLEARNED}. Une competence apprise garde son trait plein ; toutes les
     * autres le portent a quatre dixiemes, qu'elles soient a portee (sept dixiemes) ou non (un quart).
     */
    static double linkDim(double dim, boolean learned) {
        return dim * (learned ? 1.0d : LINK_UNLEARNED);
    }

    /** Une opacite de zero a 255, bornee. */
    private static int alpha(double fraction) {
        return (int) Math.round(clamp01(fraction) * 255);
    }

    /**
     * Fait glisser le fond de la zone de l'arbre dans son cadre.
     *
     * <p>C'est l'image de l'original — {@code effect_developer_background} — un centieme plus
     * <b>grande</b> que la zone : elle deborde donc de cette marge, et la fenetre s'y deplace au rythme
     * de la souris. Elle ne bouge presque pas : juste assez pour que la zone paraisse posee devant
     * quelque chose. Les noeuds, eux, se deplacent de dix unites, et c'est ce decalage entre les deux
     * qui fait le relief.
     *
     * <p>ELLE AVAIT DISPARU, ET LE JOUEUR L'A VU DEUX FOIS. D'abord parce qu'elle n'etait pas dessinee
     * du tout ; ensuite parce qu'elle etait dessinee <b>avant tous les widgets</b>, donc sous le fond du
     * panneau — une image noire a moitie — et sous les ornements du panneau de droite. Le joueur l'a
     * dit a sa facon : « je me demande si ce n'est pas parce que l'effet de noir dans le fond passe par
     * devant et qu'il le cache ». C'etait exactement cela, sauf que le noir en question n'est pas celui
     * de l'ecran mais celui de la page.
     *
     * <p>Elle est maintenant posee <b>sur le widget de la zone</b>, comme l'original qui la dessinait
     * depuis l'evenement d'image de cette meme zone. Le moteur dessine la texture d'un widget avant ses
     * enfants : l'image se retrouve donc entre le fond de la page et les noeuds, a sa place exacte.
     */
    private void drawAreaBackdrop(double dx, double dy) {
        CWidget area = widget("area");
        if (area == null || area.texture == null) return;

        area.texture.u0 = slide(dx * BACKDROP_SLIDE);
        area.texture.v0 = slide(dy * BACKDROP_SLIDE);
        area.texture.u1 = area.texture.u0 + BACKDROP_WINDOW;
        area.texture.v1 = area.texture.v0 + BACKDROP_WINDOW;
    }

    /**
     * Ou la fenetre s'ouvre dans l'image.
     *
     * <p>L'original ne la fait pas partir du bord : elle se pose a un demi-centieme du centre, ce qui
     * laisse de quoi glisser d'un cote comme de l'autre. C'est son {@code scale()}.
     */
    private static double slide(double offset) {
        return (offset - 0.5) * BACKDROP_WINDOW + 0.5;
    }

    private static double lerp(double from, double to, double progress) {
        return from + (to - from) * progress;
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private static double now() {
        return Util.getMillis() / 1000.0;
    }

    /**
     * Ce que l'arbre montre, sous forme d'une phrase comparable.
     *
     * <p>L'original rebatissait tout son ecran apres un apprentissage, par un evenement. Le port
     * regarde plutot ce qui a change : le niveau de la categorie, et quelles competences sont apprises.
     * Tant que rien de tout cela ne bouge, l'arbre ne se refait pas — et des qu'une competence
     * s'ouvre, un noeud de plus apparait et son fondu repart pour lui.
     */
    private String signature() {
        Category category = category();
        if (category == null) return "-";
        AbilityData data = ClientAbilityData.get();

        StringBuilder out = new StringBuilder(category.getName());
        out.append('/').append(data.getCategoryLevel(category)).append('/');
        for (Skill skill : category.getSkills()) {
            out.append(data.isSkillLearned(skill) ? '+' : '-');
        }
        return out.toString();
    }

    /** Rebatit l'arbre si ce qu'il montre a change. */
    private void refresh() {
        String current = signature();
        if (current.equals(signature)) return;

        CWidget page = pageWindow(PAGE);
        if (page == null) return;

        // Les deux panneaux de gauche portent l'aptitude du joueur : ils changent avec elle, et
        // sans ce repose le panneau continuerait d'annoncer « No ability » apres la premiere
        // aptitude, jusqu'a la prochaine ouverture de l'ecran.
        buildAbilityPanel(page);
        buildMachinePanel(page);
        buildTree(page);
        // Les noeuds qui viennent de naitre n'ont pas encore de place : sans ce repose, ils se
        // dessineraient tous au coin, le temps d'une image.
        reflow();
        // Et si une surface est ouverte, l'arbre reste dessine SOUS son voile : c'est ce que faisait
        // l'original, dont le voile couvrait tout l'ecran sans rien cacher — le joueur : « quand on
        // lit ce que fait l'attaque, on doit quand meme voir l'interface de base derriere (avec
        // toutes les competences, leur cercle d'xp etc) ».
        reflow();
    }

    // ------------------------------------------------------------------
    // Les surfaces : apprendre, monter de niveau, changer d'aptitude
    // ------------------------------------------------------------------

    /**
     * La surface d'une competence : son icone, son nom, et de quoi l'apprendre.
     *
     * <p>C'est le {@code skillViewArea} de l'original : un voile noir sur tout l'ecran, l'icone au
     * milieu, et dessous ce qu'il faut savoir. Une competence deja apprise y montre son experience ;
     * une autre montre ce qui manque — les conditions non remplies, dites par elles-memes — et le
     * bouton qui l'apprend.
     *
     * <p>Le bouton ne se montre que si l'apprentissage est <b>possible</b>. L'original, lui, le montrait
     * toujours et repondait par une phrase quand il refusait ; ici les raisons sont deja ecrites au-dessus
     * de lui, donc un bouton qui refuse n'apprendrait rien au joueur, et ne rien montrer est plus clair.
     */
    private void openSkill(Skill skill) {
        AbilityData data = ClientAbilityData.get();
        boolean learned = data.isSkillLearned(skill);

        CWidget content = new CWidget("skill");
        content.add(surfaceIcon(skill));

        content.add(text(skillName(skill), TITLE_Y, 255, 255, 255, TITLE_FONT));
        double y;
        if (learned) {
            content.add(text(Component.translatable("academy.developer.skill_exp",
                    (int) (data.getSkillExp(skill) * 100)).getString(), SUBTITLE_Y, 161, 225, 255,
                    EXP_FONT));
        } else {
            content.add(text(Component.translatable("academy.developer.not_learned").getString(),
                    SUBTITLE_Y + 1, 255, 85, 85, TEXT_FONT));
        }
        y = DESCRIPTION_Y;

        // LA DESCRIPTION, DANS LES DEUX CAS. L'original ne la montrait qu'une fois la competence
        // apprise : pour une competence qu'on ne connait pas encore, il n'affichait que ce qui manque.
        // Le joueur a demande l'inverse — « quand on click sur une competence on est cense voir une
        // petite description qui apparait pour avoir une idee de ce que fait le pouvoir » — et c'est
        // plus utile avant qu'apres : c'est au moment de la choisir qu'on veut savoir ce qu'elle fait.
        for (String line : wrap(skill.getDescription().getString(), DESCRIPTION_WIDTH)) {
            content.add(text(line, y, 255, 255, 255, DESC_FONT));
            y += 10;
        }

        if (!learned) {
            y += 6;
            for (LearningCondition condition : skill.getConditions()) {
                if (condition.accepts(data, skill, menu.getDeveloperType())) continue;
                content.add(text(condition.describe(skill).getString(), y, 255, 88, 88, DESC_FONT));
                y += 11;
            }
            y += 4;
            content.add(text(cost(skill), y, 170, 170, 170));
            if (canLearn(skill)) {
                content.add(button(() -> click(DeveloperMenu.skillButton(skill)), BUTTON_Y));
            }
        }
        open(content);
    }

    /**
     * Coupe une phrase en lignes qui tiennent dans la largeur donnee.
     *
     * <p>C'est le {@code drawSeperated} de l'original, qui decoupait la description a deux cents unites
     * et la dessinait ligne a ligne. Le moteur n'a pas de texte multiligne — une boite porte une ligne —
     * donc l'ecran coupe lui-meme et pose une boite par ligne. La coupe se fait sur les espaces, jamais
     * au milieu d'un mot, et mesure avec la vraie police : c'est la seule facon de savoir ou une ligne
     * deborde vraiment.
     */
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

    /** La surface de la montee de niveau : ce que le palier suivant coute, et de quoi le payer. */
    private void openLevel() {
        Category category = category();
        if (category == null) return;
        int index = category.getCategoryId();

        CWidget content = new CWidget("level");
        content.add(surfaceIcon(null));

        double y = -42;
        content.add(text(Component.translatable("academy.developer.level_up",
                Component.translatable(category.getDisplayKey())).getString(), y, 255, 255, 255));
        y += 14;
        content.add(text(costText(index), y, 170, 170, 170));
        y += 16;
        content.add(text(Component.translatable("academy.developer.level_question").getString(),
                y, 200, 200, 200));
        y += 18;
        if (menu.getEnergyStored() >= menu.getCostFor(index)) {
            // Le bouton de la montee de niveau porte l'identifiant de sa CATEGORIE : c'est ainsi que le
            // menu les distingue des competences, qui commencent a cent.
            content.add(button(() -> click(index), BUTTON_Y));
        }
        open(content);
    }

    /**
     * La surface du changement d'aptitude : la seule qui se demande en tenant la bobine magnetique.
     *
     * <p>L'original ne montrait pas l'arbre du tout dans ce cas, il montrait une console a commandes.
     * Le port pose la question, ce qui revient au meme pour le joueur — la console viendra si elle
     * manque.
     */
    private void openReset() {
        CWidget content = new CWidget("reset");
        content.add(surfaceIcon(null));

        double y = -42;
        content.add(text(Component.translatable("academy.developer.reset").getString(), y,
                255, 255, 255));
        y += 14;
        content.add(text(Component.translatable("academy.developer.reset.tooltip").getString(),
                y, 200, 200, 200));
        y += 18;
        content.add(button(() -> click(DeveloperMenu.RESET_BUTTON), BUTTON_Y));
        open(content);
    }

    /**
     * Pourquoi le bouton ne se montre pas.
     *
     * <p>Le serveur revalide de toute facon : ce qui se verifie ici ne decide de rien, il evite
     * seulement de proposer une action qui sera refusee — c'est la meme regle que pour les lignes de
     * l'ancien ecran. Le client ne decide pas de ce qui s'apprend, il evite de mentir.
     */
    private boolean canLearn(Skill skill) {
        AbilityData data = ClientAbilityData.get();
        if (menu.getEnergyStored() < menu.getCostFor(skill.getCategory().getCategoryId())) return false;
        if (skill.getLevel() > data.getCategoryLevel(skill.getCategory())) return false;
        return LearningHelper.canLearn(data, skill, menu.getDeveloperType());
    }

    private String cost(Skill skill) {
        return costText(skill.getCategory().getCategoryId());
    }

    private String costText(int index) {
        return Component.translatable("academy.developer.cost", menu.getStimulationsFor(index),
                (int) menu.getCostFor(index)).getString();
    }

    /** Le nom d'une competence, avec son niveau : c'est ainsi que l'original le presentait. */
    private static String skillName(Skill skill) {
        return Component.translatable(skill.getDisplayKey()).getString()
                + " (LV " + skill.getLevel() + ")";
    }

    /**
     * Le bouton de l'original : son image, mise a l'echelle, et rien dessus.
     *
     * <p>Sa teinte <b>remplace</b> la couleur de l'image, et c'est tout son retour : gris quand on ne le
     * survole pas, blanc quand on le survole. Cliquer ferme la surface et fait ce qu'il a a faire — dans
     * cet ordre, pour que l'action parte sur un ecran encore en place.
     */
    private CWidget button(Runnable action, double y) {
        return button(action, y, true);
    }

    /**
     * @param close vrai si ce bouton ferme une surface ouverte, comme celui de l'original ; faux
     *              pour celui de la console, qui ne ferme rien du tout
     */
    private CWidget button(Runnable action, double y, boolean close) {
        CWidget widget = new CWidget("button");
        widget.transform.width = BUTTON_WIDTH;
        widget.transform.height = BUTTON_HEIGHT;
        widget.transform.scale = BUTTON_SCALE;
        widget.transform.x = 0;
        widget.transform.y = y;
        widget.transform.alignWidth = "CENTER";
        widget.transform.alignHeight = "CENTER";

        widget.texture = new CWidget.DrawTexture();
        widget.texture.texture = BUTTON_TEXTURE;
        widget.tint = new CWidget.Tint();
        widget.tint.affectTexture = true;
        widget.tint.idle = new int[] { 255, 255, 255, 153 };
        widget.tint.hover = new int[] { 255, 255, 255, 255 };

        onClick(widget, () -> {
            action.run();
            if (close) beginClose();
        });
        return widget;
    }

    /** Le disque d'une surface : le fond, et l'icone par-dessus quand il y en a une. */
    private static CWidget surfaceIcon(@Nullable Skill skill) {
        CWidget icon = centred("icon", SURFACE_BACK_SIZE, SURFACE_BACK_SIZE);
        icon.texture = new CWidget.DrawTexture();
        icon.texture.texture = SURFACE_BACK;
        if (skill == null) return icon;

        CWidget glyph = centred("glyph", SURFACE_ICON_SIZE, SURFACE_ICON_SIZE);
        glyph.texture = new CWidget.DrawTexture();
        glyph.texture.texture = skill.getHintIcon().toString();
        // L'icone d'une competence fait trente-deux pixels pour un dessin de vingt-sept unites, donc
        // lissee a l'echelle de l'ecran : au plus proche voisin, elle sort en gros pixels — « l'icone
        // de la competence dans la fiche est toujours pixelisee ».
        CGuiShaders.linear(skill.getHintIcon());
        icon.add(glyph);
        return icon;
    }

    /**
     * Un bout de texte centre sur la page.
     *
     * <p>Sa boite n'est <b>pas</b> vide : le moteur n'ecrit un texte que dans une boite qui a une
     * taille, et une boite de zero sur zero ne dessine donc rien du tout.
     */
    private static CWidget text(String content, double y, int red, int green, int blue) {
        return text(content, y, red, green, blue, TEXT_FONT);
    }

    /**
     * Le meme, a la taille demandee.
     *
     * <p>Les tailles sont celles de l'original, et c'est lui qui les a fixees : la fiche d'une
     * competence ecrit son nom a douze, son experience a huit, sa description a neuf, et « pas
     * apprise » a dix. Le port les ecrivait toutes a dix, la taille par defaut du moteur, et le
     * joueur l'a vu : « le texte n'est pas bien centre, de la bonne couleur et de la bonne taille ».
     */
    private static CWidget text(String content, double y, int red, int green, int blue, double size) {
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

    /**
     * Ouvre une surface sur la page.
     *
     * <p>Elle s'y pose en <b>dernier</b>, donc au-dessus de tout le reste. Et tant qu'elle est ouverte,
     * l'arbre ne se dessine plus : le moteur donne un clic au widget le plus <b>ancien</b> de ses freres,
     * c'est-a-dire a celui du fond, et un noeud prendrait le clic destine a la surface. L'original
     * laissait son arbre visible sous un voile a sept dixiemes ; ici il s'efface, et c'est le prix de
     * cette regle-la.
     */
    private void open(CWidget content) {
        CWidget page = pageWindow(PAGE);
        if (page == null) return;

        closeSurface();

        surface = new CWidget("surface");
        surface.transform.width = PAGE_WIDTH;
        surface.transform.height = PAGE_HEIGHT;
        surface.transform.x = 0;
        surface.transform.y = 0;
        surface.transform.alignWidth = "LEFT";
        surface.transform.alignHeight = "TOP";
        surface.texture = new CWidget.DrawTexture();
        surface.texture.red = 0;
        surface.texture.green = 0;
        surface.texture.blue = 0;
        surface.texture.alpha = 0;

        // Le contenu prend la BOITE DE LA PAGE. Sans cela il n'a ni largeur ni hauteur — c'est le
        // defaut d'un widget neuf — et ses textes, qui s'accrochent au CENTRE de leur parent, se
        // posaient alors sur le coin haut-gauche de la page : ceux dont le y est negatif, c'est-a-dire
        // le nom, le niveau et la DESCRIPTION, tombaient au-dessus du bord et ne se voyaient pas. Le
        // joueur l'a dit ainsi : « quand je click sur un pouvoir, je n'ai pas la description ».
        content.transform.width = PAGE_WIDTH;
        content.transform.height = PAGE_HEIGHT;
        content.transform.alignWidth = "LEFT";
        content.transform.alignHeight = "TOP";
        surface.add(content);

        page.add(surface);
        onClick(surface, this::beginClose);
        reflow();

        surfaceSince = now();
        surfaceClosing = false;
    }

    /**
     * Un clic que rien n'a pris referme la fiche ouverte.
     *
     * <p>L'original enveloppait la sienne d'un voile de la taille de l'ECRAN, qui se refermait au
     * premier clic ; le port ne donnait ce clic qu'aux widgets de la page, donc au seul carre noir de
     * la machine — le joueur : « je suis oblige de cliquer parmi le carre noir de la machine pour
     * revenir en arriere alors qu'il faudrait que je puisse cliquer n'importe ou ».
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (surface != null && !surfaceClosing && button == 0) {
            beginClose();
            return true;
        }
        return false;
    }

    /** Referme la surface, en la faisant d'abord s'effacer. */
    private void beginClose() {
        if (surface == null || surfaceClosing) return;
        surfaceClosing = true;
        surfaceSince = now();
    }

    private void closeSurface() {
        CWidget page = pageWindow(PAGE);
        if (surface != null && page != null) page.remove(surface);
        surface = null;
        surfaceClosing = false;
    }

    /** Le fondu de la surface : elle monte a sept dixiemes, et redescend quand on la referme. */
    private void animateSurface() {
        if (surface == null || surface.texture == null) return;

        double progress = clamp01((now() - surfaceSince) / VEIL_FADE);
        double alpha = surfaceClosing ? 1 - progress : progress;
        surface.texture.alpha = (int) Math.round(alpha * VEIL_ALPHA);

        if (surfaceClosing && progress >= 1) closeSurface();
    }

    // ------------------------------------------------------------------
    // Les boutons du conteneur
    // ------------------------------------------------------------------

    /**
     * Envoie un bouton au conteneur.
     *
     * <p>C'est le chemin vanilla des boutons de menu, celui qu'utilisait deja l'ancien ecran : aucun
     * paquet a nous, et le serveur revalide tout ce qu'on lui demande.
     */
    private void click(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }
}
