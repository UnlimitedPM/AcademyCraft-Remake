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
 * Temporary test command until the Developer/skill-tree learning UI is ported.
 * Usage: /academy learn <category>
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
    }
}
