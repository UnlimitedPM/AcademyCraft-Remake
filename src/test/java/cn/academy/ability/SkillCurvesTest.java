package cn.academy.ability;

import net.minecraft.core.BlockPos;
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

        // Le depose au loin ne blesse pas ce qu'il vise mais ce qu'il traverse : c'est la ligne
        // entre lui et la case ou le bloc se pose.
        var shift = cn.academy.ability.teleporter.TeleporterCategory.SHIFT_TELEPORT;
        assertBounds("degats de shift_tp", 15f, 35f, shift::damage, shift);
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
        assertBounds("entretien par tick", 9f, 4f, shield::holdCpCost, shield);
        assertBounds("CP par coup", 50f, 30f, shield::cpPerHit, shield);
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
        Curve penetrateRange = data -> (float) penetrate.maxDistance(data);
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
        assertBounds("cout par tick", 3f, 7f, charging::cpPerTick, charging);
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

        assertBounds("cout par tick", 15f, 8f, movement::cpPerTick, movement);
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
        assertBounds("cout en CP du lancer", 35f, 100f, cp, throwing);
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

        assertBounds("cout par bloc", 12f, 4f, mark::cpPerBlock, mark);
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
        assertBounds("cout en CP de la dechirure", 130f, 270f, cp, ripping);
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

        assertBounds("entretien du rayon de base", 12f, 7f, basic::cpPerTick, basic);
        assertBounds("entretien du rayon expert", 25f, 15f, expert::cpPerTick, expert);
        assertBounds("entretien du rayon chanceux", 50f, 35f, luck::cpPerTick, luck);

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
        assertBounds("entretien de la bombe", 3f, 6f, bomb::cpPerTick, bomb);
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
        assertEquals(170f, jet.getCpCost(atExperience(jet, 0f)), 0.0001f);
        assertEquals(140f, jet.getCpCost(atExperience(jet, 1f)), 0.0001f);
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
    void leReacteurEtLeCanonContinuentApresLeRelachement() {
        var jet = cn.academy.ability.meltdowner.MeltdownerCategory.JET_ENGINE;
        var cannon = cn.academy.ability.vecmanip.VecmanipCategory.PLASMA_CANNON;
        // Le saut traversant et le depose au loin, eux, LISENT leur joueur au relachement : l'un le
        // deplace, l'autre lui prend le bloc qu'il tient. Ce sont les deux seules competences
        // tenues du port dans ce cas, et c'est pour cela qu'elles sont ecartees ici — voir
        // PenetrateTeleportTest et ShiftTeleportSkillTest pour ce qu'elles doivent faire.
        var penetrate = cn.academy.ability.teleporter.TeleporterCategory.PENETRATE_TELEPORT;
        var shift = cn.academy.ability.teleporter.TeleporterCategory.SHIFT_TELEPORT;

        for (var category : java.util.List.of(
                cn.academy.ability.meltdowner.MeltdownerCategory.INSTANCE,
                cn.academy.ability.electromaster.ElectromasterCategory.INSTANCE,
                cn.academy.ability.teleporter.TeleporterCategory.INSTANCE,
                cn.academy.ability.vecmanip.VecmanipCategory.INSTANCE)) {
            for (Skill skill : category.getSkills()) {
                if (!skill.isHeld() || skill == jet || skill == cannon || skill == penetrate
                        || skill == shift) {
                    continue;
                }
                // Le relachement termine le maintien de toutes les autres, et le joueur
                // n'est meme pas lu.
                assertFalse(skill.onRelease(null, new AbilityData(), 20),
                        skill.getName() + " ne doit pas se prolonger apres le relachement");
            }
        }

        // Le canon a plasma, lui, ne part que sur une charge complete : sous la charge
        // minimale, l'original mourait sans rien faire, et ne versait rien.
        assertFalse(cannon.onRelease(null, new AbilityData(), 20),
                "sous la charge minimale, le canon ne part pas");

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

    /**
     * La salve de rayons : ses deux versions, et la forme de son cone.
     *
     * C'est la seule competence du mod qui change de nature selon ce que le regard trouve :
     * un tir simple sur ce qu'elle voit, ou une salve en cone si elle trouve une bille de
     * silicium en vol. Le cone, lui, n'est pas rond — 27,5 degres de lacet pour 55 de
     * tangage, parce que l'original s'est servi deux fois de la meme portee de deux facons
     * differentes. Personne ne devinerait ces proportions en jouant, donc elles sont figees
     * ici.
     */
    @Test
    void laSalveDeRayonsOuvreUnConeDeuxFoisPlusHautQueLarge() {
        var barrage = cn.academy.ability.meltdowner.MeltdownerCategory.RAY_BARRAGE;

        assertFalse(barrage.isHeld(), "c'est un tir, pas un maintien");
        assertFalse(barrage.isChargeable(), "et il ne se charge pas non plus");
        assertEquals(55.0, cn.academy.ability.meltdowner.RayBarrageSkill.CONE_RANGE, 0.0001);
        assertEquals(27.5, cn.academy.ability.meltdowner.RayBarrageSkill.HALF_YAW, 0.0001);
        assertEquals(55.0, cn.academy.ability.meltdowner.RayBarrageSkill.HALF_PITCH, 0.0001);

        var look = new Vec3(0, 0, 1);

        // Dans l'axe : oui.
        assertTrue(cn.academy.ability.meltdowner.RayBarrageSkill.inCone(look, new Vec3(0, 0, 10)));
        // Vers le haut de 50 degres : oui, le cone est haut.
        assertTrue(cn.academy.ability.meltdowner.RayBarrageSkill.inCone(look,
                new Vec3(0, Math.tan(Math.toRadians(50)), 1)));
        // Vers le haut de 60 : non, on est sorti par le plafond.
        assertFalse(cn.academy.ability.meltdowner.RayBarrageSkill.inCone(look,
                new Vec3(0, Math.tan(Math.toRadians(60)), 1)));
        // Vers la droite de 25 degres : oui.
        assertTrue(cn.academy.ability.meltdowner.RayBarrageSkill.inCone(look,
                new Vec3(Math.tan(Math.toRadians(25)), 0, 1)));
        // Vers la droite de 30 : non, on est sorti par le cote — le cone est plus etroit
        // que haut, et c'est ce qui le distingue d'un entonnoir.
        assertFalse(cn.academy.ability.meltdowner.RayBarrageSkill.inCone(look,
                new Vec3(Math.tan(Math.toRadians(30)), 0, 1)));

        // Derriere : non, evidemment.
        assertFalse(cn.academy.ability.meltdowner.RayBarrageSkill.inCone(look, new Vec3(0, 0, -10)));

        // Et le lacet se ramene, sinon viser vers l'ouest ferait passer une cible a cote
        // pour une cible a l'oppose.
        var west = new Vec3(-1, 0, 0);
        assertTrue(cn.academy.ability.meltdowner.RayBarrageSkill.inCone(west, new Vec3(-10, 0, 0.1)));
        assertFalse(cn.academy.ability.meltdowner.RayBarrageSkill.inCone(west, new Vec3(10, 0, 0)));

        // Les deux versions de la competence.
        assertBounds("degats du tir simple", 25f, 60f, barrage::plainDamage, barrage);
        assertBounds("degats de la salve", 10f, 18f, barrage::scatteredDamage, barrage);
        assertBounds("surcout de la salve", 300f, 140f, barrage::getOverloadCost, barrage);
        assertCooldownBounds("recharge de la salve", barrage, 100, 40);
        // 450 a 380 CP chez l'original, divises par 28.
        assertEquals(450f, barrage.getCpCost(atExperience(barrage, 0f)), 0.0001f);
        assertEquals(380f, barrage.getCpCost(atExperience(barrage, 1f)), 0.0001f);
    }

    /**
     * Le scintillement : ses quatre directions, ses sauts, et ce qu'ils coutent.
     *
     * <p>C'est la seule competence du port qui ecoute les touches de deplacement, et la
     * seule dont les quatre gestes se ressemblent assez pour qu'une inversion se voie a
     * peine en jeu — avancer la ou on voulait reculer, ou un saut en miroir quand le joueur
     * se retourne. Les directions sont donc verifiees une par une.
     */
    @Test
    void leScintillementSauteDansLesQuatreSens() {
        var flashing = cn.academy.ability.teleporter.TeleporterCategory.FLASHING;

        assertTrue(flashing.isHeld(), "le scintillement se tient");
        assertTrue(flashing.listensToDirections(), "et c'est lui qui ecoute les directions");

        // Regard vers +Z (lacet zero de Minecraft), sans inclinaison : avancer c'est aller
        // vers +Z, et la gauche d'un joueur qui regarde le sud, c'est l'est (+X).
        Vec3 south = new Vec3(0, 0, 1);

        assertEquals(new Vec3(0, 0, 1),
                round(cn.academy.ability.teleporter.FlashingSkill
                        .dashDirection(south, 0, cn.academy.ability.teleporter.FlashingSkill.FORWARD)));
        assertEquals(new Vec3(0, 0, -1),
                round(cn.academy.ability.teleporter.FlashingSkill
                        .dashDirection(south, 0, cn.academy.ability.teleporter.FlashingSkill.BACK)));
        assertEquals(new Vec3(1, 0, 0),
                round(cn.academy.ability.teleporter.FlashingSkill
                        .dashDirection(south, 0, cn.academy.ability.teleporter.FlashingSkill.LEFT)));
        assertEquals(new Vec3(-1, 0, 0),
                round(cn.academy.ability.teleporter.FlashingSkill
                        .dashDirection(south, 0, cn.academy.ability.teleporter.FlashingSkill.RIGHT)));

        // Regard vers l'est (+X) : les quatre directions tournent avec le joueur.
        Vec3 east = new Vec3(1, 0, 0);
        assertEquals(new Vec3(1, 0, 0),
                round(cn.academy.ability.teleporter.FlashingSkill
                        .dashDirection(east, 0, cn.academy.ability.teleporter.FlashingSkill.FORWARD)));
        assertEquals(new Vec3(0, 0, -1),
                round(cn.academy.ability.teleporter.FlashingSkill
                        .dashDirection(east, 0, cn.academy.ability.teleporter.FlashingSkill.LEFT)));

        // Le regard incline le saut : vers le haut on monte, vers le sol on descend, et la
        // longueur reste celle du saut — c'est une direction, pas une portee.
        Vec3 up = cn.academy.ability.teleporter.FlashingSkill
                .dashDirection(south, -45, cn.academy.ability.teleporter.FlashingSkill.FORWARD);
        assertEquals(-Math.sin(Math.toRadians(-45)), up.y, 0.0001);
        assertEquals(1.0, up.length(), 0.0001);
        Vec3 down = cn.academy.ability.teleporter.FlashingSkill
                .dashDirection(south, 45, cn.academy.ability.teleporter.FlashingSkill.FORWARD);
        assertEquals(-Math.sin(Math.toRadians(45)), down.y, 0.0001);

        // De cote, en revanche, le regard ne fait rien du tout : l'original tournait un
        // vecteur (0, 0, +-1) autour de l'axe Z, une rotation qui ne peut ni le monter ni le
        // descendre. Viser ses pieds laisse donc le saut lateral a la meme hauteur, et non
        // plongeant — c'est ce que le joueur voyait en jeu.
        assertEquals(new Vec3(1, 0, 0),
                round(cn.academy.ability.teleporter.FlashingSkill
                        .dashDirection(south, 89, cn.academy.ability.teleporter.FlashingSkill.LEFT)));
        assertEquals(new Vec3(-1, 0, 0),
                round(cn.academy.ability.teleporter.FlashingSkill
                        .dashDirection(south, 89, cn.academy.ability.teleporter.FlashingSkill.RIGHT)));
        assertEquals(new Vec3(0, 0, -1),
                round(cn.academy.ability.teleporter.FlashingSkill
                        .dashDirection(east, -89, cn.academy.ability.teleporter.FlashingSkill.LEFT)));

        // Regard pile a la verticale : il n'y a plus d'horizon, et le nord prend le relais.
        assertEquals(new Vec3(0, -1, 0),
                round(cn.academy.ability.teleporter.FlashingSkill
                        .dashDirection(new Vec3(0, -1, 0), 90,
                                cn.academy.ability.teleporter.FlashingSkill.FORWARD)));

        // Les directions sont celles de l'original, et rien d'autre.
        assertTrue(cn.academy.ability.teleporter.FlashingSkill.isDirection(1));
        assertTrue(cn.academy.ability.teleporter.FlashingSkill.isDirection(4));
        assertFalse(cn.academy.ability.teleporter.FlashingSkill.isDirection(0));
        assertFalse(cn.academy.ability.teleporter.FlashingSkill.isDirection(5));

        // Les courbes : 12 a 18 blocs, 13 a 6 CP par saut (divises par 28), 80 a 60 CP et
        // 250 a 180 de surcout a l'ouverture, et la plus longue recharge du port.
        assertEquals(12.0, flashing.distance(atExperience(flashing, 0f)), 0.0001);
        assertEquals(18.0, flashing.distance(atExperience(flashing, 1f)), 0.0001);
        assertBounds("cout d'un saut", 13f, 6f, flashing::dashCost, flashing);
        assertEquals(80f, flashing.getCpCost(atExperience(flashing, 0f)), 0.0001f);
        assertEquals(60f, flashing.getCpCost(atExperience(flashing, 1f)), 0.0001f);
        assertBounds("surcout du scintillement", 250f, 180f, flashing::getOverloadCost, flashing);
        assertEquals(60, flashing.getMaxHoldTicks(atExperience(flashing, 0f)));
        assertEquals(150, flashing.getMaxHoldTicks(atExperience(flashing, 1f)));
        assertCooldownBounds("recharge du scintillement", flashing, 900, 400);
    }

    /**
     * Le choc dirige : une fenetre de charge etroite, et une poussee qui n'existe qu'a
     * partir d'un quart d'experience.
     */
    @Test
    void leChocDirigeSeChargeEtPousse() {
        var dirShock = cn.academy.ability.vecmanip.VecmanipCategory.DIRECTED_SHOCK;

        // La touche se tient, et la fenetre est etroite : six ticks au minimum, cinquante au
        // dela desquels le coup est perdu. Contrairement aux autres competences a charge, le
        // relachement n'est jamais "trop tot" ni "trop tard" de la meme facon : en dessous,
        // le paquet ne declenche rien ; au-dessus, la charge s'abandonne d'elle-meme.
        assertTrue(dirShock.isChargeable(), "le choc dirige se charge");
        assertEquals(6, dirShock.getMinChargeTicks(atExperience(dirShock, 0f)),
                "un appui de moins de six ticks ne part pas");
        assertEquals(50, dirShock.getMaxChargeTicks(atExperience(dirShock, 0f)),
                "au dela de cinquante ticks, le coup est perdu");

        // Ses courbes : 7 a 15 degats, 50 a 100 CP divisees par 28, 18 a 12 de surcout, et
        // 60 a 20 ticks de recharge.
        assertBounds("degats de dir_shock", 7f, 15f, dirShock::damage, dirShock);
        assertBounds("cout de dir_shock", 50f, 100f, dirShock::getCpCost, dirShock);
        assertBounds("surcout de dir_shock", 18f, 12f, dirShock::getOverloadCost, dirShock);
        assertBounds("recharge de dir_shock", 60f, 20f, dirShock::cooldown, dirShock);

        // La recharge n'est posee par personne au moment de partir : c'est le coup qui la
        // pose, et seulement s'il a touche quelque chose. Le paquet ne doit donc rien poser
        // de lui-meme, sinon un poing dans le vide ferait deja attendre.
        assertEquals(0, dirShock.getCooldownTicks(atExperience(dirShock, 0f)),
                "une recharge qui ne se pose qu'au contact");

        // Tout se verse au coup : 0,0035 s'il touche, 0,001 dans le vide, par l'effet lui-meme.
        assertTrue(dirShock.earnsExpOnEffect(), "le choc verse son experience depuis son effet");
        assertEquals(0f, dirShock.getExpGain(atExperience(dirShock, 0f)), 0.000001f,
                "rien au declenchement : le paquet ne voit pas si le poing a touche");
    }

    /**
     * La poussee du choc, verifiee sur ses seuls points de visee.
     *
     * C'est la quatrieme coquille corrigee de l'original : son axe Z recevait la composante
     * verticale de la direction au lieu de l'horizontale. Une cible droit devant, a la meme
     * hauteur — le cas le plus courant — ne reculait donc pas du tout : elle montait.
     */
    @Test
    void laPousseeDuChocDirigeRepousseEtSouleve() {
        // Cible pile en face, meme hauteur : elle recule et monte, mais reste dans l'axe.
        Vec3 face = cn.academy.ability.vecmanip.DirectedShockSkill.knockbackVelocity(
                new Vec3(0, 0, 0), new Vec3(0, 0, 1));
        assertTrue(face.z > 0, "une cible devant recule : " + face);
        assertTrue(face.y > 0, "et la poussee la souleve : " + face);
        assertEquals(0.0, face.x, 0.0001, "sans derive laterale : " + face);
        // Le recul vaut 0,6 de bloc par tick, et le soulevement 0,36 : c'est la poussee de
        // l'original, dont l'axe Z prenait la composante verticale — nulle ici — au lieu de
        // l'horizontale. Sans la correction, la cible montait sans reculer d'un pouce.
        assertEquals(0.600, face.z, 0.001, "le recul horizontal");
        assertEquals(0.360, face.y, 0.001, "le soulevement");

        // Cible de cote : elle part de l'autre cote, sans derive sur l'axe Z.
        Vec3 side = cn.academy.ability.vecmanip.DirectedShockSkill.knockbackVelocity(
                new Vec3(0, 0, 0), new Vec3(1, 0, 0));
        assertTrue(side.x > 0, "une cible de cote part de l'autre cote : " + side);
        assertEquals(0.0, side.z, 0.0001, "sans derive sur l'axe Z");

        // Cible au-dessus : elle monte, c'est le sens de l'eloignement.
        Vec3 above = cn.academy.ability.vecmanip.DirectedShockSkill.knockbackVelocity(
                new Vec3(0, 0, 0), new Vec3(0, 3, 0));
        assertTrue(above.y > 0, "une cible au-dessus monte : " + above);

        // Cible en dessous : elle part vers le bas, mais le soulevement de 0,6 use la
        // direction — une cible tres en dessous repart doucement vers le haut.
        Vec3 below = cn.academy.ability.vecmanip.DirectedShockSkill.knockbackVelocity(
                new Vec3(0, 0, 0), new Vec3(0, -3, 0));
        assertTrue(below.y < 0, "une cible en dessous part vers le bas : " + below);

        // La force est celle de l'original : 0,7 de long, quelle que soit la visee —
        // l'original appliquait 0,7 a une direction unitaire.
        for (Vec3 eye : new Vec3[] {
                new Vec3(2, 0, 0), new Vec3(0, 0, -4), new Vec3(3, 1, 3),
                new Vec3(0, -3, 0), new Vec3(0, 5, 0) }) {
            double length = cn.academy.ability.vecmanip.DirectedShockSkill
                    .knockbackVelocity(new Vec3(0, 0, 0), eye).length();
            assertEquals(0.7, length, 0.0001, "force constante pour " + eye);
        }

        // Deux yeux au meme endroit n'ont pas de direction : pas de division par zero.
        assertEquals(Vec3.ZERO, cn.academy.ability.vecmanip.DirectedShockSkill
                .knockbackVelocity(new Vec3(1, 2, 3), new Vec3(1, 2, 3)));
    }

    /**
     * L'onde de choc : son energie de chantier, ses pas, et l'axe qu'elle suit.
     */
    @Test
    void lOndeDeChocDepenseSonEnergieLeLongDuRegard() {
        var groundshock = cn.academy.ability.vecmanip.VecmanipCategory.GROUNDSHOCK;

        // La touche se tient, avec un minimum de cinq ticks et aucun maximum : l'original
        // frappait quel que soit le temps tenu.
        assertTrue(groundshock.isChargeable(), "l'onde de choc se charge");
        assertEquals(5, groundshock.getMinChargeTicks(atExperience(groundshock, 0f)),
                "un appui de moins de cinq ticks ne part pas");
        assertEquals(0, groundshock.getMaxChargeTicks(atExperience(groundshock, 0f)),
                "aucun maximum : rien ne se referme pendant qu'on tient");

        // Ses courbes : 60 a 120 d'energie, 4 a 6 degats, 80 a 150 CP (divises par 28),
        // 15 a 10 de surcout, 10 a 25 pas, et 80 a 40 ticks de recharge.
        assertBounds("energie de ground_shock", 60f, 120f,
                data -> (float) groundshock.energy(data), groundshock);
        assertBounds("degats de ground_shock", 4f, 6f, groundshock::damage, groundshock);
        assertBounds("cout de ground_shock", 80f, 150f, groundshock::consumption, groundshock);
        assertBounds("surcout de ground_shock", 15f, 10f, groundshock::overload, groundshock);
        assertBounds("recharge de ground_shock", 80f, 40f, groundshock::cooldown, groundshock);
        assertBounds("butin de ground_shock", 0.3f, 1.0f, groundshock::dropRate, groundshock);
        assertEquals(10, groundshock.maxIterations(atExperience(groundshock, 0f)),
                "dix pas au depart");
        assertEquals(25, groundshock.maxIterations(atExperience(groundshock, 1f)),
                "vingt-cinq au maximum");

        // La poussee verticale : de 0,48 a 1,17 bloc par tick, selon le tirage et
        // l'experience. Les deux bornes se lisent aux extremes des deux facteurs.
        assertEquals(0.48d, groundshock.ySpeed(atExperience(groundshock, 0f), 0.0d), 0.0001d);
        assertEquals(1.17d, groundshock.ySpeed(atExperience(groundshock, 1f), 1.0d), 0.0001d);

        // Tout est verse par le coup, et la recharge aussi : rien ne part d'un appui qui
        // n'a rien fait, pas meme quand le joueur est en l'air.
        assertTrue(groundshock.earnsExpOnEffect(), "l'onde verse son experience elle-meme");
        assertEquals(0f, groundshock.getExpGain(atExperience(groundshock, 0f)), 0.000001f);
        assertEquals(0, groundshock.getCooldownTicks(atExperience(groundshock, 0f)),
                "la recharge est posee par le coup, pas par l'activation");
        assertEquals(0f, groundshock.getCpCost(), 0.000001f,
                "aucun cout a l'appui : le prix se paie dans l'effet, apres le controle du sol");
        // Et le paquet ne paie rien non plus, sans quoi il paierait le surcout avant que
        // l'onde ait pu constater qu'elle ne part pas — voir Skill#paysOnEffect.
        assertTrue(groundshock.paysOnEffect(), "l'onde de choc se paie elle-meme");
    }

    /**
     * L'axe de l'onde, et ses cinq colonnes.
     *
     * Le regard est ramene a l'horizontale : viser le sol plus loin ne change pas la
     * longueur du chemin. Les colonnes, elles, se lisent sur la perpendiculaire — et cette
     * perpendiculaire <b>garde la composante verticale du regard</b>, comme l'original.
     */
    @Test
    void lOndeDeChocAvanceSurLeLacetEtOuvreCinqColonnes() {
        // Regard vers +Z, un peu vers le sol : la marche reste horizontale.
        Vec3 sought = cn.academy.ability.vecmanip.GroundshockSkill.walkDirection(
                new Vec3(0, -0.5, 1));
        assertEquals(0.0, sought.y, 0.0001, "l'onde ne monte pas avec le regard");
        assertEquals(1.0, sought.length(), 0.0001, "et sa direction est unitaire");
        assertEquals(new Vec3(0, 0, 1), round(sought), "viser vers le bas ne devie pas la marche");

        // Regard vers l'est : la marche suit le lacet, pas le tangage.
        assertEquals(new Vec3(1, 0, 0), round(cn.academy.ability.vecmanip.GroundshockSkill
                .walkDirection(new Vec3(1, 1, 0))));

        // Regard pile a la verticale : plus d'horizon du tout. L'original levait une
        // exception dans son marcheur ; le port prend le nord, comme ses autres
        // competences qui se posent la meme question.
        assertEquals(new Vec3(0, 0, 1), cn.academy.ability.vecmanip.GroundshockSkill
                .walkDirection(new Vec3(0, -1, 0)));
        assertEquals(new Vec3(0, 0, 1), cn.academy.ability.vecmanip.GroundshockSkill
                .walkDirection(new Vec3(0, 1, 0)));

        // Les cinq colonnes, pour un regard horizontal vers +Z : le centre, puis un bloc
        // de chaque cote, puis deux.
        Vec3 south = new Vec3(0, 0, 1);
        BlockPos row = new BlockPos(10, 5, 10);
        assertEquals(new BlockPos(10, 5, 10), lateral(row, south, 0), "colonne du centre");
        assertEquals(new BlockPos(11, 5, 10), lateral(row, south, 1), "premiere colonne a droite");
        assertEquals(new BlockPos(9, 5, 10), lateral(row, south, 2), "premiere colonne a gauche");
        assertEquals(new BlockPos(12, 5, 10), lateral(row, south, 3), "deuxieme a droite");
        assertEquals(new BlockPos(8, 5, 10), lateral(row, south, 4), "deuxieme a gauche");

        // Regard vers l'est : les colonnes sont au nord et au sud du marcheur.
        Vec3 east = new Vec3(1, 0, 0);
        assertEquals(new BlockPos(10, 5, 9), lateral(row, east, 1),
                "colonne a gauche quand on regarde l'est");
        assertEquals(new BlockPos(10, 5, 11), lateral(row, east, 2),
                "et l'autre de l'autre cote");

        // Le regard baisse fait descendre les colonnes : la perpendicularite est prise dans
        // les trois coordonnees, comme l'original, et non dans le plan horizontal. C'est
        // aussi pour cela que la marche, elle, est ramenee a l'horizontale : les deux
        // lectures du regard sont bien distinctes.
        Vec3 tilted = new Vec3(0, -1, 1).normalize();
        assertEquals(new BlockPos(10, 4, 10), lateral(row, tilted, 1),
                "un regard a 45 degres vers le sol descend la colonne d'un bloc");

        // Et les chances de chaque colonne : le centre toujours, les bords une fois sur trois.
        assertEquals(1.0, cn.academy.ability.vecmanip.GroundshockSkill.LATERAL_CHANCES[0]);
        assertEquals(0.7, cn.academy.ability.vecmanip.GroundshockSkill.LATERAL_CHANCES[1]);
        assertEquals(0.3, cn.academy.ability.vecmanip.GroundshockSkill.LATERAL_CHANCES[3]);
    }

    /** La cellule d'une des cinq colonnes, pour alleger le test ci-dessus. */
    private static BlockPos lateral(BlockPos row, Vec3 look, int index) {
        return cn.academy.ability.vecmanip.GroundshockSkill.lateralCell(row, look, index);
    }

    /**
     * L'onde de choc dirigee : ce qu'elle coute, ce qu'elle casse, et ce qu'elle projette.
     */
    @Test
    void lOndeDirigeeCasseSelonSonExperience() {
        var blast = cn.academy.ability.vecmanip.VecmanipCategory.DIRECTED_BLASTWAVE;

        // La meme fenetre de charge que le choc dirige : six ticks au minimum, cinquante
        // au dela desquels le coup est perdu.
        assertTrue(blast.isChargeable(), "l'onde dirigee se charge");
        assertEquals(6, blast.getMinChargeTicks(atExperience(blast, 0f)),
                "un appui de moins de six ticks ne part pas");
        assertEquals(50, blast.getMaxChargeTicks(atExperience(blast, 0f)),
                "au dela de cinquante ticks, le coup est perdu");

        // Ses courbes : 10 a 25 degats, 160 a 200 CP (divises par 28), 50 a 30 de surcout,
        // une chance de casse de 0,5 a 0,8 et un butin de 0,4 a 0,9.
        assertBounds("degats de dir_blast", 10f, 25f, blast::damage, blast);
        assertBounds("cout de dir_blast", 160f, 200f, blast::consumption, blast);
        assertBounds("surcout de dir_blast", 50f, 30f, blast::overload, blast);
        assertBounds("chance de casse", 0.5f, 0.8f, blast::breakProbability, blast);
        assertBounds("butin de dir_blast", 0.4f, 0.9f, blast::dropRate, blast);
        assertBounds("recharge de dir_blast", 80f, 50f, blast::cooldown, blast);

        // La recharge est celle que le paquet pose : elle ne depend pas de ce que l'onde a
        // trouve, contrairement au gain d'experience.
        assertEquals(blast.cooldown(atExperience(blast, 0f)),
                blast.getCooldownTicks(atExperience(blast, 0f)), "recharge posee par le paquet");

        // La durete acceptee : trois paliers, ceux de l'original. A 2,9 l'onde ouvre la
        // pierre (1,5) et la pierre taillee (2) mais pas l'obsidienne (50) ; a 25, l'objet
        // de fer y passe aussi ; a 55, plus rien de cassable ne lui resiste.
        assertEquals(2.9f, cn.academy.ability.vecmanip.DirectedBlastwaveSkill.breakHardness(0f),
                0.0001f, "depart");
        assertEquals(2.9f, cn.academy.ability.vecmanip.DirectedBlastwaveSkill.breakHardness(0.24f),
                0.0001f, "juste avant le premier palier");
        assertEquals(25f, cn.academy.ability.vecmanip.DirectedBlastwaveSkill.breakHardness(0.25f),
                0.0001f, "premier palier");
        assertEquals(25f, cn.academy.ability.vecmanip.DirectedBlastwaveSkill.breakHardness(0.49f),
                0.0001f, "juste avant le second");
        assertEquals(55f, cn.academy.ability.vecmanip.DirectedBlastwaveSkill.breakHardness(0.5f),
                0.0001f, "second palier");
        assertEquals(55f, cn.academy.ability.vecmanip.DirectedBlastwaveSkill.breakHardness(1f),
                0.0001f, "a la maitrise");

        // L'experience suit ce que la vague a trouve, et c'est elle qui se verse :
        // 0,0025 si elle a projete quelqu'un, 0,0012 sinon.
        assertEquals(0.0025f, cn.academy.ability.vecmanip.DirectedBlastwaveSkill.expGain(true),
                0.000001f, "une vague qui a trouve quelqu'un");
        assertEquals(0.0012f, cn.academy.ability.vecmanip.DirectedBlastwaveSkill.expGain(false),
                0.000001f, "une vague dans le vide");
        assertTrue(blast.earnsExpOnEffect(), "l'onde verse son experience elle-meme");
        assertEquals(0f, blast.getExpGain(atExperience(blast, 0f)), 0.000001f,
                "rien au declenchement : le paquet ne sait pas ce qui a ete touche");
    }

    /**
     * Le cube de l'onde dirigee, et son asymetrie.
     *
     * L'original ecrivait sa boucle {@code (x - 3) until (x + 3)} : la borne haute exclue,
     * le cube va donc de moins trois a <b>plus deux</b>. Ce n'est pas un detail de style —
     * le relief laisse derriere est ampute d'un bloc d'un cote — et le port a garde la
     * borne telle quelle, plutot que de la corriger en silence.
     */
    @Test
    void leCubeDeLOndeDirigeeEstAsymetrique() {
        assertEquals(-3, cn.academy.ability.vecmanip.DirectedBlastwaveSkill.BLAST_LOW);
        assertEquals(2, cn.academy.ability.vecmanip.DirectedBlastwaveSkill.BLAST_HIGH);
        assertEquals(3, cn.academy.ability.vecmanip.DirectedBlastwaveSkill.BLAST_RANGE,
                "la portee sur les entites, elle, est bien de trois blocs de chaque cote");
        assertEquals(6, cn.academy.ability.vecmanip.DirectedBlastwaveSkill.BLAST_RADIUS_SQ);
        assertEquals(4.0, cn.academy.ability.vecmanip.DirectedBlastwaveSkill.REACH,
                "le rayon de visee");
    }

    /**
     * Le nombre d'anneaux de l'onde dirigee : <b>deux</b>, et jamais trois.
     *
     * <p>L'original tirait {@code rangei(2, 3)}, qui rend deux — sa borne haute est exclue. Le port
     * en tirait deux ou trois, et cette troisieme pile allait trop loin : elle s'ouvrait a un bloc
     * et demi de plus, soit pres de six blocs devant les yeux, alors que la visee n'en porte que
     * quatre. Le joueur l'a vu : « les ondes vont trop loin du joueur quand on tape dans le vide
     * [...] l'impression qu'on a une tres grande portee ».
     */
    @Test
    void lOndeDirigeeNOuvreQueDeuxAnneaux() {
        for (long seed = 0; seed < 64; seed++) {
            assertEquals(2, cn.academy.ability.vecmanip.DirectedBlastwaveSkill.ringCount(
                            net.minecraft.util.RandomSource.create(seed)),
                    "rangei(2, 3) rend deux, quel que soit le tirage");
        }
    }

    /**
     * Le retour de sang : le plus court rayon du port, pour le plus gros coup.
     */
    @Test
    void leRetourDeSangFrappeFortEtDePres() {
        var blood = cn.academy.ability.vecmanip.VecmanipCategory.BLOOD_RETROGRADE;

        // Deux blocs de contact, aucun minimum de charge et aucun maximum : la touche part
        // des qu'elle trouve quelque chose.
        assertEquals(2.0, cn.academy.ability.vecmanip.BloodRetrogradeSkill.REACH,
                "la portee du contact");
        assertTrue(blood.isChargeable(), "le retour de sang se charge");
        assertEquals(0, blood.getMinChargeTicks(atExperience(blood, 0f)),
                "aucun minimum : un appui suffit quand la main touche");
        assertEquals(0, blood.getMaxChargeTicks(atExperience(blood, 0f)),
                "et aucun maximum : l'original s'arretait a trente ticks sans que rien n'en depende");

        // Ses courbes : 30 a 60 degats, 280 a 350 CP (divises par 28), 55 a 40 de surcout,
        // et 90 a 40 ticks de recharge.
        assertBounds("degats de blood_retro", 30f, 60f, blood::damage, blood);
        assertBounds("cout de blood_retro", 280f, 350f, blood::consumption, blood);
        assertBounds("surcout de blood_retro", 55f, 40f, blood::overload, blood);
        assertBounds("recharge de blood_retro", 90f, 40f, blood::cooldown, blood);

        // Rien n'est declare au paquet, et pourtant le surcout non plus n'est pas nul : le
        // prix se paie dans l'effet, apres le contact — voir Skill#paysOnEffect.
        assertTrue(blood.paysOnEffect(), "le contact se paie lui-meme");
        assertEquals(0f, blood.getCpCost(), 0.000001f, "rien a l'activation");
        assertEquals(0, blood.getCooldownTicks(atExperience(blood, 0f)),
                "et rien non plus en recharge : c'est le contact qui la pose");
        assertTrue(blood.earnsExpOnEffect(), "l'experience est versee par le contact");
        assertEquals(0f, blood.getExpGain(atExperience(blood, 0f)), 0.000001f);
    }

    /**
     * La deviation de vecteur : ce qu'elle coute, et ce qu'elle epargne.
     */
    @Test
    void laDeviationCouteParTickEtEpargneDesDegats() {
        var deviation = cn.academy.ability.vecmanip.VecmanipCategory.VEC_DEVIATION;

        // Une competence tenue, sans duree : elle tient tant que la reserve suit.
        assertTrue(deviation.isHeld(), "la deviation se tient");
        assertEquals(0, deviation.getMaxHoldTicks(atExperience(deviation, 0f)),
                "aucune duree programmee");

        // Ses courbes : entretien 0,46 a 0,18 CP par tick (13 a 5 chez l'original, divises
        // par 28), surcout epingle 80 a 50, et 15 a 12 de surcout par entite arretee.
        assertBounds("entretien de vec_deviation", 13f, 5f, deviation::tickCost, deviation);
        assertBounds("epingle de vec_deviation", 80f, 50f, deviation::pin, deviation);
        assertBounds("cout par entite de vec_deviation", 15f, 12f, deviation::entityCost, deviation);

        // La reduction : de 40 % a 90 % des degats, payee 0,54 a 0,43 CP par coup encaisse,
        // mais jamais plus que ce qu'il reste en reserve.
        assertBounds("reduction de vec_deviation", 0.4f, 0.9f, deviation::reduction, deviation);
        assertBounds("cout de la reduction", 15f, 12f, deviation::resistCost, deviation);

        // La reserve borne la depense, dans les deux sens : avec de quoi payer, un coup
        // encaisse coute le prix du palier et pas la reserve entiere ; a sec, il ne coute
        // que ce qu'il reste — c'est le `min` de l'original, et c'est ce qui fait qu'un
        // dernier coup encaisse ne laisse pas de dette.
        AbilityData rich = atExperience(deviation, 0f);
        assertEquals(15f, deviation.resistCharge(rich), 0.0001f,
                "avec de quoi payer, le coup coute le prix du palier");

        AbilityData poor = atExperience(deviation, 0f);
        assertTrue(poor.consumeControlPoint(poor.getControlPoint()),
                "vider la reserve doit marcher");
        assertEquals(0f, poor.getControlPoint(), 0.0001f, "la reserve doit etre vide");
        assertEquals(0f, deviation.resistCharge(poor), 0.0001f,
                "une reserve vide ne paie rien du tout");

        // Le prix d'ouverture est le surcout epingle, et rien d'autre ; la recharge, elle,
        // n'existe pas : c'est un maintien.
        assertEquals(0f, deviation.getCpCost(), 0.000001f, "aucun cout en reserve a l'ouverture");
        assertEquals(deviation.pin(atExperience(deviation, 0f)),
                deviation.getOverloadCost(atExperience(deviation, 0f)), 0.0001f,
                "et le surcout epingle pour seul prix");
        assertEquals(0, deviation.getCooldownTicks(atExperience(deviation, 0f)),
                "aucune recharge : le maintien se termine et se reprend");
        assertTrue(deviation.earnsExpOnEffect(), "tout est verse par ce que la veille arrete");
        assertEquals(0f, deviation.getExpGain(atExperience(deviation, 0f)), 0.000001f);
    }

    /**
     * La reflexion de vecteur : la soeur de la deviation, mais elle <b>renvoie</b> au lieu
     * d'arreter.
     */
    @Test
    void laReflexionRenvoieCeQuiVoleEtRendLesCoups() {
        var reflection = cn.academy.ability.vecmanip.VecmanipCategory.VEC_REFLECTION;

        // Une competence tenue, sans duree ni recharge : elle tient tant que la reserve suit,
        // comme la veille de la deviation dont elle descend.
        assertTrue(reflection.isHeld(), "la reflexion se tient");
        assertFalse(reflection.isPassive(), "ce n'est plus une passive");
        assertEquals(0, reflection.getMaxHoldTicks(atExperience(reflection, 0f)),
                "aucune duree programmee");
        assertEquals(0, reflection.getCooldownTicks(atExperience(reflection, 0f)),
                "aucune recharge : un maintien se termine et se reprend");

        // Ses courbes : entretien 15 a 11 CP par tick, surcout epingle 350 a 250 (verbatim),
        // et 300 a 160 CP par entite renvoyee.
        assertBounds("entretien de vec_reflection", 15f, 11f, reflection::tickCost, reflection);
        assertBounds("epingle de vec_reflection", 350f, 250f, reflection::pin, reflection);
        assertBounds("cout d'une entite renvoyee",
                300f, 160f, d -> reflection.entityCost(d, 1f), reflection);

        // La difficulte multiplie ce qu'une entite coute, comme elle multiplie ce qu'elle
        // rapporte : une potion (1,4) coute plus cher qu'une fleche (1,0).
        assertBounds("cout d'une potion",
                300f * 1.4f, 160f * 1.4f, d -> reflection.entityCost(d, 1.4f), reflection);
        assertEquals(reflection.entityCost(atExperience(reflection, 0f), 0f), 0f, 0.0001f,
                "et ce que la config ne connait pas ne coute rien");

        // Les coups : la part renvoyee va de 60 % a 120 %, et son prix suit ce qu'elle rend
        // — 20 a 15 CP par point de degats.
        assertBounds("part renvoyee", 0.6f, 1.2f, reflection::reflectRatio, reflection);
        assertBounds("prix d'un coup de 10 points",
                200f, 150f, d -> reflection.damageCost(d, 10f), reflection);

        // Le prix d'ouverture est le surcout epingle, et rien d'autre.
        assertEquals(0f, reflection.getCpCost(), 0.000001f, "aucun cout en reserve a l'ouverture");
        assertEquals(reflection.pin(atExperience(reflection, 0f)),
                reflection.getOverloadCost(atExperience(reflection, 0f)), 0.0001f,
                "et le surcout epingle pour seul prix");
        assertTrue(reflection.earnsExpOnEffect(), "tout est verse par la veille et par ses renvois");
        assertEquals(0f, reflection.getExpGain(atExperience(reflection, 0f)), 0.000001f);
    }

    @Test
    void lesAilesDeTempeteCoutentLeVolEtRienDAutre() {
        var wing = cn.academy.ability.vecmanip.VecmanipCategory.STORM_WING;

        // Un maintien sans duree, qui se sert des touches de deplacement : c'est avec elles
        // qu'on vole, comme le scintillement saute avec elles.
        assertTrue(wing.isHeld(), "les ailes se tiennent");
        assertEquals(0, wing.getMaxHoldTicks(atExperience(wing, 0f)), "sans duree programmee");
        assertTrue(wing.listensToDirections(), "le geste se sert des quatre touches");
        assertTrue(wing.earnsExpOnEffect(), "le vol verse son experience lui-meme");
        assertEquals(0f, wing.getExpGain(atExperience(wing, 0f)), 0.000001f,
                "rien a l'ouverture : la charge ne rapporte rien");

        // La charge : 70 ticks au depart, 30 au maximum, et gratuite.
        assertBounds("charge des ailes", 70f, 30f, wing::chargeTime, wing);
        assertEquals(0f, wing.getCpCost(), 0.000001f, "aucun cout en reserve a l'ouverture");

        // Le vol : 40 a 25 CP par tick (divises par 28) et 10 a 7 de surcout.
        assertBounds("cout du vol", 40f, 25f, wing::consumption, wing);
        assertBounds("surcout du vol", 10f, 7f, wing::overload, wing);
        assertEquals(wing.overload(atExperience(wing, 0f)),
                wing.getOverloadCost(atExperience(wing, 0f)), 0.0001f,
                "le surcout d'ouverture est le premier tick de vol");

        // La vitesse : deux regimes separes par 45 % d'experience, et la recharge qui suit.
        assertBounds("vitesse des ailes", 1.4f, 3.6f, wing::speed, wing);
        assertBounds("recharge des ailes", 30f, 10f, d -> (float) wing.getCooldownTicks(d), wing);

        // Les ailes maladroites, et le souffle de l'ouverture : sous 15 % d'experience elles
        // cassent ce qu'elles trouvent, et a pleine experience elles repoussent.
        assertTrue(wing.clumsy(atExperience(wing, 0f)), "un debutant casse tout");
        assertFalse(wing.clumsy(atExperience(wing, 0.2f)), "et un peu d'experience suffit");
        assertTrue(wing.blows(atExperience(wing, 1f)), "a pleine experience elles repoussent");
        assertFalse(wing.blows(atExperience(wing, 0.9f)), "et pas avant");
    }

    /**
     * Le canon a plasma : la charge la plus chere du port, et sa plus longue recharge.
     */
    @Test
    void leCanonAPlasmaSeChargePuisPoseUneRechargeDeCinquanteSecondes() {
        var cannon = cn.academy.ability.vecmanip.VecmanipCategory.PLASMA_CANNON;

        assertTrue(cannon.isHeld(), "le canon se tient");
        assertEquals(0, cannon.getMaxHoldTicks(atExperience(cannon, 0f)), "sans duree programmee");
        assertTrue(cannon.earnsExpOnEffect(), "l'experience se verse au tir");
        assertEquals(0f, cannon.getExpGain(atExperience(cannon, 0f)), 0.000001f);

        // La charge raccourcit et coute plus cher par tick : 60 a 30 ticks, 18 a 25 CP.
        assertBounds("charge du canon", 60f, 30f, cannon::chargeTime, cannon);
        assertBounds("cout de la charge", 18f, 25f, cannon::chargeCost, cannon);

        // Le surcout epingle : 500 a 400, verbatim, et rien a l'appui en reserve.
        assertBounds("epingle du canon", 500f, 400f, cannon::pin, cannon);
        assertEquals(0f, cannon.getCpCost(), 0.000001f, "aucun cout en reserve a l'ouverture");
        assertEquals(cannon.pin(atExperience(cannon, 0f)),
                cannon.getOverloadCost(atExperience(cannon, 0f)), 0.0001f,
                "et le surcout epingle pour seul prix");

        // L'explosion : 80 a 150 points de degats, 12 a 15 de puissance.
        assertBounds("degats du canon", 80f, 150f, cannon::damage, cannon);
        assertBounds("puissance du canon", 12f, 15f, cannon::power, cannon);

        // Et la recharge la plus longue du port : 50 secondes a zero, 30 au maximum.
        assertBounds("recharge du canon", 1000f, 600f,
                d -> (float) cannon.getCooldownTicks(d), cannon);
    }

    /**
     * La detection de minerais : la seule competence du port qui ne fait rien au monde.
     */
    @Test
    void laDetectionDeMineraisPaieSonRegard() {
        var detect = cn.academy.ability.electromaster.ElectromasterCategory.MINE_DETECT;

        // Rien n'est declare au paquet : l'eclat decide, paie, et pose sa recharge — sinon
        // un appui sans reserve couterait une attente pour un regard qui n'a pas eu lieu.
        assertTrue(detect.paysOnEffect(), "c'est l'effet qui paie");
        assertTrue(detect.earnsExpOnEffect(), "et qui verse l'experience");
        assertEquals(0f, detect.getExpGain(atExperience(detect, 0f)), 0.000001f);
        assertEquals(0f, detect.getCpCost(), 0.000001f, "aucun cout en reserve a l'appui");
        assertEquals(0, detect.getCooldownTicks(atExperience(detect, 0f)),
                "et aucune recharge posee par le paquet");

        // Ce qu'il paie : 1500 a 1000 CP divises par 28, et 200 a 180 de surcout. Le prix
        // baisse quand la portee grandit, ce qui est l'envers des habitudes.
        assertBounds("cout de l'eclat", 1500f, 1000f, detect::consumption, detect);
        assertBounds("surcout de l'eclat", 200f, 180f, detect::overload, detect);
        assertBounds("portee de l'eclat", 15f, 30f, detect::range, detect);

        // Quarante-cinq secondes d'attente au depart, vingt au maximum : c'est la deuxieme
        // plus longue du port, apres le canon a plasma.
        assertBounds("recharge de l'eclat", 900f, 400f, d -> (float) detect.cooldown(d), detect);

        // L'eclat complet, lui, se mesure ailleurs : voir MineDetectTest.
        assertFalse(detect.advanced(atExperience(detect, 0f), 0), "un novice ne l'a pas");
    }

    /**
     * La manipulation d'un bloc : ce que coute un lancer, et rien avant.
     */
    @Test
    void laManipulationDUnBlocPaieSonLancer() {
        var manip = cn.academy.ability.electromaster.ElectromasterCategory.MAG_MANIP;

        // Une competence tenue, qui ne paie rien tant que le bloc est tenu : c'est le lancer
        // qui decide, comme la detection de minerais.
        assertTrue(manip.isHeld(), "le bloc se tient");
        assertEquals(0, manip.getMaxHoldTicks(atExperience(manip, 0f)), "sans duree programmee");
        assertTrue(manip.paysOnEffect(), "c'est le lancer qui paie");
        assertTrue(manip.earnsExpOnEffect(), "et qui verse l'experience");
        assertEquals(0f, manip.getExpGain(atExperience(manip, 0f)), 0.000001f);
        assertEquals(0f, manip.getCpCost(), 0.000001f, "aucun cout en reserve a l'appui");
        assertEquals(0, manip.getCooldownTicks(atExperience(manip, 0f)),
                "et aucune recharge posee par le paquet");

        // Le lancer : 140 a 270 CP divises par 28, et 35 a 20 de surcout.
        assertBounds("cout du lancer", 140f, 270f, manip::consumption, manip);
        assertBounds("surcout du lancer", 35f, 20f, manip::overload, manip);

        // Sa vitesse, et la recharge qu'il pose : 60 ticks au depart, 40 au maximum.
        assertBounds("vitesse du lancer", 0.5f, 1.0f, d -> (float) manip.speed(d), manip);
        assertBounds("recharge du lancer", 60f, 40f, d -> (float) manip.cooldown(d), manip);
    }

    /** Arrondi d'un vecteur de direction, pour comparer sans se battre avec les arrondis. */
    private static Vec3 round(Vec3 v) {
        return new Vec3(Math.round(v.x * 1000) / 1000.0, Math.round(v.y * 1000) / 1000.0,
                Math.round(v.z * 1000) / 1000.0);
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
        // Les passives n'en ont jamais eu, et les maintiens non plus : leur fin est leur
        // propre fin, pas une recharge a attendre.
        assertEquals(0, cooldownOf(cn.academy.ability.vecmanip.VecmanipCategory.VEC_REFLECTION));
        assertEquals(0, cooldownOf(cn.academy.ability.vecmanip.VecmanipCategory.VEC_DEVIATION));
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

    @Test
    void lArcPaieSelonSonExperience() {
        // Le prix de l'arc n'est PAS un nombre fixe : `ArcGen` de l'original calcule
        // `cp = lerpf(30, 70, exp)` et `overload = lerpf(18, 11, exp)`. Les deux
        // varient donc avec ce que la competence a appris, et grandissent en sens
        // INVERSE : plus l'arc sait faire, plus il coute cher en reserve et moins il
        // charge la surcharge.
        var arcGen = cn.academy.ability.electromaster.ElectromasterCategory.ARC_GEN;

        assertEquals(30f, arcGen.getCpCost(atExperience(arcGen, 0f)), 0.0001f, "CP au depart");
        assertEquals(50f, arcGen.getCpCost(atExperience(arcGen, 0.5f)), 0.0001f, "CP a moitie");
        assertEquals(70f, arcGen.getCpCost(atExperience(arcGen, 1f)), 0.0001f, "CP au maximum");

        assertEquals(18f, arcGen.getOverloadCost(atExperience(arcGen, 0f)), 0.0001f,
                "surcout au depart");
        assertEquals(14.5f, arcGen.getOverloadCost(atExperience(arcGen, 0.5f)), 0.0001f,
                "surcout a moitie");
        assertEquals(11f, arcGen.getOverloadCost(atExperience(arcGen, 1f)), 0.0001f,
                "surcout au maximum");
    }

    @Test
    void lArcRapporteSelonCeQuIlTouche() {
        // L'original donnait deux montants d'experience, et rien du tout quand son rayon
        // ne rencontrait rien : 0,48 % a 0,72 % de la barre pour un etre vivant, 0,18 % a
        // 0,27 % pour un BLOC — le cas courant, on tire sur un mur — et zero dans le vide.
        // Le port versait le montant du coup au but a chaque appui, depuis le paquet, qui
        // ne savait pas encore ce que l'arc allait toucher. C'est l'effet qui le verse
        // maintenant, et ce test fige les deux montants.
        var arcGen = cn.academy.ability.electromaster.ElectromasterCategory.ARC_GEN;

        assertEquals(0f, arcGen.getExpGain(atExperience(arcGen, 0.5f)), 0.000001f,
                "le paquet ne verse plus rien : l'effet s'en charge");
        assertTrue(arcGen.earnsExpOnEffect(), "et il le declare");

        assertEquals(0.0048f, arcGen.hitExp(atExperience(arcGen, 0f)), 0.000001f);
        assertEquals(0.006f, arcGen.hitExp(atExperience(arcGen, 0.5f)), 0.000001f);
        assertEquals(0.0072f, arcGen.hitExp(atExperience(arcGen, 1f)), 0.000001f);

        assertEquals(0.0018f, arcGen.blockExp(atExperience(arcGen, 0f)), 0.000001f);
        assertEquals(0.00225f, arcGen.blockExp(atExperience(arcGen, 0.5f)), 0.000001f);
        assertEquals(0.0027f, arcGen.blockExp(atExperience(arcGen, 1f)), 0.000001f);

        assertTrue(arcGen.blockExp(atExperience(arcGen, 0f))
                        < arcGen.hitExp(atExperience(arcGen, 0f)),
                "un mur rapporte moins qu'une cible vivante");
    }
}
