package cn.academy.ability.client.md;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L'anneau de plasma du missile electronique, relu sans Minecraft.
 *
 * <p>C'est la seule animation de contact du missile, et elle vient toute entiere de l'original :
 * une a deux etincelles par tick, sur un anneau de 0,5 a 1 bloc autour du corps, a une hauteur
 * prise entre la poitrine et 1,2 bloc plus bas, et qui montent. Ces nombres ne se voient qu'en
 * jeu ; ici, ils se figent.
 */
class MdMissileSprayTest {

    @Test
    @DisplayName("une a deux etincelles par tick, jamais zero")
    void lAnneauNeSArreteJamais() {
        // L'original prenait rangei(1, 3), borne haute exclue : c'est une a deux etincelles, et
        // pas zero comme la fumee de la radiation, dont le tirage commence a zero. Un anneau qui
        // disparaitrait un tick sur trois ne serait plus un anneau.
        Random random = new Random(1);
        for (int i = 0; i < 200; i++) {
            int count = MdMissileSpray.perTick(random);
            assertTrue(count >= 1 && count <= 2, "tirage hors bornes : " + count);
        }
        assertEquals(2, MdMissileSpray.COUNT_BOUND - 1, "la borne est exclue, comme l'original");
    }

    @Test
    @DisplayName("l'anneau fait de 0,5 a 1 bloc autour du corps")
    void lAnneauEntoureLeCorps() {
        // Le centre est a 1,6 bloc au-dessus des pieds — le getHeightFix de l'original — et la
        // hauteur tiree redescend de 1,2 au plus : les etincelles vont donc de la poitrine aux
        // hanches, jamais sous les pieds ni au-dessus de la tete.
        Random random = new Random(2);
        Vec3 feet = new Vec3(10, 64, 10);

        for (int i = 0; i < 200; i++) {
            Vec3 pos = MdMissileSpray.ring(feet, random);
            double radius = Math.hypot(pos.x - feet.x, pos.z - feet.z);
            assertTrue(radius >= MdMissileSpray.RING_MIN - 1e-9
                            && radius <= MdMissileSpray.RING_MAX + 1e-9,
                    "rayon hors de l'anneau : " + radius);

            double height = pos.y - feet.y;
            assertTrue(height >= MdMissileSpray.HEIGHT_FIX + MdMissileSpray.DROP_MIN - 1e-9
                            && height <= MdMissileSpray.HEIGHT_FIX + MdMissileSpray.DROP_MAX + 1e-9,
                    "hauteur hors du corps : " + height);
            assertTrue(height > 0, "et jamais sous les pieds : " + height);
        }
    }

    @Test
    @DisplayName("et elle monte")
    void lAnneauMonte() {
        // C'est ce qui distingue le missile de la fumee de la radiation, qui derive dans
        // n'importe quel sens : ici le plasma s'eleve, de un a cinq centimetres par tick.
        Random random = new Random(3);
        for (int i = 0; i < 200; i++) {
            Vec3 drift = MdMissileSpray.drift(random);
            assertTrue(drift.y >= MdMissileSpray.RISE_MIN - 1e-9
                            && drift.y <= MdMissileSpray.RISE_MAX + 1e-9,
                    "montee hors bornes : " + drift.y);
            assertTrue(Math.abs(drift.x) <= MdMissileSpray.DRIFT_XZ + 1e-9
                            && Math.abs(drift.z) <= MdMissileSpray.DRIFT_XZ + 1e-9,
                    "derive trop large : " + drift);
        }
    }
}
