package cn.academy.ability;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import cn.academy.AcademyCraft;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.network.SyncAbilityDataPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Les commandes d'aptitude : celle du joueur, et celles du debogage.
 *
 * <p>{@code /learn} est la porte d'entree : elle donne sa premiere aptitude a un joueur qui n'en a
 * aucune, en lisant son inventaire comme le fait le developpeur — un facteur d'induction designe sa
 * categorie, sinon le sort en donne une. {@code /academy learn <categorie>} reste pour les essais :
 * elle force une categorie, sans rien demander au joueur.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID)
public class AbilityCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        // Les commandes de debogage du mod d'origine : /aim (sur soi) et /aimp (sur un autre).
        cn.academy.command.AimCommands.register(dispatcher);
        dispatcher.register(Commands.literal("academy")
                .then(Commands.literal("learn")
                        .then(Commands.argument("category", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String catName = StringArgumentType.getString(ctx, "category");
                                    Category category = CategoryManager.INSTANCE.getCategory(catName);
                                    if (category == null) {
                                        ctx.getSource().sendFailure(Component.literal("Unknown category: " + catName));
                                        return 0;
                                    }
                                    player.getCapability(AbilityCapability.ABILITY_DATA).ifPresent(data -> {
                                        data.setCategoryLevel(category, 1);
                                        AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                                                new SyncAbilityDataPacket(data));
                                    });
                                    ctx.getSource().sendSuccess(() -> Component.literal("Learned " + catName), true);
                                    return 1;
                                }))));

        // La commande du JOUEUR, celle qui ouvre le mod : sans aptitude, elle en donne une. Elle lit
        // l'inventaire comme le developpeur — un facteur designe sa categorie et disparait, sinon le
        // sort en donne une — et elle previent le client, sans quoi l'affichage du joueur resterait
        // vide jusqu'a la prochaine reconnexion. Elle passe par le meme chemin que les deux boutons
        // LEARN de l'ecran, pour qu'il n'y ait qu'une facon de donner cette premiere aptitude.
        dispatcher.register(Commands.literal("learn").executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            Category learned = cn.academy.ability.develop.DevelopActionFirstCategory.grantAndSync(player);
            if (learned == null) {
                ctx.getSource().sendFailure(Component.translatable("academy.developer.already_has"));
                return 0;
            }
            ctx.getSource().sendSuccess(() -> Component.translatable("academy.developer.learned",
                    Component.translatable(learned.getDisplayKey())), false);
            return 1;
        }));
    }
}
