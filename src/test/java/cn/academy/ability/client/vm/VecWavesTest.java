package cn.academy.ability.client.vm;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les ondes de choc de vecmanip.
 *
 * <p>Ce qui se relit sans Minecraft : la facon dont les anneaux se semment le long de l'axe, la
 * montee de l'echelle, et le fait qu'une onde <b>finisse toujours au meme moment</b> — quinze
 * ticks — quels que soient ses tirages. Une faute ici se verrait a l'ecran sous la forme d'une
 * onde trop courte, d'anneaux empiles, ou d'un effet qui ne s'efface jamais.
 */
class VecWavesTest {

    @Test
    @DisplayName("les anneaux se semment le long de l'axe")
    void lesAnneauxSeSemmentLeLongDeLAxe() {
        List<VecWaves.Ring> rings = VecWaves.rings(RandomSource.create(1), 3, 1.1);

        assertEquals(3, rings.size(), "autant d'anneaux que demande");
        for (int i = 0; i < 3; i++) {
            VecWaves.Ring ring = rings.get(i);

            assertEquals(i * VecWaves.RING_STEP, ring.offset(), VecWaves.RING_JITTER + 1e-9,
                    "l'anneau " + i + " se tient a un bloc et demi du precedent");
            assertTrue(ring.life() >= VecWaves.RING_LIFE_MIN
                            && ring.life() < VecWaves.RING_LIFE_MAX,
                    "et vit de huit a onze ticks : " + ring.life());
            assertTrue(ring.size() >= 1.1 * 0.8 && ring.size() <= 1.1 * 1.2,
                    "sa taille est celle demandee, au cinquieme pres : " + ring.size());
            assertTrue(ring.timeOffset() >= i * 2 - 1 && ring.timeOffset() <= i * 2,
                    "et son retard suit son rang : " + ring.timeOffset());
        }
    }

    @Test
    @DisplayName("l'echelle est celle de l'original")
    void lEchelleEstCelleDeLOriginal() {
        assertEquals(0.4, VecWaves.sizeScale(0), 1e-9,
                "une onde qui nait part de quatre dixiemes");
        assertEquals(0.8, VecWaves.sizeScale(4), 1e-9,
                "et passe a huit dixiemes en quatre ticks — le point de l'original");
        assertTrue(VecWaves.sizeScale(40) > VecWaves.sizeScale(4),
                "elle grandit ensuite, et ne depasse pas son dernier point : "
                        + VecWaves.sizeScale(40));
    }

    @Test
    @DisplayName("l'opacite monte, se tient, et retombe")
    void lOpaciteMontePuisRetombe() {
        assertEquals(0, VecWaves.maxAlpha(0), 1e-9, "une onde qui nait ne se voit pas encore");
        assertEquals(1, VecWaves.maxAlpha(3), 1e-9, "elle s'allume en trois ticks — son cinquieme");
        assertTrue(VecWaves.maxAlpha(7) > 0.9, "et se tient : " + VecWaves.maxAlpha(7));
        assertTrue(VecWaves.maxAlpha(14) < 0.5, "puis retombe a la fin : "
                + VecWaves.maxAlpha(14));
    }

    @Test
    @DisplayName("au bout de quinze ticks, plus rien n'est dessine")
    void rienNeSeDessineAuBout() {
        List<VecWaves.Ring> rings = VecWaves.rings(RandomSource.create(2), 3, 1);
        List<VecWaves.Ring> long_rings = VecWaves.rings(RandomSource.create(3), 3, 1);

        for (int i = 0; i < rings.size(); i++) {
            assertEquals(0, VecWaves.alpha(VecWaves.LIFE, rings.get(i)), 1e-9,
                    "la vie de l'onde prend le pas sur celle de ses anneaux");
            assertEquals(0, VecWaves.alpha(VecWaves.LIFE, long_rings.get(i)), 1e-9,
                    "quel que soit le tirage");
        }

        // Et l'opacite globale prend toujours le pas sur celle d'un anneau : un anneau qui vit
        // encore ne peut pas rendre l'onde plus opaque qu'elle ne l'est.
        for (int ticks = 0; ticks <= VecWaves.LIFE; ticks++) {
            for (VecWaves.Ring ring : rings) {
                assertTrue(VecWaves.alpha(ticks, ring)
                                <= VecWaves.maxAlpha(ticks) * VecWaves.DRAW_ALPHA + 1e-9,
                        "a " + ticks + " ticks, l'anneau ne depasse pas son onde");
            }
        }
    }

    @Test
    @DisplayName("la pile avance d'un quarantieme de bloc par tick")
    void laPileAvance() {
        assertEquals(0, VecWaves.drift(0), 1e-12, "elle part de son point d'ouverture");
        assertEquals(1, VecWaves.drift(40), 1e-12, "et avance d'un bloc en quarante ticks");
    }

    @Test
    @DisplayName("le tibre de dessin est celui de l'original")
    void leTibreDeDessinEstCeluiDeLOriginal() {
        assertEquals(0.7, VecWaves.DRAW_ALPHA, 1e-9,
                "l'original multipliait son opacite par sept dixiemes");
        assertEquals(15, VecWaves.LIFE, "et sa vie valait quinze ticks");
    }

    /**
     * L'age se lit entre deux ticks, pas au tick.
     *
     * <p>Tout ce qui vieillit dans une onde — l'echelle, l'opacite, l'avancee — ne changeait donc
     * qu'une fois par tick, et la lueur qui s'efface sautait vingt fois par seconde. Le joueur l'a
     * vu : « l'animation ne va qu'a 20 fps ». C'est la meme lecon que la poussiere des ailes, ou
     * que la fumee du choc au sol.
     */
    @Test
    @DisplayName("une onde vieillit entre deux ticks, pas par sauts")
    void lAgeSeLitEntreDeuxTicks() {
        VecWaves.clear();
        VecWaves.play(net.minecraft.world.phys.Vec3.ZERO, 0f, 0f, 2, 1);
        VecWaves.Wave onde = VecWaves.live().get(0);

        assertEquals(0.0, VecWaves.ageAt(onde, 0f), 1e-9, "a sa naissance, l'age est son tick");
        assertEquals(0.5, VecWaves.ageAt(onde, 0.5f), 1e-9, "plus la part du tick en cours");
        VecWaves.tick();
        assertEquals(1.75, VecWaves.ageAt(onde, 0.75f), 1e-9, "et il suit les ticks");
        VecWaves.clear();

        // Et les courbes lisent cet age-la : entre deux ticks, tout bouge encore.
        VecWaves.Ring anneau = new VecWaves.Ring(10, 0, 1, 0);
        double demiEchelle = VecWaves.sizeScale(4.5);
        assertTrue(demiEchelle > VecWaves.sizeScale(4) && demiEchelle < VecWaves.sizeScale(5),
                "l'echelle d'un demi-tick tient entre ses deux ticks : " + demiEchelle);

        double basse = VecWaves.alpha(8, anneau);
        double haute = VecWaves.alpha(9, anneau);
        double demiAlpha = VecWaves.alpha(8.5, anneau);
        assertTrue(demiAlpha > Math.min(basse, haute) && demiAlpha < Math.max(basse, haute),
                "et l'opacite d'un demi-tick aussi, elle qui retombe : " + demiAlpha);
    }
}
