package cn.academy.ability.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * L'aller-retour du paquet des rayons du plasma.
 *
 * <p>Comme celui des eclairs, c'est la seule partie de la fonctionnalite qu'aucune porte ne
 * regarde : le serveur n'ouvre jamais de rendu, et le client ne fabrique jamais le paquet qui
 * lui arrive. Un genre mal ecrit ne se verrait qu'en jeu — et chez tous les joueurs a la fois,
 * sauf celui qui a tire.
 */
class MdRayPacketTest {

    private static MdRayPacket relire(MdRayPacket original) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        MdRayPacket.encode(original, buf);
        return MdRayPacket.decode(buf);
    }

    @Test
    @DisplayName("le genre du rayon et ses deux bouts survivent au voyage")
    void leGenreEtLesBoutsSurvivent() {
        MdRayPacket lue = relire(new MdRayPacket("mdray_small", new Vec3(1.5, 64, -3),
                new Vec3(9, 65.25, 12)));

        assertEquals("mdray_small", lue.kind());
        assertEquals(1.5, lue.from().x, 1e-9);
        assertEquals(64, lue.from().y, 1e-9);
        assertEquals(-3, lue.from().z, 1e-9);
        assertEquals(9, lue.to().x, 1e-9);
        assertEquals(65.25, lue.to().y, 1e-9);
        assertEquals(12, lue.to().z, 1e-9);
    }

    @Test
    @DisplayName("un genre inconnu retombe sur le petit rayon")
    void unGenreInconnuNeCasseRien() {
        assertEquals(cn.academy.ability.client.md.MdRayKind.SMALL,
                cn.academy.ability.client.md.MdRayKind.byName(relire(
                        new MdRayPacket("mdray_qui_n_existe_pas", Vec3.ZERO, Vec3.ZERO)).kind()));
    }
}
