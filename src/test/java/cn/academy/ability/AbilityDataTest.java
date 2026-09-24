package cn.academy.ability;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitaires de {@link AbilityData} : arithmetique des Control Points et
 * persistance NBT.
 *
 * {@code CompoundTag} est une classe de donnees pure : elle s'instancie sans
 * demarrer Minecraft. C'est justement l'interet de ces tests, qui tournent en
 * quelques centaines de millisecondes via {@code gradlew test}.
 */
class AbilityDataTest {

    private static final float EPSILON = 1.0e-4f;

    private static final class DummySkill extends Skill {
        DummySkill(String name) { super(name); }
    }

    private static Category category(String name) {
        Category category = new Category(name);
        category.addSkill(new DummySkill("skill"));
        return category;
    }

    // ------------------------------------------------------------------
    // Control Points
    // ------------------------------------------------------------------

    @Test
    @DisplayName("consumeControlPoint debite quand il y a assez et refuse sinon")
    void consumeControlPoint() {
        AbilityData data = new AbilityData();
        float start = data.getControlPoint();

        assertTrue(data.consumeControlPoint(start - 10f), "largement dans les moyens");
        assertEquals(10f, data.getControlPoint(), EPSILON);

        assertFalse(data.consumeControlPoint(11f), "11 CP demandes pour 10 disponibles");
        assertEquals(10f, data.getControlPoint(), EPSILON,
                "un echec ne doit rien debiter");
    }

    @Test
    @DisplayName("consommer exactement le solde passe, et laisse 0")
    void consumeExactlyEverything() {
        AbilityData data = new AbilityData();
        float all = data.getControlPoint();

        assertTrue(data.consumeControlPoint(all));
        assertEquals(0f, data.getControlPoint(), EPSILON);
        assertFalse(data.consumeControlPoint(0.1f), "plus rien a consommer");
    }

    @Test
    @DisplayName("tickRegen remonte les CP sans jamais depasser le plafond")
    void tickRegenIsCappedAtMax() {
        AbilityData data = new AbilityData();
        data.consumeControlPoint(data.getControlPoint());
        assertEquals(0f, data.getControlPoint(), EPSILON);

        float before = data.getMaxControlPoint();
        // 0,86 point par tick en moyenne sur une reserve de 1800 : il en faut des milliers
        // pour la remplir, et c'est justement ce que l'original donnait a voir.
        for (int i = 0; i < 4000; i++) {
            data.tickRegen();
        }

        assertEquals(data.getMaxControlPoint(), data.getControlPoint(), EPSILON);
        assertTrue(data.getMaxControlPoint() >= before, "le plafond a suivi");
    }

    @Test
    @DisplayName("tickRegen suit la formule de l'original, plafond par plafond")
    void tickRegenFollowsTheOriginalFormula() {
        AbilityData empty = new AbilityData();
        empty.consumeControlPoint(empty.getControlPoint());
        // Le paiement arme le delai de recuperation : quinze ticks pendant lesquels rien ne
        // remonte, comme le cp_recover_cooldown de l'original.
        float beforeDelay = empty.getControlPoint();
        for (int i = 0; i < 15; i++) {
            empty.tickRegen();
        }
        assertEquals(beforeDelay, empty.getControlPoint(), EPSILON,
                "le delai qui suit un paiement ne regenere rien");

        float before = empty.getControlPoint();
        empty.tickRegen();
        float emptyGain = empty.getControlPoint() - before;

        // 0,0003 x 1800 = 0,54 : le chiffre de l'original, sur une barre vide.
        assertEquals(0.54f, emptyGain, 0.01f);

        AbilityData entamee = new AbilityData();
        float beforeEntamee = entamee.getControlPoint();
        entamee.tickRegen();
        float entameeGain = entamee.getControlPoint() - beforeEntamee;

        assertTrue(entameeGain > emptyGain,
                "plus la barre est pleine, plus ca remonte : " + entameeGain + " > " + emptyGain);
    }

    @Test
    @DisplayName("changer de niveau donne la reserve du niveau, pleine et sans ajout")
    void levelChangeGivesTheNewReserve() {
        AbilityData data = new AbilityData();
        Category category = category("test");

        data.setCategoryLevel(category, 1);
        assertEquals(1800f, data.getMaxControlPoint(), EPSILON, "l'init_cp du niveau 1");
        assertEquals(1800f, data.getControlPoint(), EPSILON,
                "et la reserve du niveau arrive pleine, comme CPData.recalcMaxValue");

        // On s'en sert : le plafond grandit, comme chez l'original.
        data.consumeControlPoint(20f);
        assertTrue(data.getAddMaxControlPoint() > 0f, "l'usage agrandit la reserve");

        data.setCategoryLevel(category, 3);
        assertEquals(4000f, data.getMaxControlPoint(), EPSILON, "l'init_cp du niveau 3");
        assertEquals(4000f, data.getControlPoint(), EPSILON);
        assertEquals(0f, data.getAddMaxControlPoint(), EPSILON,
                "le changeur de niveau remettait l'ajout a zero, chez l'original");
    }

    // ------------------------------------------------------------------
    // Niveaux de categorie
    // ------------------------------------------------------------------

    @Test
    @DisplayName("une categorie inconnue a le niveau 0 et n'est pas apprise")
    void unknownCategoryHasLevelZero() {
        AbilityData data = new AbilityData();
        Category category = category("test");

        assertEquals(0, data.getCategoryLevel(category));
        assertFalse(data.hasLearned(category));
    }

    @Test
    @DisplayName("apprendre une categorie la marque comme apprise")
    void learningACategory() {
        AbilityData data = new AbilityData();
        Category category = category("test");

        data.setCategoryLevel(category, 3);

        assertEquals(3, data.getCategoryLevel(category));
        assertTrue(data.hasLearned(category));
    }

    // ------------------------------------------------------------------
    // Persistance
    // ------------------------------------------------------------------

    @Test
    @DisplayName("serializeNBT / deserializeNBT conserve CP, plafond et niveaux")
    void nbtRoundTrip() {
        AbilityData source = new AbilityData();
        Category category = category("test");
        source.setCategoryLevel(category, 2);
        source.consumeControlPoint(30f);

        AbilityData restored = new AbilityData();
        restored.deserializeNBT(source.serializeNBT());

        assertEquals(source.getControlPoint(), restored.getControlPoint(), EPSILON);
        assertEquals(source.getMaxControlPoint(), restored.getMaxControlPoint(), EPSILON);
        assertEquals(2, restored.getCategoryLevel(category));
    }

    @Test
    @DisplayName("copyFrom transfere l'etat sans partager les objets")
    void copyFromTransfersState() {
        AbilityData source = new AbilityData();
        Category category = category("test");
        source.setCategoryLevel(category, 4);
        source.consumeControlPoint(25f);

        AbilityData target = new AbilityData();
        target.copyFrom(source);

        assertEquals(source.getControlPoint(), target.getControlPoint(), EPSILON);
        assertEquals(source.getMaxControlPoint(), target.getMaxControlPoint(), EPSILON);
        assertEquals(4, target.getCategoryLevel(category));
        assertNotSame(source, target);

        // Modifier la copie ne doit pas toucher a l'original.
        target.setCategoryLevel(category, 0);
        assertEquals(4, source.getCategoryLevel(category));
    }

    @Test
    @DisplayName("copyFrom remplace les niveaux au lieu de les fusionner")
    void copyFromReplacesLevels() {
        AbilityData source = new AbilityData();
        source.setCategoryLevel(category("gardee"), 1);

        AbilityData target = new AbilityData();
        target.setCategoryLevel(category("perimee"), 5);

        target.copyFrom(source);

        assertEquals(1, target.getCategoryLevel(category("gardee")));
        assertEquals(0, target.getCategoryLevel(category("perimee")),
                "les niveaux de la cible doivent etre ecrases");
    }

    @Test
    @DisplayName("un NBT sans plafond prend celui du niveau")
    void missingMaxFallsBackToTheLevelTable() {
        AbilityData data = new AbilityData();
        CompoundTag tag = new CompoundTag();
        tag.putFloat("cp", 12.5f);

        data.deserializeNBT(tag);

        assertEquals(12.5f, data.getControlPoint(), EPSILON);
        assertEquals(AbilityData.baseMaxControlPoint(0), data.getMaxControlPoint(), EPSILON,
                "le plafond ne se sauvegarde plus : il vient du niveau");
    }

    @Test
    @DisplayName("une sauvegarde trop genereuse est ramenee au plafond du niveau")
    void oversizedSaveIsCropped() {
        AbilityData data = new AbilityData();
        CompoundTag tag = data.serializeNBT();
        tag.putFloat("cp", 999_999f);

        data.deserializeNBT(tag);

        assertEquals(data.getMaxControlPoint(), data.getControlPoint(), EPSILON,
                "les CP en trop doivent etre rognes au plafond");
    }
}
