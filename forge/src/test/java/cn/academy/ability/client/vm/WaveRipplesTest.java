package cn.academy.ability.client.vm;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les ondulations d'ecran des deux veilles de vecmanip.
 *
 * <p>Ce qui se relit sans Minecraft : la courbe d'opacite d'une ondulation, sa croissance, la
 * naissance d'une seule ondulation par image, et les trois nombres de chaque veille. Une faute ici
 * se verrait a l'ecran sous la forme d'un ecran couvert de ronds d'un coup, ou d'un rond qui ne
 * part jamais.
 */
class WaveRipplesTest {

    private static final WaveRipples.Settings DEVIATION = new WaveRipples.Settings(0.2, 100, 1.4);

    /** Une ondulation a un moment donne de sa vie : son age vaut la fraction demandee. */
    private static WaveRipples.Ripple ripple(double progress, double life) {
        return new WaveRipples.Ripple(0, 0, life, 100, progress * life);
    }

    @Test
    @DisplayName("l'opacite monte, se tient, puis retombe")
    void lOpaciteSuitSaCourbe() {
        assertEquals(0, ripple(0, 2).alpha(), 1e-9, "une ondulation qui nait ne se voit pas");
        assertEquals(0.5, ripple(0.1, 2).alpha(), 1e-9,
                "elle s'allume en un cinquieme de sa vie");
        assertEquals(1, ripple(0.3, 2).alpha(), 1e-9, "et se tient");
        assertEquals(1, ripple(0.4, 2).alpha(), 1e-9, "jusqu'a la moitie");
        assertEquals(0.5, ripple(0.75, 2).alpha(), 1e-9, "puis retombe");
        assertEquals(0, ripple(2, 2).alpha(), 1e-9, "jusqu'a s'eteindre");
    }

    @Test
    @DisplayName("elle grandit de vingt pixels par seconde")
    void elleGranditAvecLeTemps() {
        assertEquals(100, ripple(0, 2).drawSize(), 1e-9, "elle part de sa taille");
        assertEquals(140, ripple(1, 2).drawSize(), 1e-9,
                "et gagne quarante pixels en deux secondes");
    }

    @Test
    @DisplayName("une image ne fait naitre qu'une ondulation")
    void uneImageNeFaitNaitreQuUne() {
        List<WaveRipples.Ripple> ripples = new ArrayList<>();
        RandomSource random = RandomSource.create(1);

        // Un delta enorme, pour que le tirage soit sur a coup sur : c'est le cas ou l'original
        // pouvait en semer plusieurs d'un coup, d'ou la borne du port. Chaque image emporte celle
        // de la precedente, qui a vecu plus que son age — le compte ne monte donc jamais.
        for (int i = 0; i < 20; i++) {
            WaveRipples.advance(ripples, 10, DEVIATION, 1920, 1080, random);
            assertTrue(ripples.size() <= 1, "au plus une par image : " + ripples.size());
        }
        assertEquals(1, ripples.size(), "et elle en seme bien une");
    }

    @Test
    @DisplayName("elles naissent dans l'ecran, et a sa taille pres")
    void ellesNaissentDansLEcran() {
        List<WaveRipples.Ripple> ripples = new ArrayList<>();
        RandomSource random = RandomSource.create(2);

        for (int i = 0; i < 200; i++) {
            WaveRipples.advance(ripples, 10, DEVIATION, 1920, 1080, random);
        }

        for (WaveRipples.Ripple ripple : ripples) {
            assertTrue(ripple.x() >= 0 && ripple.x() < 1920, "en large : " + ripple.x());
            assertTrue(ripple.y() >= 0 && ripple.y() < 1080, "en hauteur : " + ripple.y());
            assertTrue(ripple.life() >= WaveRipples.LIFE_MIN && ripple.life() <= WaveRipples.LIFE_MAX,
                    "et sa vie est celle des deux tirages : " + ripple.life());
            assertTrue(ripple.size() >= 100 * 0.8 && ripple.size() <= 100 * 1.2,
                    "comme sa taille : " + ripple.size());
        }
    }

    @Test
    @DisplayName("une onde morte s'en va")
    void lesMortesSEffacent() {
        List<WaveRipples.Ripple> ripples = new ArrayList<>();
        RandomSource random = RandomSource.create(3);
        WaveRipples.advance(ripples, 10, DEVIATION, 800, 600, random);
        assertFalse(ripples.isEmpty(), "elle est nee");

        WaveRipples.advance(ripples, WaveRipples.LIFE_MAX + 0.01,
                new WaveRipples.Settings(0.2, 100, 0), 800, 600, RandomSource.create(4));
        assertTrue(ripples.isEmpty(), "et elle s'en va au bout de sa vie");
    }

    @Test
    @DisplayName("un delta nul ne fait rien naitre")
    void unDeltaNulNeFaitRienNaitre() {
        List<WaveRipples.Ripple> ripples = new ArrayList<>();

        WaveRipples.advance(ripples, 0, DEVIATION, 800, 600, RandomSource.create(5));

        assertTrue(ripples.isEmpty(), "la premiere image d'une veille ne seme rien");
    }

    @Test
    @DisplayName("les ronds se dessinent au quart de l'ecran, et moitie moins grands")
    void lesRondsSeDessinentAuQuartDeLEcran() {
        // L'original avait oublie un facteur deux dans son nuanceur : ses ronds tenaient dans la
        // moitie centrale de l'ecran, et moitie moins grands que leur taille annoncee. Le joueur
        // l'a confirme en trouvant ceux du port « trop grands ».
        List<WaveRipples.Ripple> ripples = new ArrayList<>();
        RandomSource random = RandomSource.create(6);
        WaveRipples.Settings dense = new WaveRipples.Settings(0.2, 100, 20);

        for (int i = 0; i < 60; i++) {
            WaveRipples.advance(ripples, 0.05, dense, 1920, 1080, random);
        }
        assertTrue(ripples.size() > 5, "il y a de quoi regarder : " + ripples.size());

        for (WaveRipples.Ripple ripple : ripples) {
            double x = WaveRipples.drawnX(ripple, 1920);
            double y = WaveRipples.drawnY(ripple, 1080);

            assertTrue(x >= 480 && x <= 1440, "en large, entre le quart et les trois quarts : " + x);
            assertTrue(y >= 270 && y <= 810, "en hauteur aussi : " + y);
            assertEquals(ripple.drawSize() / 2, WaveRipples.drawnSize(ripple), 1e-9,
                    "et moitie moins grand que sa taille");
        }

        assertEquals(480, WaveRipples.drawnX(ripple(0, 2), 1920), 1e-9,
                "un rond ne a zero tombe au quart de l'ecran");
        assertEquals(1440, WaveRipples.drawnX(new WaveRipples.Ripple(1920, 0, 2, 100, 0), 1920),
                1e-9, "et un rond ne a la largeur, aux trois quarts");
    }

    /**
     * Le centre d'un rond ne bouge pas : seule sa taille grandit.
     *
     * <p>C'est la faute que le joueur a vue : « le centre du rond n'est jamais au meme endroit, ce
     * qui fait que le cercle vibre et ce n'est pas normal ». Un blit de HUD arrondit la position
     * <b>et</b> la taille, et les deux arrondis ne tombent pas ensemble — le carre se posait donc
     * de travers des que la taille changeait, d'un pixel, plusieurs fois par seconde. Ici rien
     * n'est arrondi, et le milieu du carre reste le centre a tout age.
     */
    @Test
    @DisplayName("le centre d'un rond ne bouge pas, seule sa taille grandit")
    void leCentreDUnRondNeBougePas() {
        for (double age = 0; age <= 2; age += 0.05) {
            WaveRipples.Ripple rond = new WaveRipples.Ripple(700, 400, 2, 100, age);
            double size = WaveRipples.drawnSize(rond);

            assertEquals(WaveRipples.drawnX(rond, 1920),
                    WaveRipples.cornerX(rond, 1920) + size / 2, 1e-9,
                    "le milieu du carre est le centre, a l'age " + age);
            assertEquals(WaveRipples.drawnY(rond, 1080),
                    WaveRipples.cornerY(rond, 1080) + size / 2, 1e-9,
                    "en hauteur aussi, a l'age " + age);
        }

        // Et le coin n'est pas entier : un demi-pixel compte, c'est tout le propos. Arrondir ici
        // ramenerait la vibration.
        WaveRipples.Ripple demi = new WaveRipples.Ripple(700, 400, 2, 100, 0.5);
        assertEquals(802.5, WaveRipples.cornerX(demi, 1920), 1e-9, "le coin tombe a 802,5");
        assertEquals(442.5, WaveRipples.cornerY(demi, 1080), 1e-9, "et en hauteur a 442,5");
        assertEquals(55, WaveRipples.drawnSize(demi), 1e-9, "pour un carre de 55 pixels");
    }

    @Test
    @DisplayName("les deux veilles ont leurs trois nombres")
    void lesDeuxVeillesOntLeursNombres() {
        WaveRipples.Settings deviation = WaveRipples.forSkill("vec_deviation");
        WaveRipples.Settings reflection = WaveRipples.forSkill("vec_reflection");

        assertNotNull(deviation, "la deviation ondule");
        assertEquals(0.2, deviation.alpha(), 1e-9, "discretement");
        assertEquals(100, deviation.size(), 1e-9, "avec des ronds de cent pixels");
        assertEquals(1.4, deviation.intensity(), 1e-9, "et un peu plus d'une naissance par seconde");

        assertNotNull(reflection, "le renvoi ondule aussi, et plus fort");
        assertEquals(0.4, reflection.alpha(), 1e-9, "deux fois plus visible");
        assertEquals(110, reflection.size(), 1e-9, "un peu plus large");
        assertEquals(1.6, reflection.intensity(), 1e-9, "et un peu plus dense");

        assertNull(WaveRipples.forSkill("storm_wing"), "les autres competences n'ondulent pas");
        assertNull(WaveRipples.forSkill(null), "et rien du tout n'ondule non plus");
    }
}
