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
import cn.academy.client.gui.cgui.CGuiWireless;
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

    /** Le bouton de l'original : son image, sa taille dessinee, et ses deux teintes. */
    private static final String BUTTON_TEXTURE = "academy:textures/guis/developer/button.png";
    private static final double BUTTON_WIDTH = 64.0;
    private static final double BUTTON_HEIGHT = 32.0;
    private static final double BUTTON_SCALE = 0.5;

    /**
     * La place du bouton : quatre-vingts unites sous le centre de la fiche.
     *
     * <p>C'est la {@code pos(0, 55)} de l'original — son bloc de texte commencait a vingt-cinq, donc
     * le bouton tombait a quatre-vingts. Le port le posait a trente, c'est-a-dire au milieu du
     * disque de la competence, et le joueur l'a vu : « le bouton pour apprendre est au mauvais
     * endroit ».
     */
    private static final double BUTTON_Y = 80.0;

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
    private static final double TITLE_Y = 28.0;
    private static final double SUBTITLE_Y = 40.0;

    /**
     * La description d'une competence apprise, et ce qui suit.
     *
     * <p>L'original ne la montrait <b>que</b> pour une competence apprise : pour une autre, sa fiche
     * disait « Skill not learned », posait ses conditions, puis sa question. Le port l'ecrivait dans
     * les deux cas, et c'est ce que le joueur vient de refuser : « pour apprendre une competence il
     * affiche la description alors qu'il ne devrait pas ».
     */
    private static final double DESCRIPTION_Y = 49.0;

    /**
     * La rangee des conditions, celle de l'original.
     *
     * <p>Ses nombres sont dans {@code skillViewArea} : des icones de quatorze unites, ecartees de
     * seize, posees a cinquante unites du centre — et non sous une description qui n'existe pas
     * dans ce cas. Le port les ecrivait en phrases rouges ; le joueur a reclame l'affichage du vrai
     * mod, puis a vu que la rangee etait poussee hors de la fiche.
     */
    private static final double CONDITION_ICON_SIZE = 14.0;
    private static final double CONDITION_STEP = 16.0;
    private static final double CONDITION_Y = 50.0;

    /** Le « Req. » de l'original : deux unites avant le bord gauche de la rangee, et trois sous elle. */
    private static final double CONDITION_LABEL_GAP = 2.0;

    /**
     * De combien la boite de « Req. » descend sous le haut des icones.
     *
     * <p>L'original ecrit ce mot a {@code y = 28} et pose ses icones a {@code y = 25} : trois
     * unites, donc. Notre boite centre son glyphe dans douze unites, ce qui le remonte de quatre et
     * demie par rapport a l'ordonnee qu'on lui donne — d'ou ces trois plus quatre et demie, qui
     * remettent le mot a sa vraie place, trois unites sous le haut des icones. Le joueur, captures
     * cote a cote, l'a vu : « le Req. est un peu trop haut quand on compare ».
     */
    private static final double REQ_DESCENT = 7.5;

    /**
     * La question du cout, a soixante-cinq unites — c'est la {@code pos(0, 40)} du bloc de texte,
     * qui commence a vingt-cinq.
     *
     * <p>La boite se compte en plus : elle fait douze unites et centre son glyphe, donc son texte
     * tombe cinq unites plus haut que l'ordonnee qu'on lui donne. Ces cinq-la sont ajoutes pour que
     * le glyphe atterrisse bien a soixante-cinq, juste sous la rangee d'icones qui finit a
     * soixante-quatre.
     */
    private static final double QUESTION_Y = 70.0;

    /** Combien de temps le verdict d'un apprentissage reste affiche avant que la fiche se ferme. */
    private static final double VERDICT_HOLD = 1.5;

    /**
     * Le temps laisse au serveur pour repondre avant de conclure qu'il a refuse.
     *
     * <p>Un clic de bouton fait un aller-retour : la machine ne passe pas a DEVELOPING dans la meme
     * image. Et un coup de lag peut depasser une demi-seconde — la fiche rendait alors sa question
     * alors que l'apprentissage venait de partir, et le joueur voyait l'animation s'arreter sur un
     * chiffre. Deux secondes laissent de la marge sans jamais faire attendre un vrai refus.
     */
    private static final double LEARN_START_GRACE = 2.0;

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

    /**
     * Vrai quand la console a lance un apprentissage et n'a pas encore dit son verdict.
     *
     * <p>Sans ce drapeau, la console etait remplacee des que l'aptitude arrivait : elle etait deja
     * « au repos » avant le `/learn`, donc la condition « elle a fini son demarrage » etait vraie
     * tout de suite, et le joueur ne voyait ni « Start stimulation » ni le verdict — « avec le
     * learn, on a toujours pas l'animation, elle dure a peine 1 seconde ».
     */
    private boolean consoleLearning;

    /**
     * Vrai quand un changement de categorie vient d'aboutir.
     *
     * <p>Le joueur garde sa bobine en main apres un reset — elle ne disparait qu'a l'unite — donc
     * l'ecran se rebatissait en mode {@code reset} et la console repartait de sa banniere : « a la
     * fin du reset, ca reouvre la machine, mais du coup, vu que j'ai encore des coil en main il la
     * reouvre en mode reset ». Ce drapeau la fait taire jusqu'a ce que la bobine soit rangee.
     */
    private boolean resetDone;

    /**
     * L'apprentissage en cours, tel que la fiche l'anime — ou {@code null}.
     *
     * <p>Il porte sa propre duree : c'est la machine qui dit ou en est la stimulation, et la fiche
     * la relit a chaque image comme la console relit la sienne.
     */
    @Nullable
    private Learning learning;

    /** L'instant ou la fiche d'un apprentissage se refermera d'elle-meme, ou zero. */
    private double closeAfter;

    /** L'anneau de la fiche ouverte, relu a chaque image : c'est la jauge de l'apprentissage. */
    @Nullable
    private CWidget.Ring surfaceRing;

    /**
     * Les icones de condition de la fiche ouverte, et le texte de leur survol.
     *
     * <p>L'original n'ecrivait le texte d'une condition qu'au survol de son icone — c'est le
     * {@code getHoveringWidget} de son {@code FrameEvent}. Le port ecrivait une phrase rouge par
     * condition manquante, ce qui n'etait pas le vrai mod ; ici la rangee est muette, et seule
     * l'icone survolee parle.
     */
    private final Map<CWidget, Component> conditionIcons = new HashMap<>();

    /** La ligne du survol, vide tant qu'aucune icone ne l'est. */
    @Nullable
    private CWidget hintText;

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

        // L'onglet sans fil de l'original, pose EN DERNIER : le moteur ouvre la premiere page posee,
        // et cette machine doit s'ouvrir sur son arbre. C'est par cet onglet qu'on choisit le noeud
        // qui l'alimente — l'original le donnait a toutes ses machines, celle-ci comprise.
        if (!menu.isPortable()) new CGuiWireless(screen).install();
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
            // L'original ne cachait pas l'icone : sans aptitude il montrait `icon_nocategory`, le
            // point d'interrogation. Le port la cachait, et le joueur a reclame l'image du vrai mod.
            if (logo.texture == null) logo.texture = new CWidget.DrawTexture();
            logo.transform.doesDraw = true;
            logo.texture.texture = category == null
                    ? "academy:textures/guis/icons/icon_nocategory.png"
                    : categoryIcon(category);
        }

        CWidget name = ability.child("text_abilityname");
        if (name != null && name.text != null) {
            // « N/A » est le texte de l'original quand il n'y a pas d'aptitude — c'est ce que le
            // joueur a montre sur sa capture du vrai mod.
            name.text.content = category == null
                    ? "N/A"
                    : Component.translatable(category.getDisplayKey()).getString();
        }

        CWidget level = ability.child("text_level");
        if (level != null && level.text != null) {
            // `AbilityLocalization.levelDesc` : la cle `ac.ability.level<n>`, donc « Level 0 » sans
            // aptitude. Le port ecrivait une chaine vide, et le joueur voyait un panneau a moitie nu.
            level.text.content = Component.translatable("academy.ability.level"
                    + (category == null ? 0 : data.getCategoryLevel(category))).getString();
        }

        // Le pourcentage d'experience. Le document porte « EXP 30% » : un texte d'exemple, que l'ecran
        // doit remplacer — sinon il reste la, et c'est exactement ce que le joueur a vu : « la machine
        // montre comme quoi je suis a 30% de mon niveau alors que dans le menu F4 je ne suis qu'a 3% ».
        // Le chiffre est celui de la barre juste au-dessus, donc de l'avancement du niveau en cours.
        CWidget exp = ability.child("text_exp");
        if (exp != null && exp.text != null) {
            // Sans aptitude, l'original annonce 100 % : son palier vaut zero, et un palier nul est
            // un palier franchi. C'est ce que montre sa capture — « EXP 100% » a cote de « Level 0 ».
            exp.text.content = Component.translatable("academy.ability.exp",
                    category == null ? 100 : (int) (data.getLevelProgress(category) * 100))
                    .getString();
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
            //
            // SANS APTITUDE, IL NE SE MONTRE PAS : l'original exigeait `aData.hasCategory`, et c'est
            // la console qui donne la premiere aptitude (sa commande `learn`). Le port le montrait,
            // et le joueur a vu un bouton LEARN la ou le vrai mod n'en a pas.
            boolean open = category != null
                    && data.getCategoryLevel(category) < DevelopActionLevel.MAX_LEVEL
                    && data.canLevelUp(category);
            upgrade.transform.doesDraw = open;
            if (open) onClick(upgrade, this::openLevel);
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
        // d'energie. Le libelle est dans la page ; c'est le nom du noeud, connu du seul serveur,
        // qui manquait.
        //
        // Le texte est « N/A » quand il n'y a pas de noeud, comme chez l'original — et non
        // « Not Connected », qui est le texte de l'onglet sans fil et non celui de cette ligne.
        CWidget wireless = machine.child("button_wireless");
        CWidget node = wireless == null ? null : wireless.child("text_nodename");
        if (node != null && node.text != null) {
            String name = menu.getNodeName();
            node.text.content = name.isEmpty() ? "N/A" : name;
        }

        if (menu.isPortable()) {
            machine.transform.doesDraw = false;
            CWidget left = panel.child("ui_left");
            if (left != null && left.texture != null) {
                left.texture.texture = "academy:textures/guis/ui/ui_developerleft_skilltree.png";
            }
            return;
        }

        // Le cadre du noeud ouvre l'onglet sans fil : c'est ce que faisait l'original, dont le
        // `LeftClickEvent` posait sa `WirelessPage` par-dessus l'ecran. Le port n'avait ni l'onglet
        // sur cette machine ni le moyen de changer de page depuis le code, donc le cadre ne faisait
        // rien du tout — le joueur : « si j'appuie sur le "Not Connected", ça ne me met pas la page
        // de connexion ».
        if (wireless != null) {
            onClick(wireless, () -> showPage(CGuiWireless.PAGE));
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
        consoleLearning = true;
        click(DeveloperMenu.FIRST_CATEGORY_BUTTON);
        console.enqueue(progressTask(consoleTexts().devSucc(), consoleTexts().devFail(), true));
    }
    /**
     * La sequence d'un apprentissage, jouee par la console.
     *
     * <p>Elle est la meme que celle de la commande {@code learn} : le texte de debut, la ligne de
     * progression, puis le verdict. C'est ce qui permet a un {@code /learn} tape dans le chat de
     * montrer la meme animation qu'un {@code learn} tape dans la console.
     */
    private void playLearnSequence() {
        DeveloperConsole console = this.console;
        if (console == null) return;

        console.enqueue(console.print(consoleTexts().devBegin()));
        console.enqueue(console.print(consoleTexts().progress().apply(0)));
        consoleLearning = true;
        console.enqueue(progressTask(consoleTexts().devSucc(), consoleTexts().devFail(), true));
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
        consoleLearning = true;
        click(DeveloperMenu.RESET_BUTTON);
        console.enqueue(progressTask(consoleTexts().resetSucc(), consoleTexts().resetFail(), false));
    }
    /**
     * Le temps laisse a la premiere aptitude avant que la console ne dise son verdict.
     *
     * <p>Elle se donne tout de suite, sans stimulation : sans ce delai la tache se terminerait a
     * l'image suivante, et le joueur ne verrait ni la ligne de progression ni le verdict. Une
     * seconde suffit a lire les deux lignes, et c'est le temps que l'original mettait a jouer sa
     * sequence.
     */
    private static final double MIN_FIRST_CATEGORY = 1.0;

    /**
     * Le texte d'un pourcentage tel qu'il s'ecrit dans la console : le nombre et son signe.
     *
     * <p>Le signe vient de la phrase de la langue ({@code Progress: %s%%}), donc il n'est pas dans
     * {@link DeveloperConsole#percent} — mais la tache doit savoir combien de caracteres elle a
     * ecrits pour les effacer, et c'est ce texte-la qu'elle a ecrit.
     */
    private static String progressText(int value) {
        return DeveloperConsole.percent(value) + "%";
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
     *
     * <p>Ce raccourci ne vaut QUE pour la premiere aptitude : un changement de categorie laisse une
     * categorie en place du debut a la fin, donc s'y fier ferait dire « reussi » a l'instant meme
     * ou la commande part.
     *
     * @param firstCategory vrai pour {@code learn}, faux pour {@code reset}
     */
    private DeveloperConsole.Task progressTask(String success, String failure,
                                               boolean firstCategory) {
        return new DeveloperConsole.Task() {
            /**
             * Ce que la ligne porte deja, pour l'effacer exactement.
             *
             * <p>Il vaut {@code 0%} et non vide : l'appelant vient d'ecrire la ligne de progression
             * a zero, et une valeur vide faisait donc ecrire le premier pourcentage A LA SUITE du
             * zero — le joueur a vu « Progress: 0%49% » sur sa capture. Le signe fait partie du
             * texte efface, puisque la phrase de la langue le pose deja.
             */
            private String written = progressText(0);

            /** L'instant ou la tache a commence : voir {@link #MIN_FIRST_CATEGORY}. */
            private double born = -1;

            @Override
            public void begin(double now) {
                born = now;
            }

            @Override
            public boolean isFinished(double now) {
                if (menu.getState() == DevelopProgress.DevState.FAILED
                        || menu.getState() == DevelopProgress.DevState.DONE) return true;
                // La machine mene l'apprentissage : c'est elle qui decide de la fin, et son etat
                // passe a DONE ou FAILED. Le delai minimum ne sert qu'au don immediat, celui d'une
                // machine sans energie — sans lui, la tache se terminerait a l'image suivante et
                // le joueur ne verrait ni la ligne de progression ni le verdict.
                if (menu.isDeveloping()) return false;
                return firstCategory && category() != null
                        && now - born >= MIN_FIRST_CATEGORY;
            }

            @Override
            public void update(double now) {
                if (console == null) return;
                // On efface EXACTEMENT ce qu'on a ecrit, et pas un nombre fixe de caracteres :
                // « 1% » fait deux caracteres, « 100% » en fait quatre, et un `\b\b\b` fixe
                // mordait donc dans la phrase precedente des que le chiffre changeait de taille.
                //
                // Le chiffre est celui de la MACHINE quand elle travaille : c'est elle qui paie
                // l'energie et compte les stimulations, donc c'est elle qui sait ou en est
                // l'apprentissage. Le temps ecoule ne sert qu'au don immediat, ou il n'y a aucune
                // progression a montrer.
                int percent = menu.isDeveloping()
                        ? (int) (menu.getProgress() * 100)
                        : (int) Math.min(100, (now - born) / MIN_FIRST_CATEGORY * 100);
                String next = progressText(percent);
                if (next.equals(written)) return;
                console.output("\b".repeat(written.length()) + next);
                written = next;
            }

            @Override
            public void finish() {
                if (console == null) return;
                boolean done = menu.getState() == DevelopProgress.DevState.DONE
                        || firstCategory && category() != null;
                console.outputln();
                console.output(done ? success : failure);
                // Un changement de categorie abouti ne rouvre pas la console du reset : le joueur
                // garde sa bobine en main, et la machine repartait donc en mode reset a chaque
                // reconstruction.
                if (done && !firstCategory) resetDone = true;
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
        if (holdsCoil() && !resetDone) {
            buildConsole(area, "reset");
            tree.note(null, data);
            return;
        }

        // L'aptitude vient d'arriver alors que la console etait encore la — c'est le cas d'un
        // `/learn` tape dans le CHAT, qui ne passe pas par la console. Elle joue alors sa propre
        // sequence, comme si on avait tape `learn` dedans : sans cela, l'arbre remplacait la
        // console a l'instant du paquet et le joueur ne voyait rien — « l'animation a l'air d'etre
        // presente, mais je ne peux pas la voir parce que la machine se reouvre directement ».
        if (console != null && "learn".equals(consoleMode) && !consoleLearning) {
            playLearnSequence();
            buildConsole(area, "learn");
            tree.note(null, data);
            return;
        }

        // L'aptitude est arrivee, mais la console joue encore : elle garde la place jusqu'a la fin
        // de son animation. Sans cela, taper `/learn` dans le chat faisait disparaitre la banniere
        // au premier paquet de synchronisation — le joueur : « quand je tape /learn, j'apprends
        // bien un pouvoir mais je ne vois pas l'animation du terminal qui est cense etre la ».
        //
        // La condition est la FIN DU DEMARRAGE, et non « la file est vide » : la banniere et la
        // sequence de nombres sont deux taches, et entre les deux la file est vide une image — la
        // console cedait donc la place au milieu de son animation, ce que le joueur voyait comme
        // « elle dure a peine 1 seconde ».
        if (console != null && (consoleLearning || !console.startupFinished())) {
            buildConsole(area, consoleMode == null ? "learn" : consoleMode);
            tree.note(null, data);
            return;
        }

        // L'aptitude est arrivee, ou la bobine est rangee : la console a fini son office, et son
        // texte ne doit pas rester derriere l'arbre.
        console = null;
        consoleBox = null;
        consoleMode = null;
        consoleLearning = false;
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
        // Le nom du noeud se relit a CHAQUE image : il vient du serveur par la balise du bloc, qui
        // arrive quand elle arrive, et le panneau n'est rebati que lorsque l'arbre change. Sans
        // cette relecture, le joueur branchait sa machine et « Current Node » restait sur « N/A »
        // jusqu'a ce qu'autre chose force une reconstruction — « le nom du current node se met a
        // jour tres lentement ».
        refreshNodeName();
        refreshEnergy();
        animateLearning(mouseX, mouseY);
        veil.animate();
        tree.frame(ClientAbilityData.get(), mouseX, mouseY, width, height, veil.opened());
    }

    /**
     * L'animation de la fiche d'une competence : sa jauge, puis sa fermeture.
     *
     * <p>Deux choses s'y font, et c'est tout ce que la fiche demande a chaque image : la question
     * du cout devient la progression de la machine ({@link Learning#animate}), puis, le verdict
     * passe, la fiche se referme d'elle-meme.
     */
    private void animateLearning(double mouseX, double mouseY) {
        // La fiche s'est refermee : elle n'a plus rien a animer, et son anneau ne doit pas rester
        // accroche au prochain apprentissage.
        if (!veil.opened()) {
            learning = null;
            closeAfter = 0;
            surfaceRing = null;
            conditionIcons.clear();
            hintText = null;
            return;
        }

        if (learning != null) {
            learning.animate();
            // La fiche se referme au verdict, pas avant : l'original laissait le joueur cliquer,
            // mais la console, elle, enchainait toute seule, et deux animations qui ne finissent
            // pas ensemble laissent le joueur devant un ecran fige.
            if (closeAfter > 0 && net.minecraft.Util.getMillis() / 1000.0 >= closeAfter) {
                closeAfter = 0;
                learning = null;
                veil.beginClose();
            }
        }

        // L'anneau de la fiche est relu ici, comme la barre d'energie : il se remplit a chaque
        // image pendant un apprentissage, et la fiche n'est pas rebatie pour autant. Il montre le
        // meme chiffre que la question, donc le meme avancement monotone.
        if (surfaceRing != null) {
            surfaceRing.progress = learning == null ? 0.0 : learning.shown();
            surfaceRing.lit = learning != null && learning.isDone();
        }

        // L'echec rend la question et son bouton : c'est ce qui permet de reessayer sans rouvrir la
        // fiche. Le texte du verdict a eu son instant, la question reprend sa place — et l'anneau
        // repart de zero, puisque plus rien ne se developpe.
        if (learning != null && learning.isDone() && closeAfter == 0
                && learning.sinceVerdict() >= VERDICT_HOLD) {
            learning.restore();
            learning = null;
            if (surfaceRing != null) surfaceRing.progress = 0;
        }

        refreshHint(mouseX, mouseY);
    }

    /**
     * Le texte du survol d'une icone de condition.
     *
     * <p>Il se relit a chaque image, comme tout le reste de la fiche : la souris bouge, et le
     * moteur ne previent pas quand elle passe d'un widget a l'autre. Vide, la ligne ne dessine
     * rien — c'est ce qui la rend invisible au repos.
     */
    private void refreshHint(double mouseX, double mouseY) {
        if (hintText == null || hintText.text == null) return;

        String content = "";
        for (Map.Entry<CWidget, Component> entry : conditionIcons.entrySet()) {
            if (!entry.getKey().isPointWithin(mouseX, mouseY)) continue;
            content = entry.getValue() == null ? "" : entry.getValue().getString();
            break;
        }
        if (!content.equals(hintText.text.content)) hintText.text.content = content;
    }

    /** Le nom du noeud lie, relu sur le menu a chaque image. */
    private void refreshNodeName() {        CWidget page = pageWindow(PAGE);
        CWidget panel = page == null ? null : page.child("parent_left");
        CWidget machine = panel == null ? null : panel.child("panel_machine");
        CWidget wireless = machine == null ? null : machine.child("button_wireless");
        CWidget node = wireless == null ? null : wireless.child("text_nodename");
        if (node == null || node.text == null) return;

        String name = menu.getNodeName();
        String content = name.isEmpty() ? "N/A" : name;
        if (!content.equals(node.text.content)) node.text.content = content;
    }

    /**
     * La barre d'energie, relue a chaque image.
     *
     * <p>Elle n'etait remplie qu'a la reconstruction de la page, donc elle ne bougeait pas pendant
     * un apprentissage : le joueur en a conclu que « la machine n'utilise tout simplement pas
     * d'electricite quand j'apprend un pouvoir ». Elle en depense pourtant a chaque tick — c'est
     * juste que rien ne le montrait.
     */
    private void refreshEnergy() {
        CWidget page = pageWindow(PAGE);
        CWidget panel = page == null ? null : page.child("parent_left");
        CWidget machine = panel == null ? null : panel.child("panel_machine");
        CWidget power = machine == null ? null : machine.child("progress_power");
        if (power == null || power.bar == null) return;

        int max = Math.max(1, menu.getMaxEnergyStored());
        power.bar.progress = Math.max(0.0, Math.min(1.0, (double) menu.getEnergyStored() / max));
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
        // La console appartient a la page de l'arbre : sur l'onglet sans fil, elle n'a rien a dire.
        if (!PAGE.equals(currentPageId())) return;

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
            consoleLearning = false;
            CWidget page = pageWindow(PAGE);
            if (page != null) {
                // Les DEUX panneaux de gauche se refont ici, et pas seulement la zone de droite :
                // `refresh` ne les rebatira plus, puisque l'arbre vient de noter sa signature et
                // qu'elle ne bougera plus. Le joueur voyait donc l'arbre de sa nouvelle aptitude
                // avec, a cote, « N/A » et le point d'interrogation de la precedente — « il ne met
                // pas a jour le nom de ma competence ni l'image du pouvoir ».
                buildAbilityPanel(page);
                buildMachinePanel(page);
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

        // La bobine rangee : le changement de categorie est termine, et la console du reset peut
        // reparler si le joueur la reprend. C'est ici, et non dans `buildAptitude`, parce que
        // celui-ci n'est appele que sur une reconstruction — ranger la bobine n'en declenche pas.
        if (!holdsCoil()) resetDone = false;

        // La console a son propre etat : elle change de mode quand le joueur prend ou range la
        // bobine, et l'arbre ne voit rien de tout cela — sa signature ne parle que de l'aptitude.
        //
        // Un reset abouti eteint la console du reset meme si la bobine est encore en main : c'est
        // ce qui evite qu'elle se rouvre en boucle apres le changement de categorie.
        String mode = category == null ? "learn" : holdsCoil() && !resetDone ? "reset" : null;

        // Tant qu'une console est a l'ecran, c'est ELLE qui decide : la signature de l'arbre ne
        // bouge pas (elle vaut « pas d'arbre »), donc la consulter rebatirait la page a chaque
        // image. Seul un changement de mode — ou la fin de son animation — demande un repose.
        boolean rebuild;
        if (mode != null) {
            rebuild = !mode.equals(consoleMode);
        } else if (console != null) {
            if (consoleLearning) {
                // Un apprentissage est en cours : la console reste jusqu'a son verdict.
                rebuild = console.rebuildRequested();
            } else if ("learn".equals(consoleMode)) {
                // L'aptitude vient d'arriver alors que la console etait encore la : c'est un
                // `/learn` tape dans le CHAT. On lui fait jouer la sequence, comme si la commande
                // avait ete tapee dedans — sinon l'arbre la remplace a l'instant du paquet.
                rebuild = true;
            } else {
                // La console cede la place quand elle a fini de jouer son demarrage.
                rebuild = console.startupFinished();
            }
        } else {
            rebuild = !tree.unchanged(category, data);
        }

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
     * <p>C'est le {@code skillViewArea} de l'original, dans son ordre : le disque et son icone au
     * milieu, le nom en gras dessous, puis « Skill not learned », puis la rangee de conditions sous
     * le mot « Req. », puis la question du cout, puis le bouton.
     *
     * <h2>Les conditions sont des ICONES</h2>
     *
     * <p>Le port ecrivait une phrase rouge par condition manquante. L'original n'en ecrivait
     * aucune : il posait une rangee de petites icones de quatorze unites, chacune <b>grisee</b> tant
     * que sa condition n'etait pas remplie, et son texte n'apparaissait qu'au survol. Le joueur a
     * reclame les deux a la fois : « l'affichage des pouvoirs debloquables n'est pas du tout pareil
     * que dans le vrai mod ».
     *
     * <h2>La fiche reste ouverte pendant l'apprentissage</h2>
     *
     * <p>Elle ne se referme plus au clic sur le bouton : elle devient une <b>jauge</b>. L'anneau de
     * l'icone se remplit de la progression que la machine annonce, au meme rythme qu'elle, et la
     * fiche se referme toute seule au verdict. Sans cela le joueur voyait l'animation de la console
     * et jamais celle de la competence — « on n'a pas l'animation lorsqu'on apprend la competence ».
     */
    private void openSkill(Skill skill) {
        AbilityData data = ClientAbilityData.get();
        boolean learned = data.isSkillLearned(skill);
        double textOffsetY = -5.0;

        CWidget content = new CWidget("skill");
        content.add(surfaceIcon(skill));

        content.add(text(skillName(skill),
            TITLE_Y + TITLE_FONT / 2 + textOffsetY,
            255, 255, 255, TITLE_FONT));
        if (learned) {
            content.add(text(Component.translatable("academy.developer.skill_exp",
                    (int) (data.getSkillExp(skill) * 100)).getString(),
                    SUBTITLE_Y + EXP_FONT / 2 + textOffsetY, 161, 225, 255,
                    EXP_FONT));
            // LA DESCRIPTION N'EST LA QUE POUR UNE COMPETENCE APPRISE. C'est l'ordre de
            // l'original : « Skill not learned » pour une autre, puis ses conditions, puis sa
            // question. Le port la montrait dans les deux cas, et le joueur a tranche : « pour
            // apprendre une competence il affiche la description alors qu'il ne devrait pas ».
            double y = DESCRIPTION_Y + DESC_FONT / 2 + textOffsetY;
            for (String line : wrap(skill.getDescription().getString(), DESCRIPTION_WIDTH)) {
                content.add(text(line, y, 255, 255, 255, DESC_FONT));
                y += 10;
            }
            open(content);
            return;
        }

        content.add(text(Component.translatable("academy.developer.not_learned").getString(),
            SUBTITLE_Y + TEXT_FONT / 2 + textOffsetY, 255, 85, 85, TEXT_FONT));

        // La rangee des conditions, puis la question du cout : les deux a leur place de l'original,
        // et non poussees sous une description qui n'existe pas dans ce cas.
        conditions(content, skill, data, CONDITION_Y + textOffsetY);

        // « Learn? (Estm. Consumption: 3500 IF) », en BLANC a deux tiers d'opacite et en CORPS DIX :
        // c'est {@code foSkillUnlearned2}, {@code FontOption(10, CENTER, 0xaaffffff)}. Le port
        // l'ecrivait en corps neuf et en gris-bleu, et le joueur a vu les deux : « la couleur du
        // texte avec Learn? n'est pas de la bonne couleur », puis « pas assez grand ».
        CWidget question = textAlpha(learnQuestion(skill), QUESTION_Y + textOffsetY,
            255, 255, 255, 170);
        question.text.fontSize = TEXT_FONT;
        content.add(question);

        if (canLearn(skill)) {
            content.add(button(() -> startLearning(skill, question), BUTTON_Y, false));
        }
        open(content);
    }

    /**
     * La rangee des conditions, telle que l'original la dessinait.
     *
     * <p>Le mot « Req. » a gauche, puis une icone par condition a afficher, ecartees de seize
     * unites — celles qui ne se montrent pas (le niveau du joueur) sont sautees. Chacune est grisee
     * tant qu'elle n'est pas remplie, et son infobulle ne s'ecrit qu'au survol.
     *
     * @return l'ordonnee ou poser la ligne suivante
     */
    private double conditions(CWidget content, Skill skill, AbilityData data, double y) {
        List<LearningCondition> shown = new ArrayList<>();
        for (LearningCondition condition : skill.getConditions()) {
            if (!condition.shouldDisplay()) continue;
            if (condition.getIcon() == null) continue;
            shown.add(condition);
        }
        if (shown.isEmpty()) return y;

        // « Req. » : deux unites avant le bord gauche de la rangee, et a DROITE de sa boite — c'est
        // l'alignement de l'original, qui grandit vers la gauche au lieu de pousser sur les icones.
        //
        // Le decalage se calcule sur le BORD de la boite, et non sur son centre : elle fait
        // {@code TEXT_WIDTH} et le moteur la pose centree, donc son bord droit tombe a
        // {@code TEXT_WIDTH / 2} du centre. Le port l'ajoutait au lieu de le retirer, et le mot
        // partait hors de la page.
        //
        // SA HAUTEUR, ENFIN. L'original dessine ce mot a {@code y = 28} de son bloc de texte, et ses
        // icones a {@code y = 25} : le mot tombe donc trois unites SOUS le haut des icones, c'est a
        // dire a leur milieu. Notre boite centre son glyphe dans douze unites, donc elle se pose un
        // et demi plus haut que son texte : {@code + 1,5} la remet a sa place.
        double length = CONDITION_STEP * shown.size();
        CWidget label = textAlpha(Component.translatable("academy.developer.req").getString(),
                y + REQ_DESCENT, 255, 255, 255, 170);
        label.text.fontSize = DESC_FONT;
        label.text.align = "RIGHT";
        label.transform.x = -length / 2 - CONDITION_LABEL_GAP - TEXT_WIDTH / 2;
        content.add(label);

        conditionIcons.clear();
        double x = -length / 2;
        for (LearningCondition condition : shown) {
            boolean met = developerSatisfies(condition, data, skill);
            CWidget icon = conditionIcon(condition, !met);
            // LE COIN HAUT-GAUCHE, et non le centre. L'original pose ses icones par {@code pos()},
            // qui est un coin ; {@code centred} les pose autour de leur milieu. Une demi-taille plus
            // loin, leur bord gauche mordait donc sur « Req. » — le joueur a vu « les icones
            // deborde dessus ».
            icon.transform.x = x + CONDITION_ICON_SIZE / 2;
            icon.transform.y = y + CONDITION_ICON_SIZE / 2;
            content.add(icon);
            // Le survol d'une icone ecrit son texte a cote de la rangee : c'est le {@code
            // getHintText} de l'original, et le seul endroit ou la condition se dit en toutes
            // lettres. Ce texte est pose une fois, vide, et rempli a chaque image.
            conditionIcons.put(icon, condition.getHint());
            x += CONDITION_STEP;
        }

        hintText = text("", y + REQ_DESCENT, 238, 255, 255, DESC_FONT);
        hintText.text.align = "LEFT";
        hintText.transform.x = length / 2 + 3 + TEXT_WIDTH / 2;
        content.add(hintText);

        return y + CONDITION_ICON_SIZE;
    }

    /** La condition est-elle remplie ? La machine du joueur repond a celle qui la concerne. */
    private boolean developerSatisfies(LearningCondition condition, AbilityData data, Skill skill) {
        // Une condition qui parle de la machine se repond avec la machine, les autres sans elle :
        // c'est la regle de `needsDeveloper`, posee pour que l'application du terminal ne plante
        // plus (voir {@code SkillTreeScreen}).
        return condition.accepts(data, skill,
                condition.needsDeveloper() ? menu.getDeveloperType() : null);
    }

    /** L'icone d'une condition a sa place, grisee quand elle n'est pas remplie. */
    private static CWidget conditionIcon(LearningCondition condition, boolean grey) {
        CWidget icon = centred("condition", CONDITION_ICON_SIZE, CONDITION_ICON_SIZE);
        icon.texture = new CWidget.DrawTexture();
        icon.texture.texture = condition.getIcon().toString();
        icon.texture.grey = grey;
        // Ces images font trente-deux pixels pour un dessin de quatorze : lues au plus proche
        // voisin, elles sortent en gros pixels.
        CGuiShaders.linear(condition.getIcon());
        return icon;
    }

    /**
     * « Learn? (Estm. Consumption: 3500 IF) » — le texte de la fiche avant de lancer l'apprentissage.
     *
     * <p>C'est le {@code learn_question} de l'original, avec sa consommation estimee : les
     * stimulations que la competence demande, fois le prix d'une stimulation de cette machine.
     */
    private String learnQuestion(Skill skill) {
        return Component.translatable("academy.developer.learn_question",
                (int) Math.round(skillCost(skill))).getString();
    }

    /**
     * Ce que coute l'apprentissage d'une competence, en Imag.
     *
     * <p>C'est le {@code getEstimatedConsumption} de l'original : les stimulations que la
     * <b>competence</b> demande — et non celles d'une montee de niveau — fois le prix d'une
     * stimulation de cette machine. Le port lisait le cout de la categorie, ce qui annoncait le
     * prix d'un palier pour une competence.
     */
    private double skillCost(Skill skill) {
        return menu.getDeveloperType().getTotalCost(skill.getLearningStims());
    }

    /**
     * Lance l'apprentissage et anime la fiche jusqu'au verdict.
     *
     * <p>C'est le clic de l'original : la fiche cesse de se fermer, la machine recoit la demande,
     * et la question du cout est reecrite sur place — « Progress: 43% » pendant, puis le verdict.
     * L'anneau de l'icone suit la meme progression, et la fiche se referme d'elle-meme a la fin.
     */
    private void startLearning(Skill skill, CWidget question) {
        click(DeveloperMenu.skillButton(skill));
        learning = new Learning(skill, question);
    }

    /**
     * La fiche d'un apprentissage en cours : sa question reecrite, son anneau, et son verdict.
     *
     * <p>C'est le bloc {@code DevState} de l'original, relu a chaque image. Le port lancait
     * l'apprentissage et refermait la fiche dans le meme clic : le joueur ne voyait ni la
     * progression ni le succes — « on n'a pas l'animation lorsqu'on apprend la competence ».
     */
    private final class Learning {
        private final Skill skill;
        private final CWidget question;
        private final double born = net.minecraft.Util.getMillis() / 1000.0;
        private String written;

        /**
         * Le dernier avancement VU, de zero a un.
         *
         * <p>Il ne redescend jamais : la machine remet son compteur a zero quand elle a fini — ou
         * quand elle echoue — donc lire son avancement brut ferait sauter la jauge de quarante pour
         * cent a rien, et le joueur verrait l'animation « s'arreter ». La jauge garde donc son
         * dernier point, et c'est le verdict qui la remplace.
         */
        private double shown;

        private boolean started;
        private boolean done;

        /** L'instant du verdict d'echec, ou zero : c'est lui qui rend la question a sa place. */
        private double failedAt;

        Learning(Skill skill, CWidget question) {
            this.skill = skill;
            this.question = question;
            this.written = question.text == null ? "" : question.text.content;
        }

        /** Ce qui change a chaque image : le chiffre, puis le verdict. */
        void animate() {
            if (done || question == null || question.text == null) return;

            // L'avancement se lit AVANT l'etat : la machine remet son compteur a zero en passant a
            // DONE, donc le lire apres rendrait zero sur la derniere image.
            if (menu.isDeveloping()) {
                started = true;
                shown = Math.max(shown, menu.getProgress());
                String next = Component.translatable("academy.developer.skill_progress",
                        (int) (shown * 100)).getString();
                if (!next.equals(written)) {
                    question.text.content = next;
                    written = next;
                }
                return;
            }

            DevelopProgress.DevState state = menu.getState();
            if (state == DevelopProgress.DevState.DONE && started) {
                finish(true, Component.translatable("academy.developer.skill_successful")
                        .getString());
                return;
            }
            if (state == DevelopProgress.DevState.FAILED && started) {
                // La machine dit POURQUOI elle a echoue, et le cas courant est l'energie : son
                // tampon n'a pas suivi la stimulation, et elle s'arrete la ou elle en etait. C'est
                // exactement ce que le joueur a vu — « l'animation ne dure pas pendant tout le
                // temps, elle s'arrete environ vers 40% » : la jauge montait puis se figeait, sans
                // un mot. L'original avait deux phrases pour cela, et les voici.
                String reason = menu.getEnergyStored() <= 0
                        ? Component.translatable("academy.developer.skill_no_energy").getString()
                        : Component.translatable("academy.developer.skill_failed").getString();
                finish(false, reason);
                return;
            }
            // Rien n'a demarre — le serveur a refuse, ou la machine s'est arretee sans verdict.
            //
            // On rend la fiche a son etat d'avant au lieu de la refermer : la question du cout
            // revient, et le bouton est toujours la pour reessayer. Sans cela le joueur restait
            // devant « Progress: 0% » sans rien pour repartir.
            if (net.minecraft.Util.getMillis() / 1000.0 - born > LEARN_START_GRACE) {
                learning = null;
                shown = 0;
                question.text.content = learnQuestion(skill);
            }
        }

        /**
         * Le verdict.
         *
         * <p>Un SUCCES referme la fiche, une fois le texte lu. Un ECHEC ne la referme pas : il rend
         * la question et son bouton, parce qu'un echec se rejoue — la machine n'a pas eu assez
         * d'energie, ou la condition a change en route. L'original laissait le joueur cliquer pour
         * fermer ; le port refermait tout seul, sur un refus que le joueur n'avait pas eu le temps
         * de lire.
         */
        private void finish(boolean success, String verdict) {
            done = true;
            if (question.text != null) question.text.content = verdict;
            if (success) {
                closeAfter = net.minecraft.Util.getMillis() / 1000.0 + VERDICT_HOLD;
            } else {
                // Un echec se rejoue : la fiche reste ouverte, et la question reprend sa place une
                // fois le verdict lu. Le bouton n'a jamais quitte l'ecran, donc il n'y a rien a
                // refaire pour reessayer.
                failedAt = net.minecraft.Util.getMillis() / 1000.0;
            }
        }

        private double sinceVerdict() {
            return net.minecraft.Util.getMillis() / 1000.0 - failedAt;
        }

        /** Remet la question du cout a sa place, apres un verdict d'echec. */
        void restore() {
            if (question != null && question.text != null) {
                question.text.content = learnQuestion(skill);
            }
        }

        /** L'avancement montre, de zero a un : il ne redescend jamais. */
        double shown() {
            return shown;
        }

        boolean isDone() {
            return done;
        }
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
        if (menu.getEnergyStored() < skillCost(skill)) return false;
        if (skill.getLevel() > data.getCategoryLevel(skill.getCategory())) return false;
        return LearningHelper.canLearn(data, skill, menu.getDeveloperType());
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
     * Le meme, qui peut laisser la fiche ouverte.
     *
     * <p>C'est ce que demande l'apprentissage d'une competence : la fiche ne se referme pas au clic,
     * elle devient la jauge de l'operation — c'est le {@code canClose = false} de l'original, et
     * sans lui le joueur ne voyait jamais la progression ni le verdict.
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

    /**
     * Le disque d'une surface : le fond, l'icone par-dessus, et l'anneau qui les enferme.
     *
     * <p>C'est le {@code drawActionIcon} de l'original — cinquante unites de fond, vingt-sept
     * d'icone — avec son anneau de progression. Une competence non apprise y porte l'anneau au
     * repos ; son apprentissage le remplit. L'anneau est toujours pose, meme a progression nulle :
     * c'est lui qui donne au disque son cerne, et l'original le dessinait dans tous les cas.
     */
    private CWidget surfaceIcon(@Nullable Skill skill) {
        CWidget icon = centred("icon", SURFACE_BACK_SIZE, SURFACE_BACK_SIZE);
        icon.texture = new CWidget.DrawTexture();
        icon.texture.texture = SURFACE_BACK;

        CWidget.Ring ring = new CWidget.Ring();
        // L'anneau du disque d'une fiche fait la taille du disque, et non trente et un comme celui
        // d'un noeud de l'arbre : cinquante unites, la taille de son fond.
        ring.size = SURFACE_BACK_SIZE;
        ring.progress = 0;
        surfaceRing = ring;
        icon.ring = ring;

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
     * Le meme, avec une opacite.
     *
     * <p>C'est la {@code foSkillUnlearned2} de l'original : un blanc a {@code 0xAA}, et non une
     * teinte grise. La boite porte ses quatre canaux, opacite comprise.
     */
    private static CWidget textAlpha(String content, double y, int red, int green, int blue,
                                     int alpha) {
        CWidget box = text(content, y, red, green, blue, DESC_FONT);
        box.text.alpha(alpha);
        return box;
    }

    /**
     * Un bout de texte aligne a droite de son point d'ancrage.
     *
     * <p>C'est ce qu'il faut pour « Req. » : l'original le posait a {@code -len/2 - 2} avec un
     * alignement a droite, donc son dernier caractere finit deux unites avant la rangee d'icones.
     * Centre, il aurait chevauche la premiere icone des qu'une condition de plus apparaissait.
     */
    private static CWidget textRight(String content, double x, double y, int red, int green,
                                     int blue, double size) {
        CWidget box = text(content, y, red, green, blue, size);
        box.text.align = "RIGHT";
        box.transform.x = x + TEXT_WIDTH / 2;
        return box;
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
        // L'onglet sans fil se referme au premier clic qui ne prend rien : c'est la `Cover` de
        // l'original, qui posait sa page par-dessus l'ecran et la retirait au clic. Sans cela, le
        // joueur n'aurait aucun moyen de revenir a son arbre — cette machine n'a pas d'onglets.
        if (button == 0 && CGuiWireless.PAGE.equals(currentPageId())) {
            showPage(PAGE);
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
     *
     * <p>Et la touche d'inventaire ne ferme PAS l'ecran tant que la console attend sa frappe :
     * c'est la regle de l'original, dont le {@code CGuiScreenContainer} ne laissait passer que
     * l'echappement quand un champ avait le clavier. Sans elle, taper un mot qui contient un « e »
     * fermait la machine — le joueur : « appuyer sur la touche E me fait ressortir de la machine ».
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // La console n'a le clavier que sur SA page : sur l'onglet sans fil, ses champs a elle
        // prennent la main, et la console ne doit rien avaler.
        if (console != null && PAGE.equals(currentPageId())) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
                console.backspace();
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                    || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                console.submit();
                return true;
            }
            // Tout le reste est avale : la console a le clavier, comme un champ de saisie. Sans
            // cela, la touche d'inventaire (E par defaut) ferme l'ecran au premier caractere.
            if (keyCode != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Une lettre tapee va dans la ligne de la console. */
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (console != null && PAGE.equals(currentPageId())
                && net.minecraft.SharedConstants.isAllowedChatCharacter(codePoint)) {
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
