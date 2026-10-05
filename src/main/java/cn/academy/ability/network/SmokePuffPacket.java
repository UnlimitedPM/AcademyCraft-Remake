package cn.academy.ability.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * S2C : une bouffee de fumee vient de naitre ici.
 *
 * <p>Le serveur ne dit que l'endroit. Tout le reste — sa vitesse, son image parmi quatre, et le
 * modificateur qui etire sa vie — se tire chez le client, comme dans l'entite de fumee de
 * l'original. Un paquet par bouffee, donc, mais une bouffee nait sur la moitie des blocs casses :
 * c'est le meme trafic qu'une particule de vanilla, et c'est ce que le port faisait avant.
 *
 * <p>Comme les autres paquets de ce genre, seul {@code handle} nomme une classe de client.
 */
public class SmokePuffPacket {

    private final Vec3 position;

    public SmokePuffPacket(Vec3 position) {
        this.position = position;
    }

    /** Annonce la bouffee a ceux qui voient le coup, et a celui qui l'a porte. */
    public static void send(Entity caster, Vec3 position) {
        if (caster == null) return;
        AbilityNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> caster),
                new SmokePuffPacket(position));
    }

    public static void encode(SmokePuffPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.position.x);
        buf.writeDouble(msg.position.y);
        buf.writeDouble(msg.position.z);
    }

    public static SmokePuffPacket decode(FriendlyByteBuf buf) {
        return new SmokePuffPacket(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    public static void handle(SmokePuffPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> cn.academy.ability.client.vm.Smokes.puff(msg.position));
        ctx.setPacketHandled(true);
    }

    /** L'endroit annonce. Lisible par le test, qui relit l'aller-retour du paquet. */
    Vec3 position() {
        return position;
    }
}
