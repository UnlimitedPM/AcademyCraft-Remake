package cn.academy.ability.electromaster;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Les nombres de la charge du renfort.
 *
 * <p>Ce que la charge rend — sa probabilite, son niveau, sa duree — ne se voit ni a la
 * compilation ni au test en jeu : c'est une courbe, et une courbe se relit. Les valeurs sont
 * celles de l'original, reprises de son {@code getProbability} et de son {@code getBuffTime}.
 */
class BodyIntensifyChargeTest {

    @Test
    @DisplayName("la probabilite part de zero au minimum, et passe 1 a 28 ticks")
    void laProbabilitePartDeZero() {
        assertEquals(0.0, BodyIntensifySkill.probability(10), 1e-9, "rien avant le minimum");
        assertEquals(0.5, BodyIntensifySkill.probability(19), 1e-9);
        assertEquals(1.0, BodyIntensifySkill.probability(28), 1e-9);
        assertEquals(30.0 / 18.0, BodyIntensifySkill.probability(40), 1e-9,
                "au plafond elle vaut 1,67 : de quoi tirer deux effets, jamais trois");
    }

    @Test
    @DisplayName("le niveau est la partie entiere, et l'entretien tombe de 20 a 15 CP")
    void leNiveauEtLEntretien() {
        assertEquals(0, BodyIntensifySkill.level(27), "un cran avant 28 ne donne rien");
        assertEquals(1, BodyIntensifySkill.level(28));
        assertEquals(20f, BodyIntensifySkill.cpPerTick(0.0), 1e-4);
        assertEquals(15f, BodyIntensifySkill.cpPerTick(1.0), 1e-4);
    }

    @Test
    @DisplayName("la duree d'un effet suit le temps tenu et le facteur")
    void laDureeSuitLeTempsTenu() {
        assertEquals(60, BodyIntensifySkill.buffTime(40, 1.0, 1.5f), "1,5 fois quarante");
        assertEquals(200, BodyIntensifySkill.buffTime(40, 2.0, 2.5f),
                "2,5 fois quarante, au plus");
        assertEquals(1.5f, BodyIntensifySkill.timeFactor(0.0), 1e-4);
        assertEquals(2.5f, BodyIntensifySkill.timeFactor(1.0), 1e-4);
    }

    @Test
    @DisplayName("la charge dure de 10 a 40 ticks, et ne coute rien a l'ouverture")
    void lesBornesDeLaCharge() {
        BodyIntensifySkill skill = new BodyIntensifySkill();

        assertEquals(BodyIntensifySkill.MIN_TIME, skill.getMinChargeTicks(null));
        assertEquals(BodyIntensifySkill.MAX_TIME, skill.getMaxChargeTicks(null));
        assertEquals(0f, skill.getCpCost(), 1e-4,
                "la charge se paie tick par tick, pas d'un coup a l'ouverture");
    }
}
