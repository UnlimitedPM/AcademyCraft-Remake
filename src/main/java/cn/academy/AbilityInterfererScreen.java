package cn.academy;

import cn.academy.client.gui.cgui.CGuiContainerScreen;
import cn.academy.client.gui.cgui.CGuiHist;
import cn.academy.client.gui.cgui.CGuiWireless;
import cn.academy.client.gui.cgui.CWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ecran du brouilleur d'aptitudes : sa page, sa liste blanche, et ses deux reglages.
 *
 * <p>La page EST celle de l'inventaire : l'original appelait {@code InventoryPage(window)} sur
 * {@code page_interfere}, donc son {@code ui_block} porte le fond de la machine et c'est lui qui
 * accueille les emplacements. C'est la seule machine ou la liste blanche est dans la <b>page</b> et
 * non dans le panneau d'infos.
 *
 * <h2>La liste blanche</h2>
 *
 * <p>Une zone de quatre lignes, ou chaque nom epargne prend la sienne : la liste defile par lignes
 * entieres quand il y en a plus, et la ligne choisie s'allume. Les trois boutons de la page font le
 * reste — celui du haut et celui du bas font defiler, celui qui ajoute fait naitre un champ de saisie
 * sur le panneau (il prend le clavier, et il disparait des qu'on clique ailleurs), et celui qui
 * retire enleve la ligne choisie.
 *
 * <p>Un nom voyage par le serveur — c'est un texte, pas un nombre — mais la liste, elle, revient par
 * la mise a jour de bloc que la machine envoie dix fois par seconde : un nom ajoute s'y trouve une
 * demi-seconde plus tard, et c'est ce qui fait vivre la liste a l'ecran.
 *
 * <h2>Ce que le port ajoute</h2>
 *
 * <p>L'onglet sans fil de l'original — choisir le noeud qui alimente la machine — est la, comme sur
 * les cinq autres machines : c'est le meme geste, pose par {@code CGuiWireless}. Dans le panneau, le
 * port ajoute en plus ce que la machine fait : combien de joueurs elle brouille en ce moment, et ce
 * que cela coute.
 */
public class AbilityInterfererScreen extends CGuiContainerScreen<AbilityInterfererMenu> {

    private static final String SWITCH_ON = "academy:textures/guis/button/button_switch_on.png";
    private static final String SWITCH_OFF = "academy:textures/guis/button/button_switch_off.png";

    /** Ce qu'une ligne de la liste montre : sept dixiemes, et pleine quand on la choisit. */
    private static final int ROW_ALPHA = 178;
    private static final int ROW_FOCUSED_ALPHA = 255;

    /** Le champ du prix, qui vire au rouge quand la machine ne peut pas le payer. */
    private CWidget costField;

    /** Le panneau de la liste, sa zone, et le modele de ligne dont chaque nom fait une copie. */
    private CWidget listPanel;
    private CWidget zone;
    private CWidget rowTemplate;

    /** Le nom que chaque ligne porte : c'est par lui que la ligne choisie se retrouve. */
    private final Map<CWidget, String> rowNames = new IdentityHashMap<>();

    /** Le nom choisi, ou rien : c'est lui que le bouton « retirer » enleve. */
    private String selected;

    /** Ce que la liste montrait la derniere fois, pour ne la refaire que quand elle change. */
    private List<String> shownWhitelist = List.of();

    /** Le champ de saisie ne du bouton « ajouter », ou rien. */
    private CWidget addField;

    public AbilityInterfererScreen(AbilityInterfererMenu menu, Inventory playerInventory,
                                   Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void build(CGuiContainerScreen<AbilityInterfererMenu> screen) {
        CWidget window = pageWindow("inv");
        if (window == null) window = page("inv", "rework/page_interfere");

        histogram(CGuiHist.energy(menu::getEnergyStored, menu.getMaxEnergyStored()));

        // Ce que la machine fait, en deux lignes que l'original n'avait pas : le nombre de joueurs
        // qu'elle brouille en ce moment, et ce que cela lui coute. Le prix vire au rouge quand le
        // tampon ne peut pas le payer — c'est ce qui explique pourquoi l'interrupteur refuse de
        // s'allumer, et sans quoi le bouton a l'air casse.
        seplineInfo();
        property(label("prop.affected"), Integer.toString(menu.getAffectedCount()));
        costField = property(label("prop.cost"),
                Integer.toString((int) menu.getCostPerTick()) + " IF/T").child("value");

        // L'onglet sans fil de l'original, pose en dernier : le moteur ouvre la premiere page posee,
        // et cette machine doit s'ouvrir sur sa page d'inventaire, celle qui porte la liste blanche.
        new CGuiWireless(screen).install();

        wirePage(window);
        fillRows(menu.whitelist());
    }

    private static String label(String key) {
        return Component.translatable("ac.gui.common." + key).getString();
    }

    /**
     * Les gestes de la page : ses quatre boutons, l'interrupteur, et le pas du rayon.
     *
     * <p>Appelee a chaque reconstruction du panneau, et c'est sans danger : elle retrouve la page qui
     * existe deja, et les gestes sont ranges par widget — les refaire remplace les anciens.
     */
    private void wirePage(CWidget window) {
        listPanel = window.child("panel_whitelist");
        zone = listPanel.child("zone_whitelist");
        rowTemplate = zone.child("element");
        if (rowTemplate != null) zone.remove(rowTemplate);

        // Les lignes s'empilent dans la zone : le modele porte deja la bonne hauteur, il n'y avait
        // qu'a le lui dire. C'est ce que l'original faisait en remplacant son ElementList.
        zone.list = new CWidget.ElementList();
        zone.list.spacing = 0;

        onClick(listPanel.child("btn_up"), () -> zone.list.progress = Math.max(0, zone.list.progress - 1));
        onClick(listPanel.child("btn_down"), this::progressNext);
        onClick(listPanel.child("btn_add"), this::beginAdd);
        onClick(listPanel.child("btn_remove"), () -> {
            if (selected != null) menu.removeFromWhitelist(selected);
        });

        // Le panneau des reglages : deux lignes de seize dans une boite de trente-deux, que le fichier
        // laisse toutes les deux a zero. Une liste les empile — et c'est justement pour cela qu'on
        // leur rend un alignement en HAUT : celle du bas porte un `alignHeight=BOTTOM` d'origine, qui
        // la repousse une seconde fois et la fait tomber seize unites plus bas, sous la barre de la
        // liste blanche, au lieu de rester entre l'interrupteur et elle.
        //
        // La faute est dans le fichier de l'original, pas dans le moteur : un `y=16` sur une ligne
        // deja calee en bas, dans une boite de deux fois sa hauteur, la pose a trente-deux.
        CWidget config = window.child("panel_config");
        config.list = new CWidget.ElementList();
        config.list.spacing = 0;
        for (CWidget row : config.children()) row.transform.alignHeight = "TOP";

        onClick(config.child("element_switch").child("element_btn_switch"),
                () -> press(AbilityInterfererMenu.BUTTON_TOGGLE));
        // Et toute la ligne allume aussi : le bouton fait seize unites de cote, et le libelle qui le
        // regarde ne servait a rien. Le document met le clic sur le bouton seul ; on l'elargit a la
        // ligne, sans rien changer a ce qu'elle montre.
        onClick(config.child("element_switch"), () -> press(AbilityInterfererMenu.BUTTON_TOGGLE));
        onClick(config.child("element_range").child("element_btn_left"),
                () -> press(rangeButton(false)));
        onClick(config.child("element_range").child("element_btn_right"),
                () -> press(rangeButton(true)));
    }

    /**
     * Le bouton de rayon a envoyer, selon la touche Maj.
     *
     * <p>L'original n'avait qu'un pas, et c'etait dix. Le joueur a demande cinq, et dix quand on
     * tient Maj : c'est donc le CLIENT qui choisit l'identifiant, et le serveur applique le pas
     * qu'il annonce — il ne voit pas le clavier.
     */
    private static int rangeButton(boolean up) {
        boolean far = net.minecraft.client.gui.screens.Screen.hasShiftDown();
        if (up) {
            return far ? AbilityInterfererMenu.BUTTON_RANGE_UP_FAR
                    : AbilityInterfererMenu.BUTTON_RANGE_UP;
        }
        return far ? AbilityInterfererMenu.BUTTON_RANGE_DOWN_FAR
                : AbilityInterfererMenu.BUTTON_RANGE_DOWN;
    }

    /** Une ligne plus bas, sans aller au-dela de la fin de la liste. */
    private void progressNext() {
        int last = Math.max(0, zone.children().size() - 1);
        zone.list.progress = Math.min(zone.list.progress + 1, last);
    }

    /**
     * Refait les lignes de la liste : une par nom epargne.
     *
     * <p>Le nom choisi qui disparait de la liste cesse de l'etre : sans cela, le bouton « retirer »
     * garderait le nom d'un joueur qui n'est plus la, et le retirerait deux fois.
     */
    private void fillRows(List<String> names) {
        shownWhitelist = List.copyOf(names);

        if (zone == null || rowTemplate == null) return;
        zone.children().clear();
        rowNames.clear();
        if (selected != null && !names.contains(selected)) selected = null;

        for (String name : names) {
            CWidget row = rowTemplate.copy();
            CWidget text = row.child("element_name");
            if (text != null && text.text != null) text.text.content = name;

            rowNames.put(row, name);
            onClick(row, () -> selected = name);
            zone.add(row);
        }
    }

    /**
     * Le bouton « ajouter » fait naitre un champ, comme l'original : il se pose sur le panneau, prend
     * le clavier, et disparait des qu'on clique ailleurs — c'est ce que l'original appelait
     * {@code LostFocusEvent}, et c'est le meme geste.
     */
    private void beginAdd() {
        if (addField != null || listPanel == null) return;

        CWidget field = new CWidget("add_name");
        field.transform.x = 50;
        field.transform.y = 5;
        field.transform.width = 40;
        field.transform.height = 10;

        // Un blanc a moitie transparent : un DrawTexture sans image ne dessine qu'une couleur, et
        // c'est exactement ce que l'original posait derriere son champ.
        field.texture = new CWidget.DrawTexture();
        field.texture.red = 255;
        field.texture.green = 255;
        field.texture.blue = 255;
        field.texture.alpha = 50;

        field.text = new CWidget.TextBox();
        field.text.allowEdit = true;

        listPanel.add(field);
        onConfirm(field, name -> {
            menu.addToWhitelist(name);
            endAdd();
        });

        addField = field;
        focus(field);
    }

    /** Ce que le champ d'ajout porte devient la liste. */
    private void endAdd() {
        if (addField == null) return;
        if (listPanel != null) listPanel.remove(addField);
        addField = null;
        focus(null);
    }

    /** Un bouton du menu : allumer, et pousser le rayon d'un pas. */
    private void press(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    @Override
    protected void frame(GuiGraphics graphics, double mouseX, double mouseY) {
        // La liste se refait quand son contenu change : un nom ajoute revient du serveur une
        // demi-seconde plus tard, par la mise a jour de bloc de la machine.
        List<String> names = menu.whitelist();
        if (!names.equals(shownWhitelist)) fillRows(names);

        // Et la ligne choisie s'allume : c'est ce qui dit au joueur laquelle le bouton retirera.
        for (Map.Entry<CWidget, String> entry : rowNames.entrySet()) {
            CWidget row = entry.getKey();
            if (row.texture != null) {
                row.texture.alpha = entry.getValue().equals(selected) ? ROW_FOCUSED_ALPHA : ROW_ALPHA;
            }
        }

        // L'interrupteur : son image dit l'etat, et sa lumiere le confirme — pleine lumiere quand il
        // est allume, un peu moins des qu'il s'eteint.
        CWidget button = widget("element_btn_switch");
        if (button != null && button.texture != null) {
            button.texture.texture = menu.isEnabled() ? SWITCH_ON : SWITCH_OFF;
            int lum = menu.isEnabled() ? 255 : 153;
            button.texture.red = lum;
            button.texture.green = lum;
            button.texture.blue = lum;
        }

        CWidget range = widget("element_text_range");
        if (range != null && range.text != null) {
            // Le rayon s'ecrit comme dans l'original, qui posait un `Double` dans son champ :
            // « 16.0 » et non « 16 ». Le zero ne sert a rien, il est demande.
            range.text.content = Double.toString(menu.getRange());
        }

        // Le prix : rouge quand le tampon ne peut pas payer un cycle, blanc quand il peut. C'est la
        // meme regle que celle qui refuse d'allumer la machine.
        if (costField != null && costField.text != null) {
            if (menu.canPay()) costField.text.color(255, 255, 255);
            else costField.text.color(0x9E, 0x2B, 0x25);
        }

        // Le champ d'ajout s'en va des qu'il n'a plus le clavier.
        if (addField != null && !isFocused(addField)) endAdd();
    }
}
