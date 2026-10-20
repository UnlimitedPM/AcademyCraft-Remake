package cn.academy.compat.jei;

import cn.academy.AcademyCraft;
import cn.academy.ModBlocks;
import cn.academy.crafting.ImagFusorRecipes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * La categorie JEI du <b>fusor d'Imag</b>.
 *
 * <h2>Le dessin est celui de l'original</h2>
 *
 * Comme le formeur de metal, le fusor a sa planche NEI d'origine —
 * {@code guis/nei_fusor.png} (115x66) — qui n'avait jamais ete branchee : IN, OUT, la cuve en haut
 * et la chambre de fusion au milieu. On la pose telle quelle, et la seule chose qu'on y ajoute est
 * la quantite de phase, ecrite dans la cuve : c'est le chiffre qui decide de l'usage — 3000 mB pour
 * passer en purete moyenne, 8000 pour la haute, soit la cuve entiere.
 *
 * <p>Le fond est clair (198 de gris) et les emplacements sombres (119) : les cristaux, presque
 * blancs, s'y lisent.
 */
public class ImagFusorCategory implements IRecipeCategory<ImagFusorRecipes.Recipe> {

    public static final RecipeType<ImagFusorRecipes.Recipe> TYPE =
            RecipeType.create(AcademyCraft.MOD_ID, "imag_fusor", ImagFusorRecipes.Recipe.class);

    /** La planche de l'original, dans sa taille exacte. */
    private static final ResourceLocation PAGE = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/guis/nei_fusor.png");

    private static final int WIDTH = 115;
    private static final int HEIGHT = 66;

    /** Les deux emplacements, la ou la planche les dessine. */
    private static final int INPUT_X = 6;
    private static final int OUTPUT_X = 94;
    private static final int SLOT_Y = 37;

    /**
     * La cuve : son interieur va de 27 a 88, mais la goutte de phase y est dessinee jusqu'a 36, donc
     * le chiffre prend la place qui reste a sa droite.
     */
    private static final int GAUGE_LEFT = 40;
    private static final int GAUGE_RIGHT = 88;
    private static final int GAUGE_Y = 13;

    /** La couleur du texte : du gris fonce, sur le gris clair de la planche. */
    private static final int TEXT_COLOUR = 0xFF303030;

    private final IDrawable icon;
    private final Component title;

    public ImagFusorCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.IMAG_FUSOR.get()));
        this.title = Component.translatable("jei.academy.imag_fusor");
    }

    /** Les deux recettes du fusor : elles sont fixes, et ne dependent d'aucun monde. */
    public static List<ImagFusorRecipes.Recipe> recipes() {
        return ImagFusorRecipes.all();
    }

    @Override
    public RecipeType<ImagFusorRecipes.Recipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    /** La planche, et la quantite de phase ecrite dans sa cuve (voir {@link MachinePlaque}). */
    @Override
    public void createRecipeExtras(mezz.jei.api.gui.widgets.IRecipeExtrasBuilder builder,
                                   ImagFusorRecipes.Recipe recipe, IFocusGroup focuses) {
        builder.addDrawableWidget(new MachinePlaque(PAGE, WIDTH, HEIGHT, null,
                new MachinePlaque.Label(recipe.liquid() + " mB",
                        GAUGE_LEFT, GAUGE_RIGHT, GAUGE_Y)))
                .setPosition(0, 0);
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ImagFusorRecipes.Recipe recipe,
                          IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, SLOT_Y)
                .addItemStack(new ItemStack(recipe.input(), recipe.inputCount()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .addItemStack(recipe.createOutput());
    }
}
