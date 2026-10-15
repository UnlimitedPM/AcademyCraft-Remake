package cn.academy.energy.network;

import cn.academy.energy.NodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Ce qu'un joueur demande au noeud depuis son interface : un nouveau nom, ou un nouveau mot de passe.
 *
 * <p>C'est le {@code MSG_RENAME} et le {@code MSG_CHANGE_PASS} de l'original, dans un seul paquet —
 * les deux ne diffèrent que par le champ qu'ils visent, et un drapeau dit lequel.
 *
 * <h2>Pourquoi le serveur seul ecrit</h2>
 *
 * <p>Le noeud appartient a celui qui l'a pose, et c'est une <b>regle du serveur</b> : un client ne
 * peut pas en decider seul, sans quoi n'importe qui renommerait le noeud de n'importe qui. Le paquet
 * ne transporte donc qu'une demande, et le serveur verifie le poseur avant d'ecrire — exactement
 * comme l'original, dont les deux ecouteurs commencaient par {@code if (player.getName ==
 * node.getPlacerName)}.
 */
public class NodeConfigPacket {

    private final BlockPos pos;

    /** Vrai pour le mot de passe, faux pour le nom. */
    private final boolean password;

    private final String value;

    public NodeConfigPacket(BlockPos pos, boolean password, String value) {
        this.pos = pos;
        this.password = password;
        this.value = value == null ? "" : value;
    }

    public static void encode(NodeConfigPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeBoolean(msg.password);
        // Un nom de noeud tient dans seize caracteres : la borne est aussi celle du champ de l'ecran,
        // et elle evite qu'un texte enorme traverse le reseau pour rien.
        buf.writeUtf(msg.value, 16);
    }

    public static NodeConfigPacket decode(FriendlyByteBuf buf) {
        return new NodeConfigPacket(buf.readBlockPos(), buf.readBoolean(), buf.readUtf(16));
    }

    public static void handle(NodeConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            if (sender.level().getBlockEntity(msg.pos) instanceof NodeBlockEntity node
                    && node.isOwner(sender.getGameProfile().getName())) {
                if (msg.password) node.setPassword(msg.value);
                else node.setNodeName(msg.value);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
