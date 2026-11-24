package cn.academy.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

/**
 * Les commandes du mod, posees sur le repartiteur du chargeur.
 *
 * <p>Il n'y a qu'une famille : {@code /aim} et {@code /aimp}, le portage de {@code CommandAIMBase}.
 * Le joueur a fait retirer {@code /learn} du chat, puis {@code /academy learn <categorie>} : « elle
 * n'a rien a faire la non plus etant donne qu'il y a deja une VRAIE commande pour donner des pouvoirs
 * et c'est {@code /aim cat <cat>} ». Une seule porte pour les essais, donc, et c'est celle de
 * l'original.
 *
 * <p>Le repartiteur, lui, appartient au chargeur : Forge le donne a son evenement d'enregistrement,
 * Fabric a son rappel de commandes. Le code commun ne fait que nommer ce qu'il faut poser.
 */
public final class Commands {

    private Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        AimCommands.register(dispatcher);
    }
}
