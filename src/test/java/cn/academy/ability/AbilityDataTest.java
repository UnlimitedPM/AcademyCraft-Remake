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

        float max = data.getMaxControlPoint();
        // On regenere bien au-dela du plafond.
        for (int i = 0; i < 100; i++) {
            data.tickRegen(1f);
        }

        assertEquals(max, data.getControlPoint(), EPSILON);
    }

    @Test
    @DisplayName("clampToConfiguredMax ramene le plafond et rogne les CP en trop")
    void clampToConfiguredMax() {
        AbilityData data = new AbilityData();
        // On simule un etat sauvegarde avec un plafond plus genereux que la config.
        CompoundTag tag = data.serializeNBT();
        tag.putFloat("maxCp", 999_999f);
        tag.putFloat("cp", 999_999f);
        data.deserializeNBT(tag);
        assertEquals(999_999f, data.getMaxControlPoint(), EPSILON);

        data.clampToConfiguredMax();

        assertEquals((float) cn.academy.Config.controlPointMax, data.getMaxControlPoint(), EPSILON);
        assertEquals((float) cn.academy.Config.controlPointMax, data.getControlPoint(), EPSILON,
                "les CP en trop doivent etre rognes au nouveau plafond");
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
    @DisplayName("un NBT sans plafond prend la valeur de la config")
    void missingMaxFallsBackToConfig() {
        AbilityData data = new AbilityData();
        CompoundTag tag = new CompoundTag();
        tag.putFloat("cp", 12.5f);

        data.deserializeNBT(tag);

        assertEquals(12.5f, data.getControlPoint(), EPSILON);
        assertEquals((float) cn.academy.Config.controlPointMax, data.getMaxControlPoint(), EPSILON);
    }
}
