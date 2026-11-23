package cn.academy.ability.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * S2C : une bouffee de fumee vient de naitre a cet endroit.
 *
 * <p>C'est le choc au sol de vecmanip : le serveur sait ou le coup a porte, le client allume la
 * bouffee. Voir {@code Smokes}.
 */
public class SmokePuffPacket {

    private final Vec3 position;

    public SmokePuffPacket(Vec3 position) {
        this.position = position;
    }

    /** Annonce la bouffee a ceux qui voient le coup, et a celui qui l'a porte. */
    public static void send(Entity caster, Vec3 position) {
        if (caster == null) return;
        cn.academy.platform.network.Network.sendToTracking(new SmokePuffPacket(position), caster);
    }

    public static void encode(SmokePuffPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.position.x);
        buf.writeDouble(msg.position.y);
        buf.writeDouble(msg.position.z);
    }

    public static SmokePuffPacket decode(FriendlyByteBuf buf) {
        return new SmokePuffPacket(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    public static void handle(SmokePuffPacket msg) {
        cn.academy.ability.client.vm.Smokes.puff(msg.position);
    }

    /** L'endroit annonce. Lisible par le test, qui relit l'aller-retour du paquet. */
    Vec3 position() {
        return position;
    }
}
