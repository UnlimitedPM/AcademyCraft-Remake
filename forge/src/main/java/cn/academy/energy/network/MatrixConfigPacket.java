package cn.academy.energy.network;

import cn.academy.energy.MatrixBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Ce qu'un joueur demande au Matrix depuis son interface : creer le reseau, le renommer, ou changer
 * son mot de passe.
 *
 * <p>Ce sont les trois messages de l'original — {@code MSG_INIT}, {@code MSG_CHANGE_SSID} et
 * {@code MSG_CHANGE_PASSWORD} — dans un seul paquet : les trois s'ecrivent au meme endroit et ne
 * different que par ce qu'on envoie, et une action dit lequel.
 *
 * <h2>Pourquoi le serveur seul ecrit</h2>
 *
 * <p>Le reseau appartient a celui qui a pose le Matrix : c'est une regle du serveur, et l'original
 * l'ecrivait deja — {@code if (matrix.getPlacerName == player.getName)}. Le paquet ne transporte donc
 * qu'une demande, et c'est le serveur qui verifie le poseur avant d'ecrire.
 */
public class MatrixConfigPacket {

    /** Ce que la demande veut faire du reseau. */
    public enum Action {
        /** Creer le reseau : c'est le bouton « INIT », qui n'arrive qu'une fois. */
        INIT,
        /** Renommer le reseau. */
        SSID,
        /** Changer son mot de passe. */
        PASSWORD
    }

    private final BlockPos pos;
    private final Action action;
    private final String ssid;
    private final String password;

    public MatrixConfigPacket(BlockPos pos, Action action, String ssid, String password) {
        this.pos = pos;
        this.action = action;
        this.ssid = ssid == null ? "" : ssid;
        this.password = password == null ? "" : password;
    }

    public static void encode(MatrixConfigPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeEnum(msg.action);
        // Deux textes plutot qu'un seul : l'action INIT a besoin des deux, et les deux autres
        // n'en lisent qu'un. Cela evite un paquet par champ, et cela tient en trente-deux octets.
        buf.writeUtf(msg.ssid, cn.academy.energy.ConfigText.MAX_LENGTH);
        buf.writeUtf(msg.password, cn.academy.energy.ConfigText.MAX_LENGTH);
    }

    public static MatrixConfigPacket decode(FriendlyByteBuf buf) {
        return new MatrixConfigPacket(buf.readBlockPos(), buf.readEnum(Action.class),
                buf.readUtf(cn.academy.energy.ConfigText.MAX_LENGTH),
                buf.readUtf(cn.academy.energy.ConfigText.MAX_LENGTH));
    }

    public static void handle(MatrixConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            if (!(sender.level().getBlockEntity(msg.pos) instanceof MatrixBlockEntity matrix)) return;
            if (!matrix.isOwner(sender.getGameProfile().getName())) return;

            switch (msg.action) {
                case INIT -> matrix.initNetwork(msg.ssid, msg.password);
                case SSID -> matrix.setSsid(msg.ssid);
                case PASSWORD -> matrix.setPassword(msg.password);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
