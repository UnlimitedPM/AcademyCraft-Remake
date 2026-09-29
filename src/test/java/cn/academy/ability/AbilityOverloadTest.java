package cn.academy.ability;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le surcout, seconde ressource des competences.
 *
 * Portage de {@code CPData} : en plus des CP, chaque activation charge une reserve qui
 * se remplit, bloque tout quand elle est pleine, et redescend apres un delai. Trois
 * choses valent la peine d'etre figees ici : le plafond suit le niveau, la reserve
 * grandit a l'usage, et la recuperation attend son delai avant de commencer.
 */
class AbilityOverloadTest {

    private static final class TestCategory extends Category {
        TestCategory(String name) {
            super(name);
        }
    }

    private static final class DummySkill extends Skill {
        DummySkill(String name) {
            super(name, 1);
        }
    }

    /** Un joueur dont l'aptitude est au niveau demande. */
    private static AbilityData atLevel(int level) {
        AbilityData data = new AbilityData();
        data.setCategoryLevel(new TestCategory("test"), level);
        return data;
    }

    @Test
    void laReserveDeSurcoutSuitLeNiveau() {
        // Table init_overload de l'original, indexee par le niveau de l'aptitude.
        assertEquals(100f, AbilityData.baseMaxOverload(0), 0.0001f);
        assertEquals(100f, AbilityData.baseMaxOverload(1), 0.0001f);
        assertEquals(150f, AbilityData.baseMaxOverload(2), 0.0001f);
        assertEquals(240f, AbilityData.baseMaxOverload(3), 0.0001f);
        assertEquals(350f, AbilityData.baseMaxOverload(4), 0.0001f);
        assertEquals(500f, AbilityData.baseMaxOverload(5), 0.0001f);

        assertEquals(100f, atLevel(1).getMaxOverload(), 0.0001f);
        assertEquals(350f, atLevel(4).getMaxOverload(), 0.0001f);
    }

    @Test
    void unNiveauHorsBornesNeDebordePasDeLaTable() {
        assertEquals(100f, AbilityData.baseMaxOverload(-3), 0.0001f);
        assertEquals(500f, AbilityData.baseMaxOverload(9), 0.0001f);
    }

    @Test
    void leNiveauLePlusHautDonneLaReserve() {
        // L'original n'avait qu'une aptitude ; le port prend la meilleure categorie,
        // sinon un joueur ayant monte une aptitude au niveau 4 serait penalise par la
        // categorie qu'il delaisse.
        AbilityData data = new AbilityData();
        data.setCategoryLevel(new TestCategory("basse"), 1);
        data.setCategoryLevel(new TestCategory("haute"), 4);

        assertEquals(4, data.getHighestLevel());
        assertEquals(350f, data.getMaxOverload(), 0.0001f);
    }

    @Test
    void uneReserveVideNAPasDeSurcout() {
        AbilityData data = atLevel(1);

        assertEquals(0f, data.getOverload(), 0.0001f);
        assertEquals(0f, data.getAddMaxOverload(), 0.0001f);
        assertFalse(data.isOverloaded());
        assertEquals(0, data.getUntilOverloadRecover());
    }

    @Test
    void activerDepenseLesDeuxRessources() {
        AbilityData data = atLevel(1);
        float cp = data.getControlPoint();

        assertTrue(data.perform(20f, 18f));

        assertEquals(cp - 20f, data.getControlPoint(), 0.0001f);
        assertEquals(18f, data.getOverload(), 0.0001f);
        // La recuperation est armee par l'activation : rien ne redescend avant le
        // delai de l'original, soit 32 ticks.
        assertEquals(32, data.getUntilOverloadRecover());
    }

    @Test
    void uneActivationRefuseeNAjoutePasDeSurcout() {
        AbilityData data = atLevel(1);
        // Une reserve a cinq points : de quoi refuser un cout de 20, et pas de quoi payer.
        data.consumeControlPoint(data.getControlPoint() - 5f);
        float cp = data.getControlPoint();

        // Portage de CPData.perform : les deux ressources ensemble, ou aucune. Sans
        // cette atomicite, une competence refusee faute de CP laisserait du surcout
        // derriere elle — le joueur paierait pour un tir qui n'a pas eu lieu.
        assertFalse(data.perform(20f, 50f));
        assertEquals(cp, data.getControlPoint(), 0.0001f);
        assertEquals(0f, data.getOverload(), 0.0001f);
        assertEquals(0, data.getUntilOverloadRecover());
    }

    @Test
    void leSurcoutEstPlafonneALaReserve() {
        AbilityData data = atLevel(1);

        data.perform(0f, 5000f);

        assertEquals(100f, data.getOverload(), 0.0001f);
    }

    @Test
    void uneReservePleineMetEnSurcharge() {
        AbilityData data = atLevel(1);

        data.perform(0f, 500f);

        assertTrue(data.isOverloadRecovering(), "une reserve pleine doit bloquer les competences");
        // Et le temoin la montre : le delai de recuperation vient d'etre arme.
        assertTrue(data.isOverloaded(), "le temoin doit afficher la surcharge des qu'elle est atteinte");
    }

    @Test
    void laSurchargeMontreeSEffaceAvantLaFinDeLaDescente() {
        AbilityData data = atLevel(1);
        data.perform(0f, 500f);

        // Le delai de recuperation s'ecoule : c'est le temps pendant lequel l'original
        // affichait la surcharge. Une fois passe il ne la montrait plus — la bande refluait vers
        // la gauche — alors que le verrou, lui, tenait encore.
        for (int i = 0; i < 32; i++) {
            data.tickOverload();
        }

        assertTrue(data.getOverload() > 0f, "la reserve n'a pas encore commence a redescendre");
        assertFalse(data.isOverloaded(), "l'etat montre doit s'effacer des le delai ecoule");
        assertTrue(data.isOverloadRecovering(), "mais le verrou des competences tient toujours");

        for (int i = 0; i < 400 && data.getOverload() > 0f; i++) {
            data.tickOverload();
            // Le dernier tick de la descente vide la reserve : c'est LA que le verrou se
            // releve, comme chez l'original.
            if (data.getOverload() > 0f) {
                assertTrue(data.isOverloadRecovering(), "le verrou tient pendant toute la descente");
            }
        }

        assertEquals(0f, data.getOverload(), 0.0001f);
        assertFalse(data.isOverloadRecovering(), "la reserve videe rend ses competences");
        assertFalse(data.isOverloaded());
    }

    @Test
    void laRecuperationAttendSonDelaiAvantDeCommencer() {
        AbilityData data = atLevel(1);
        data.perform(0f, 50f);

        for (int i = 0; i < 32; i++) {
            data.tickOverload();
        }

        assertEquals(50f, data.getOverload(), 0.0001f,
                "rien ne doit redescendre pendant le delai");
        assertEquals(0, data.getUntilOverloadRecover());

        data.tickOverload();

        assertTrue(data.getOverload() < 50f, "la reserve doit commencer a redescendre");
        assertTrue(data.getOverload() > 49f, "et d'un seul tick de recuperation");
    }

    @Test
    void laSurchargeSeLeveQuandLaReserveEstVide() {
        AbilityData data = atLevel(1);
        data.perform(0f, 500f);

        for (int i = 0; i < 400; i++) {
            data.tickOverload();
        }

        assertEquals(0f, data.getOverload(), 0.0001f);
        assertFalse(data.isOverloaded(), "la reserve vide doit rendre ses competences au joueur");
        assertFalse(data.isOverloadRecovering());
    }

    @Test
    void laRecuperationAccelereSurUneReservePresqueVide() {
        // Niveau 0 : la reserve ne grandit pas a l'usage (add_overload y vaut 0), donc
        // le plafond reste 100 et la formule se lit a l'unite.
        AbilityData data = atLevel(0);

        // Portage de getOverloadRecoverSpeed sur une reserve de 100 points : 0,7 point
        // par tick quand elle est vide, 0,6125 a moitie pleine.
        assertEquals(0.7f, data.getOverloadRecoverSpeed(), 0.0001f);
        data.perform(0f, 50f);
        assertEquals(0.6125f, data.getOverloadRecoverSpeed(), 0.0001f);
    }

    @Test
    void lUsageAgranditLaReserve() {
        AbilityData data = atLevel(5);

        data.perform(0f, 100f);

        // maxo_incr_rate de l'original : 0,0058 par point de surcout consomme.
        assertEquals(0.58f, data.getAddMaxOverload(), 0.0001f);
        assertEquals(500.58f, data.getMaxOverload(), 0.0001f);
    }

    @Test
    void lUsageNEtAgranditPasLaReserveDePlusDeDixPointsALAFois() {
        AbilityData data = atLevel(5);

        data.perform(0f, 5000f);

        assertEquals(10f, data.getAddMaxOverload(), 0.0001f);
    }

    @Test
    void laReserveNeGranditPasAuDelaDeSonNiveau() {
        AbilityData data = atLevel(1);

        for (int i = 0; i < 10; i++) {
            data.perform(0f, 5000f);
        }

        // Table add_overload : 40 points de plus au niveau 1, pas un de plus.
        assertEquals(40f, data.getAddMaxOverload(), 0.0001f);
        assertEquals(140f, data.getMaxOverload(), 0.0001f);
    }

    @Test
    void unSurcoutNulNArmePasLaRecuperation() {
        AbilityData data = atLevel(1);

        assertTrue(data.perform(10f, 0f));

        assertEquals(0, data.getUntilOverloadRecover(),
                "une competence sans surcout ne doit pas lancer la recuperation");
        assertEquals(0f, data.getOverload(), 0.0001f);
    }

    @Test
    void leSurcoutSurvitAUneSauvegarde() {
        AbilityData data = atLevel(2);
        data.perform(0f, 60f);

        CompoundTag tag = data.serializeNBT();
        AbilityData reloaded = new AbilityData();
        reloaded.deserializeNBT(tag);

        assertEquals(60f, reloaded.getOverload(), 0.0001f);
        assertEquals(data.getAddMaxOverload(), reloaded.getAddMaxOverload(), 0.0001f);
        assertEquals(data.getMaxOverload(), reloaded.getMaxOverload(), 0.0001f);
        assertEquals(150f, AbilityData.baseMaxOverload(reloaded.getHighestLevel()), 0.0001f);
        assertFalse(reloaded.isOverloaded());
    }

    @Test
    void uneSauvegardeSansDrapeauResteDisponible() {
        CompoundTag tag = new AbilityData().serializeNBT();
        // Une sauvegarde d'avant la surcharge : le joueur doit repartir disponible,
        // pas bloque par la valeur par defaut d'un booleen.
        tag.remove("overloadFine");

        AbilityData reloaded = new AbilityData();
        reloaded.deserializeNBT(tag);

        assertFalse(reloaded.isOverloaded());
    }

    @Test
    void copyFromTransfereLeSurcout() {
        AbilityData source = atLevel(4);
        source.perform(0f, 200f);
        AbilityData target = new AbilityData();

        target.copyFrom(source);

        assertEquals(200f, target.getOverload(), 0.0001f);
        assertEquals(source.getAddMaxOverload(), target.getAddMaxOverload(), 0.0001f);
        assertEquals(source.getMaxOverload(), target.getMaxOverload(), 0.0001f);
    }

    @Test
    void leSurcoutNeRedescendPasPendantUnMaintien() {
        DummySkill skill = new DummySkill("tenue");
        AbilityData data = atLevel(1);
        data.perform(0f, 50f);
        data.beginCharge(skill);
        data.setHeldOverload(skill, data.getOverload());

        for (int i = 0; i < 200; i++) {
            data.tickOverload();
        }

        // L'original epinglait le surcout d'un maintien : sans cela, tenir un bouclier
        // rembourserait son cout d'ouverture au bout de quelques secondes, donc les
        // competences tenues finiraient par ne plus rien couter.
        assertEquals(50f, data.getOverload(), 0.0001f);

        data.endCharge(skill);
        for (int i = 0; i < 60; i++) {
            data.tickOverload();
        }

        assertTrue(data.getOverload() < 50f, "la reserve redescend une fois le maintien fini");
    }

    @Test
    void uneCompetenceOrdinaireNAPasDeSurcout() {
        // Le surcout est une seconde ressource, pas une obligation : les competences
        // deja portees qui n'en avaient pas dans l'original n'en ont pas ici non plus.
        DummySkill skill = new DummySkill("ordinaire");

        assertEquals(0f, skill.getOverloadCost(new AbilityData()), 0.0001f);
    }
}
