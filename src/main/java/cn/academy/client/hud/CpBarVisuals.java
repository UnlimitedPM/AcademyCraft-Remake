package cn.academy.client.hud;

/**
 * La geometrie et les couleurs du temoin de points de controle.
 *
 * <p>Portage des nombres de {@code CPBar} de l'original. Sa barre est une image de
 * {@code 964x147} dessinee a <b>0,2</b>, dans laquelle trois morceaux se posent : le fond sur
 * toute l'image, la <b>surcharge</b> dans une bande posee vers le haut, et le <b>remplissage de
 * CP</b> dans un rectangle de {@code 883x84} a partir de {@code (47, 30)}.
 *
 * <p>Deux details de l'original sont repris ici parce qu'ils se voient :
 *
 * <ul>
 *   <li>le remplissage ne part <b>jamais de zero</b> : il vit entre 16 % et 96 % de sa largeur,
 *       donc la barre montre toujours un morceau, meme a vide ;</li>
 *   <li>son extremite gauche est <b>coupee en biais</b> — 103 pixels de plus en bas qu'en haut,
 *       soit 44 degres — et l'original la dessinait a la main, faute de pouvoir le faire avec un
 *       rectangle. C'est {@link #fillLeftAt(float, int)} qui la decrit, ligne par ligne.</li>
 * </ul>
 *
 * <p>Le remplissage est aussi <b>colore selon le niveau</b> : rouge a vide, orange vers un tiers,
 * blanc a plein. La surcharge a sa propre echelle, presque transparente au debut.
 *
 * <p>Aucun type Minecraft ici : ce sont des nombres, relisibles en JUnit.
 */
public final class CpBarVisuals {

    /** L'echelle de dessin de l'original : ses images font 964x147, dessinees a 0,2. */
    public static final float SCALE = 0.2f;

    /** La taille des images de la barre. */
    public static final int TEX_W = 964;
    public static final int TEX_H = 147;

    /** Le remplissage de CP, dans l'image. */
    public static final int FILL_X = 47;
    public static final int FILL_Y = 30;
    public static final int FILL_W = 883;
    public static final int FILL_H = 84;

    /** La coupe de l'extremite : 103 pixels de large pour 44 degres. */
    public static final double CUT = 103.0 * Math.sin(Math.toRadians(44.0));

    /** Le remplissage vit entre 16 % et 96 % de sa largeur, meme a vide ou a plein. */
    public static final float FILL_MIN = 0.16f;
    public static final float FILL_SPAN = 0.8f;

    /**
     * L'embleme du pouvoir, decoupe DANS le remplissage.
     *
     * <p>L'original perlait la barre avec l'icone de la categorie : son {@code cpbar_cp.frag}
     * multiplie l'opacite du remplissage par {@code 1 - icone.a}, dans une boite de 65 unites
     * posee a {@code (857, 43)}. La silhouette du pouvoir se lit donc en creux dans la barre.
     * Chaque categorie a son icone a elle ({@code abilities/<nom>/icon_overlay.png}).
     */
    public static final int ICON_X = 857;
    public static final int ICON_Y = 43;
    public static final int ICON_SIZE = 65;

    /**
     * La couleur du FOND de la barre, celle qui se voit dans le creux de l'embleme.
     *
     * <p>Mesuree sur {@code back_normal.png} : un gris 52, a 128 d'opacite partout ou la plaque
     * est posee. C'est ce que l'original laissait voir dans la decoupe de son icone — le monde,
     * avec la plaque par-dessus —, et c'est donc cette couleur-la qu'on ecrit dans la forme.
     */
    public static final float PLATE_RED = 52f / 255f;
    public static final float PLATE_GREEN = 52f / 255f;
    public static final float PLATE_BLUE = 52f / 255f;

    /**
     * La part de la plaque dans l'embleme, quand elle ne recouvre pas le monde mais le REMPLISSAGE.
     *
     * <p>Chez l'original, le creux laissait voir le monde a travers la plaque — donc quelque chose
     * de sombre, et l'embleme se lisait comme un gris tres peu transparent. Ici il n'y a pas de
     * monde sous le remplissage : la forme se pose sur la barre, qui est claire.
     *
     * <p>REMIS A 1 SUR DEMANDE DU JOUEUR (21/10) : « pour l'instant remet le a 1, on verra plus
     * tard ». Les deux autres essais sont gardes en memoire — 0,50 donnait une silhouette delavee
     * (« trop transparent ») et 0,8 un entre-deux qu'il n'a pas retenu. C'est le seul chiffre a
     * tourner si l'envie revient.
     */
    public static final float EMBLEM_COVERAGE = 1.0f;

    /**
     * Le rappel des prereglages, sous la barre : quatre carres numerotes.
     *
     * <p>C'est le {@code drawPresetHint} de l'original — {@code x0 = 580}, {@code y0 = 136},
     * carres de 52 tous les 62 — dessine pendant deux secondes apres un changement de
     * prereglage. Il sort volontairement du cadre de l'image (qui fait 147 de haut) : chez lui
     * aussi, rien ne le coupait.
     */
    public static final int PRESET_X = 580;
    public static final int PRESET_Y = 136;
    public static final int PRESET_SIZE = 52;
    public static final int PRESET_STEP = 62;
    public static final long PRESET_HOLD_MS = 2000L;

    /** La cellule du numero, et sa hauteur dans le carre : {@code FontOption(46)} a {@code y + 5}. */
    public static final int PRESET_CELL = 46;
    public static final int PRESET_LABEL_LIFT = 5;

    /** L'epaisseur du halo du carre en service, celle de l'original. */
    public static final int PRESET_GLOW = 5;

    /** Le fond d'un carre : un gris sombre translucide. */
    public static final int PRESET_BACK_RGB = 0x303030;
    /** Le numero, blanc. */
    public static final int PRESET_TEXT_RGB = 0xFFFFFF;
    /** Le halo du carre en service, blanc. */
    public static final float PRESET_GLOW_ALPHA = 200f / 255f;

    /** L'opacite du rappel estimes par l'original : le coefficient final de son fondu. */
    public static final float PRESET_ALPHA_SCALE = 0.75f;

    /** Le delai, en millisecondes, avant lequel un premier rappel apparait sans fondu. */
    public static final long PRESET_FADE_AFTER_MS = 3000L;

    /**
     * La vitesse de l'animation, celle de l'original : deux unites de progression par seconde.
     *
     * <p>Chez lui, {@code CP_BALANCE_SPEED} et {@code O_BALANCE_SPEED} valent tous deux 2,0.
     * C'est elle qui fait GLISSER la surcharge au lieu de la faire sauter — dans un sens comme
     * dans l'autre, donc aussi quand la surcharge reflue apres une surcharge pleine. C'est ce
     * reflux, couleur comprise, que le joueur appelle « l'overload qui va a l'envers ».
     */
    public static final float BALANCE_SPEED = 2.0f;

    /** La surcharge se dessine dans cette bande, posee vers le haut de l'image. */
    public static final int OVER_X = 0;
    public static final int OVER_Y = 21;
    public static final int OVER_W = 943;
    public static final int OVER_H = 104;

    /** L'etat surcharge : le bandeau strie se dessine a partir de 30, sur 914 de large. */
    public static final int STRIPE_X = 30;
    public static final int STRIPE_W = 914;

    /** Les trois arrets de la couleur du remplissage, ceux de l'original. */
    private static final float[] CP_STOP = {0.0f, 0.35f, 1.0f};
    private static final int[] CP_COLOR = {0xFFF06767, 0xFFFFAE44, 0xFFFFFFFF};

    /** Les trois arrets de la surcharge : presque transparente, doree, puis rouge. */
    /**
     * Les trois arrets de la surcharge, opacite comprise.
     *
     * <p>Ce sont ceux de l'original (10, 35 puis 80 sur 255), densifies d'un facteur 1,5 a la
     * demande du joueur (30/09) : sa barre lisait plus pale que la sienne, la faute au voile
     * d'ecran de l'original qui n'existe pas encore ici. Le facteur est garde ici, en clair,
     * pour pouvoir le retirer d'un geste le jour ou ce voile arrive.
     */
    private static final float[] OVER_STOP = {0.0f, 0.55f, 1.0f};
    private static final int[] OVER_COLOR = {0x0FDFDFDF, 0x34F0D49D, 0x78F56464};

    private CpBarVisuals() {
    }

    /** La largeur du remplissage, en pixels d'image, pour un niveau de 0 a 1. */
    public static double fillWidth(float progress) {
        return FILL_W * (FILL_MIN + clamp01(progress) * FILL_SPAN);
    }

    /** Le bord gauche du remplissage <b>en haut</b> : c'est de la que part la coupe. */
    public static double fillLeft(float progress) {
        return FILL_X + FILL_W - fillWidth(progress);
    }

    /**
     * Le bord gauche du remplissage a une ligne donnee.
     *
     * <p>Le bas est decale vers la droite : la coupe va du coin haut-gauche au coin bas-droit
     * d'un rectangle de {@link #CUT} de large sur toute la hauteur. Une ligne sur deux de cette
     * bande n'appartient donc pas au remplissage, et c'est ce que le dessin doit respecter.
     */
    public static double fillLeftAt(float progress, int row) {
        return fillLeft(progress) + CUT * row / (double) FILL_H;
    }

    /**
     * Le bord gauche de la boite de l'embleme, ramene DANS le remplissage.
     *
     * <p>A barre presque vide, la coupe en biais du remplissage passe a droite du coin
     * haut-gauche de la boite de l'icone (860 contre 857, sur les dernieres lignes) : percer
     * l'icone entiere y enleverait aussi le fond de la barre. L'original n'avait pas ce
     * probleme — son shader ne touchait qu'aux texels du remplissage —, donc on ramene le bord
     * de la boite sur celui du remplissage : le trou reste dans la barre, comme chez lui.
     */
    public static double emblemLeft(float progress) {
        return Math.max(ICON_X, fillLeftAt(progress, FILL_H - 1));
    }

    /** La largeur de la surcharge, en pixels d'image : elle pousse vers la gauche. */
    public static double overloadWidth(float overload) {
        return OVER_W * clamp01(overload);
    }

    /** Le bord gauche de la surcharge. */
    public static double overloadLeft(float overload) {
        return OVER_X + OVER_W - overloadWidth(overload);
    }

    /** La couleur du remplissage a ce niveau : rouge a vide, orange, puis blanc a plein. */
    public static int fillColor(float progress) {
        return ramp(CP_STOP, CP_COLOR, clamp01(progress));
    }

    /** La couleur de la surcharge : presque transparente, doree, puis rouge. */
    public static int overloadColor(float overload) {
        return ramp(OVER_STOP, OVER_COLOR, clamp01(overload));
    }

    /**
     * L'opacite du rappel des prereglages, pour un temps ecoule.
     *
     * <p>Portage de la courbe du {@code drawPresetHint} de l'original : le rappel sort en
     * fondu sur le premier cinquieme de sa duree <b>seulement s'il n'y en a pas eu un autre</b>
     * depuis trois secondes (sinon deux rappels qui se suivent clignoteraient), et son dernier
     * cinquieme s'efface. Le tout est ramene a 0,75, ce qui le garde lisible sans ecraser la
     * barre.
     *
     * @param elapsedMs temps depuis CE changement de prereglage
     * @param sinceLastMs temps depuis le precedent, ou {@link Long#MAX_VALUE} s'il n'y en a pas
     */
    public static float presetHintAlpha(long elapsedMs, long sinceLastMs) {
        if (elapsedMs < 0 || elapsedMs >= PRESET_HOLD_MS) return 0.0f;
        double progress = elapsedMs / (double) PRESET_HOLD_MS;
        double alpha;
        if (sinceLastMs > PRESET_FADE_AFTER_MS && progress < 0.2) {
            alpha = progress / 0.2;
        } else if (progress > 0.8) {
            alpha = (1.0 - progress) / 0.2;
        } else {
            alpha = 1.0;
        }
        return (float) Math.max(0.0, Math.min(alpha, 1.0) * PRESET_ALPHA_SCALE);
    }

    /**
     * L'opacite du NUMERO d'un carre : quatre cinquiemes de celle du rappel, jamais nulle.
     *
     * <p>L'original ecrivait {@code max(0.05, alpha * 0.8)} : le chiffre reste lisible meme
     * quand le carre s'efface, ce qui evite un clignotement d'un dixieme de seconde.
     */
    public static float presetLabelAlpha(float hintAlpha) {
        return Math.max(0.05f, hintAlpha * 0.8f);
    }

    /** La valeur affichee, avancee vers la valeur reelle d'au plus {@code step}.
     *
     * <p>Portage exact du {@code balance} de l'original : un pas lineaire, jamais de
     * depassement, et le meme pas dans les deux sens. C'est ce qui donne a la barre un mouvement
     * regulier plutot qu'une suite de sauts.
     */
    public static float balance(float from, float to, float step) {
        float delta = to - from;
        if (Math.abs(delta) <= step) return to;
        return from + Math.signum(delta) * step;
    }

    /** Le pas d'un intervalle de temps, en secondes : la vitesse de l'original. */
    public static float balanceStep(float seconds) {
        return Math.max(0.0f, seconds) * BALANCE_SPEED;
    }

    /**
     * L'opacite d'une couleur, de 0 a 1.
     *
     * <p>Les couleurs de la surcharge portent leur propre transparence — {@code 0x0A}, {@code 0x23}
     * puis {@code 0x50} sur 255 chez l'original, soit a peine visible au debut. Encore faut-il
     * la lire : un dessin qui impose son opacite a la place efface justement ce que ces nombres
     * disent.
     */
    public static float alphaOf(int rgb) {
        return ((rgb >>> 24) & 0xFF) / 255.0f;
    }

    /** La meme couleur, a une opacite donnee (de 0 a 1) : RGBA sur l'octet de tete. */
    public static int withAlpha(int rgb, float alpha) {
        int a = Math.round(Math.max(0.0f, Math.min(1.0f, alpha)) * 255.0f);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /** La couleur d'un canal alpha compris, entre deux arrets. */
    private static int ramp(float[] stops, int[] colors, float value) {
        int index = 0;
        while (index < stops.length - 2 && value > stops[index + 1]) {
            index++;
        }
        float span = stops[index + 1] - stops[index];
        float ratio = span <= 0 ? 0.0f : (value - stops[index]) / span;
        return mix(colors[index], colors[index + 1], ratio);
    }

    private static int mix(int from, int to, float ratio) {
        return channel(from, to, ratio, 24) | channel(from, to, ratio, 16)
                | channel(from, to, ratio, 8) | channel(from, to, ratio, 0);
    }

    private static int channel(int from, int to, float ratio, int shift) {
        int a = (from >> shift) & 0xFF;
        int b = (to >> shift) & 0xFF;
        return Math.round(a + (b - a) * ratio) << shift;
    }

    private static float clamp01(float value) {
        return value < 0.0f ? 0.0f : Math.min(value, 1.0f);
    }
}
