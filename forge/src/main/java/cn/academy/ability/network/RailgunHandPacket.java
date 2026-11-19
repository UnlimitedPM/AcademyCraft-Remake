package cn.academy.ability.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C : la rafale d'arcs du railgun vient de partir de la main de ce joueur — dessinez-la.
 *
 * <p>Portage du {@code MSG_CHARGE_EFFECT} de l'original, celui que son serveur envoyait a trente
 * blocs a la ronde quand une piece partait. Sans lui, l'electricite ne se dessinerait que chez
 * celui qui a lance la piece, et les autres joueurs le verraient jeter une piece sans rien de
 * special — c'est faux a l'ecran, et c'est meme tout ce que le joueur voulait voir.
 *
 * <p>Il ne porte qu'un <b>numero de joueur</b>, et c'est voulu : l'effet est le meme pour tout le
 * monde et ne depend d'aucune donnee de jeu — c'est une animation, pas un evenement. Le client qui
 * le recoit le dessine sur le modele de ce joueur-la, de trois quarts, et l'oublie au bout d'une
 * seconde et demie.
 *
 * <p>Comme les autres paquets de ce genre, seul {@code handle} nomme une classe de client et il ne
 * s'execute que la : un serveur dedie ne connait ni les arcs ni leur rendu.
 */
public class RailgunHandPacket {

    private final int playerId;

    public RailgunHandPacket(int playerId) {
        this.playerId = playerId;
    }

    public static void encode(RailgunHandPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.playerId);
    }

    public static RailgunHandPacket decode(FriendlyByteBuf buf) {
        return new RailgunHandPacket(buf.readVarInt());
    }

    public static void handle(RailgunHandPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> cn.academy.ability.client.RailgunHandEffect.onAnnounced(msg.playerId));
        ctx.setPacketHandled(true);
    }
}
