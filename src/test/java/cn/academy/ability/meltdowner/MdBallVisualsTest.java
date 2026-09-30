package cn.academy.ability.meltdowner;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La bille de plasma : ses nombres, son ecart, et sa courbe d'opacite.
 *
 * <p>C'est le premier objet du meltdowner, et trois competences s'en servent : ses nombres
 * sont donc figes ici, hors du rendu, pour qu'un reglage ne les fasse pas deriver.
 *
 * <p>Sa vie se compte en <b>secondes</b> et non en ticks : l'horloge de l'original etait une
 * horloge a la seconde, et ses trois courbes — apparition, palier, gonflement — se lisent
 * donc au dixieme de seconde. Une bille de vingt ticks vit une seconde, un point c'est tout.
 */
class MdBallVisualsTest {

    @Test
    void laBilleVitUneSecondeEtSaCourteVersionUnQuart() {
        assertEquals(20, MdBallVisuals.LIFE_TICKS);
        assertEquals(5, MdBallVisuals.LIFE_IMPROVED_TICKS,
                "un quart de seconde, passe 80 % d'experience");
        assertEquals(0.8f, MdBallVisuals.IMPROVED_EXP, 1e-6);
        assertEquals(1.0, MdBallVisuals.lifeSeconds(MdBallVisuals.LIFE_TICKS), 1e-9);
        assertEquals(2, MdBallVisuals.SHOT_DELAY, "elle tire deux ticks avant de mourir");
    }

    @Test
    void lOpaciteMonteSeTientPuisGonfleAvantLeTir() {
        int life = MdBallVisuals.LIFE_TICKS;

        assertEquals(0f, MdBallVisuals.alpha(0, life), 1e-6, "elle nait invisible");
        assertEquals(0.2f, MdBallVisuals.alpha(0.1, life), 1e-6, "un tiers de son opacite a 100 ms");
        assertEquals(0.6f, MdBallVisuals.alpha(0.3, life), 1e-6, "pleine a 300 ms");

        assertEquals(0.6f, MdBallVisuals.alpha(0.5, life), 1e-6, "et elle s'y tient");
        assertEquals(0.6f, MdBallVisuals.alpha(0.6, life), 1e-6, "jusqu'a 400 ms de la fin");

        // Les quatre derniers dixiemes sont le tir : l'opacite y monte a un, puis s'efface.
        assertEquals(0.6f, MdBallVisuals.alpha(0.6, life), 1e-6);
        assertTrue(MdBallVisuals.alpha(0.75, life) > 0.6f, "elle gonfle");
        assertEquals(1f, MdBallVisuals.alpha(0.85, life), 1e-6, "jusqu'a un a 150 ms de la fin");
        assertEquals(0f, MdBallVisuals.alpha(1.0, life), 1e-6, "et s'efface a la fin");
    }

    @Test
    void laBilleSeTientDansUnAnneauAutourDuPorteur() {
        // Le rayon de l'anneau, et sa hauteur : c'est ce qui la met a cote du corps plutot
        // que devant les yeux.
        assertEquals(0.8, MdBallVisuals.RANGE_FROM, 1e-9);
        assertEquals(1.3, MdBallVisuals.RANGE_TO, 1e-9);
        assertEquals(-1.2, MdBallVisuals.SUB_Y_MIN, 1e-9);
        assertEquals(0.2, MdBallVisuals.SUB_Y_MAX, 1e-9);

        RandomSource random = RandomSource.create(11L);
        for (int i = 0; i < 200; i++) {
            Vec3 sub = MdBallVisuals.subOffset(0f, random);
            double radius = Math.sqrt(sub.x * sub.x + sub.z * sub.z);

            assertTrue(radius >= MdBallVisuals.RANGE_FROM - 1e-9
                            && radius <= MdBallVisuals.RANGE_TO + 1e-9,
                    "l'ecart tient dans l'anneau : " + radius);
            assertTrue(sub.y >= MdBallVisuals.SUB_Y_MIN - 1e-9
                            && sub.y <= MdBallVisuals.SUB_Y_MAX + 1e-9,
                    "et dans sa hauteur : " + sub.y);
        }
    }

    @Test
    void lEcartSuitLeLacetDuPorteur() {
        // Le lacet ouvre l'anneau de plus ou moins 45 % d'un demi-tour : a 0 degre l'ecart
        // moyen se tient donc devant, vers les Z positifs.
        double forward = 0;
        double backward = 0;
        RandomSource random = RandomSource.create(3L);

        for (int i = 0; i < 400; i++) {
            forward += MdBallVisuals.subOffset(0f, random).z;
            backward += MdBallVisuals.subOffset(180f, random).z;
        }

        assertTrue(forward > 0, "vers l'avant, l'ecart est positif : " + forward);
        assertTrue(backward < 0, "et vers l'arriere, negatif : " + backward);
    }

    @Test
    void leBalancementFaitQuelquesCentimetres() {
        assertEquals(0.03, MdBallVisuals.WOBBLE_XZ, 1e-9);
        assertEquals(0.04, MdBallVisuals.WOBBLE_Y, 1e-9);

        for (double age = 0; age < 3; age += 0.05) {
            assertTrue(Math.abs(MdBallVisuals.wobbleX(age)) <= MdBallVisuals.WOBBLE_XZ + 1e-9);
            assertTrue(Math.abs(MdBallVisuals.wobbleZ(age)) <= MdBallVisuals.WOBBLE_XZ + 1e-9);
            assertTrue(Math.abs(MdBallVisuals.wobbleY(age)) <= MdBallVisuals.WOBBLE_Y + 1e-9);
        }
    }

    @Test
    void saTailleNeChangePas() {
        // L'original avait une courbe de gonflement, mais elle comparait un age en secondes a
        // une duree en millisecondes : elle ne s'allumait jamais. La bille garde sa taille.
        assertEquals(1f, MdBallVisuals.size(), 1e-6);
        assertEquals(0.7, MdBallVisuals.GLOW_SIZE, 1e-9);
        assertEquals(0.5, MdBallVisuals.CORE_SIZE, 1e-9, "le coeur est plus petit que le halo");
    }

    @Test
    void elleSeDessineAEtuteurDEux() {
        // Sa position est celle des pieds, mais l'original la dessinait 1,6 bloc plus haut :
        // c'est ce qui la met a hauteur d'yeux, la ou son rayon prend sa source. Sans ce
        // decalage elle se dessinait dans les jambes — jusqu'a 1,2 bloc sous les pieds, ce que
        // le joueur a vu — pendant que le rayon partait un bloc et demi plus haut.
        assertEquals(1.6, MdBallVisuals.RENDER_HEIGHT, 1e-9);
        assertTrue(MdBallVisuals.RENDER_HEIGHT + MdBallVisuals.SUB_Y_MIN > 0,
                "meme la bille la plus basse se dessine au-dessus des pieds : "
                        + (MdBallVisuals.RENDER_HEIGHT + MdBallVisuals.SUB_Y_MIN));
        assertTrue(MdBallVisuals.RENDER_HEIGHT + MdBallVisuals.SUB_Y_MAX < 2,
                "et la plus haute reste sous le sommet du corps");
    }
}
