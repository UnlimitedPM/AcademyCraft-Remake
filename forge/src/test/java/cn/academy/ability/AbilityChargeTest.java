package cn.academy.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le temps de charge des competences.
 *
 * Portage du compteur que l'original tenait dans son contexte d'activation : la touche
 * reste enfoncee, le compteur avance, et la competence part au relachement avec ce
 * qu'elle a accumule. Trois choix du port valent la peine d'etre figes ici : le
 * plafond, l'independance entre competences, et le fait que le compteur reste lisible
 * apres le relachement — c'est ainsi que la competence dose ensuite ses degats, sa
 * recharge et son gain d'experience.
 */
class AbilityChargeTest {

    private static final class TestCategory extends Category {
        TestCategory(String name) {
            super(name);
        }

        void add(Skill skill) {
            addSkill(skill);
        }
    }

    /** Une competence qui se charge, avec le maximum demande. */
    private static final class ChargingSkill extends Skill {

        private final int max;

        ChargingSkill(String name, int max) {
            super(name, 1);
            this.max = max;
        }

        @Override
        public boolean isChargeable() {
            return true;
        }

        @Override
        public int getMaxChargeTicks(AbilityData data) {
            return max;
        }
    }

    /** Une competence ordinaire, qui part a l'appui. */
    private static final class InstantSkill extends Skill {

        boolean activated;

        InstantSkill(String name) {
            super(name, 1);
        }

        @Override
        public void onActivate(Player player, AbilityData data) {
            activated = true;
        }
    }

    /** Tient une charge pendant {@code ticks} puis la relache, comme un vrai joueur. */
    private static int charge(Skill skill, AbilityData data, int ticks) {
        data.beginCharge(skill);
        for (int i = 0; i < ticks; i++) {
            data.tickCharges();
        }
        data.endCharge(skill);
        return data.getChargeTicks(skill);
    }

    @Test
    void uneChargePartDeZeroEtAvanceDUnTickParTick() {
        ChargingSkill skill = new ChargingSkill("charge", 10);
        AbilityData data = new AbilityData();

        data.beginCharge(skill);
        assertEquals(0, data.getChargeTicks(skill), "une charge ouverte ne compte rien encore");
        assertTrue(data.isCharging(skill));

        data.tickCharges();
        data.tickCharges();
        data.tickCharges();

        assertEquals(3, data.getChargeTicks(skill));
        assertTrue(data.isChargingAnything());
    }

    @Test
    void uneChargeNeDepassePasSonMaximum() {
        ChargingSkill skill = new ChargingSkill("charge", 10);
        AbilityData data = new AbilityData();
        data.beginCharge(skill);

        for (int i = 0; i < 50; i++) {
            data.tickCharges();
        }

        // Tenir la touche indefiniment ne rend pas plus fort qu'au maximum : c'est ce
        // qui borne les degats d'un tir tenu.
        assertEquals(10, data.getChargeTicks(skill));
    }

    @Test
    void leCompteurResteLisibleApresLeRelachement() {
        ChargingSkill skill = new ChargingSkill("charge", 10);
        AbilityData data = new AbilityData();

        assertEquals(5, charge(skill, data, 5));

        assertFalse(data.isCharging(skill), "la touche n'est plus tenue");
        assertFalse(data.isChargingAnything());
        assertEquals(5, data.getChargeTicks(skill),
                "la competence doit pouvoir lire combien de temps elle a ete chargee");
    }

    @Test
    void uneNouvelleChargeRepartDeZero() {
        ChargingSkill skill = new ChargingSkill("charge", 10);
        AbilityData data = new AbilityData();
        charge(skill, data, 8);

        data.beginCharge(skill);
        data.tickCharges();

        assertEquals(1, data.getChargeTicks(skill), "le compteur ne reprend pas ou il en etait");
    }

    @Test
    void annulerUneChargeOublieSonCompteur() {
        ChargingSkill skill = new ChargingSkill("charge", 10);
        AbilityData data = new AbilityData();
        data.beginCharge(skill);
        data.tickCharges();

        data.cancelCharge(skill);

        assertEquals(0, data.getChargeTicks(skill));
        assertFalse(data.isChargingAnything());
    }

    @Test
    void deuxCompetencesSeChargentIndependamment() {
        ChargingSkill slow = new ChargingSkill("slow", 10);
        ChargingSkill fast = new ChargingSkill("fast", 3);
        AbilityData data = new AbilityData();

        data.beginCharge(slow);
        data.beginCharge(fast);
        for (int i = 0; i < 6; i++) {
            data.tickCharges();
        }

        assertEquals(6, data.getChargeTicks(slow));
        assertEquals(3, data.getChargeTicks(fast), "chacune a son propre maximum");

        data.endCharge(slow);
        assertTrue(data.isCharging(fast), "relacher l'une ne touche pas l'autre");
    }

    @Test
    void uneChargeNeSurvitPasAUneSauvegarde() {
        ChargingSkill skill = new ChargingSkill("charge", 10);
        AbilityData data = new AbilityData();
        data.beginCharge(skill);
        data.tickCharges();
        data.tickCharges();

        // Choix inverse de celui des recharges : une recharge qui survit a un
        // rechargement est une attente que le joueur a meritee, une charge qui
        // survivrait serait une touche encore enfoncee dans une partie qu'il n'a plus.
        CompoundTag tag = data.serializeNBT();
        AbilityData reloaded = new AbilityData();
        reloaded.deserializeNBT(tag);

        assertEquals(0, reloaded.getChargeTicks(skill));
        assertFalse(reloaded.isChargingAnything());
    }

    @Test
    void laMortOublieLesCharges() {
        ChargingSkill skill = new ChargingSkill("charge", 10);
        AbilityData data = new AbilityData();
        data.beginCharge(skill);
        data.tickCharges();

        data.clearCharges();

        assertEquals(0, data.getChargeTicks(skill));
        assertFalse(data.isChargingAnything());
    }

    @Test
    void uneCompetenceOrdinaireNeSeChargePas() {
        TestCategory category = new TestCategory("test");
        InstantSkill skill = new InstantSkill("instant");
        category.add(skill);
        AbilityData data = new AbilityData();

        assertFalse(skill.isChargeable());
        assertEquals(0, skill.getMaxChargeTicks(data));
        assertEquals(0, skill.getMinChargeTicks(data));
        assertFalse(skill.isHeld(), "et elle ne se tient pas non plus");

        // Et son activation chargee par defaut reste l'activation ordinaire : les huit
        // competences deja portees n'ont pas eu a connaitre la charge.
        skill.onActivateCharged(null, data, 7);
        assertTrue(skill.activated);
    }

    // ------------------------------------------------------------------
    // Etat d'un maintien
    // ------------------------------------------------------------------

    @Test
    void lEtatDuMaintienSuitLaCharge() {
        ChargingSkill skill = new ChargingSkill("charge", 10);
        AbilityData data = new AbilityData();

        assertEquals(-1, data.getHoldMark(skill), "aucun repere avant le premier effet");
        assertEquals(0f, data.getHeldOverload(skill), 0.0001f);
        assertFalse(data.isHoldingOverload());
        assertTrue(data.getChargingSkills().isEmpty());

        data.beginCharge(skill);
        data.setHeldOverload(skill, 42f);
        data.setHoldMark(skill, 3);
        data.tickCharges();

        assertEquals(42f, data.getHeldOverload(skill), 0.0001f);
        assertTrue(data.isHoldingOverload());
        assertEquals(3, data.getHoldMark(skill));
        assertEquals(1, data.getChargingSkills().size());

        // La fin du maintien emporte son etat : il n'a de sens que pendant.
        data.endCharge(skill);

        assertEquals(0f, data.getHeldOverload(skill), 0.0001f);
        assertEquals(-1, data.getHoldMark(skill));
        assertFalse(data.isHoldingOverload());
        assertEquals(1, data.getChargeTicks(skill), "le compteur, lui, reste lisible");
    }

    @Test
    void annulerUnMaintienOublieSonEtat() {
        ChargingSkill skill = new ChargingSkill("charge", 10);
        AbilityData data = new AbilityData();
        data.beginCharge(skill);
        data.setHeldOverload(skill, 20f);

        data.cancelCharge(skill);

        assertEquals(0f, data.getHeldOverload(skill), 0.0001f);
        assertEquals(0, data.getChargeTicks(skill));
    }

    @Test
    void laMortOublieLEtatDuMaintien() {
        ChargingSkill skill = new ChargingSkill("charge", 10);
        AbilityData data = new AbilityData();
        data.beginCharge(skill);
        data.setHeldOverload(skill, 30f);
        data.setHoldMark(skill, 5);

        data.clearCharges();

        assertFalse(data.isChargingAnything());
        assertFalse(data.isHoldingOverload());
        assertEquals(0f, data.getHeldOverload(skill), 0.0001f);
    }

    @Test
    void lEtatDuMaintienNeSurvitPasAUneSauvegarde() {
        ChargingSkill skill = new ChargingSkill("charge", 10);
        AbilityData data = new AbilityData();
        data.beginCharge(skill);
        data.setHeldOverload(skill, 25f);

        CompoundTag tag = data.serializeNBT();
        AbilityData reloaded = new AbilityData();
        reloaded.deserializeNBT(tag);

        assertFalse(reloaded.isHoldingOverload());
        assertEquals(0f, reloaded.getHeldOverload(skill), 0.0001f);
        assertEquals(-1, reloaded.getHoldMark(skill));
    }
}
