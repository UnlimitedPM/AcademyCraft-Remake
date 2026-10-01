package cn.academy.ability.meltdowner;

/**
 * Les nombres de l'effet du reacteur, portage de {@code RippleMarkRender} et de
 * {@code RenderDiamondShield}.
 *
 * <p>Classe sans aucun type qui ait besoin du jeu, expres : ce sont les courbes de l'original —
 * trois ondes qui se relaient sur un cycle de 3,6 secondes, et un bouclier de diamant qui
 * apparait pendant le vol — et les figer par un test est la seule facon de verifier qu'elles ne
 * bougent pas sans lancer un jeu.
 */
public final class JetEngineVisuals {

    // --- LA MARQUE AU SOL ---

    /** Le cycle d'une onde, en secondes : le {@code CYCLE} de l'original. */
    public static final double CYCLE = 3.6;

    /**
     * Les trois ondes, et leur decalage dans le cycle.
     *
     * <p>Elles se relaient a un tiers de cycle : chacune part quand la precedente a fait un tiers
     * du chemin, et c'est ce qui donne a la marque son air de vague continue plutot que de
     * clignotement.
     */
    public static final double[] OFFSETS = { 0.0, -1.2, -2.4 };

    /** Taille de l'onde : elle part large et se resserre, de 1,9 a 1,4. */
    public static final float SIZE_FROM = 1.9f;
    public static final float SIZE_TO = 1.4f;

    /** Hauteur gagnee par seconde, en blocs : trente centimetres. */
    public static final double RISE_PER_SECOND = 0.3;

    /** Duree du fondu d'entree, puis du fondu de sortie, en secondes. */
    public static final double FADE_IN = 1.6;
    public static final double FADE_OUT = 1.6;

    /** Sa couleur, celle que l'original posait sur la marque : un vert franc. */
    public static final float RED = 51 / 255f;
    public static final float GREEN = 255 / 255f;
    public static final float BLUE = 51 / 255f;

    // --- LE BOUCLIER DE DIAMANT ---

    /** Taille de l'entite de l'original, en blocs. */
    public static final float SHIELD_SIZE = 1.8f;

    /** Et l'echelle appliquee a sa pyramide, {@code glScalef(s, s, s)}. */
    public static final float SHIELD_SCALE = 1.5f;

    /** Ou il flotte : un bloc devant les yeux, 1,1 bloc au-dessus des pieds. */
    public static final double SHIELD_FORWARD = 1.0;
    public static final double SHIELD_HEIGHT = 1.1;

    // --- LA TRAINEE ---

    /** Etincelles posees par tick de vol : dix, comme l'original. */
    public static final int TRAIL_PER_TICK = 10;

    /** Autour du porteur, dans un cube de 0,3 bloc de cote. */
    public static final double TRAIL_SPREAD = 0.3;

    /** Et avec une derive de deux centimetres par tick, dans n'importe quel sens. */
    public static final double TRAIL_DRIFT = 0.02;

    private JetEngineVisuals() {}

    /** La competence qui possede ces effets, et elle seule. */
    public static final String SKILL = "jet_engine";

    /**
     * Ce maintien doit-il montrer la marque au sol ?
     *
     * <p>La meme regle que le bouclier de lumiere : le port dessinait l'effet pour n'importe quel
     * maintien, et c'est le nom de la competence qui decide — celui que {@code ClientCharge} porte
     * deja pour sa boucle sonore.
     */
    public static boolean showsMark(String skill, boolean sustained) {
        return sustained && SKILL.equals(skill);
    }

    /**
     * Ou en est l'onde numero {@code index}, dans son cycle.
     *
     * @return un temps entre 0 et {@link #CYCLE}, en secondes
     */
    public static double phase(double ageSeconds, int index) {
        double mod = (ageSeconds - OFFSETS[index % OFFSETS.length]) % CYCLE;
        return mod < 0 ? mod + CYCLE : mod;
    }

    /** Taille de l'onde a cet instant du cycle : elle se resserre en vieillissant. */
    public static float size(double phase) {
        return SIZE_FROM + (SIZE_TO - SIZE_FROM) * (float) (phase / CYCLE);
    }

    /** Hauteur de l'onde au-dessus du sol : elle monte tant qu'elle vit. */
    public static double height(double phase) {
        return phase * RISE_PER_SECOND;
    }

    /**
     * Son opacite : elle apparait en 1,6 seconde, se tient, puis s'efface en 1,6 seconde.
     *
     * <p>Les deux fondus se touchent presque : le cycle fait exactement deux fois 1,6 seconde plus
     * quatre dixiemes de seconde de tenue. L'onde est donc presque tout le temps en train de
     * monter ou de descendre, et c'est ce qui la fait respirer.
     */
    public static float alpha(double phase) {
        if (phase < FADE_IN) return (float) (phase / FADE_IN);
        if (phase > CYCLE - FADE_OUT) return (float) (1 - (phase - (CYCLE - FADE_OUT)) / FADE_OUT);
        return 1f;
    }
}
