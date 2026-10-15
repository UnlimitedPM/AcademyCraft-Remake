package cn.academy;

import cn.academy.client.gui.cgui.CGuiContainerScreen;
import cn.academy.client.gui.cgui.CGuiHist;
import cn.academy.client.gui.cgui.CWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ecran du Matrix sans fil : ses quatre emplacements, et le reseau qu'il porte.
 *
 * <p>La page de l'inventaire, comme les autres machines — l'original appelait
 * {@code InventoryPage("matrix")}, soit {@code page_inv} avec le fond du Matrix. Et quatre
 * emplacements aux positions de {@code ContainerMatrix} : trois plaques de contrainte et un coeur.
 *
 * <h2>Un panneau a deux formes</h2>
 *
 * <p>C'est la premiere machine dont le panneau <b>change de forme</b>. Un Matrix sans nom de reseau
 * n'en a pas encore : il propose alors de le creer — deux champs vides et un bouton « INIT ». Une
 * fois le reseau ne, les memes champs montrent son nom et son mot de passe, et se modifient. C'est ce
 * que l'original rebatissait apres chaque reponse du serveur, et c'est ce que fait {@code rebuild()}
 * ici : le panneau se rebatit au moment ou l'etat change, et pas avant.
 *
 * <p>Le reste du panneau suit l'original : un histogramme de la charge sur la capacite, puis le
 * poseur, la portee et la bande passante.
 *
 * <h2>Ce qui reste dehors</h2>
 *
 * <p>Le mot de passe du reseau n'ouvre encore rien : le noeud du port cherche son Matrix tout seul et
 * ne demande rien a personne. Le champ existe, se garde et se montre — mais la regle qui ferait
 * entrer un noeud dans un reseau protege reste a ecrire, et elle changera le raccordement de tout le
 * monde.
 */
public class MatrixScreen extends CGuiContainerScreen<MatrixMenu> {

    /** Ce qu'un Matrix sans poseur affiche a sa place. */
    private static final String NONE = "-";

    /** Le champ du poseur, et les deux du reseau : relus a chaque image. */
    private CWidget ownerField;
    private CWidget ssidField;
    private CWidget passwordField;

    /** Ce que le panneau montrait quand il a ete bati : c'est ce qui dit s'il faut le rebatir. */
    private boolean shownInitialized;
    private boolean shownOwner;
    private int shownCapacity;

    public MatrixScreen(MatrixMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void build(CGuiContainerScreen<MatrixMenu> screen) {
        inventoryPage("matrix");

        histogram(CGuiHist.capacity(menu::load, menu.getCapacity()));

        seplineInfo();
        ownerField = property(label("prop.owner"), NONE).child("value");
        property(label("prop.range"), Integer.toString(menu.getRange()));
        property(label("prop.bandwidth"), menu.getBandwidth() + " IF/T");

        // L'etat du moment : c'est lui qui dit a frame() si le panneau est encore d'actualite.
        shownInitialized = menu.initialized();
        shownOwner = menu.isOwner();
        shownCapacity = menu.getCapacity();

        if (shownInitialized) {
            sepline("wireless_info");

            // Le nom du reseau se change chez son poseur, et se lit chez les autres. Le mot de passe,
            // lui, ne se montre qu'a lui — masque, chez les autres.
            ssidField = property(label("prop.ssid"), menu.ssid(), shownOwner ? menu::rename : null);
            if (shownOwner) sepline("change_pass");

            passwordField = property(label("prop.password"), menu.password(),
                    shownOwner ? menu::repassword : null);
            CWidget hidden = passwordField.child("value");
            if (hidden != null && hidden.text != null) hidden.text.doesEcho = true;
        } else if (shownOwner) {
            sepline("wireless_init");

            // Deux champs vides : le nom qu'on va donner au reseau, et son mot de passe. Valider l'un
            // ou l'autre cree le reseau, comme le bouton — l'original les laissait sans rien faire,
            // mais un champ qui accepte la touche Entree pour ne rien faire est un piege.
            ssidField = property(label("prop.ssid"), "", value -> createNetwork());
            passwordField = property(label("prop.password"), "", value -> createNetwork(), true);
            blank(1);
            button("INIT", this::createNetwork);
        } else {
            sepline("wireless_noinit");
        }
    }

    private static String label(String key) {
        return Component.translatable("ac.gui.common." + key).getString();
    }

    /** Ce que le bouton INIT fait : ce que les deux champs portent devient le reseau. */
    private void createNetwork() {
        menu.initNetwork(content(ssidField), content(passwordField));
    }

    private static String content(CWidget field) {
        return field == null || field.text == null ? "" : field.text.content;
    }

    @Override
    protected void frame(GuiGraphics graphics, double mouseX, double mouseY) {
        // Le panneau se rebatit quand la machine change de forme : un reseau qui vient de naitre
        // remplace les deux champs vides et le bouton par son nom et son mot de passe. La capacite en
        // fait partie, parce qu'elle est le maximum de l'histogramme et qu'un coeur insere la change.
        if (menu.initialized() != shownInitialized || menu.isOwner() != shownOwner
                || menu.getCapacity() != shownCapacity) {
            rebuild();
            return;
        }

        // Les valeurs se relisent tant qu'on ne les ecrit pas : c'est ainsi qu'une modification
        // acceptee par le serveur finit par se voir, sans que l'ecran ait a la deviner.
        refresh(ownerField, menu.owner().isEmpty() ? NONE : menu.owner());
        if (shownInitialized) {
            refresh(ssidField, menu.ssid());
            refresh(passwordField, menu.password());
        }
    }

    /** Reecrit la valeur d'un champ, sauf s'il a le clavier — on ne corrige pas ce qu'on tape. */
    private void refresh(CWidget field, String value) {
        if (field == null || field.text == null || isFocused(field)) return;
        field.text.content = value;
    }
}
