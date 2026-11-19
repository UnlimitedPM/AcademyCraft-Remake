package cn.academy.ability.client.tp;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La gerbe de sang : son defilement, sa place, et son semis.
 *
 * <p>C'est ce qu'aucune porte ne regarde autrement : la gerbe ne vit qu'a l'ecran, chez le client,
 * et rien n'en garde trace. Ses nombres, eux, viennent de l'original, et ce sont eux qui decident
 * de l'allure du coup.
 */
class BloodSplashesTest {

    @Test
    @DisplayName("une eclaboussure defile ses dix images, une par tick")
    void leDefilementDesImages() {
        BloodSplashes.Splash splash = new BloodSplashes.Splash(Vec3.ZERO, 1.0, 1_000L);

        assertEquals(0, splash.frame(1_000L), "elle nait sur sa premiere image");
        assertEquals(0, splash.frame(1_049L), "quarante-neuf millisecondes plus tard, la meme");
        assertEquals(1, splash.frame(1_050L), "a cinquante, la deuxieme");
        assertEquals(9, splash.frame(1_499L), "et la dixieme a la fin de sa vie");
        assertEquals(9, splash.frame(9_999L), "sans jamais depasser la table d'images");

        assertFalse(splash.finished(1_499L), "elle vit dix ticks");
        assertTrue(splash.finished(1_500L), "et s'efface au onzieme");
    }

    @Test
    @DisplayName("une eclaboussure se pose sur le cote de la bete")
    void ouTombeUneEclaboussure() {
        Vec3 feet = new Vec3(10, 64, -5);

        // En haut du corps et sur le flanc droit : le rayon se lit en sinus et cosinus, la hauteur
        // en fraction de celle de la creature.
        Vec3 at = BloodSplashes.place(feet, 2.0, Math.PI / 2, 0.4, 1.0);
        assertEquals(10.4, at.x, 1e-9);
        assertEquals(66.0, at.y, 1e-9);
        assertEquals(-5.0, at.z, 1e-9);

        // Et le tirage peut tomber au ras du sol, de l'autre cote.
        Vec3 low = BloodSplashes.place(feet, 2.0, 0.0, 0.25, 0.0);
        assertEquals(10.0, low.x, 1e-9);
        assertEquals(64.0, low.y, 1e-9);
        assertEquals(-4.75, low.z, 1e-9);
    }

    @Test
    @DisplayName("une gerbe seme quatre a six eclaboussures, dans le corps vise")
    void uneGerbeSeSemeDansLeCorps() {
        Vec3 feet = new Vec3(3, 70, 8);
        double width = 0.6;
        double height = 1.8;
        BloodSplashes.clear();

        for (int i = 0; i < 40; i++) {
            BloodSplashes.burst(feet, width, height);
        }

        List<BloodSplashes.Splash> live = BloodSplashes.live();
        assertTrue(live.size() >= 40 * BloodSplashes.MIN_COUNT,
                "chaque gerbe en seme au moins quatre : " + live.size());
        assertTrue(live.size() <= 40 * BloodSplashes.MAX_COUNT,
                "et six au plus : " + live.size());

        for (BloodSplashes.Splash splash : live) {
            Vec3 at = splash.position();
            double radius = Math.sqrt(Math.pow(at.x - feet.x, 2) + Math.pow(at.z - feet.z, 2));
            assertTrue(radius <= BloodSplashes.SPREAD * width + 1e-9,
                    "jamais plus loin du corps que la part de son rayon : " + radius);
            assertTrue(at.y >= feet.y && at.y <= feet.y + height,
                    "et toujours entre ses pieds et son sommet : " + at.y);
            assertTrue(splash.size() >= BloodSplashes.MIN_SIZE
                            && splash.size() <= BloodSplashes.MAX_SIZE,
                    "sa taille est tiree dans les bornes : " + splash.size());
        }

        BloodSplashes.clear();
        assertTrue(BloodSplashes.live().isEmpty(), "et on peut les oublier d'un coup");
    }
}
