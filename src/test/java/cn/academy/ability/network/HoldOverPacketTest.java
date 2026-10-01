package cn.academy.ability.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * L'aller-retour du paquet de fin de maintien.
 *
 * <p>C'est ce que le client relit pour savoir quel maintien fermer : un nom mal ecrit ne fermerait
 * rien du tout, et le joueur garderait son animation pour un pouvoir que le serveur a refuse —
 * exactement ce que ce paquet existe pour empecher. Le nom est donc la seule chose a figer ici.
 */
class HoldOverPacketTest {

    private static HoldOverPacket relire(HoldOverPacket original) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        HoldOverPacket.encode(original, buf);
        return HoldOverPacket.decode(buf);
    }

    @Test
    @DisplayName("le nom de la competence survit au voyage")
    void leNomSurvit() {
        assertEquals("light_shield", relire(new HoldOverPacket("light_shield")).skillName().orElse(null));
        assertEquals("jet_engine", relire(new HoldOverPacket("jet_engine")).skillName().orElse(null));
    }
}
