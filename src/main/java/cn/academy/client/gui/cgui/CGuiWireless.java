package cn.academy.client.gui.cgui;

import java.util.List;

import javax.annotation.Nullable;

import cn.academy.ability.network.AbilityNetwork;
import cn.academy.energy.network.WirelessRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * L'onglet sans fil d'un ecran de machine.
 *
 * <p>L'original l'appelait {@code WirelessPage}, et il l'accrochait a <b>huit</b> machines : le
 * generateur solaire, l'eolien, le generateur de phase, le formeur de metal, le fusor d'imag, le
 * brouilleur d'aptitude — et, dans l'autre sens, le noeud sans fil, qui y choisit son <b>Matrix</b>.
 * Le port avait le document de la page — identique au bit pres — et aucun ecran ne l'ouvrait : les
 * machines trouvaient leur noeud toutes seules, et les noeuds trouvaient leur Matrix de meme.
 *
 * <h2>Deux sens, une page</h2>
 *
 * <p>Une machine cherche un noeud, un noeud cherche un Matrix : la page, sa liste et ses gestes sont
 * les memes, et seule l'icone du coin le dit. C'est ce que faisaient {@code userPage} et
 * {@code nodePage}, qui ne differaient que par cette image.
 *
 * <h2>Ce que l'ecran fait, et ce qu'il ne fait pas</h2>
 *
 * <p>Il ne decide rien : il demande la liste au serveur, il montre ce que le serveur renvoie, et
 * chaque geste repart en demande. C'est ainsi que le cadre « Connected » ne peut pas dire autre chose
 * que ce que la machine fait vraiment, et c'est la regle du port depuis le changement de categorie du
 * developeur.
 *
 * <p>La page ne transporte jamais la position de sa machine : c'est le serveur qui prend celle du
 * conteneur ouvert. Un client ne peut donc pas raccorder une machine devant laquelle il n'est pas.
 */
public final class CGuiWireless {

    /** L'identifiant de la page : c'est lui qui choisit l'icone de l'onglet, `icon_wireless.png`. */
    public static final String PAGE = "wireless";

    /** Le document de l'original, repris tel quel. */
    private static final String DOCUMENT = "rework/page_wireless";

    /** Le dossier des icones de l'onglet et de ses lignes. */
    private static final String ICONS = "academy:textures/guis/icons/";

    /**
     * Une ligne de la liste : de quoi la dessiner, et de quoi s'y raccorder.
     *
     * <p>{@code encrypted} allume la petite cle et le champ de mot de passe ; {@code room} dit si le
     * noeud a encore une place. Un noeud plein s'affiche comme les autres, mais se fera refuser.
     */
    public record Row(BlockPos pos, String name, boolean encrypted, boolean room) {}

    /**
     * La page ouverte, s'il y en a une : c'est elle que le serveur repond.
     *
     * <p>Un ecran a la fois, et c'est toujours vrai : le serveur ne repond qu'au joueur dont le
     * conteneur est ouvert, donc la page qui recoit est forcement la sienne. La comparaison avec
     * l'ecran courant protege du seul cas ou cela pourrait ne pas l'etre : une reponse qui arrive
     * apres la fermeture de l'ecran qui l'avait demandee.
     */
    @Nullable
    private static CGuiWireless open;

    /** Ce que le serveur vient d'annoncer : le noeud lie, et les noeuds joignables. */
    public static void deliver(String linked, List<Row> rows) {
        CGuiWireless page = open;
        if (page == null) return;
        if (Minecraft.getInstance().screen != page.screen) return;
        page.apply(linked, rows);
    }

    private final CGuiContainerScreen<?> screen;

    /** Vrai pour l'onglet d'un NOEUD : il cherche un Matrix, quand une machine cherche un noeud. */
    private final boolean matrices;

    /** La zone des lignes, son gabarit, le cadre du noeud lie, et le champ de mot de passe. */
    @Nullable private CWidget zone;
    @Nullable private CWidget template;
    @Nullable private CWidget connectedRow;
    @Nullable private CWidget passwordField;

    /** Ce que la page montre, pour pouvoir le remettre quand elle se rebatit. */
    private String linked = "";
    private List<Row> shown = List.of();

    /** L'onglet d'une machine : elle cherche le noeud qui l'alimente. */
    public CGuiWireless(CGuiContainerScreen<?> screen) {
        this(screen, false);
    }

    /** L'onglet d'un noeud : il cherche le reseau qu'il rejoint. */
    public CGuiWireless(CGuiContainerScreen<?> screen, boolean matrices) {
        this.screen = screen;
        this.matrices = matrices;
    }

    /**
     * Pose la page et son onglet, puis demande l'etat au serveur.
     *
     * <p>Appelee par le {@code build} de l'ecran, donc a chaque ouverture : la page se rebatit avec
     * lui, et {@code page(...)} rend celle qui existe deja si elle y est.
     */
    public void install() {
        open = this;

        CWidget window = screen.page(PAGE, DOCUMENT);

        // Le logo du coin respire : c'est une ligne de l'original, `TechUI.breathe(widget.
        // getWidget("icon_logo"))`, et c'est elle qui fait varier sa couleur. Le port ne faisait
        // respirer que les fonds nommes `ui_`, donc celui-ci restait blanc.
        screen.breathe(window.child("icon_logo"));

        // L'icone du coin dit ce qu'on cherche : un noeud pour une machine, un Matrix pour un noeud.
        // C'est la seule chose qui distingue les deux pages — chez l'original aussi, dont le
        // `nodePage` remplacait cette image et rien d'autre.
        if (matrices) {
            CWidget logo = window.child("icon_logo");
            if (logo != null && logo.texture != null) {
                logo.texture.texture = ICONS + "icon_tomatrix.png";
            }
        }

        CWidget panel = window.child("panel_wireless");
        if (panel == null) return;

        // Les deux libelles du document sont ecrits en dur dans le fichier de l'original — « Connected »
        // et « Available ». L'ecran les remplace, comme il remplace les noms d'aptitudes du developpeur.
        setText(panel.child("text_connected"), local("connected"));
        setText(panel.child("text_available"), local("available"));

        screen.onClick(panel.child("btn_arrowup"), () -> scroll(-1));
        screen.onClick(panel.child("btn_arrowdown"), () -> scroll(1));

        connectedRow = panel.child("elem_connected");
        if (connectedRow != null) {
            // Le cadre du noeud lie : son icone est le bouton de deconnexion, comme chez l'original.
            screen.onClick(connectedRow.child("icon_connect"), this::disconnect);
        }

        zone = panel.child("zone_elementlist");
        template = zone == null ? null : zone.child("element");
        if (template != null) zone.remove(template);
        if (zone != null) {
            zone.list = new CWidget.ElementList();
            zone.list.spacing = 0;
        }

        // Ce que la page montrait est remis tout de suite : un ecran qui se rouvre ne doit pas avoir
        // l'air vide le temps que le serveur reponde.
        apply(linked, shown);
        request(WirelessRequestPacket.find());
    }

    /** Le prefixe des textes de l'onglet : ils vivent avec les autres textes d'interface du port. */
    private static final String KEYS = "ac.gui.wireless.";

    private static String local(String key) {
        return Component.translatable(KEYS + key).getString();
    }

    private static void setText(@Nullable CWidget widget, String content) {
        if (widget != null && widget.text != null) widget.text.content = content;
    }

    private void request(WirelessRequestPacket packet) {
        AbilityNetwork.CHANNEL.sendToServer(packet);
    }

    private void disconnect() {
        request(WirelessRequestPacket.disconnect());
    }

    /** Une ligne plus haut ou plus bas, sans sortir de la liste. */
    private void scroll(int delta) {
        if (zone == null || zone.list == null) return;
        int last = Math.max(0, zone.children().size() - 1);
        zone.list.progress = Math.max(0, Math.min(zone.list.progress + delta, last));
    }

    /** Remet la page a ce que le serveur vient d'annoncer. */
    private void apply(String newLinked, List<Row> rows) {
        this.linked = newLinked == null ? "" : newLinked;
        this.shown = List.copyOf(rows);

        if (connectedRow != null) {
            boolean any = !this.linked.isEmpty();
            setText(connectedRow.child("text_name"), any ? this.linked : local("not_connected"));

            // L'icone de la liaison : elle dit l'etat, et l'original la laisse a soixante pour cent
            // quand rien n'est lie — sa TEINTE est meme eteinte, sans quoi elle reprendrait la
            // couleur du survol et se dessinerait blanche. Le port changeait l'image sans toucher a
            // l'opacite, et le joueur voyait un logo allume sur une machine eteinte.
            int alpha = any ? 255 : 153;

            CWidget icon = connectedRow.child("icon_connect");
            if (icon != null && icon.texture != null) {
                icon.texture.texture = ICONS + (any ? "icon_connected.png" : "icon_unconnected.png");
                icon.texture.alpha = alpha;
                if (icon.tint != null) icon.tint.enabled = any;
            }

            // Le logo du coin, a la meme lumiere : c'est l'image qui dit ce qu'on cherche.
            CWidget logo = connectedRow.child("icon_logo");
            if (logo != null && logo.texture != null) logo.texture.alpha = alpha;
        }

        if (zone == null || template == null) return;

        zone.children().clear();
        passwordField = null;
        if (zone.list != null) zone.list.progress = 0;

        for (Row row : this.shown) {
            zone.add(rowWidget(row));
        }
    }

    /**
     * Une ligne de la liste : le gabarit de l'original, copie.
     *
     * <p>Un noeud qui demande un mot de passe montre sa petite cle et son champ ; les autres cachent
     * les deux, exactement comme l'original le faisait a la construction de sa page. Le champ se
     * valide par Entree : c'est l'evenement de confirmation du moteur, celui que le noeud utilise deja
     * pour son nom et son mot de passe.
     */
    private CWidget rowWidget(Row row) {
        CWidget widget = template.copy();
        setText(widget.child("text_name"), row.name());

        CWidget key = widget.child("icon_key");
        if (key != null) key.transform.doesDraw = row.encrypted();

        CWidget field = widget.child("input_pass");
        if (field != null) {
            field.transform.doesDraw = row.encrypted();
            if (row.encrypted()) {
                passwordField = field;
                screen.onConfirm(field, password -> {
                    request(WirelessRequestPacket.connect(row.pos(), password));
                    if (field.text != null) field.text.content = "";
                });
            }
        }

        // L'icone de connexion, et elle seule : le champ de saisie est dans la meme ligne, et un clic
        // sur la ligne entiere se declencherait avant que le champ prenne le clavier.
        CWidget connect = widget.child("icon_connect");
        if (connect != null) {
            screen.onClick(connect, () -> request(WirelessRequestPacket.connect(row.pos(),
                    field != null && field.text != null ? field.text.content : "")));
        }
        return widget;
    }
}
