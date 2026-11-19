package cn.academy.ability.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * L'aller-retour du paquet des eclairs de sol du claquement d'orage.
 *
 * <p>Comme les autres paquets d'effet, c'est la seule partie de la fonctionnalite qu'aucune porte ne
 * regarde : le serveur ne seme jamais d'eclair, et le client ne fabrique jamais le paquet qui lui
 * arrive. Un champ ecrit d'un cote et lu dans un autre ordre ne se verrait qu'en jeu — sous la forme
 * d'eclairs qui jaillissent a cote de l'impact, ou d'un rayon faux, donc d'une vague qui couvre trop
 * ou pas assez.
 */
class ThunderClapGroundPacketTest {

    private static ThunderClapGroundPacket relire(ThunderClapGroundPacket original) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ThunderClapGroundPacket.encode(original, buf);
        return ThunderClapGroundPacket.decode(buf);
    }

    @Test
    @DisplayName("l'impact et le rayon survivent au voyage")
    void lImpactEtLeRayonSurviventAuVoyage() {
        ThunderClapGroundPacket round =
                relire(new ThunderClapGroundPacket(new Vec3(12.5, 64.0625, -31.125), 22.5));

        assertEquals(new Vec3(12.5, 64.0625, -31.125), round.impact(),
                "la foudre est tombee au meme endroit, jusqu'au dix-millieme de bloc");
        assertEquals(22.5, round.radius(), 1e-9, "et le semis couvre le meme rayon");
    }
}
