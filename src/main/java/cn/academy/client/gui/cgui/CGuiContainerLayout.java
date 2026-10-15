package cn.academy.client.gui.cgui;

/**
 * La geometrie d'un ecran de machine, en nombres nus : les chiffres de l'original, a un seul endroit.
 *
 * <p>Ils vivent ici plutot que dans l'ecran pour une raison bete : un {@code AbstractContainerScreen}
 * ne se construit pas sans {@code Minecraft}, donc un ecran qui garderait ses nombres ne se testerait
 * pas. Ce fichier-ci ne connait que des widgets, et tout ce qu'il affirme se verifie en JUnit.
 *
 * <h2>Pourquoi le conteneur fait 207 sur 186</h2>
 *
 * <p>Le conteneur vanilla fait 176 sur 166, et les <b>slots</b> se comptent depuis son coin. Le
 * document de l'original, lui, fait 172 sur 187 et se centre sur l'ecran. Les deux trames ne
 * tombent donc pas l'une sur l'autre : c'est le {@code +31} et le {@code +20} qui les recollent, et
 * le demi-pixel qui reste — la racine tombe a {@code guiLeft - 0,5} — est exactement ce que donne
 * le calcul de l'original. Sans ces deux nombres, chaque slot se dessine a cote de son fond.
 */
final class CGuiContainerLayout {

    /** Le conteneur : 176 + 31 et 166 + 20. Voir le commentaire de la classe. */
    static final int WIDTH = 207;
    static final int HEIGHT = 186;

    /** La racine de l'original : 172 sur 187, posee dix-huit unites a gauche du centre. */
    static final double ROOT_WIDTH = 172;
    static final double ROOT_HEIGHT = 187;
    static final double ROOT_X = -18;

    /** Une page : 176 sur 187, centree dans la racine, donc DEUX unites a sa gauche. */
    static final double PAGE_WIDTH = 176;
    static final double PAGE_HEIGHT = 187;

    /** Un onglet : vingt-quatre unites, a l'echelle 0,7, a vingt unites a gauche, tous les 22. */
    static final double TAB_SIZE = 24;
    static final double TAB_SCALE = 0.7;
    static final double TAB_X = -20;
    static final double TAB_STEP = 22;

    /** Le panneau d'infos : sept unites a droite de la racine, cinq en dessous de son haut. */
    static final double INFO_X = ROOT_WIDTH + 7;
    static final double INFO_Y = 5;
    static final double INFO_WIDTH = 100;

    private CGuiContainerLayout() {}

    /** La racine : accrochee au centre de son parent, decalee de dix-huit unites a gauche. */
    static CWidget root(CWidget root) {
        root.transform.width = ROOT_WIDTH;
        root.transform.height = ROOT_HEIGHT;
        root.transform.x = ROOT_X;
        root.transform.y = 0;
        root.transform.scale = 1.0;
        root.transform.alignWidth = "CENTER";
        root.transform.alignHeight = "CENTER";
        root.transform.pivotX = 0;
        root.transform.pivotY = 0;
        root.transform.doesDraw = true;
        return root;
    }

    /** Pose l'arbre sur le conteneur : c'est ce qui aligne le document sur les slots. */
    static void place(CWidget root, double leftPos, double topPos) {
        CGuiLayout.place(root, leftPos, topPos, WIDTH, HEIGHT, 1.0);
    }

    /** Une page : centree dans la racine, cachee tant que son onglet n'est pas ouvert. */
    static CWidget page(CWidget window) {
        window.transform.width = PAGE_WIDTH;
        window.transform.height = PAGE_HEIGHT;
        window.transform.alignWidth = "CENTER";
        window.transform.alignHeight = "CENTER";
        window.transform.scale = 1.0;
        window.transform.doesDraw = false;
        return window;
    }

    /** L'onglet d'une page : le bouton du fichier, avec l'icone de son identifiant. */
    static CWidget tab(CWidget button, int index, String page) {
        button.transform.width = TAB_SIZE;
        button.transform.height = TAB_SIZE;
        button.transform.alignWidth = "LEFT";
        button.transform.alignHeight = "TOP";
        button.transform.scale = TAB_SCALE;
        button.transform.x = TAB_X;
        button.transform.y = index * TAB_STEP;
        if (button.texture != null) {
            button.texture.texture = "academy:textures/guis/icons/icon_" + page + ".png";
        }
        return button;
    }

    /** Le panneau d'infos : il se pose a droite de la racine, et c'est son cadre qui le fonce. */
    static CWidget info(CWidget info, double contentHeight) {
        info.transform.x = INFO_X;
        info.transform.y = INFO_Y;
        info.transform.width = INFO_WIDTH;
        info.transform.height = Math.max(50, contentHeight + 8);
        info.transform.alignWidth = "LEFT";
        info.transform.alignHeight = "TOP";
        info.transform.scale = 1.0;
        info.transform.doesDraw = true;

        // Son fond ne vient pas d'une image : c'est le cadre souple de l'original, qui pose un noir
        // a moitie derriere la boite et deux bandes claires sur ses bords haut et bas. Le poser en
        // plus d'un aplat noir le foncerait deux fois.
        info.texture = null;
        info.frame = new CWidget.Frame();
        return info;
    }

    /**
     * L'alpha du souffle des fonds de page : l'original les fait respirer entre 0,675 et 0,85,
     * une fois toutes les cinq secondes environ (un sinus de periode 0,8 seconde).
     */
    static int breatheAlpha(double seconds) {
        double sin = (1 + Math.sin(seconds / 0.8)) * 0.5;
        return (int) ((0.675 + sin * 0.175) * 255);
    }
}
