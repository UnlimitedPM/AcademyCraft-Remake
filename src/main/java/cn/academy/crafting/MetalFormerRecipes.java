package cn.academy.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.academy.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Recettes du formeur de metal.
 *
 * Portage de {@code MetalFormerRecipes} de la 1.12.2. L'original remplissait sa
 * table depuis {@code VanillaCategories.init}, avec quatre recettes :
 *
 * <ul>
 *   <li>INCISE : plaque de fer renforcee vers 6 aiguilles ;</li>
 *   <li>INCISE : rail vanilla vers 2 aiguilles ;</li>
 *   <li>PLATE : 2 plaques de fer renforcees vers 3 pieces ;</li>
 *   <li>ETCH : tranche de silicium vers un silbarn.</li>
 * </ul>
 *
 * <h2>Objets et pas ItemStack</h2>
 *
 * Les recettes retiennent des {@link Item} et des quantites plutot que des
 * {@link ItemStack}. Deux raisons : un ItemStack ne peut pas etre construit avant
 * que les registres du jeu ne soient remplis, et une recette ne doit de toute
 * facon pas partager d'objet mutable avec les piles du jeu. Le seul ItemStack qui
 * compte est celui compare, et il est fourni par l'appelant.
 *
 * La construction est differee au premier acces, pour la meme raison : la classe
 * peut etre chargee sans que les registres soient prets, c'est l'appel qui doit
 * l'etre.
 */
public final class MetalFormerRecipes {

    private MetalFormerRecipes() {}

    private static List<Recipe> recipes;

    /**
     * Une transformation : tant d'un objet vers tant d'un autre, dans un mode
     * donne.
     */
    public record Recipe(Item input, int inputCount, Item output, int outputCount, MetalFormerMode mode) {

        /**
         * Vrai si la pile fournie peut servir d'entree pour ce mode.
         *
         * Reprend {@code RecipeObject.accepts} de l'original : meme objet, mode
         * egal, et assez d'exemplaires. Les degats de l'objet n'entrent pas en
         * compte, aucun des objets concernes n'etant usurable.
         */
        public boolean accepts(ItemStack stack, MetalFormerMode askedMode) {
            return !stack.isEmpty()
                    && mode == askedMode
                    && stack.is(input)
                    && stack.getCount() >= inputCount;
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

    /** Toutes les recettes, dans l'ordre de l'original. */
    public static List<Recipe> all() {
        if (recipes == null) recipes = buildDefaults();
        return Collections.unmodifiableList(recipes);
    }

    /** La premiere recette qui accepte cette entree dans ce mode, ou {@code null}. */
    public static Recipe get(ItemStack input, MetalFormerMode mode) {
        if (input.isEmpty()) return null;
        for (Recipe recipe : all()) {
            if (recipe.accepts(input, mode)) return recipe;
        }
        return null;
    }

    private static List<Recipe> buildDefaults() {
        List<Recipe> built = new ArrayList<>();
        built.add(new Recipe(ModItems.REINFORCED_IRON_PLATE.get(), 1, ModItems.NEEDLE.get(), 6,
                MetalFormerMode.INCISE));
        built.add(new Recipe(Items.RAIL, 1, ModItems.NEEDLE.get(), 2, MetalFormerMode.INCISE));
        built.add(new Recipe(ModItems.REINFORCED_IRON_PLATE.get(), 2, ModItems.COIN.get(), 3,
                MetalFormerMode.PLATE));
        built.add(new Recipe(ModItems.WAFER.get(), 1, ModItems.SILBARN.get(), 1, MetalFormerMode.ETCH));
        return built;
    }
}
