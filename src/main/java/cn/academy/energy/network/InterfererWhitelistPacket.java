package cn.academy.energy.network;

import cn.academy.AbilityInterfererBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Ce qu'un joueur demande a la liste blanche du brouilleur : ajouter un nom, ou en retirer un.
 *
 * <h2>Pourquoi un paquet, alors que les boutons passent par le menu</h2>
 *
 * <p>Allumer la machine et regler son rayon passent par {@code clickMenuButton}, et c'est parfait :
 * ce sont deux nombres, et un identifiant de bouton suffit a les porter. Un nom de joueur, non : il
 * faut du texte. C'est le seul endroit de cette machine ou un identifiant de bouton ne suffit pas.
 *
 * <h2>Qui a le droit</h2>
 *
 * <p>Seul celui qui a pose la machine ecrit sa liste blanche. L'original laissait le panneau ouvert a
 * tout le monde, mais sa liste etait un reglage ordinaire ; ici, elle decide qui la machine epargne —
 * la laisser ouverte reviendrait a laisser n'importe qui s'y inscrire.
 */
public class InterfererWhitelistPacket {

    private final BlockPos pos;
    private final String name;

    /** Vrai pour retirer, faux pour ajouter. */
    private final boolean remove;

    public InterfererWhitelistPacket(BlockPos pos, String name, boolean remove) {
        this.pos = pos;
        this.name = name == null ? "" : name;
        this.remove = remove;
    }

    public static void encode(InterfererWhitelistPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeBoolean(msg.remove);
        buf.writeUtf(msg.name, cn.academy.energy.ConfigText.MAX_LENGTH);
    }

    public static InterfererWhitelistPacket decode(FriendlyByteBuf buf) {
        // L'ordre de lecture est celui de l'ecriture, et il ne l'etait pas : le drapeau passait
        // AVANT le nom, la lecture le prenait pour la longueur du texte, et tout nom ajoute arrivait
        // chez le serveur en charabia. Aucun nom n'a donc jamais ete inscrit — « je ne vois pas la
        // liste des pseudos qui sont whitelistes ». Un paquet se relit dans l'ordre ou il s'ecrit,
        // et c'est le genre de faute qu'aucune porte ne voit : seul un essai en jeu la montre.
        BlockPos pos = buf.readBlockPos();
        boolean remove = buf.readBoolean();
        String name = buf.readUtf(cn.academy.energy.ConfigText.MAX_LENGTH);
        return new InterfererWhitelistPacket(pos, name, remove);
    }

    public static void handle(InterfererWhitelistPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            if (!(sender.level().getBlockEntity(msg.pos) instanceof AbilityInterfererBlockEntity machine)) {
                return;
            }
            if (!sender.getGameProfile().getName().equals(machine.getPlacer())) return;

            if (msg.remove) machine.removeFromWhitelist(msg.name);
            else machine.addToWhitelist(msg.name);
        });
        ctx.get().setPacketHandled(true);
    }
}
