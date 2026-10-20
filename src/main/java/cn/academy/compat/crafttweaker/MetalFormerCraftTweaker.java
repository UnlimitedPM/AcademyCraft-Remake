package cn.academy.compat.crafttweaker;

import java.util.ArrayList;
import java.util.List;

import org.openzen.zencode.java.ZenCodeType;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.item.IItemStack;

import cn.academy.crafting.MetalFormerMode;
import cn.academy.crafting.MetalFormerRecipes;
import net.minecraft.world.item.ItemStack;

/**
 * Le formeur de metal, ouvert aux scripts CraftTweaker : {@code mods.academy.MetalFormer}.
 *
 * <p>Le formeur a une table a lui, qui ne ressemble pas a une recette de Minecraft — pas de fichier
 * JSON, pas de type de recette du jeu, juste quatre modes et deux objets. CraftTweaker ne peut donc
 * pas la gerer tout seul : ce sont ces quelques methodes qui la lui ouvrent.
 *
 * <h2>Scripts rejouables</h2>
 *
 * Un script CraftTweaker est re-execute a chaque {@code /reload}. Ajouter une recette ne doit donc
 * pas la dupliquer, et c'est la regle des deux tables : {@code addRecipe} remplace ce qui traitait
 * deja la meme entree (dans le meme mode pour le formeur), {@code removeRecipe} retire ce qui reste.
 * Un script rejoue donne exactement le meme etat.
 *
 * <p>La contrepartie est assumee : une suppression tient jusqu'au redemarrage, meme si l'on efface
 * la ligne du script puis recharge — il faut remettre la recette a la main (ou redemarrer) pour la
 * retrouver. C'est le prix d'une table qui n'est pas du JSON relu par le jeu.
 *
 * <h2>Exemple</h2>
 *
 * <pre>{@code
 * // deux aiguilles dans le mode incise, a partir d'un lingot de fer
 * mods.academy.MetalFormer.addRecipe(<item:minecraft:iron_ingot>, <item:academy:needle> * 2, "incise");
 *
 * // plus de pieces dans le mode plaque
 * mods.academy.MetalFormer.removeRecipe(<item:academy:coin>);
 *
 * // tout refaire a zero
 * mods.academy.MetalFormer.removeAllRecipes();
 * }</pre>
 */
@ZenRegister
@ZenCodeType.Name("mods.academy.MetalFormer")
public final class MetalFormerCraftTweaker {

    private MetalFormerCraftTweaker() {}

    /**
     * Ajoute une transformation.
     *
     * @param input ce qui entre, quantite comprise dans la pile
     * @param output ce qui sort, quantite comprise dans la pile
     * @param mode {@code plate}, {@code incise}, {@code etch} ou {@code refine}
     */
    @ZenCodeType.Method
    public static void addRecipe(IItemStack input, IItemStack output, String mode) {
        ItemStack in = internal(input, "entree");
        ItemStack out = internal(output, "sortie");
        MetalFormerMode parsed = mode(mode);

        MetalFormerRecipes.add(new MetalFormerRecipes.Recipe(
                in.getItem(), in.getCount(), out.getItem(), out.getCount(), parsed));
    }

    /**
     * Retire les recettes qui donnent cet objet.
     *
     * @return le nombre de recettes retirees
     */
    @ZenCodeType.Method
    public static int removeRecipe(IItemStack output) {
        return MetalFormerRecipes.removeByOutput(internal(output, "sortie").getItem());
    }

    /**
     * Retire la recette de cette entree dans ce mode.
     *
     * @return le nombre de recettes retirees
     */
    @ZenCodeType.Method
    public static int removeRecipeByInput(IItemStack input, String mode) {
        return MetalFormerRecipes.removeByInput(internal(input, "entree").getItem(), mode(mode));
    }

    /** Vide la table : plus aucune transformation, dans aucun mode. */
    @ZenCodeType.Method
    public static void removeAllRecipes() {
        MetalFormerRecipes.removeAll();
    }

    /** Les modes connus, pour les scripts qui veulent les lister. */
    @ZenCodeType.Method
    public static List<String> getModes() {
        List<String> ids = new ArrayList<>();
        for (MetalFormerMode mode : MetalFormerMode.values()) ids.add(mode.getId());
        return ids;
    }

    /** La pile d'un script, en refusant le vide : elle n'a rien a faire dans une recette. */
    private static ItemStack internal(IItemStack stack, String what) {
        ItemStack internal = stack == null ? ItemStack.EMPTY : stack.getInternal();
        if (internal.isEmpty()) {
            throw new IllegalArgumentException("AcademyCraft : la " + what + " d'une recette est vide.");
        }
        return internal;
    }

    /** Le mode ecrit dans le script, en refusant un nom inconnu. */
    private static MetalFormerMode mode(String mode) {
        MetalFormerMode parsed = MetalFormerMode.byId(mode);
        if (parsed == null) {
            throw new IllegalArgumentException(
                    "AcademyCraft : mode de formeur inconnu '" + mode + "' (attendu : plate, incise, etch, refine).");
        }
        return parsed;
    }
}
