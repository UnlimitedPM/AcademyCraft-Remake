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

    /**
     * La duree de l'eclat, en ticks, et elle vient du SERVEUR.
     *
     * <p>Le client la lisait autrefois dans {@code MineDetectSkill.TIME}, une constante : un
     * eclat qui tient par la reserve (voir {@code MineDetectSkill.extraTicks}) dure pourtant
     * aussi longtemps que le serveur l'a paye, et c'est donc a lui de le dire.
     */
    private final int ticks;

    /**
     * L'eclat peut-il tenir au-dela de {@code ticks} ? Oui quand la competence a de l'entretien
     * a donner : le client garde alors l'eclat allume jusqu'au paquet d'arret.
     *
     * <p>A 0 % d'experience il vaut faux, et le client se contente des {@code ticks} du paquet —
     * l'eclat de l'original, au tick pres.
     */
    private final boolean hold;

    public MineDetectPacket(float range, boolean advanced, int ticks, boolean hold) {
        this.range = range;
        this.advanced = advanced;
        this.ticks = ticks;
        this.hold = hold;
    }

    public static void encode(MineDetectPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.range);
        buf.writeBoolean(msg.advanced);
        buf.writeVarInt(msg.ticks);
        buf.writeBoolean(msg.hold);
    }

    public static MineDetectPacket decode(FriendlyByteBuf buf) {
        return new MineDetectPacket(buf.readFloat(), buf.readBoolean(), buf.readVarInt(),
                buf.readBoolean());
    }

    public static void handle(MineDetectPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> MineDetectOverlay.begin(msg.range, msg.advanced, msg.ticks, msg.hold));
        ctx.setPacketHandled(true);
    }
}
