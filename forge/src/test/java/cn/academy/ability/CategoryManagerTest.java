package cn.academy.ability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests unitaires de {@link CategoryManager} : tri, attribution des identifiants
 * et verrouillage apres bake.
 */
class CategoryManagerTest {

    private static final class DummySkill extends Skill {
        DummySkill(String name) { super(name); }
    }

    private static Category categoryWith(String name, String... skillNames) {
        Category category = new Category(name);
        for (String skillName : skillNames) {
            category.addSkill(new DummySkill(skillName));
        }
        return category;
    }

    @Test
    @DisplayName("bake trie les categories par nom et leur donne des identifiants stables")
    void bakeSortsAndAssignsIds() {
        CategoryManager manager = new CategoryManager();
        Category vecmanip = categoryWith("vecmanip");
        Category electromaster = categoryWith("electromaster");
        Category meltdowner = categoryWith("meltdowner");

        // Enregistrees dans un ordre different du tri alphabetique.
        manager.register(vecmanip);
        manager.register(electromaster);
        manager.register(meltdowner);
        manager.bake();

        // Ordre alphabetique : electromaster, meltdowner, vecmanip.
        assertEquals(0, electromaster.getCategoryId());
        assertEquals(1, meltdowner.getCategoryId());
        assertEquals(2, vecmanip.getCategoryId());

        // L'identifiant doit correspondre a la position dans la liste triee.
        assertSame(electromaster, manager.getCategory(0));
        assertSame(meltdowner, manager.getCategory(1));
        assertSame(vecmanip, manager.getCategory(2));
    }

    @Test
    @DisplayName("bake est idempotent : un second appel ne change rien")
    void bakeIsIdempotent() {
        CategoryManager manager = new CategoryManager();
        Category a = categoryWith("a");
        Category b = categoryWith("b");
        manager.register(b);
        manager.register(a);

        manager.bake();
        manager.bake();

        assertEquals(0, a.getCategoryId());
        assertEquals(1, b.getCategoryId());
        assertEquals(2, manager.getCategories().size());
    }

    @Test
    @DisplayName("getCategory accepte un nom et refuse un identifiant hors bornes")
    void lookupByNameAndBounds() {
        CategoryManager manager = new CategoryManager();
        Category teleporter = categoryWith("teleporter");
        manager.register(teleporter);
        manager.bake();

        assertSame(teleporter, manager.getCategory("teleporter"));
        assertNull(manager.getCategory("inconnu"));
        assertNull(manager.getCategory(5));
        assertNull(manager.getCategory(-1));
    }

    @Test
    @DisplayName("enregistrer une categorie apres bake est refuse")
    void registerAfterBakeIsRejected() {
        CategoryManager manager = new CategoryManager();
        manager.register(categoryWith("first"));
        manager.bake();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> manager.register(categoryWith("late")));
        assertEquals("Cannot register category after bake()", ex.getMessage());
    }

    @Test
    @DisplayName("la liste des categories est non modifiable")
    void categoriesListIsImmutable() {
        CategoryManager manager = new CategoryManager();
        manager.register(categoryWith("a"));
        manager.bake();

        assertThrows(UnsupportedOperationException.class,
                () -> manager.getCategories().add(categoryWith("b")));
    }

    @Test
    @DisplayName("un manager vide se bake sans erreur")
    void emptyManagerBakes() {
        CategoryManager manager = new CategoryManager();
        manager.bake();

        assertEquals(0, manager.getCategories().size());
        assertNull(manager.getCategory(0));
    }
}
