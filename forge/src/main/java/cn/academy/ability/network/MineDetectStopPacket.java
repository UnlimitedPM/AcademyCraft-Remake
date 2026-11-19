package cn.academy.ability.network;

import cn.academy.ability.client.MineDetectOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: l'eclat de la detection de minerais s'arrete.
 *
 * <p>Il n'existait pas dans l'original, qui chronometrait son eclat : cent ticks, et le client
 * savait tout seul quand eteindre. Le port le fait tenir par la reserve (voir
 * {@code MineDetectSkill.tick}), donc c'est le serveur qui decide de la fin — reserve epuisee,
 * plafond de la competence, brouillage, mort du porteur, ou un second appui du joueur.
 *
 * <p>Il n'a rien a porter : le client n'a qu'a eteindre ce qu'il allumait.
 */
public class MineDetectStopPacket {

    public static void encode(MineDetectStopPacket msg, FriendlyByteBuf buf) {
        // Rien : l'arret est un evenement, pas une donnee.
    }

    public static MineDetectStopPacket decode(FriendlyByteBuf buf) {
        return new MineDetectStopPacket();
    }

    public static void handle(MineDetectStopPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(MineDetectOverlay::stop);
        ctx.setPacketHandled(true);
    }
}
