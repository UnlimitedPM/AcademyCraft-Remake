package cn.academy.ability.client.tp;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La geometrie de la trainee du depose au loin.
 *
 * <p>C'est la partie qu'aucune porte ne regarde : le serveur n'envoie que les deux bouts, et c'est
 * le client qui seme. Une faute de pas ne se verrait qu'a l'ecran, sous la forme d'une trainee qui
 * s'arrete avant sa cible ou qui la depasse — c'est-a-dire d'un geste dont on ne comprend pas ou il
 * est alle.
 */
class ShiftTrailTest {

    /** Un depart et une arrivee de reference : un regard franc vers le sud, a dix blocs. */
    private static final Vec3 FEET = new Vec3(0.5, 64.5, 0.5);
    private static final BlockPos CELL = new BlockPos(0, 64, 10);

    private static List<ShiftTrail.Spark> semis(long seed) {
        return ShiftTrail.sparks(FEET, CELL, RandomSource.create(seed));
    }

    @Test
    @DisplayName("le depart se prend un demi-bloc sous les pieds")
    void leDepartEstSousLesPieds() {
        assertEquals(new Vec3(0.5, 64.0, 0.5), ShiftTrail.start(FEET),
                "c'est le posY - 0.5 de l'original, garde tel quel");
    }

    @Test
    @DisplayName("les grains suivent la ligne, du premier pas jusqu'a la case")
    void lesGrainsSuiventLaLigne() {
        Vec3 from = ShiftTrail.start(FEET);
        Vec3 direction = ShiftTrail.end(CELL).subtract(from).normalize();
        double distance = ShiftTrail.end(CELL).subtract(from).length();
        List<ShiftTrail.Spark> sparks = semis(1L);

        assertFalse(sparks.isEmpty(), "un geste de dix blocs seme quelque chose");

        for (ShiftTrail.Spark spark : sparks) {
            Vec3 offset = spark.pos().subtract(from);
            double travelled = offset.length();

            assertTrue(travelled >= ShiftTrail.FIRST_STEP - 1e-9,
                    "aucun grain avant le premier pas : " + travelled);
            assertTrue(travelled <= distance + 1e-9,
                    "et aucun au-dela de la case visee : " + travelled + " pour " + distance);
            assertEquals(1.0, offset.normalize().dot(direction), 1e-9,
                    "chaque grain est SUR la ligne");
        }

        assertEquals(ShiftTrail.FIRST_STEP, sparks.get(0).pos().subtract(from).length(), 1e-9,
                "et le premier se pose a un bloc, comme chez l'original");
    }

    @Test
    @DisplayName("les pas sont ceux de l'original : entre six dixiemes et un bloc")
    void lesPasSontCeuxDeLOriginal() {
        List<ShiftTrail.Spark> sparks = semis(2L);

        for (int i = 1; i < sparks.size(); i++) {
            double step = sparks.get(i).pos().distanceTo(sparks.get(i - 1).pos());

            assertTrue(step >= ShiftTrail.STEP_MIN - 1e-9 && step <= ShiftTrail.STEP_MAX + 1e-9,
                    "un pas irregulier, entre 0,6 et 1 : " + step);
        }
    }

    @Test
    @DisplayName("la trainee s'arrete au bout, sans le depasser")
    void laTraineeSArreteAuBout() {
        Vec3 from = ShiftTrail.start(FEET);
        double distance = ShiftTrail.end(CELL).subtract(from).length();
        List<ShiftTrail.Spark> sparks = semis(3L);

        double last = sparks.get(sparks.size() - 1).pos().subtract(from).length();

        assertTrue(distance - last < ShiftTrail.STEP_MAX,
                "le dernier grain est a moins d'un pas de la cible : " + (distance - last));
        assertTrue(last <= distance, "et il ne la depasse pas");
    }

    @Test
    @DisplayName("le nombre de grains suit la distance")
    void leNombreSuitLaDistance() {
        BlockPos far = new BlockPos(0, 64, 30);
        Vec3 from = ShiftTrail.start(FEET);
        double distance = ShiftTrail.end(far).subtract(from).length();
        int count = ShiftTrail.sparks(FEET, far, RandomSource.create(4L)).size();

        // Un premier a un bloc, puis des pas d'au moins 0,6 et d'au plus 1 : le compte est donc
        // borne par la distance, et c'est ce qui fait que la trainee ne s'etire pas toute seule.
        int most = (int) Math.floor((distance - ShiftTrail.FIRST_STEP) / ShiftTrail.STEP_MIN) + 1;
        int least = (int) Math.ceil((distance - ShiftTrail.FIRST_STEP) / ShiftTrail.STEP_MAX) + 1;

        assertTrue(count >= least && count <= most,
                "entre " + least + " et " + most + " grains pour " + distance + " blocs, pas "
                        + count);
        assertTrue(count > 30, "et trente blocs en portent plus de trente : " + count);
    }

    @Test
    @DisplayName("la derive est celle de l'original, plus haute que basse")
    void laDeriveEstCelleDeLOriginal() {
        RandomSource random = RandomSource.create(5L);

        for (int i = 0; i < 200; i++) {
            Vec3 drift = ShiftTrail.drift(random);

            assertTrue(Math.abs(drift.x) <= ShiftTrail.DRIFT_XZ + 1e-9,
                    "en large, une etincelle ne s'ecarte pas de cinq centimetres : " + drift.x);
            assertTrue(Math.abs(drift.z) <= ShiftTrail.DRIFT_XZ + 1e-9, "ni en profondeur");
            assertTrue(drift.y >= ShiftTrail.DRIFT_Y_MIN - 1e-9
                            && drift.y <= ShiftTrail.DRIFT_Y_MAX + 1e-9,
                    "et elle monte un peu plus qu'elle ne descend : " + drift.y);
        }
    }

    @Test
    @DisplayName("rien du tout quand le geste vise ses pieds")
    void rienQuandLaCibleEstAtteinte() {
        // Une case dont le centre est a un demi-bloc du depart : l'original n'y semait rien, sa
        // boucle commencant a un bloc.
        assertEquals(List.of(), ShiftTrail.sparks(new Vec3(0.5, 64.5, 0.5), new BlockPos(0, 64, 0),
                RandomSource.create(6L)), "aucun grain sous le premier pas");
    }
}
