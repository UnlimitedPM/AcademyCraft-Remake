package cn.academy.ability.client;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.network.LocationTeleportPacket;
import cn.academy.ability.teleporter.LocationMark;
import cn.academy.ability.teleporter.LocationTeleportSkill;
import cn.academy.ability.teleporter.TeleporterCategory;
import cn.academy.client.gui.cgui.CGuiScreen;
import cn.academy.client.gui.cgui.CWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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

    /** Le texte du fichier, son gris quand la marque ne mene nulle part, et le rouge du refus. */
    private static final int TEXT_ON = 0xC1CFD5;
    private static final int TEXT_OFF = 0xA2A2A2;
    private static final int TEXT_BAD = 0xE08080;

    /** Combien de lignes une explication peut porter : ou, combien, et pourquoi pas. */
    private static final int HINT_LINES = 3;

    private final Category category;
    private final Skill skill;

    private final long openedAt = EffectClock.now();

    private CWidget list;
    private CWidget info;
    private CWidget field;

    private double scroll;

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

    private int addRow() {
        return marks().size();
    }

    private int rowCount() {
        return marks().size() + 1;
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
        CWidget element = find("elem_template");
        CWidget add = find("add_template");
        element.transform.doesDraw = false;
        add.transform.doesDraw = false;

        list = find("list");
        info = find("info");
        info.transform.doesDraw = false;

        // Le fichier ancre l'explication au bord droit de la racine, ce qui la pose autour du
        // milieu de l'ecran — donc par-dessus la liste. L'original la deplacait de toute facon au
        // moment de s'en servir, ligne par ligne ; le port la pose donc une fois pour toutes
        // contre le bord GAUCHE du panneau, ou elle se lit comme une annotation de la ligne.
        CWidget menu = find("menu");
        info.transform.alignWidth = "LEFT";
        info.transform.x = menu.transform.x - info.transform.width - 5;

        // L'explication : une ligne de texte par renseignement, posee dans le panneau de droite.
        for (int i = 0; i < HINT_LINES; i++) {
            CWidget line = new CWidget("hint" + i);
            line.transform.width = 335.83 - 40;
            line.transform.height = 42;
            line.transform.x = 20;
            line.transform.y = 20 + i * 42;
            line.text = new CWidget.TextBox();
            line.text.align = "RIGHT";
            // Quarante : le `textSize` du MessageTab de l'original, en unites du document — donc un
            // peu moins de dix pixels une fois l'echelle de la racine (0,24) appliquee. Le port y
            // avait laisse la hauteur de sa ligne (quarante-deux), ce qui ne se voyait pas tant que
            // le moteur dessinait tout a la taille de l'interface.
            line.text.fontSize = 40;
            line.text.heightAlign = "TOP";
            info.add(line);
        }

        for (int i = 0; i < marks().size(); i++) {
            list.add(markRow(element, i));
        }
        list.add(addRowTemplate(add, element));
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
            text.text.color(TEXT_OFF, TEXT_OFF, TEXT_OFF);
        } else {
            onClick(jump, () -> perform(index));
        }

        onClick(out.child("btn_remove"), () -> send(LocationTeleportPacket.Action.REMOVE, index, ""));
        return out;
    }

    /** La ligne qui ajoute : le second modele, son champ, et sa coche. */
    private CWidget addRowTemplate(CWidget add, CWidget element) {
        int index = addRow();
        CWidget out = new CWidget("row" + index);
        out.transform.width = element.transform.width;
        out.transform.height = ROW_HEIGHT;
        out.transform.y = index * (ROW_HEIGHT + ROW_SPACING);
        out.tint = new CWidget.Tint();

        CWidget input = add.child("input_text").copy();
        input.text.content = "";
        input.text.allowEdit = true;
        input.text.hint = Component.translatable(
                "ac.ability.teleporter.location_teleport.name_hint").getString();
        out.add(input);
        field = input;

        CWidget confirm = add.child("btn_confirm").copy();
        out.add(confirm);

        onClick(out, () -> focus(field));
        onClick(confirm, this::confirmInput);
        return out;
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

    /** Le champ valide : la marque est ajoutee sous le nom qu'il porte, puis il se vide. */
    private void confirmInput() {
        if (field == null || field.text == null) return;
        send(LocationTeleportPacket.Action.ADD, -1, field.text.content);
        field.text.content = "";
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

    /** L'explication d'une ligne : ou elle mene, ce qu'elle coute, et le refus s'il y en a un. */
    private List<String> hint(int index) {
        if (index == addRow()) {
            Player player = player();
            if (player == null) return List.of();
            var at = player.position();
            return List.of(LocationMark.of(player.level()) + " " + coords(at.x, at.y, at.z));
        }

        LocationMark mark = marks().get(index);
        String no = refusal(mark);
        String cost = (int) price(mark) + " CP";
        String where = mark.dimensionName() + " " + coords(mark.x(), mark.y(), mark.z());
        return no.isEmpty() ? List.of(where, cost) : List.of(where, cost, no);
    }

    private static String coords(double x, double y, double z) {
        return "(" + Math.round(x) + ", " + Math.round(y) + ", " + Math.round(z) + ")";
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
        if (menu != null) {
            menu.transform.height = MENU_HEIGHT * Math.min(1.0, age() / OPEN_SECONDS);
        }

        if (list == null || info == null) {
            return;
        }

        // Le defilement deplace toutes les lignes d'abord : c'est ce qui les met a leur place.
        for (CWidget row : list.children()) {
            row.transform.y = rowIndex(row) * (ROW_HEIGHT + ROW_SPACING) - scroll;
        }
        layout();

        double top = list.absY;
        double bottom = top + Math.min(list.transform.height, menu.transform.height - 18)
                * list.absScale;
        int hovered = -1;

        for (CWidget row : list.children()) {
            int index = rowIndex(row);
            double fade = rowFade(index);

            // Rien ne depasse de la liste : le moteur n'a pas d'ecran de decoupe, donc c'est la
            // ligne elle-meme qui s'efface quand elle sort.
            row.transform.doesDraw = fade > 0.0 && row.absY + row.absH > top && row.absY < bottom;
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
                    child.texture.alpha = (int) Math.round(icon * 255);
                }
                if (child.text != null) child.text.alpha((int) Math.round(fade * 255));
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
        info.transform.y = row == null ? 0
                : (row.absY - root().absY) / root().absScale;
        info.transform.height = lines.size() * 42 + 40;

        for (int i = 0; i < HINT_LINES; i++) {
            CWidget line = info.child("hint" + i);
            line.transform.doesDraw = i < lines.size();
            if (i >= lines.size()) continue;

            line.text.content = lines.get(i);
            boolean last = i == lines.size() - 1;
            int colour = last && lines.size() == HINT_LINES ? TEXT_BAD
                    : (i == 0 ? TEXT_ON : TEXT_OFF);
            line.text.color((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF);
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
