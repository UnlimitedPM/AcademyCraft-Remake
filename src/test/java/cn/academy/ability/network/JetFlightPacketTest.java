package cn.academy.ability.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * L'aller-retour du paquet de depart du reacteur.
 *
 * <p>Il ne porte qu'un numero, mais c'est celui qui decide si le bouclier s'ouvre : le client
 * compare l'identifiant recu au sien, et un octet perdu en chemin veut dire un vol sans bouclier
 * ni trainee — silencieusement, puisque rien d'autre ne se plaint.
 */
class JetFlightPacketTest {

    private static JetFlightPacket relire(JetFlightPacket original) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        JetFlightPacket.encode(original, buf);
        return JetFlightPacket.decode(buf);
    }

    @Test
    @DisplayName("l'identifiant du porteur survit au voyage")
    void lIdentifiantSurvit() {
        assertEquals(42, relire(new JetFlightPacket(42)).playerId());
        assertEquals(0, relire(new JetFlightPacket(0)).playerId());
    }
}
