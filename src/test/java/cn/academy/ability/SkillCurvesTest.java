package cn.academy.ability;

import net.minecraft.world.phys.Vec3;
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

    @Test
    void leThunderClapSuitSaCharge() {
        var clap = cn.academy.ability.electromaster.ElectromasterCategory.THUNDER_CLAP;

        // Quarante ticks obligatoires, soixante au maximum, comme l'original.
        assertTrue(clap.isChargeable(), "le thunder clap se charge");
        assertEquals(40, clap.getMinChargeTicks(new AbilityData()));
        assertEquals(60, clap.getMaxChargeTicks(new AbilityData()));

        // Le facteur de charge ne vaut jamais les 1,2 de sa borne : la charge plafonne a
        // soixante ticks la ou le diviseur de l'original en vaut soixante apres un
        // decalage de quarante. La formule est reprise telle quelle.
        assertEquals(1f, clap.damageFactor(40), 0.0001f);
        assertEquals(1.0667f, clap.damageFactor(60), 0.0001f);
        assertEquals(1f, clap.damageFactor(0), 0.0001f, "et jamais moins de 1");

        // Degats : 36 a 72 selon l'experience, fois le facteur de charge.
        assertEquals(36f, clap.damage(atExperience(clap, 0f, 40)), 0.0001f);
        assertEquals(38.4f, clap.damage(atExperience(clap, 0f, 60)), 0.0001f,
                "60 ticks au depart");
        assertEquals(76.8f, clap.damage(atExperience(clap, 1f, 60)), 0.0001f,
                "et 60 ticks au maximum");

        Curve range = data -> (float) clap.range(data);
        assertBounds("rayon de la foudre", 15f, 30f, range, clap);
    }

    @Test
    void laRechargeDuThunderClapSuitCeQuOnATenu() {
        var clap = cn.academy.ability.electromaster.ElectromasterCategory.THUNDER_CLAP;

        // Le temps tenu fois 10 a 6 : 400 ticks (20 s) au depart pour une charge
        // minimale, 240 (12 s) au maximum.
        assertEquals(400, clap.getCooldownTicks(atExperience(clap, 0f, 40)));
        assertEquals(360, clap.getCooldownTicks(atExperience(clap, 1f, 60)));
        assertEquals(0, clap.getCooldownTicks(new AbilityData()),
                "un claquement jamais charge ne laisse rien derriere lui");
    }

    @Test
    void leBranchementVerseSonEnergieParTick() {
        var charging = cn.academy.ability.electromaster.ElectromasterCategory.CHARGING;

        // Competence tenue, sans duree maximale : elle s'arrete quand la reserve est
        // vide, comme l'original.
        assertTrue(charging.isHeld(), "le branchement se tient");
        assertEquals(0, charging.getMaxHoldTicks(new AbilityData()));
        assertEquals(0f, charging.getCpCost(), 0.0001f, "brancher ne coute rien");

        // Energie versee : 15 a 35 par tick, entiere comme dans l'original.
        assertEquals(15.0, charging.chargeSpeed(new AbilityData()), 0.0001);
        assertEquals(35.0, charging.chargeSpeed(atExperience(charging, 1f)), 0.0001);

        // Cout en CP ramene a l'echelle du port, et surcout garde tel quel.
        assertBounds("cout par tick", 0.11f, 0.25f, charging::cpPerTick, charging);
        assertBounds("surcout d'ouverture", 65f, 48f, charging::getOverloadCost, charging);
    }

    @Test
    void laTractionSePaieParTick() {
        var movement = cn.academy.ability.electromaster.ElectromasterCategory.MAG_MOVEMENT;

        assertTrue(movement.isHeld(), "la traction se tient");
        assertEquals(0, movement.getMaxHoldTicks(new AbilityData()),
                "elle s'arrete quand la reserve est vide, pas au bout d'un temps");
        assertEquals(25.0, cn.academy.ability.electromaster.MagMovementSkill.getMaxDistance(), 0.0001,
                "portee de la visee, comme l'original");
        assertEquals(0f, movement.getCpCost(), 0.0001f, "s'accrocher ne coute rien");

        assertBounds("cout par tick", 0.55f, 0.3f, movement::cpPerTick, movement);
        assertBounds("surcout d'ouverture", 60f, 30f, movement::getOverloadCost, movement);
    }

    @Test
    void leLancerDObjetSuitLExperience() {
        var throwing = cn.academy.ability.teleporter.TeleporterCategory.THREATENING_TELEPORT;

        assertTrue(throwing.isChargeable(), "le lancer se charge");
        assertEquals(0, throwing.getMinChargeTicks(new AbilityData()),
                "et part des le relachement, sans minimum");
        assertEquals(0, throwing.getMaxChargeTicks(new AbilityData()));

        assertBounds("degats du lancer", 3f, 6f, throwing::damage, throwing);
        Curve range = data -> (float) throwing.range(data);
        assertBounds("portee du lancer", 8f, 15f, range, throwing);
        assertBounds("surcout du lancer", 18f, 10f, throwing::getOverloadCost, throwing);
        Curve cp = throwing::getCpCost;
        assertBounds("cout en CP du lancer", 1.25f, 3.5f, cp, throwing);
        assertCooldownBounds("recharge du lancer", throwing, 30, 15);
    }

    @Test
    void laTeleportationAuMarqueurGranditAvecLaVisee() {
        var mark = cn.academy.ability.teleporter.TeleporterCategory.MARK_TELEPORT;

        assertTrue(mark.isChargeable(), "la visée se charge");

        // Deux blocs par tick de visee...
        assertEquals(20.0, cn.academy.ability.teleporter.MarkTeleportSkill.maxDistance(0f, 100f, 9, 0.43f),
                0.0001);
        // ... sans depasser la portee d'experience (25 a 60 blocs)...
        assertEquals(25.0, cn.academy.ability.teleporter.MarkTeleportSkill.maxDistance(0f, 100f, 999, 0.43f),
                0.0001);
        assertEquals(60.0, cn.academy.ability.teleporter.MarkTeleportSkill.maxDistance(1f, 100f, 999, 0.14f),
                0.0001);
        // ... ni ce que la reserve peut payer : 100 points a 0,43 le bloc font 232 blocs,
        // donc c'est l'experience qui decide ici.
        assertEquals(10.0, cn.academy.ability.teleporter.MarkTeleportSkill.maxDistance(0f, 4.3f, 999, 0.43f),
                0.0001, "une reserve a moitie vide ne porte qu'a dix blocs");

        assertBounds("cout par bloc", 0.43f, 0.14f, mark::cpPerBlock, mark);
        assertBounds("surcout du saut", 40f, 20f, mark::getOverloadCost, mark);
        assertCooldownBounds("recharge du saut", mark, 30, 0);

        // L'experience se paie au bloc, l'effet s'en charge : rien a declarer ici.
        assertTrue(mark.earnsExpOnEffect(), "l'effet verse l'experience");
        assertEquals(0f, mark.getExpGain(new AbilityData()), 0.0001f);
    }

    @Test
    void laDechirureSePaieAuCoup() {
        var ripping = cn.academy.ability.teleporter.TeleporterCategory.FLESH_RIPPING;

        assertTrue(ripping.isChargeable(), "la visée se charge");
        assertEquals(0, ripping.getMinChargeTicks(new AbilityData()));
        assertEquals(0, ripping.getMaxChargeTicks(new AbilityData()));

        assertBounds("degats de la dechirure", 5f, 12f, ripping::damage, ripping);
        Curve range = data -> (float) ripping.range(data);
        assertBounds("portee de la dechirure", 6f, 14f, range, ripping);
        assertBounds("surcout de la dechirure", 60f, 50f, ripping::getOverloadCost, ripping);
        Curve cp = ripping::cpCost;
        assertBounds("cout en CP de la dechirure", 4.6f, 9.6f, cp, ripping);
        assertEquals(90, ripping.cooldown(atExperience(ripping, 0f)), "recharge au depart");
        assertEquals(40, ripping.cooldown(atExperience(ripping, 1f)), "recharge au maximum");

        // Ni l'experience ni la recharge ne sont declarees au paquet : l'effet s'en
        // charge, parce que lui seul sait si le coup est parti.
        assertTrue(ripping.earnsExpOnEffect(), "l'effet verse l'experience");
        assertEquals(0f, ripping.getExpGain(new AbilityData()), 0.0001f);
        assertEquals(0, ripping.getCooldownTicks(new AbilityData()),
                "le paquet ne pose aucune recharge de lui-meme");
    }

    @Test
    void lesRayonsMinierCreusentDifferemment() {
        var basic = cn.academy.ability.meltdowner.MeltdownerCategory.MINE_RAY_BASIC;
        var expert = cn.academy.ability.meltdowner.MeltdownerCategory.MINE_RAY_EXPERT;
        var luck = cn.academy.ability.meltdowner.MeltdownerCategory.MINE_RAY_LUCK;

        // Trois competences tenues, de la meme famille, aux portees et aux vitesses de
        // creusement croissantes : c'est ce que l'original vendait, et rien d'autre.
        assertTrue(basic.isHeld() && expert.isHeld() && luck.isHeld(), "les rayons se tiennent");
        assertEquals(10.0, basic.range(), 0.0001);
        assertEquals(20.0, expert.range(), 0.0001);
        assertEquals(20.0, luck.range(), 0.0001);
        assertEquals(2, basic.tier(), "un rayon en fer");
        assertEquals(5, expert.tier(), "et deux qui percent tout");
        assertEquals(5, luck.tier());

        assertBounds("creusement du rayon de base", 0.2f, 0.4f, basic::speed, basic);
        assertBounds("creusement du rayon expert", 0.5f, 1f, expert::speed, expert);
        assertBounds("creusement du rayon chanceux", 0.5f, 1f, luck::speed, luck);

        assertBounds("entretien du rayon de base", 0.43f, 0.25f, basic::cpPerTick, basic);
        assertBounds("entretien du rayon expert", 0.9f, 0.54f, expert::cpPerTick, expert);
        assertBounds("entretien du rayon chanceux", 1.8f, 1.25f, luck::cpPerTick, luck);

        assertBounds("surcout du rayon de base", 200f, 150f, basic::getOverloadCost, basic);
        assertBounds("surcout du rayon expert", 300f, 200f, expert::getOverloadCost, expert);
        assertBounds("surcout du rayon chanceux", 350f, 300f, luck::getOverloadCost, luck);

        assertCooldownBounds("recharge du rayon de base", basic, 40, 20);
        assertCooldownBounds("recharge du rayon expert", expert, 60, 30);
        assertEquals(0.0005f, basic.expPerBlock(), 0.00001f);
        assertEquals(0.0003f, expert.expPerBlock(), 0.00001f);
    }

    private static void assertCooldownBounds(String what, Skill skill, int min, int max) {
        assertEquals(min, skill.getCooldownTicks(atExperience(skill, 0f)), what + " au depart");
        assertEquals(max, skill.getCooldownTicks(atExperience(skill, 1f)), what + " au maximum");
    }

    /**
     * La bombe a fragmentation : son calendrier de pose, et ce qu'elle en fait.
     *
     * C'est la competence la plus « en deux temps » du port — elle pose des billes
     * pendant quatre secondes et ne les envoie qu'a la fin — donc ce qui merite d'etre
     * fige est autant le calendrier que les degats. Le calendrier est reproduit ici a la
     * main, tick par tick, pour que ce soit bien l'original qui soit lu et pas la
     * fonction qui se relit elle-meme.
     */
    @Test
    void laBombeAFragmentationPoseSesBillesParDizaines() {
        var bomb = cn.academy.ability.meltdowner.MeltdownerCategory.SCATTER_BOMB;

        assertTrue(bomb.isHeld(), "la bombe se tient");
        // Pas de duree maximale : c'est le contrepoids du tick 200 qui termine le
        // maintien, et il doit pouvoir blesser le lanceur en passant.
        assertEquals(0, bomb.getMaxHoldTicks(new AbilityData()), "aucune duree maximale");
        assertEquals(200, cn.academy.ability.meltdowner.ScatterBombSkill.BACKFIRE_TICK,
                "dix secondes avant le contrepoids");

        // La premiere bille a une seconde, une toutes les dix ticks, la derniere a
        // quatre secondes : sept en tout.
        int[] expected = new int[201];
        for (int tick = 20; tick <= 80; tick += 10) {
            expected[tick] = 1;
        }
        int seen = 0;
        for (int tick = 1; tick <= 200; tick++) {
            assertEquals(expected[tick] == 1,
                    cn.academy.ability.meltdowner.ScatterBombSkill.spawnsBallAt(tick),
                    "bille au tick " + tick);
            seen += expected[tick];
            assertEquals(seen, cn.academy.ability.meltdowner.ScatterBombSkill.ballCount(tick),
                    "billes posees au tick " + tick);
        }
        assertEquals(7, cn.academy.ability.meltdowner.ScatterBombSkill.ballCount(200),
                "sept billes pour un maintien complet");

        // Les billes ne visent d'elles-memes qu'a partir de la moitie de l'experience,
        // et elles sont alors billes x experience a le faire.
        assertEquals(0, cn.academy.ability.meltdowner.ScatterBombSkill.autoTargetCount(0f, 7));
        assertEquals(0, cn.academy.ability.meltdowner.ScatterBombSkill.autoTargetCount(0.5f, 7),
                "pile a la moitie : non");
        assertEquals(4, cn.academy.ability.meltdowner.ScatterBombSkill.autoTargetCount(0.6f, 7));
        assertEquals(7, cn.academy.ability.meltdowner.ScatterBombSkill.autoTargetCount(1f, 7),
                "au maximum, elles visent toutes");

        assertBounds("degats d'une bille", 5f, 9f, bomb::ballDamage, bomb);
        assertBounds("entretien de la bombe", 0.11f, 0.21f, bomb::cpPerTick, bomb);
        assertBounds("surcout de la bombe", 80f, 60f, bomb::getOverloadCost, bomb);

        // Rien a l'ouverture en CP : les billes se paient une par une pendant la ponte.
        assertEquals(0f, bomb.getCpCost(), 0.0001f, "pas de cout en CP a l'ouverture");
        // Et aucune recharge : l'original n'en posait pas, le prix est le surcout.
        assertEquals(0, bomb.getCooldownTicks(atExperience(bomb, 1f)));
    }

    /**
     * Le reacteur : ce que vaut son vol, et ou il mene.
     *
     * C'est la seule competence dont l'effet commence au relachement — {@code onRelease}
     * garde le maintien ouvert, et le vol se termine lui-meme. Le calcul de trajectoire
     * est donc le vrai sujet : huit ticks pour rejoindre la cible, et quinze ticks de vol,
     * donc une cible <b>depassee</b> de presque une fois la distance visee. C'est
     * volontaire : c'est le comportement de l'original, et c'est ce qui en fait un
     * deplacement et non une teleportation.
     */
    @Test
    void leVolDuReacteurDepasseSaCible() {
        var jet = cn.academy.ability.meltdowner.MeltdownerCategory.JET_ENGINE;

        assertTrue(jet.isHeld(), "le reacteur se tient");
        assertEquals(0, jet.getMaxHoldTicks(new AbilityData()),
                "la visee dure tant que la touche est tenue");
        assertEquals(8, cn.academy.ability.meltdowner.JetEngineSkill.FLIGHT_TIME,
                "huit ticks pour rejoindre la cible");
        assertEquals(15, cn.academy.ability.meltdowner.JetEngineSkill.LIFETIME,
                "quinze ticks de vol en tout");

        Vec3 start = new Vec3(0, 64, 0);
        Vec3 target = new Vec3(12, 64, 0);

        // Au huitieme tick, le porteur est sur la cible.
        assertEquals(12.0, cn.academy.ability.meltdowner.JetEngineSkill
                .pathPosition(start, target, 8).x, 0.0001);
        // A la moitie du temps, a mi-chemin.
        assertEquals(6.0, cn.academy.ability.meltdowner.JetEngineSkill
                .pathPosition(start, target, 4).x, 0.0001);
        // Au dernier tick de vol, il l'a depassee de presque une fois la visee.
        assertEquals(22.5, cn.academy.ability.meltdowner.JetEngineSkill
                .pathPosition(start, target, 15).x, 0.0001);
        // Et il ne quitte jamais la ligne.
        assertEquals(64.0, cn.academy.ability.meltdowner.JetEngineSkill
                .pathPosition(start, target, 11).y, 0.0001);

        // La vitesse posee a chaque tick est celle qui l'emmene en huit ticks.
        Vec3 velocity = cn.academy.ability.meltdowner.JetEngineSkill.flightVelocity(start, target);
        assertEquals(1.5, velocity.x, 0.0001);

        assertBounds("degats du vol", 7f, 20f, jet::flightDamage, jet);
        assertBounds("surcout du reacteur", 60f, 50f, jet::getOverloadCost, jet);
        assertCooldownBounds("recharge du reacteur", jet, 60, 30);

        // Le cout en CP de l'original, divise par 28 : 170 a 140 sur plusieurs milliers.
        assertEquals(6.07f, jet.getCpCost(atExperience(jet, 0f)), 0.0001f);
        assertEquals(5f, jet.getCpCost(atExperience(jet, 1f)), 0.0001f);
    }

    /**
     * Le contrat du relachement.
     *
     * Une competence tenue qui ne dit rien de special voit son maintien se terminer au
     * relachement : c'est le cas de toutes sauf une. Le test fige les deux roles — sans
     * quoi un effet qui se prolongerait par erreur passerait inapercu.
     *
     * <p>Le vol du reacteur, lui, ne peut pas se derouler ici : il deplace un joueur dans
     * un monde. Ce qui se verifie sans monde est ce qui l'encadre — l'effet n'a pas
     * commence tant que la touche est tenue, et il se termine au-dela de sa duree.
     */
    @Test
    void seulLeReacteurContinueApresLeRelachement() {
        var jet = cn.academy.ability.meltdowner.MeltdownerCategory.JET_ENGINE;

        for (var category : java.util.List.of(
                cn.academy.ability.meltdowner.MeltdownerCategory.INSTANCE,
                cn.academy.ability.electromaster.ElectromasterCategory.INSTANCE,
                cn.academy.ability.teleporter.TeleporterCategory.INSTANCE,
                cn.academy.ability.vecmanip.VecmanipCategory.INSTANCE)) {
            for (Skill skill : category.getSkills()) {
                if (!skill.isHeld() || skill == jet) continue;
                // Le relachement termine le maintien de toutes les autres, et le joueur
                // n'est meme pas lu.
                assertFalse(skill.onRelease(null, new AbilityData(), 20),
                        skill.getName() + " ne doit pas se prolonger apres le relachement");
            }
        }

        // Le reacteur, lui, ne vole pas tant qu'on tient la touche : il vise.
        AbilityData vising = new AbilityData();
        for (int tick = 1; tick <= 200; tick++) {
            assertTrue(jet.onHoldTick(null, vising, tick),
                    "la visee ne doit pas s'interrompre au tick " + tick);
        }

        // Une fois parti (le repere de temps du maintien est le tick du depart), le vol se
        // termine au tick qui suit sa duree. Les quinze ticks de vol eux-memes deplacent
        // un joueur dans un monde : ils sont hors de portee d'un test unitaire, et c'est
        // la fin qui se verifie ici — un vol qui ne s'arreterait jamais laisserait le
        // joueur a la merci d'une competence qu'il ne peut plus relacher.
        AbilityData flying = new AbilityData();
        flying.setHoldMark(jet, 5);
        assertFalse(jet.onHoldTick(null, flying, 5 + 16), "le vol doit se terminer");
        assertFalse(jet.onHoldTick(null, flying, 5 + 60), "et le rester");
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
