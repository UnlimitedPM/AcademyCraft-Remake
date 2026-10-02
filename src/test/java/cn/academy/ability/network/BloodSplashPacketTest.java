package cn.academy.ability.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * L'aller-retour du paquet de sang.
 *
 * <p>Comme celui des eclairs, c'est la seule partie de la fonctionnalite qu'aucune porte ne
 * regarde : le serveur ne dessine jamais de sang, et le client ne fabrique jamais le paquet qui
 * lui arrive. Un champ ecrit d'un cote et lu dans un autre ordre ne se verrait qu'en jeu, chez
 * l'autre joueur, et jamais chez celui qui frappe.
 */
class BloodSplashPacketTest {

    private static BloodSplashPacket relire(BloodSplashPacket original) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        BloodSplashPacket.encode(original, buf);
        return BloodSplashPacket.decode(buf);
    }

    @Test
    @DisplayName("la victime survit au voyage, avec sa boite")
    void laVictimeSurvit() {
        BloodSplashPacket round = relire(
                new BloodSplashPacket(new Vec3(12.5, 64.0625, -31.125), 0.6f, 1.95f));

        assertEquals(new Vec3(12.5, 64.0625, -31.125), round.feet(),
                "les pieds de la victime gardent leur position");
        assertEquals(0.6f, round.width(), 1e-6, "et sa largeur, qui donne le rayon de la gerbe");
        assertEquals(1.95f, round.height(), 1e-6, "et sa hauteur, qui la lui donne en hauteur");
    }
}
