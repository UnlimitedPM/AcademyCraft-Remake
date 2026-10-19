package cn.academy.terminal.client;

import cn.academy.AcademyCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Les apercus d'un tutoriel : ce que l'original mettait dans {@code showWindow}.
 *
 * <p>L'original les construisait a l'initialisation ({@code TutorialInit}) avec quatre
 * fabriques de {@code ViewGroups} : {@code drawsBlock} pour un bloc en volume,
 * {@code drawsItem} pour un objet, {@code displayIcon} pour une image, et
 * {@code recipes} pour une recette. Chaque apercu portait une etiquette — CRAFT, SMELT
 * ou VIEW — dessinee dans {@code tag_area}, et les fleches tournaient entre les vues.
 *
 * <p>Ici le tableau est une simple table : l'identifiant du tutoriel, puis ses vues. Le
 * dessin vit dans l'ecran ; cette classe ne fait que dire QUOI montrer, et retrouver la
 * pile d'un objet ou les ingredients d'une recette.
 */
@OnlyIn(Dist.CLIENT)
public final class TutorialPreviews {

    /** Ce qu'une vue montre, et donc l'etiquette qu'elle porte. */
    public enum Kind {
        BLOCK("view"), ITEM("view"), ICON("view"), RECIPE("craft");

        /** Le nom de l'icone de l'etiquette : {@code guis/icons/icon_<icon>.png}. */
        public final String icon;

        Kind(String icon) {
            this.icon = icon;
        }
    }

    /** Une vue : sa sorte, et l'identifiant de ce qu'elle dessine. */
    public record View(Kind kind, String id) {}

    /**
     * Une recette d'artisanat, posee dans la grille de trois sur trois.
     *
     * <p>C'est le {@code CraftingGridDisplay} de l'original : ses neuf cases a la place que la
     * recette leur donne — une recette en forme de triangle ne se lit pas comme une recette
     * pleine — et son resultat a droite.
     */
    public static final class Recipe {

        /** Vrai pour une recette en forme, faux pour une recette en vrac. */
        public final boolean shaped;

        /** Les neuf cases, dans l'ordre de lecture : une case vide porte une pile vide. */
        public final List<ItemStack> grid;

        /** Le resultat, ou une pile vide si la recette est introuvable. */
        public final ItemStack result;

        private Recipe(boolean shaped, List<ItemStack> grid, ItemStack result) {
            this.shaped = shaped;
            this.grid = Collections.unmodifiableList(grid);
            this.result = result;
        }
    }

    private static final Map<String, List<View>> TABLE = new HashMap<>();
    private static final Map<String, Recipe> RECIPES = new HashMap<>();

    private TutorialPreviews() {}

    private static void add(String tutorial, Kind kind, String... ids) {
        List<View> views = TABLE.computeIfAbsent(tutorial, key -> new ArrayList<>());
        for (String id : ids) views.add(new View(kind, id));
    }

    static {
        // L'ordre est celui de TutorialInit : il decide de l'ordre des etiquettes.
        add("ores", Kind.BLOCK,
                "constraint_metal", "imagsil_ore", "crystal_ore", "reso_ore");
        add("ores", Kind.ITEM, "matter_unit_phase_liquid");
        add("ores", Kind.RECIPE,
                "constraint_plate", "imag_silicon_ingot", "wafer", "imag_silicon_piece");

        add("phase_generator", Kind.RECIPE, "phase_gen");
        add("solar_generator", Kind.RECIPE, "solar_gen");
        add("wind_generator", Kind.RECIPE,
                "windgen_base", "windgen_pillar", "windgen_main", "windgen_fan");
        add("metal_former", Kind.RECIPE, "metal_former");
        add("imag_fusor", Kind.RECIPE, "imag_fusor");
        add("terminal", Kind.RECIPE, "terminal_installer",
                "app_skill_tree", "app_media_player", "app_freq_transmitter");
        add("ability_developer", Kind.RECIPE,
                "developer_portable", "dev_normal", "dev_advanced");
    }

    /** Les vues d'un tutoriel, dans l'ordre de l'original. */
    public static List<View> views(String tutorial) {
        return TABLE.getOrDefault(tutorial, List.of());
    }

    /** La pile d'un bloc ou d'un objet, ou une pile vide si l'identifiant est inconnu. */
    public static ItemStack stack(String id) {
        ResourceLocation location = ResourceLocation.tryParse(
                id.contains(":") ? id : AcademyCraft.MOD_ID + ":" + id);
        if (location == null) return ItemStack.EMPTY;

        Item item = BuiltInRegistries.ITEM.get(location);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /**
     * La premiere recette d'artisanat qui donne cet objet, mise en grille de trois sur trois.
     *
     * <p>L'original cherchait toutes les recettes d'un objet et les faisait defiler par les
     * fleches ; ici la premiere suffit — c'est celle que le joueur a sous les yeux dans son
     * etabli, et les fleches tournent entre les vues du tutoriel.
     */
    public static Recipe recipe(String id) {
        Recipe known = RECIPES.get(id);
        if (known != null) return known;

        Recipe found = null;
        ItemStack wanted = stack(id);
        var level = Minecraft.getInstance().level;

        if (level != null && !wanted.isEmpty()) {
            for (CraftingRecipe recipe : level.getRecipeManager()
                    .getAllRecipesFor(RecipeType.CRAFTING)) {
                ItemStack result = recipe.getResultItem(level.registryAccess());
                if (!result.is(wanted.getItem())) continue;

                found = grid(recipe, result);
                break;
            }
        }

        if (found == null) found = new Recipe(false, emptyGrid(), wanted);
        RECIPES.put(id, found);
        return found;
    }

    /** Une recette posee dans la grille : sa forme decide de la place de chaque ingredient. */
    private static Recipe grid(CraftingRecipe recipe, ItemStack result) {
        List<ItemStack> grid = emptyGrid();
        List<Ingredient> ingredients = recipe.getIngredients();
        boolean shaped = recipe instanceof ShapedRecipe;

        if (shaped) {
            int width = Math.max(1, ((ShapedRecipe) recipe).getWidth());
            for (int i = 0; i < ingredients.size() && i < 9; i++) {
                int row = i / width;
                int col = i % width;
                int slot = row * 3 + col;
                if (row < 3 && slot < 9) grid.set(slot, first(ingredients.get(i)));
            }
        } else {
            // En vrac : l'original les posait a la suite, sans case imposee.
            int slot = 0;
            for (Ingredient ingredient : ingredients) {
                if (slot >= 9) break;
                grid.set(slot++, first(ingredient));
            }
        }

        return new Recipe(shaped, grid, result);
    }

    /** La premiere pile d'un ingredient : l'original les faisait defiler toutes les deux secondes. */
    private static ItemStack first(Ingredient ingredient) {
        ItemStack[] stacks = ingredient.getItems();
        return stacks.length == 0 ? ItemStack.EMPTY : stacks[0];
    }

    private static List<ItemStack> emptyGrid() {
        List<ItemStack> grid = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) grid.add(ItemStack.EMPTY);
        return grid;
    }

    /** L'icone de l'etiquette d'une vue. */
    public static ResourceLocation tagIcon(View view) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                "textures/guis/icons/icon_" + view.kind().icon + ".png");
    }
}
