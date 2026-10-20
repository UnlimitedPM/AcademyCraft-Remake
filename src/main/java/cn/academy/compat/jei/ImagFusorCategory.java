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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * La categorie JEI du <b>fusor d'Imag</b>.
 *
 * <p>Une recette tient en trois choses : le cristal qui entre, la <b>phase</b> consommee — 3000
 * milli-bassins pour passer en purete moyenne, 8000 pour la haute, soit la cuve entiere — et le
 * cristal qui sort. La quantite de phase est ecrite entre les deux emplacements plutot que
 * dessinee comme un fluide : c'est la seule chose que le joueur doit lire, et un emplacement de
 * fluide n'aurait pas la place de la dire.
 */
public class ImagFusorCategory implements IRecipeCategory<ImagFusorRecipes.Recipe> {

    public static final RecipeType<ImagFusorRecipes.Recipe> TYPE =
            RecipeType.create(AcademyCraft.MOD_ID, "imag_fusor", ImagFusorRecipes.Recipe.class);

    private static final int WIDTH = 128;
    private static final int HEIGHT = 46;

    private static final int INPUT_X = 10;
    private static final int OUTPUT_X = 96;
    private static final int SLOT_Y = 14;

    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    public ImagFusorCategory(IGuiHelper helper) {
        this.background = JeiBackground.of(WIDTH, HEIGHT, INPUT_X, SLOT_Y, OUTPUT_X, SLOT_Y);
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
    public IDrawable getBackground() {
        return background;
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

    @Override
    public void draw(ImagFusorRecipes.Recipe recipe, IRecipeSlotsView slotsView,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        String phase = recipe.liquid() + " mB";
        graphics.drawString(font, phase, (WIDTH - font.width(phase)) / 2, 10, 0xFF9BE0F5, false);
        graphics.drawString(font, "->", (WIDTH - font.width("->")) / 2, 24, 0xFFB0B0B0, false);
    }
}
