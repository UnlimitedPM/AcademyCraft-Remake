package cn.academy;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.ability.client.ClientAbilityData;
import cn.academy.ability.develop.DevelopActionLevel;
import cn.academy.ability.develop.DevelopProgress;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.LearningHelper;
import cn.academy.ability.develop.condition.LearningCondition;
import cn.academy.client.gui.cgui.CGuiContainerScreen;
import cn.academy.client.gui.cgui.CGuiShaders;
import cn.academy.client.gui.cgui.CWidget;
import cn.academy.client.gui.cgui.DeveloperConsole;
import cn.academy.client.gui.cgui.GuiPageHost;
import cn.academy.client.gui.cgui.GuiSurface;
import cn.academy.client.gui.cgui.SkillTreeView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * La console : ses lignes se posent a cinq unites du coin, tous les dix, a la taille huit.
     *
     * <p>Ce sont les nombres de l'original ({@code Console} : {@code x = 5}, {@code y = 5},
     * {@code y += 10}, {@code FontOption(8)}), et non un centrage : sa console s'ecrit comme un
     * terminal, en haut a gauche de la zone.
     */
    private static final double CONSOLE_X = 5.0;
    private static final double CONSOLE_Y = 5.0;
    private static final double CONSOLE_LINE = 10.0;
    private static final double CONSOLE_FONT = 8.0;

    /** L'icone d'une surface : le meme disque, mais plus grand. */
    private static final String SURFACE_BACK = "academy:textures/guis/developer/skill_back.png";
    private static final double SURFACE_BACK_SIZE = 50.0;
    private static final double SURFACE_ICON_SIZE = 27.0;

    /**
     * L'arbre des competences, et la fiche qui s'ouvre dessus.
     *
     * <p>Les deux vivent maintenant hors de cet ecran — voir {@link SkillTreeView} et
     * {@link GuiSurface} — parce que l'application du terminal les montre aussi, sans machine et
     * sans apprentissage. C'est exactement ce que faisait l'original, dont les deux ecrans
     * passaient par le meme {@code Common.initialize}.
     */
    private final GuiPageHost host = new GuiPageHost() {
        @Override
        public CWidget page() {
            return pageWindow(PAGE);
        }

        @Override
        public void onClick(CWidget widget, Runnable action) {
            DeveloperScreen.this.onClick(widget, action);
        }

        @Override
        public void reflow() {
            DeveloperScreen.this.reflow();
        }
    };

    private final SkillTreeView tree = new SkillTreeView(host);
    private final GuiSurface veil = new GuiSurface(host, PAGE_WIDTH, PAGE_HEIGHT);

    /**
     * La console du joueur sans aptitude, et la boite qui la porte.
     *
     * <p>Elle survit aux reconstructions de la page : c'est elle qui tient le texte deja ecrit, la
     * tache en cours et la saisie. La recreer a chaque {@code build} ferait repartir l'animation de
     * demarrage a chaque image, et le joueur ne verrait jamais la fin de la banniere.
     */
    @Nullable
    private DeveloperConsole console;

    @Nullable
    private CWidget consoleBox;

    /**
     * Ce que la console montre : {@code null} quand il n'y en a pas.
     *
     * <p>L'original avait deux consoles — celle du joueur sans aptitude, qui propose {@code learn},
     * et celle de la bobine magnetique, qui propose {@code reset}. Le port les distingue par ce
     * drapeau, et recree la console quand il change : passer de l'une a l'autre doit repartir d'une
     * banniere neuve, comme a l'ouverture.
     */
    @Nullable
    private String consoleMode;

    public DeveloperScreen(DeveloperMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        // La page de l'original n'est pas un cadre de machine : elle fait quatre cents unites de large,
        // et c'est elle qui decide de la taille de l'ecran.
        this.imageWidth = (int) PAGE_WIDTH;
        this.imageHeight = (int) PAGE_HEIGHT;
    }

    /** La page occupe toute la place : elle ne se pose pas au milieu d'un cadre de machine. */
    @Override
    protected void layoutRoot(CWidget root) {        root.transform.width = PAGE_WIDTH;
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

        buildAbilityPanel(page);
        buildMachinePanel(page);
        buildAptitude(page);
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
     * a taper. Le port affichait ce texte d'un coup et remplacait la frappe par un bouton ; le
     * joueur a reclame la mecanique : « il n'y a pas l'animation du texte ni meme la mecanique de
     * devoir taper /learn pour apprendre le pouvoir ».
     *
     * <p>La console vit dans {@link DeveloperConsole} — sans Minecraft, donc relisible en JUnit — et
     * cet ecran ne fait que la dessiner et lui passer le clavier.
     */
    private void buildConsole(CWidget area, String mode) {
        area.children().clear();

        // Une console neuve quand le MODE change : celle du joueur sans aptitude propose `learn`,
        // celle de la bobine propose `reset`, et l'original en ouvrait une autre dans chaque cas.
        if (console == null || !mode.equals(consoleMode)) {
            console = newConsole(mode);
            consoleMode = mode;
        }

        // La console se dessine elle-meme : ses lignes changent a chaque image, et un widget par
        // ligne obligerait a rebatir l'arbre soixante fois par seconde.
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

    /** La console du joueur, avec ses textes et sa commande. */
    private DeveloperConsole newConsole(String mode) {
        DeveloperConsole.Texts texts = newConsoleTexts();

        String name = minecraft == null || minecraft.player == null
                ? "" : minecraft.player.getGameProfile().getName();
        boolean reset = "reset".equals(mode);
        DeveloperConsole console = new DeveloperConsole(texts, name, reset, true,
                new java.util.Random());

        // Les deux commandes de l'original. `learn` n'existe que sur une machine — c'est elle qui
        // donne l'aptitude — et `reset` que si le joueur tient la bobine.
        if (reset) {
            console.command("reset", this::consoleReset);
        } else {
            console.command("learn", this::consoleLearn);
        }
        return console;
    }

    private static String key(String translationKey) {
        return Component.translatable(translationKey).getString();
    }

    /**
     * La commande {@code learn} : la stimulation, puis son verdict.
     *
     * <p>C'est le {@code Command("learn", ...)} de l'original, dans son ordre : le texte de debut,
     * la ligne de progression a zero, l'envoi au serveur, puis une tache qui attend la fin de
     * l'apprentissage et reecrit le pourcentage sur place.
     */
    private void consoleLearn() {
        DeveloperConsole console = this.console;
        if (console == null) return;

        console.enqueue(console.print(consoleTexts().devBegin()));
        console.enqueue(console.print(consoleTexts().progress().apply(0)));
        click(DeveloperMenu.FIRST_CATEGORY_BUTTON);
        console.enqueue(progressTask(consoleTexts().devSucc(), consoleTexts().devFail()));
    }

    /** La commande {@code reset} : le changement de categorie, avec ses trois refus. */
    private void consoleReset() {
        DeveloperConsole console = this.console;
        if (console == null) return;

        if (!menu.canReset()) {
            // L'original distingue les deux raisons : la machine n'est pas la bonne, ou le joueur
            // n'a pas ce qu'il faut. Le port les separe de la meme facon.
            console.enqueue(console.print(menu.getDeveloperType() != DeveloperType.ADVANCED
                    ? consoleTexts().resetFailDev()
                    : consoleTexts().resetFailOther()));
            return;
        }

        console.enqueue(console.print(consoleTexts().resetBegin()));
        console.enqueue(console.print(consoleTexts().progress().apply(0)));
        click(DeveloperMenu.RESET_BUTTON);
        console.enqueue(progressTask(consoleTexts().resetSucc(), consoleTexts().resetFail()));
    }

    /**
     * La tache qui suit un apprentissage : elle reecrit le pourcentage sur place, puis dit le
     * verdict et demande la reconstruction de l'ecran.
     *
     * <p>C'est la tache anonyme de l'original, avec son {@code \b\b\b} : trois retours arriere
     * effacent le pourcentage precedent, et le nouveau s'ecrit dessus. Sans eux, la console
     * empilerait une ligne par pourcentage.
     *
     * <p>Elle se termine aussi quand l'aptitude est <b>arrivee</b>, et pas seulement quand la
     * machine a fini : la premiere aptitude du port se donne tout de suite, sans stimulation
     * ({@code FIRST_CATEGORY_BUTTON}), donc l'etat de la machine ne bouge jamais et la tache
     * attendrait un verdict qui ne vient pas.
     */
    private DeveloperConsole.Task progressTask(String success, String failure) {
        return new DeveloperConsole.Task() {
            @Override
            public boolean isFinished(double now) {
                return menu.getState() == DevelopProgress.DevState.FAILED
                        || menu.getState() == DevelopProgress.DevState.DONE
                        || category() != null;
            }

            @Override
            public void update(double now) {
                if (console == null) return;
                console.output("\b\b\b" + (int) (menu.getProgress() * 100) + "%");
            }

            @Override
            public void finish() {
                if (console == null) return;
                boolean done = menu.getState() == DevelopProgress.DevState.DONE
                        || category() != null;
                console.outputln();
                console.output(done ? success : failure);
                console.pause(0.5);
                console.enqueueRebuild();
            }
        };
    }

    /** Les textes de la console, pour les commandes qui les ecrivent hors de sa construction. */
    private DeveloperConsole.Texts consoleTexts() {
        return console == null ? newConsoleTexts() : console.texts();
    }

    private DeveloperConsole.Texts newConsoleTexts() {
        return new DeveloperConsole.Texts(
                key("academy.skill_tree.console.init"),
                key("academy.skill_tree.console.boot_failed"),
                key("academy.skill_tree.console.invalid_cat"),
                key("academy.skill_tree.console.learn_hint"),
                key("academy.skill_tree.console.override"),
                key("academy.skill_tree.console.invalid_command"),
                percent -> Component.translatable("academy.skill_tree.console.progress", percent)
                        .getString(),
                key("academy.skill_tree.console.dev_begin"),
                key("academy.skill_tree.console.dev_succ"),
                key("academy.skill_tree.console.dev_fail"),
                key("academy.skill_tree.console.reset_begin"),
                key("academy.skill_tree.console.reset_succ"),
                key("academy.skill_tree.console.reset_fail"),
                key("academy.skill_tree.console.reset_fail_dev"),
                key("academy.skill_tree.console.reset_fail_other"));
    }

    /**
     * La zone de droite : l'arbre, ou la console quand le joueur n'a encore aucune aptitude.
     *
     * <p>L'arbre lui-meme vit hors de cet ecran, dans {@link SkillTreeView} : l'application du
     * terminal montre le meme, sans machine. Ici il reste a decider ce qui prend la place quand il
     * n'y a rien a dessiner — et c'est la console de l'original, qui dit au joueur comment entrer
     * dans le mod. Le passage se fait tout seul une fois la premiere aptitude apprise, puisque
     * l'ecran rebatit des que l'aptitude change.
     */
    private void buildAptitude(CWidget page) {
        // La zone se prend par la PAGE, et non par `widget("area")` : celui-ci passe par la vue, qui
        // n'existe pas encore quand la page se construit. Le joueur voyait donc, a la place de la
        // console, le vieux message « ask a developer for one » — la console ne s'affichait jamais.
        CWidget right = page.child("parent_right");
        CWidget area = right == null ? null : right.child("area");
        if (area == null) return;

        Category category = category();
        AbilityData data = ClientAbilityData.get();

        // Les trois cas de l'original, dans son ordre : pas d'aptitude -> la console qui propose
        // `learn` ; la bobine magnetique en main -> la console qui propose `reset` ; sinon l'arbre.
        if (category == null) {
            buildConsole(area, "learn");
            tree.note(null, data);
            return;
        }
        if (holdsCoil()) {
            buildConsole(area, "reset");
            tree.note(null, data);
            return;
        }

        // L'aptitude est arrivee, ou la bobine est rangee : la console a fini son office, et son
        // texte ne doit pas rester derriere l'arbre.
        console = null;
        consoleBox = null;
        consoleMode = null;
        tree.build(category, data, this::openSkill);
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
     * Ce qui bouge a chaque image : les panneaux, le voile de la fiche ouverte, et l'arbre.
     *
     * <p>L'arbre vit dans {@link SkillTreeView} : c'est lui qui connait le relief, le survol, les
     * fondus et le trait qui pousse, et c'est ce qui permet a l'application du terminal d'en montrer
     * exactement le meme.
     */
    @Override
    protected void frame(GuiGraphics graphics, double mouseX, double mouseY) {
        refresh();
        veil.animate();
        tree.frame(ClientAbilityData.get(), mouseX, mouseY, width, height, veil.opened());
    }

    /**
     * Le dessin de l'ecran, et la console par-dessus.
     *
     * <p>La console se dessine <b>apres</b> la page : {@code frame} passe avant le document, donc
     * tout ce qu'on y ecrit est recouvert par le fond de la page — c'est la regle du moteur, et
     * c'est ce qui a deja fait disparaitre un texte par le passe. Elle se dessine donc ici, une
     * fois tout le reste pose.
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        drawConsole(graphics);
    }

    /**
     * Dessine la console, ligne par ligne, a la place de la zone de l'arbre.
     *
     * <p>Elle se dessine ici et non par des widgets : ses lignes changent a chaque image — le texte
     * s'ecrit lettre par lettre, le pourcentage se reecrit sur place — et un widget par ligne
     * obligerait a rebatir l'arbre soixante fois par seconde. C'est aussi ce que faisait l'original,
     * dont la console dessinait son texte dans son propre {@code FrameEvent}.
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

        // La console a fini son travail : l'arbre prend sa place. C'est l'evenement de
        // reconstruction de l'original, et sans lui le joueur ne verrait jamais le verdict.
        //
        // La reconstruction est FORCEE : `refresh` compare la signature de l'arbre, et celle-ci ne
        // bouge pas quand l'aptitude n'est pas encore arrivee du serveur — la zone resterait alors
        // vide, sans console et sans arbre.
        if (console.rebuildRequested()) {
            console = null;
            consoleBox = null;
            consoleMode = null;
            CWidget page = pageWindow(PAGE);
            if (page != null) {
                buildAptitude(page);
                reflow();
            }
        }
    }

    /** Rebatit l'arbre et les panneaux si ce qu'ils montrent a change. */
    private void refresh() {
        CWidget page = pageWindow(PAGE);
        if (page == null) return;

        Category category = category();
        AbilityData data = ClientAbilityData.get();

        // La console a son propre etat : elle change de mode quand le joueur prend ou range la
        // bobine, et l'arbre ne voit rien de tout cela — sa signature ne parle que de l'aptitude.
        String mode = category == null ? "learn" : holdsCoil() ? "reset" : null;

        // Tant qu'une console est a l'ecran, c'est ELLE qui decide : la signature de l'arbre ne
        // bouge pas (elle vaut « pas d'arbre »), donc la consulter rebatirait la page a chaque
        // image. Seul un changement de mode — ou l'arrivee de l'aptitude — demande un repose.
        boolean rebuild = mode == null
                ? !tree.unchanged(category, data)
                : !mode.equals(consoleMode);

        if (!rebuild) return;

        // Les deux panneaux de gauche portent l'aptitude du joueur : ils changent avec elle, et
        // sans ce repose le panneau continuerait d'annoncer « No ability » apres la premiere
        // aptitude, jusqu'a la prochaine ouverture de l'ecran.
        buildAbilityPanel(page);
        buildMachinePanel(page);
        buildAptitude(page);
        // Les noeuds qui viennent de naitre n'ont pas encore de place : sans ce repose, ils se
        // dessineraient tous au coin, le temps d'une image.
        reflow();
        // Et si une fiche est ouverte, l'arbre reste dessine SOUS son voile : c'est ce que faisait
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
            beginClose();
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
     * Ouvre une fiche sur la page.
     *
     * <p>C'est {@link GuiSurface} qui la pose en <b>dernier</b> dans la page, donc au-dessus de tout
     * le reste, et qui la referme au premier clic.
     */
    private void open(CWidget content) {
        veil.open(content);
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
        if (veil.opened() && button == 0) {
            veil.beginClose();
            return true;
        }
        return false;
    }

    /**
     * Le clavier va a la console quand elle est la.
     *
     * <p>C'est le {@code KeyEvent} de l'original : retour arriere efface une lettre, Entree valide
     * la ligne, et tout le reste s'ecrit. Le port n'avait rien de tout cela — sa console affichait
     * son texte et ne prenait aucune frappe.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (console != null) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
                console.backspace();
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                    || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                console.submit();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Une lettre tapee va dans la ligne de la console. */
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (console != null && net.minecraft.SharedConstants.isAllowedChatCharacter(codePoint)) {
            console.type(codePoint);
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    /** Referme la fiche, en la faisant d'abord s'effacer. */
    private void beginClose() {
        veil.beginClose();
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
