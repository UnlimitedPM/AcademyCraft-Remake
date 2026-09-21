package cn.academy.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.academy.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Recettes du fusor d'Imag.
 *
 * <h2>D'ou viennent les deux recettes</h2>
 *
 * Le depot de l'original ne contient aucune recette de fusor : elles etaient
 * ajoutees de l'exterieur, par un script MineTweaker ({@code ImagFusorSupport}).
 * La seule trace dans les sources est donc la documentation du mod, et elle est
 * precise : le tutoriel {@code imag_fusor.md} annonce 3000 mB de phase pour
 * passer d'un cristal de basse purete a un cristal de purete moyenne, et 8000 mB
 * pour passer de la purete moyenne a la haute. Ce sont ces deux chiffres qui sont
 * portes, avec les trois cristaux du mod.
 *
 * A noter que 8000 mB est exactement la capacite de la cuve du fusor : la
 * deuxieme etape demande donc de remplir la cuve en entier, ce qui n'est pas un
 * hasard mais une contrainte de conception.
 *
 * <h2>Objets et pas ItemStack</h2>
 *
 * Comme pour le formeur de metal, la table retient des {@link Item} et des
 * quantites plutot que des {@link ItemStack} : un ItemStack ne peut pas etre
 * construit avant que les registres du jeu ne soient remplis, et une recette ne
 * doit pas partager d'objet mutable avec les piles du jeu.
 */
public final class ImagFusorRecipes {

    private ImagFusorRecipes() {}

    private static List<Recipe> recipes;

    /**
     * Une transformation : tant d'un objet, tant de millibassins de phase, vers
     * tant d'un autre objet.
     */
    public record Recipe(Item input, int inputCount, int liquid, Item output, int outputCount) {

        /** Vrai si cette recette concerne cet objet. */
        public boolean matches(ItemStack stack) {
            return !stack.isEmpty() && stack.is(input);
        }

        /** Le resultat, en pile neuve. */
        public ItemStack createOutput() {
            return new ItemStack(output, outputCount);
        }

        /** Vrai si la pile peut accueillir le resultat sans deborder. */
        public boolean fits(ItemStack existing) {
            if (existing.isEmpty()) return true;
            if (!existing.is(output)) return false;
            return existing.getCount() + outputCount <= existing.getMaxStackSize();
        }
    }

    /** Toutes les recettes. */
    public static List<Recipe> all() {
        if (recipes == null) recipes = buildDefaults();
        return Collections.unmodifiableList(recipes);
    }

    /** La recette qui concerne cet objet, ou {@code null}. */
    public static Recipe get(ItemStack input) {
        if (input.isEmpty()) return null;
        for (Recipe recipe : all()) {
            if (recipe.matches(input)) return recipe;
        }
        return null;
    }

    private static List<Recipe> buildDefaults() {
        List<Recipe> built = new ArrayList<>();
        built.add(new Recipe(ModItems.CRYSTAL_LOW.get(), 1, 3000, ModItems.CRYSTAL_NORMAL.get(), 1));
        built.add(new Recipe(ModItems.CRYSTAL_NORMAL.get(), 1, 8000, ModItems.CRYSTAL_PURE.get(), 1));
        return built;
    }
}
