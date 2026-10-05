package cn.academy.ability.client.vm;

import cn.academy.ability.vecmanip.VecAccelSkill;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La parabole de visee de l'acceleration de vecteur.
 *
 * <p>Tout se relit sans Minecraft : d'ou elle part, comment elle retombe, et de quel poids elle
 * s'efface. C'est un instrument de visee — une faute d'un dixieme de bloc sur le depart, ou une
 * pesanteur trop faible, et le joueur part dans le mur qu'il croyait survoler.
 */
class VecAccelPreviewTest {

    /** Un porteur regardant vers +Z (le sud), a la hauteur des pieds. */
    private static final Vec3 FEET = new Vec3(0.5, 64, 0.5);
    private static final Vec3 LOOK = new Vec3(0, 0, 1);

    /** La verticale de l'ecran, celle de la camera. */
    private static final Vec3 UP = new Vec3(0, 1, 0);

    @Test
    @DisplayName("elle part de la main, comme les eclairs")
    void ellePartDeLaMain() {
        // Les yeux d'un porteur regardant vers +Z, ses pieds 1,62 plus bas.
        Vec3 eyes = FEET.add(0, 1.62, 0);
        Vec3 hand = VecAccelPreview.handFrom(eyes, LOOK, UP);

        // Le decalage des eclairs, chiffre par chiffre : un peu en arriere du depart, un quart de
        // bloc plus bas, et vingt centimetres sur le cote. Le cote lui-meme n'est pas verifie — il
        // vient de la visee, et le signe appartient a ArcView.fixStart, deja teste.
        assertEquals(-0.05, hand.z - eyes.z, 1e-9, "cinq centimetres en arriere du depart");
        assertEquals(-0.25, hand.y - eyes.y, 1e-9, "et un quart de bloc plus bas");
        assertEquals(0.2, Math.abs(hand.x - eyes.x), 1e-9, "vingt centimetres sur le cote");
    }

    @Test
    @DisplayName("un regard vertical ne casse pas le depart")
    void unRegardVerticalNeCasseRien() {
        Vec3 eyes = FEET.add(0, 1.62, 0);
        // Visee et verticale confondues : il n'y a plus de repere d'ecran. Le repere s'en choisit
        // alors un autre — une perpendiculaire stable — et le decalage reste celui des eclairs :
        // un tiers de bloc, pas davantage. C'est le repli de ArcFrame.
        Vec3 hand = VecAccelPreview.handFrom(eyes, UP, UP);

        assertEquals(Math.sqrt(0.05 * 0.05 + 0.25 * 0.25 + 0.2 * 0.2), hand.distanceTo(eyes), 1e-9,
                "un tiers de bloc a peine : " + hand);
    }

    @Test
    @DisplayName("le lancer vise dix degres au-dessus du regard")
    void leLancerViseAuDessusDuRegard() {
        Vec3 speed = VecAccelPreview.initialSpeed(0, 0, 2.1);

        assertEquals(2.1, speed.length(), 1e-6, "a la vitesse de la charge");
        assertTrue(speed.y > 0, "et vers le haut : " + speed.y);
        // Dix degres au-dessus de l'horizontale : sin(10 degres) = 0,1736. C'est le `pitch - 10`
        // de l'original, qui porte le saut plus loin que le regard.
        assertEquals(0.1736 * 2.1, speed.y, 1e-3, "de dix degres, pas plus");
    }

    @Test
    @DisplayName("la trajectoire monte puis retombe")
    void laTrajectoireMontePuisRetombe() {
        List<Vec3> points = VecAccelPreview.points(FEET,
                VecAccelPreview.initialSpeed(0, 0, 2.5));

        assertEquals(VecAccelPreview.STEPS, points.size(), "cent pas, comme l'original");
        assertEquals(FEET, points.get(0), "le premier pas est le depart");

        double highest = points.get(0).y;
        int highestAt = 0;
        for (int i = 1; i < points.size(); i++) {
            if (points.get(i).y > highest) {
                highest = points.get(i).y;
                highestAt = i;
            }
        }
        assertTrue(highestAt > 0 && highestAt < points.size() - 1,
                "elle monte d'abord, puis retombe : sommet au pas " + highestAt);
        assertTrue(points.get(points.size() - 1).y < highest,
                "et le dernier pas est plus bas que le sommet");
        assertTrue(points.get(points.size() - 1).z > points.get(0).z,
                "elle avance vers le regard, elle ne recule pas");
    }

    @Test
    @DisplayName("l'air freine et la pesanteur tire, aux nombres de l'original")
    void lAirFreineEtLaPesanteurTire() {
        assertEquals(0.98, VecAccelPreview.DRAG, 1e-9, "deux centiemes de frein par pas");
        assertEquals(1.9, VecAccelPreview.GRAVITY, 1e-9, "et 1,9 de pesanteur par seconde carree");

        // Sans pesanteur, la meme vitesse initiale irait plus loin : c'est la seule chose qui
        // fait retomber la parabole.
        List<Vec3> points = VecAccelPreview.points(FEET, new Vec3(0, 0, 2.5));
        assertTrue(points.get(points.size() - 1).y < FEET.y,
                "le tir finit sous le depart : " + points.get(points.size() - 1).y);
    }

    @Test
    @DisplayName("le ruban s'efface vers sa fin")
    void leRubanSEffaceVersSaFin() {
        assertEquals(0.7, VecAccelPreview.alpha(0), 1e-9, "il part a sept dixiemes");
        assertTrue(VecAccelPreview.alpha(10) < VecAccelPreview.alpha(9), "et s'efface de pas en pas");
        assertEquals(0, VecAccelPreview.alpha(100), 1e-9, "jusqu'a ne plus rien valoir");
    }

    @Test
    @DisplayName("la vitesse suit la charge, aux nombres du serveur")
    void laVitesseSuitLaCharge() {
        assertEquals(0.974, VecAccelSkill.speedAt(0), 1e-3,
                "un appui bref pousse a 0,97 — deja plus qu'un pas de marche");
        assertEquals(2.104, VecAccelSkill.speedAt(VecAccelSkill.MAX_CHARGE), 1e-3,
                "et une charge pleine a 2,1");
        assertEquals(VecAccelSkill.speedAt(40), VecAccelSkill.speedAt(VecAccelSkill.MAX_CHARGE),
                1e-12, "au-dela de la charge maximale, elle ne monte plus");
    }
}
