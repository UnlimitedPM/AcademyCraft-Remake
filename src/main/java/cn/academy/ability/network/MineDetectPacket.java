package cn.academy.ability.network;

import cn.academy.ability.client.MineDetectOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: l'eclat de la detection de minerais a eu lieu, et voici ce qu'il faut allumer.
 *
 * <p>C'est le portage du {@code MSG_EFFECT} de l'original, et il ne part que si le paiement a
 * reussi : un client qui allumerait ses minerais sur une competence refusee montrerait ce que
 * le serveur ne lui a pas donne. Le client ne recoit donc que le droit de regarder — la
 * portee et le mode — et balaie le monde lui-meme, comme le faisait l'entite de rendu de
 * l'original.
 *
 * <p>Comme {@link SyncAbilityDataPacket}, ce paquet est le seul endroit qui touche a l'etat du
 * client : c'est le sens de la circulation, et le serveur ne connait pas ces classes.
 */
public class MineDetectPacket {

    private final float range;
    private final boolean advanced;

    public MineDetectPacket(float range, boolean advanced) {
        this.range = range;
        this.advanced = advanced;
    }

    public static void encode(MineDetectPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.range);
        buf.writeBoolean(msg.advanced);
    }

    public static MineDetectPacket decode(FriendlyByteBuf buf) {
        return new MineDetectPacket(buf.readFloat(), buf.readBoolean());
    }

    public static void handle(MineDetectPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> MineDetectOverlay.begin(msg.range, msg.advanced));
        ctx.setPacketHandled(true);
    }
}
