package cn.academy.compat.crafttweaker;

import org.openzen.zencode.java.ZenCodeType;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.item.IItemStack;

import cn.academy.ImagFusorBlockEntity;
import cn.academy.crafting.ImagFusorRecipes;
import net.minecraft.world.item.ItemStack;

/**
 * Le fusor d'Imag, ouvert aux scripts CraftTweaker : {@code mods.academy.ImagFusor}.
 *
 * <p>Deux choses seulement font une recette de fusor : ce qui entre, et combien de millibassins de
 * phase il faut. Le fusor choisit sa recette par l'entree seule, d'ou un remplacement par entree —
 * voir {@link ImagFusorRecipes#add(ImagFusorRecipes.Recipe)}.
 *
 * <p>La quantite de phase est bornee par la cuve : une recette qui en demanderait plus que
 * {@link ImagFusorBlockEntity#TANK_SIZE} ne pourrait jamais tourner, et c'est refuse a l'ecriture
 * plutot que decouvert en jeu.
 *
 * <h2>Exemple</h2>
 *
 * <pre>{@code
 * // un cristal de purete moyenne vers un cristal pur, pour 5000 mA de phase
 * mods.academy.ImagFusor.addRecipe(<item:academy:crystal_normal>, <item:academy:crystal_pure>, 5000);
 *
 * // plus de fusion depuis les cristaux de basse purete
 * mods.academy.ImagFusor.removeRecipeByInput(<item:academy:crystal_low>);
 *
 * mods.academy.ImagFusor.removeAllRecipes();
 * }</pre>
 */
@ZenRegister
@ZenCodeType.Name("mods.academy.ImagFusor")
public final class ImagFusorCraftTweaker {

    private ImagFusorCraftTweaker() {}

    /**
     * Ajoute une fusion.
     *
     * @param input ce qui entre, quantite comprise dans la pile
     * @param output ce qui sort, quantite comprise dans la pile
     * @param liquid les millibassins de phase consommes, entre 1 et la contenance de la cuve
     */
    @ZenCodeType.Method
    public static void addRecipe(IItemStack input, IItemStack output, int liquid) {
        ItemStack in = internal(input, "entree");
        ItemStack out = internal(output, "sortie");
        if (liquid <= 0 || liquid > ImagFusorBlockEntity.TANK_SIZE) {
            throw new IllegalArgumentException("AcademyCraft : la phase d'une recette de fusor doit tenir entre 1 et "
                    + ImagFusorBlockEntity.TANK_SIZE + " mB, pas " + liquid + ".");
        }

        ImagFusorRecipes.add(new ImagFusorRecipes.Recipe(
                in.getItem(), in.getCount(), liquid, out.getItem(), out.getCount()));
    }

    /**
     * Retire les recettes qui donnent cet objet.
     *
     * @return le nombre de recettes retirees
     */
    @ZenCodeType.Method
    public static int removeRecipe(IItemStack output) {
        return ImagFusorRecipes.removeByOutput(internal(output, "sortie").getItem());
    }

    /**
     * Retire la recette de cette entree.
     *
     * @return le nombre de recettes retirees
     */
    @ZenCodeType.Method
    public static int removeRecipeByInput(IItemStack input) {
        return ImagFusorRecipes.removeByInput(internal(input, "entree").getItem());
    }

    /** Vide la table : le fusor ne fait plus rien. */
    @ZenCodeType.Method
    public static void removeAllRecipes() {
        ImagFusorRecipes.removeAll();
    }

    /** La pile d'un script, en refusant le vide : elle n'a rien a faire dans une recette. */
    private static ItemStack internal(IItemStack stack, String what) {
        ItemStack internal = stack == null ? ItemStack.EMPTY : stack.getInternal();
        if (internal.isEmpty()) {
            throw new IllegalArgumentException("AcademyCraft : la " + what + " d'une recette est vide.");
        }
        return internal;
    }
}
