package cn.academy.ability.client.md;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les rayons du plasma : leurs nombres, et les trois courbes de leur vie.
 *
 * <p>Tout cela se lisait dans l'original en <b>millisecondes</b> — sa {@code getLength},
 * sa {@code getWidth} et sa {@code getAlpha} multipliaient son horloge a la seconde par
 * mille — donc les courbes ci-dessous se deroulent au dixieme de seconde, et non au tick.
 * C'est ce qui les rend fausses des qu'on les prend pour des ticks : un rayon de quatorze
 * ticks vit sept dixiemes de seconde, pas quatorze.
 */
class MdRaysTest {

    private static final long BIRTH = 100_000L;

    private static MdRays.LiveRay rayon(MdRayKind kind) {
        MdRays.clear();
        MdRays.spawn(kind, Vec3.ZERO, new Vec3(0, 0, 15), BIRTH);
        return MdRays.live().get(0);
    }

    @Test
    void lePetitRayonEstCeluiDeLoriginal() {
        // Les nombres de SmallMdRayRender : un coeur de 3 cm, une gaine de 4,5, une lueur de
        // 0,3 bloc a la moitie d'opacite, et une vie de quatorze ticks.
        assertEquals("mdray_small", MdRayKind.SMALL.name());
        assertEquals(14, MdRayKind.SMALL.lifeTicks());
        assertEquals(700, MdRayKind.SMALL.lifeMs(), "cinquante millisecondes par tick");

        assertEquals(0.03, MdRayKind.SMALL.innerRadius(), 1e-6);
        assertEquals(216, MdRayKind.SMALL.inner().r());
        assertEquals(248, MdRayKind.SMALL.inner().g());
        assertEquals(216, MdRayKind.SMALL.inner().b());
        assertEquals(230, MdRayKind.SMALL.inner().a());

        assertEquals(0.045, MdRayKind.SMALL.outerRadius(), 1e-6);
        assertEquals(106, MdRayKind.SMALL.outer().r());
        assertEquals(242, MdRayKind.SMALL.outer().g());
        assertEquals(106, MdRayKind.SMALL.outer().b());
        assertEquals(50, MdRayKind.SMALL.outer().a(), "la gaine est presque transparente");

        assertEquals(0.3, MdRayKind.SMALL.glowWidth(), 1e-6);
        assertEquals(0.5, MdRayKind.SMALL.glowAlpha(), 1e-6);
        assertEquals(1.0, MdRayKind.SMALL.sparkRate(), 1e-6, "une etincelle par tick");
        assertEquals("md.ray_small", MdRayKind.SMALL.sound(), "le son qu'il joue en nassant");
        assertEquals(0.8f, MdRayKind.SMALL.soundVolume(), 1e-6);

        assertEquals(MdRayKind.SMALL, MdRayKind.byName("mdray_small"));
        assertEquals(MdRayKind.SMALL, MdRayKind.byName("n'importe quoi"),
                "un nom inconnu ne doit pas laisser le rayon sans genre");
    }

    @Test
    void leRayonPousseSurLesDeuxPremiersDixiemes() {
        MdRays.LiveRay ray = rayon(MdRayKind.SMALL);

        assertEquals(0.0, ray.drawnLength(BIRTH), 1e-9, "il nait d'un point");
        assertEquals(7.5, ray.drawnLength(BIRTH + 100), 1e-9, "a moitie au bout de 100 ms");
        assertEquals(15.0, ray.drawnLength(BIRTH + 200), 1e-9, "et entier a 200 ms");
        assertEquals(15.0, ray.drawnLength(BIRTH + 690), 1e-9, "sans repousser ensuite");
    }

    @Test
    void lOpaciteTientPuisSEffaceSurLesQuatreDerniers() {
        MdRays.LiveRay ray = rayon(MdRayKind.SMALL);

        assertEquals(1f, ray.alpha(BIRTH), 1e-6);
        assertEquals(1f, ray.alpha(BIRTH + 300), 1e-6, "la chute commence a 300 ms");
        assertEquals(0.5f, ray.alpha(BIRTH + 500), 1e-6, "a moitie a 500 ms");
        assertEquals(0f, ray.alpha(BIRTH + 700), 1e-6, "eteint a 700 ms");
    }

    @Test
    void laLargeurSEffondreSurLesCinqDerniers() {
        MdRays.LiveRay ray = rayon(MdRayKind.SMALL);

        assertEquals(1.0, ray.widthFactor(BIRTH + 100), 1e-9);
        assertEquals(1.0, ray.widthFactor(BIRTH + 200), 1e-9, "la chute commence a 200 ms");
        assertEquals(0.5, ray.widthFactor(BIRTH + 450), 1e-9, "a moitie a 450 ms");
        assertEquals(0.0, ray.widthFactor(BIRTH + 700), 1e-9);
    }

    @Test
    void unRayonMeurtALaFinDeSaVie() {
        MdRays.LiveRay ray = rayon(MdRayKind.SMALL);

        assertFalse(ray.dead(BIRTH + 699));
        assertTrue(ray.dead(BIRTH + 700));

        MdRays.tick(BIRTH + 699);
        assertEquals(1, MdRays.live().size(), "il vit jusqu'au bout");
        MdRays.tick(BIRTH + 700);
        assertEquals(0, MdRays.live().size(), "et s'en va apres");
    }

    @Test
    void laLueurTrembleEntreNeufDixiemesEtUn() {
        MdRays.LiveRay ray = rayon(MdRayKind.SMALL);

        assertEquals(0.9, ray.glowAlpha(BIRTH + 100), 1e-9,
                "sans tremblement, la lueur vaut 0,9 fois l'opacite du rayon");
    }

    @Test
    @DisplayName("seuls les rayons nes sur le tireur se recollent a sa main")
    void seulsCeuxNesSurLeTireurSeRecollentALaMain() {
        // Le `viewOptimize` de l'original — `EntityRayBase.viewOptimize`, vrai par defaut —
        // n'etait eteint que sur les trois rayons nes sur une BILLE : ceux des deux bombes
        // (`ElectronBomb` et `SBNetDelegate`) et la salve (`EntityMdRayBarrage`). Le pre-rayon de
        // la salve, lui, part des yeux du tireur et se dessine sur sa main, comme l'eclair de
        // l'electromaster. C'est ce que le joueur a demande, et c'est ce que `MdRayView` lit.
        assertTrue(MdRayKind.BARRAGE_PRE_HIT.viewOptimize(),
                "le pre-rayon se recolle a la main de son tireur");
        assertTrue(MdRayKind.BARRAGE_PRE_MISS.viewOptimize(), "le meme, quand il n'a rien trouve");

        assertFalse(MdRayKind.SMALL.viewOptimize(),
                "le rayon d'une bille ne se recolle a aucune main : il nait sur la bille");
        assertFalse(MdRayKind.BARRAGE.viewOptimize(), "la salve aussi part de la bille");
    }
}
