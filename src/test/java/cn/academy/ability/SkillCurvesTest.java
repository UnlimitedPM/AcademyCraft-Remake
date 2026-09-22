package cn.academy.ability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les courbes de puissance des competences portees.
 *
 * L'original fait grandir chaque competence avec son experience, par une
 * interpolation lineaire entre une valeur de depart et une valeur d'arrivee
 * ({@code MathUtils.lerpf}). C'est ce qui donne un sens a l'experience au-dela du
 * verrou de niveau.
 *
 * Ce test fige les deux bornes de chaque courbe, comme {@code PortedSkillsTest}
 * fige les niveaux, les dependances et les gains d'experience. Une valeur de depart
 * ou d'arrivee modifiee par erreur se verrait ici, et pas apres une partie.
 *
 * <h2>Ce qui n'est pas porte</h2>
 *
 * Les couts en CP de l'original (200 a 450 pour le railgun, 300 par coup renvoye
 * pour la reflexion) supposent une reserve de plusieurs milliers de points, la ou
 * le port plafonne a 100 : ceux du port sont conserves tels quels. De meme, la
 * duree de recharge continue du meltdowner (10 a 15 points par tick pendant la
 * charge) n'a pas d'equivalent a cette echelle, donc le port ne facture que le tir,
 * pas la charge.</p>
 */
class SkillCurvesTest {

    /** Un joueur qui a appris la competence, avec l'experience demandee. */
    private static AbilityData atExperience(Skill skill, float exp) {
        AbilityData data = new AbilityData();
        data.setCategoryLevel(skill.getCategory(), 1);
        data.learnSkill(skill);
        if (exp > 0f) data.addSkillExp(skill, exp);
        return data;
    }

    /**
     * La meme chose, avec une charge tenue pendant {@code chargeTicks} ticks.
     *
     * Le compteur de charge survit au relachement : c'est ainsi que la competence lit
     * combien de temps elle a ete chargee, exactement comme le paquet d'activation le
     * fait en jeu.
     */
    private static AbilityData atExperience(Skill skill, float exp, int chargeTicks) {
        AbilityData data = atExperience(skill, exp);
        data.beginCharge(skill);
        for (int i = 0; i < chargeTicks; i++) {
            data.tickCharges();
        }
        data.endCharge(skill);
        return data;
    }

    private static void assertBounds(String what, float min, float max, Curve curve, Skill skill) {
        assertEquals(min, curve.at(atExperience(skill, 0f)), 0.0001f, what + " au depart");
        assertEquals(max, curve.at(atExperience(skill, 1f)), 0.0001f, what + " au maximum");
    }

    /** Ce qu'on interroge, pour passer la meme fonction aux deux bornes. */
    private interface Curve {
        float at(AbilityData data);
    }

    @Test
    void lesDegatsSuiventLExperience() {
        var arcGen = cn.academy.ability.electromaster.ElectromasterCategory.ARC_GEN;
        assertBounds("degats de arc_gen", 5f, 9f, arcGen::damage, arcGen);

        var railgun = cn.academy.ability.electromaster.ElectromasterCategory.RAILGUN;
        assertBounds("degats de railgun", 60f, 110f, railgun::damage, railgun);

        // Le thunder bolt fait deux degats : la cible touchee, et ce qui l'entoure.
        var thunderBolt = cn.academy.ability.electromaster.ElectromasterCategory.THUNDER_BOLT;
        assertBounds("degats de thunder_bolt", 10f, 25f, thunderBolt::damage, thunderBolt);
        assertBounds("degats de propagation", 6f, 15f, thunderBolt::aoeDamage, thunderBolt);

        var electronBomb = cn.academy.ability.meltdowner.MeltdownerCategory.ELECTRON_BOMB;
        assertBounds("degats de electron_bomb", 6f, 12f, electronBomb::damage, electronBomb);

        var meltdowner = cn.academy.ability.meltdowner.MeltdownerCategory.MELTDOWNER;
        // Le meltdowner multiplie ses degats par son facteur de charge : les deux
        // bornes de sa courbe se lisent donc a une seconde et a deux secondes de tir.
        assertEquals(14.4f, meltdowner.damage(atExperience(meltdowner, 0f, 20)), 0.0001f,
                "degats de meltdowner au depart");
        assertEquals(60f, meltdowner.damage(atExperience(meltdowner, 1f, 40)), 0.0001f,
                "degats de meltdowner au maximum");
    }

    @Test
    void lesCompetencesQuiSeChargentSuiventLeurTempsDeCharge() {
        var meltdowner = cn.academy.ability.meltdowner.MeltdownerCategory.MELTDOWNER;
        var vecAccel = cn.academy.ability.vecmanip.VecmanipCategory.VEC_ACCEL;

        // Le meltdowner exige une seconde de charge et ne gagne plus rien apres deux ;
        // l'acceleration de vecteur plafonne a une seconde, sans minimum.
        assertTrue(meltdowner.isChargeable(), "le meltdowner se charge");
        assertEquals(20, meltdowner.getMinChargeTicks(new AbilityData()), "TICKS_MIN");
        assertEquals(40, meltdowner.getMaxChargeTicks(new AbilityData()), "TICKS_MAX");
        assertTrue(vecAccel.isChargeable(), "vec_accel se charge");
        assertEquals(0, vecAccel.getMinChargeTicks(new AbilityData()), "vec_accel part toujours");
        assertEquals(20, vecAccel.getMaxChargeTicks(new AbilityData()), "MAX_CHARGE");

        // Facteur de charge du meltdowner : 0,8 a une seconde, 1,2 a deux.
        assertEquals(0.8f, meltdowner.timeRate(atExperience(meltdowner, 0f, 20)), 0.0001f);
        assertEquals(1.2f, meltdowner.timeRate(atExperience(meltdowner, 0f, 40)), 0.0001f,
                "deux secondes de charge valent moitie plus qu'une");

        // Et il multiplie aussi la recharge : tenir son tir se paie en attente.
        assertEquals(240, meltdowner.getCooldownTicks(atExperience(meltdowner, 0f, 20)));
        assertEquals(168, meltdowner.getCooldownTicks(atExperience(meltdowner, 1f, 40)));

        // Vitesse de vec_accel : sin(0,4) x 2,5 a l'appui, sin(1) x 2,5 a pleine charge.
        assertEquals(Math.sin(0.4) * 2.5, vecAccel.speed(new AbilityData()), 0.0001);
        assertEquals(Math.sin(1.0) * 2.5, vecAccel.speed(atExperience(vecAccel, 0f, 20)), 0.0001);
    }

    @Test
    void leBouclierSeTientEtSePaie() {
        var shield = cn.academy.ability.meltdowner.MeltdownerCategory.LIGHT_SHIELD;

        // Le bouclier est une competence tenue, plus une passive : c'est ce qui lui rend
        // sa duree, sa recharge et son surcout.
        assertTrue(shield.isHeld(), "le bouclier se tient");
        assertFalse(shield.isPassive(), "ce n'est plus une passive");
        assertEquals(120, shield.getMaxHoldTicks(new AbilityData()), "MAX_TIME au depart");
        assertEquals(180, shield.getMaxHoldTicks(atExperience(shield, 1f)), "MAX_TIME au maximum");

        // Bornes des courbes, toutes reprises de l'original...
        assertBounds("degats absorbes", 15f, 50f, shield::absorbDamage, shield);
        assertBounds("degats de contact", 2f, 6f, shield::touchDamage, shield);
        assertBounds("surcout d'ouverture", 110f, 60f, shield::getOverloadCost, shield);
        assertBounds("surcout par coup", 5f, 3f, shield::overloadPerHit, shield);

        // ...sauf les couts en CP, ramenes a l'echelle de la reserve du port pour que
        // la duree de maintien reste celle de l'original.
        assertBounds("entretien par tick", 1f, 0.7f, shield::holdCpCost, shield);
        assertBounds("CP par coup", 2f, 1f, shield::cpPerHit, shield);
        assertEquals(0f, shield.getCpCost(), 0.0001f, "pas de cout en CP a l'ouverture");
    }

    @Test
    void laRechargeDuBouclierSuitCeQuIlAFalluTenir() {
        var shield = cn.academy.ability.meltdowner.MeltdownerCategory.LIGHT_SHIELD;

        // L'original posait la recharge a la fin du maintien, avec les ticks tenus :
        // deux fois la duree au depart, une seule au maximum.
        assertEquals(120, shield.getCooldownTicks(atExperience(shield, 0f, 60)),
                "une minute tenue coute deux minutes d'attente au depart");
        assertEquals(60, shield.getCooldownTicks(atExperience(shield, 1f, 60)),
                "et une seule au maximum");
        assertEquals(0, shield.getCooldownTicks(new AbilityData()),
                "un bouclier jamais tenu ne laisse rien derriere lui");
    }

    @Test
    void lesPorteesSuiventLExperience() {
        var arcGen = cn.academy.ability.electromaster.ElectromasterCategory.ARC_GEN;
        Curve arcRange = data -> (float) arcGen.range(data);
        assertBounds("portee de arc_gen", 6f, 15f, arcRange, arcGen);

        var penetrate = cn.academy.ability.teleporter.TeleporterCategory.PENETRATE_TELEPORT;
        Curve penetrateRange = data -> (float) penetrate.range(data);
        assertBounds("portee de penetrate_teleport", 10f, 35f, penetrateRange, penetrate);

        var shift = cn.academy.ability.teleporter.TeleporterCategory.SHIFT_TELEPORT;
        Curve shiftRange = data -> (float) shift.maxRange(data);
        assertBounds("portee de shift_tp", 25f, 35f, shiftRange, shift);
    }

    @Test
    void laChanceDEbrasementSuitLExperience() {
        var arcGen = cn.academy.ability.electromaster.ElectromasterCategory.ARC_GEN;

        // Nulle au depart, 60 % au maximum : l'arc n'embrase pas un debutant.
        assertBounds("chance d'embrasement", 0f, 0.6f, arcGen::igniteChance, arcGen);
    }

    @Test
    void laReflexionSuitLExperience() {
        var reflection = cn.academy.ability.vecmanip.VecmanipCategory.VEC_REFLECTION;

        // Au maximum elle renvoie plus qu'elle n'encaisse : c'est voulu par l'original.
        assertBounds("part renvoyee", 0.6f, 1.2f, reflection::reflectRatio, reflection);
    }

    private static void assertCooldownBounds(String what, Skill skill, int min, int max) {
        assertEquals(min, skill.getCooldownTicks(atExperience(skill, 0f)), what + " au depart");
        assertEquals(max, skill.getCooldownTicks(atExperience(skill, 1f)), what + " au maximum");
    }

    @Test
    void lesRechargesSuiventLExperience() {
        assertCooldownBounds("recharge de arc_gen",
                cn.academy.ability.electromaster.ElectromasterCategory.ARC_GEN, 15, 5);
        assertCooldownBounds("recharge de railgun",
                cn.academy.ability.electromaster.ElectromasterCategory.RAILGUN, 300, 160);
        assertCooldownBounds("recharge de thunder_bolt",
                cn.academy.ability.electromaster.ElectromasterCategory.THUNDER_BOLT, 120, 50);
        assertCooldownBounds("recharge de body_intensify",
                cn.academy.ability.electromaster.ElectromasterCategory.BODY_INTENSIFY, 900, 600);
        assertCooldownBounds("recharge de electron_bomb",
                cn.academy.ability.meltdowner.MeltdownerCategory.ELECTRON_BOMB, 20, 10);
        assertCooldownBounds("recharge de penetrate_teleport",
                cn.academy.ability.teleporter.TeleporterCategory.PENETRATE_TELEPORT, 50, 30);
        assertCooldownBounds("recharge de shift_tp",
                cn.academy.ability.teleporter.TeleporterCategory.SHIFT_TELEPORT, 100, 60);
        assertCooldownBounds("recharge de vec_accel",
                cn.academy.ability.vecmanip.VecmanipCategory.VEC_ACCEL, 80, 50);
    }

    @Test
    void lesCompetencesSansRechargeNEnOntPas() {
        // Les passives n'en ont jamais eu.
        assertEquals(0, cooldownOf(cn.academy.ability.vecmanip.VecmanipCategory.VEC_REFLECTION));
        assertEquals(0,
                cooldownOf(cn.academy.ability.teleporter.TeleporterCategory.DIM_FOLDING_THEOREM));
    }

    private static int cooldownOf(Skill skill) {
        return skill.getCooldownTicks(atExperience(skill, 1f));
    }

    @Test
    void uneCompetenceNonAppriseEstAuDepartDeSaCourbe() {
        // Sans categorie apprise, l'experience vaut 0 : les courbes doivent donc
        // rendre leur borne basse, et non une valeur aberrante.
        var arcGen = cn.academy.ability.electromaster.ElectromasterCategory.ARC_GEN;
        AbilityData blank = new AbilityData();

        assertEquals(5f, arcGen.damage(blank), 0.0001f);
        assertEquals(6f, (float) arcGen.range(blank), 0.0001f);
        assertEquals(0f, arcGen.igniteChance(blank), 0.0001f);
    }

    @Test
    void uneExperienceHorsBornesNeDebordePasDeLaCourbe() {        var arcGen = cn.academy.ability.electromaster.ElectromasterCategory.ARC_GEN;

        // Un gain negatif est ignore, et un gain enorme sature a 100 % : dans les deux
        // cas la courbe reste entre ses deux bornes, ce qui protege d'une sauvegarde
        // abimee ou d'une commande de debogage.
        assertEquals(5f, arcGen.damage(atExperience(arcGen, -1f)), 0.0001f,
                "un gain negatif ne doit rien changer");
        assertEquals(9f, arcGen.damage(atExperience(arcGen, 5f)), 0.0001f,
                "un gain enorme doit saturer au maximum");
    }
}
