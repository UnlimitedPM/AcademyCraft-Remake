package cn.academy.ability.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * L'aller-retour du paquet de depart du reacteur.
 *
 * <p>Il porte un numero et deux points, et ce n'est pas du confort : le client pose sa position sur
 * cette trajectoire a chaque tick, et le serveur pose la meme. Un chiffre perdu en chemin, et les
 * deux se corrigent l'un l'autre a chaque tick — le vol redevient la saccade qu'il etait, sans que
 * rien ne se plaigne.
 */
class JetFlightPacketTest {

    private static JetFlightPacket relire(JetFlightPacket original) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        JetFlightPacket.encode(original, buf);
        return JetFlightPacket.decode(buf);
    }

    private static void memePoint(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x, actual.x, 0.0);
        assertEquals(expected.y, actual.y, 0.0);
        assertEquals(expected.z, actual.z, 0.0);
    }

    @Test
    @DisplayName("l'identifiant du porteur et sa trajectoire survivent au voyage")
    void laTrajectoireSurvit() {
        var start = new Vec3(-232.5, 127.0625, -118.5);
        var target = new Vec3(-226.5, 128.0, -124.25);

        var relu = relire(new JetFlightPacket(42, start, target));

        assertEquals(42, relu.playerId());
        memePoint(start, relu.start());
        memePoint(target, relu.target());
    }
}
