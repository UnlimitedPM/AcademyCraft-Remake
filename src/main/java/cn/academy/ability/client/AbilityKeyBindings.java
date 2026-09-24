package cn.academy.ability.client;

import cn.academy.AcademyCraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Les touches du mod : une par competence, et celles des prereglages.
 *
 * <p>Les touches par competence ont ete <b>retirees de l'enregistrement</b> : l'original n'en
 * donnait que quatre, avec quatre prereglages pour dire ce qu'elles allument. Le port avait
 * fini par en poser trente-cinq, jusqu'a la ponctuation et aux touches de defilement, ce qui
 * n'etait plus utilisable. Les declarations mortes qui les suivaient restent dans le fichier
 * le temps d'un passage de menage : elles ne sont plus enregistrees, donc invisibles.
 *
 * <p>Le cablage vit dans {@link AbilityClientEvents}.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AbilityKeyBindings {

    /**
     * Les quatre touches d'aptitude, avec les defauts de l'original.
     *
     * <p>Chez lui c'etaient {@code MOUSE_LEFT, MOUSE_RIGHT, R, F}, reglables dans son propre
     * ecran de touches. Ce sont les SEULES touches qui declenchent une competence : le reste
     * depend du <b>prereglage</b> en service, qui dit quelle competence va sur quelle touche.
     * C'est la reponse de l'original a la penurie de touches, et elle remplace les trente-cinq
     * liaisons « une par competence » que le port avait fini par poser sur de la ponctuation
     * et des touches de defilement.
     */
    public static final KeyMapping ABILITY_1 = mouseKey("key.academy.ability_1", InputConstants.MOUSE_BUTTON_LEFT);
    public static final KeyMapping ABILITY_2 = mouseKey("key.academy.ability_2", InputConstants.MOUSE_BUTTON_RIGHT);
    public static final KeyMapping ABILITY_3 = abilityKey("key.academy.ability_3", InputConstants.KEY_R);
    public static final KeyMapping ABILITY_4 = abilityKey("key.academy.ability_4", InputConstants.KEY_F);

    /** Le passage au prereglage suivant, qui fait le tour des quatre. C, comme chez l'original. */
    public static final KeyMapping PRESET_NEXT = abilityKey("key.academy.preset_next", InputConstants.KEY_C);

    /**
     * L'allumage de l'aptitude.
     *
     * <p>V, comme chez l'original ({@code KEY_ACTIVATE_ABILITY} sur {@code Keyboard.KEY_V}).
     * Tant qu'elle est eteinte, aucune competence ne part et le HUD reste cache : c'est la
     * touche qui les fait apparaitre.
     *
     * <p>ATTENTION : le port lie encore une competence sur V ({@code mag_movement}) : la touche
     * fait donc les deux. Le systeme de prereglages de l'original, qui remplace ces touches
     * par competence, reglera la question a la racine.
     */
    public static final KeyMapping TOGGLE_ABILITY = abilityKey("key.academy.toggle_ability", InputConstants.KEY_V);

    /** L'ecran qui regle les prereglages. N, comme chez l'original. */
    public static final KeyMapping PRESET_EDIT = abilityKey("key.academy.preset_edit", InputConstants.KEY_N);

    /**
     * L'ecran de debogage du mod : informations du joueur, puis etat des competences.
     *
     * <p>F4 comme chez l'original. Le jeu s'en sert deja pour changer de mode de jeu, mais
     * seulement pour les operateurs : les autres joueurs n'y perdent rien, et la touche se
     * reconfigure dans les options comme toutes les autres.
     */
    public static final KeyMapping DEBUG_CONSOLE = abilityKey("key.academy.debug_console", InputConstants.KEY_F4);

    private static KeyMapping abilityKey(String description, int defaultKey) {
        return new KeyMapping(description, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
                defaultKey, "key.categories.academy");
    }

    private static KeyMapping mouseKey(String description, int defaultButton) {
        return new KeyMapping(description, KeyConflictContext.IN_GAME, InputConstants.Type.MOUSE,
                defaultButton, "key.categories.academy");
    }

    // ------------------------------------------------------------------
    // DECLARATIONS MORTES : les trente-cinq touches « une par competence ».
    //
    // Elles ne sont PLUS enregistrees (voir register(), en bas du fichier) : la competence
    // d'une touche vient maintenant du prereglage, et il n'y a plus que quatre touches. Tout
    // ce qui suit n'est donc lu par personne, et n'apparait plus dans les options du jeu.
    // Le port les garde le temps d'un passage de menage, pour que rien d'autre ne casse.
    // ------------------------------------------------------------------

    public static final KeyMapping ACTIVATE_ARC_GEN = new KeyMapping(
            "key.academy.activate_arc_gen", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_G, "key.categories.academy");

    public static final KeyMapping ACTIVATE_RAILGUN = new KeyMapping(
            "key.academy.activate_railgun", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_H, "key.categories.academy");

    public static final KeyMapping ACTIVATE_BODY_INTENSIFY = new KeyMapping(
            "key.academy.activate_body_intensify", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_J, "key.categories.academy");

    public static final KeyMapping ACTIVATE_SHIFT_TP = new KeyMapping(
            "key.academy.activate_shift_tp", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_K, "key.categories.academy");

    public static final KeyMapping ACTIVATE_PENETRATE_TP = new KeyMapping(
            "key.academy.activate_penetrate_tp", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_L, "key.categories.academy");

    public static final KeyMapping ACTIVATE_MELTDOWNER = new KeyMapping(
            "key.academy.activate_meltdowner", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_Y, "key.categories.academy");

    public static final KeyMapping ACTIVATE_ELECTRON_BOMB = new KeyMapping(
            "key.academy.activate_electron_bomb", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_U, "key.categories.academy");

    /** Le bouclier se tient : c'est la touche qui reste enfoncee. */
    public static final KeyMapping ACTIVATE_LIGHT_SHIELD = new KeyMapping(
            "key.academy.activate_light_shield", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_I, "key.categories.academy");

    public static final KeyMapping ACTIVATE_THUNDER_BOLT = new KeyMapping(
            "key.academy.activate_thunder_bolt", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_N, "key.categories.academy");

    public static final KeyMapping ACTIVATE_THUNDER_CLAP = new KeyMapping(
            "key.academy.activate_thunder_clap", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_B, "key.categories.academy");

    /** Brancher sa reserve sur une machine : la touche reste enfoncee. */
    public static final KeyMapping ACTIVATE_CHARGING = new KeyMapping(
            "key.academy.activate_charging", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_C, "key.categories.academy");

    /** S'accrocher a un metal et se faire tirer dessus : la touche reste enfoncee. */
    public static final KeyMapping ACTIVATE_MAG_MOVEMENT = new KeyMapping(
            "key.academy.activate_mag_movement", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_V, "key.categories.academy");

    /** Lancer l'objet tenu : la touche reste enfoncee, l'objet part au relachement. */
    public static final KeyMapping ACTIVATE_THREATENING_TELEPORT = new KeyMapping(
            "key.academy.activate_threatening_teleport", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_X, "key.categories.academy");

    /** Viser loin : la portee grandit tant que la touche est tenue. */
    public static final KeyMapping ACTIVATE_MARK_TELEPORT = new KeyMapping(
            "key.academy.activate_mark_teleport", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_P, "key.categories.academy");

    /** Viser quelqu'un pour le frapper au relachement : la touche reste enfoncee. */
    public static final KeyMapping ACTIVATE_FLESH_RIPPING = new KeyMapping(
            "key.academy.activate_flesh_ripping", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_Z, "key.categories.academy");

    /** Les trois rayons miniers : la touche reste enfoncee tant que le rayon creuse. */
    public static final KeyMapping ACTIVATE_MINE_RAY_BASIC = new KeyMapping(
            "key.academy.activate_mine_ray_basic", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_O, "key.categories.academy");

    public static final KeyMapping ACTIVATE_MINE_RAY_EXPERT = new KeyMapping(
            "key.academy.activate_mine_ray_expert", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_M, "key.categories.academy");

    /**
     * Le troisieme rayon. Le schema « une touche par competence » arrive a saturation —
     * les lettres libres sont prises — donc celui-ci se pose sur la virgule. Le systeme
     * de presets de l'original reste a porter, et c'est lui qui reglera la question.
     */
    public static final KeyMapping ACTIVATE_MINE_RAY_LUCK = new KeyMapping(
            "key.academy.activate_mine_ray_luck", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_COMMA, "key.categories.academy");

    /**
     * La bombe a fragmentation. Les lettres libres sont prises, donc elle se pose sur le
     * point : le systeme de presets de l'original reste a porter, et c'est lui qui reglera
     * la question.
     */
    public static final KeyMapping ACTIVATE_SCATTER_BOMB = new KeyMapping(
            "key.academy.activate_scatter_bomb", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_PERIOD, "key.categories.academy");

    /**
     * Le reacteur. Le schema « une touche par competence » est sature bien au-dela des
     * lettres : il ne reste que de la ponctuation. Le systeme de presets de l'original
     * reste a porter, et c'est lui qui reglera la question.
     */
    public static final KeyMapping ACTIVATE_JET_ENGINE = new KeyMapping(
            "key.academy.activate_jet_engine", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_SEMICOLON, "key.categories.academy");

    /** La salve de rayons. Il ne reste que de la ponctuation : voir la note du reacteur. */
    public static final KeyMapping ACTIVATE_RAY_BARRAGE = new KeyMapping(
            "key.academy.activate_ray_barrage", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_SLASH, "key.categories.academy");

    /**
     * Le scintillement. Pendant son maintien, les quatre touches de deplacement visent et
     * declenchent les sauts : c'est la seule competence dont une partie du geste echappe a
     * cette touche.
     */
    public static final KeyMapping ACTIVATE_FLASHING = new KeyMapping(
            "key.academy.activate_flashing", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_APOSTROPHE, "key.categories.academy");

    /** Le choc dirige : la touche se tient, le coup part au relachement. */
    public static final KeyMapping ACTIVATE_DIRECTED_SHOCK = new KeyMapping(
            "key.academy.activate_dir_shock", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_EQUALS, "key.categories.academy");

    /** L'onde de choc : la touche se tient, le sol s'effondre au relachement. */
    public static final KeyMapping ACTIVATE_GROUNDSHOCK = new KeyMapping(
            "key.academy.activate_ground_shock", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_LBRACKET, "key.categories.academy");

    /** L'onde de choc dirigee : la meme chose, mais la ou le regard s'arrete. */
    public static final KeyMapping ACTIVATE_DIRECTED_BLASTWAVE = new KeyMapping(
            "key.academy.activate_dir_blast", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_RBRACKET, "key.categories.academy");

    /** Le retour de sang : un contact de deux blocs, pour le plus gros coup du port. */
    public static final KeyMapping ACTIVATE_BLOOD_RETROGRADE = new KeyMapping(
            "key.academy.activate_blood_retro", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_BACKSLASH, "key.categories.academy");

    /** La deviation de vecteur : une veille qui tient tant que la reserve suit. */
    public static final KeyMapping ACTIVATE_VEC_DEVIATION = new KeyMapping(
            "key.academy.activate_vec_deviation", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_GRAVE, "key.categories.academy");

    /**
     * La reflexion de vecteur. Les lettres sont prises depuis longtemps, et la ponctuation
     * s'epuise a son tour : celle-ci se pose donc sur la touche d'insertion. Le systeme de
     * presets de l'original reste a porter, et c'est lui qui reglera la question.
     */
    public static final KeyMapping ACTIVATE_VEC_REFLECTION = new KeyMapping(
            "key.academy.activate_vec_reflection", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_INSERT, "key.categories.academy");

    /**
     * Les ailes de tempete. Il n'y a plus rien de libre pres des touches du jeu, et celle-ci
     * se tient d'une main pendant que l'autre vole avec W, A, S et D. Le systeme de presets
     * de l'original reste a porter, et c'est lui qui reglera la question.
     */
    public static final KeyMapping ACTIVATE_STORM_WING = new KeyMapping(
            "key.academy.activate_storm_wing", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_HOME, "key.categories.academy");

    /**
     * Le canon a plasma. Meme remarque que les ailes : il n'y a plus rien de libre, le systeme
     * de presets de l'original reste a porter.
     */
    public static final KeyMapping ACTIVATE_PLASMA_CANNON = new KeyMapping(
            "key.academy.activate_plasma_cannon", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_END, "key.categories.academy");

    /**
     * La detection de minerais. Meme remarque que les autres : il n'y a plus rien de libre,
     * le systeme de presets de l'original reste a porter.
     */
    public static final KeyMapping ACTIVATE_MINE_DETECT = new KeyMapping(
            "key.academy.activate_mine_detect", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_PAGEUP, "key.categories.academy");

    /**
     * La manipulation d'un bloc. Il ne reste que les touches de defilement, et celle-ci se
     * tient pendant qu'on vise. Le systeme de presets de l'original reste a porter.
     */
    public static final KeyMapping ACTIVATE_MAG_MANIP = new KeyMapping(
            "key.academy.activate_mag_manip", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_PAGEDOWN, "key.categories.academy");

    /**
     * La teleportation a la marque. Sa touche n'allume rien : elle ouvre la liste des
     * endroits marques, et c'est un clic qui fait partir.
     */
    public static final KeyMapping ACTIVATE_LOCATION_TELEPORT = new KeyMapping(
            "key.academy.activate_location_teleport", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_MINUS, "key.categories.academy");

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        // Les quatre touches d'aptitude, et rien d'autre : la competence de chacune vient du
        // prereglage en service, comme chez l'original.
        event.register(ABILITY_1);
        event.register(ABILITY_2);
        event.register(ABILITY_3);
        event.register(ABILITY_4);
        event.register(PRESET_NEXT);
        event.register(PRESET_EDIT);
        event.register(DEBUG_CONSOLE);
        event.register(TOGGLE_ABILITY);
    }
}
