package cn.academy.advancements;

import cn.academy.AcademyCraft;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Les quinze declencheurs de succes du mod, et la facon de les declencher.
 *
 * <p>Les noms sont ceux de l'original, un par succes : {@code ac_level_3},
 * {@code getting_factor}, {@code convert_category}... Les fichiers de succes les citent par
 * leur nom, donc une faute ici ne ferait rien tomber en jeu — le succes resterait
 * simplement impossible a obtenir, sans un mot.
 *
 * <p>C'est ce que le GameTest verifie : que chaque nom cite par un fichier repond bien
 * quelque chose dans la table des declencheurs.
 *
 * <p>En 1.20.1 cette table est celle de {@code CriteriaTriggers} — il n'y a pas de registre
 * de declencheurs a remplir par {@code DeferredRegister}. L'enregistrement se fait donc au
 * demarrage, une fois les classes chargees.
 */
public final class AcademyAdvancements {

    /** Les noms des succes du mod, dans l'ordre de l'original. */
    public static final String[] NAMES = {
            "ac_developer",
            "ac_exp_full",
            "ac_learning_skill",
            "ac_level_3",
            "ac_level_5",
            "ac_matrix",
            "ac_node",
            "ac_overload",
            "convert_category",
            "dev_category",
            "getting_factor",
            "getting_phase",
            "open_misaka_cloud",
            "phase_generator",
            "terminal_installed",
    };

    public static final String AC_DEVELOPER = "ac_developer";
    public static final String AC_EXP_FULL = "ac_exp_full";
    public static final String AC_LEARNING_SKILL = "ac_learning_skill";
    public static final String AC_LEVEL_3 = "ac_level_3";
    public static final String AC_LEVEL_5 = "ac_level_5";
    public static final String AC_MATRIX = "ac_matrix";
    public static final String AC_NODE = "ac_node";
    public static final String AC_OVERLOAD = "ac_overload";
    public static final String CONVERT_CATEGORY = "convert_category";
    public static final String DEV_CATEGORY = "dev_category";
    public static final String GETTING_FACTOR = "getting_factor";
    public static final String GETTING_PHASE = "getting_phase";
    public static final String OPEN_MISAKA_CLOUD = "open_misaka_cloud";
    public static final String PHASE_GENERATOR = "phase_generator";
    public static final String TERMINAL_INSTALLED = "terminal_installed";

    private static final Map<String, AcademyTrigger> TRIGGERS = new LinkedHashMap<>();

    private AcademyAdvancements() {}

    /** Enregistre les quinze declencheurs. Appele une fois, au demarrage du mod. */
    public static void register() {
        for (String name : NAMES) {
            AcademyTrigger trigger = new AcademyTrigger(id(name));
            CriteriaTriggers.register(trigger);
            TRIGGERS.put(name, trigger);
        }
    }

    /** Enregistre les declencheurs quand Forge en donne le signal. */
    public static void setup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        register();
    }

    /** Le declencheur d'un nom, ou {@code null} s'il n'a pas ete enregistre. */
    @Nullable
    public static AcademyTrigger get(String name) {
        return TRIGGERS.get(name);
    }

    /**
     * Declenche un succes pour un joueur, sans condition.
     *
     * <p>Un declencheur absent ne fait rien : c'est voulu. Le port ne doit pas tomber
     * parce qu'un succes est mal orthographie dans un fichier — c'est un contenu, pas du
     * code.
     */
    public static void award(@Nullable ServerPlayer player, @Nullable String name) {
        if (player == null || name == null) return;
        AcademyTrigger trigger = TRIGGERS.get(name);
        if (trigger != null) trigger.trigger(player);
    }

    /** Le niveau d'une categorie vient de changer : les succes de palier s'en occupent. */
    public static void onCategoryLevel(@Nullable ServerPlayer player, int level) {
        for (String name : levelNames(level)) {
            award(player, name);
        }
    }

    /**
     * Les succes qu'un palier de categorie donne, dans l'ordre de l'original.
     *
     * <p>Seuls trois niveaux donnent quelque chose : le premier (la categorie est apprise),
     * puis les troisieme et cinquieme. La liste est pure, donc verifiable sans joueur.
     */
    public static java.util.List<String> levelNames(int level) {
        return switch (level) {
            case 1 -> java.util.List.of(DEV_CATEGORY);
            case 3 -> java.util.List.of(AC_LEVEL_3);
            case 5 -> java.util.List.of(AC_LEVEL_5);
            default -> java.util.List.of();
        };
    }

    /** L'identifiant complet d'un succes du mod, pour les tests. */
    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, name);
    }
}
