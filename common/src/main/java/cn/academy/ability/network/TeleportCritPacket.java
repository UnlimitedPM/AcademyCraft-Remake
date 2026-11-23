package cn.academy.ability.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * S2C : un coup de teleporteur vient de porter un critique, et voici sur qui.
 *
 * <p>C'est le portage du {@code MSG_EFFECT} de l'original, celui qui portait jusqu'au client la
 * victime du coup — sa position, sa largeur, sa hauteur — pour que la gerbe de formule eclose autour
 * d'elle. Le serveur est seul a savoir qui a ete touche : c'est lui qui trace le rayon, et c'est donc
 * de lui que part la gerbe. Voir {@code FormulaParticles}.
 *
 * <p>L'original ne l'envoyait qu'a celui qui frappait, parce que son effet naissait dans le contexte
 * client du tireur. Le port le dit a tous ceux qui <b>voient</b> la victime : la formule se deplie
 * autour de la bete, pas du geste, et les joueurs d'a cote verraient sinon leur compagnon encaisser
 * un coup critique sans rien voir.
 *
 * <p>Les deux passives du teleporteur ne s'activent jamais : ce message est la seule chose qu'elles
 * donnent a voir, et c'est pour cela qu'il existe.
 */
public class TeleportCritPacket {

    private final Vec3 feet;
    private final float width;
    private final float height;

    public TeleportCritPacket(Vec3 feet, float width, float height) {
        this.feet = feet;
        this.width = width;
        this.height = height;
    }

    /** Annonce la gerbe a ceux qui voient la victime, et a elle. */
    public static void send(Entity target) {
        if (target == null) return;
        cn.academy.platform.network.Network.sendToTracking(new TeleportCritPacket(
                target.position(), target.getBbWidth(), target.getBbHeight()), target);
    }

    public static void encode(TeleportCritPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.feet.x);
        buf.writeDouble(msg.feet.y);
        buf.writeDouble(msg.feet.z);
        buf.writeFloat(msg.width);
        buf.writeFloat(msg.height);
    }

    public static TeleportCritPacket decode(FriendlyByteBuf buf) {
        return new TeleportCritPacket(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                buf.readFloat(), buf.readFloat());
    }

    public static void handle(TeleportCritPacket msg) {
        cn.academy.ability.client.tp.FormulaParticles.burst(msg.feet, msg.width, msg.height);
    }

    /** Les pieds de la victime. Lisible par le test, qui relit l'aller-retour du paquet. */
    Vec3 feet() {
        return feet;
    }

    /** Sa largeur, et sa hauteur. Lisible par le test, comme les pieds. */
    float width() {
        return width;
    }

    float height() {
        return height;
    }
}
