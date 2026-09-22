package cn.academy.ability.vecmanip;

import cn.academy.ability.AbilityData;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les calculs des ailes de tempete, {@code StormWingSkill}.
 *
 * <p>C'est la partie du vol qui ne demande ni monde ni joueur : le pas d'acceleration, le
 * reperage d'une direction locale par le regard, le flottement, le souffle de l'ouverture, et
 * ce que les ailes cassent. Le reste — la vitesse posee chaque tick — se lit en vol, et c'est
 * le client qui la pose, comme dans l'original.
 *
 * <p>Le reperage merite son test : c'est un enchainement de deux rotations, dont l'une est de
 * sens inverse, et une erreur de signe enverrait le joueur a l'oppose de son regard sans que
 * rien d'autre ne le dise.
 */
class StormWingTest {

    private static final StormWingSkill WING = VecmanipCategory.STORM_WING;
    private static final double EPS = 1.0e-9;

    @Test
    void lePasRapprocheSansDepasser() {
        // Le `move` de l'original : 0,16 par tick, et la valeur voulue quand elle est proche.
        assertEquals(StormWingSkill.ACCEL, StormWingSkill.step(0, 1, StormWingSkill.ACCEL), EPS);
        assertEquals(0.84, StormWingSkill.step(1, 0, StormWingSkill.ACCEL), EPS);
        assertEquals(0.1, StormWingSkill.step(0, 0.1, StormWingSkill.ACCEL), EPS,
                "un ecart plus petit que le pas est simplement comble");
        assertEquals(0.5, StormWingSkill.step(0.5, 0.5, StormWingSkill.ACCEL), EPS);
    }

    @Test
    void laDirectionSuitLeRegard() {
        // Lacet zero : on regarde vers +Z, et la gauche est +X. C'est le repere de Minecraft,
        // et c'est celui dans lequel l'original tournait ses quatre clefs.
        assertVec(new Vec3(0, 0, 1), StormWingSkill.worldSpace(0f, 0f, new Vec3(0, 0, 1)),
                "avant, sans regard particulier");
        assertVec(new Vec3(1, 0, 0), StormWingSkill.worldSpace(0f, 0f, new Vec3(1, 0, 0)),
                "gauche");
        assertVec(new Vec3(-1, 0, 0), StormWingSkill.worldSpace(0f, 0f, new Vec3(-1, 0, 0)),
                "droite");
        assertVec(new Vec3(0, 0, -1), StormWingSkill.worldSpace(0f, 0f, new Vec3(0, 0, -1)),
                "arriere");

        // Un quart de tour a gauche : on regarde vers -X, et l'avant suit.
        assertVec(new Vec3(-1, 0, 0), StormWingSkill.worldSpace(90f, 0f, new Vec3(0, 0, 1)),
                "avant, regard a l'ouest");

        // Le tangage, lui, ne concerne que l'avant et l'arriere : viser ses pieds envoie vers
        // le bas, mais la gauche reste horizontale.
        assertVec(new Vec3(0, -1, 0), StormWingSkill.worldSpace(0f, 90f, new Vec3(0, 0, 1)),
                "avant, regard vers le bas");
        assertVec(new Vec3(1, 0, 0), StormWingSkill.worldSpace(0f, 90f, new Vec3(1, 0, 0)),
                "et la gauche reste horizontale");
    }

    @Test
    void lesQuatreClefsSontCellesDeLOriginal() {
        assertVec(new Vec3(1, 0, 0), StormWingSkill.localDirection(1), "gauche");
        assertVec(new Vec3(-1, 0, 0), StormWingSkill.localDirection(2), "droite");
        assertVec(new Vec3(0, 0, 1), StormWingSkill.localDirection(3), "avant");
        assertVec(new Vec3(0, 0, -1), StormWingSkill.localDirection(4), "arriere");
        assertVec(Vec3.ZERO, StormWingSkill.localDirection(0), "aucune touche");
        assertVec(Vec3.ZERO, StormWingSkill.localDirection(9), "une direction qui n'existe pas");
    }

    @Test
    void onFlotteSansMonterEtOnSePoseSurLeSol() {
        // En l'air, le 0,078 de l'original : il ne compense pas tout a fait la gravite, mais
        // il fait monter doucement.
        assertEquals(StormWingSkill.LIFT + 0.3,
                StormWingSkill.hoverVelocity(false, 0.3), EPS);
        assertTrue(StormWingSkill.hoverVelocity(false, 0.0) > 0.0, "et il fait monter");
        // Sur le sol, la vitesse est posee : c'est ce qui tient le joueur a hauteur au lieu
        // de le faire rebondir.
        assertEquals(StormWingSkill.HOVER, StormWingSkill.hoverVelocity(true, -0.5), EPS);
    }

    @Test
    void leSouffleRepousseSansSoccuperDeLaPortee() {
        // L'original tirait une portee entre 0,9 et 1,2, puis normalisait : le tirage
        // s'annulait. Le port ne prend donc que la direction et la vitesse.
        assertVec(new Vec3(0.6, 0, 0.8),
                StormWingSkill.blastVelocity(new Vec3(0, 0, 0), new Vec3(3, 0, 4), 1.0),
                "l'eloignement, a la vitesse demandee");
        assertEquals(0.5, StormWingSkill.blastVelocity(new Vec3(1, 1, 1),
                new Vec3(5, 1, 1), 0.5).length(), EPS, "la vitesse est celle du tirage");
        assertVec(Vec3.ZERO,
                StormWingSkill.blastVelocity(new Vec3(2, 2, 2), new Vec3(2, 2, 2), 1.0),
                "une cible au meme endroit n'a pas de direction");
    }

    @Test
    void lesAilesCassentCeQuiEstTendre() {
        // Toute durete entre 0 et 0,3, comme l'original — et rien de negatif, ce qui est la
        // pierre angulaire (durete -1) ou l'obsidienne.
        assertTrue(StormWingSkill.breaks(0f), "l'herbe");
        assertTrue(StormWingSkill.breaks(0.3f), "la limite de l'original");
        assertFalse(StormWingSkill.breaks(0.31f), "juste au-dessus");
        assertFalse(StormWingSkill.breaks(1.5f), "la pierre");
        assertFalse(StormWingSkill.breaks(-1f), "ce qui ne se casse pas");
    }

    @Test
    void lesAilesSOuvrentApresLaCharge() {
        AbilityData data = new AbilityData();
        int charge = WING.chargeTicks(data);
        assertEquals(70, charge, "septante ticks au depart");

        assertFalse(WING.opened(data, charge), "au dernier tick de la charge, elles sont fermees");
        assertTrue(WING.opened(data, charge + 1), "et ouvertes juste apres");
        assertFalse(WING.opensThisTick(data, charge), "le souffle ne part pas avant");
        assertTrue(WING.opensThisTick(data, charge + 1), "il part au premier tick de vol");
    }

    /**
     * Le saut de vitesse de l'original, a 45 % d'experience.
     *
     * Sous ce seuil les ailes poussent a 0,7, au-dela a 1,2 — et les deux grandissent de 2 a 3
     * fois avec l'experience. C'est le seul endroit du port ou une courbe n'est pas continue,
     * et c'est celui de l'original.
     */
    @Test
    void laVitesseSuitLExperienceEtChangeDeRegime() {
        AbilityData start = atExperience(0f);
        AbilityData novice = atExperience(0.44f);
        AbilityData expert = atExperience(0.46f);
        AbilityData full = atExperience(1f);

        assertEquals(1.4f, WING.speed(start), 1.0e-4f, "0,7 fois 2 au depart");
        assertEquals(1.708f, WING.speed(novice), 1.0e-3f, "et 0,7 fois 2,44 juste avant le saut");
        assertEquals(2.952f, WING.speed(expert), 1.0e-3f, "puis 1,2 fois 2,46 : le regime rapide");
        assertTrue(WING.speed(expert) > WING.speed(novice) + 1.2f,
                "le saut de regime se sent passer : " + WING.speed(expert));
        assertEquals(3.6f, WING.speed(full), 1.0e-4f, "1,2 fois 3 au maximum");
    }

    /** Une donnee d'aptitude a cette experience, sans passer par un vrai joueur. */
    private static AbilityData atExperience(float exp) {
        AbilityData data = new AbilityData();
        // Sans categorie apprise, l'experience d'une competence se lit a zero : c'est la
        // garde de l'original, conservee.
        data.setCategoryLevel(WING.getCategory(), 1);
        data.learnSkill(WING);
        data.addSkillExp(WING, exp);
        return data;
    }

    /** Comparaison de vecteurs, sans se battre avec les arrondis flottants. */
    private static void assertVec(Vec3 expected, Vec3 actual, String what) {
        assertEquals(expected.x, actual.x, 1.0e-6, what + " (x)");
        assertEquals(expected.y, actual.y, 1.0e-6, what + " (y)");
        assertEquals(expected.z, actual.z, 1.0e-6, what + " (z)");
    }
}
