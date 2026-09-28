package cn.academy.ability.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L'aller-retour du paquet d'eclair.
 *
 * <p>C'est la seule partie de la fonctionnalite qu'aucune porte ne regarde : le serveur
 * n'ouvre jamais de rendu, et le client ne fabrique jamais le paquet qui lui arrive. Un
 * champ ecrit d'un cote et lu dans un autre ordre ne se verrait qu'en jeu, chez l'autre
 * joueur, et jamais chez celui qui tire.
 */
class ArcEffectPacketTest {

    private static ArcEffectPacket relire(ArcEffectPacket original) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ArcEffectPacket.encode(original, buf);
        return ArcEffectPacket.decode(buf);
    }

    @Test
    @DisplayName("les deux bouts survivent au voyage, en doubles")
    void lesDeuxBoutsSurvivent() {
        ArcEffectPacket original = new ArcEffectPacket("strong",
                new Vec3(12.5, 64.06250001, -31.125),
                new Vec3(-7.75, 70.5, 2004.25),
                10, true);

        ArcEffectPacket round = relire(original);

        assertEquals("strong", round.pattern(), "le motif voyage par son nom");
        assertEquals(original.from(), round.from(), "un eclair a mille blocs garde sa position");
        assertEquals(original.to(), round.to());
        assertEquals(10, round.lifeTicks());
        assertTrue(round.clipToDistance());
    }

    @Test
    @DisplayName("la duree de vie et l'arrondi voyagent aussi")
    void laDureeDeVieVoyage() {
        ArcEffectPacket round = relire(new ArcEffectPacket("weak",
                new Vec3(0, 0, 0), new Vec3(1, 1, 1), 1_200, false));

        assertEquals("weak", round.pattern());
        assertEquals(1_200, round.lifeTicks(), "un arc long vit plus de cent ticks");
        assertEquals(1, round.to().x, 1e-12, "et le dernier chiffre apres la virgule reste");
        assertFalse(round.clipToDistance(), "un arc fige garde la portee de son motif");
    }
}
