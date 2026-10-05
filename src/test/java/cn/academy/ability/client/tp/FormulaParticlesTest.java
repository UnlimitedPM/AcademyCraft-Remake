package cn.academy.ability.client.tp;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les particules de formule : leur vie, leur derive, leur place, et leur gerbe.
 *
 * <p>C'est ce qu'aucune porte ne regarde autrement : elles ne vivent qu'a l'ecran, chez le client, et
 * rien n'en garde trace. Leurs nombres, eux, viennent de l'original — {@code FormulaParticleFactory}
 * pour la vie et la taille, {@code CriticalHitEffect} pour le semis — et ce sont eux qui decident de
 * l'allure du coup critique.
 */
class FormulaParticlesTest {

    @Test
    @DisplayName("un fragment apparait, se tient, puis s'efface")
    void laVieDUneParticule() {
        // Il apparait en deux ticks, se tient dix, puis s'efface en vingt : trente-deux ticks.
        FormulaParticles.Glyph glyph = new FormulaParticles.Glyph(
                Vec3.ZERO, Vec3.ZERO, 0, 0.1, 0.5f, 10, 1_000L);

        assertEquals(0f, glyph.alpha(1_000L), 1e-6, "il nait invisible");
        assertEquals(0.25f, glyph.alpha(1_050L), 1e-6, "apres un tick, la moitie de son opacite");
        assertEquals(0.5f, glyph.alpha(1_100L), 1e-6, "et plein a la fin de son apparition");
        assertEquals(0.5f, glyph.alpha(1_500L), 1e-6, "il se tient dix ticks durant");

        assertEquals(0.25f, glyph.alpha(2_000L), 1e-6, "a mi-effacement, la moitie");
        assertFalse(glyph.finished(2_499L), "il vit trente ticks en tout");
        assertTrue(glyph.finished(2_500L), "et s'efface au trente-et-unieme");
        assertEquals(0f, glyph.alpha(3_000L), 1e-6, "sans opacite, et pour toujours");
    }

    @Test
    @DisplayName("un fragment derive de sa vitesse, un bloc par tick")
    void laDeriveDUneParticule() {
        FormulaParticles.Glyph glyph = new FormulaParticles.Glyph(
                new Vec3(1, 2, 3), new Vec3(0.01, 0, -0.02), 0, 0.1, 1f, 10, 0L);

        Vec3 at = glyph.position(500L);
        assertEquals(1.1, at.x, 1e-9, "dix ticks de derive");
        assertEquals(2.0, at.y, 1e-9);
        assertEquals(2.8, at.z, 1e-9);

        assertEquals(2.0, glyph.position(0L).y, 1e-9, "et il part de son origine");
    }

    @Test
    @DisplayName("un fragment nait sur le cote de la bete")
    void ouNaitUnFragment() {
        Vec3 feet = new Vec3(10, 64, -5);

        // En haut du corps et sur le flanc droit : le tour se lit en sinus et cosinus, la hauteur en
        // fraction de celle de la creature.
        Vec3 at = FormulaParticles.place(feet, 2.0, Math.PI / 2, 0.4, 1.0);
        assertEquals(10.4, at.x, 1e-9);
        assertEquals(66.0, at.y, 1e-9);
        assertEquals(-5.0, at.z, 1e-9);

        // Et le tirage peut tomber au ras du sol, de l'autre cote.
        Vec3 low = FormulaParticles.place(feet, 2.0, 0.0, 0.25, 0.0);
        assertEquals(10.0, low.x, 1e-9);
        assertEquals(64.0, low.y, 1e-9);
        assertEquals(-4.75, low.z, 1e-9);
    }

    @Test
    @DisplayName("une gerbe seme cinq a sept fragments, dans le corps vise")
    void uneGerbeSeSemeDansLeCorps() {
        Vec3 feet = new Vec3(3, 70, 8);
        double width = 0.6;
        double height = 1.8;
        FormulaParticles.clear();

        for (int i = 0; i < 40; i++) {
            FormulaParticles.burst(feet, width, height);
        }

        List<FormulaParticles.Glyph> live = FormulaParticles.live();
        assertTrue(live.size() >= 40 * FormulaParticles.COUNT_MIN,
                "chaque gerbe en seme au moins cinq : " + live.size());
        assertTrue(live.size() <= 40 * (FormulaParticles.COUNT_MAX - 1),
                "et sept au plus, la borne haute etant exclue comme chez l'original : " + live.size());

        for (FormulaParticles.Glyph glyph : live) {
            Vec3 at = glyph.origin();
            double radius = Math.sqrt(Math.pow(at.x - feet.x, 2) + Math.pow(at.z - feet.z, 2));
            assertTrue(radius >= FormulaParticles.RADIUS_MIN * width - 1e-9
                            && radius <= FormulaParticles.RADIUS_MAX * width + 1e-9,
                    "jamais plus pres ni plus loin du corps que sa part de largeur : " + radius);
            assertTrue(at.y >= feet.y && at.y <= feet.y + height,
                    "et toujours entre ses pieds et son sommet : " + at.y);
            assertTrue(glyph.size() >= FormulaParticles.SIZE_MIN
                            && glyph.size() <= FormulaParticles.SIZE_MAX,
                    "sa taille est tiree dans les bornes : " + glyph.size());
            assertTrue(glyph.frame() >= 0 && glyph.frame() < FormulaParticles.TEXTURES,
                    "il porte une des dix images de formule : " + glyph.frame());
            assertTrue(glyph.startAlpha() >= FormulaParticles.ALPHA_MIN / 255f - 1e-6
                            && glyph.startAlpha() <= 1f,
                    "son opacite est tiree de pale a pleine : " + glyph.startAlpha());
            assertTrue(glyph.holdTicks() >= FormulaParticles.HOLD_MIN_TICKS
                            && glyph.holdTicks() < FormulaParticles.HOLD_MAX_TICKS,
                    "et sa tenue aussi : " + glyph.holdTicks());
            assertTrue(glyph.velocity().length() <= FormulaParticles.DRIFT + 1e-9
                            && glyph.velocity().length() > 0,
                    "sa derive ne depasse pas un dixieme de bloc : " + glyph.velocity().length());        }

        FormulaParticles.clear();
        assertTrue(FormulaParticles.live().isEmpty(), "et on peut les oublier d'un coup");
    }
}
