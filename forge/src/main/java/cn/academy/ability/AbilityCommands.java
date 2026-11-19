package cn.academy.ability;

import com.mojang.brigadier.CommandDispatcher;
import cn.academy.AcademyCraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Les commandes du mod, posees a l'enregistrement.
 *
 * <p>Il n'y a plus qu'une famille : {@code /aim} et {@code /aimp}, le portage de
 * {@code CommandAIMBase}. Le joueur a fait retirer {@code /learn} du chat, puis
 * {@code /academy learn <categorie>} : « elle n'a rien a faire la non plus etant donne qu'il y a
 * deja une VRAIE commande pour donner des pouvoirs et c'est {@code /aim cat <cat>} ». Une seule
 * porte pour les essais, donc, et c'est celle de l'original.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID)
public class AbilityCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        // Les commandes de debogage du mod d'origine : /aim (sur soi) et /aimp (sur un autre).
        cn.academy.command.AimCommands.register(dispatcher);
    }
}
