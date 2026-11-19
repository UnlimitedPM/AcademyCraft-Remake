package cn.academy.ability.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * L'aller-retour du paquet de la marque de radiation.
 *
 * <p>Comme les autres, c'est la partie qu'aucune porte ne regarde : le serveur n'ouvre jamais de
 * rendu, et le client ne fabrique jamais le paquet qui lui arrive. Une duree mal ecrite ne se
 * verrait qu'en jeu — et la fumee du plasma s'arreterait trop tot chez tout le monde sauf le
 * tireur, ce qui est exactement le defaut que ce paquet corrige.
 */
class RadiationMarkPacketTest {

    private static RadiationMarkPacket relire(RadiationMarkPacket original) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        RadiationMarkPacket.encode(original, buf);
        return RadiationMarkPacket.decode(buf);
    }

    @Test
    @DisplayName("la cible et la duree de la marque survivent au voyage")
    void laCibleEtLaDureeSurvivent() {
        RadiationMarkPacket lue = relire(new RadiationMarkPacket(512, 60));

        assertEquals(512, lue.entityId());
        assertEquals(60, lue.ticks());
    }

    @Test
    @DisplayName("une duree longue passe aussi")
    void uneDureeLonguePasse() {
        // La marque dure soixante ticks au minimum, mais une cible deja marquee garde le temps
        // qui lui restait s'il est plus long — d'ou des durees bien au-dela.
        assertEquals(1_500_000, relire(new RadiationMarkPacket(1, 1_500_000)).ticks());
    }
}
