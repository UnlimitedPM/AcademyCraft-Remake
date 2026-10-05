package cn.academy.ability.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * L'aller-retour du paquet de la trainee du depose au loin.
 *
 * <p>Comme celui du sang et du coup critique, c'est la seule partie de la fonctionnalite qu'aucune
 * porte ne regarde : le serveur ne seme jamais d'etincelles, et le client ne fabrique jamais le
 * paquet qui lui arrive. Les deux bouts ecrits d'un cote et lus dans un autre ordre ne se verraient
 * qu'en jeu, sous la forme d'une trainee qui part d'ailleurs.
 */
class ShiftTeleportPacketTest {

    private static ShiftTeleportPacket relire(ShiftTeleportPacket original) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ShiftTeleportPacket.encode(original, buf);
        return ShiftTeleportPacket.decode(buf);
    }

    @Test
    @DisplayName("les deux bouts du trajet survivent au voyage")
    void lesDeuxBoutsSurvivent() {
        ShiftTeleportPacket round = relire(new ShiftTeleportPacket(
                new Vec3(12.5, 64.0625, -31.125), new BlockPos(-7, 69, 1284)));

        assertEquals(new Vec3(12.5, 64.0625, -31.125), round.feet(),
                "les pieds du lanceur gardent leur position, et jusqu'au centieme de bloc");
        assertEquals(new BlockPos(-7, 69, 1284), round.cell(),
                "et la case visee aussi : c'est elle qui donne le bout de la trainee");
    }
}
