package cn.academy.client.hud;

/**
 * Un element du HUD, et son ancrage.
 *
 * <p>Portage de l'{@code ACHud} de l'original. Le HUD n'est pas un dessin fixe : c'est une
 * liste d'elements <b>nommes</b>, chacun ayant une position reglable en jeu et rangee dans
 * la config du mod. Le joueur les deplace par <i>Settings &rarr; Misc &rarr; Customize UI</i>.
 *
 * <h2>Les coordonnees</h2>
 *
 * <p>Chaque element a un <b>ancrage horizontal</b> (gauche ou droite) et un <b>ancrage
 * vertical</b> (haut, milieu ou bas). Le X et le Y enregistres sont des <b>pixels</b>
 * comptes depuis cet ancrage, et leur signe suit celui de l'original :
 *
 * <ul>
 *   <li>ancre a droite : X negatif rentre vers la gauche de l'ecran ;</li>
 *   <li>ancre en haut : Y positif descend depuis le haut ;</li>
 *   <li>ancre au milieu : Y positif descend depuis le milieu ;</li>
 *   <li>ancre en bas : Y negatif monte depuis le bas.</li>
 * </ul>
 *
 * <p>Ce ne sont pas des regles inventees : ce sont celles qui replacent les quatre defauts
 * de l'original la ou il les met. La barre de CP a {@code -12/12} et se pose a 12 pixels du
 * bord droit et 12 du haut ; le lecteur media a {@code -6/-6} et se pose en bas a droite ;
 * les notifications a {@code 0/15} en haut a gauche ; le rappel des touches a {@code 0/30},
 * 30 pixels sous le milieu, contre le bord droit.
 *
 * <p>Chaque element porte deux tailles : la sienne, et celle de son <b>apercu</b> dans
 * l'ecran de reglage. Les deux ne sont pas toujours egales — l'original montre le rappel
 * des touches au double de sa taille, pour qu'on le voie — et c'est sa taille d'apercu qui
 * a servi a le poser dans cet ecran-la, comme il le faisait.
 *
 * <p>Rien ici ne connait Minecraft : c'est un modele, donc relisible en JUnit.
 */
public enum HudElement {

    /**
     * Le rappel des touches d'aptitude : contre le bord droit, sous le milieu. 140x210 a 0.23.
     *
     * <p>L'ordre de ces quatre constantes <b>est</b> celui du panneau "Elements" de
     * l'original — Control Hint, Media Player, Notification, CP Indicator — et non un ordre
     * choisi : l'original les listait dans l'ordre ou ils s'inscrivaient, et changer le notre
     * reorganiserait sa liste sous les yeux du joueur.
     */
    KEY_HINT("keyhint", "ac.gui.uiedit.elm.keyhint", Side.RIGHT, Slide.MIDDLE, 0.0, 30.0,
            32, 48, 64, 97),

    /** Le morceau en cours : bas a droite. 145x36, sans echelle. */
    MEDIA("media", "ac.gui.uiedit.elm.media", Side.RIGHT, Slide.BOTTOM, -6.0, -6.0,
            145, 36, 145, 36),

    /** Les messages du mod : haut a gauche. 517x170 a 0.25. */
    NOTIFICATION("notification", "ac.gui.uiedit.elm.notification", Side.LEFT, Slide.TOP, 0.0, 15.0,
            129, 43, 129, 43),

    /** Le temoin de points de controle : haut a droite. Sa barre fait 964x147 a l'echelle 0.2. */
    CP_BAR("cpbar", "ac.gui.uiedit.elm.cpbar", Side.RIGHT, Slide.TOP, -12.0, 12.0,
            193, 29, 193, 29);

    /** De quel cote de l'ecran l'element est accroche. */
    public enum Side { LEFT, RIGHT }

    /** A quelle hauteur de l'ecran l'element est accroche. */
    public enum Slide { TOP, MIDDLE, BOTTOM }

    private final String name;
    private final String labelKey;
    private final Side side;
    private final Slide slide;
    private final double defaultX;
    private final double defaultY;
    private final int width;
    private final int height;
    private final int previewWidth;
    private final int previewHeight;

    HudElement(String name, String labelKey, Side side, Slide slide,
               double defaultX, double defaultY,
               int width, int height, int previewWidth, int previewHeight) {
        this.name = name;
        this.labelKey = labelKey;
        this.side = side;
        this.slide = slide;
        this.defaultX = defaultX;
        this.defaultY = defaultY;
        this.width = width;
        this.height = height;
        this.previewWidth = previewWidth;
        this.previewHeight = previewHeight;
    }

    /** La largeur de l'element, en pixels d'interface. */
    public int getWidth() {
        return width;
    }

    /** La hauteur de l'element, en pixels d'interface. */
    public int getHeight() {
        return height;
    }

    /**
     * La largeur de l'apercu de l'element dans l'ecran de reglage.
     *
     * <p>Differente de {@link #getWidth()} pour le rappel des touches : l'original le montre
     * au double de sa taille, et c'est cette taille-la qu'il utilise pour le poser dans
     * l'ecran de reglage.
     */
    public int getPreviewWidth() {
        return previewWidth;
    }

    public int getPreviewHeight() {
        return previewHeight;
    }

    /** Le nom de l'element, tel qu'il est ecrit dans la config. C'est l'identite stable. */
    public String getName() {
        return name;
    }

    /** La cle de langue de son nom, dans le panneau « Elements ». */
    public String getLabelKey() {
        return labelKey;
    }

    public Side getSide() {
        return side;
    }

    public Slide getSlide() {
        return slide;
    }

    public double getDefaultX() {
        return defaultX;
    }

    public double getDefaultY() {
        return defaultY;
    }

    /**
     * Le nom de l'element dans la config, ou {@code null} si ce nom n'en designe aucun.
     *
     * <p>Sert a relire une config ecrite a la main : un nom inconnu est ignore au lieu de
     * faire tomber le jeu.
     */
    public static HudElement byName(String name) {
        if (name == null) return null;
        for (HudElement element : values()) {
            if (element.name.equals(name)) return element;
        }
        return null;
    }
}
