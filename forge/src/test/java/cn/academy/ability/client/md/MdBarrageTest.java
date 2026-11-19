package cn.academy.ability.client.md;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La gerbe de la salve de rayons : ses tirages, et sa forme.
 *
 * <p>Ce sont les nombres d'{@code EntityMdRayBarrage}, et deux d'entre eux meritent un test : la
 * gerbe <b>n'est pas ronde</b> — 50 a 60 degres en lacet, la moitie en tangage — et ses traits
 * sont tires au hasard, donc rien ne se verifie a l'oeil sur une seule salve. Ce qui se fige, ce
 * sont les bornes, et le fait qu'un tirage ne sorte jamais de l'une d'elles.
 */
class MdBarrageTest {

    private static final double EPSILON = 1e-6;

    @Test
    @DisplayName("une gerbe porte de 25 a 30 traits")
    void laGerbePorteVingtCinqATrenteTraits() {
        var random = new Random(4321);
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;

        for (int i = 0; i < 400; i++) {
            int count = MdBarrage.subCount(random);
            lowest = Math.min(lowest, count);
            highest = Math.max(highest, count);
        }

        assertEquals(MdBarrage.SUBS_MIN, lowest, "jamais moins de 25 traits");
        assertTrue(highest < MdBarrage.SUBS_MAX,
                "et jamais 30 : le rangei de l'original exclut sa borne haute, comme le port. "
                        + "Vu : " + highest);
        assertTrue(highest > MdBarrage.SUBS_MIN + 2, "mais la borne est approchee, vu " + highest);
    }

    @Test
    @DisplayName("elle est deux fois plus large que haute")
    void laGerbeNEstPasRonde() {
        var random = new Random(99);
        double widest = 0;
        double highest = 0;

        for (int i = 0; i < 2000; i++) {
            double spread = MdBarrage.spread(random);
            assertTrue(spread >= MdBarrage.SPREAD_MIN && spread < MdBarrage.SPREAD_MAX,
                    "la portee angulaire sort de ses bornes : " + spread);

            double yaw = MdBarrage.yawOffset(spread, random);
            double pitch = MdBarrage.pitchOffset(spread, random);

            assertTrue(Math.abs(yaw) <= spread, "le lacet sort de la portee : " + yaw);
            assertTrue(Math.abs(pitch) <= spread / 2 + EPSILON,
                    "le tangage sort de la moitie de la portee : " + pitch);

            widest = Math.max(widest, Math.abs(yaw));
            highest = Math.max(highest, Math.abs(pitch));
        }

        // L'original se servait de la MEME portee deux fois : entiere en lacet, moitie en
        // tangage. Sur assez de tirages, les deux extremes se separent donc d'un facteur deux.
        assertTrue(widest > highest * 1.5,
                "le lacet doit aller bien plus loin que le tangage : " + widest + " contre "
                        + highest);
    }

    @Test
    @DisplayName("un trait est le regard tourne de ses decalages")
    void leTraitSuitLeRegard() {
        Vec3 look = new Vec3(0, 0, 1);

        // Sans decalage, rien ne bouge : c'est ce qui permet de dire que la gerbe part bien
        // dans l'axe du tireur.
        assertEquals(0.0, MdBarrage.direction(look, 0, 0).distanceTo(look), EPSILON,
                "sans decalage, le trait suit le regard");

        // Et quelle que soit la rotation, la direction reste unitaire : un trait de plus ou de
        // moins ne doit pas devenir plus long. La tolerance est celle du FLOAT — les rotations de
        // 1.20.1 prennent des radians en simple precision — et non celle du double.
        var random = new Random(7);
        for (int i = 0; i < 500; i++) {
            double spread = MdBarrage.spread(random);
            Vec3 direction = MdBarrage.direction(look,
                    MdBarrage.yawOffset(spread, random), MdBarrage.pitchOffset(spread, random));
            assertEquals(1.0, direction.length(), 1e-4,
                    "direction non unitaire : " + direction);
        }

        // Le lacet tourne bien autour de la verticale, et le tangage fait monter ou descendre.
        Vec3 side = MdBarrage.direction(look, 90, 0);
        assertEquals(0.0, side.y, EPSILON, "un lacet pur ne monte pas");
        assertTrue(Math.abs(side.x) > 0.99, "il part bien de cote : " + side);

        Vec3 up = MdBarrage.direction(look, 0, 90);
        assertTrue(up.y > 0.99, "un tangage pur fait monter : " + up);
    }
}
