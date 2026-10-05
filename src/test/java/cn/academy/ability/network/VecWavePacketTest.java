package cn.academy.ability.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * L'aller-retour du paquet d'onde de vecmanip.
 *
 * <p>Comme les autres paquets d'effet, c'est la seule partie de la fonctionnalite qu'aucune porte ne
 * regarde : le serveur n'ouvre jamais d'onde, et le client ne fabrique jamais le paquet qui lui
 * arrive. Un champ ecrit d'un cote et lu dans un autre ordre ne se verrait qu'en jeu, sous la forme
 * d'une onde qui s'ouvre ailleurs, ou du mauvais cote.
 */
class VecWavePacketTest {

    private static VecWavePacket relire(VecWavePacket original) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        VecWavePacket.encode(original, buf);
        return VecWavePacket.decode(buf);
    }

    @Test
    @DisplayName("l'onde survit au voyage, avec son orientation")
    void lOndeSurvitAuVoyage() {
        VecWavePacket round = relire(new VecWavePacket(
                new Vec3(12.5, 64.0625, -31.125), -37.5f, 12.25f, 3, 1.1));

        assertEquals(new Vec3(12.5, 64.0625, -31.125), round.position(),
                "l'onde s'ouvre au meme endroit, jusqu'au centieme de bloc");
        assertEquals(-37.5f, round.yaw(), 1e-6, "du meme cote : son lacet");
        assertEquals(12.25f, round.pitch(), 1e-6, "et son tangage");
        assertEquals(3, round.rings(), "avec le meme nombre d'anneaux");
        assertEquals(1.1, round.size(), 1e-9, "et la meme taille");
    }
}
