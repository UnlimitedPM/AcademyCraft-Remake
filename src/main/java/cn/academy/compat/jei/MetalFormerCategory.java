package cn.academy.compat.jei;

import cn.academy.AcademyCraft;
import cn.academy.ModBlocks;
import cn.academy.crafting.MetalFormerMode;
import cn.academy.crafting.MetalFormerRecipes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * La categorie JEI du <b>formeur de metal</b>.
 *
 * <p>Une ligne par recette : ce qui entre a gauche, ce qui sort a droite, et le <b>mode</b> entre
 * les deux — c'est lui qui tranche, la meme entree donnant deux resultats differents selon qu'on
 * incise ou qu'on grave. Les quatre modes vivent donc dans une seule categorie, avec le nom du
 * mode ecrit au milieu.
 *
 * <p>Le raffinage generique (tout minerai qui fond donne deux fois sa fonte) est calcule a part,
 * pour la raison qui l'empeche d'etre une table fermee : la liste depend des mods installes. Voir
 * {@link MetalFormerRecipes#allIn(net.minecraft.world.level.Level)}.
 */
public class MetalFormerCategory implements IRecipeCategory<MetalFormerRecipes.Recipe> {

    public static final RecipeType<MetalFormerRecipes.Recipe> TYPE =
            RecipeType.create(AcademyCraft.MOD_ID, "metal_former", MetalFormerRecipes.Recipe.class);

    private static final int WIDTH = 128;
    private static final int HEIGHT = 46;

    /** Ou tombent les deux emplacements, dans la page de la categorie. */
    private static final int INPUT_X = 10;
    private static final int OUTPUT_X = 96;
    private static final int SLOT_Y = 14;

    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    public MetalFormerCategory(IGuiHelper helper) {
        this.background = JeiBackground.of(WIDTH, HEIGHT, INPUT_X, SLOT_Y, OUTPUT_X, SLOT_Y);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.METAL_FORMER.get()));
        this.title = Component.translatable("jei.academy.metal_former");
    }

    /**
     * Les recettes a montrer.
     *
     * <p>On les lit dans le monde du client : c'est lui qui porte la liste des recettes de fonte,
     * dont le raffinage generique se deduit. Sans monde (menu principal), seules les recettes
     * fixes sont montrees — elles n'ont besoin de rien.
     */
    public static List<MetalFormerRecipes.Recipe> recipes() {
        var level = Minecraft.getInstance().level;
        try {
            return level == null ? MetalFormerRecipes.all() : MetalFormerRecipes.allIn(level);
        } catch (RuntimeException e) {
            // Une partie de recettes qui n'est pas encore prete ne doit pas empecher JEI de
            // demarrer : on montre au moins les recettes fixes.
            return MetalFormerRecipes.all();
        }
    }

    @Override
    public RecipeType<MetalFormerRecipes.Recipe> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, MetalFormerRecipes.Recipe recipe,
                          IFocusGroup focuses) {
        builder.addSlot(mezz.jei.api.recipe.RecipeIngredientRole.INPUT, INPUT_X, SLOT_Y)
                .addItemStack(new ItemStack(recipe.input(), recipe.inputCount()));
        builder.addSlot(mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .addItemStack(recipe.createOutput());
    }

    @Override
    public void draw(MetalFormerRecipes.Recipe recipe, IRecipeSlotsView slotsView,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        String mode = Component.translatable(
                "academy.metal_former.mode." + recipe.mode().getId()).getString();

        // Le nom du mode est CENTRE entre les deux emplacements, et la fleche sous lui : c'est ce
        // qui dit a quoi sert la recette, sans quoi deux recettes de meme entree se
        // ressembleraient.
        int width = font.width(mode);
        graphics.drawString(font, mode, (WIDTH - width) / 2, 10, 0xFFFFFFFF, false);
        graphics.drawString(font, "->", (WIDTH - font.width("->")) / 2, 24, 0xFFB0B0B0, false);
    }
}
