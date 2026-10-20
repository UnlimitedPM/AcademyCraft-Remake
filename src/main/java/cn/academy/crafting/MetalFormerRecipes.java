package cn.academy.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.academy.ModBlocks;
import cn.academy.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Recettes du formeur de metal.
 *
 * Portage de {@code MetalFormerRecipes} de la 1.12.2. L'original remplissait sa
 * table depuis deux endroits :
 *
 * <ul>
 *   <li>{@code VanillaCategories.init}, quatre recettes — la plaque de fer renforcee
 *       vers six aiguilles, le rail vanilla vers deux aiguilles, deux plaques vers
 *       trois pieces, et la tranche de silicium vers un silbarn ;</li>
 *   <li>{@code MFIFRecipes.init}, neuf autres — tailler le lingot de silicium en deux
 *       tranches et une tranche en quatre morceaux, graver une puce de donnees en puce
 *       de calcul, plaquer le fer et le lingot de contrainte, et raffiner ses quatre
 *       minerais ;</li>
 *   <li>et l'OreDictionary, pour tout le reste : chaque minerai qui fond se rafraichit
 *       en deux fois son resultat. C'est la surcharge de {@code get} qui l'apporte, et
 *       c'est la regle qui donne son sens au mode REFINE.</li>
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
        return Collections.unmodifiableList(ensure());
    }

    /** La table vivante, construite au premier acces. */
    private static List<Recipe> ensure() {
        if (recipes == null) recipes = buildDefaults();
        return recipes;
    }

    /**
     * Ajoute une recette, en remplacant celle qui a la meme entree dans le meme mode.
     *
     * <p>Remplacer plutot qu'empiler est ce qui rend les scripts rejouables : un script
     * CraftTweaker est re-execute a chaque {@code /reload}, et une recette ajoutee deux fois ne doit
     * pas apparaitre deux fois. Le mode est donc la cle : une entree n'a qu'une transformation par
     * mode, c'est deja la regle de {@link #get(ItemStack, MetalFormerMode)} qui prend la premiere.
     */
    public static void add(Recipe recipe) {
        List<Recipe> list = ensure();
        list.removeIf(existing -> existing.mode() == recipe.mode() && existing.input() == recipe.input());
        list.add(recipe);
    }

    /** Retire les recettes qui donnent cet objet. Renvoie le nombre de recettes retirees. */
    public static int removeByOutput(Item output) {
        List<Recipe> list = ensure();
        int before = list.size();
        list.removeIf(recipe -> recipe.output() == output);
        return before - list.size();
    }

    /** Retire la recette de cette entree dans ce mode. Renvoie le nombre de recettes retirees. */
    public static int removeByInput(Item input, MetalFormerMode mode) {
        List<Recipe> list = ensure();
        int before = list.size();
        list.removeIf(recipe -> recipe.input() == input && recipe.mode() == mode);
        return before - list.size();
    }

    /** Vide la table. Les recettes fixes ne reviennent qu'au redemarrage. */
    public static void removeAll() {
        ensure().clear();
    }

    /**
     * La table de depart, pour les tests : oublie tout ce que les scripts ont pu y mettre.
     *
     * <p>Reservee aux tests unitaires — en jeu, elle n'est jamais appelee : un {@code /reload} ne
     * doit pas effacer le travail des scripts, et c'est pourquoi les modifications sont ecrites pour
     * pouvoir etre rejouees (voir {@link #add(Recipe)}).
     */
    public static void reset() {
        recipes = buildDefaults();
    }

    /**
     * Toutes les recettes d'un monde : les fixes, puis le raffinage du jeu.
     *
     * <p>Sert a JEI, qui tourne cote client : {@link #refined} y est volontairement eteint (c'est le
     * serveur qui decide d'une transformation), donc la table de raffinage est refaite ici, sans
     * cache, a partir des recettes de fonte que le client connait. Le resultat est celui du serveur :
     * meme regle, meme source.
     */
    public static List<Recipe> allIn(net.minecraft.world.level.Level level) {
        List<Recipe> built = new ArrayList<>(all());
        if (level != null) built.addAll(resolveRefine(level));
        return built;
    }
    /** La premiere recette qui accepte cette entree dans ce mode, ou {@code null}. */
    public static Recipe get(ItemStack input, MetalFormerMode mode) {
        if (input.isEmpty()) return null;
        for (Recipe recipe : all()) {
            if (recipe.accepts(input, mode)) return recipe;
        }
        return null;
    }

    /**
     * La meme chose, en tenant compte des <b>minerais du jeu</b>.
     *
     * <p>L'original remplissait sa table de l'OreDictionary : chaque minerai connu de Forge, du cuivre
     * de tel mod a l'argent de tel autre, donnait deux fois ce que le four en fond. Le port n'en
     * connaissait que quatre : les siens. Les recettes fixes passent donc d'abord — un minerai du mod
     * a la sienne, et le raffinage generique ne doit pas la lui prendre — puis vient le raffinage du
     * jeu, resolu une fois par partie de recettes.
     */
    public static Recipe get(net.minecraft.world.level.Level level, ItemStack input, MetalFormerMode mode) {
        Recipe fixed = get(input, mode);
        if (fixed != null) return fixed;
        if (mode != MetalFormerMode.REFINE || input.isEmpty()) return null;

        for (Recipe recipe : refined(level)) {
            if (recipe.accepts(input, mode)) return recipe;
        }
        return null;
    }

    /**
     * Les recettes de raffinage du jeu : tout minerai qui fond se rafine en deux fois son resultat.
     *
     * <p>C'est la regle de l'original, et c'est elle qui donne son sens a la machine : sans elle, le
     * mode REFINE ne savait rien faire des minerais, c'est-a-dire de tout ce pour quoi on construit un
     * formeur de metal. Elle se lit dans les recettes de fonte plutot que dans une liste d'etiquettes
     * ecrite a la main, ce qui la fait marcher pour les minerais de n'importe quel mod : un minerai
     * qui fond est un minerai qui se raffine.
     */
    private static List<Recipe> refined(net.minecraft.world.level.Level level) {
        if (level == null || level.isClientSide) return List.of();

        net.minecraft.world.item.crafting.RecipeManager manager = level.getRecipeManager();
        if (manager != cachedFor) {
            cachedFor = manager;
            refined = resolveRefine(level);
        }
        return refined;
    }

    private static List<Recipe> resolveRefine(net.minecraft.world.level.Level level) {
        List<Recipe> built = new ArrayList<>();
        for (var smelting : level.getRecipeManager()
                .getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.SMELTING)) {
            ItemStack result = smelting.getResultItem(level.registryAccess());
            if (result.isEmpty()) continue;

            // Deux fois ce que le four en donne, plafonne a une pile, comme l'original.
            int count = result.getCount() * 2;
            if (count > result.getMaxStackSize()) count = result.getMaxStackSize();

            for (net.minecraft.world.item.crafting.Ingredient ingredient : smelting.getIngredients()) {
                for (ItemStack ore : ingredient.getItems()) {
                    if (!isOre(ore)) continue;
                    built.add(new Recipe(ore.getItem(), 1, result.getItem(), count,
                            MetalFormerMode.REFINE));
                }
            }
        }
        return built;
    }

    /** Vrai si cet objet est un minerai, au sens de Forge comme de vanilla. */
    private static boolean isOre(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(FORGE_ORES) || stack.is(VANILLA_ORES));
    }

    /**
     * Les recettes fixes : celles de l'original, et rien d'autre.
     *
     * <p>Les quatre premieres viennent de son {@code VanillaCategories.init}, les suivantes de son
     * {@code MFIFRecipes.init} — neuf transformations, dont les quatre minerais du mod. La table du
     * port s'arretait aux quatre premieres : il manquait de quoi tailler le silicium, graver une puce,
     * plaquer le fer, et surtout raffiner le moindre minerai.
     */
    private static List<Recipe> buildDefaults() {
        List<Recipe> built = new ArrayList<>();

        built.add(new Recipe(ModItems.REINFORCED_IRON_PLATE.get(), 1, ModItems.NEEDLE.get(), 6,
                MetalFormerMode.INCISE));
        built.add(new Recipe(Items.RAIL, 1, ModItems.NEEDLE.get(), 2, MetalFormerMode.INCISE));
        built.add(new Recipe(ModItems.REINFORCED_IRON_PLATE.get(), 2, ModItems.COIN.get(), 3,
                MetalFormerMode.PLATE));
        built.add(new Recipe(ModItems.WAFER.get(), 1, ModItems.SILBARN.get(), 1, MetalFormerMode.ETCH));

        built.add(new Recipe(ModItems.IMAG_SILICON_INGOT.get(), 1, ModItems.WAFER.get(), 2,
                MetalFormerMode.INCISE));
        built.add(new Recipe(ModItems.WAFER.get(), 1, ModItems.IMAG_SILICON_PIECE.get(), 4,
                MetalFormerMode.INCISE));
        built.add(new Recipe(ModItems.DATA_CHIP.get(), 1, ModItems.CALC_CHIP.get(), 1,
                MetalFormerMode.ETCH));
        built.add(new Recipe(Items.IRON_INGOT, 1, ModItems.REINFORCED_IRON_PLATE.get(), 1,
                MetalFormerMode.PLATE));
        built.add(new Recipe(ModItems.CONSTRAINT_INGOT.get(), 1, ModItems.CONSTRAINT_PLATE.get(), 1,
                MetalFormerMode.PLATE));

        built.add(new Recipe(ModBlocks.IMAGSIL_ORE.get().asItem(), 1,
                ModItems.IMAG_SILICON_INGOT.get(), 4, MetalFormerMode.REFINE));
        built.add(new Recipe(ModBlocks.CONSTRAINT_METAL_ORE.get().asItem(), 1,
                ModItems.CONSTRAINT_INGOT.get(), 2, MetalFormerMode.REFINE));
        built.add(new Recipe(ModBlocks.RESO_ORE.get().asItem(), 1,
                ModItems.RESO_CRYSTAL.get(), 3, MetalFormerMode.REFINE));
        built.add(new Recipe(ModBlocks.CRYSTAL_ORE.get().asItem(), 1,
                ModItems.CRYSTAL_LOW.get(), 4, MetalFormerMode.REFINE));

        // Les deux minerais que l'original ne doublait pas : le redstone donne un bloc, et le lapis en
        // donne douze. Le raffinage generique les prendrait autrement, donc ils passent avant lui.
        for (Item ore : new Item[] { Items.REDSTONE_ORE, Items.DEEPSLATE_REDSTONE_ORE }) {
            built.add(new Recipe(ore, 1, Items.REDSTONE_BLOCK, 1, MetalFormerMode.REFINE));
        }
        for (Item ore : new Item[] { Items.LAPIS_ORE, Items.DEEPSLATE_LAPIS_ORE }) {
            built.add(new Recipe(ore, 1, Items.LAPIS_LAZULI, 12, MetalFormerMode.REFINE));
        }
        return built;
    }

    /** Le jeu tient la liste des minerais dans ses etiquettes : ce sont elles qu'on interroge. */
    private static final net.minecraft.tags.TagKey<Item> FORGE_ORES =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "ores"));

    private static final net.minecraft.tags.TagKey<Item> VANILLA_ORES =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "ores"));

    /** La partie de recettes dont les recettes de raffinage ont ete tirees. */
    private static net.minecraft.world.item.crafting.RecipeManager cachedFor;
    private static List<Recipe> refined = List.of();
}
