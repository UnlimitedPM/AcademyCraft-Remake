package cn.academy.ability.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L'aller-retour du paquet qui annonce un rayon minier.
 *
 * <p>Ce paquet ne porte que trois choses, et les trois comptent : sans le nom de la competence,
 * le client ne saurait pas quel rayon dessiner ; sans le mineur, il ne saurait pas sur quelle
 * tete le poser ; et sans l'ouverture, il ne saurait pas s'il faut l'allumer ou l'eteindre. Rien
 * de tout cela ne s'execute dans un test autrement — c'est le jeu qui relie les deux bouts.
 */
class MineRaySightPacketTest {

    private static MineRaySightPacket relire(MineRaySightPacket original) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        MineRaySightPacket.encode(original, buf);
        return MineRaySightPacket.decode(buf);
    }

    @Test
    @DisplayName("la competence, le mineur et l'ouverture survivent au voyage")
    void lesTroisChampsSurvivent() {
        MineRaySightPacket lue = relire(new MineRaySightPacket("mine_ray_luck", 4242, true));

        assertEquals("mine_ray_luck", lue.skill());
        assertEquals(4242, lue.ownerId(), "sans lui, le rayon se poserait sur la mauvaise tete");
        assertTrue(lue.open());
    }

    @Test
    @DisplayName("un arret se distingue d'une ouverture")
    void unArretSeDistingue() {
        assertFalse(relire(new MineRaySightPacket("mine_ray_basic", 7, false)).open(),
                "un arret lu comme une ouverture laisserait le rayon allume pour toujours");
    }
}
