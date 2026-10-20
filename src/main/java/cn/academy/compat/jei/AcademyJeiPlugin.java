package cn.academy.compat.jei;

import cn.academy.AcademyCraft;
import cn.academy.ModBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * L'integration JEI du mod : les recettes des machines, la ou le jeu ne les montre pas.
 *
 * <p>Le mod a deux machines qui transforment sans passer par une table de craft — le <b>formeur
 * de metal</b> (quatre modes) et le <b>fusor d'Imag</b>. Sans cette classe, un joueur qui installe
 * JEI ne voit ni l'une ni l'autre : leurs recettes vivent dans les classes Java des machines, pas
 * dans les fichiers de recettes du jeu.
 *
 * <h2>Une dependance FACULTATIVE, jamais livree</h2>
 *
 * <p>JEI n'est qu'une dependance de COMPILATION (voir {@code build.gradle}) : le mod ne le charge
 * pas, ne le nomme pas ailleurs, et ne le declare que comme optionnel dans {@code mods.toml}. Cette
 * classe n'est instanciee que par JEI lui-meme, quand il la trouve — un joueur sans JEI ne charge
 * donc jamais ce paquet, et rien ne manque.
 */
@JeiPlugin
public class AcademyJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "jei");

    private static final Logger LOGGER = LoggerFactory.getLogger("academy-jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new MetalFormerCategory(registration.getJeiHelpers().getGuiHelper()),
                new ImagFusorCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var former = MetalFormerCategory.recipes();
        var fusor = ImagFusorCategory.recipes();
        registration.addRecipes(MetalFormerCategory.TYPE, former);
        registration.addRecipes(ImagFusorCategory.TYPE, fusor);
        // Une ligne par chargement : c'est la seule facon de savoir, dans un journal, si JEI a
        // bien pris nos deux categories (il les ignore en silence si le plugin lui a echappe).
        LOGGER.debug("JEI: {} recettes de formeur, {} de fusor", former.size(), fusor.size());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.METAL_FORMER.get()),
                MetalFormerCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.IMAG_FUSOR.get()),
                ImagFusorCategory.TYPE);
    }

    /**
     * Le runtime de JEI, retenu des qu'il est pret.
     *
     * <p>C'est l'usage prevu par l'API (les autres mods s'en servent pour cacher des objets ou
     * ajouter des recettes a chaud), et c'est aussi la seule porte par laquelle le banc de test du
     * projet peut ouvrir une de nos deux categories sans passer par un clic.
     */
    private static IJeiRuntime runtime;

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        hideTagPages(jeiRuntime);
    }

    /**
     * Retire de JEI ses pages de <b>tags</b>.
     *
     * <p>JEI en cree une par tag (« Block Tags », « Item Tags »...) et les montre dans la liste des
     * utilisations de l'objet, a cote des vraies recettes. Le joueur les trouve inutiles : c'est
     * une liste de noms internes, pas une recette.
     *
     * <p>On les cache plutot que de toucher a la configuration de JEI : c'est l'API qui le prevoit
     * ({@code hideRecipeCategory}), et ca reste chez nous. Elles sont reconnaissables a leur classe
     * ({@code ...plugins.jei.tags.TagInfoRecipeCategory}) ; si un jour JEI la renomme, on ne cache
     * simplement plus rien, sans rien casser.
     */
    private static void hideTagPages(IJeiRuntime jeiRuntime) {
        var manager = jeiRuntime.getRecipeManager();
        var tags = manager.createRecipeCategoryLookup().get()
                .filter(category -> category.getClass().getName().endsWith("tags.TagInfoRecipeCategory"))
                .map(mezz.jei.api.recipe.category.IRecipeCategory::getRecipeType)
                .toList();
        tags.forEach(manager::hideRecipeCategory);
        LOGGER.debug("JEI: {} pages de tags masquees", tags.size());
    }

    /** Le runtime de JEI, ou null si JEI n'est pas la (ou pas encore pret). */
    public static IJeiRuntime runtime() {
        return runtime;
    }
}
