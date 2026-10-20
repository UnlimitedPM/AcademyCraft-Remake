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
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * La categorie JEI du <b>formeur de metal</b>.
 *
 * <h2>Le dessin est celui de l'original</h2>
 *
 * L'original livrait deux planches pour son integration NEI — {@code guis/nei_metalformer.png} et
 * {@code guis/nei_fusor.png} — et ne les avait jamais branchees. Ce sont exactement celles qu'il
 * faut ici : IN, MODE, OUT, les deux emplacements, la case du mode et les deux fleches. On s'en sert
 * telles quelles plutot que de redessiner, pour que le joueur reconnaisse sa machine et lise d'un
 * coup d'oeil <b>dans quelle condition</b> la recette s'emploie : c'est la case du mode qui le dit.
 *
 * <p>Le fond est clair (198 de gris) et les emplacements plus sombres (139) : les objets du mod,
 * presque blancs, s'y lisent — c'est ce que le premier essai, sur un fond blanc, ratait.
 *
 * <p>La case du mode porte l'ICONE de ce mode, celle que la machine affiche dans son propre ecran
 * ({@code icons/icon_former_<mode>.png}, 48x48 ramenee a 14), son nom ecrit sous les emplacements,
 * et ce meme nom en infobulle sur la case : IN, MODE et OUT sont des images, donc muettes pour qui
 * n'a jamais ouvert la machine.
 */
public class MetalFormerCategory implements IRecipeCategory<MetalFormerRecipes.Recipe> {

    public static final RecipeType<MetalFormerRecipes.Recipe> TYPE =
            RecipeType.create(AcademyCraft.MOD_ID, "metal_former", MetalFormerRecipes.Recipe.class);

    /** La planche de l'original, dans sa taille exacte (94x57). */
    private static final ResourceLocation PAGE = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/guis/nei_metalformer.png");

    private static final int PAGE_W = 94;
    private static final int PAGE_H = 57;

    /**
     * La page fait la planche PLUS une case de mode : les icones de mode sont des dessins de 48, et
     * reduites a la case de 14 de la planche (ou aux 24 de l'ecran de la machine) il n'en reste
     * qu'une cloche blanche — c'est ce que le joueur voyait. A l'echelle 1, la scie circulaire de
     * INCISE, le stylet d'ETCH et la plaque de PLATE se lisent enfin.
     */
    private static final int MODE_X = 100;
    private static final int MODE_Y = 4;
    private static final int MODE_BOX = 48;
    private static final int MODE_RIGHT = MODE_X + MODE_BOX;

    private static final int WIDTH = MODE_RIGHT + 2;
    private static final int HEIGHT = 66;

    /** La ligne du nom du mode, sous la case. */
    private static final int NAME_Y = 55;

    /** Les deux emplacements, la ou la planche les dessine (son cadre fait 18, l'objet 16). */
    private static final int INPUT_X = 6;
    private static final int OUTPUT_X = 72;
    private static final int SLOT_Y = 24;

    private final IDrawable icon;
    private final Component title;

    public MetalFormerCategory(IGuiHelper helper) {
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
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    /**
     * La planche, l'icone du mode et son nom — les trois d'un seul tenant.
     *
     * <p>Passe par {@code createRecipeExtras} et non par {@code getBackground}, que JEI 15 marque
     * pour suppression, ni par {@code draw} : celui-ci est peint PAR-DESSUS les objets, donc une
     * planche y cacherait les piles. Voir {@link MachinePlaque}.
     */
    @Override
    public void createRecipeExtras(mezz.jei.api.gui.widgets.IRecipeExtrasBuilder builder,
                                   MetalFormerRecipes.Recipe recipe, IFocusGroup focuses) {
        builder.addDrawableWidget(new MachinePlaque(WIDTH, HEIGHT,
                new MachinePlaque.Picture(PAGE, PAGE_W, PAGE_H, 0, 0),
                new MachinePlaque.Icon(iconOf(recipe.mode()), MODE_X, MODE_Y),
                new MachinePlaque.Label(modeName(recipe.mode()), MODE_X, MODE_RIGHT, NAME_Y)))
                .setPosition(0, 0);
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MetalFormerRecipes.Recipe recipe,
                          IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, SLOT_Y)
                .addItemStack(new ItemStack(recipe.input(), recipe.inputCount()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .addItemStack(recipe.createOutput());
    }

    /** L'infobulle de la case du mode : son nom, ecrit en toutes lettres. */
    @Override
    public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder tooltip,
                           MetalFormerRecipes.Recipe recipe, IRecipeSlotsView slotsView,
                           double mouseX, double mouseY) {
        if (mouseX >= MODE_X && mouseX < MODE_RIGHT
                && mouseY >= MODE_Y && mouseY < MODE_Y + MODE_BOX) {
            tooltip.add(Component.translatable(
                    "academy.metal_former.mode." + recipe.mode().getId()));
        }
    }

    /** L'icone d'un mode : celle que la machine affiche dans son propre ecran. */
    private static ResourceLocation iconOf(MetalFormerMode mode) {
        return ResourceLocation.fromNamespaceAndPath(
                AcademyCraft.MOD_ID, "textures/guis/icons/icon_former_" + mode.getId() + ".png");
    }

    /** Le nom traduit d'un mode. */
    private static String modeName(MetalFormerMode mode) {
        return Component.translatable("academy.metal_former.mode." + mode.getId()).getString();
    }
}
