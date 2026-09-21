package cn.academy.ability;

import cn.academy.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Verifie que {@code general.damageScale} est bien applique aux degats des
 * competences (portage de {@code ac.ability.calc_global.damage_scale}).
 *
 * En 1.12.2 ce reglage etait lu dans un fichier HOCON ; ici il vient de la config
 * Forge, mais la fonction {@link Skill#scaled} doit rester le seul point d'entree
 * pour que le reglage reste effectif partout.
 */
class SkillDamageScaleTest {

    private static final class Attacker extends Skill {
        Attacker() { super("test_attacker"); }

        /** Expose le calcul protege pour le test. */
        float damageFor(float base) {
            return scaled(base);
        }
    }

    private final double original = Config.damageScale;

    @AfterEach
    void restoreScale() {
        Config.damageScale = original;
    }

    @Test
    @DisplayName("a l'echelle 1.0 les degats sont inchanges")
    void neutralScale() {
        Config.damageScale = 1.0d;
        assertEquals(20f, new Attacker().damageFor(20f), 1.0e-4f);
    }

    @Test
    @DisplayName("l'echelle multiplie les degats de base")
    void scaleMultiplies() {
        Config.damageScale = 2.5d;
        assertEquals(50f, new Attacker().damageFor(20f), 1.0e-4f);
    }

    @Test
    @DisplayName("une echelle a 0 annule les degats")
    void zeroScaleDisablesDamage() {
        Config.damageScale = 0.0d;
        assertEquals(0f, new Attacker().damageFor(20f), 1.0e-4f);
    }

    @Test
    @DisplayName("une echelle fractionnaire reduit les degats")
    void fractionalScaleReduces() {
        Config.damageScale = 0.25d;
        assertEquals(5f, new Attacker().damageFor(20f), 1.0e-4f);
    }

    @Test
    @DisplayName("changer l'echelle change bien le resultat")
    void scaleIsReadAtCallTime() {
        Config.damageScale = 1.0d;
        float base = new Attacker().damageFor(12f);

        Config.damageScale = 3.0d;
        float scaled = new Attacker().damageFor(12f);

        assertNotEquals(base, scaled, "l'echelle doit etre relue a chaque appel");
        assertEquals(36f, scaled, 1.0e-4f);
    }
}
