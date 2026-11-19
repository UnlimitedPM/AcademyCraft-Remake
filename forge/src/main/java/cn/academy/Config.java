package cn.academy;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

/**
 * Configuration du mod (fichier {@code config/academy-common.toml}).
 *
 * <p>Portage de l'ancien {@code config/academy-craft-data.conf} (HOCON) de la 1.12.2. L'original
 * exposait ses reglages par un systeme de donnees complexe ; le port garde ce qu'il honore
 * reellement, plus les <b>tables de points de controle</b> et l'<b>interrupteur par competence</b>
 * que l'original avait et qui manquaient ici.
 *
 * <h2>Deux pieges, tous les deux payes</h2>
 *
 * <ul>
 *   <li><b>Une liste se declare avec {@code defineList}</b> et un predicat. Un {@code define} sur
 *       une liste ecrit le TYPE exact de la liste Java ({@code ImmutableCollections$ListN}) : le
 *       jeu la juge alors toujours fausse et la remplace par le defaut a chaque lecture — le
 *       reglage « ne se retient pas », sans une erreur ;</li>
 *   <li><b>toute option se declare AVANT {@code SPEC = BUILDER.build()}</b> : apres, elle compile
 *       et n'est simplement jamais lue.</li>
 * </ul>
 *
 * <p>Les champs statiques sont initialises a leur valeur par defaut : un appel avant le chargement
 * de la config (datapack, biome modifier, test unitaire) reste donc coherent.
 *
 * <p>Chaque option porte son commentaire en francais PUIS en anglais, parce que le fichier est lu
 * par les joueurs des deux langues et que le reste du projet fait deja ce choix.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Config {

    private Config() {}

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ------------------------------------------------------------------
    // General
    // ------------------------------------------------------------------
    private static final ForgeConfigSpec.BooleanValue GEN_ORES = BUILDER
            .comment("Genere les minerais d'AcademyCraft dans l'Overworld.",
                     "Equivalent de genOres dans la 1.12.2.",
                     "Whether AcademyCraft's ores generate in the overworld (the 1.12.2 genOres).")
            .define("general.generateOres", true);

    private static final ForgeConfigSpec.BooleanValue GEN_PHASE_LIQUID = BUILDER
            .comment("Genere des poches de liquide Imag Phase sous terre.",
                     "Equivalent de genPhaseLiquid dans la 1.12.2.",
                     "Whether pockets of Imag Phase liquid generate underground (the 1.12.2 genPhaseLiquid).")
            .define("general.generatePhaseLiquid", true);

    private static final ForgeConfigSpec.DoubleValue DAMAGE_SCALE = BUILDER
            .comment("Multiplicateur global des degats infliges par les competences.",
                     "Equivalent de ac.ability.calc_global.damage_scale dans la 1.12.2.",
                     "Global multiplier on the damage every skill deals (the 1.12.2 damage_scale).")
            .defineInRange("general.damageScale", 1.0d, 0.0d, 1000.0d);

    private static final ForgeConfigSpec.BooleanValue DESTROY_BLOCKS = BUILDER
            .comment("Autorise les competences a casser des blocs.",
                     "Portage de generic.destroyBlocks de la 1.12.2 : les competences qui",
                     "effacent des blocs le consultent avant de le faire.",
                     "Whether skills may break blocks (the 1.12.2 generic.destroyBlocks).")
            .define("general.destroyBlocks", true);

    private static final ForgeConfigSpec.BooleanValue ATTACK_PLAYER = BUILDER
            .comment("Autorise les competences a toucher d'autres joueurs (PvP).",
                     "Portage de generic.attackPlayer de la 1.12.2, ou il valait vrai. Sans elle,",
                     "une competence ne fait rien a un joueur : ni degats, ni recul.",
                     "Whether skills may damage other players, i.e. PvP (the 1.12.2 attackPlayer).")
            .define("general.attackPlayer", true);

    private static final ForgeConfigSpec.BooleanValue HEADS_OR_TAILS = BUILDER
            .comment("Annonce le resultat du lancer de piece (pile ou face).",
                     "Portage de generic.headsOrTails de la 1.12.2, ou il valait faux.",
                     "Whether a coin toss announces heads or tails (the 1.12.2 headsOrTails).")
            .define("general.headsOrTails", false);

    private static final ForgeConfigSpec.BooleanValue USE_MOUSE_WHEEL = BUILDER
            .comment("La molette regle la distance du saut traversant ; sinon elle defile la barre.",
                     "Portage de generic.useMouseWheel de la 1.12.2 — avec un ecart assume : il y",
                     "valait faux, alors que le port a toujours regle la distance a la molette.",
                     "Whether the mouse wheel sets the phase jump distance (the 1.12.2 useMouseWheel).")
            .define("general.useMouseWheel", true);

    /** Le cadeau de l'original : l'objet MisakaCloud, une fois, au premier joueur. */
    private static final ForgeConfigSpec.BooleanValue GIVE_CLOUD_TERMINAL = BUILDER
            .comment("Donne l'objet MisakaCloud au joueur, une seule fois, a sa premiere venue.",
                     "Portage de generic.giveCloudTerminal de la 1.12.2.",
                     "Whether the player receives the MisakaCloud item once (the 1.12.2 giveCloudTerminal).")
            .define("general.giveCloudTerminal", true);

    // ------------------------------------------------------------------
    // Ability (Control Points)
    // ------------------------------------------------------------------
    private static final ForgeConfigSpec.DoubleValue CP_START = BUILDER
            .comment("Control Points d'un joueur qui n'a pas encore de categorie.",
                     "Le plafond, lui, appartient au niveau : voir ability.initCp.",
                     "Control points of a player who has no ability category yet.")
            .defineInRange("ability.controlPointStart", 100.0d, 0.0d, 1_000_000.0d);

    private static final ForgeConfigSpec.DoubleValue CP_REGEN_SPEED = BUILDER
            .comment("Vitesse de recuperation des Control Points.",
                     "La reserve regagne une part de son PLAFOND par tick (0,0003 chez l'original,",
                     "doublee quand elle est pleine), donc ce reglage est un MULTIPLICATEUR :",
                     "1.0 = la vitesse de l'original ; 0 = plus rien ne remonte tout seul.",
                     "La valeur s'ecrit sur la ligne du reglage, PAS dans ce commentaire : le jeu",
                     "reecrit les commentaires a chaque demarrage.",
                     "Control point regeneration, as a multiplier: 1.0 is the 1.12.2 speed, 0 freezes it.",
                     "Edit the setting's own line, not this comment: the game rewrites comments on load.")
            .defineInRange("ability.controlPointRegenSpeed", 1.0d, 0.0d, 100.0d);

    private static final ForgeConfigSpec.IntValue CP_RECOVER_COOLDOWN = BUILDER
            .comment("Ticks d'attente avant que la reserve ne se remette a remonter,",
                     "apres chaque paiement. Repris de ac.ability.data.cp_recover_cooldown.",
                     "Ticks of silence after a payment before the reserve regenerates again.")
            .defineInRange("ability.controlPointRecoverCooldown", 15, 0, 20_000);

    private static final ForgeConfigSpec.IntValue OVERLOAD_RECOVER_COOLDOWN = BUILDER
            .comment("Ticks d'attente avant que le surcout ne redescende.",
                     "Repris de ac.ability.data.overload_recover_cooldown de la 1.12.2.",
                     "Ticks before the overload bar starts going down again.")
            .defineInRange("ability.overloadRecoverCooldown", 32, 0, 20_000);

    private static final ForgeConfigSpec.DoubleValue OVERLOAD_RECOVER_SPEED = BUILDER
            .comment("Vitesse de recuperation du surcout. 1.0 = celle de la 1.12.2,",
                     "soit environ un dixieme de seconde par point sur une reserve pleine.",
                     "Overload recovery, as a multiplier: 1.0 is the 1.12.2 speed.")
            .defineInRange("ability.overloadRecoverSpeed", 1.0d, 0.0d, 1000.0d);

    private static final ForgeConfigSpec.DoubleValue PROGRESS_INCR_RATE = BUILDER
            .comment("Vitesse de progression des niveaux d'aptitude.",
                     "1.0 = la valeur de la 1.12.2 : il faut remplir un palier en utilisant",
                     "ses competences avant de pouvoir monter d'un niveau.",
                     "Ability level progression speed: 1.0 is the 1.12.2 value.")
            .defineInRange("ability.progressIncrRate", 1.0d, 0.01d, 1000.0d);

    // ------------------------------------------------------------------
    // Les tables de points de controle (init_cp, add_cp, init_overload, add_overload)
    // ------------------------------------------------------------------

    /** Les plafonds de l'original, du niveau 0 au niveau 5 : {@code init_cp}. */
    private static final List<Double> DEFAULT_INIT_CP =
            List.of(1800.0, 1800.0, 2800.0, 4000.0, 5800.0, 8000.0);

    /** Ce que l'usage ajoute au plafond, de l'original : {@code add_cp}. */
    private static final List<Double> DEFAULT_ADD_CP =
            List.of(0.0, 900.0, 1000.0, 1500.0, 1700.0, 12000.0);

    /** Les plafonds de surcout, de l'original : {@code init_overload}. */
    private static final List<Double> DEFAULT_INIT_OVERLOAD =
            List.of(100.0, 100.0, 150.0, 240.0, 350.0, 500.0);

    /** Ce que l'usage ajoute au surcout, de l'original : {@code add_overload}. */
    private static final List<Double> DEFAULT_ADD_OVERLOAD =
            List.of(0.0, 40.0, 70.0, 80.0, 100.0, 500.0);

    private static final ForgeConfigSpec.ConfigValue<List<? extends Double>> INIT_CP = BUILDER
            .comment("Plafond de la reserve par niveau, du niveau 0 au niveau 5 (six valeurs).",
                     "Repris de init_cp de la 1.12.2 : 1800 des le niveau 1, 8000 au niveau 5.",
                     "Une liste qui n'a pas six valeurs est ignoree, et le defaut revient.",
                     "CP ceiling per level, level 0 to level 5 (six values). From the 1.12.2 init_cp.",
                     "A list without exactly six values is ignored and the default comes back.")
            .defineList("ability.initCp", DEFAULT_INIT_CP, o -> o instanceof Double);

    private static final ForgeConfigSpec.ConfigValue<List<? extends Double>> ADD_CP = BUILDER
            .comment("Ce que l'usage des competences peut ajouter au plafond, par niveau (six valeurs).",
                     "Repris de add_cp de la 1.12.2 : payer une competence agrandit la reserve.",
                     "How much use may add to the CP ceiling, per level (six values). From add_cp.")
            .defineList("ability.addCp", DEFAULT_ADD_CP, o -> o instanceof Double);

    private static final ForgeConfigSpec.ConfigValue<List<? extends Double>> INIT_OVERLOAD = BUILDER
            .comment("Plafond du surcout par niveau (six valeurs). Repris de init_overload.",
                     "Overload ceiling per level (six values). From the 1.12.2 init_overload.")
            .defineList("ability.initOverload", DEFAULT_INIT_OVERLOAD, o -> o instanceof Double);

    private static final ForgeConfigSpec.ConfigValue<List<? extends Double>> ADD_OVERLOAD = BUILDER
            .comment("Ce que l'usage peut ajouter au surcout, par niveau (six valeurs). Repris de add_overload.",
                     "How much use may add to the overload ceiling, per level (six values). From add_overload.")
            .defineList("ability.addOverload", DEFAULT_ADD_OVERLOAD, o -> o instanceof Double);

    // ------------------------------------------------------------------
    // Les multiplicateurs de cout
    // ------------------------------------------------------------------
    private static final ForgeConfigSpec.DoubleValue CP_COST_SCALE = BUILDER
            .comment("Multiplicateur de TOUT ce qui est paye en Control Points.",
                     "0.5 divise les couts par deux, 2.0 les double. Les ecrans qui affichent",
                     "un prix montrent la valeur de base : c'est le paiement qui suit ce reglage.",
                     "Multiplier on every control point payment. 0.5 halves costs, 2.0 doubles them.",
                     "Screens that show a price show the base value; the payment follows this setting.")
            .defineInRange("ability.cpCostScale", 1.0d, 0.0d, 100.0d);

    private static final ForgeConfigSpec.DoubleValue OVERLOAD_COST_SCALE = BUILDER
            .comment("Multiplicateur de tout ce qui est paye en surcout.",
                     "Multiplier on every overload payment.")
            .defineInRange("ability.overloadCostScale", 1.0d, 0.0d, 100.0d);

    private static final ForgeConfigSpec.DoubleValue COOLDOWN_SCALE = BUILDER
            .comment("Multiplicateur des recharges des competences.",
                     "0.5 = deux fois plus court, 2.0 = deux fois plus long.",
                     "Multiplier on every skill cooldown: 0.5 is half as long, 2.0 twice as long.")
            .defineInRange("ability.cooldownScale", 1.0d, 0.0d, 100.0d);

    private static final ForgeConfigSpec.DoubleValue EXP_SCALE = BUILDER
            .comment("Multiplicateur de l'experience gagnee en utilisant ses competences.",
                     "Portage de exp_incr_speed de la 1.12.2, en global plutot que par competence.",
                     "Multiplier on the experience skills earn when used. From the 1.12.2 exp_incr_speed.")
            .defineInRange("ability.expScale", 1.0d, 0.0d, 1000.0d);

    /**
     * Les competences mises hors service.
     *
     * <p>C'est le {@code enabled = false} de l'original, ecrit en une seule liste : une competence
     * citee ici ne se lance plus et ne se range plus sur une touche, comme si elle n'etait pas
     * apprise (elle le reste, et son experience est gardee). Ecrire le nom court
     * ({@code arc_gen}) ou le nom complet ({@code electromaster.arc_gen}) revient au meme.
     */
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_SKILLS = BUILDER
            .comment("Competences desactivees : nom court (arc_gen) ou complet (electromaster.arc_gen).",
                     "Une competence citee ici ne se lance plus et ne se range plus sur une touche,",
                     "comme le enabled = false de la 1.12.2.",
                     "Disabled skills, by short name (arc_gen) or full name (electromaster.arc_gen).",
                     "A listed skill cannot be cast nor put on a key, like the 1.12.2 'enabled = false'.")
            .defineList("ability.disabledSkills", List.of(), o -> o instanceof String);

    // ------------------------------------------------------------------
    // Ce que l'electromaster attire
    // ------------------------------------------------------------------

    /**
     * Ce que l'electromaster attire, repris de {@code normalMetalBlocks} de l'original.
     *
     * <p>Ces blocs s'attrapent a tout niveau d'experience, avec les blocs ajoutes par le port (porte
     * et trappe en fer, cuivre travaille). Voir {@code MetalTargets.NEW_STRONG_METALS} : les memes
     * blocs sont aussi en code, parce qu'une config deja ecrite ne recoit jamais une entree ajoutee
     * a une liste existante.
     */
    private static final List<String> DEFAULT_METAL_BLOCKS = List.of(
            "minecraft:rail",
            "minecraft:iron_bars",
            "minecraft:iron_block",
            "minecraft:iron_door",
            "minecraft:iron_trapdoor",
            "minecraft:copper_block",
            "minecraft:cut_copper",
            "minecraft:cut_copper_stairs",
            "minecraft:cut_copper_slab",
            "minecraft:activator_rail",
            "minecraft:detector_rail",
            "minecraft:golden_rail",
            "minecraft:sticky_piston",
            "minecraft:piston");

    /** Les blocs faiblement metalliques, repris de {@code weakMetalBlocks} (60 % d'experience). */
    private static final List<String> DEFAULT_WEAK_METAL_BLOCKS = List.of(
            "minecraft:dispenser",
            "minecraft:hopper",
            "minecraft:iron_ore",
            "minecraft:deepslate_iron_ore",
            "minecraft:copper_ore",
            "minecraft:deepslate_copper_ore",
            "minecraft:raw_copper_block",
            "minecraft:raw_iron_block");

    /** Les entites metalliques, reprises de {@code metalEntities} (crochet magnetique compris). */
    private static final List<String> DEFAULT_METAL_ENTITIES = List.of(
            "minecraft:minecart",
            "minecraft:chest_minecart",
            "minecraft:furnace_minecart",
            "minecraft:tnt_minecart",
            "minecraft:hopper_minecart",
            "minecraft:spawner_minecart",
            "minecraft:commandblock_minecart",
            "minecraft:villager_golem",
            "academy:mag_hook");

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> METAL_BLOCKS = BUILDER
            .comment("Blocs franchement metalliques, que l'electromaster attire a tout niveau.",
                     "Noms de blocs, comme minecraft:iron_block. Repris de normalMetalBlocks.",
                     "Strongly metallic blocks the electromaster pulls at any level (normalMetalBlocks).")
            .defineList("ability.metalBlocks", DEFAULT_METAL_BLOCKS, o -> o instanceof String);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> WEAK_METAL_BLOCKS = BUILDER
            .comment("Blocs faiblement metalliques, attires a partir de 60 % d'experience.",
                     "Repris de weakMetalBlocks de la 1.12.2.",
                     "Weakly metallic blocks, pulled from 60 % experience on (weakMetalBlocks).")
            .defineList("ability.weakMetalBlocks", DEFAULT_WEAK_METAL_BLOCKS, o -> o instanceof String);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> METAL_ENTITIES = BUILDER
            .comment("Entites metalliques, que l'electromaster peut attirer.",
                     "Repris de metalEntities de la 1.12.2.",
                     "Metallic entities the electromaster can pull (metalEntities).")
            .defineList("ability.metalEntities", DEFAULT_METAL_ENTITIES, o -> o instanceof String);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> AFFECTED_ENTITIES = BUILDER
            .comment("Entites que vecmanip peut devier ou reflechir, avec leur difficulte.",
                     "Ecriture : \"<id>=<difficulte>\", par exemple minecraft:arrow=1.0 ;",
                     "un id seul vaut la difficulte par defaut, 1.0.",
                     "Entities vecmanip may deflect or reflect, with their difficulty.",
                     "Syntax: \"<id>=<difficulty>\", e.g. minecraft:arrow=1.0.")
            .defineList("ability.affectedEntities",
                    List.of("minecraft:arrow=1.0", "minecraft:potion=1.4", "minecraft:snowball=0.1"),
                    o -> o instanceof String);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_ENTITIES = BUILDER
            .comment("Entites que vecmanip ne touche jamais.",
                     "Des ids d'entites, plus deux mots-cles : \"living\" (tout ce qui vit)",
                     "et \"mob\" (les creatures).",
                     "Entities vecmanip never touches. Two keywords exist: \"living\" and \"mob\".")
            .defineList("ability.excludedEntities",
                    List.of("minecraft:item", "minecraft:xp_bottle", "living", "mob"),
                    o -> o instanceof String);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    // ------------------------------------------------------------------
    // Valeurs en cache, relues a chaque chargement/rechargement de la config.
    // ------------------------------------------------------------------
    public static boolean generateOres = true;
    public static boolean generatePhaseLiquid = true;
    public static double damageScale = 1.0d;

    public static double controlPointStart = 100.0d;
    public static double controlPointRegenSpeed = 1.0d;
    public static int controlPointRecoverCooldown = 15;
    public static int overloadRecoverCooldown = 32;
    public static double overloadRecoverSpeed = 1.0d;
    public static double progressIncrRate = 1.0d;
    public static boolean destroyBlocks = true;
    public static boolean attackPlayer = true;
    public static boolean headsOrTails = false;
    public static boolean useMouseWheel = true;
    public static boolean giveCloudTerminal = true;

    public static double cpCostScale = 1.0d;
    public static double overloadCostScale = 1.0d;
    public static double cooldownScale = 1.0d;
    public static double expScale = 1.0d;

    /** Les plafonds par niveau, lus par {@code AbilityData}. Copie privee : jamais partagee. */
    public static float[] initCp = toFloats(DEFAULT_INIT_CP);
    public static float[] addCp = toFloats(DEFAULT_ADD_CP);
    public static float[] initOverload = toFloats(DEFAULT_INIT_OVERLOAD);
    public static float[] addOverload = toFloats(DEFAULT_ADD_OVERLOAD);

    /** Les competences mises hors service. */
    public static List<String> disabledSkills = List.of();

    /** Les listes de metaux, relues a chaque chargement de config. */
    public static List<String> metalBlocks = List.of();
    public static List<String> weakMetalBlocks = List.of();
    public static List<String> metalEntities = List.of();

    /** Ce que vecmanip peut devier, et ce qu'il ne touche jamais. */
    public static List<String> affectedEntities = List.of();
    public static List<String> excludedEntities = List.of();

    /** La config chargee, pour pouvoir la reecrire depuis l'ecran de reglage du mod. */
    private static net.minecraftforge.fml.config.ModConfig loadedConfig;

    /** Retient la config une fois chargee : sans elle, l'ecran ne saurait rien sauvegarder. */
    public static void capture(net.minecraftforge.fml.config.ModConfig config) {
        loadedConfig = config;
    }

    /** Valeur de {@link #controlPointStart}. Le plafond, lui, appartient au niveau. */
    public static float startingControlPoint() {
        return (float) controlPointStart;
    }

    // ------------------------------------------------------------------
    // L'ecran de reglage du mod
    // ------------------------------------------------------------------

    /**
     * Bascule « Destroy blocks » et l'ecrit dans le fichier.
     *
     * <p>La valeur est ecrite des deux cotes : dans la config, et dans le champ que le jeu
     * lit vraiment. Oublier le second ferait une case a cocher qui ne change rien.
     */
    public static boolean toggleDestroyBlocks() {
        destroyBlocks = !destroyBlocks;
        DESTROY_BLOCKS.set(destroyBlocks);
        save();
        return destroyBlocks;
    }

    /**
     * Ecrit le verrou du serveur, et le sauve.
     *
     * <p>Sert a l'ecran de reglage en solo : le joueur y est le serveur, et une case cochee qui
     * ne casse rien parce qu'un verrou est reste ferme ailleurs serait un mensonge.
     */
    public static boolean setDestroyBlocks(boolean value) {
        destroyBlocks = value;
        DESTROY_BLOCKS.set(value);
        save();
        return destroyBlocks;
    }

    /** Bascule « Enable PvP » et l'ecrit dans le fichier. */
    public static boolean toggleAttackPlayer() {
        attackPlayer = !attackPlayer;
        ATTACK_PLAYER.set(attackPlayer);
        save();
        return attackPlayer;
    }

    /** Bascule « Play Heads or Tails » et l'ecrit dans le fichier. */
    public static boolean toggleHeadsOrTails() {
        headsOrTails = !headsOrTails;
        HEADS_OR_TAILS.set(headsOrTails);
        save();
        return headsOrTails;
    }

    /** Bascule « Mouse wheel adjust tp distance » et l'ecrit dans le fichier. */
    public static boolean toggleUseMouseWheel() {
        useMouseWheel = !useMouseWheel;
        USE_MOUSE_WHEEL.set(useMouseWheel);
        save();
        return useMouseWheel;
    }

    /** Reecrit le fichier de config. Un echec ne fait pas tomber le jeu. */
    public static void save() {
        if (loadedConfig == null) return;
        try {
            loadedConfig.save();
        } catch (Exception e) {
            // Disque plein, fichier verrouille : la valeur reste en memoire.
        }
    }

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        generateOres = GEN_ORES.get();
        generatePhaseLiquid = GEN_PHASE_LIQUID.get();
        damageScale = DAMAGE_SCALE.get();
        controlPointStart = CP_START.get();
        controlPointRegenSpeed = CP_REGEN_SPEED.get();
        controlPointRecoverCooldown = CP_RECOVER_COOLDOWN.get();
        overloadRecoverCooldown = OVERLOAD_RECOVER_COOLDOWN.get();
        overloadRecoverSpeed = OVERLOAD_RECOVER_SPEED.get();
        progressIncrRate = PROGRESS_INCR_RATE.get();
        destroyBlocks = DESTROY_BLOCKS.get();
        attackPlayer = ATTACK_PLAYER.get();
        headsOrTails = HEADS_OR_TAILS.get();
        useMouseWheel = USE_MOUSE_WHEEL.get();
        giveCloudTerminal = GIVE_CLOUD_TERMINAL.get();

        cpCostScale = CP_COST_SCALE.get();
        overloadCostScale = OVERLOAD_COST_SCALE.get();
        cooldownScale = COOLDOWN_SCALE.get();
        expScale = EXP_SCALE.get();

        // Les tables : six valeurs, sinon on garde le defaut. Une liste trop courte ferait
        // planter le premier acces a un niveau eleve, et c'est un reglage, pas un bug.
        initCp = table(INIT_CP.get(), DEFAULT_INIT_CP);
        addCp = table(ADD_CP.get(), DEFAULT_ADD_CP);
        initOverload = table(INIT_OVERLOAD.get(), DEFAULT_INIT_OVERLOAD);
        addOverload = table(ADD_OVERLOAD.get(), DEFAULT_ADD_OVERLOAD);

        // Une nouvelle instance de liste a chaque chargement : c'est ce que MetalTargets
        // surveille pour reconstruire ses ensembles.
        metalBlocks = List.copyOf(METAL_BLOCKS.get());
        weakMetalBlocks = List.copyOf(WEAK_METAL_BLOCKS.get());
        metalEntities = List.copyOf(METAL_ENTITIES.get());
        affectedEntities = List.copyOf(AFFECTED_ENTITIES.get());
        excludedEntities = List.copyOf(EXCLUDED_ENTITIES.get());
        disabledSkills = List.copyOf(DISABLED_SKILLS.get());
    }

    /** Une table de config, ou son defaut si elle n'a pas la bonne longueur. */
    private static float[] table(List<? extends Double> read, List<Double> fallback) {
        List<? extends Double> source = read.size() == fallback.size() ? read : fallback;
        return toFloats(source);
    }

    /** Une liste de nombres, en tableau de flottants — ce que lit la logique du jeu. */
    private static float[] toFloats(List<? extends Double> values) {
        float[] out = new float[values.size()];
        for (int i = 0; i < out.length; i++) out[i] = values.get(i).floatValue();
        return out;
    }

    /**
     * Validation d'un identifiant d'item, conservee du template MDK : utile des
     * qu'on exposera des listes d'items en config.
     */
    @SuppressWarnings("unused")
    private static boolean validateItemName(final Object obj) {
        return obj instanceof final String itemName
                && net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(ResourceLocation.tryParse(itemName));
    }
}
