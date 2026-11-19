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

    // ------------------------------------------------------------------
    // Les competences, vues comme une seule liste
    // ------------------------------------------------------------------

    /**
     * Nombre de competences de toutes les categories.
     *
     * L'ecran du developpeur ne peut pas passer un objet {@code Skill} a un bouton :
     * le clic d'un bouton de conteneur est un entier. Il passe donc l'indice de la
     * competence dans cette liste a plat, et le serveur refait le chemin inverse. Les
     * deux cotes enumerent les memes categories dans le meme ordre — celui du
     * registre — donc les indices concordent.
     */
    public int getSkillCount() {
        int total = 0;
        for (Category category : categories) {
            total += category.getSkills().size();
        }
        return total;
    }

    /** La competence a cet indice, ou {@code null} s'il est hors bornes. */
    public Skill getSkill(int globalIndex) {
        if (globalIndex < 0) return null;
        int remaining = globalIndex;
        for (Category category : categories) {
            int size = category.getSkills().size();
            if (remaining < size) return category.getSkill(remaining);
            remaining -= size;
        }
        return null;
    }

    /** L'indice a plat d'une competence, ou -1 si elle n'est pas enregistree. */
    public int indexOfSkill(Skill skill) {
        if (skill == null) return -1;
        int offset = 0;
        for (Category category : categories) {
            int size = category.getSkills().size();
            if (category == skill.getCategory()) {
                int index = category.getSkills().indexOf(skill);
                return index < 0 ? -1 : offset + index;
            }
            offset += size;
        }
        return -1;
    }
}
