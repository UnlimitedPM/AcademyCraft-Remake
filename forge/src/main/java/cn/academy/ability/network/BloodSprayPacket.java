package cn.academy.ability.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S2C : voici les faces de blocs que le sang du retour de sang a tachees.
 *
 * <p>Le serveur les a tracees lui-meme, et c'est le point : lui seul sait <b>qui</b> a ete touche,
 * donc lui seul sait s'il faut en semer. Le port les tirait chez le client, et le joueur a vu les
 * deux defauts que cela donne — « parfois les monstres ne subissent aucun degat » et pourtant des
 * taches apparaissaient, et « si je vise dans le vide, ca fait quand meme des taches de sang ». Le
 * client ne pouvait pas savoir : il ne tire pas le rayon du coup, c'est le serveur.
 *
 * <p>Il tirait aussi ses neuf directions des <b>yeux du tireur</b> au lieu de la tete de la
 * victime, ce qui envoyait les taches plus loin — « les taches de sang se dispersent trop sur les
 * murs ». C'est le serveur qui les tire maintenant, depuis la bonne tete et avec les nombres de
 * l'original.
 *
 * <p>Comme les autres paquets de ce genre, seul {@code handle} nomme une classe de client, et il ne
 * s'execute que la.
 */
public class BloodSprayPacket {

    private final List<BlockPos> positions;
    private final List<Direction> faces;

    public BloodSprayPacket(List<BlockPos> positions, List<Direction> faces) {
        this.positions = List.copyOf(positions);
        this.faces = List.copyOf(faces);
    }

    /** Annonce les taches a ceux qui voient la victime, et a elle. */
    public static void send(Entity target, List<BlockPos> positions, List<Direction> faces) {
        if (target == null || positions.isEmpty()) return;
        AbilityNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target),
                new BloodSprayPacket(positions, faces));
    }

    public static void encode(BloodSprayPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.positions.size());
        for (int i = 0; i < msg.positions.size(); i++) {
            buf.writeBlockPos(msg.positions.get(i));
            buf.writeByte(msg.faces.get(i).get3DDataValue());
        }
    }

    public static BloodSprayPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<BlockPos> positions = new ArrayList<>(count);
        List<Direction> faces = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            positions.add(buf.readBlockPos());
            faces.add(Direction.from3DDataValue(buf.readByte()));
        }
        return new BloodSprayPacket(positions, faces);
    }

    public static void handle(BloodSprayPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> cn.academy.ability.client.tp.BloodSprays.poseAll(
                msg.positions, msg.faces));
        ctx.setPacketHandled(true);
    }

    /** Les blocs taches. Lisible par le test, qui relit l'aller-retour du paquet. */
    List<BlockPos> positions() {
        return positions;
    }

    /** Et la face de chacun, celle que porte la tache. */
    List<Direction> faces() {
        return faces;
    }
}
