package cn.academy.client.hud;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;

/**
 * La geometrie du rappel des touches (Control Hint), et ce qu'une ligne montre.
 *
 * <p>Portage de {@code KeyHintUI} de l'original : une <b>ligne par touche d'aptitude</b>, une
 * plaque en degrade, le capuchon de la touche a gauche et l'icone de la competence a droite.
 * Ses mesures sont les siennes, a l'unite pres ({@code drawSingle}) :
 *
 * <ul>
 *   <li>plaque 185x83, une ligne tous les 92 ;</li>
 *   <li>capuchon 70x70 a (24, 10), cadre de l'icone 72x72 a (94, 5), icone 62x62 a (99, 10) ;</li>
 *   <li>le tout a l'echelle {@code 0,23}, celle de son {@code SCALE}.</li>
 * </ul>
 *
 * <p>Une difference assumee : l'original ne listait que les delegues <b>existants</b>, donc une
 * touche sans competence ne s'affichait pas du tout. Le port, lui, dessine toujours ses quatre
 * touches — celles de son systeme de prereglages — et une touche sans competence montre son
 * capuchon et un cadre vide : le joueur voit ainsi quelles touches existent.
 *
 * <p>Aucun type Minecraft ici : c'est un modele, donc relisible en JUnit.
 */
public final class KeyHintVisuals {

    /** L'echelle du rappel, chez l'original ({@code SCALE = 0.23f}). */
    public static final float SCALE = 0.23f;

    /** Une ligne par touche d'aptitude. */
    public static final int ROWS = 4;

    /** Le pas vertical d'une ligne, et la taille de sa plaque. */
    public static final int ROW_STEP = 92;
    public static final int PLATE_W = 185;
    public static final int PLATE_H = 83;

    /** Le capuchon de la touche, dans la ligne. */
    public static final int CAP_X = 24;
    public static final int CAP_Y = 10;
    public static final int CAP_SIZE = 70;

    /** Le cadre de l'icone, et l'icone dedans. */
    public static final int FRAME_X = 94;
    public static final int FRAME_Y = 5;
    public static final int FRAME_SIZE = 72;
    public static final int ICON_X = 99;
    public static final int ICON_Y = 10;
    public static final int ICON_SIZE = 62;

    /** La couleur de l'etiquette d'une touche, chez l'original ({@code 0xff194246}). */
    public static final int LABEL_COLOR = 0xFF194246;

    /**
     * Le decalage horizontal que l'original appliquait a sa colonne de lignes, en unites.
     *
     * <p>C'est le chiffre le plus important de ce fichier, et il vient de son propre code :
     * avant de dessiner une ligne, `KeyHintUI` faisait
     * <pre>GL11.glTranslated(-200 - availIdx * 200, y, 0)</pre>
     * avec `availIdx` = 0 pour le premier groupe de touches (chaque groupe suivant reculait de
     * 200 unites de plus, c'est ainsi qu'il rangeait ses colonnes). Ses lignes se dessinent
     * ensuite a partir de x = 122, donc son repere va de -78 a +107 unites — pas de 0 a 185.
     *
     * <p>Le port ne connait qu'une colonne et part deja du bord de la plaque (0 au lieu de
     * 122) : il lui faut donc -200 + 122 = -78. Sans ce decalage, plaque, cadre et icone
     * mordent de 18 pixels sur le bord droit de l'ecran — le cadre tombe 12 pixels trop a
     * droite et l'icone y perd la moitie de sa surface (vecu, vu a l'ecran).
     */
    public static final int CONTENT_SHIFT_X = -78;

    /**
     * De combien une COLONNE recule par rapport a la precedente, en unites.
     *
     * <p>C'est le pas de l'original : {@code GL11.glTranslated(-200 - availIdx * 200, y, 0)}.
     * Le port n'a longtemps eu qu'une colonne (les quatre touches d'aptitude) ; la seconde, celle
     * des <b>directions</b> d'une competence qui s'en sert, se pose donc 200 unites plus a gauche.
     */
    public static final int COLUMN_STEP = 200;

    /** Le gris qui noircit le bas d'une icone en recharge : gris 0,6 a 30 %. */
    public static final int COOLDOWN_OVERLAY = 0x4C999999;

    /**
     * Les trois etats de l'original ({@code DelegateState}), avec SA couleur et SA pulsation.
     *
     * <p>C'est ce que le port n'avait pas : son icone etait soit pleine, soit estompee a 40 %,
     * alors que l'original la faisait <b>respirer</b> et lui posait un halo de la couleur de
     * l'etat. Le halo orange dit « en cours de charge », le bleu « en train de s'executer », et
     * l'absence de halo « au repos ». Le joueur decrit le premier comme « le carre orange
     * pleinement chargee, utilisable ».
     */
    public enum State {
        /** Au repos : opacite 0,7 constante, aucun halo. */
        IDLE(0.7f, 0x000000, false),
        /** En charge : halo ORANGE, et l'icone respire entre 40 et 100 %. */
        CHARGE(1.0f, 0xFFAD37, true),
        /** En cours d'execution (un maintien, un vol) : halo BLEU, meme respiration. */
        ACTIVE(1.0f, 0x46B3FF, true);

        /** L'opacite de base de l'icone, avant la respiration. */
        public final float alpha;
        /** La couleur du halo, en RGB. Zero = aucun halo. */
        public final int glow;
        /** Vrai si l'etat fait respirer l'icone (et le halo). */
        public final boolean pulse;

        State(float alpha, int glow, boolean pulse) {
            this.alpha = alpha;
            this.glow = glow;
            this.pulse = pulse;
        }
    }

    /**
     * L'epaisseur du halo autour de l'icone, en unites (le {@code size} de l'original).
     *
     * <p>L'original posait la huit images de degrade ({@code glow_left|right|up|down|ru|rd|lu|ld}),
     * quatre cotes et quatre coins. A cette epaisseur elles ne font qu'un pixel : le port dessine
     * donc un filet UNI de la couleur de l'etat, qui est ce qu'on y voit — le joueur a demande une
     * « petite ligne » et non un halo flou.
     */
    public static final int GLOW_SIZE = 5;

    /**
     * La respiration des etats qui pulsent, entre 0,6 et 1,0.
     *
     * <p>Formule de l'original, gardee telle quelle : {@code 0.6 + (1 + sin((t % 100) / 50)) * 0.2}
     * sur le temps en secondes. Son {@code MathHelper.sin} est une table ; {@link Math#sin} donne
     * le meme nombre a un dix-millieme pres, et le decalage ne se voit pas sur un souffle.
     */
    public static float sinWave(double seconds) {
        double phase = (seconds % 100.0) / 50.0;
        return 0.6f + (float) ((1.0 + Math.sin(phase)) * 0.2);
    }

    /**
     * L'opacite de l'icone : 40 % pendant une recharge (l'original), sinon celle de l'etat,
     * respiree si l'etat respire.
     */
    public static float iconAlpha(State state, float remaining, float wave) {
        if (state == null) state = State.IDLE;
        if (remaining > 0f) return COOLDOWN_ICON_ALPHA;
        float breathe = state.pulse ? wave : 1.0f;
        return state.alpha * (0.4f + breathe * 0.6f);
    }

    /**
     * L'opacite du halo de l'etat, ou zero s'il n'y a rien a allumer.
     *
     * <p>Trois cas : un etat sans halo (au repos), une icone en recharge, et un etat qui respire.
     * L'original multipliait l'alpha de sa couleur par la respiration et la dessinait toujours ;
     * une couleur d'alpha nul rendait donc la meme chose qu'un halo absent.
     */
    public static float glowAlpha(State state, float remaining, float wave) {
        if (state == null || state.glow == 0 || remaining > 0f) return 0f;
        return state.pulse ? wave : 1.0f;
    }

    /**
     * La meme couleur, avec son opacite multipliee par {@code fade}.
     *
     * <p>Sert au fondu d'apparition du rappel : l'aplat gris d'une recharge est une couleur
     * ARGB fixe, donc il ne suivrait pas le fondu des textures sans ce calcul.
     */
    public static int withAlpha(int argb, float fade) {
        int alpha = Math.round(((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, fade)));
        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    /**
     * La teinte du capuchon : gris 0,7 quand l'aptitude ne peut pas servir <b>ou</b> quand la
     * competence est en recharge, blanche sinon.
     *
     * <p>L'original testait les deux ensemble ({@code !canUseAbility || data.getTickLeft() > 0}) ;
     * le port ne grisait que pour l'aptitude, donc une touche en recharge gardait un capuchon
     * eclatant alors que son icone etait deja eteinte.
     */
    public static float capTint(boolean usable, float remaining) {
        return usable && remaining <= 0f ? 1.0f : 0.7f;
    }

    /**
     * Le decalage d'une colonne : la premiere est celle de l'original, les suivantes reculent.
     *
     * <p>La colonne 0 est celle des quatre touches d'aptitude, la 1 celle des directions.
     */
    public static int contentShift(int column) {
        return CONTENT_SHIFT_X - COLUMN_STEP * column;
    }

    /**
     * Les quatre directions, dans l'ordre ou le port les lit ({@code options.keyLeft} puis
     * {@code keyRight}, {@code keyUp}, {@code keyDown}) — c'est l'ordre de la VISEe, celui que
     * {@code AbilityClientEvents} transmet au serveur.
     */
    public static final String[] DIRECTIONS = { "a", "d", "w", "s" };

    /**
     * L'ordre des LIGNES de la colonne des directions : l'avant en haut, puis gauche, arriere,
     * droite — la croix ZQSD se lit comme cela.
     *
     * <p>Ce sont les indices de {@link #DIRECTIONS} et de {@code directionHeld}, pas un ordre a
     * part : le port lit les touches dans l'ordre gauche/droite/avant/arriere (celui de sa visee),
     * donc l'avant y occupe la place 2. Le joueur : « normalement le Z est en haut, pas en 3e ».
     */
    public static final int[] DIRECTION_ROWS = { 2, 0, 3, 1 };

    /**
     * L'icone d'une direction, pour les competences qui en dessinent une par direction.
     *
     * <p>Seul le scintillement a quatre images (l'original : {@code abilities/teleporter/
     * flashing/a|d|w|s}) ; les ailes de tempete montrent leur propre icone sur les quatre
     * lignes, ce que rend le {@code null} d'ici.
     */
    public static String directionIcon(String skillName, int index) {
        if (!"flashing".equals(skillName)) return null;
        if (index < 0 || index >= DIRECTIONS.length) return null;
        return "textures/abilities/teleporter/flashing/" + DIRECTIONS[index] + ".png";
    }

    /**
     * L'etat d'une competence rangee sur une touche : au repos, en preparation ({@link State#CHARGE},
     * halo orange) ou en cours d'execution ({@link State#ACTIVE}, halo bleu).
     *
     * <p>C'est la regle de l'original, et le port l'avait INVERSEE : la-bas, un contexte vivant
     * rend {@code ACTIVE} (bleu) et <b>seules</b> les competences qui declarent une preparation
     * rendent {@code CHARGE} ({@code PlasmaCannon} avant sa fenetre, {@code Groundshock} sur ses
     * cinq premiers ticks, {@code StormWing} avant l'ouverture des ailes). Le scintillement, le
     * meltdowner, le railgun charge, un maintien : tout cela etait donc BLEU. L'orange n'apparait
     * que la ou la competence le dit, et pendant l'armement du railgun (le seul geste que
     * l'original colorait ainsi sans ouvrir de contexte).
     */
    public static State stateOf(Skill skill, AbilityData data, boolean armed) {
        if (skill == null) return State.IDLE;
        if (!cn.academy.ability.client.ClientCharge.isOpen(skill.getName())) {
            return armed ? State.CHARGE : State.IDLE;
        }
        Skill.HintState state = skill.hintState(data,
                cn.academy.ability.client.ClientCharge.getTicks(skill.getName()));
        return switch (state) {
            case CHARGE -> State.CHARGE;
            case ACTIVE -> State.ACTIVE;
            default -> State.IDLE;
        };
    }

    /** De quel genre de touche il s'agit : c'est l'image du capuchon qui change. */
    public enum Cap {
        MOUSE_LEFT, MOUSE_RIGHT, MOUSE_OTHER, KEY
    }

    private KeyHintVisuals() {}

    /** Le genre de touche, tel que le dit son {@code KeyMapping}. */
    public static Cap cap(boolean mouse, int button) {
        if (!mouse) return Cap.KEY;
        return switch (button) {
            case 0 -> Cap.MOUSE_LEFT;
            case 1 -> Cap.MOUSE_RIGHT;
            default -> Cap.MOUSE_OTHER;
        };
    }

    /** Le nom du fichier du capuchon, dans {@code textures/guis/key_hint/}. */
    public static String capTexture(Cap cap, String label) {
        return switch (cap) {
            case MOUSE_LEFT -> "mouse_left";
            case MOUSE_RIGHT -> "mouse_right";
            case MOUSE_OTHER -> "mouse_generic";
            case KEY -> label.length() <= 2 ? "key_short" : "key_long";
        };
    }

    /** Ce que le capuchon porte : la lettre, le numero du bouton, ou rien du tout. */
    public static String capLabel(Cap cap, String label, int button) {
        return switch (cap) {
            case MOUSE_LEFT, MOUSE_RIGHT -> "";
            case MOUSE_OTHER -> String.valueOf(button);
            case KEY -> label;
        };
    }

    /**
     * La part de recharge restante, entre 0 et 1.
     *
     * <p>L'original noircissait le bas de l'icone de cette part, ce qui dit d'un coup d'oeil
     * combien de temps il reste — voir {@link #COOLDOWN_OVERLAY}.
     */
    public static float cooldownFraction(int ticksLeft, int totalTicks) {
        if (ticksLeft <= 0 || totalTicks <= 0) return 0f;
        return Math.min(1f, ticksLeft / (float) totalTicks);
    }

    /**
     * L'opacite d'une icone dont la competence est en recharge : {@code alpha = 0.4f} chez
     * l'original, quand sa recharge courait ({@code prog != 0}), et celle de l'etat sinon —
     * voir {@link #iconAlpha}.
     *
     * <p>C'est ce qui fait lire une competence indisponible d'un coup d'oeil : sans cet
     * estompage, une touche en recharge s'affiche aussi vive qu'une touche prete, et seule la
     * barre grise du bas le dit — le joueur appuie et se fait refuser.
     */
    public static final float COOLDOWN_ICON_ALPHA = 0.4f;

    /**
     * Les rangs des touches a dessiner : celles qui portent une competence, dans l'ordre.
     *
     * <p>L'original ne parcourait que ses delegues <b>existants</b> et avancait d'une ligne a
     * chacun : une touche sans competence ne laissait donc ni plaque, ni capuchon, ni trou —
     * les lignes se suivaient. Le port en dessinait quatre quoi qu'il arrive (il a fallu que le
     * joueur le signale pour qu'on s'en apercoive) : une touche vide montrait une plaque, un
     * capuchon et un cadre vide, et laissait un trou luisant dans le rappel.
     *
     * <p>PUR : elle ne prend que les noms du prereglage, donc elle se relit en JUnit.
     */
    public static java.util.List<Integer> visibleSlots(java.util.List<String> names) {
        java.util.List<Integer> slots = new java.util.ArrayList<>();
        for (int slot = 0; slot < names.size(); slot++) {
            if (names.get(slot) != null) slots.add(slot);
        }
        return slots;
    }

    /**
     * Une competence par son nom, ou {@code null} si aucune categorie ne la porte.
     *
     * <p>Le prereglage ne garde que le <b>nom</b> de la competence : les quatre categories
     * sont donc fouillees, comme partout ailleurs dans le port.
     */
    public static Skill skillByName(String name) {
        if (name == null) return null;
        for (Category category : CategoryManager.INSTANCE.getCategories()) {
            Skill skill = category.getSkill(name);
            if (skill != null) return skill;
        }
        return null;
    }
}
