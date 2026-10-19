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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
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

    private static final Map<String, List<View>> TABLE = new HashMap<>();
    private static final Map<String, List<ItemStack>> RECIPES = new HashMap<>();

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
        add("terminal", Kind.RECIPE, "terminal_installer");
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
     * Les ingredients d'une recette d'artisanat, dans l'ordre du fichier.
     *
     * <p>L'original dessinait une grille de trois sur trois, l'emplacement de chaque
     * ingredient etant celui de la recette. Le port se contente de les ranger dans l'ordre :
     * une recette en forme de triangle ne se lit donc pas exactement comme la sienne, mais
     * les ingredients et le resultat sont les bons.
     */
    public static List<ItemStack> ingredients(String id) {
        List<ItemStack> known = RECIPES.get(id);
        if (known != null) return known;

        List<ItemStack> out = new ArrayList<>();
        ItemStack wanted = stack(id);
        var level = Minecraft.getInstance().level;

        if (level != null && !wanted.isEmpty()) {
            for (CraftingRecipe recipe : level.getRecipeManager()
                    .getAllRecipesFor(RecipeType.CRAFTING)) {
                if (!recipe.getResultItem(level.registryAccess()).is(wanted.getItem())) continue;

                for (Ingredient ingredient : recipe.getIngredients()) {
                    ItemStack[] items = ingredient.getItems();
                    out.add(items.length == 0 ? ItemStack.EMPTY : items[0]);
                    if (out.size() == 9) break;
                }
                break;
            }
        }

        RECIPES.put(id, out);
        return out;
    }

    /** L'icone de l'etiquette d'une vue. */
    public static ResourceLocation tagIcon(View view) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                "textures/guis/icons/icon_" + view.kind().icon + ".png");
    }
}
