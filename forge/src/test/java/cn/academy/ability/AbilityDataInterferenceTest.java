package cn.academy.ability;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitaires des sources d'interference de {@link AbilityData}.
 *
 * Tout est du calcul pur : une source est une fonction qui repond oui ou non, et
 * le seul effet est un drapeau. C'est exactement le genre de mecanique qu'il vaut
 * mieux verifier ici qu'en jeu, ou il faudrait poser un brouilleur, s'en approcher,
 * essayer d'utiliser une competence et recommencer apres avoir coupe la machine.
 */
class AbilityDataInterferenceTest {

    private static final String KEY = "interferer@minecraft:overworld 10, 64, 10";

    // ------------------------------------------------------------------
    // Ajout et retrait
    // ------------------------------------------------------------------

    @Test
    @DisplayName("sans source, le joueur n'est pas brouille")
    void noSourceMeansNotInterfered() {
        AbilityData data = new AbilityData();

        data.refreshInterference();

        assertFalse(data.isInterfered());
        assertEquals(0, data.interferenceSourceCount());
    }

    @Test
    @DisplayName("une source active brouille le joueur")
    void activeSourceJams() {
        AbilityData data = new AbilityData();
        data.addInterference(KEY, () -> true);

        data.refreshInterference();

        assertTrue(data.isInterfered());
        assertEquals(1, data.interferenceSourceCount());
    }

    @Test
    @DisplayName("une source qui repond non est oubliee, et doit etre reposee pour rebrouiller")
    void inactiveSourceIsPruned() {
        AbilityData data = new AbilityData();
        boolean[] alive = {false};
        data.addInterference(KEY, () -> alive[0]);

        data.refreshInterference();
        assertFalse(data.isInterfered(), "une source eteinte ne brouille pas");
        assertEquals(0, data.interferenceSourceCount(), "et elle est oubliee tout de suite");

        // C'est la consequence directe de l'oubli : rallumer la source ne suffit
        // pas, il faut la reposer. Les machines du port le font a chaque cycle,
        // donc c'est invisible en jeu, mais le comportement est bien celui-la.
        alive[0] = true;
        data.refreshInterference();
        assertFalse(data.isInterfered(), "la source oubliee ne revient pas toute seule");

        data.addInterference(KEY, () -> alive[0]);
        data.refreshInterference();
        assertTrue(data.isInterfered(), "reposee, elle brouille de nouveau");
    }

    @Test
    @DisplayName("reposer la meme source ne la compte pas deux fois")
    void addingTwiceReplaces() {
        AbilityData data = new AbilityData();

        data.addInterference(KEY, () -> true);
        data.addInterference(KEY, () -> true);
        data.refreshInterference();

        assertEquals(1, data.interferenceSourceCount(),
                "une machine repose la sienne a chaque cycle : l'appel doit etre idempotent");
    }

    @Test
    @DisplayName("retirer une source la fait disparaitre")
    void removeSource() {
        AbilityData data = new AbilityData();
        data.addInterference(KEY, () -> true);
        data.refreshInterference();
        assertTrue(data.isInterfered());

        data.removeInterference(KEY);
        data.refreshInterference();

        assertFalse(data.isInterfered());
        assertEquals(0, data.interferenceSourceCount());
    }

    @Test
    @DisplayName("il suffit d'une source parmi plusieurs pour brouiller")
    void oneSourceIsEnough() {
        AbilityData data = new AbilityData();
        data.addInterference("a", () -> false);
        data.addInterference("b", () -> true);

        data.refreshInterference();

        assertTrue(data.isInterfered());
        assertEquals(1, data.interferenceSourceCount(), "la source muette doit avoir ete retiree");
    }

    // ------------------------------------------------------------------
    // Persistance
    // ------------------------------------------------------------------

    @Test
    @DisplayName("le drapeau survit a un aller-retour NBT, pas les sources")
    void flagSurvivesButNotSources() {
        AbilityData source = new AbilityData();
        source.addInterference(KEY, () -> true);
        source.refreshInterference();

        AbilityData restored = new AbilityData();
        restored.deserializeNBT(source.serializeNBT());

        assertTrue(restored.isInterfered(), "le client n'a que ce drapeau pour savoir");
        assertEquals(0, restored.interferenceSourceCount(),
                "les sources appartiennent au serveur : elles n'ont pas a voyager");
    }

    @Test
    @DisplayName("un rechargement efface le brouillage")
    void copyFromClearsInterference() {
        AbilityData source = new AbilityData();
        source.addInterference(KEY, () -> true);
        source.refreshInterference();

        AbilityData clone = new AbilityData();
        clone.addInterference("autre", () -> true);
        clone.refreshInterference();

        clone.copyFrom(source);

        assertFalse(clone.isInterfered(),
                "apres une mort, les brouilleurs en place reposeront leur source en quelques ticks");
        assertEquals(0, clone.interferenceSourceCount());
    }

    @Test
    @DisplayName("une donnee sans brouillage ne se relit pas comme brouillee")
    void absentFlagReadsAsFalse() {
        AbilityData data = new AbilityData();

        data.deserializeNBT(new CompoundTag());

        assertFalse(data.isInterfered(), "un fichier d'avant ce mecanisme ne doit pas brouiller");
    }
}
