package cn.academy.ability.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * L'aller-retour du paquet du renfort.
 *
 * <p>Comme celui des eclairs, c'est la seule partie de la fonctionnalite qu'aucune porte ne
 * regarde : le serveur n'ouvre jamais de rendu, et le client ne fabrique jamais le paquet qui
 * lui arrive. Un numero d'entite mal ecrit ne se verrait qu'en jeu — et chez tous les joueurs a
 * la fois, sauf celui qui a lance le renfort.
 */
class BodyIntensifyPacketTest {

    private static BodyIntensifyPacket relire(BodyIntensifyPacket original) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        BodyIntensifyPacket.encode(original, buf);
        return BodyIntensifyPacket.decode(buf);
    }

    @Test
    @DisplayName("le porteur du renfort survit au voyage")
    void lePorteurSurvit() {
        assertEquals(42, relire(new BodyIntensifyPacket(42)).ownerId());
    }

    @Test
    @DisplayName("un identifiant au-dela du million passe aussi")
    void unGrandIdentifiantPasse() {
        assertEquals(1_500_000, relire(new BodyIntensifyPacket(1_500_000)).ownerId());
    }
}
