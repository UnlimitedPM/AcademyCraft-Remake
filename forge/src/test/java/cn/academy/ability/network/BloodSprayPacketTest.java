package cn.academy.ability.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * L'aller-retour du paquet des taches de sang.
 *
 * <p>Comme celui de la gerbe, c'est la seule partie de la fonctionnalite qu'aucune porte ne
 * regarde : le serveur ne dessine jamais de sang, et le client ne fabrique jamais le paquet qui lui
 * arrive. Une face ecrite d'un cote et lue dans un autre ordre ne se verrait qu'en jeu, chez
 * l'autre joueur.
 */
class BloodSprayPacketTest {

    private static BloodSprayPacket relire(BloodSprayPacket original) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        BloodSprayPacket.encode(original, buf);
        return BloodSprayPacket.decode(buf);
    }

    @Test
    @DisplayName("les faces tachees survivent au voyage, dans l'ordre")
    void lesFacesSurvivent() {
        List<BlockPos> positions = List.of(new BlockPos(12, 64, -31), new BlockPos(-8, 70, 5),
                new BlockPos(0, -60, 0));
        List<Direction> faces = List.of(Direction.UP, Direction.NORTH, Direction.DOWN);

        BloodSprayPacket round = relire(new BloodSprayPacket(positions, faces));

        assertEquals(positions, round.positions(), "les blocs gardent leur position et leur ordre");
        assertEquals(faces, round.faces(), "et chacun sa face");
    }

    @Test
    @DisplayName("un coup qui n'a rien touche ne pose rien")
    void unCoupDansLeVideNePoseRien() {
        BloodSprayPacket round = relire(new BloodSprayPacket(List.of(), List.of()));

        assertEquals(0, round.positions().size(), "rien a poser");
        assertEquals(0, round.faces().size(), "et rien a lire");
    }
}
