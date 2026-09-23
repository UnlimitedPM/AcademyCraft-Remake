package cn.academy.client.gui;

/**
 * La geometrie de l'ecran de reglage du HUD, relevee sur l'original.
 *
 * <p>L'original ne dessinait pas cet ecran a la main : il le lisait dans
 * {@code assets/academy/guis/ui_edit.xml}. Ce fichier est la source de tous les nombres
 * ci-dessous. Le fait que le panneau fasse 144x203 et soit pose en (94, 104) n'est pas un
 * hasard : c'est l'endroit d'ou il ne recouvre pas l'apercu des notifications, qui est en
 * haut a gauche de l'ecran.
 *
 * <p>Le panneau entier est dessine a <b>moitie</b> (c'est son echelle dans l'original), d'ou
 * les conversions : les nombres sont ecrits ici dans les unites du xml, et {@link #scaled(int)}
 * les ramene a l'ecran. Les textes suivent la meme regle : la police de l'original n'est pas
 * la notre, et son corps se lit en douziemes — voir {@link #fontScale(float)}.
 *
 * <p>Rien ici ne connait Minecraft : c'est de la geometrie, donc verifiable en JUnit.
 */
public final class CustomizeUiLayout {

    /** Le panneau : 144x203 pose en (94, 104), a l'echelle 0.5. */
    public static final int PANEL_X = 94;
    public static final int PANEL_Y = 104;
    public static final int PANEL_W = 144;
    public static final int PANEL_H = 203;
    public static final double SCALE = 0.5;

    /** L'en-tete du panneau : 128x28 en (8, 8), son texte a 5 pixels du bord, en corps 18. */
    public static final int HEADER_X = 8;
    public static final int HEADER_Y = 8;
    public static final int HEADER_H = 28;
    public static final int HEADER_TEXT_X = 5;
    public static final float HEADER_FONT = 18.0f;
    /** La couleur de son texte : blanc, un peu adouci. */
    public static final int HEADER_TEXT = 0xBBFFFFFF;

    /** Le corps de la liste : 128x160 en (8, 36). Une ligne fait 128x24. */
    public static final int BODY_X = 8;
    public static final int BODY_Y = 36;
    public static final int BODY_W = 128;
    public static final int BODY_H = 160;
    public static final int ROW_W = 128;
    public static final int ROW_H = 24;
    /** Le texte d'une ligne commence a 10 pixels du bord, et s'ecrit en corps 18. */
    public static final int ROW_TEXT_X = 10;
    public static final float ROW_FONT = 18.0f;
    public static final int ROW_TEXT = 0xFFFFFFFF;
    /** Le fond d'une ligne : blanc a 10 %, et blanc a 50 % quand la souris la survole. */
    public static final int ROW_TINT = 0x19FFFFFF;
    public static final int ROW_TINT_HOVER = 0x7FFFFFFF;

    /** Le cadre des deux champs : 90x16, a 5 pixels a droite de la ligne choisie. */
    public static final int EDIT_W = 90;
    public static final int EDIT_H = 16;
    public static final int EDIT_GAP = 5;
    public static final int EDIT_BACK = 0xBB222222;
    public static final int EDIT_EDGE = 0xFFFFFFFF;

    /** Dans ce cadre : le « X » a 2, son champ a 10, le « Y » a 46, le sien a 53. */
    public static final int LABEL_X = 2;
    public static final int FIELD_X = 10;
    public static final int LABEL_Y = 46;
    public static final int FIELD_Y = 53;
    public static final int FIELD_W = 34;
    public static final int FIELD_H = 10;
    /** Les deux champs sont a 3 pixels du haut du cadre, sur un fond gris fonce. */
    public static final int FIELD_TOP = 3;
    public static final int FIELD_BACK = 0xFF333333;
    public static final int FIELD_TEXT = 0xFFFFFFFF;
    /** Le rouge de l'original quand la saisie ne passe pas. */
    public static final int FIELD_BAD = 0xFFBB3333;

    /** Le lecteur media, tel que son {@code media_player_aux.xml} le pose : 145x36, sans fond. */
    public static final int MEDIA_TITLE_X = 13;
    /** Son titre est cale par le BAS de sa boite de 10, posee a 17 : donc a 27. */
    public static final int MEDIA_TITLE_BOTTOM = 27;
    public static final float MEDIA_TITLE_FONT = 10.0f;
    /** Sa duree est calee de meme, mais sa boite part de 27 : son bas tombe a 37, dix plus bas. */
    public static final int MEDIA_TIME_X = 117;
    public static final int MEDIA_TIME_BOTTOM = 37;
    public static final float MEDIA_TIME_FONT = 8.5f;
    /**
     * La barre du lecteur media, en <b>dixiemes de pixel</b> : son xml donne 1,5 de haut pour le
     * fond gris et un peu plus pour la progression blanche, qui deborde donc a peine de chaque
     * cote. Au pixel entier ce debordement serait invisible ; a un dixieme il se voit comme chez
     * lui.
     */
    public static final int MEDIA_BAR_X = 14;
    public static final int MEDIA_BAR_Y = 27;
    public static final int MEDIA_BAR_W = 120;
    public static final float MEDIA_BAR_PROGRESS = 0.5f;
    public static final int MEDIA_BAR_GREY_TENTHS = 15;
    public static final int MEDIA_BAR_WHITE_TENTHS = 20;
    public static final int MEDIA_BAR_BACK = 0x1F000000;
    public static final int MEDIA_BAR_FILL = 0xCCFFFFFF;
    /** Le pas de dessin de la barre : un dixieme de pixel. */
    public static final float MEDIA_BAR_STEP = 0.1f;

    /**
     * La police n'est plus rasterisee par Minecraft : ses glyphes sont graves d'avance, avec
     * hinting, par {@code scripts/bake-font.ps1}, une planche par taille entiere.
     *
     * <p>Chaque planche est ensuite dessinee a sa taille EXACTE (voir
     * {@link #sheetScale(float, int)}) : echelle fractionnaire veut dire reechantillonnage, donc
     * retour du flou. La taille voulue est donc arrondie au pixel, et c'est cet entier qui
     * designe la planche.
     */
    public static final float FONT_RATIO = 1.0f / 12.0f;

    /**
     * Le corps qui correspond a l'echelle 1 : la taille que la police du systeme dessinait
     * elle-meme, et donc celle qui a ete acceptee a l'ecran avant le gravage des glyphes.
     */
    public static final float BASE_EM = 9.0f;

    /** Les tailles de planches que {@code scripts/bake-font.ps1} grave. */
    public static final int SHEET_MIN = 6;
    public static final int SHEET_MAX = 20;

    /**
     * La hauteur de ligne du jeu, pour centrer un texte dans sa ligne.
     *
     * <p>C'est une constante, et non {@code font.lineHeight} : avec une planche de glyphes, cette
     * derniere vaut la hauteur de la planche (13 pixels pour celle de 9), et le centrage se
     * decalerait d'un pixel.
     */
    public static final int LINE = 9;

    private CustomizeUiLayout() {
    }

    /** Une longueur du panneau, ramenee a l'ecran. */
    public static int scaled(int length) {
        return (int) Math.round(length * SCALE);
    }

    public static int panelWidth() {
        return scaled(PANEL_W);
    }

    public static int panelHeight() {
        return scaled(PANEL_H);
    }

    /** L'echelle d'un texte du panneau : le corps de l'original, ramene a l'ecran. */
    public static float fontScale(float fontSize) {
        return fontSize * FONT_RATIO * (float) SCALE;
    }

    /** L'echelle d'un texte qui n'est pas dans le panneau : il n'y a pas a le reduire. */
    public static float plainFontScale(float fontSize) {
        return fontSize * FONT_RATIO;
    }

    /**
     * La taille voulue, en pixels entiers : {@code BASE_EM} fois l'echelle, arrondi.
     *
     * <p>L'arrondi precede le choix de la planche, et non l'inverse : on ne cherche pas la
     * planche la plus proche d'une taille fractionnaire, on fixe la taille entiere a dessiner
     * puis on grave exactement cette taille. L'ecart qui en resulte (un demi-pixel au pire)
     * est celui qu'aurait la police du jeu, et il ne coute rien : la planche est ensuite
     * dessinee sans reechantillonnage.
     */
    public static int pixelSize(float scale) {
        return Math.round(BASE_EM * scale);
    }

    /**
     * La planche de glyphes a utiliser, pour une echelle qui inclut deja celle du panneau.
     *
     * <p>On prend la planche la plus proche, dans les bornes gravees.
     */
    public static int sheetFor(float scale) {
        return Math.max(SHEET_MIN, Math.min(SHEET_MAX, pixelSize(scale)));
    }

    /**
     * L'echelle a appliquer a cette planche pour tomber pile sur la taille voulue.
     *
     * <p>Elle vaut 1 quand la planche est celle de cette taille, c'est-a-dire dans tous les cas
     * ou la taille voulue tombe dans les bornes gravees. Une planche dessinee a l'echelle 1
     * garde ses pixels : rien n'est reechantillonne, donc rien n'est flou.
     */
    public static float sheetScale(float scale, int sheet) {
        return pixelSize(scale) / (float) sheet;
    }

    /** Le haut d'un texte dont on veut centrer la ligne sur {@code centerY}. */
    public static int textTop(int centerY, int lineHeight) {
        return centerY - lineHeight / 2;
    }

    /** Le bord gauche du titre du panneau. */
    public static int headerTextLeft() {
        return PANEL_X + scaled(HEADER_X + HEADER_TEXT_X);
    }

    /** Le milieu vertical de l'en-tete : son texte y est centre. */
    public static int headerTextCenterY() {
        return PANEL_Y + scaled(HEADER_Y + HEADER_H / 2);
    }

    /** Le bord gauche des lignes de la liste. */
    public static int rowLeft() {
        return PANEL_X + scaled(BODY_X);
    }

    /** La largeur et la hauteur d'une ligne, a l'ecran. */
    public static int rowWidth() {
        return scaled(ROW_W);
    }

    public static int rowHeight() {
        return scaled(ROW_H);
    }

    /** Le haut d'une ligne, la premiere etant a l'index 0. */
    public static int rowTop(int index) {
        return PANEL_Y + scaled(BODY_Y + index * ROW_H);
    }

    /** Le milieu vertical d'une ligne : son texte y est centre. */
    public static int rowTextCenterY(int index) {
        return PANEL_Y + scaled(BODY_Y + index * ROW_H + ROW_H / 2);
    }

    /** Le bord gauche du texte d'une ligne. */
    public static int rowTextLeft() {
        return rowLeft() + scaled(ROW_TEXT_X);
    }

    /** Le bord gauche du cadre des deux champs. */
    public static int editLeft() {
        return rowLeft() + rowWidth() + EDIT_GAP;
    }

    /** Le haut de ce cadre, aligne sur le milieu de la ligne choisie. */
    public static int editTop(int index) {
        return rowTop(index) + rowHeight() / 2 - EDIT_H / 2;
    }

    /** Le bord gauche du champ X (vrai) ou Y (faux). */
    public static int fieldLeft(boolean x) {
        return editLeft() + (x ? FIELD_X : FIELD_Y);
    }

    /** Le haut des deux champs. */
    public static int fieldTop(int index) {
        return editTop(index) + FIELD_TOP;
    }
}
