package cn.academy.command;

import cn.academy.ability.AbilityCapability;
import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.network.SyncAbilityDataPacket;
import cn.academy.ability.preset.PresetTracker;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Les commandes de debogage {@code /aim} et {@code /aimp}, portage de {@code CommandAIMBase}.
 *
 * <p>L'original avait deux racines : {@code /aim} pour qui veut tester sur lui-meme, et
 * {@code /aimp <joueur>} pour l'operateur (et la console) qui vise quelqu'un d'autre. Les
 * sous-commandes sont les memes des deux cotes : {@code cat}, {@code catlist}, {@code learn},
 * {@code unlearn}, {@code learn_all}, {@code reset}, {@code learned}, {@code skills},
 * {@code fullcp}, {@code level}, {@code exp}, {@code cd_clear}, {@code maxout} — plus
 * {@code help} qui les liste et {@code cheats_on}/{@code cheats_off} pour armer la commande
 * du joueur.
 *
 * <h2>La regle du seul pouvoir</h2>
 *
 * Le mod d'origine ne laissait porter qu'<b>une</b> categorie, et {@code setCategory} la
 * remplacait : prendre le vecteur manipulation faisait oublier l'electromaster, avec son
 * niveau et ses competences. La commande du port applique la meme regle, mais le dit :
 * {@code /aim cat <nom>} oublie d'abord les autres pouvoirs, vide les prereglages qui les
 * allumaient, puis pose le nouveau au niveau 1 (sans niveau, l'aptitude reste eteinte et la
 * commande ne servirait a rien). {@code /aim reset} lache le pouvoir sans en prendre un autre.
 */
public final class AimCommands {

    /**
     * La marque du mode « triche » : l'original rangeait {@code aim_cheats} dans les donnees
     * du joueur, et le port fait pareil (les donnees persistantes de Forge, qui survivent a
     * la mort).
     */
    private static final String CHEATS_TAG = "academy.aim_cheats";

    private AimCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(tree("aim"));
        dispatcher.register(tree("aimp"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> tree(String root) {
        boolean other = root.equals("aimp");
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(root);
        if (other) {
            // Ecart assume : l'original laissait /aimp a tout le monde (il n'avait jamais pose
            // de permission), ce qui laissait n'importe qui changer le pouvoir d'un autre. Le
            // port la reserve aux operateurs.
            builder.requires(source -> source.hasPermission(2));
        }

        // Ces trois-la ne dependent d'aucun joueur et passent avant le controle d'armement :
        // c'est ainsi qu'on s'arme, et qu'on lit de quoi on dispose.
        builder.then(Commands.literal("help").executes(ctx -> help(ctx.getSource(), root)));
        builder.then(Commands.literal("?").executes(ctx -> help(ctx.getSource(), root)));
        builder.then(Commands.literal("catlist").executes(ctx -> catlist(ctx.getSource(), root)));

        Function<CommandContext<CommandSourceStack>, ServerPlayer> target = other
                ? ctx -> quiet(() -> EntityArgument.getPlayer(ctx, "player"))
                : ctx -> quiet(() -> ctx.getSource().getPlayerOrException());

        List<LiteralArgumentBuilder<CommandSourceStack>> subs = subcommands(target, !other);

        if (other) {
            // `/aimp <joueur> <sous-commande>` : le joueur vise vient avant.
            RequiredArgumentBuilder<CommandSourceStack, ?> player =
                    Commands.argument("player", EntityArgument.player());
            for (LiteralArgumentBuilder<CommandSourceStack> sub : subs) player.then(sub);
            builder.then(player);
        } else {
            builder.then(Commands.literal("cheats_on").executes(ctx -> cheats(ctx.getSource(), true)));
            builder.then(Commands.literal("cheats_off").executes(ctx -> cheats(ctx.getSource(), false)));
            for (LiteralArgumentBuilder<CommandSourceStack> sub : subs) builder.then(sub);
        }
        return builder;
    }

    /**
     * Les sous-commandes qui visent un joueur.
     *
     * @param gate vrai pour {@code /aim} : la commande du joueur doit etre armee (voir
     *             {@link #armed}). {@code /aimp} n'a pas ce controle, c'est deja l'operateur
     *             qui parle.
     */
    private static List<LiteralArgumentBuilder<CommandSourceStack>> subcommands(
            Function<CommandContext<CommandSourceStack>, ServerPlayer> target, boolean gate) {
        List<LiteralArgumentBuilder<CommandSourceStack>> subs = new ArrayList<>();

        subs.add(Commands.literal("cat")
                .executes(ctx -> exec(ctx, target, gate, "cat", null, null))
                .then(Commands.argument("category", StringArgumentType.word())
                        .suggests(CATEGORIES)
                        .executes(ctx -> exec(ctx, target, gate, "cat",
                                StringArgumentType.getString(ctx, "category"), null))));

        subs.add(Commands.literal("learn").then(skillArg(target, gate, "learn")));
        subs.add(Commands.literal("unlearn").then(skillArg(target, gate, "unlearn")));
        subs.add(Commands.literal("learn_all").executes(ctx -> exec(ctx, target, gate, "learn_all", null, null)));
        subs.add(Commands.literal("reset").executes(ctx -> exec(ctx, target, gate, "reset", null, null)));
        subs.add(Commands.literal("learned").executes(ctx -> exec(ctx, target, gate, "learned", null, null)));
        subs.add(Commands.literal("skills").executes(ctx -> exec(ctx, target, gate, "skills", null, null)));
        subs.add(Commands.literal("fullcp").executes(ctx -> exec(ctx, target, gate, "fullcp", null, null)));
        subs.add(Commands.literal("cd_clear").executes(ctx -> exec(ctx, target, gate, "cd_clear", null, null)));
        subs.add(Commands.literal("maxout").executes(ctx -> exec(ctx, target, gate, "maxout", null, null)));

        subs.add(Commands.literal("level")
                .executes(ctx -> exec(ctx, target, gate, "level", null, null))
                .then(Commands.argument("level", StringArgumentType.word())
                        .suggests(LEVELS)
                        .executes(ctx -> exec(ctx, target, gate, "level",
                                StringArgumentType.getString(ctx, "level"), null))));

        subs.add(Commands.literal("exp").then(Commands.argument("skill", StringArgumentType.word())
                .suggests(SKILLS)
                .executes(ctx -> exec(ctx, target, gate, "exp",
                        StringArgumentType.getString(ctx, "skill"), null))
                .then(Commands.argument("value", StringArgumentType.word())
                        .executes(ctx -> exec(ctx, target, gate, "exp",
                                StringArgumentType.getString(ctx, "skill"),
                                StringArgumentType.getString(ctx, "value"))))));

        return subs;
    }

    /** L'argument « competence », present sur {@code learn} et {@code unlearn}. */
    private static RequiredArgumentBuilder<CommandSourceStack, String> skillArg(
            Function<CommandContext<CommandSourceStack>, ServerPlayer> target, boolean gate, String sub) {
        return Commands.argument("skill", StringArgumentType.word())
                .suggests(SKILLS)
                .executes(ctx -> exec(ctx, target, gate, sub,
                        StringArgumentType.getString(ctx, "skill"), null));
    }

    // ------------------------------------------------------------------
    // Execution
    // ------------------------------------------------------------------

    private static int exec(CommandContext<CommandSourceStack> ctx,
                            Function<CommandContext<CommandSourceStack>, ServerPlayer> target,
                            boolean gate, String sub, String first, String second) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = target.apply(ctx);
        if (player == null) return 0;
        if (gate && !armed(source, player)) return 0;
        // `gate` ne vaut vrai que pour /aim : c'est donc lui qui dit qui parle, et les
        // rares messages qui nomment la commande s'en servent (voir `run`).
        return run(source, player, gate, sub, first, second);
    }

    /**
     * La commande du joueur est-elle armee ?
     *
     * <p>Portage du controle de {@code CommandAIM} : {@code /aim} arrivait desarmee et
     * s'armait par {@code /aim cheats_on}, qui previent que la balance du jeu n'y survivra
     * pas. L'original s'armait aussi <b>tout seul</b> des que le monde autorise les
     * commandes — un joueur qui peut taper la commande peut donc s'en servir sans rien faire
     * de plus, et c'est ce qui rend l'outil utilisable pour tester.
     */
    private static boolean armed(CommandSourceStack source, ServerPlayer player) {
        // « Le monde autorise les commandes » se lit ici par la permission : en solo avec les
        // triches le joueur est operateur, et c'est la seule forme qui vaille aussi sur un
        // serveur (`WorldData` ne porte pas ce renseignement dans cette version).
        if (!cheats(player) && source.hasPermission(2)) {
            setCheats(player, true);
        }
        if (cheats(player) || player.isCreative()) return true;
        send(source, "ac.command.aim.notactive");
        return false;
    }

    private static boolean cheats(ServerPlayer player) {
        return player.getPersistentData().getBoolean(CHEATS_TAG);
    }

    private static void setCheats(ServerPlayer player, boolean value) {
        player.getPersistentData().putBoolean(CHEATS_TAG, value);
    }

    private static int cheats(CommandSourceStack source, boolean value) {
        ServerPlayer player = quiet(() -> source.getPlayerOrException());
        if (player == null) return 0;
        setCheats(player, value);
        send(source, "ac.command.successful");
        if (value) {
            // L'original ne prevenait qu'a l'allumage : c'est la qu'on casse sa partie.
            send(source, "ac.command.aim.warning");
        }
        return 1;
    }

    /** La liste des commandes, dans l'ordre de l'original (voir {@link AimCommandSpec}). */
    private static int help(CommandSourceStack source, String root) {
        send(source, "ac.command." + root + ".usage");
        for (String name : AimCommandSpec.COMMANDS) {
            send(source, "ac.command." + root + "." + name);
        }
        if (root.equals("aim")) {
            // Deux commandes de plus, qui n'existent que du cote joueur : le message
            // `notactive` parle de `cheats_on`, et une liste qui l'omettrait enverrait
            // chercher une commande qu'elle ne montre pas.
            send(source, "ac.command.aim.cheats_on");
            send(source, "ac.command.aim.cheats_off");
        }
        return 1;
    }

    private static int catlist(CommandSourceStack source, String root) {
        send(source, "ac.command." + root + ".cats");
        List<Category> all = CategoryManager.INSTANCE.getCategories();
        for (int i = 0; i < all.size(); i++) {
            Category category = all.get(i);
            int index = i;
            source.sendSuccess(() -> Component.literal(AimCommandSpec.categoryPrefix(index, category))
                    .append(Component.translatable(category.getDisplayKey())), false);
        }
        return 1;
    }

    private static int run(CommandSourceStack source, ServerPlayer player, boolean personal,
                           String sub, String first, String second) {
        AbilityData data = player.getCapability(AbilityCapability.ABILITY_DATA).orElse(null);
        if (data == null) return 0;
        List<Category> all = CategoryManager.INSTANCE.getCategories();
        Category power = AimCommandSpec.activePower(data, all);
        String token = first == null ? null : first.toLowerCase(Locale.ROOT);
        // Les quelques messages qui nomment la commande suivent la racine en service.
        String msg = personal ? "ac.command.aim." : "ac.command.aimp.";

        switch (sub) {
            case "cat" -> {
                if (token == null) {
                    if (power == null) {
                        send(source, "ac.command.aim.nonecat");
                    } else {
                        Category shown = power;
                        source.sendSuccess(() -> Component.translatable("ac.command.aim.curcat",
                                Component.translatable(shown.getDisplayKey())), false);
                    }
                    return 1;
                }
                Category adopted = CategoryManager.INSTANCE.getCategory(token);
                if (adopted == null) {
                    send(source, msg + "nocat");
                    return 1;
                }
                List<Category> dropped = AimCommandSpec.powersToForget(data, all, adopted);
                for (Category category : dropped) {
                    abandon(data, category);
                }
                if (!dropped.isEmpty()) {
                    // Les prereglages ranges allumaient les competences du pouvoir qu'on
                    // lache : l'original les effacait sur le meme evenement.
                    PresetTracker.clearOnCategoryChange(player);
                }
                if (data.getCategoryLevel(adopted) == 0) {
                    // Niveau 1 : sans niveau, `isActivated` est faux, l'aptitude reste eteinte
                    // et la commande ne permettrait rien tester.
                    data.setCategoryLevel(adopted, 1);
                }
                sync(player, data);
                send(source, "ac.command.successful");
                if (!dropped.isEmpty()) {
                    send(source, "ac.command.aim.power_dropped", names(dropped));
                }
            }

            case "learn" -> {
                Skill skill = AimCommandSpec.findSkill(power, token);
                if (skill == null) {
                    send(source, power == null ? msg + "nonecathint" : msg + "noskill");
                    return 1;
                }
                data.learnSkill(skill);
                sync(player, data);
                send(source, "ac.command.successful");
            }

            case "unlearn" -> {
                Skill skill = AimCommandSpec.findSkill(power, token);
                if (skill == null) {
                    send(source, power == null ? msg + "nonecathint" : msg + "noskill");
                    return 1;
                }
                data.forgetSkill(skill);
                sync(player, data);
                send(source, "ac.command.successful");
            }

            case "learn_all" -> {
                if (power == null) {
                    send(source, "ac.command.aim.nonecathint");
                    return 1;
                }
                for (Skill skill : power.getSkills()) data.learnSkill(skill);
                sync(player, data);
                send(source, "ac.command.successful");
            }

            case "reset" -> {
                // Aucun pouvoir n'est adopte : `powersToForget` les rend donc tous.
                for (Category category : AimCommandSpec.powersToForget(data, all, null)) {
                    abandon(data, category);
                }
                PresetTracker.clearOnCategoryChange(player);
                sync(player, data);
                send(source, "ac.command.successful");
            }

            case "learned" -> {
                List<Skill> learned = power == null ? List.of() : data.getLearnedSkills(power);
                send(source, "ac.command.aim.learned.format", AimCommandSpec.learnedLine(learned));
            }

            case "skills" -> {
                if (power == null) {
                    send(source, "ac.command.aim.nonecathint");
                    return 1;
                }
                for (Skill skill : power.getSkills()) {
                    source.sendSuccess(() -> Component.literal(AimCommandSpec.skillPrefix(skill))
                            .append(skill.getDisplayName()), false);
                }
            }

            case "fullcp" -> {
                if (power == null) {
                    send(source, "ac.command.aim.nonecathint");
                    return 1;
                }
                data.setControlPoint(data.getMaxControlPoint());
                data.setOverload(0f);
                sync(player, data);
                send(source, "ac.command.successful");
            }

            case "level" -> {
                if (power == null) {
                    send(source, "ac.command.aim.nonecathint");
                    return 1;
                }
                if (token == null) {
                    int level = data.getCategoryLevel(power);
                    source.sendSuccess(() -> Component.literal(String.valueOf(level)), false);
                    return 1;
                }
                Integer level = parseInt(token);
                if (level == null) {
                    send(source, "ac.command.aim.invalidnum", token);
                    return 1;
                }
                if (!AimCommandSpec.levelInRange(level)) {
                    send(source, "ac.command.aim.outofrange",
                            AimCommandSpec.LEVEL_MIN, AimCommandSpec.LEVEL_MAX);
                    return 1;
                }
                data.setCategoryLevel(power, level);
                sync(player, data);
                send(source, "ac.command.successful");
            }

            case "exp" -> {
                Skill skill = AimCommandSpec.findSkill(power, token);
                if (skill == null) {
                    send(source, power == null ? "ac.command.aim.nonecathint" : "ac.command.aim.noskill");
                    return 1;
                }
                if (second == null) {
                    Skill shown = skill;
                    source.sendSuccess(() -> Component.translatable("ac.command.aim.curexp",
                            shown.getDisplayName(), percent(data.getSkillExp(shown))), false);
                    return 1;
                }
                Float value = parseFloat(second);
                if (value == null) {
                    send(source, "ac.command.aim.invalidnum", second);
                    return 1;
                }
                if (!AimCommandSpec.expInRange(value)) {
                    send(source, "ac.command.aim.outofrange",
                            String.valueOf(AimCommandSpec.EXP_MIN), String.valueOf(AimCommandSpec.EXP_MAX));
                    return 1;
                }
                data.setSkillExp(skill, value);
                sync(player, data);
                send(source, "ac.command.successful");
            }

            case "cd_clear" -> {
                if (power == null) {
                    send(source, "ac.command.aim.nonecathint");
                    return 1;
                }
                data.clearCooldowns();
                sync(player, data);
                send(source, "ac.command.successful");
            }

            case "maxout" -> {
                if (power == null) {
                    send(source, "ac.command.aim.nonecathint");
                    return 1;
                }
                data.maxOutLevelProgress(power);
                sync(player, data);
                send(source, "ac.command.successful");
            }

            default -> send(source, "ac.command.aim.nocomm");
        }
        return 1;
    }

    /**
     * Lache un pouvoir : ses competences sont oubliees, leur experience avec, et son niveau
     * retombe a zero.
     *
     * <p>C'est ce que faisait {@code setCategory} de l'original, qui remplacait la categorie du
     * joueur — et donc tout ce qu'il savait d'elle. Les prereglages sont vides a part (voir
     * {@link PresetTracker#clearOnCategoryChange}), sur le meme evenement que le changement de
     * categorie du developpeur.
     */
    private static void abandon(AbilityData data, Category category) {
        for (Skill skill : category.getSkills()) {
            data.forgetSkill(skill);
            data.setSkillExp(skill, 0f);
        }
        data.setCategoryLevel(category, 0);
    }

    private static void sync(ServerPlayer player, AbilityData data) {
        AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncAbilityDataPacket(data));
    }

    private static void send(CommandSourceStack source, String key, Object... args) {
        source.sendSuccess(() -> Component.translatable(key, args), false);
    }

    private static String names(List<Category> categories) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Category category : categories) {
            if (!first) sb.append(", ");
            sb.append(category.getName());
            first = false;
        }
        return sb.toString();
    }

    /** Le pourcentage de `/aim exp <competence>`, arrondi comme l'original (0 a 100). */
    private static String percent(float exp) {
        return String.format(Locale.ROOT, "%.0f%%", exp * 100f);
    }

    private static Integer parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Float parseFloat(String text) {
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private interface PlayerLookup {
        ServerPlayer get() throws CommandSyntaxException;
    }

    private static ServerPlayer quiet(PlayerLookup lookup) {
        try {
            return lookup.get();
        } catch (CommandSyntaxException e) {
            return null;
        }
    }

    private static ServerPlayer suggestionTarget(CommandContext<CommandSourceStack> ctx) {
        try {
            return EntityArgument.getPlayer(ctx, "player");
        } catch (IllegalArgumentException | CommandSyntaxException ignored) {
            // Pas d'argument joueur : c'est celui qui tape la commande.
        }
        return quiet(() -> ctx.getSource().getPlayerOrException());
    }

    private static final SuggestionProvider<CommandSourceStack> CATEGORIES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    CategoryManager.INSTANCE.getCategories().stream().map(Category::getName).toList(),
                    builder);

    private static final SuggestionProvider<CommandSourceStack> LEVELS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(List.of("1", "2", "3", "4", "5"), builder);

    /**
     * Les competences a proposer : celles du pouvoir vise.
     *
     * <p>Rien a proposer s'il n'y a pas de pouvoir : la commande repondrait
     * {@code nonecathint} de toute facon.
     */
    private static final SuggestionProvider<CommandSourceStack> SKILLS = (ctx, builder) -> {
        ServerPlayer player = suggestionTarget(ctx);
        if (player == null) return builder.buildFuture();
        AbilityData data = player.getCapability(AbilityCapability.ABILITY_DATA).orElse(null);
        Category power = data == null
                ? null
                : AimCommandSpec.activePower(data, CategoryManager.INSTANCE.getCategories());
        if (power == null) return builder.buildFuture();
        return SharedSuggestionProvider.suggest(
                power.getSkills().stream().map(Skill::getName).toList(), builder);
    };
}
