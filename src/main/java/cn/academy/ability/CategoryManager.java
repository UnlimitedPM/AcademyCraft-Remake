package cn.academy.ability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Registry of all ability Categories. Port of original CategoryManager.
 */
public final class CategoryManager {

    public static final CategoryManager INSTANCE = new CategoryManager();

    private final List<Category> categories = new ArrayList<>();
    private boolean baked = false;

    /** Visible au paquet pour que les tests unitaires partent d'un etat propre. */
    CategoryManager() {}

    public void register(Category category) {
        if (baked) throw new IllegalStateException("Cannot register category after bake()");
        categories.add(category);
    }

    public void bake() {
        if (baked) return;
        categories.sort(Comparator.comparing(Category::getName));
        for (int i = 0; i < categories.size(); i++) {
            categories.get(i).setCategoryId(i);
        }
        baked = true;
    }

    public Category getCategory(int id) {
        return id >= 0 && id < categories.size() ? categories.get(id) : null;
    }

    public Category getCategory(String name) {
        for (Category c : categories) {
            if (c.getName().equals(name)) return c;
        }
        return null;
    }

    public List<Category> getCategories() {
        return Collections.unmodifiableList(categories);
    }
}
