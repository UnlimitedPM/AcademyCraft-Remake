package cn.academy.ability.client;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.network.LocationTeleportPacket;
import cn.academy.ability.teleporter.LocationMark;
import cn.academy.ability.teleporter.LocationTeleportSkill;
import cn.academy.ability.teleporter.TeleporterCategory;
import cn.academy.client.gui.AcademyFonts;
import cn.academy.client.gui.cgui.CGuiScreen;
import cn.academy.client.gui.cgui.CWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * La liste des endroits marques : l'ecran {@code loctele_new} de l'original, conduit par son moteur.
 *
 * <p>Ce n'est plus un dessin a la main : c'est le <b>document</b> de l'original, lu par le petit
 * moteur CGui repris avec lui, et la logique que son {@code Gui} executait autour — un panneau qui
 * s'ouvre, des lignes qui apparaissent l'une apres l'autre, un champ de saisie, et l'explication de
 * ce que chaque marque coute. C'est la reponse a la question du joueur : « n'y aurait-il pas moyen
 * de creer ce qui est necessaire de l'ancien moteur pour ca ? » — oui, et c'est ceci.
 *
 * <h2>Ce que l'original faisait de chaque ligne</h2>
 *
 * <p>Le fichier ne porte qu'un <b>modele</b> de ligne, cache ; l'ecran en prend une copie par
 * marque, y met son nom, et l'accroche a la liste. La ligne qui ajoute est le second modele, une
 * seule fois, en bas. Chaque ligne s'allume quand la souris la survole, et son explication se pose
 * alors contre elle, a gauche.
 *
 * <h2>Ce que l'ecran ne decide pas</h2>
 *
 * <p>Il grise ce qui ne marchera pas et dit pourquoi, mais il ne fait que prevenir : c'est le
 * serveur qui paie et qui deplace, et lui seul connait la reserve au point pres.
 */
@OnlyIn(Dist.CLIENT)
public class LocationTeleportScreen extends CGuiScreen {

    /** Le document de l'original : {@code assets/academy/guis/loctele_new.xml}. */
    private static final String DOCUMENT = "loctele_new";

    /** La hauteur du panneau dans le fichier. */
    private static final double MENU_HEIGHT = 530.0;

    /** Une ligne : les quatre-vingts unites du fichier, plus les deux de l'{@code ElementList}. */
    private static final double ROW_HEIGHT = 80.0;
    private static final double ROW_SPACING = 2.0;

    /** L'ouverture du panneau, puis le decalage et le fondu des lignes, en secondes. */
    private static final double OPEN_SECONDS = 0.4;
    private static final double ROW_STEP = 0.06;
    private static final double ROW_FADE = 0.2;

    /** L'opacite d'une ligne au repos et au survol : 25 et 102 sur 255, comme le fichier. */
    private static final double TINT_IDLE = 25.0 / 255.0;
    private static final double TINT_HOVER = 102.0 / 255.0;

    /** Une icone au repos, puis survolee : les 0,7 et 1,0 du fichier. */
    private static final double ICON_IDLE = 0.7;
    private static final double ICON_HOVER = 1.0;

    /** Le texte du fichier, et son gris quand la marque ne mene nulle part. */
    private static final int TEXT_ON = 0xC1CFD5;
    private static final int TEXT_OFF = 0xA2A2A2;

    // ------------------------------------------------------------------
    // L'explication : quatre lignes, et la boite qui les porte
    // ------------------------------------------------------------------

    /**
     * Combien de lignes une explication peut porter : la dimension, les coordonnees, le prix, et le
     * refus s'il y en a un. C'est le decoupage de l'original — sa {@code MessageTab} recevait une
     * ligne par renseignement — et le joueur l'a redemande le 22/10 : « la dimension et les
     * coordonnees devraient etre sur des lignes separees ».
     */
    private static final int HINT_LINES = 4;

    /** La hauteur d'une ligne et la marge de la boite : les 42 et 20 de {@code MessageTab}. */
    private static final double HINT_LINE = 42.0;
    private static final double HINT_MARGIN = 20.0;

    /** La taille de la police des explications : le {@code textSize} de {@code MessageTab}. */
    private static final double HINT_FONT = 40.0;

    /** L'espace entre le panneau et la boite d'explication, en unites du document. */
    private static final double INFO_GAP = 5.0;

    // ------------------------------------------------------------------
    // Le champ d'ajout
    // ------------------------------------------------------------------

    /**
     * Le texte du champ tant qu'on n'y ecrit pas : celui du document, mot pour mot.
     *
     * <p>Le port y mettait une traduction a lui ({@code name_hint}), si bien que la case ne
     * ressemblait pas a celle de l'original : « la case qui permet de creer un nouveau point n'a pas
     * le meme texte ». C'est ce « Add... »-la que l'original posait, et qu'il remettait a la fin de
     * chaque saisie.
     */
    private static final String ADD_TEXT = "Add...";

    /** L'opacite du champ : 0,4 au repos et 0,8 quand il a le clavier, comme l'original. */
    private static final double ADD_IDLE = 0.4;
    private static final double ADD_FOCUS = 0.8;

    private final Category category;
    private final Skill skill;

    private final long openedAt = EffectClock.now();

    private CWidget list;
    private CWidget info;
    private CWidget field;

    /**
     * Les deux modeles du fichier : la ligne d'une marque, et celle qui ajoute.
     *
     * <p>Il faut les GARDER : ils sont enfants de la liste, et refaire la liste commence par la
     * vider. Les redemander par leur nom apres coup rendait {@code null} et faisait tomber le jeu a
     * la premiere marque creee — le joueur l'a vu le 22/10 : « quand j'ai cree un nouveau marqueur,
     * mon jeu a crash ».
     */
    private CWidget elementTemplate;
    private CWidget addTemplate;

    private double scroll;

    /** L'empreinte des marques affichees : des qu'elle change, la liste se refait. */
    private String shown;

    public LocationTeleportScreen(Category category, Skill skill) {
        super(skill.getDisplayName(), DOCUMENT);
        this.category = category;
        this.skill = skill;
    }

    private static AbilityData data() {
        return ClientAbilityData.get();
    }

    private static Player player() {
        return net.minecraft.client.Minecraft.getInstance().player;
    }

    private static LocationTeleportSkill teleport() {
        return TeleporterCategory.LOCATION_TELEPORT;
    }

    private List<LocationMark> marks() {
        AbilityData data = data();
        return data == null ? List.of() : data.getMarks();
    }

    /**
     * Ce que la liste montre, en une chaine : ses noms, et ou ils menent.
     *
     * <p>C'est assez pour savoir qu'il faut la refaire, et c'est tout ce que l'ecran demande : une
     * marque ajoutee, oubliee ou renommee change cette chaine, une marque immobile ne la change pas.
     */
    private String fingerprint() {
        StringBuilder out = new StringBuilder();
        for (LocationMark mark : marks()) {
            out.append(mark.name()).append('|').append(mark.dimension()).append('|')
                    .append(mark.x()).append('|').append(mark.y()).append('|')
                    .append(mark.z()).append(';');
        }
        return out.toString();
    }

    private int addRow() {
        return canAdd() ? marks().size() : -1;
    }

    /**
     * Peut-on encore poser une marque ? Six au plus, comme le panneau de l'original.
     *
     * <p>Au-dela, la ligne qui ajoute n'existe plus : c'est ce que le joueur a vu dans le vrai mod —
     * « a partir du 6eme point de teleportation on ne peut pas en rajouter de nouveau » — et c'est
     * le serveur qui tient la regle. Voir {@link LocationMark#MAX_MARKS}.
     */
    private boolean canAdd() {
        return marks().size() < LocationMark.MAX_MARKS;
    }

    private int rowCount() {
        return marks().size() + (canAdd() ? 1 : 0);
    }

    private double age() {
        return (EffectClock.now() - openedAt) / 1000.0;
    }

    /** L'opacite d'une ligne : elle apparait un peu apres la precedente. */
    private double rowFade(int index) {
        double local = age() - index * ROW_STEP;
        return Math.max(0.0, Math.min(1.0, local / ROW_FADE));
    }

    @Override
    protected void init() {
        // Les deux modeles du fichier ne se dessinent pas : ils servent a faire des copies.
        elementTemplate = find("elem_template");
        addTemplate = find("add_template");
        elementTemplate.transform.doesDraw = false;
        addTemplate.transform.doesDraw = false;

        list = find("list");
        info = find("info");
        info.transform.doesDraw = false;

        // La liste defile : ses enfants se dessinent decoupes a son cadre, sans quoi une ligne de
        // trop sort sous le panneau. Le joueur l'a vu le 22/10 : « avec 7 points le 7eme debordait
        // en dessous ». Voir CGuiScreen.clipped.
        clipTo(list);

        // Le fichier ancre l'explication au bord droit de la racine, ce qui la pose autour du
        // milieu de l'ecran — donc par-dessus la liste. L'original la deplacait de toute facon au
        // moment de s'en servir, ligne par ligne ; le port la pose donc une fois pour toutes
        // contre le bord GAUCHE du panneau, ou elle se lit comme une annotation de la ligne.
        info.transform.alignWidth = "LEFT";

        // L'explication : une ligne de texte par renseignement, posee dans le panneau de droite.
        for (int i = 0; i < HINT_LINES; i++) {
            CWidget line = new CWidget("hint" + i);
            line.transform.height = HINT_LINE;
            line.text = new CWidget.TextBox();
            line.text.align = "RIGHT";
            // Quarante : le `textSize` du MessageTab de l'original, en unites du document — donc un
            // peu moins de dix pixels une fois l'echelle de la racine (0,24) appliquee. Le port y
            // avait laisse la hauteur de sa ligne (quarante-deux), ce qui ne se voyait pas tant que
            // le moteur dessinait tout a la taille de l'interface.
            line.text.fontSize = HINT_FONT;
            line.text.heightAlign = "TOP";
            info.add(line);
        }

        rebuild();
    }

    /**
     * Refaire la liste : une ligne par marque, puis celle qui ajoute.
     *
     * <p>C'est l'{@code updateList} de l'original, appele apres chaque ajout et chaque oubli — sauf
     * que l'original le declenchait sur la reponse du serveur, et que le port le fait des que la
     * liste change, d'ou qu'elle vienne. Sans cela, le joueur voyait ce qu'il a signale le 22/10 :
     * « quand je cree un nouveau point, ca ne met pas a jour la liste automatiquement » — et une
     * marque devenue inabordable gardait son texte clair, puisqu'elle avait ete peinte avant.
     *
     * <p>ET LES MODELES NE SE REDEMANDENT PAS PAR LEUR NOM : ils sont enfants de la liste, et vider
     * la liste les emporte. Les redemander rendait {@code null} et faisait tomber le jeu a la
     * premiere marque creee — le joueur l'a vu le 22/10 : « quand j'ai cree un nouveau marqueur,
     * mon jeu a crash ».
     */
    private void rebuild() {
        forgetClicks();
        list.children().clear();
        field = null;
        // Le champ precedent n'existe plus : le clavier ne doit plus le chercher.
        focus(null);

        for (int i = 0; i < marks().size(); i++) {
            list.add(markRow(elementTemplate, i));
        }
        if (canAdd()) {
            list.add(addRowTemplate(addTemplate, elementTemplate));
        }

        shown = fingerprint();
    }

    /**
     * Une ligne de marque : une copie du modele, son nom, et ses deux icones.
     *
     * <p>Le modele porte les trois enfants de la ligne — le texte et les deux icones — donc la
     * copie les porte aussi. L'icone de saut disparait quand la marque ne mene nulle part, et le
     * nom passe alors en gris : c'est exactement ce que l'original retirait a la main.
     */
    private CWidget markRow(CWidget template, int index) {
        LocationMark mark = marks().get(index);
        CWidget out = new CWidget("row" + index);
        out.transform.width = template.transform.width;
        out.transform.height = ROW_HEIGHT;
        out.transform.y = index * (ROW_HEIGHT + ROW_SPACING);
        out.tint = new CWidget.Tint();

        for (CWidget child : template.children()) {
            out.add(child.copy());
        }

        CWidget text = out.child("text");
        text.text.content = mark.name();

        String no = refusal(mark);
        CWidget jump = out.child("btn_teleport");
        if (!no.isEmpty()) {
            jump.transform.doesDraw = false;
            // LE GRIS SE DONNE PAR CANAUX, jamais par la constante entiere : `color` les ecrit
            // tels quels dans le rouge, le vert et le bleu, et 0xA2A2A2 dans un canal est ecrete a
            // 255 — donc BLANC. C'est ce que le joueur a vu le 22/10 : « les points de TP ou je ne
            // peux pas aller sont trop blancs, alors qu'ils doivent etre grises ».
            text.text.color((TEXT_OFF >> 16) & 0xFF, (TEXT_OFF >> 8) & 0xFF, TEXT_OFF & 0xFF);
        } else {
            onClick(jump, () -> perform(index));
        }

        onClick(out.child("btn_remove"), () -> send(LocationTeleportPacket.Action.REMOVE, index, ""));
        return out;
    }

    /**
     * La ligne qui ajoute : le second modele, son champ, et sa coche.
     *
     * <p>Le champ se conduit comme chez l'original : il porte « Add... » au repos, se vide et prend
     * le clavier au clic, et remet son texte a la fin de la saisie. Son opacite dit aussi ou il en
     * est — 0,4 au repos, 0,8 avec le clavier — et c'est cette difference-la, avec le curseur, qui
     * indique qu'on peut ecrire.
     */
    private CWidget addRowTemplate(CWidget add, CWidget element) {
        int index = addRow();
        CWidget out = new CWidget("row" + index);
        out.transform.width = element.transform.width;
        out.transform.height = ROW_HEIGHT;
        out.transform.y = index * (ROW_HEIGHT + ROW_SPACING);
        out.tint = new CWidget.Tint();

        CWidget input = add.child("input_text").copy();
        input.text.content = ADD_TEXT;
        input.text.allowEdit = false;
        input.text.hint = null;
        out.add(input);
        field = input;

        CWidget confirm = add.child("btn_confirm").copy();
        out.add(confirm);

        onClick(out, this::takeField);
        onClick(confirm, this::confirmInput);
        return out;
    }

    /** Un clic sur la ligne : le champ se vide et prend le clavier, comme chez l'original. */
    private void takeField() {
        if (field == null || field.text == null || field.text.allowEdit) return;
        field.text.content = "";
        field.text.allowEdit = true;
        focus(field);
    }

    private static void send(LocationTeleportPacket.Action action, int markId, String name) {
        AbilityNetwork.CHANNEL.sendToServer(new LocationTeleportPacket(
                teleport().getCategory().getCategoryId(), teleport().getId(), action, markId, name));
    }

    /** Le saut : l'ecran se ferme, et le serveur fait le reste — c'est lui qui joue le son. */
    private void perform(int index) {
        send(LocationTeleportPacket.Action.PERFORM, index, "");
        onClose();
    }

    /**
     * Le champ valide : la marque est ajoutee sous le nom qu'il porte, puis il reprend son repos.
     *
     * <p>Un champ qui n'a jamais pris le clavier ne valide rien : son contenu est encore
     * « Add... », et l'envoyer creerait une marque portant ce nom-la.
     */
    private void confirmInput() {
        if (field == null || field.text == null || !field.text.allowEdit) return;
        send(LocationTeleportPacket.Action.ADD, -1, field.text.content);
        field.text.content = ADD_TEXT;
        field.text.allowEdit = false;
        focus(null);
    }

    @Override
    protected void confirm(CWidget box) {
        confirmInput();
    }

    /**
     * Pourquoi cette marque ne mene nulle part, ou rien.
     *
     * Les deux refus de l'original, dans son ordre : la dimension qu'on n'a pas le droit de
     * traverser, puis la reserve qui ne suffit pas.
     */
    private String refusal(LocationMark mark) {
        AbilityData data = data();
        Player player = player();
        if (mark == null || data == null || player == null) return "";

        LocationTeleportSkill skill = teleport();
        if (LocationTeleportSkill.crossDimension(player, mark) && !skill.canCrossDimension(data)) {
            return Component.translatable(
                    "ac.ability.teleporter.location_teleport.err_exp").getString();
        }
        return price(mark) > data.getControlPoint()
                ? Component.translatable(
                        "ac.ability.teleporter.location_teleport.err_cp").getString()
                : "";
    }

    private float price(LocationMark mark) {
        AbilityData data = data();
        Player player = player();
        if (mark == null || data == null || player == null) return 0f;

        return teleport().cpCost(data, LocationTeleportSkill.distanceTo(player, mark),
                LocationTeleportSkill.crossDimension(player, mark));
    }

    /**
     * L'explication d'une ligne, une ligne par renseignement comme l'original : la dimension, les
     * coordonnees, le prix — puis le refus s'il y en a un.
     *
     * <p>Le port mettait la dimension et les coordonnees sur la meme ligne, et la boite ne
     * grandissait pas : le texte y defilait au lieu de tenir. Le joueur l'a redemande le 22/10.
     */
    private List<String> hint(int index) {
        List<String> out = new ArrayList<>(HINT_LINES);

        if (index == addRow()) {
            Player player = player();
            if (player == null) return out;
            var at = player.position();
            out.add(dimensionName(player.level().dimension().location().toString()));
            out.add(coords(at.x, at.y, at.z));
            return out;
        }

        LocationMark mark = marks().get(index);
        out.add(dimensionName(mark.dimension()));
        out.add(coords(mark.x(), mark.y(), mark.z()));
        out.add(Math.round(price(mark)) + " CP");
        String no = refusal(mark);
        if (!no.isEmpty()) out.add(no);
        return out;
    }

    /** La dimension sous sa forme courte : {@code overworld}, {@code the_nether}. */
    private static String dimensionName(String dimension) {
        return new LocationMark("", dimension, 0, 0, 0).dimensionName();
    }

    private static String coords(double x, double y, double z) {
        return "(" + Math.round(x) + ", " + Math.round(y) + ", " + Math.round(z) + ")";
    }

    /**
     * La largeur et la hauteur de la boite d'explication, pour que le texte entier y tienne.
     *
     * <p>C'est le {@code updateText} de la {@code MessageTab} de l'original, au calcul pres : la
     * largeur du plus long des textes, plus les deux marges, et une hauteur de ligne par texte. Le
     * port laissait la largeur du fichier — 335 unites, ecrite pour l'anglais — et le moteur faisait
     * donc defiler ce qui depassait au lieu de l'ecrire. Le joueur : « le carre avec les infos
     * devrait grandir pour que le texte entier rentre dedans plutot que d'avoir du texte
     * deroulant ».
     */
    private void sizeInfo(List<String> lines) {
        double scale = info.absScale;
        int cell = AcademyFonts.cell(HINT_FONT * Math.max(1.0E-4, scale));

        int widest = 0;
        for (String line : lines) {
            widest = Math.max(widest, AcademyFonts.width(null, line, cell));
        }

        double[] size = infoSize(lines.size(), widest, scale);
        info.transform.width = size[0];
        info.transform.height = size[1];

        CWidget menu = find("menu");
        if (menu != null) {
            info.transform.x = menu.transform.x - info.transform.width - INFO_GAP;
        }

        // Et les lignes se replacent dans la boite, alignees a droite comme chez l'original.
        for (int i = 0; i < HINT_LINES; i++) {
            CWidget line = info.child("hint" + i);
            line.transform.x = HINT_MARGIN;
            line.transform.y = HINT_MARGIN + i * HINT_LINE;
            line.transform.width = Math.max(0.0, info.transform.width - 2 * HINT_MARGIN);
        }
    }

    /**
     * La taille de la boite d'explication, en unites du document : largeur et hauteur.
     *
     * <p>La largeur du plus long des textes, plus les deux marges ; une hauteur de ligne par texte.
     * La mesure arrive en <b>pixels d'ecran</b> et ressort en unites du document, parce que c'est
     * en unites que le fichier pose ses widgets — l'echelle est celle de la racine, 0,24.
     *
     * <p>Fonction pure, donc verifiable : c'est la regle du {@code MessageTab} de l'original, et
     * c'est elle qui remplace la largeur figee du fichier — celle qui faisait defiler le texte.
     */
    public static double[] infoSize(int lines, int widestPixels, double scale) {
        double perUnit = scale <= 0 ? 1.0 : scale;
        double width = Math.max(0, widestPixels) / perUnit + 2 * HINT_MARGIN;
        double height = lines * HINT_LINE + 2 * HINT_MARGIN;
        return new double[] { width, height };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0.0, Math.min(maxScroll(), scroll - delta * ROW_HEIGHT));
        return true;
    }

    private double maxScroll() {
        return Math.max(0, rowCount() * (ROW_HEIGHT + ROW_SPACING) - list.transform.height);
    }

    /** L'index d'une ligne, lu dans son nom : {@code row3}. */
    private int rowIndex(CWidget row) {
        try {
            return Integer.parseInt(row.name().substring("row".length()));
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /**
     * Ce que l'ecran pose avant chaque dessin : l'ouverture du panneau, le defilement, les couleurs
     * du survol, et l'explication de la ligne survolee.
     */
    @Override
    protected void frame(GuiGraphics graphics, double mouseX, double mouseY) {
        CWidget menu = find("menu");
        if (menu == null || list == null || info == null) {
            return;
        }

        // Les marques ont pu changer depuis la derniere image : ajout, oubli, ou simple
        // synchronisation. La liste se refait alors, avec ses gris a jour.
        if (!fingerprint().equals(shown)) {
            rebuild();
            layout();
        }

        menu.transform.height = MENU_HEIGHT * Math.min(1.0, age() / OPEN_SECONDS);

        // Une marque oubliee raccourcit la liste : le defilement se ramene alors dans ses bornes,
        // sinon les dernieres lignes resteraient hors du cadre.
        scroll = Math.max(0.0, Math.min(maxScroll(), scroll));

        // Le defilement deplace toutes les lignes d'abord : c'est ce qui les met a leur place.
        for (CWidget row : list.children()) {
            row.transform.y = rowIndex(row) * (ROW_HEIGHT + ROW_SPACING) - scroll;
        }
        layout();

        // Le champ d'ajout dit ou il en est par son opacite, et prend le clavier au clic : c'est la
        // meme regle que l'original (0,4 au repos, 0,8 avec le focus).
        if (field != null && field.text != null) {
            boolean editing = focused() == field;
            double alpha = (editing ? ADD_FOCUS : ADD_IDLE) * rowFade(addRow());
            field.text.alpha((int) Math.round(Math.max(0.0, Math.min(1.0, alpha)) * 255));
        }

        int hovered = -1;

        for (CWidget row : list.children()) {
            int index = rowIndex(row);
            double fade = rowFade(index);

            // La ligne ne s'efface que tant qu'elle nait : au-dela, c'est le decoupage de la liste
            // qui la cache quand elle sort — la couper en entier laissait des trous au defilement.
            row.transform.doesDraw = fade > 0.0;
            if (!row.transform.doesDraw) continue;

            boolean over = row.isPointWithin(mouseX, mouseY);
            if (over) hovered = index;

            // La teinte du fichier s'allume au survol, et son opacite suit le fondu de la ligne.
            int alpha = (int) Math.round(fade * (over ? TINT_HOVER : TINT_IDLE) * 255);
            row.tint.idle = white(alpha);
            row.tint.hover = white(alpha);

            for (CWidget child : row.children()) {
                if (child.texture != null) {
                    double icon = fade * (child.isPointWithin(mouseX, mouseY) ? ICON_HOVER
                            : ICON_IDLE);
                    // LA TEINTE DES ICONES EST CELLE DU TEXTE, et non le blanc du fichier :
                    // l'original remplacait la couleur de son `DrawTexture` par son TextNormal
                    // (0xC1CFD5) des qu'il posait un bouton — voir son `wrapButton`. Le fichier,
                    // lui, ne dit que « blanc », et le port le suivait : « les icones pour le TP ou
                    // pour la suppression sont trop blanches » (joueur, 22/10).
                    child.texture.red = (TEXT_ON >> 16) & 0xFF;
                    child.texture.green = (TEXT_ON >> 8) & 0xFF;
                    child.texture.blue = TEXT_ON & 0xFF;
                    child.texture.alpha = (int) Math.round(icon * 255);
                }
                // Le champ porte son opacite a lui : voir plus haut.
                if (child.text != null && child != field) {
                    child.text.alpha((int) Math.round(fade * 255));
                }
            }
        }

        // Et l'explication se repose le long de la ligne survolee, comme le `setMessage` de
        // l'original : elle vit contre elle, a gauche du panneau.
        List<String> lines = hovered < 0 ? List.of() : hint(hovered);
        info.transform.doesDraw = !lines.isEmpty();
        if (lines.isEmpty()) {
            return;
        }

        CWidget row = rowWidget(hovered);
        sizeInfo(lines);
        info.transform.y = row == null ? 0
                : (row.absY - root().absY) / root().absScale;
        layout();

        for (int i = 0; i < HINT_LINES; i++) {
            CWidget line = info.child("hint" + i);
            line.transform.doesDraw = i < lines.size();
            if (i >= lines.size()) continue;

            line.text.content = lines.get(i);
            // UNE SEULE COULEUR pour les quatre renseignements, comme le `FontOption` unique du
            // MessageTab de l'original. Le port les teintait differemment — la dimension d'un cote,
            // le prix de l'autre, le refus en rouge — et le joueur l'a vu : « la couleur du texte
            // qui indique les coordonnees etc n'est pas de la meme couleur ».
            line.text.color((TEXT_ON >> 16) & 0xFF, (TEXT_ON >> 8) & 0xFF, TEXT_ON & 0xFF);
        }
    }

    private static int[] white(int alpha) {
        return new int[] { 255, 255, 255, alpha };
    }

    private CWidget rowWidget(int index) {
        for (CWidget row : list.children()) {
            if (rowIndex(row) == index) return row;
        }
        return null;
    }
}
